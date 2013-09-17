package com.library.activity;


import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;

import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.widget.ViewSwitcher;

import com.appgeneration.magmanager.library.R;
import com.artifex.mupdf.AsyncTask;
import com.artifex.mupdf.LinkInfo;
import com.artifex.mupdf.LinkInfoExternal;
import com.artifex.mupdf.MediaHolder;
import com.artifex.mupdf.MuPDFCore;
import com.artifex.mupdf.MuPDFPageAdapter;
import com.artifex.mupdf.MuPDFPageView;
import com.artifex.mupdf.PageView;

import com.artifex.mupdf.PDFPreviewPagerAdapter;
import com.artifex.mupdf.domain.OutlineActivityData;
import com.artifex.mupdf.domain.PDFParser;
import com.artifex.mupdf.domain.SearchTask;
import com.artifex.mupdf.domain.SearchTaskResult;
import com.artifex.mupdf.domain.TinySafeAsyncTask;
import com.artifex.mupdf.view.DocumentReaderView;
import com.artifex.mupdf.view.ReaderView;
import com.artifex.mupdf.view.ThumbnailViews;
import com.artifex.mupdf.view.ThumbnailViews.Orientation;



import com.pdf.effect.CurlPage;
import com.pdf.effect.CurlView;



//TODO: remove preffix mXXXX from all properties this class
public class MuPDFActivity extends BaseActivity{

	public static boolean flagSetBackgroundTransparent = false;
/** Flag for add all features **/	
	
	/**
	 * Clear static data on first create
	 */
	private final boolean clearStaticDataOnCreate = true;
	
	/**
	 *  Use thread to loading all data on create
	 */
	private final boolean useThreadToLoading = true;
	/**
	 * Show progress bar while loading in first screen 
	 */
	private final static boolean useProgressBarToLoading = true;
	/**
	 * Cache thumbnail view images to show fast when scrolling
	 */
	public static final boolean useImageThumbnailCacheList = true;
	/**
	 * Use curl effect when change page
	 */
	public final static boolean useEffectPage = true;
	/**
	 * Search text on all pages
	 */
	public static final boolean searchAllPages = true;
	
/** Flag for add all features **/		
	private FrameLayout mPreviewBarHolder;
	private ThumbnailViews mPreview;
	private PDFPreviewPagerAdapter pdfPreviewPagerAdapter;
	private Point mPreviewSize = null;
	public static List<Bitmap> listThumbnailBitmap = null;

/**
 * Show progress bar while loading in first screen 
 */
	public static ProgressDialog process;
	private static RelativeLayout progressBarLayout;
	ProgressBar mBusyIndicator;	
	private static int countView = 0;
/**
 * Search text on all pages
 */
	public static int[] countResultOnePage; // for search all page
	public static SearchTaskResult[] searchAllPageList;
	public static SearchTaskResult searchTaskTwoPages = null;

/**
 * Use curl effect when change page
 */
	public static int PHONE_WIDTH = 0;
	public static int PHONE_HEIGHT = 0;
	//static boolean sIsHandler = false;
	public static CurlView mCurlView;
	public static int currentViewMode;
	public static float marginWidth =0.0f;
	public static float marginHeight =0.0f;
/**
 * Main source
 */		
	private static final String TAG = "MuPDFActivity";
	private static final String FILE_NAME = "FileName";
	private MuPDFPageAdapter mDocViewAdapter;
	private SparseArray<LinkInfoExternal[]> linkOfDocument;
    private static final int START_BILLING_ACTIVITY = 100;
    private static final int START_OUTLINE_ACTIVITY = 101;	
    public static MuPDFCore core;
	public static String fileName;// fix bug wrong name cache image
	private int mOrientation;
	private boolean      buttonsVisible;
	public static boolean      mTopBarIsSearch;
	private SearchTask   mSearchTask;
	private AlertDialog.Builder alertBuilder;
	private static ReaderView   docView;
	private View         buttonsView;
	private EditText     mPasswordView;
	private TextView     mFilenameView;
	private ImageButton  mSearchButton;
	private ImageButton  mCancelButton;
	//private ImageButton  mOutlineButton;
	private ViewSwitcher mTopBarSwitcher;
	private ImageButton  mSearchBack;
	private ImageButton  mSearchFwd;
	private EditText     mSearchText;

	
////end add	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{

		super.onCreate(savedInstanceState);
		if( useProgressBarToLoading) //nhi add to use progressbar for show loading
		{

			progressBarLayout = new RelativeLayout(getApplicationContext());
			final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
			mBusyIndicator = new ProgressBar(getApplicationContext());
			mBusyIndicator.setBackgroundResource(R.drawable.busy);
			mBusyIndicator.setLayoutParams(layoutParams);
			mBusyIndicator.setIndeterminate(true);
			progressBarLayout.addView(mBusyIndicator);
	
			setContentView(progressBarLayout);
		} /// end add
		
		alertBuilder = new AlertDialog.Builder(this);
	
		core = getMuPdfCore(savedInstanceState);
	
		if (core == null) {
			return;
		}
	
		mOrientation = getResources().getConfiguration().orientation;

		if(mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			core.setDisplayPages(2);
		} else {
			core.setDisplayPages(1);
		}
		
