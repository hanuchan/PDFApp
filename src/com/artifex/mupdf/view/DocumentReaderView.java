package com.artifex.mupdf.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;

import android.util.FloatMath;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;


import com.artifex.mupdf.LinkInfoExternal;
import com.artifex.mupdf.MuPDFPageView;
import com.artifex.mupdf.PageView;
import com.artifex.mupdf.domain.SearchTaskResult;
import com.library.activity.MuPDFActivity;
import com.library.activity.SlideShowActivity;
import com.pdf.effect.CurlView;

public abstract class DocumentReaderView extends ReaderView {
	private static final String TAG = "DocumentReaderView";

	private enum LinkState {
		DEFAULT, HIGHLIGHT, INHIBIT
	};

	private static int tapPageMargin = 70;

	private LinkState linkState = LinkState.DEFAULT;

	private boolean showButtonsDisabled;

	public static DocumentReaderView s_Instant = null;
	public DocumentReaderView(Context context, 
			SparseArray<LinkInfoExternal[]> pLinkOfDocument) {
		super(context, pLinkOfDocument);
		s_Instant = this;
		Log.i("DocumentReaderView", "DocumentReaderView");
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		tapPageMargin = (int) (getWidth() * .1);
	}
public boolean onHitLinkActived( MotionEvent e)
{
	
	if (!showButtonsDisabled)
	{
		int linkPage = -1;
		String linkString = null;
		if (linkState != LinkState.INHIBIT) {
			MuPDFPageView pageView = (MuPDFPageView) getDisplayedView();
			if (pageView != null) {
				linkPage = pageView.hitLinkPage(e.getX(), e.getY());
				linkString = pageView.hitLinkUri(e.getX(),  e.getY());
			}
		}
	
		if (linkPage != -1) {
			// block pageView from sliding to next page
			noAutomaticSlide = true;
			//Log.d(TAG,"linkPage ="+ linkPage);
			setDisplayedViewIndex(linkPage);
			return true;
		} else if (linkString != null) {
			// start intent with url as linkString
			openLink(linkString);
			return true;
		} 
	}
	return false;
}
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		if (!showButtonsDisabled) {
			int linkPage = -1;
			String linkString = null;
			if (linkState != LinkState.INHIBIT) {
				MuPDFPageView pageView = (MuPDFPageView) getDisplayedView();
				if (pageView != null) {
					linkPage = pageView.hitLinkPage(e.getX(), e.getY());
					linkString = pageView.hitLinkUri(e.getX(),  e.getY());
				}
			}

			if (linkPage != -1) {
				// block pageView from sliding to next page
				noAutomaticSlide = true;
				//Log.d(TAG,"linkPage ="+ linkPage);
				setDisplayedViewIndex(linkPage);
			} else if (linkString != null) {
				// start intent with url as linkString
				openLink(linkString);
				if( MuPDFActivity.useEffectPage )
					return true;
			} 
			else 
			{
				if (e.getX() < tapPageMargin) 
				{
				//	Log.d(TAG, "moveToPrevious");
					super.moveToPrevious();
				} 
				else if (e.getX() > super.getWidth() - tapPageMargin) 
				{
				//	Log.d(TAG, "moveToNext");

					super.moveToNext();
				} 
				else 
				{
						
					onContextMenuClick();
				}
			}
		}
		//Log.i("onSingleTapConfirmed : "+e.getX(), "action: " +e.getAction());
		return super.onSingleTapUp(e);
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector d) {
		// Disabled showing the buttons until next touch.
		// Not sure why this is needed, but without it
		// pinch zoom can make the buttons appear
		showButtonsDisabled = true;
		return super.onScaleBegin(d);
	}
	private PointF mPointDown = new PointF();
	private static float k_delta = 30;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) 
		{
			showButtonsDisabled = false;
		}
		if(MuPDFActivity.useEffectPage)
		{
			if( mScale == 1.0f)
			{
				if( MuPDFActivity.mCurlView!= null)
				{
					Log.i("touch ", " anim : "+ MuPDFActivity.mCurlView.isInAnimated());
					if( MuPDFActivity.mCurlView.isInAnimated())
						return false;
				}
				switch ( event.getActionMasked())
				{
					case MotionEvent.ACTION_DOWN:
						//Log.d("doc MotionEvent.ACTION_DOWN: ","x: "+ event.getX()+" ; y:"+event.getY() );
						mPointDown.set(event.getX(), event.getY());
						MuPDFActivity.mCurlView.onTouchEffectDown(event);
						MuPDFActivity.mCurlView.setVisibility(VISIBLE);
						isJustScale = false;
					break;
					
					case MotionEvent.ACTION_POINTER_DOWN:
					//	Log.d("doc zooom : "+ MuPDFActivity.mCurlView.getVisibility(),"MotionEvent.ACTION_POINTER_DOWN");
						if(MuPDFActivity.mCurlView.mState != MuPDFActivity.mCurlView.k_EffectState){
							MuPDFActivity.mCurlView.mState = MuPDFActivity.mCurlView.k_ZoomState;
							setVisibility(VISIBLE);
							MuPDFActivity.mCurlView.setVisibility(INVISIBLE);	
						}
						//MuPDFActivity.mCurlView.onTouchZoomDown(event);
					break;
					case MotionEvent.ACTION_UP:
					//	Log.d("MotionEvent.ACTION_UP", "MuPDFActivity.mCurlView.mState: "+MuPDFActivity.mCurlView.mState);
					//	Log.d("curl : "+ MuPDFActivity.mCurlView.getVisibility()," doc: "+ getVisibility());
						//MuPDFActivity.mCurlView.onTouchZoomUp(event);
						if(MuPDFActivity.mCurlView.mState == MuPDFActivity.mCurlView.k_EffectState)
						{
							MuPDFActivity.mCurlView.onTouchEffectUp(event);
						MuPDFActivity.mCurlView.mState = MuPDFActivity.mCurlView.k_DummySate;
						return false;
						}
					break;
					case MotionEvent.ACTION_POINTER_UP:
					//	Log("MotionEvent.ACTION_POINTER_UP");
						//MuPDFActivity.mCurlView.onTouchZoomUp(event);
						MuPDFActivity.mCurlView.mState = MuPDFActivity.mCurlView.k_DummySate;
						if( isJustScale )//reset pointer down
						{
							mPointDown.set(0, 0);
						}
					break;
					case MotionEvent.ACTION_MOVE:
					{
						if( event.getPointerCount() > 1)
						{
							mScaleGestureDetector.onTouchEvent(event);
						}
						else
						{
							float dx1 = mPointDown.x - event.getX();
							float dy1 = mPointDown.y - event.getY();
							float delta = FloatMath.sqrt(dx1 * dx1 + dy1 * dy1);
							if(delta >= k_delta && !isJustScale){
								
								onShowEffect( event);
								MuPDFActivity.mCurlView.onTouch(s_Instant, event); ///effect show better
								
								
								return false;
							}	
							
						
						}
					}
					break;	
				}
			}
			else
			{
				setVisibility(VISIBLE);
				//Log.i(TAG, "invisibleEffectWhenZooming: ");
				invisibleEffectWhenZooming();
			}
		}
		//end add
		/*
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) 
		{
			showButtonsDisabled = false;
			//Log.i(TAG, "action down: "+ event.getX());
			// nhi add to control buttons list in menu	
			if( MuPDFActivity.useEffectPage )
			{
				if( mScale == 1.0f && !onHitLinkActived( event))
				{
					
				//	Log.d("+++++++++++++++++++++++++++++++++TOUCH EVENT", " show effect");
					if( MuPDFActivity.currentViewMode == CurlView.SHOW_ONE_PAGE)
					{
						if( event.getX() <= MuPDFActivity.PHONE_WIDTH/4)
						{
							onShowEffect(event);
						
							return false;
						}
						else if(  event.getX() >= MuPDFActivity.PHONE_WIDTH*3/4 )
						{
							onShowEffect( event);
				
							return false;
						}
						else
						{
							invisibleEffectWhenZooming();
						}
					}
					else
					{
						if( event.getX() <= MuPDFActivity.PHONE_WIDTH/3)
						{
							onShowEffect(event);
					
							return false;
						}
						else if(  event.getX() >= MuPDFActivity.PHONE_WIDTH*2/3 )
						{
							onShowEffect(event);
						
							return false;
						}
						else
						{
							invisibleEffectWhenZooming();
						}
					}
				}
				else
				{
					invisibleEffectWhenZooming();
				}
			}
		}*/
		// nhi end add

		return super.onTouchEvent(event);
	}
	protected void onShowEffect( MotionEvent event){}
	protected void invisibleEffectWhenZooming(){}
	public boolean isShowButtonsDisabled() {
		return showButtonsDisabled;
	}

	abstract protected void onContextMenuClick();


	//	protected void onChildSetup(int i, View v) {
