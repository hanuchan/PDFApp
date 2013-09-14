package com.appgeneration.magmanager.ui.fragment;

/**
 * 
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.appgeneration.magmanager.imagefetcher.ImageCache;
import com.appgeneration.magmanager.imagefetcher.ImageCache.ImageCacheParams;
import com.appgeneration.magmanager.imagefetcher.ImageFetcher;
import com.appgeneration.magmanager.interfaces.IssueChangesHandler;
import com.appgeneration.magmanager.interfaces.IssueDownloadHandler;
import com.appgeneration.magmanager.interfaces.IssuePurchaseHandler;
import com.appgeneration.magmanager.interfaces.IssueUIHandler;
import com.appgeneration.magmanager.interfaces.IssueUncompressHandler;
import com.appgeneration.magmanager.interfaces.MagazineChangesHandler;
import com.appgeneration.magmanager.interfaces.MainActivityInterface;
import com.appgeneration.magmanager.library.R;
import com.appgeneration.magmanager.model.DocumentStatus;
import com.appgeneration.magmanager.model.Issue;
import com.appgeneration.magmanager.model.ItemStatus;
import com.appgeneration.magmanager.model.Magazine;
import com.appgeneration.magmanager.services.DataManager;
import com.appgeneration.magmanager.services.DataManager.LocalBinder;
import com.appgeneration.magmanager.tracking.AnalyticsTracking;
import com.appgeneration.magmanager.ui.adapter.IssueCellViewHolder;
import com.appgeneration.magmanager.ui.adapter.IssueGridAdapter;
import com.appgeneration.magmanager.util.FileUtils;
import com.appgeneration.magmanager.util.GlobalSettings;

import com.crashlytics.android.Crashlytics;
import com.library.activity.MuPDFActivity;

/**
 * @author miguelferreira
 * 
 */