		PHONE_WIDTH = getWindowManager().getDefaultDisplay().getWidth();
		PHONE_HEIGHT = getWindowManager().getDefaultDisplay().getHeight();
		
		if( clearStaticDataOnCreate && savedInstanceState == null)
		{
			//listThumbnailBitmap = null;
			countResultOnePage = null;
			searchAllPageList = null;
		}
		if(useThreadToLoading) // nhi add to use thread for loading data
		{
			ThumbnailAsyncTask task = new ThumbnailAsyncTask(this);
			task.execute(savedInstanceState);
			
		}
		else
		{
			
			if( useImageThumbnailCacheList ) // nhi add to cache thumbnail list image
			{
				
				if (mPreviewSize == null) {
					mPreviewSize = new Point();
					int padding = getResources().getDimensionPixelSize(
							R.dimen.page_preview_size);
					PointF mPageSize = core.getSinglePageSize(core.countDisplays()-1);
					float scale = mPageSize.y / mPageSize.x;
					mPreviewSize.x = (int) ((float) padding / scale);
					mPreviewSize.y = padding;
				}
				
				if( savedInstanceState == null)
				{
					listThumbnailBitmap = null;
					if( !useProgressBarToLoading ) //nhi add to use progressbar for show loading
						process = ProgressDialog.show(this, "", "");
					List<Bitmap> list = new ArrayList<Bitmap>();
					
					for(int i = 0; i < core.countSinglePages(); i++)
					{

						Bitmap lq = Bitmap.createBitmap(mPreviewSize.x, mPreviewSize.y,
								Bitmap.Config.ARGB_8888);
						core.drawSinglePage(i, lq, mPreviewSize.x, mPreviewSize.y);
						list.add(lq);
			
					}
					listThumbnailBitmap = list;
				}
			} // nhi end add
			createUI(savedInstanceState);
		}
	}
	
	private void requestPassword(final Bundle savedInstanceState) 
	{
		mPasswordView = new EditText(this);
		mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

		AlertDialog alert = alertBuilder.create();
		alert.setTitle(R.string.enter_password);
		alert.setView(mPasswordView);
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.ok),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (core.authenticatePassword(mPasswordView.getText().toString())) {
					createUI(savedInstanceState);
				} else {
					requestPassword(savedInstanceState);
				}
			}
		});
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		alert.show();
	}

	private MuPDFCore getMuPdfCore(Bundle savedInstanceState) {
		MuPDFCore core = null;
		if (core == null) {
			core = (MuPDFCore)getLastNonConfigurationInstance();

			if (savedInstanceState != null && savedInstanceState.containsKey(FILE_NAME)) {
				fileName = savedInstanceState.getString(FILE_NAME);
			}
		}
		if (core == null) {
			Intent intent = getIntent();
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				Uri uri = intent.getData();
				if (uri.toString().startsWith("content://")) {
					// Handle view requests from the Transformer Prime's file manager
					// Hopefully other file managers will use this same scheme, if not
					// using explicit paths.
					Cursor cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
					if (cursor.moveToFirst()) {
						uri = Uri.parse(cursor.getString(0));
					}
				}

				core = openFile(Uri.decode(uri.getEncodedPath()));
				SearchTaskResult.recycle();
			}
			if (core != null && core.needsPassword()) {
				requestPassword(savedInstanceState);
				return null;
			}
		}
		if (core == null) {
			AlertDialog alert = alertBuilder.create();
			
			alert.setTitle(R.string.open_failed);
			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			alert.show();
			return null;
		}
		return core;
	}
	private void createUI(Bundle savedInstanceState) {
		if (core == null)
			return;
		RelativeLayout layout = new RelativeLayout(this);
		// Reinstate last state if it was recorded
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		int orientation = prefs.getInt("orientation", mOrientation);
		int pageNum = prefs.getInt("page"+fileName, 0);
		int pageDisplay = pageNum;
		if( orientation != mOrientation)
		{
			if(orientation == Configuration.ORIENTATION_PORTRAIT) {
				pageDisplay = (pageNum + 1) / 2;
			} else {
				pageDisplay = (pageNum == 0) ? 0 : (pageNum * 2 - 1);
			}
		}
		
		if( useEffectPage) //add effect first to have time for loading effect
		{
			mCurlView = new CurlView(MuPDFActivity.this)
			{
					@Override
				protected void onTapMainDocArea() {
						// TODO Auto-generated method stub
						if (!buttonsVisible) {
							showButtons();
						} else {
							hideButtons();
						}
						Log.i(PageView.isDrawingNewPage+ ";11tap main doc area", "doc: "+docView.getVisibility()+"; curl:"+mCurlView.getVisibility());
						//if( !PageView.isDrawingNewPage)
							docView.setVisibility(View.VISIBLE);

					}
				
					@Override
					protected void hideMenu() {
						// TODO Auto-generated method stub
						hideButtons();
					}
				};
				
			mCurlView.setPageProvider(new PageProvider());
			mCurlView.setSizeChangedObserver(new SizeChangedObserver());
	
			mCurlView.setCurrentIndex(pageDisplay);
			
			mCurlView.setBackgroundColor(Color.TRANSPARENT);
			layout.addView(mCurlView);
		}
		// Now create the UI.
		// First create the document view making use of the ReaderView's internal
		// gesture recognition
		docView = new DocumentReaderView(MuPDFActivity.this, linkOfDocument) 
		{
			ActivateAutoLinks mLinksActivator = null;

	// for show effect		
			protected void onShowEffect( MotionEvent event) 
			{
				// TODO Auto-generated method stub
				if( !useEffectPage )
					return;
				if( mCurlView == null )
				{
					System.gc();
					mCurlView = new CurlView(MuPDFActivity.this)
					{
						@Override
						protected void onTapMainDocArea() {
							// TODO Auto-generated method stub
							if (!buttonsVisible) {
								showButtons();
							} else {
								hideButtons();
							}
							Log.i(PageView.isDrawingNewPage+ ";tap on main menu:"+ docView.getVisibility(), " curl: "+ mCurlView.getVisibility());
							//if( !PageView.isDrawingNewPage)
							{
								docView.setVisibility(View.VISIBLE);
								mCurlView.setVisibility(View.INVISIBLE);
							}
						}
					
						@Override
						protected void hideMenu() {
							// TODO Auto-generated method stub
							hideButtons();
						}
					};
					if( PHONE_WIDTH > PHONE_HEIGHT )
					{
						mCurlView.setViewMode(CurlView.SHOW_TWO_PAGES);
					}
					else
					{
						mCurlView.setViewMode(CurlView.SHOW_ONE_PAGE);
					}
					mCurlView.setPageProvider(new PageProvider());
					mCurlView.setSizeChangedObserver(new SizeChangedObserver());
					
					mCurlView.setBackgroundColor(Color.TRANSPARENT);
				}

				Log.i("show effect : doc: "+ docView.getVisibility(), "curl:"+mCurlView.getVisibility());
				mCurlView.setVisibility(View.VISIBLE);
				hideButtons();
				docView.setVisibility(View.INVISIBLE);
			}
	// end effect		
			@Override
			protected void onMoveToChild(View view, final int i) 
			{
				Log.d(TAG,"onMoveToChild id = "+i);

				if (core == null){
					return;
				}
				if( MuPDFActivity.useEffectPage && mCurlView!= null )
				{
					if( i != mCurlView.getCurrentIndex())///update curl view index
					{
						new Thread(new Runnable() {
	
							@Override
							public void run() {
								mCurlView.setCurrentIndex(i);
							}
						}).start(); 
						
						
					}
				}
			
				if( mTopBarIsSearch )
				{
					updateSearchBox(i , false);
				}
				MuPDFPageView pageView = (MuPDFPageView) docView.getDisplayedView();
				if(pageView!=null){
					pageView.cleanRunningLinkList();
				}
				super.onMoveToChild(view, i);
				if(mLinksActivator != null)
					mLinksActivator.cancel(true);
				mLinksActivator = new ActivateAutoLinks(pageView);
				mLinksActivator.safeExecute(i);
				setCurrentlyViewedPreview();
			
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				if (!isShowButtonsDisabled()) {
					hideButtons();
				}
				return super.onScroll(e1, e2, distanceX, distanceY);
			}

			@Override
			protected void onContextMenuClick() {
				if (!buttonsVisible) {
					showButtons();
				} else {
					hideButtons();
				}
			}

	
			@Override
			protected void invisibleEffectWhenZooming() {
				// TODO Auto-generated method stub
				if( !useEffectPage )
					return;
				if( mCurlView != null)
					mCurlView.setVisibility(View.GONE);

			}
		};
		mDocViewAdapter = new MuPDFPageAdapter(MuPDFActivity.this, core);
		docView.setAdapter(mDocViewAdapter);
		
		mSearchTask = new SearchTask(MuPDFActivity.this, core) {
			@Override
			protected void onTextFound(SearchTaskResult result) {
				
				if( searchAllPages )//nhi add for search all pages
				{
					hideKeyboard();
					updateThumbnailList(); // change background
					updateSearchBox(docView.getCurrentPage(), true); // show on current page
				}
				else
				{
					searchTaskTwoPages = null; // nhi add
					// Ask the ReaderView to move to the resulting page
					
					if( core.getDisplayPages() == 1)
					{
						SearchTaskResult.set(result);
						docView.setDisplayedViewIndex(result.pageNumber);
					}
					else
					{
						searchTaskTwoPages = result;
						docView.setDisplayedViewIndex(result.pageNumber);
					}
				}
				// Make the ReaderView act on the change to SearchTaskResult
				// via overridden onChildSetup method.
				docView.resetupChildren();
			}
		};

		// Make the buttons overlay, and store all its
		// controls in variables
		makeButtonsView();

		// Set the magazine title text
		String title = getIntent().getStringExtra("title");
		if (title != null) {
			mFilenameView.setText(title);
		} else {
			mFilenameView.setText(fileName);
		}
		
		/*if (core.hasOutline()) {
			mOutlineButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					OutlineItem outline[] = core.getOutline();
					if (outline != null) {
						OutlineActivityData.get().items = outline;
						Intent intent = new Intent(MuPDFActivity.this, OutlineActivity.class);
						startActivityForResult(intent, START_OUTLINE_ACTIVITY);
					}
				}
			});
		} else {
			mOutlineButton.setVisibility(View.GONE);
		}*/
/// add to search task
		// Activate the search-preparing button
		mSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOn();
				if( useEffectPage )
					mCurlView.setVisibility(View.INVISIBLE); // nhi add for search
			}
		});
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOff();
				System.gc(); //nhi add for search
				if( useEffectPage )
					mCurlView.setVisibility(View.VISIBLE);

			}
		});
		// Search invoking buttons are disabled while there is no text specified
		if( !MuPDFActivity.searchAllPages)//nhi add for search all pages
		{
			mSearchBack.setEnabled(false);
			mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
		}
		
		mSearchFwd.setEnabled(false);

		mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));

		// React to interaction with the text widget
		mSearchText.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				boolean haveText = s.toString().length() > 0;
				if( !MuPDFActivity.searchAllPages)//nhi add for search all pages
				{
					mSearchBack.setEnabled(haveText);
				}
				mSearchFwd.setEnabled(haveText);
				if (haveText) {
					if( !MuPDFActivity.searchAllPages)//nhi add for search all pages
						mSearchBack.setColorFilter(Color.argb(255, 255, 255, 255));
					mSearchFwd.setColorFilter(Color.argb(255, 255, 255, 255));
				} else {
					if( !MuPDFActivity.searchAllPages)//nhi add for search all pages
						mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
					mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));
				}

				// Remove any previous search results
				if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
					SearchTaskResult.recycle();
					docView.resetupChildren();
				}
			}
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}
		});

		//React to Done button on keyboard
		mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE)
					search(1);
				return false;
			}
		});

		mSearchText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
					search(1);
				return false;
			}
		});
		if( !MuPDFActivity.searchAllPages)//nhi add for search all pages
		{
			// Activate search invoking buttons
			mSearchBack.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					search(-1);
				}
			});
		}
		mSearchFwd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(1);
			}
		});

