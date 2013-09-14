package com.artifex.mupdf;


import java.util.ArrayList;
import java.util.Iterator;

import com.appgeneration.magmanager.library.R;
import com.library.activity.MuPDFActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;


abstract class TextSelector 
{
	final private TextWord[][] mText;
	final private RectF mSelectBox;

	public TextSelector(TextWord[][] text, RectF selectBox) 
	{
		mText = text;
		mSelectBox = selectBox;
	}

	protected abstract void onStartLine();
	protected abstract void onWord(TextWord word);
	protected abstract void onEndLine();

	public void select() {
		if (mText == null || mSelectBox == null)
			return;

		ArrayList<TextWord[]> lines = new ArrayList<TextWord[]>();
		for (TextWord[] line : mText)
			if (line[0].bottom > mSelectBox.top && line[0].top < mSelectBox.bottom)
				lines.add(line);

		Iterator<TextWord[]> it = lines.iterator();
		while (it.hasNext()) {
			TextWord[] line = it.next();
			boolean firstLine = line[0].top < mSelectBox.top;
			boolean lastLine = line[0].bottom > mSelectBox.bottom;
			float start = Float.NEGATIVE_INFINITY;
			float end = Float.POSITIVE_INFINITY;

			if (firstLine && lastLine) {
				start = Math.min(mSelectBox.left, mSelectBox.right);
				end = Math.max(mSelectBox.left, mSelectBox.right);
			} else if (firstLine) {
				start = mSelectBox.left;
			} else if (lastLine) {
				end = mSelectBox.right;
			}

			onStartLine();

			for (TextWord word : line)
				if (word.right > start && word.left < end)
					onWord(word);

			onEndLine();
		}
	}
}


class PatchInfo {
	public BitmapHolder bmh;
	private Bitmap bm;
	public Point patchViewSize;
	public Rect  patchArea;
	public boolean completeRedraw;

	public PatchInfo(Point aPatchViewSize, Rect aPatchArea, BitmapHolder aBmh, boolean aCompleteRedraw) {
		bmh = aBmh;
		bm = null;
		patchViewSize = aPatchViewSize;
		patchArea = aPatchArea;
		completeRedraw = aCompleteRedraw;
	}

	public Bitmap getBm() {
		return bm;
	}

	public void setBm(Bitmap pBm) {
		bm = pBm;
	}
}

// Make our ImageViews opaque to optimize redraw
class OpaqueImageView extends ImageView {

	public OpaqueImageView(Context context) {
		super(context);
	}

	@Override
	public boolean isOpaque() {
		return true;
	}
}

public abstract class PageView extends ViewGroup {
	private static final String TAG = "PageView";

	private static final int HIGHLIGHT_COLOR = 0x805555FF;
	private static final int LINK_COLOR = 0x80FFCC88;
	private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
	private static final int PROGRESS_DIALOG_DELAY = 10;
	private final Context   mContext;
	protected     int       mPageNumber;
	private       Point     mParentSize;
	protected     Point     mSize;   // Size of page at minimum zoom
	protected     float     mSourceScale;

	private       ImageView mEntire; // Image rendered at minimum zoom
	private       BitmapHolder    mEntireBmh;
	private       AsyncTask<Void, Void, Bitmap> mDrawEntire;

	private       Point     mPatchViewSize; // View size on the basis of which the patch was created
	private       Rect      mPatchArea;
	private       ImageView mPatch;
	private       BitmapHolder mPatchBmh;

	private       AsyncTask<PatchInfo, Void, PatchInfo> mDrawPatch;
	private       RectF     mSearchBoxes[];
	private       RectF     mSelectBox;
	private       TextWord  mText[][];
	protected       LinkInfo[]  mLinks;
	private       LinkInfo  mUrls[];
	private       View      mSearchView;
	private       boolean   mIsBlank;
	private       boolean   mUsingHardwareAcceleration;
	private       boolean   mHighlightLinks;

	private       ProgressBar mBusyIndicator;
	private final Handler   mHandler = new Handler();
	private FrameLayout mLinksView;

	private com.artifex.mupdf.AsyncTask<Void, Void, com.artifex.mupdf.LinkInfo[]> mGetLinkInfo;