public class StoreIssueGridFragment extends Fragment implements
		IssueChangesHandler, MagazineChangesHandler, IssueDownloadHandler,
		IssueUncompressHandler, IssuePurchaseHandler, IssueUIHandler {

	// tag
	private final String TAG = "StoreIssueGridFragment";

	// image fetcher
	private ImageFetcher mImageFetcher;
	private final String IMAGE_CACHE_DIR = "thumbs";

	// grid adapter
	private IssueGridAdapter mAdapter;

	// grid
	private GridView mGridView;
	boolean userHasFlinged = false;

	// data
	private List<Issue> rawIssues;
	private List<Issue> filteredIssues;
	private Magazine magazine;

	// service-binding
	DataManager mService;
	boolean mBound = false;
	Intent dataManagerServiceIntent = null;
	private boolean iWantedToRefreshToTheServiceWasNotConnected = false;

	// store view
	View storeView;

	// cell size
	private int cellSize = -1;
	private int cellSpacing = -1;

	// issue download
	private long issueCurrentlyBeingDownloadedId = -1;
	private int issueCurrentlyBeingDownloadedGridPosition = -1;
	private Issue issueCurrentlyBeingDownloaded = null;
	private View issueCurrentlyBeingDownloadedView = null;
	private HashMap<Long, Integer> issueLastKnowProgress = new HashMap<Long, Integer>();

	// store activities broadcast
	private BroadcastReceiver refreshBroadcastReceiver = null;
	private BroadcastReceiver deleteBroadcastReceiver = null;
	private BroadcastReceiver editBroadcastReceiver = null;

	private boolean isUserViewingStore = true;
	private BroadcastReceiver isUserViewingStoreBroadcastReceiver = null;
//// 
	private final boolean clearImageCacheWhenUnused = true; /// nhi add to free memory
////
	/**
	 * LIFECYCLE
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(false);
		Crashlytics.setBool("isViewingStore", true);
		isUserViewingStoreBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Crashlytics.setBool("isViewingStore", isUserViewingStore);
				setUserViewingStore(!isUserViewingStore);
				onIssuesChanged(null, true);
			}
		};
		cellSize = getResources()
				.getDimensionPixelSize(R.dimen.issue_cell_size);
		cellSpacing = getResources().getDimensionPixelSize(
				R.dimen.issue_cell_spacing);

		mImageFetcher = new ImageFetcher(getActivity());
		ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(
				getActivity(), IMAGE_CACHE_DIR);
		cacheParams.setMemCacheSizePercent(getActivity(), 0.25f);
		mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(),
				cacheParams);

		rawIssues = new ArrayList<Issue>();
		filteredIssues = new ArrayList<Issue>();
		mAdapter = new IssueGridAdapter(getActivity(), filteredIssues,
				mImageFetcher, this);
		dataManagerServiceIntent = new Intent(getActivity(), DataManager.class);

		// load issue's last known progress
		loadIssuesLastKnownProgressFromSharedPreferences();

		// bind to service
		getActivity().bindService(dataManagerServiceIntent, mConnection,
				Context.BIND_AUTO_CREATE);

		// listen to refreshes broadcast
		refreshBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if (mBound) {
					Toast.makeText(getActivity(),
							getResources().getString(R.string.refreshing),
							Toast.LENGTH_SHORT).show();
					Log.i(TAG, "Refreshing data");
					mService.refresh();
				}
			}
		};

		// listen to delete broadcasts

		deleteBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// deletes the issue (the confirmation was handled by the store
				// activity)
				Issue issueToBeDeleted = getIssueForIssueId((intent
						.getLongExtra(GlobalSettings.BROADCAST_DELETE_ID_EXTRA,
								-1)));

				if (issueToBeDeleted == null) {
					// just ignore
					return;
				}

				deleteIssue(issueToBeDeleted);

			}
		};

		editBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				GlobalSettings.isUserInEditMode = !GlobalSettings.isUserInEditMode;
				mAdapter.notifyDataSetChanged();

			}
		};

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				refreshBroadcastReceiver,
				new IntentFilter(GlobalSettings.BROADCAST_REFRESH_ACTION));

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				deleteBroadcastReceiver,
				new IntentFilter(GlobalSettings.BROADCAST_DELETE_ACTION));

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				editBroadcastReceiver,
				new IntentFilter(GlobalSettings.BROADCAST_EDIT_ACTION));

	}

	private void deleteIssue(Issue issueToBeDeleted) {
		// analytics tracking
		AnalyticsTracking.trackIssueDelete(issueToBeDeleted);

		if (FileUtils.deleteIssueContents(issueToBeDeleted, getActivity())) {
			Toast.makeText(getActivity(), R.string.issue_delete_success,
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getActivity(), R.string.issue_delete_failure,
					Toast.LENGTH_LONG).show();
		}

		issueToBeDeleted.setDocumentStatus(DocumentStatus.AVAILABLE);
		if (mAdapter != null) {
//			mAdapter.onIssueDeleted(getViewForPosition(getGridPositionForIssueId(issueToBeDeleted
//					.getId())));
			onIssuesChanged(null, true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Fragment#onCreateView(android.view.LayoutInflater,
	 * android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		storeView = inflater.inflate(R.layout.store, container, false);
		mGridView = (GridView) storeView
				.findViewById(R.id.magazine_store_grid_view);
		getmGridView().setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();
		getmGridView().setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView absListView,
					int scrollState) {
				// Pause fetcher to ensure smoother scrolling when flinging
				if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
					userIsFlinging();
					mImageFetcher.setPauseWork(true);
				} else {
					userIsNotFlinging();
					mImageFetcher.setPauseWork(false);
				}

			}

			@Override
			public void onScroll(AbsListView absListView, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
			}
		});

		// This listener is used to get the final width of the GridView and then
		// calculate the
		// number of columns and the width of each column. The width of each
		// column is variable
		// as the GridView has stretchMode=columnWidth. The column width is used
		// to set the height
		// of each view so we get nice square thumbnails.
		// mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
		// new ViewTreeObserver.OnGlobalLayoutListener() {
		// @Override
		// public void onGlobalLayout() {
		// if (mAdapter.getNumColumns() == 0) {
		// final int numColumns = (int) Math.floor(mGridView
		// .getWidth() / (cellSize + cellSpacing));
		// if (numColumns > 0) {
		// final int columnWidth = (mGridView.getWidth() / numColumns)
		// - cellSpacing;
		// mAdapter.setNumColumns(numColumns);
		// mAdapter.setItemHeight(columnWidth);
		// Log.d("", "onCreateView - numColumns set to "
		// + numColumns);
		// }
		// }
		// }
		// });

		// banners

		return storeView;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onStart()
	 */
	@Override
	public void onStart() {
		Log.i(TAG, "Store onStart()");
		if(clearImageCacheWhenUnused) //nhi add for free memory
		{
			if( mImageFetcher == null)
			{
				Log.i("mImageFetcher:"+mImageFetcher, TAG +"                 ; onStart");
				mImageFetcher = new ImageFetcher(getActivity());
				ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(
						getActivity(), IMAGE_CACHE_DIR);
				cacheParams.setMemCacheSizePercent(getActivity(), 0.25f);
				mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(),
						cacheParams);
				mAdapter = new IssueGridAdapter(getActivity(), filteredIssues,
						mImageFetcher, this);
				mAdapter.notifyDataSetChanged();
			}
		}
		if (mService != null) {
			mService.refresh();
			Log.i(TAG, "Calling service refresh!");
		} else {
			iWantedToRefreshToTheServiceWasNotConnected = true;
		}

		super.onStart();
	}

	private void userIsFlinging() {
		userHasFlinged = true;

	}

	private void userIsNotFlinging() {
		userHasFlinged = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.appgeneration.magmanager.interfaces.IssueUIHandler#
	 * onIssueActionClickedForIssue(com.appgeneration.magmanager.model.Issue)
	 */
	@Override
	public void onIssueCoverClickedForIssue(Issue clickedIssue) {
		Log.i("", "onItemClickedAtPosition(+" + clickedIssue.getId() + ")");
		DocumentStatus documentStatus = clickedIssue.getDocumentStatus();

		if (GlobalSettings.isUserInEditMode && clickedIssue.isReadyToBeRead()) {
			((MainActivityInterface) getActivity())
					.setIssueIdBeingDeleted(clickedIssue.getId());
			DeletionConfirmationDialogFragment newFragment = DeletionConfirmationDialogFragment
					.newInstance(R.string.alert_dialog_delete_issues_text,
							(MainActivityInterface) getActivity());

			newFragment.show(getActivity().getSupportFragmentManager(),
					"dialog");
			getActivity().getSupportFragmentManager().popBackStack();
		} else if ((documentStatus == DocumentStatus.DOWNLOADING || documentStatus == DocumentStatus.PAUSED)
				|| clickedIssue.isReadyToBeRead()) {
			onIssueActionClickedForIssue(clickedIssue);
			return;
		} else {
			if (GlobalSettings.isUserInEditMode) {
				GlobalSettings.isUserInEditMode = false;
				mAdapter.notifyDataSetChanged();
			}

			((MainActivityInterface) getActivity()).previewIssue(clickedIssue);
		}

		return;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.appgeneration.magmanager.interfaces.IssueUIHandler#
	 * onIssueActionClickedForIssue(com.appgeneration.magmanager.model.Issue)
	 */
	@Override
	public void onIssueActionClickedForIssue(Issue clickedIssue) {
		
		
		if (clickedIssue.getDocumentStatus() == DocumentStatus.READY) {

			// close image cache
			// mImageFetcher.closeCache();

			// analytics tracking
			AnalyticsTracking.trackIssueRead(clickedIssue);
			Crashlytics.log("Store.click: opening book "+clickedIssue.getId());
			openIssueAccordingToItsFileType(clickedIssue);
			return;
		} else if (clickedIssue.isAvailableToDownloadActions()) {
			issueCurrentlyBeingDownloadedView = null;
			mService.startOrPauseIssueDownload(clickedIssue, this, true);
			return;
		} else if (clickedIssue.getDocumentStatus() == DocumentStatus.COMPRESSED) {
			mService.startUncompressingIssue(clickedIssue, this);
			return;
		}

		if (clickedIssue.isPaid() && !clickedIssue.isPriceReady()) {
			Crashlytics.log("Store.click: Book "+clickedIssue.getId()+"'s price still loading");
			Toast.makeText(getActivity(), R.string.issue_price_still_loading,
					Toast.LENGTH_LONG).show();
			return;

		}

		if (clickedIssue.isPaid() && !clickedIssue.isPriceAvaible()) {
			Crashlytics.log("Store.click: Book "+clickedIssue.getId()+"'s price unavailable");
			Toast.makeText(getActivity(), R.string.issue_price_unavailable,
					Toast.LENGTH_LONG).show();
			return;
		}

		if (clickedIssue.isPaid() && clickedIssue.isPriceReady()
				&& !clickedIssue.isCurrentlyBeingDownloadedExtractedOrExists()) {
			Crashlytics.log("Store.click: Buying book "+clickedIssue.getId());
			// start purchase process
			mService.startPurchaseFlowForIssue(getActivity(), this,
					clickedIssue);

			return;
		}

		// issue is in an invalid state, do nothing
		Log.i("A", "Invalid state");

	}
	
	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		return mService.handleActivityResult(requestCode, resultCode, data);
	}
	/*
	 * Helpers
	 */

	private void openIssueAccordingToItsFileType(Issue issueToBeOpened) {
		Intent issueReadIntent = null;
		// save issues last known progress
		saveIssuesLastKnownProgressFromSharedPreferences();

		if (FileUtils.isIssueAPDFFile(issueToBeOpened)) {
			issueReadIntent = getIntentForOpeningIssueAsAPDF(issueToBeOpened);
		}else {
			throw new AssertionFailedError("Unknown file type "
					+ issueToBeOpened.getFileType());
		}

		if (issueReadIntent != null) {
			startActivity(issueReadIntent);
		}
	}

	private Intent getIntentForOpeningIssueAsAPDF(Issue pdfIssueToBeOpened) {
		Intent readIntent = new Intent(getActivity(), com.library.activity.MuPDFActivity.class);
		readIntent.setAction(Intent.ACTION_VIEW);
		readIntent.setData(Uri.fromFile(FileUtils.getIssueFileCompletedPath(
				pdfIssueToBeOpened, getActivity())));

		return readIntent;
	}

	@Override
	public void onResume() {
		Crashlytics.log("Store.flow: onResume");
		super.onResume();
		mImageFetcher.setExitTasksEarly(false);
		mAdapter.notifyDataSetChanged();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				isUserViewingStoreBroadcastReceiver,
				new IntentFilter("storeStatedChanged"));
	}

	@Override
	public void onPause() {
		Crashlytics.log("Store.flow: onPause");
		super.onPause();
		if( clearImageCacheWhenUnused ) //nhi add for free memory
		{
			if( mImageFetcher!= null )
			{
				mImageFetcher.setExitTasksEarly(true);
				mImageFetcher.flushCache();
			}
		}
		else
		{
			mImageFetcher.setExitTasksEarly(true);
			mImageFetcher.flushCache();
		}
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(
				isUserViewingStoreBroadcastReceiver);
	}

	@Override
	public void onDestroy() {
		Crashlytics.log("Store.flow: onDestroy");
		if( clearImageCacheWhenUnused ) //nhi add for free memory
		{
			if( mImageFetcher!= null )
			{
				mImageFetcher.flushCache();
				mImageFetcher.closeCache();
				mImageFetcher = null;
			}
		}
		else
		{
			mImageFetcher.flushCache();
			mImageFetcher.closeCache();
			mImageFetcher = null;
		}
		if (issueCurrentlyBeingDownloadedId != -1) {
			mService.stopTrackingIssueDownload(issueCurrentlyBeingDownloadedId);
		}

		// save issues last known progress
		saveIssuesLastKnownProgressFromSharedPreferences();

		// unregister receivers
		if (refreshBroadcastReceiver != null) {
			LocalBroadcastManager.getInstance(getActivity())
					.unregisterReceiver(refreshBroadcastReceiver);
			refreshBroadcastReceiver = null;
		}

		if (deleteBroadcastReceiver != null) {
			LocalBroadcastManager.getInstance(getActivity())
					.unregisterReceiver(deleteBroadcastReceiver);
			deleteBroadcastReceiver = null;
		}

		// Unbind from the service
		if (mBound) {
			getActivity().unbindService(mConnection);
			mBound = false;
		}

		super.onDestroy();
	}

	/**
	 * SERVICE BINDING
	 */

	/**
	 * Called when the service binding is complete, register this fragment as a
	 * listener for issue changes
	 */
	private void onServiceBound() {
		Crashlytics.log("Store.flow: bound to datamanager");
		mBound = true;
		// register for issue and magazine changes
		mService.registerForIssuesChanges(this);
		mService.registerForMagazineChanges(this);
		mService.initInAppPurchasing(getActivity());

		// if there are any issues that are being downloaded, tell the service
		// to update their download handler

		if (iWantedToRefreshToTheServiceWasNotConnected) {
			Log.i(TAG, "Refreshing for the second try");
			mService.refresh();
			iWantedToRefreshToTheServiceWasNotConnected = false;
		}
	}

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
			onServiceBound();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			mService = null;
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.appgeneration.magmanager.interfaces.IssueChangesHandler#issuesChanged
	 * (java.util.List)
	 */
	@Override
	public void onIssuesChanged(List<Issue> currentIssues,
			boolean areIssuesLocal) {
		Log.i("", "OnIssuesChanged");
		if (currentIssues!=null) {
			rawIssues.clear();
			rawIssues.addAll(currentIssues);
		}
		filterIssues();

		for (Issue issue : rawIssues) {
			issue.setDownloadProgress(getLasKnownProgressForIssueId(issue
					.getId()));
		}

		if (!areIssuesLocal) {

			for (Issue issue : rawIssues) {
				if (issueCurrentlyBeingDownloadedId == -1) {
					if (issue.getDocumentStatus() == DocumentStatus.PAUSED) {
						mService.startOrPauseIssueDownload(issue, this, false);
					}
				}

				if (issue.getDocumentStatus() == DocumentStatus.UNCOMPRESSING
						|| issue.getDocumentStatus() == DocumentStatus.COMPRESSED) {
					mService.startUncompressingIssue(issue, this);
				}
			}

		}
	}

	private void filterIssues() {
		filteredIssues.clear();
// nhi rem to show all list item		
		// if (isUserViewingStore) {
			// for (Issue issueBeingFiltered : rawIssues) {
				// if (!issueBeingFiltered
						// .isCurrentlyBeingDownloadedExtractedOrExists()) {
					// filteredIssues.add(issueBeingFiltered);
				// }
			// }
		// } else {
			// for (Issue issueBeingFiltered : rawIssues) {
				// if (issueBeingFiltered
						// .isCurrentlyBeingDownloadedExtractedOrExists()) {
					// filteredIssues.add(issueBeingFiltered);
				// }
			// }
		// }
//end rem		
// show all list item
		for (Issue issueBeingFiltered : rawIssues) 
		{
				//if (issueBeingFiltered
				//		.isCurrentlyBeingDownloadedExtractedOrExists()) {
					filteredIssues.add(issueBeingFiltered);
			//	}
		}
//end		
		mAdapter.notifyDataSetChanged();

//		if (filteredIssues.isEmpty() && isUserViewingStore) {
//			Toast.makeText(getActivity(), getResources().getString(R.string.store_no_books_available), Toast.LENGTH_SHORT).show();
//		}
	}

	@Override
	public void onIssuesNotLoadedDueToNoInternetConnection() {
		//show dialog instead
		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        
        alert.setTitle(R.string.problem );
        alert.setMessage( R.string.issue_remote_load_internet_problem );
        alert.setCancelable(false);
        alert.setPositiveButton( android.R.string.ok,
                                          new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface _dialog, int _which )
            {
               
            }
        } );
            
        alert.show();