//end	
		// Stick the document view and the buttons overlay into a parent view
		/// nhi add for page effect		
		layout.addView(docView);
		if( useEffectPage )
		{
			
			docView.postDelayed(new Runnable() {
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							docView.setDisplayedViewIndex(mCurlView.getCurrentIndex());
						}
					});
				}
			}, 300);

		}
		else
			docView.setDisplayedViewIndex(pageDisplay);
	/// end page effect
	
	// Give preview thumbnails time to appear before showing bottom bar
		if (savedInstanceState == null
				|| !savedInstanceState.getBoolean("ButtonsHidden", false)) {
			mPreview.postDelayed(new Runnable() {
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if(!useProgressBarToLoading)//nhi add to use progressbar for show loading
								process.dismiss();
							showButtons();
						}
					});
				}
			}, 500);
		}
		
		
		//layout.addView(docView);
		layout.addView(buttonsView);

		layout.setBackgroundColor(Color.BLACK);
		
		setContentView(layout);
		
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == START_BILLING_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
		if (requestCode == START_OUTLINE_ACTIVITY && resultCode >= 0) {
			if (core.getDisplayPages() == 2) {
				resultCode = (resultCode + 1) / 2;
			}
			docView.setDisplayedViewIndex(resultCode);
        }
		super.onActivityResult(requestCode, resultCode, data);
	}

	public Object onRetainNonConfigurationInstance() {
		MuPDFCore mycore = core;
		core = null;
		return mycore;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (fileName != null && docView != null) {
			outState.putString("FileName", fileName);

			// Store current page in the prefs against the file name,
			// so that we can pick it up each time the file is loaded
			// Other info is needed only for screen-orientation change,
			// so it can go in the bundle
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("page"+fileName, docView.getDisplayedViewIndex());
			edit.putInt("orientation", mOrientation);
			edit.commit();
		}

		if (!buttonsVisible)
			outState.putBoolean("ButtonsHidden", true);

		if (mTopBarIsSearch)
			outState.putBoolean("SearchMode", true);
	}

	@Override
	protected void onPause() {

		super.onPause();

		if( mSearchTask != null)
			mSearchTask.stop(); // for search task
		if(searchTaskTwoPages!= null)
		{
			if( useEffectPage )
				mCurlView.setVisibility(View.INVISIBLE);
			searchTaskTwoPages = null; //nhi add for search
		}
		if( useEffectPage 
		  && getWindowManager().getDefaultDisplay().getWidth()!= PHONE_WIDTH )//crash when pause
		{
			if( useThreadToLoading && progressBarLayout == null)
			{
				mCurlView.clearData();
				mCurlView = null; 
			}
		}
		System.gc();

		if (fileName != null && docView != null) {
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("page"+fileName, docView.getDisplayedViewIndex());
			edit.putInt("orientation", mOrientation);
			edit.commit();
		}
	}
	
	@Override
	public void onDestroy() {

		if (core != null) {
			core.onDestroy();
		}
		core = null;

		super.onDestroy();
		System.gc();
	}

	void showButtons() {
		if (core == null) {
			return;
		}

		if (!buttonsVisible) {
			buttonsVisible = true;
			// Update page number text and slider

			Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mTopBarSwitcher.setVisibility(View.VISIBLE);
					mSearchButton.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {}
			});
			mTopBarSwitcher.startAnimation(anim);
			// Update listView position
			setCurrentlyViewedPreview();
			anim = new TranslateAnimation(0, 0, mPreviewBarHolder.getHeight(), 0);
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPreviewBarHolder.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {

				}
			});
			mPreviewBarHolder.startAnimation(anim);
            buttonsView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

			if (mTopBarIsSearch) {//nhi add for search all pages
				mSearchText.requestFocus();
			
				mTopBarSwitcher.showNext();
				showKeyboard();
			}

		}
	}

	void hideButtons() {
		if (buttonsVisible) {
			buttonsVisible = false;
			hideKeyboard();

			Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mTopBarSwitcher.setVisibility(View.INVISIBLE);
				}
			});
			mTopBarSwitcher.startAnimation(anim);
			
			anim = new TranslateAnimation(0, 0, 0, this.mPreviewBarHolder.getHeight());
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPreviewBarHolder.setVisibility(View.INVISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
				}
			});
			mPreviewBarHolder.startAnimation(anim);
            buttonsView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		}
	}

	void searchModeOn() {
		if (!mTopBarIsSearch) {
			mTopBarIsSearch = true;
			//Focus on EditTextWidget
			mSearchText.requestFocus();
			showKeyboard();
			mTopBarSwitcher.showNext();
			updateThumbnailList();//nhi add for search all pages
		}
	}

	void searchModeOff() {
		if (mTopBarIsSearch) {
			mTopBarIsSearch = false;
			hideKeyboard();
			mTopBarSwitcher.showPrevious();
			SearchTaskResult.recycle();
			// Make the ReaderView act on the change to mSearchTaskResult
			// via overridden onChildSetup method.
			searchTaskTwoPages = null;
			if( searchAllPages )//nhi add for search all pages
			{
				countResultOnePage = null;
				searchAllPageList = null;
			}
			updateThumbnailList();
			docView.resetupChildren();
		}
	}

	void makeButtonsView() {
		buttonsView = getLayoutInflater().inflate(R.layout.buttons,null);
		mFilenameView = (TextView)buttonsView.findViewById(R.id.docNameText);
		mPreviewBarHolder = (FrameLayout) buttonsView.findViewById(R.id.PreviewBarHolder);

		mPreview = new ThumbnailViews(MuPDFActivity.this);
		mPreview.setOrientation(Orientation.HORIZONTAL);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
		mPreview.setLayoutParams(lp);
		pdfPreviewPagerAdapter = new PDFPreviewPagerAdapter(MuPDFActivity.this, core);

		
		mPreview.setAdapter(pdfPreviewPagerAdapter);
		
		
		mPreview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> pArg0, View pArg1,
					int position, long id) {
				hideButtons();
				if( MuPDFActivity.mTopBarIsSearch && MuPDFActivity.searchAllPages)//nhi add for search all pages
				{
					updateSearchBox((int)id, true);
				}
				else
				{
					if( docView.getCurrentPage() != (int)id)
						docView.setDisplayedViewIndex((int)id);
				}
			}
		});
		mPreviewBarHolder.addView(mPreview);
	
		
		mSearchButton = (ImageButton)buttonsView.findViewById(R.id.searchButton);
		mCancelButton = (ImageButton)buttonsView.findViewById(R.id.cancel);
		//mOutlineButton = (ImageButton)buttonsView.findViewById(R.id.outlineButton);
		mTopBarSwitcher = (ViewSwitcher)buttonsView.findViewById(R.id.switcher);
		mSearchBack = (ImageButton)buttonsView.findViewById(R.id.searchBack);
		if( MuPDFActivity.searchAllPages)//nhi add for search all pages
			mSearchBack.setVisibility(View.GONE);
		mSearchFwd = (ImageButton)buttonsView.findViewById(R.id.searchForward);
		if( MuPDFActivity.searchAllPages)//nhi add for search all pages
		{
			mSearchFwd.setImageDrawable(getResources().getDrawable(R.drawable.ic_magnifying_glass));
		}
		mSearchText = (EditText)buttonsView.findViewById(R.id.searchText);
		mTopBarSwitcher.setVisibility(View.INVISIBLE);
		mPreviewBarHolder.setVisibility(View.INVISIBLE);
	}

	void showKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.showSoftInput(mSearchText, 0);
	}

	void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
	}



	void search(int direction) {
		System.gc();
		hideKeyboard();
		if (core == null)
			return;
		searchTaskTwoPages = null;
		if( searchAllPages )//nhi add for search all pages
		{
			countResultOnePage = null;
			searchAllPageList = null;
		}
		updateThumbnailList();
		int displayPage = docView.getDisplayedViewIndex();
		SearchTaskResult r = SearchTaskResult.get();
		int searchPage = r != null ? r.pageNumber : -1;
		mSearchTask.go(mSearchText.getText().toString(), direction, displayPage, searchPage);
	}