//		if (SearchTaskResult.get() != null && SearchTaskResult.get().pageNumber == i)
//			((PageView)v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
//		else
//			((PageView)v).setSearchBoxes(null);
//
//		((PageView)v).setLinkHighlighting(mLinkState == LinkState.HIGHLIGHT);
//	}

	@Override
	protected void onMoveToChild(View view, int i) {

		if( MuPDFActivity.currentViewMode == CurlView.SHOW_ONE_PAGE)
		{
			if (SearchTaskResult.get() != null && SearchTaskResult.get().pageNumber != i) {
				SearchTaskResult.recycle();
		}
		else
		{
			if( MuPDFActivity.searchTaskTwoPages!= null)
			{
				if(MuPDFActivity.searchTaskTwoPages.pageNumber != i)
					SearchTaskResult.recycle();
			}
				
		}
			resetupChildren();
		}
	}

	@Override
	protected void onSettle(View v) {
		((PageView)v).addHq(true);
	}

	@Override
	protected void onUnsettle(View v) {
		((PageView)v).removeHq();
	}

	@Override
	protected void onNotInUse(View v) {
		((PageView)v).releaseResources();
	}

	/**
	 * @param linkString - url to open
	 */
	private void openLink(String linkString) {
		Log.d(TAG, "!openLink " + linkString);
		Uri uri = Uri.parse(linkString);
		
		String warect = null;
		if( uri.isHierarchical())
			warect = uri.getQueryParameter("warect");
		Boolean isFullScreen = warect != null && warect.equals("full");
		if(linkString.startsWith("http://localhost/")) {
			// display local content
			
			// get the current page view
			String path = uri.getPath();
			Log.d(TAG, "localhost path = " + path);
			if(path == null)
				return;
			
			if(path.endsWith("jpg") || path.endsWith("png") || path.endsWith("bmp")) {
				// start image slideshow
				Intent intent = new Intent(getContext(), SlideShowActivity.class);
				intent.putExtra("path", path);
				intent.putExtra("uri", linkString);
				Log.d(TAG,"basePath = "+path+"\nuri = "+ linkString);
				//startActivity(intent);
			}
			if(path.endsWith("mp4") && isFullScreen) {
				// start a video player
				//Uri videoUri = Uri.parse("file://" + getStoragePath() + "/wind_355" + path);
				//Intent intent = new Intent(Intent.ACTION_VIEW, videoUri);
				//startActivity(intent);
			}
		/*} else if(linkString.startsWith("buy://localhost")) {
			onBuy(uri.getPath().substring(1));*/
		} 
		else if(linkString.startsWith("http://"))
		{
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			getContext().startActivity(intent);
		}
		else {
			//TODO: replace with custom activity
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+linkString));
			getContext().startActivity(intent);
		}
		
	}
	
	@Override
	protected void onChildSetup(int i, View v) {
		// TODO Auto-generated method stub
		if( MuPDFActivity.currentViewMode == CurlView.SHOW_ONE_PAGE)
		{
			if (SearchTaskResult.get() != null
					&& SearchTaskResult.get().pageNumber == i)
				((PageView) v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
			else
				((PageView) v).setSearchBoxes(null);
		}
		else
		{
			if( MuPDFActivity.searchTaskTwoPages != null )
			{
			//	Log.i("---------------------------------------------:"+MuPDFActivity.searchTaskTwoPages.pageNumber, "set search view:"+i);
				if (MuPDFActivity.searchTaskTwoPages != null
						&& MuPDFActivity.searchTaskTwoPages.pageNumber == i)
				{
				//	Log.i("---------------------------------------------", "set search view");
					((PageView) v).setSearchBoxes(MuPDFActivity.searchTaskTwoPages.searchBoxes);
				}
				else
					((PageView) v).setSearchBoxes(null);
			}
			else
				((PageView) v).setSearchBoxes(null);
			
		}

	}
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		//Log.d(TAG, "SCROLL   "+ mScale);
		if( mScale == 1.0f && MuPDFActivity.useEffectPage )
		{
			return false;
		}
		return super.onScroll(e1, e2, distanceX, distanceY);
	}
}