	public PageView(Context c, Point parentSize) {
		super(c);
		mContext    = c;
		mParentSize = parentSize;
		setBackgroundColor(BACKGROUND_COLOR);
		mEntireBmh = new BitmapHolder();
		mPatchBmh = new BitmapHolder();
//		mUsingHardwareAcceleration = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	protected abstract Bitmap drawPage(int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight);
	protected abstract Bitmap updatePage(BitmapHolder h, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight);
	protected abstract LinkInfo[] getLinkInfo();

	public void releaseResources() {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel(true);
			mDrawEntire = null;
		}

		if (mDrawPatch != null) {
			mDrawPatch.cancel(true);
			mDrawPatch = null;
		}

		mIsBlank = true;
		mPageNumber = 0;

		if (mSize == null)
			mSize = mParentSize;

		if (mEntire != null) {
			Drawable d = mEntire.getDrawable();
			if(d instanceof BitmapDrawable) {
				Bitmap b = ((BitmapDrawable)d).getBitmap();
				if(b!= null)
					b.recycle();
			}
			mEntire.setImageBitmap(null);
		}

		if (mPatch != null) {
			Drawable d = mPatch.getDrawable();
			if(d instanceof BitmapDrawable) {
				
				Bitmap bm = ((BitmapDrawable)d).getBitmap();
				if(bm != null) bm.recycle();
			}
			mPatch.setImageBitmap(null);
			
		}

		if (mBusyIndicator != null) {
			removeView(mBusyIndicator);
			mBusyIndicator = null;
		}
		if( mSearchBoxes!= null )
			mSearchBoxes =null;
		System.gc();
	}

	public void blank(int page) {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel(true);
			mDrawEntire = null;
		}

		if (mDrawPatch != null) {
			mDrawPatch.cancel(true);
			mDrawPatch = null;
		}

		mIsBlank = true;
		mPageNumber = page;

		if (mSize == null)
			mSize = mParentSize;

		if (mEntire != null)
			mEntire.setImageBitmap(null);

		if (mPatch != null)
			mPatch.setImageBitmap(null);