//	@Override
	public boolean onSearchRequested() {
		if (buttonsVisible && mTopBarIsSearch) {
			hideButtons();
		} else {
			showButtons();
			searchModeOn();
		}
		return super.onSearchRequested();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (buttonsVisible && !mTopBarIsSearch) {
			hideButtons();
		} else {
			showButtons();
			searchModeOff();
		}
		return super.onPrepareOptionsMenu(menu);
	}

	private MuPDFCore openFile(String path) {
		int lastSlashPos = path.lastIndexOf('/');
		fileName = new String(lastSlashPos == -1
					? path
					: path.substring(lastSlashPos+1));
		System.out.println("Trying to open "+path);
	
		PDFParser linkGetter = new PDFParser(path);
		linkOfDocument = linkGetter.getLinkInfo();

		try {
			core = new MuPDFCore(path);
			// New file: drop the old outline data
			OutlineActivityData.set(null);
		} catch (Exception e) {
			Log.e(TAG, "get core failed", e);
			return null;
		}
		return core;
	}



	private Context getContext() {
		return this;
	}

	private class ActivateAutoLinks extends TinySafeAsyncTask<Integer, Void, ArrayList<LinkInfoExternal>> {
		private MuPDFPageView pageView;// = (MuPDFPageView) docView.getDisplayedView();
		
		public ActivateAutoLinks(MuPDFPageView pParent) {
			pageView = pParent;
		}
		
		@Override
		protected ArrayList<LinkInfoExternal> doInBackground(Integer... params) {
			int page = params[0].intValue();
			//Log.d(TAG, "Page = " + page);
			if (null != core) {
				LinkInfo[] links = core.getPageLinks(page);
				if(null == links){
					return null;
				}
				ArrayList<LinkInfoExternal> autoLinks = new ArrayList<LinkInfoExternal>();
				for (LinkInfo link : links) {
					if(link instanceof LinkInfoExternal) {
						LinkInfoExternal currentLink = (LinkInfoExternal) link;
					
						if (null == currentLink.url) {
							continue;
						}
						Log.d(TAG, "checking link for autoplay: " + currentLink.url);
	
						if (currentLink.isMediaURI()) {
							if (currentLink.isAutoPlay()) {
								autoLinks.add(currentLink);
							}
						}
					}
				}
				return autoLinks;
			}
			return null;
		}

		@Override
		protected void onPostExecute(final ArrayList<LinkInfoExternal> autoLinks) {
			if (isCancelled() || autoLinks == null) {
				return;
			}
			docView.post(new Runnable() {
				public void run() {
					for(LinkInfoExternal link : autoLinks){
						if (pageView != null && null != core) {
							String basePath = core.getFileDirectory();
							MediaHolder mediaHolder = new MediaHolder(getContext(), link, basePath);
							pageView.addMediaHolder(mediaHolder, link.url);
							pageView.addView(mediaHolder);
							mediaHolder.setVisibility(View.VISIBLE);
							mediaHolder.requestLayout();
						}
					}
				}
			});
		}
	}