//		Toast.makeText(
//				getActivity(),
//				getResources().getString(
//						R.string.issue_remote_load_internet_problem),
//				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onIssuesNotLoadedDueToServerResponseError() {
		Toast.makeText(
				getActivity(),
				getResources().getString(
						R.string.issue_remote_load_server_problem),
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onIssuesNotLoadedDueToUnknownError() {
		Toast.makeText(
				getActivity(),
				getResources().getString(
						R.string.issue_remote_load_unknow_problem),
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void magazineChanged(Magazine currentMagazine) {
		magazine = currentMagazine;

		// load banners
		//refreshBanners();
	}

	/**
	 * BANNERS
	 */

	/**
	 * Refreshes all banners's images
	 */
	private void refreshBanners() {
		// TODO refresh all sides
		ImageView leftBannerImageView = (ImageView) storeView
				.findViewById(R.id.magazine_store_banner_left);
		String bannerLeft = magazine.getBannerLeft();
		if (bannerLeft != null && bannerLeft != "null") {

			leftBannerImageView.setVisibility(ViewGroup.VISIBLE);
			Log.i("", "Loading " + magazine.getBannerLeft());
			mImageFetcher.loadImage(magazine.getBannerLeft(),
					leftBannerImageView, R.drawable.cover_placeholder);
		} else {
			leftBannerImageView.setVisibility(ViewGroup.GONE);
		}
	}

	/**
	 * ISSUE UNCOMPRESS CALLBACKS
	 */

	@Override
	public void onUncompressStarted(long issueId) {
		Log.i("", "onUncompressStarted(" + issueId + ")");
		int gridPositionForIssueUncompressedStarted = getGridPositionForIssueId(issueId);
		if (gridPositionForIssueUncompressedStarted > -1) {
			Issue issueBeingDecompressed = filteredIssues
					.get(gridPositionForIssueUncompressedStarted);
			issueBeingDecompressed
					.setDocumentStatus(DocumentStatus.UNCOMPRESSING);
//			View issueBeingDecompressedView = getViewForPosition(gridPositionForIssueUncompressedStarted);
//			mAdapter.onUncompressStarted(issueBeingDecompressedView);
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onUncompressFinished(long issueId) {
		Log.i("", "onUncompressFinished(" + issueId + ")");
		int gridPositionForIssueUncompressedFinished = getGridPositionForIssueId(issueId);
		if (gridPositionForIssueUncompressedFinished > -1) {
			Issue issueDecompressed = filteredIssues
					.get(gridPositionForIssueUncompressedFinished);
			issueDecompressed.setDocumentStatus(DocumentStatus.READY);
//			View issueDecompressedView = getViewForPosition(gridPositionForIssueUncompressedFinished);
//			mAdapter.onUncompressFinished(issueDecompressedView);
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onUncompressFailed(long issueId) {
		Log.i("", "onUncompressFailed(" + issueId + ")");

		// TODO devise a better solution
		Toast.makeText(getActivity(), "Failed to uncompress issue " + issueId,
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onUncompressNotStartedDueToCongestion(long issueId) {
		Log.i("", "onUncompressNotStartedDueToCongestion(" + issueId + ")");

		// TODO devise a better solution
		Toast.makeText(getActivity(),
				"Uncompression not started for issue " + issueId,
				Toast.LENGTH_SHORT).show();
	}

	/**
	 * ISSUE DOWNLOAD CALLBACKS
	 */

	@Override
	public void onDownloadQueued(long issueId) {
		Log.i("", "onDownloadQueued(" + issueId + ")");
		int gridPositionForIssueQueued = getGridPositionForIssueId(issueId);
		if (gridPositionForIssueQueued > -1) {
			Issue issueQueued = filteredIssues.get(gridPositionForIssueQueued);
			issueQueued.setDocumentStatus(DocumentStatus.QUEUED);
//			View issueQueuedView = getViewForPosition(gridPositionForIssueQueued);
//			if (issueQueuedView != null) {
//				mAdapter.onDownloadQueued(issueQueuedView);
//			}
			mAdapter.notifyDataSetChanged();
		}

	}

	@Override
	public void onDownloadStarted(long issueId) {
		Log.i("", "onDownloadStarted(" + issueId + ")");
		issueCurrentlyBeingDownloadedId = issueId;
		int gridPositionForIssueId = getGridPositionForIssueId(issueId);
		Log.i("","mAdapter.notifyDataSetChanged() :" +gridPositionForIssueId);
		if (gridPositionForIssueId > -1) {
			issueCurrentlyBeingDownloaded = filteredIssues
					.get(gridPositionForIssueId);
			issueCurrentlyBeingDownloaded
					.setDocumentStatus(DocumentStatus.DOWNLOADING);
			issueCurrentlyBeingDownloadedGridPosition = getGridPositionForIssueId(issueCurrentlyBeingDownloadedId);
//			View issueDownloadStartedView = getViewForPosition(issueCurrentlyBeingDownloadedGridPosition);
//			if (issueDownloadStartedView != null) {
//				mAdapter.onDownloadStarted(issueDownloadStartedView);
//			}
			onIssuesChanged(null, true);
		}

	}

	@Override
	public void onDownloadNotStartedDueToNoInternet(long issueId) {
		Log.i("", "onDownloadNotStartedDueToNoInternet(" + issueId + ")");
		Toast.makeText(
				getActivity(),
				"The download didn't start because there is no internet connection. Please turn it on and try again",
				Toast.LENGTH_LONG).show();
		View issueCancelledView = getCurrentDownloadingIssueView(issueCurrentlyBeingDownloadedGridPosition);
		if (issueCancelledView != null) {
			mAdapter.onDownloadNotStartedDueToNoInternet(issueCancelledView);
		}

	}

	@Override
	public void postDownloadProgress(int progress, long issueId) {
		// do nothing when the user has flinged to prevent hiccups
		if (userHasFlinged) {
			return;
		}
		
		issueCurrentlyBeingDownloadedId = issueId;
		issueCurrentlyBeingDownloadedGridPosition = getGridPositionForIssueId(issueId);
		if (issueCurrentlyBeingDownloadedGridPosition > -1) {
			issueCurrentlyBeingDownloaded = filteredIssues
					.get(issueCurrentlyBeingDownloadedGridPosition);
			issueCurrentlyBeingDownloaded
					.setDocumentStatus(DocumentStatus.DOWNLOADING);
			mAdapter.notifyDataSetChanged();
		}
		
		

		// store download progress in hash map
		if (issueCurrentlyBeingDownloaded != null) {
			issueLastKnowProgress.put(issueId, progress);
		}
		
		if (issueCurrentlyBeingDownloaded!=null) {
			issueCurrentlyBeingDownloaded.setDownloadProgress(progress);
			
			View currentIssueBeingDownloaded = getCurrentDownloadingIssueView(issueCurrentlyBeingDownloadedGridPosition);
			if (currentIssueBeingDownloaded != null) {
				mAdapter.postDownloadProgress(progress, currentIssueBeingDownloaded);
			}
		}
	}

	@Override
	public void onDownloadFinished(long issueId) {
		Log.i("", "onDownloadFinished(" + issueId + ")");

		// make sure to clear its progress from the hash map
		issueLastKnowProgress.remove(issueId);

		// if the document is a zip, uncompress it
		if (FileUtils.isIssueAZipFile(issueCurrentlyBeingDownloaded)) {
			issueCurrentlyBeingDownloaded
					.setDocumentStatus(DocumentStatus.UNCOMPRESSING);
			mService.startUncompressingIssue(issueCurrentlyBeingDownloaded,
					this);
			View currentIssueBeingDownloaded = getCurrentDownloadingIssueView(issueCurrentlyBeingDownloadedGridPosition);
			if (currentIssueBeingDownloaded != null) {
//				mAdapter.onUncompressStarted(currentIssueBeingDownloaded);
				mAdapter.notifyDataSetChanged();
			}

		} else {
			issueCurrentlyBeingDownloaded
					.setDocumentStatus(DocumentStatus.READY);
			View currentIssueBeingDownloaded = getCurrentDownloadingIssueView(issueCurrentlyBeingDownloadedGridPosition);
			if (currentIssueBeingDownloaded != null) {
				mAdapter.onDownloadFinished(currentIssueBeingDownloaded);
			}

		}
		issueCurrentlyBeingDownloadedId = -1;
	}

	@Override
	public void onDownloadFailed(long issueId) {
		Log.i("", "onDownloadFailed(" + issueId + ")");
		Toast.makeText(
				getActivity(),
				"Book failed to download due to connection error! Please make sure you are properly connected to the internet and try again",
				Toast.LENGTH_LONG).show();

		issueCurrentlyBeingDownloaded.setDocumentStatus(DocumentStatus.PAUSED);

//		View issueCancelledView = getCurrentDownloadingIssueView(issueCurrentlyBeingDownloadedGridPosition);
//		if (issueCancelledView != null) {
//			mAdapter.onDownloadFailedOrPaused(issueCancelledView);
//		}
		mAdapter.notifyDataSetChanged();

	}

	@Override
	public void onDownloadCancelled(long issueId) {

		issueCurrentlyBeingDownloaded.setDocumentStatus(DocumentStatus.PAUSED);
//		View issueCancelled = getCurrentDownloadingIssueView(issueCurrentlyBeingDownloadedGridPosition);
//		if (issueCancelled != null) {
//			mAdapter.onDownloadFailedOrPaused(issueCancelled);
//		}
		mAdapter.notifyDataSetChanged();
	}

	/**
	 * ISSUE DOWNLOAD CALLBACK HELPERS
	 */

	/**
	 * Returns the issues list index for the given issue identifier
	 * 
	 * @param issueId
	 *            The issue's identifier
	 * @return An integer representing the position of the issue in the issues
	 *         list
	 */
	private int getGridPositionForIssueId(long issueId) {
		for (int i = 0; i < filteredIssues.size(); i++) {
			if (filteredIssues.get(i).getId() == issueId) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns a issue from an id
	 * 
	 * @param issueId
	 * @return
	 */
	private Issue getIssueForIssueId(long issueId) {
		for (int i = 0; i < filteredIssues.size(); i++) {
			if (filteredIssues.get(i).getId() == issueId) {
				return filteredIssues.get(i);
			}
		}
		return null;
	}

	private View getViewForPosition(int position) {
		if (position < 0) {
			return null;
		}
		final int numVisibleChildren = getmGridView().getChildCount();
		final int firstVisiblePosition = getmGridView()
				.getFirstVisiblePosition();

		for (int i = 0; i < numVisibleChildren; i++) {
			int positionOfView = firstVisiblePosition + i;

			if (positionOfView == position) {
				View view = getmGridView().getChildAt(i);
				return view;
			}
		}
		return null;
	}

	/**
	 * Helper method to cache the current view if no scroll is on progress (to
	 * prevent continuous redundant view searches)
	 * 
	 * @param position
	 * @return
	 */
	private View getCurrentDownloadingIssueView(int position) {

		if (position < 0) {
			return null;
		}

		if (issueCurrentlyBeingDownloadedView == null) {
			issueCurrentlyBeingDownloadedView = getViewForPosition(position);
			return issueCurrentlyBeingDownloadedView;
		}
		IssueCellViewHolder issueCellViewHolder = (IssueCellViewHolder) issueCurrentlyBeingDownloadedView
				.getTag();
		if (issueCellViewHolder.issuesListPosition != position) {
			issueCurrentlyBeingDownloadedView = getViewForPosition(position);
		}
		return issueCurrentlyBeingDownloadedView;
	}

	@Override
	public void onIssuePurchased(long issueId) {
		Log.i(TAG, "onIssuePurchased (" + issueId + ")");
		Toast.makeText(getActivity(), R.string.purchase_thank_you,
				Toast.LENGTH_LONG).show();
		Issue issuePurchased = getIssueForIssueId(issueId);
		issuePurchased.setItemStatus(ItemStatus.BOUGHT);
		View issuePurchasedView = getViewForPosition(getGridPositionForIssueId(issueId));
		mAdapter.onDownloadReadyToStart(issuePurchasedView);
	}

	@Override
	public void onIssueNotPurchasedDueToUserOption(long issueId) {
		Log.i(TAG, "onIssueNotPurchasedDueToUserOption (" + issueId + ")");
	}

	@Override
	public void onIssueNotPurchasedDueToFailure(long issueId) {
		Log.i(TAG, "onIssueNotPurchasedDueToFailure (" + issueId + ")");
		Toast.makeText(getActivity(), R.string.purchase_unknown_error,
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onIssueNotPurchasedDueToInvalidReponseKey(long issueId) {
		Log.i(TAG, "onIssueNotPurchasedDueToInvalidReponseKey (" + issueId
				+ ")");
	}

	@Override
	public void onIssueNotPurchasedDueToAPurchaseAlreadyInProgress(
			long currentIssueIdBeingBought) {
		Log.i(TAG, "onIssueNotPurchasedDueToAPurchaseAlreadyInProgress ("
				+ currentIssueIdBeingBought + ")");
	}

	/**
	 * @return the issueLastKnowProgress
	 */
	public int getLasKnownProgressForIssueId(long issueId) {
		return issueLastKnowProgress.containsKey(issueId) ? issueLastKnowProgress
				.get(issueId) : -1;
	}

	private void loadIssuesLastKnownProgressFromSharedPreferences() {
		SharedPreferences sharedPreferences = getActivity()
				.getSharedPreferences("name_icons_list", Context.MODE_PRIVATE);
		Map<String, ?> preferencesMap = sharedPreferences.getAll();
		for (String preferencesMapKey : preferencesMap.keySet()) {
			issueLastKnowProgress.put(Long.parseLong(preferencesMapKey),
					(Integer) preferencesMap.get(preferencesMapKey));
		}
	}

	private void saveIssuesLastKnownProgressFromSharedPreferences() {
		SharedPreferences sharedPreferences = getActivity()
				.getSharedPreferences("name_icons_list", Context.MODE_PRIVATE);
		Editor sharedPreferencesEditor = sharedPreferences.edit();

		for (Long issueId : issueLastKnowProgress.keySet()) {
			sharedPreferencesEditor.putInt(issueId.toString(),
					issueLastKnowProgress.get(issueId));
		}

		sharedPreferencesEditor.commit();
	}

	/**
	 * @return the mGridView
	 */
	public GridView getmGridView() {
		return mGridView;
	}

	/**
	 * @return the isUserViewingStore
	 */
	public boolean isUserViewingStore() {
		return isUserViewingStore;
	}

	/**
	 * @param isUserViewingStore
	 *            the isUserViewingStore to set
	 */
	public void setUserViewingStore(boolean isUserViewingStore) {
		this.isUserViewingStore = isUserViewingStore;
	}

}