		if (mBusyIndicator == null) {
			mBusyIndicator = new ProgressBar(mContext);
			mBusyIndicator.setIndeterminate(true);
			mBusyIndicator.setBackgroundResource(R.drawable.busy);
			addView(mBusyIndicator);
		}
	}
	public void setPage(int page, PointF size) {
		// Cancel pending tasks
		if (mDrawEntire != null) {
			mDrawEntire.cancel(true);
			mDrawEntire = null;
		}
		if(mGetLinkInfo != null) {
			mGetLinkInfo.cancel(true);
			mGetLinkInfo = null;
		}
			
		mIsBlank = false;

		mPageNumber = page;
		if (mEntire == null) {
			mEntire = new OpaqueImageView(mContext);
			mEntire.setScaleType(ImageView.ScaleType.FIT_CENTER);
			addView(mEntire);
		}

		// Calculate scaled size that fits within the screen limits
		// This is the size at minimum zoom
		mSourceScale = Math.min(mParentSize.x/size.x, mParentSize.y/size.y);
		Point newSize = new Point((int)(size.x*mSourceScale), (int)(size.y*mSourceScale));
		mSize = newSize;
		mEntire.setImageBitmap(null);
		mEntireBmh.setBm(null);

		// Get the link info in the background
		
		mGetLinkInfo = new AsyncTask<Void,Void,LinkInfo[]>() {
			protected LinkInfo[] doInBackground(Void... v) {
				return getLinkInfo();
			}

			protected void onPostExecute(LinkInfo[] v) {
				mLinks = v;
				invalidate();
			}
		};

		mGetLinkInfo.execute();

		// Render the page in the background
		mDrawEntire = new AsyncTask<Void,Void,Bitmap>() {
			protected Bitmap doInBackground(Void... v) {
				return drawPage(mSize.x, mSize.y, 0, 0, mSize.x, mSize.y);
			}

			protected void onPreExecute() {
				mEntire.setImageBitmap(null);
				mEntireBmh.setBm(null);

				if (mBusyIndicator == null) {
					mBusyIndicator = new ProgressBar(mContext);
					mBusyIndicator.setIndeterminate(true);
					mBusyIndicator.setBackgroundResource(R.drawable.busy);
					addView(mBusyIndicator);
					mBusyIndicator.setVisibility(INVISIBLE);
					mHandler.postDelayed(new Runnable() {
						public void run() {
							if (mBusyIndicator != null)
								mBusyIndicator.setVisibility(VISIBLE);
						}
					}, PROGRESS_DIALOG_DELAY);
				}
			}

			protected void onPostExecute(Bitmap bm) {
				removeView(mBusyIndicator);
				mBusyIndicator = null;
				mEntire.setImageBitmap(bm);
				mEntireBmh.setBm(bm);
				
				
				
				invalidate();
				MuPDFActivity.enableDocView(); // nhi add
			}
		};

		mDrawEntire.execute();


		if (mSearchView == null) 
		{ //// for search task
			mSearchView = new View(mContext) 
			{
				@Override
				protected void onDraw(final Canvas canvas) {
					super.onDraw(canvas);
					
					// Work out current total scale factor
					// from source to view
					final float scale = mSourceScale*(float)getWidth()/(float)mSize.x;
					final Paint paint = new Paint();
					if( !mIsBlank)
					{
						if( mParentSize.x > mParentSize.y) // 2 pages mode
						{
							
							if(mSearchBoxes!= null)
							{

								paint.setColor(HIGHLIGHT_COLOR);
								int count = 0;
								
								for( RectF rect: mSearchBoxes)
								{
									if( count>= MuPDFActivity.searchTaskTwoPages.lengthLeftBoxs) // draw right
									{
										canvas.drawRect(rect.left*scale +getWidth()/2 , rect.top*scale,
										        rect.right*scale + getWidth()/2, rect.bottom*scale,
										        paint);
										
									}
									else //draw left
									{
									
										canvas.drawRect(rect.left*scale, rect.top*scale,
												        rect.right*scale, rect.bottom*scale,
												        paint);
									}
								
									count++;
								}
							}
						}
						else // one pages
						{
							
							if ( mSearchBoxes != null) 
							{
								paint.setColor(HIGHLIGHT_COLOR);
								for (RectF rect : mSearchBoxes)
								{
									canvas.drawRect(rect.left*scale, rect.top*scale,
											        rect.right*scale, rect.bottom*scale,
											        paint);
									
								}
							}
						}
					}
		
				/*
					if (mSelectBox != null && mText != null) {
						paint.setColor(HIGHLIGHT_COLOR);
						TextSelector sel = new TextSelector(mText, mSelectBox) {
							RectF rect;

							@Override
							protected void onStartLine() {
								rect = new RectF();
							}

							@Override
							protected void onWord(TextWord word) {
								rect.union(word);
							}

							@Override
							protected void onEndLine() {
								if (!rect.isEmpty())
									canvas.drawRect(rect.left*scale, rect.top*scale, rect.right*scale, rect.bottom*scale, paint);
							}
						};

						sel.select();
					}*/
				}
			};

			addView(mSearchView);
		}
		Log.d(TAG, "Setpage "+ page);
		requestLayout();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int x, y;
		switch(View.MeasureSpec.getMode(widthMeasureSpec)) {
		case View.MeasureSpec.UNSPECIFIED:
			x = mSize.x;
			break;
		default:
			x = View.MeasureSpec.getSize(widthMeasureSpec);
		}
		switch(View.MeasureSpec.getMode(heightMeasureSpec)) {
		case View.MeasureSpec.UNSPECIFIED:
			y = mSize.y;
			break;
		default:
			y = View.MeasureSpec.getSize(heightMeasureSpec);
		}

		setMeasuredDimension(x, y);

		if (mBusyIndicator != null) {
			int limit = Math.min(mParentSize.x, mParentSize.y)/2;
			mBusyIndicator.measure(View.MeasureSpec.AT_MOST | limit, View.MeasureSpec.AT_MOST | limit);
		}
		
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int w  = right-left;
		int h = bottom-top;

		if (mEntire != null) {
			mEntire.layout(0, 0, w, h);
		}
		if (mSearchView != null) { /// for search task
			mSearchView.layout(0, 0, w, h);
		}

		if (mPatchViewSize != null) {
			if (mPatchViewSize.x != w || mPatchViewSize.y != h) {
				// Zoomed since patch was created
				mPatchViewSize = null;
				mPatchArea     = null;
				if (mPatch != null) {
					mPatch.setImageBitmap(null);
					mPatchBmh.setBm(null);
				}
			} else {
				mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
			}
		}

		if (mBusyIndicator != null) {
			int bw = mBusyIndicator.getMeasuredWidth();
			int bh = mBusyIndicator.getMeasuredHeight();

			mBusyIndicator.layout((w-bw)/2, (h-bh)/2, (w+bw)/2, (h+bh)/2);
		}
//		super.onLayout(changed, left, top, right, bottom);
	}

	public void addHq(boolean update) {
		Rect viewArea = new Rect(getLeft(),getTop(),getRight(),getBottom());
		// If the viewArea's size matches the unzoomed size, there is no need for an hq patch
		if (viewArea.width() != mSize.x || viewArea.height() != mSize.y) {
			Point patchViewSize = new Point(viewArea.width(), viewArea.height());
			Rect patchArea = new Rect(0, 0, mParentSize.x, mParentSize.y);

			// Intersect and test that there is an intersection
			if (!patchArea.intersect(viewArea))
				return;

			// Offset patch area to be relative to the view top left
			patchArea.offset(-viewArea.left, -viewArea.top);

			boolean area_unchanged = patchArea.equals(mPatchArea) && patchViewSize.equals(mPatchViewSize);
			
			// If being asked for the same area as last time, nothing to do
			if (area_unchanged && !update)
				return;

			boolean completeRedraw = !(area_unchanged && update);

			// Stop the drawing of previous patch if still going
			if (mDrawPatch != null) {
				Log.d(TAG, "cancel mDrawPatch task");
				mDrawPatch.cancel(true);
				mDrawPatch = null;
			}

			if (completeRedraw) {
				// The bitmap holder mPatchBm may still be rendered to by a
				// previously invoked task, and possibly for a different
				// area, so we cannot risk the bitmap generated by this task
				// being passed to it
				mPatchBmh.drop();
				mPatchBmh = new BitmapHolder();
			}
			
			// Create and add the image view if not already done
			if (mPatch == null) {
				mPatch = new OpaqueImageView(mContext);
				mPatch.setScaleType(ImageView.ScaleType.FIT_CENTER);
				addView(mPatch);
				mSearchView.bringToFront(); // for search task
			}

			System.gc();
			
			mDrawPatch = new AsyncTask<PatchInfo,Void,PatchInfo>() {
				protected PatchInfo doInBackground(PatchInfo... v) {
					if (v[0].completeRedraw) {
						v[0].setBm(drawPage(v[0].patchViewSize.x, v[0].patchViewSize.y,
									v[0].patchArea.left, v[0].patchArea.top,
									v[0].patchArea.width(), v[0].patchArea.height()));
					} else {
						v[0].setBm(updatePage(v[0].bmh, v[0].patchViewSize.x, v[0].patchViewSize.y,
									v[0].patchArea.left, v[0].patchArea.top,
									v[0].patchArea.width(), v[0].patchArea.height()));
					}

					return v[0];
				}

				protected void onPostExecute(PatchInfo v) {
					if (mPatchBmh == v.bmh) {
						mPatchViewSize = v.patchViewSize;
						mPatchArea     = v.patchArea;
						if (v.getBm() != null) {
							mPatch.setImageBitmap(v.getBm());
							v.bmh.setBm(v.getBm());
							v.setBm(null);
						}
						//requestLayout();
						// Calling requestLayout here doesn't lead to a later call to layout. No idea
						// why, but apparently others have run into the problem.
						mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
						invalidate();
					}
				}
			};

			mDrawPatch.execute(new PatchInfo(patchViewSize, patchArea, mPatchBmh, completeRedraw));
		}
	}
	
	public void update() {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel(true);
			mDrawEntire = null;
		}

		if (mDrawPatch != null) {
			mDrawPatch.cancel(true);
			mDrawPatch = null;
		}

		// Render the page in the background
		mDrawEntire = new AsyncTask<Void,Void,Bitmap>() {
			protected Bitmap doInBackground(Void... v) {
				// Pass the current bitmap as a basis for the update, but use a bitmap
				// holder so that the held bitmap will be nulled and not hold on to
				// memory, should this view become redundant.
				return updatePage(mEntireBmh, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y);
			}

			protected void onPostExecute(Bitmap bm) {
				if (bm != null) {
					mEntire.setImageBitmap(bm);
					mEntireBmh.setBm(bm);
				}
				invalidate();
			}
		};

		mDrawEntire.execute();

		addHq(true);
	}

	public void removeHq() {
			// Stop the drawing of the patch if still going
			if (mDrawPatch != null) {
				mDrawPatch.cancel(true);
				mDrawPatch = null;
			}

			// And get rid of it
			mPatchViewSize = null;
			mPatchArea = null;
			if (mPatch != null) {
				mPatch.setImageBitmap(null);
				mPatchBmh.setBm(null);
			}
	}

	public int getPage() {
		return mPageNumber;
	}

	@Override
	public boolean isOpaque() {
		return true;
	}
/// add for search task
	public void setSearchBoxes(RectF searchBoxes[]) {
		mSearchBoxes = searchBoxes;
		if (mSearchView != null)
			mSearchView.invalidate();
	}
	public void setLinkHighlighting(boolean f) {
		//mHighlightLinks = f;
		if (mSearchView != null)
			mSearchView.invalidate();
	}

///end search task
}