/**
 *  Use thread to loading all data on create
 */
	private class ThumbnailAsyncTask extends AsyncTask<Bundle, List<Bitmap>, List<Bitmap>>
	{
		private Context mContext;
		public ThumbnailAsyncTask(Context ctx)
		{
			mContext= ctx;
		}
		protected void onPreExecute() {
		
			if(PHONE_WIDTH > PHONE_HEIGHT )
			{
				MuPDFActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			}
			else
			{
				MuPDFActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
			if( !useProgressBarToLoading )
			{
				process = new ProgressDialog(mContext);
				process.show();
			}
			
		}
		
		@Override
		protected List<Bitmap> doInBackground(Bundle... params) 
		{
			final Bundle bd = (Bundle)params[0];
			// TODO Auto-generated method stub
			if( useImageThumbnailCacheList )
			{
			//	Log.i(" listThumbnailBitmap"+listThumbnailBitmap,"bd : "+bd);
				if( bd ==null || ( listThumbnailBitmap == null)) // fix bug crash after back from link active
				{
					listThumbnailBitmap = null;
					if (mPreviewSize == null) {
						mPreviewSize = new Point();
						int padding = getResources().getDimensionPixelSize(
								R.dimen.page_preview_size);
						PointF mPageSize = core.getSinglePageSize(core.countDisplays()-1);
						float scale = mPageSize.y / mPageSize.x;
						
						//Log.i("--------------------------image size : " + scale, " w : " +((float) padding / scale));
						mPreviewSize.x = (int) ((float) padding / scale);
						mPreviewSize.y = padding;
					}
					
					List<Bitmap> list = new ArrayList<Bitmap>();
					
					for(int i = 0; i < core.countSinglePages(); i++)
					{

						Bitmap lq = Bitmap.createBitmap(mPreviewSize.x, mPreviewSize.y,
								Bitmap.Config.ARGB_8888);
						core.drawSinglePage(i, lq, mPreviewSize.x, mPreviewSize.y);
						list.add(lq);
						
						
					}
					listThumbnailBitmap = list;
				
				}
			}
			MuPDFActivity.this.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					createUI(bd);
				}
			});
			
			return listThumbnailBitmap;
		}
		
		@Override
		protected void onPostExecute(List<Bitmap> result) {
			// TODO Auto-generated method stub
			if( !useProgressBarToLoading )
			{
				process.dismiss();
			}
			setCurrentlyViewedPreview();
			System.gc();
			if( !useEffectPage)
			MuPDFActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
		
	}
/**
 * Search text on all pages
 */	
	public void updateSearchBox( int index , boolean move)//nhi add for search all pages
	{

		searchTaskTwoPages = null; // nhi add
		// Ask the ReaderView to move to the resulting page
		if( countResultOnePage == null || searchAllPageList == null)
			return;
		if( core.getDisplayPages() == 1)
		{
			if( countResultOnePage[index]!=0)
			{
				SearchTaskResult.set(searchAllPageList[index]);
				
			}

		}
		else
		{
			int realPage = index*2;
			RectF searchHitsL[] = null;
			RectF searchHitsR[] = null;
		
			if( realPage < core.countSinglePages() && countResultOnePage[realPage] > 0 )
				searchHitsR = searchAllPageList[realPage].searchBoxes;
			if( realPage > 0 && countResultOnePage[realPage - 1] > 0 )
				searchHitsL = searchAllPageList[realPage - 1].searchBoxes;
	
			RectF searchHits[]=null;
			
			
			int l = searchHitsL != null?searchHitsL.length:0;
			int r = searchHitsR != null?searchHitsR.length:0;
			SearchTaskResult.lengthLeftBoxs = l;
			if( l +r >0)
			{
				searchHits = new RectF[l+r];
				for( int i = 0 ; i< l; i++)
				{
					searchHits[i] = searchHitsL[i];
				}
				for( int i = 0 ; i< r; i++)
				{
					searchHits[l+i] = searchHitsR[i];
				}
				searchTaskTwoPages = SearchTaskResult.init(mSearchText.getText().toString(), index, searchHits);
			}
			else
				searchTaskTwoPages = null;
			
		}
		if( move)
			docView.setDisplayedViewIndex(index);
	}
/**
 * Curl Page Methods.
 */	
	public static boolean getDocViewStatus()
	{
		return docView.getVisibility() == View.VISIBLE;
	}

	public static void enableDocView()
	{
		if( useProgressBarToLoading )
		{
			if( progressBarLayout != null )
			{
				countView++;
				if(countView >= 3)
				{

						progressBarLayout.setVisibility(View.GONE);
						progressBarLayout = null;
						docView.setVisibility(View.VISIBLE);
	
						
				}
			}
		}
		if( MuPDFActivity.useEffectPage)
		{
			if( useProgressBarToLoading && progressBarLayout == null)
				if(mCurlView!= null )
				{
					if(!mCurlView.isInAnimated())
					{
						Log.i("enableDocView : "+PageView.isDrawingNewPage, "doc : "+docView.getVisibility());
						docView.setVisibility(View.VISIBLE);
						
					}
				}
		}
		else
			docView.setVisibility(View.VISIBLE);
		
	}
	public static void invisibleDocView()
	{
	Log.i("invisibleDocView:"+ docView.getVisibility()+" ; curl: "+ mCurlView.getVisibility(), " scale: "+ docView.mScale);
		//if(docView.mScale ==1.0f)
			docView.setVisibility(View.INVISIBLE);
	}	  
	/**
	 * Handler for update docview index after curl page.
	 */

	public static Handler handler = new Handler()
	{
		public void handleMessage(Message msg) 
		{
			int mCurrentIndex = msg.getData().getInt("mCurrentIndex");
			if(mCurrentIndex >= 0 && docView.getCurrentPage() != mCurrentIndex)
			{
				//sIsHandler = true;
				Log.i("update doc index", "doc : "+docView.getVisibility());
				docView.setVisibility(View.INVISIBLE);
				flagSetBackgroundTransparent = true;
				docView.setDisplayedViewIndex(mCurrentIndex);
		
			}

		}
	};

	public Bitmap getBitmapPageView( int position, int width, int height)
	{

		Bitmap  bitmap = Bitmap.createBitmap(width,  height, Config.ARGB_8888);
		if( core != null)
			core.drawSinglePage( position, bitmap,width,  height);
		return bitmap;
	}
	/**
	 * CurlView size changed observer.
	 */

	public class SizeChangedObserver implements CurlView.SizeChangedObserver 
	{
		@Override
		public void onSizeChanged(int w, int h) 
		{
		
			PointF mPageSize = core.getSinglePageSize(0);

			float mSourceScale = Math.min(w/mPageSize.x, h/mPageSize.y);
			Point newSize = new Point((int)(mPageSize.x*mSourceScale), (int)(mPageSize.y*mSourceScale));
	
			if (w > h) 
			{
				mCurlView.setViewMode(CurlView.SHOW_TWO_PAGES);

				if( marginWidth == 0.0f )
					marginWidth = (w*1.0f - newSize.x*2.0f)/w*1.0f;
					
				mCurlView.setMargins(marginWidth/2.0f, .0f,  marginWidth/2.0f,.0f);

	
				currentViewMode = CurlView.SHOW_TWO_PAGES;
			} 
			else 
			{
				mCurlView.setViewMode(CurlView.SHOW_ONE_PAGE);

				if( marginHeight == 0.0f )
					marginHeight = (h*1.0f - newSize.y)/h*1.0f;
				mCurlView.setMargins(.0f, marginHeight/2.0f, .0f, marginHeight/2.0f);
			
				currentViewMode = CurlView.SHOW_ONE_PAGE;
			}
		}
	}
	/**
	 * Bitmap provider.
	 */
	/**
	 * @author YenNhi
	 *
	 */
	/**
	 * @author YenNhi
	 *
	 */
	private class PageProvider implements CurlView.PageProvider 
	{


		@Override
		public int getPageCount() {
			Log.i("get page count","onDestroy: "+ MuPDFCore.onDestroy);
			if( MuPDFCore.onDestroy )
				return -1;
			if( mCurlView == null )
			{
				Log.d("getPageCount:", "nulll");
				System.gc();
				MuPDFActivity.this.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						mCurlView = new CurlView(MuPDFActivity.this)
						{
							@Override
							protected void onTapMainDocArea() {
								// TODO Auto-generated method stub
								if (!buttonsVisible) {
									showButtons();
								} else {
									hideButtons();
								}
								Log.i(PageView.isDrawingNewPage+ "; onTapMainDocArea 1: "+ mCurlView.mState,"doc: "+ docView.getVisibility());
								//if( !PageView.isDrawingNewPage)
								{
									docView.setVisibility(View.VISIBLE);
									mCurlView.setVisibility(View.INVISIBLE);
								}	
							}
						
							@Override
							protected void hideMenu() {
								// TODO Auto-generated method stub
								hideButtons();
							}
						};
						if( PHONE_WIDTH > PHONE_HEIGHT )
						{
							mCurlView.setViewMode(CurlView.SHOW_TWO_PAGES);
						}
						else
						{
							mCurlView.setViewMode(CurlView.SHOW_ONE_PAGE);
						}
						mCurlView.setPageProvider(new PageProvider());
						mCurlView.setSizeChangedObserver(new SizeChangedObserver());
						
						mCurlView.setBackgroundColor(Color.TRANSPARENT);
						
					}
				});
				//if( mCurlView.getViewMode() == CurlView.SHOW_TWO_PAGES)
				if( core.getDisplayPages() == 2)
					return core.countPages()-1;
				return core.countPages();
			}
			else
			{
				//if( mCurlView.getViewMode() == CurlView.SHOW_TWO_PAGES)
				if( core.getDisplayPages() == 2)
					return core.countPages()-1;
				return core.countPages();
			}

		}

		private Bitmap loadBitmap(int width, int height, int index) {

			Bitmap b = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(b);
			b = getBitmapPageView( index, width, height );
			Paint p = new Paint();
			p.setColor(Color.TRANSPARENT);
			c.drawBitmap(b, 0, 0, p);
			return b;
		}
boolean firstUpdated = true;
		@Override
		public void updatePage(CurlPage page, int width, int height, int index) 
		{
			//Log.i(getPageCount()+ " ;update page : "+ index, " curl page : " + mCurlView.getCurrentIndex());
			if( mCurlView.getViewMode() == CurlView.SHOW_ONE_PAGE)
			{
				page.setTexture(loadBitmap(width, height, index), CurlPage.SIDE_FRONT);
				
				page.setColor(Color.rgb(180, 180, 180), CurlPage.SIDE_BACK);
				
			}
			else
			{
			
				if( index == 0 )
				{

					page.setTexture(loadBitmap(width, height, index), CurlPage.SIDE_FRONT);
					page.setTexture(loadBitmap(width, height, index+1), CurlPage.SIDE_BACK);
				}
				else
				{

					page.setTexture(loadBitmap(width, height, 2 * index), CurlPage.SIDE_FRONT);
					
					if( index == getPageCount() -1 && ( core.countSinglePages()%2 != 0))
					{
						page.setColor(Color.rgb(180, 180, 180), CurlPage.SIDE_BACK);
					}
					else
					{

						page.setTexture(loadBitmap(width, height, 2 * index + 1), CurlPage.SIDE_BACK);
					}
					
				}
			}
			if( firstUpdated )
			{
				MuPDFActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
				firstUpdated = false;
			}

		}

	}
/**
 * Thumbnail list
 */
	private void setCurrentlyViewedPreview() 
	{
		if( core == null )
			return;
		if( docView != null)
		{
			int i = docView.getDisplayedViewIndex();
			if (core.getDisplayPages() == 2) {
				i = (i * 2) - 1;
			}
			if(pdfPreviewPagerAdapter != null)
			{
				pdfPreviewPagerAdapter.setCurrentlyViewing(i);
				centerPreviewAtPosition(i);
			}
		}
	}

	public void centerPreviewAtPosition(int position) 
	{

		if (mPreview.getChildCount() > 0) 
		{
			View child = mPreview.getChildAt(0);
			// assume all children the same width
			int childMeasuredWidth = child.getMeasuredWidth();

			if (childMeasuredWidth > 0) 
			{
				if (core.getDisplayPages() == 2) 
				{
					mPreview.setSelectionFromOffset(position,
							(mPreview.getWidth() / 2) - (childMeasuredWidth));
				} 
				else 
				{
					mPreview.setSelectionFromOffset(position,
							(mPreview.getWidth() / 2)
									- (childMeasuredWidth / 2));
				}
			} 
			else 
			{
				//Log.e("centerOnPosition", "childMeasuredWidth = 0");
			}
		} 
		else 
		{
			//Log.e("centerOnPosition", "childcount = 0");
		}
	}
	
	protected void updateThumbnailList() {//nhi add for search all pages
		// TODO Auto-generated method stub
		pdfPreviewPagerAdapter.notifyDataSetChanged();
	}
	

}
