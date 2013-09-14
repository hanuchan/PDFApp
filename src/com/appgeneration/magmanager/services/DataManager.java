/**
 * 
 */
package com.appgeneration.magmanager.services;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import com.appgeneration.magmanager.inapp.abstractionlayer.InAppLayer;
import com.appgeneration.magmanager.inapp.abstractionlayer.InAppLayer.InAppSystem;
import com.appgeneration.magmanager.inapp.abstractionlayer.InAppLayerException;
import com.appgeneration.magmanager.inapp.abstractionlayer.InAppLayerItemNotFoundException;
import com.appgeneration.magmanager.inapp.abstractionlayer.InAppLayerListener;
import com.appgeneration.magmanager.inapp.abstractionlayer.InAppLayerSamsungImpl;
import com.appgeneration.magmanager.inapp.abstractionlayer.InAppLayerSystemNotReadyException;
import com.appgeneration.magmanager.interfaces.IssueChangesHandler;
import com.appgeneration.magmanager.interfaces.IssueDownloadHandler;
import com.appgeneration.magmanager.interfaces.IssuePurchaseHandler;
import com.appgeneration.magmanager.interfaces.IssueUncompressHandler;
import com.appgeneration.magmanager.interfaces.MagazineChangesHandler;
import com.appgeneration.magmanager.json.parser.DataContainer;
import com.appgeneration.magmanager.model.DataStatus;
import com.appgeneration.magmanager.model.DocumentStatus;
import com.appgeneration.magmanager.model.Issue;
import com.appgeneration.magmanager.model.ItemStatus;
import com.appgeneration.magmanager.model.Magazine;
import com.appgeneration.magmanager.model.Subscription;
import com.appgeneration.magmanager.tracking.AnalyticsTracking;
import com.appgeneration.magmanager.util.FileUtils;
import com.appgeneration.magmanager.util.JSONUtils;
import com.appgeneration.magmanager.util.RemoteConnUtils;
import com.appgeneration.magmanager.util.TargetSettingsUtils;
import com.crashlytics.android.Crashlytics;

/**
 * @author Miguel
 * 
 */
public class DataManager extends Service implements InAppLayerListener {

	// logs
	private static String TAG = "DataManager";

	// binding
	private final IBinder mBinder = new LocalBinder();
	private int numberOfBinders = 0;

	// data objects
	private Magazine magazine;
	private List<Issue> issues = new ArrayList<Issue>();
	private List<Subscription> subscriptions;

	// local workers
	private DataLocalLoader localLoader;

	// remote workers
	private DataRemoteLoader remoteLoader;

	// listeners
	private List<IssueChangesHandler> issueChangesListeners = new ArrayList<IssueChangesHandler>();
	private List<MagazineChangesHandler> magazineChangesListeners = new ArrayList<MagazineChangesHandler>();

	// issue download
	private AsyncIssueDownloader currentIssueDownloader = null;
	private List<AsyncIssueDownloader> issueDownloaders = new ArrayList<DataManager.AsyncIssueDownloader>();
	private HashMap<Long, WeakReference<IssueDownloadHandler>> issuesDownloadHandlersReferenceHashMap = new HashMap<Long, WeakReference<IssueDownloadHandler>>();
	private long lastRefreshTime = -1;
	private static final long minimumRefreshTimeDistance = 5000;

	// issue uncompress
	private AsyncIssueUncompresser currentIssueUncompresser = null;

	/*
	 * BILLING
	 */
//	private AsyncIssueItemStatusChecker currentItemStatusChecker;
//	private IabHelper iabHelper = null;
//	private boolean isIabHelperReadyToProcessPurchases = false;
//	private boolean isIabHelperUnableToDoV3BillingRequests = false;
//	private Issue currentIssueBeingBought = null;
//	private WeakReference<IssuePurchaseHandler> issuePurchaseHandlerReference = null;
	
	private InAppLayer mInAppLayer = null;

	@Override
	public void onCreate() {
		Crashlytics.log("DataManager: onCreate ");
		Log.i(TAG, "onCreate()");
		super.onCreate();
		TargetSettingsUtils.loadSettingsFromResources(this);
		// starts all loaders/checkers in a sequential manner (executor is
		// one-thread-at-a-time by default since honeycomb)
		loadLocalData();
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//			loadRemoteData();
//		}

		//check what in app system are we targeting
		if (TargetSettingsUtils.IN_APP_SYSTEM == InAppSystem.SAMSUNG) {
			Crashlytics.log("DataManager: creating inappLayer ");
			mInAppLayer = new InAppLayerSamsungImpl();
		}
		else if (TargetSettingsUtils.IN_APP_SYSTEM == InAppSystem.NONE) {
			//nothing
		}
		else {
			Crashlytics.log("DataManager: google abstract in app interface is not implemented yet!");
			Log.e(TAG,"google abstract in app interface is not implemented yet!");
		}
		
	}

	/**
	 * Starts checking item status for current issues
	 */
	private void checkItemStatusForIssues() {

//		if (iabHelper == null) {
//			// create iabHelper to handle billing processes
//			// TODO key should be encripted
//			iabHelper = new IabHelper(
//					this,
//					"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2wApTXXiFAnHMEKReFyAhMTuWHSAqMqCQ748EZg2TOF3b/qXp0ZYVZxHSvsQUeyS6otDhNS/wuiDQTPpNZ/+bZz+UAp2ZEEbjHII7XkkXGntRTO77IaejYkd31F7ELd4LiMRtbFae7XMcdNonzeGxxRGJLlVbcDqMiRibBrhNevWe4zHO6l46RFXtNAzvHHBqnnp3JOVt9hIqr7+4e9GyFjTlrHluiT2azYxVTBiY9dFAM2jVxUTcU3vpCznLhNEqV0ayFvKqiZnKoTJjMK0h9khej4AMD86xAYeD24LM+qkJaafGjjDVVn8BdYTDiOFaZ7ZIS3Z5rztpI+gUKodAQIDAQAB");
//		}
//
//		if (isIabHelperUnableToDoV3BillingRequests) {
//			// mark all priced issues a unavailable and warn the user
//			setAllPaidIssuesItemStatusAsUnavailable();
//			notifyAllIssueListenersThatDataHasChanged(false);
//		} else {
//			if (!isIabHelperReadyToProcessPurchases) {
//				iabHelper.startSetup(this);
//			} else {
//				onIabHelperSucessfullySetup();
//			}
//		}
//		
//		
//		
//		notifyAllIssueListenersThatDataHasChanged(false);

		for (Issue currentIssue : issues) {
			if (currentIssue.getSKU() == null) {
				currentIssue.setItemStatus(ItemStatus.FREE);
			}
			else {
				StringBuilder itemPriceStringBuilder = new StringBuilder();
				try {
					if (mInAppLayer==null) {
						currentIssue.setItemStatus(ItemStatus.UNAVAILABE);
					}
					else if (mInAppLayer.isItemAlreadyBought(currentIssue.getSKU(),itemPriceStringBuilder)) {
						currentIssue.setItemStatus(ItemStatus.BOUGHT);
					}
					else {
						currentIssue.setItemStatus(ItemStatus.AVAILABLE_TO_BUY);
						
						//set price
						currentIssue.setPrice(itemPriceStringBuilder.toString());
					}
				} catch (InAppLayerItemNotFoundException e) {
					currentIssue.setItemStatus(ItemStatus.UNAVAILABE);
				}
				catch (InAppLayerSystemNotReadyException e) {
					currentIssue.setItemStatus(ItemStatus.LOADING);
				} catch (InAppLayerException e) {
					e.printStackTrace();
				}
			}
		}
		notifyAllIssueListenersThatDataHasChanged(false);
	}

//	/**
//	 * 
//	 */
//	private void onIabHelperSucessfullySetup() {
//		Log.d(TAG, "onIabHelperSucessfullySetup");
//
//		isIabHelperReadyToProcessPurchases = true;
//
//		currentItemStatusChecker = new AsyncIssueItemStatusChecker(this,
//				issues, iabHelper);
//		currentItemStatusChecker.execute();
//	}
//
//	private void onIabHelperFailedToSetup(IabResult result) {
//		isIabHelperReadyToProcessPurchases = false;
//		isIabHelperUnableToDoV3BillingRequests = true;
//		Toast.makeText(this,
//				getResources().getString(R.string.in_app_v3_problem),
//				Toast.LENGTH_LONG).show();
//		Log.d(TAG, "Problem setting up In-app Billing: " + result);
//
//		setAllPaidIssuesItemStatusAsUnavailable();
//		notifyAllIssueListenersThatDataHasChanged(false);
//	}

	private void setAllPaidIssuesItemStatusAsUnavailable() {
		for (Issue issue : issues) {
			if (issue.isPaid()) {
				issue.setItemStatus(ItemStatus.UNAVAILABE);
			}
		}
	}

	/**
	 * Starts a new remoteDataLoader that will try to load issues from remote
	 * webservice
	 */
	private void loadRemoteData() {
		remoteLoader = new DataRemoteLoader();
		remoteLoader.execute(this);
	}

	/**
	 * Starts a new localDataLoder that will try to load issue data from the
	 * last .json stored
	 */
	private void loadLocalData() {
		localLoader = new DataLocalLoader();
		localLoader.execute(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Crashlytics.log("DataManager: onStartCommand ");
		Log.i(TAG, "onStartCommand()");
		super.onStartCommand(intent, flags, startId);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		Crashlytics.log("DataManager: onDestroy ");
		Log.i(TAG, "onDestroy()");
		issueDownloaders.clear();
		if (currentIssueDownloader != null) {
			currentIssueDownloader.cancel(true);
		}
		
		if (currentIssueUncompresser != null) {
			currentIssueUncompresser.cancel(true);
		}

		if (mInAppLayer != null) {
			mInAppLayer.dispose();
			mInAppLayer = null;
		}
		super.onDestroy();
	}

	private void terminateIfThereIsNothingToDo() {
		if (!areThereAnyBinders() && !areThereAnyPendingDownloads()) {
			stopSelf();
		}
	}

	/**
	 * BINDING
	 */

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		public DataManager getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return DataManager.this;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		numberOfBinders += 1;
		return mBinder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		numberOfBinders -= 1;
		Log.i(TAG, "OnUnbind()");
		terminateIfThereIsNothingToDo();
		return false;
	}

	private boolean areThereAnyBinders() {
		return numberOfBinders > 0;
	}

	/**
	 * Updates local variables to reflect the data obtained from the workers
	 * 
	 * @param dataContainer
	 *            The data that was loaded
	 */
	private void setDataFromDataContainer(DataContainer dataContainer) {
		issues = dataContainer.getIssues();
		magazine = dataContainer.getMagazine();
		subscriptions = dataContainer.getSubscriptions();
	}

	/**
	 * LISTENERS
	 */

	/**
	 * TODO finish this This method registers the issuesChanges
	 * interface-implemented listener in the listeners array. Everytime issues
	 * are inserted, updated or delete the listener will be notified of the new
	 * set of issues
	 */
	public void registerForIssuesChanges(IssueChangesHandler handler) {
		issueChangesListeners.add(handler);

		// if there are already issues, notify the handler
		if (issues != null) {
			handler.onIssuesChanged(issues, false);
		}
	}

	public void registerForMagazineChanges(MagazineChangesHandler handler) {
		magazineChangesListeners.add(handler);

		// if there is already a magazine, notify the handler
		if (magazine != null) {
			handler.magazineChanged(magazine);
		}
	}

	/**
	 * Service binding interface
	 */

	public void refresh() {
		if ((System.currentTimeMillis() - lastRefreshTime) > minimumRefreshTimeDistance ) {
			loadRemoteData();
		}
	}
	
	public void initInAppPurchasing(Activity sourceActivity) {
		Crashlytics.log("DataManager: initInAppPurchasing ");
		if (mInAppLayer!=null) {
			try {
				mInAppLayer.setmInAppLayerListener(this);
				mInAppLayer.initInAppPurchasing(sourceActivity);
			} catch (InAppLayerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sends a message to all issue changes listeners that issue-related
	 * information has changed
	 * 
	 * @param areIssuesLocal
	 *            TODO
	 */
	private void notifyAllIssueListenersThatDataHasChanged(
			boolean areIssuesLocal) {
		for (IssueChangesHandler issueChangesListener : issueChangesListeners) {
			if (issueChangesListener != null) {
				issueChangesListener.onIssuesChanged(issues, areIssuesLocal);
			}
		}
	}

	private void notifyAllIssueListenersThatThereWasAnErrorFecthingRemoteData(
			DataStatus dataStatus) {
		for (IssueChangesHandler issueChangesListener : issueChangesListeners) {
			if (issueChangesListener != null) {
				if (dataStatus == null) {
					issueChangesListener
							.onIssuesNotLoadedDueToNoInternetConnection();
				} else if (dataStatus == DataStatus.INTERNET_PROBLEM) {
					issueChangesListener
							.onIssuesNotLoadedDueToNoInternetConnection();
				} else if (dataStatus == DataStatus.SERVER_RESPONSE_PROBLEM) {
					issueChangesListener
							.onIssuesNotLoadedDueToServerResponseError();
				}

			}
		}
	}

	/**
	 * Sends a message to all issue changes listeners that magazine-related
	 * information has changed
	 */
	private void notifyAllMagazineListenersThatDataHasChanged() {
		for (MagazineChangesHandler magazineChangesListener : magazineChangesListeners) {
			if (magazineChangesListener != null) {
				magazineChangesListener.magazineChanged(magazine);
			}
		}
	}

	/**
	 * WORKERS CALLBACKS
	 */

	/**
	 * Called when the local loader has finished successfully
	 * 
	 * @param dataContainer
	 */
	private void onLocalDataLoaded(DataContainer dataContainer) {
		Crashlytics.log("DataManager: onLocalDataLoaded ");
		setDataFromDataContainer(dataContainer);

		// close local loader
		localLoader = null;

		// notify listeners
		notifyAllIssueListenersThatDataHasChanged(true);
		notifyAllMagazineListenersThatDataHasChanged();

//		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
//			loadRemoteData();
//		}
	}

	/**
	 * Called when the local loader has finished but failed
	 * 
	 * @param dataContainer
	 */
	private void onLocalDataFailedToLoad() {
		Log.i(TAG, "onLocalDataFailedToLoad");
//		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
//			loadRemoteData();
//		}
	}

	/**
	 * Called when the remote loader has finished successfully
	 * 
	 * @param dataContainer
	 */
	private void onRemoteDataLoaded(DataContainer dataContainer) {
		Crashlytics.log("DataManager: onRemoteDataLoaded ");
		//reset last refresh time
		lastRefreshTime = System.currentTimeMillis();
		
		setDataFromDataContainer(dataContainer);

		// notify listeners
		notifyAllIssueListenersThatDataHasChanged(false);
		notifyAllMagazineListenersThatDataHasChanged();

		checkItemStatusForIssues();
	}

	/**
	 * Called when the remote loader has finished successfully
	 * 
	 * @param result
	 * 
	 * @param dataContainer
	 */
	private void onRemoteDataFailedToLoad(DataContainer containerResult) {
		Crashlytics.log("DataManager: onRemoteDataFailedToLoad ");
		setAllPaidIssuesItemStatusAsUnavailable();
		// notify all issue change listeners that data failed to be fetched
		notifyAllIssueListenersThatThereWasAnErrorFecthingRemoteData(containerResult
				.getDataStatus());
	}

	/**
	 * WORKERS DEFINITION
	 */

	// local
	private class DataLocalLoader extends
			AsyncTask<Context, Void, DataContainer> {

		@Override
		protected DataContainer doInBackground(Context... params) {
			DataContainer dataContainer = null;
			try {
				Context context = params[0];
				File file = new File(context.getDir(FileUtils.JSON_REL_DIR,
						Context.MODE_PRIVATE).getAbsolutePath()
						+ File.separator + FileUtils.JSON_FILE__NAME);
				if (!file.exists()) {
					return null;
				}
				JSONObject jsonObjectFromFile = JSONUtils
						.getJsonObjectFromFileAndCloseStream(new FileInputStream(
								file));

				dataContainer = new DataContainer(jsonObjectFromFile);
				checkIfIssuesAreFree(dataContainer.getIssues());
				checkIssuesDocumentStatus(dataContainer.getIssues());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			return dataContainer;
		}

		@Override
		protected void onCancelled() {
			// TODO Auto-generated method stub
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(DataContainer result) {
			if (result == null) {
				onLocalDataFailedToLoad();
			} else {
				onLocalDataLoaded(result);
			}
		}

	}

	// remote
	private class DataRemoteLoader extends
			AsyncTask<Context, Void, DataContainer> {

		@Override
		protected DataContainer doInBackground(Context... params) {

			// check if there is an internet connection active
			ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

			NetworkInfo networkInfo = connectivityManager
					.getActiveNetworkInfo();
			if (networkInfo == null
					|| (networkInfo != null && !networkInfo.isConnected())) {
				return new DataContainer(DataStatus.INTERNET_PROBLEM);
			}

			DataContainer dataContainer = new DataContainer();
			Context context = params[0];
			try {
				HttpResponse remoteResponse = RemoteConnUtils
						.executeGetRequestAndGetResponse(context,
								TargetSettingsUtils.REMOTE_URL);
				JSONObject jsonObjectFromRemoteResponse = JSONUtils
						.getJsonObjectFromHttpResponse(remoteResponse);
				FileWriter localJsonFileWriter = new FileWriter(
						FileUtils.getLocalMagazineJsonFile(context));
				JSONUtils.writeJSONObjectToOutputStreamAndCloseStream(
						jsonObjectFromRemoteResponse, localJsonFileWriter);
				dataContainer = new DataContainer(jsonObjectFromRemoteResponse);
				checkIfIssuesAreFree(dataContainer.getIssues());
				checkIssuesDocumentStatus(dataContainer.getIssues());
			} 
			catch (JSONException e) {
				e.printStackTrace();
				return new DataContainer(DataStatus.SERVER_RESPONSE_PROBLEM);
			}
			catch (Exception e) {
				e.printStackTrace();
				return dataContainer;
			}
			return dataContainer;
		}

		@Override
		protected void onCancelled() {
			// TODO Auto-generated method stub
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(DataContainer result) {
			if (result == null || !result.isContainerStatusOK()) {
				onRemoteDataFailedToLoad(result);
			} else {
				onRemoteDataLoaded(result);
			}

		}

	}

	/**
	 * STATUS CHECKERS
	 */

	/**
	 * Iterates through all issues and sets their item (in-app) status, by
	 * checking their price attribute. If the price is set to null this issue is
	 * free, otherwise the item is checked by in-app billing API. If there is a
	 * problem using in-app billing API, the item is set as unavailable
	 * 
	 * @param issues
	 *            A list of issues whose item status will be verified
	 */
	private void checkIfIssuesAreFree(List<Issue> issues) {
		for (Issue issue : issues) {
			String issueSKU = issue.getSKU();
			if (issueSKU == null || issueSKU.equals("null")
					|| issueSKU.equals("")) {
				issue.setItemStatus(ItemStatus.FREE);
			}
		}
	}

	/**
	 * Iterate through all issues and sets their document status by checking if
	 * the full or partial document path exist. If the issue is not free nor
	 * bought, it leaves the document status as unavailable
	 * 
	 * @param issues
	 */
	private void checkIssuesDocumentStatus(List<Issue> issues) {
		for (Issue issue : issues) {

			if (currentIssueDownloader != null
					&& currentIssueDownloader.issueToDownload.getId().equals(
							issue.getId())) {
				issue.setDocumentStatus(DocumentStatus.DOWNLOADING);
			} else {
				for (AsyncIssueDownloader issueDownloader : issueDownloaders) {
					if (issueDownloader.issueToDownload.getId().equals(
							issue.getId())) {
						issue.setDocumentStatus(DocumentStatus.QUEUED);
					}
				}
			}

			if (issue.getDocumentStatus() == DocumentStatus.QUEUED
					|| issue.getDocumentStatus() == DocumentStatus.DOWNLOADING) {
				continue;
			}

			// if it is a zip, check if it is uncompressed
			if (FileUtils.isIssueAZipFile(issue)) {
				if (FileUtils.getIssueUnzippedPath(issue, this).exists()
						&& !FileUtils.getIssueFileCompletedPath(issue, this)
								.exists() && !FileUtils.getIssuePartialPath(issue, this)
								.exists()) {
					issue.setDocumentStatus(DocumentStatus.READY);
				} else if (FileUtils.getIssueFileCompletedPath(issue, this)
						.exists()) {
					issue.setDocumentStatus(DocumentStatus.COMPRESSED);
				} else if (FileUtils.getIssueUnzippedPath(issue, this).exists()) {
					issue.setDocumentStatus(DocumentStatus.UNCOMPRESSING);
				} else if (FileUtils.getIssuePartialPath(issue, this).exists()) {
					issue.setDocumentStatus(DocumentStatus.PAUSED);
				} else {
					issue.setDocumentStatus(DocumentStatus.AVAILABLE);
				}
			}
			else if (FileUtils.isIssueAPDFFile(issue)) {
				if (FileUtils.getIssueFileCompletedPath(issue, this)
						.exists()) {
					issue.setDocumentStatus(DocumentStatus.READY);
				} else if (FileUtils.getIssuePartialPath(issue, this).exists()) {
					issue.setDocumentStatus(DocumentStatus.DOWNLOADING);
				} else {
					issue.setDocumentStatus(DocumentStatus.AVAILABLE);
				}
			}
		}
	}

	/**
	 * ISSUE DOWNLOADER
	 */

	/**
	 * Starts a new issue download if no download is currently on progress, or
	 * pauses the current one. If a different one is already in progress, queue
	 * the downloader
	 * 
	 * @param issue
	 * @param wantToPause
	 *            A boolean indicating if it is supposed to pause the download
	 *            if it is already in progress. This shall only be false if a
	 *            handler knows that an issue is not finished yet and wants to
	 *            be its handler, it works as a refresh if the download failed
	 *            for whatever reason or as handler reassign, if the previous
	 *            handler was killed (by GC for example)
	 */
	public void startOrPauseIssueDownload(Issue issue,
			IssueDownloadHandler handler, boolean wantToPause) {
		Log.i(TAG, "startOrPauseIssueDownload(), issueDownloaders size: "
				+ issueDownloaders.size());

		// dont proceed if there is no active internet connection
		if (!RemoteConnUtils.isThereAnyActiveInternetConnection(this)) {
			handler.onDownloadNotStartedDueToNoInternet(issue.getId());
			return;
		}

		// add (or replace) handler to issue download handlers hash map
		issuesDownloadHandlersReferenceHashMap.put(issue.getId(),
				new WeakReference<IssueDownloadHandler>(handler));

		// only allow one download at a time
		if (currentIssueDownloader == null) {
			AsyncIssueDownloader asyncIssueDownloader = new AsyncIssueDownloader(
					this, issue);
			currentIssueDownloader = asyncIssueDownloader;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				currentIssueDownloader.executeOnExecutor(
						AsyncTask.THREAD_POOL_EXECUTOR, null);
			} else {
				currentIssueDownloader.execute();
			}
		} else {
			// if it is an issue downloader already running
			if (currentIssueDownloader.issueToDownload.getId().equals(
					issue.getId())) {
				// is it intended to pause the download
				if (wantToPause) {
					currentIssueDownloader.cancel(true);
				}

			} else {
				// a different issue downloader is already running, put it on
				// hold if it is isnt yet
				for (AsyncIssueDownloader issueDownloader : issueDownloaders) {
					if (issueDownloader.issueToDownload.getId().equals(
							issue.getId())) {
						return;
					}
				}
				if (handler != null) {
					handler.onDownloadQueued(issue.getId());
				}
				AsyncIssueDownloader asyncIssueDownloader = new AsyncIssueDownloader(
						this, issue);
				issueDownloaders.add(asyncIssueDownloader);
			}

		}

	}

	/**
	 * Stop tracking current or future downloads for the given issue id
	 * 
	 * @param issueId
	 *            The issue identifier
	 */
	public void stopTrackingIssueDownload(long issueId) {
		issuesDownloadHandlersReferenceHashMap.remove(issueId);

		// remove current downloader handler, if there is any current downloader
		if (currentIssueDownloader != null) {
			currentIssueDownloader.issueDownloadHandler.clear();
		}
	}

	/**
	 * Called when a issue downloader worker has finished. It sets the current
	 * issue downloader as null to allow new issue downloaders to start, by
	 * gettin the next one in the list
	 */
	private void onIssueDownloaderFinished() {
		issuesDownloadHandlersReferenceHashMap
				.remove(currentIssueDownloader.issueToDownload.getId());
		currentIssueDownloader = null;

		Log.i(TAG, "Download finished, issueDownloaders size:"
				+ issueDownloaders.size());
		// if there are pending downloads, start the next one
		if (issueDownloaders.size() > 0) {
			AsyncIssueDownloader asyncIssueDownloader = issueDownloaders.get(0);
			issueDownloaders.remove(0);
			currentIssueDownloader = asyncIssueDownloader;
			currentIssueDownloader.execute();
		}

		terminateIfThereIsNothingToDo();
	}

	/**
	 * @author Miguel
	 * 
	 *         This class was designed to handle issue download management. It
	 *         stores the file temporarily and if it is cancelled or the
	 *         connection to server fails, the next time the same issue is asked
	 *         to be downloaded, it will resume the previous download.
	 * 
	 */
	private class AsyncIssueDownloader extends
			AsyncTask<Void, Integer, Boolean> {

		String issueRemoteURL = null;
		public WeakReference<IssueDownloadHandler> issueDownloadHandler = null;
		public Issue issueToDownload = null;
		private WeakReference<Context> context = null;

		OutputStream output = null;
		InputStream input = null;

		public AsyncIssueDownloader(Context context, Issue issueToDownload) {
			this.issueRemoteURL = issueToDownload.getDocument();
			Log.i("Download", issueRemoteURL);
			this.issueToDownload = issueToDownload;
			if (issuesDownloadHandlersReferenceHashMap.get(issueToDownload
					.getId()) != null) {
				this.issueDownloadHandler = new WeakReference<IssueDownloadHandler>(
						issuesDownloadHandlersReferenceHashMap.get(
								issueToDownload.getId()).get());
			}

			this.context = new WeakReference<Context>(context);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			if (issueDownloadHandler.get() != null) {
				issueDownloadHandler.get().onDownloadStarted(
						issueToDownload.getId());
			}
		}

		@Override
		protected Boolean doInBackground(Void... nothing) {
			try {
				URL url = new URL(issueRemoteURL);
				URLConnection connection = url.openConnection();
				connection
						.addRequestProperty(
								"Authorization",
								"Basic "
										+ Base64.encodeToString(
												(TargetSettingsUtils.CONTENT_USERNAME
														+ ":" + TargetSettingsUtils.CONTENT_PASSWORD)
														.getBytes(),
												Base64.NO_WRAP));

				if (context.get() == null) {
					return false;
				}
				File issueFilePartialPath = FileUtils
						.getIssuePartialPath(issueToDownload,
								context.get());
				long total = 0;
				if (!issueFilePartialPath.exists()) {
					Log.i("", "Partial file path doesnt exist!");
					issueFilePartialPath.createNewFile();
				} else {

					total = issueFilePartialPath.length();
					Log.i("", "Partial file path exists!, Length: " + total);
					connection.setRequestProperty("Range", "bytes=" + (total)
							+ "-");
				}
				Log.i(TAG, "Headers: "
						+ connection.getRequestProperties().toString());
				input = new BufferedInputStream(connection.getInputStream());
				long fileLength = total + connection.getContentLength();
				output = new BufferedOutputStream(new FileOutputStream(
						issueFilePartialPath, (total != 0)));

				byte data[] = new byte[1024];

				int count;
				int totalLogGoal = 1024 * 1024;
				while ((count = input.read(data)) != -1) {
					if (isCancelled()) {
						output.flush();
						output.close();
						input.close();
						return false;
					}
					total += count;
					if (total >= totalLogGoal) {
						Log.i(TAG, "Downloaded " + total + " of " + fileLength);
						totalLogGoal += 1024 * 1024;
					}

					// publishing the progress....
					publishProgress((int) (total * 100 / fileLength));
					output.write(data, 0, count);
				}

				output.flush();
				output.close();
				input.close();

				if (context.get() == null) {
					return false;
				}
				
				//check if download was successful by comparing filesize with connection length
				
				if (issueFilePartialPath.length() == fileLength) {
					// rename file to its full path name

					issueFilePartialPath.renameTo(FileUtils
							.getIssueFileCompletedPath(issueToDownload,
									context.get()));
				}
				else {
					return false;
				}
				
				
			} catch (Exception e) {
				e.printStackTrace();
				try {
					if (output != null) {
						output.flush();
						output.close();
					}
					if (input != null) {
						input.close();
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				e.printStackTrace();
				return false;
			}
			return true;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			if (issueDownloadHandler.get() != null) {
				issueDownloadHandler.get().postDownloadProgress(progress[0],
						issueToDownload.getId());
			} else {
				// update reference by re-checking hash map
				if (issuesDownloadHandlersReferenceHashMap.get(issueToDownload
						.getId()) != null) {
					issueDownloadHandler = new WeakReference<IssueDownloadHandler>(
							issuesDownloadHandlersReferenceHashMap.get(
									issueToDownload.getId()).get());
					issueDownloadHandler.get().postDownloadProgress(
							progress[0], issueToDownload.getId());
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			Log.i("", "onCancelled");
			if (issueDownloadHandler.get() != null) {
				issueDownloadHandler.get().onDownloadCancelled(
						issueToDownload.getId());
			}
			onIssueDownloaderFinished();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Boolean result) {
			Log.i("", "onPostExecute");
			if (result) {

				// analytics tracking
				AnalyticsTracking.trackIssueDownload(issueToDownload);

				if (issueDownloadHandler.get() != null) {
					issueDownloadHandler.get().onDownloadFinished(
							issueToDownload.getId());
				}
			} else {
				if (issueDownloadHandler.get() != null) {
					issueDownloadHandler.get().onDownloadFailed(
							issueToDownload.getId());
				}
			}

			onIssueDownloaderFinished();
		}
	}

	private boolean areThereAnyPendingDownloads() {
		return currentIssueDownloader != null || !issueDownloaders.isEmpty();
	}

	/*
	 * ISSUE UNCOMPRESSER
	 */

	public void startUncompressingIssue(Issue issue,
			IssueUncompressHandler handler) {
		Log.i(TAG, "startUncompressingIssue()");

		// allow only one uncompression at a time
		if (currentIssueUncompresser != null) {

			if (!currentIssueUncompresser.issueToUncompress.getId().equals(issue.getId())) {
				// warn the handler that the issue cannot be uncompressed because
				// another uncompression is in progress
				handler.onUncompressNotStartedDueToCongestion(issue.getId());
			}
			
		} else {

			// start uncompressing
			AsyncIssueUncompresser asyncIssueUncompresser = new AsyncIssueUncompresser(
					this, issue, handler);
			currentIssueUncompresser = asyncIssueUncompresser;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				currentIssueUncompresser.executeOnExecutor(
						AsyncTask.THREAD_POOL_EXECUTOR, null);
			} else {
				currentIssueUncompresser.execute();
			}
		}

	}

	/**
	 * @author Miguel
	 * 
	 *         This class was designed to handle issue uncompress management
	 * 
	 */
	private class AsyncIssueUncompresser extends
			AsyncTask<Void, Integer, Boolean> {

		public WeakReference<IssueUncompressHandler> issueUncompressHandler = null;
		public Issue issueToUncompress = null;
		private WeakReference<Context> context = null;

		public AsyncIssueUncompresser(Context context, Issue issueToUncompress,
				IssueUncompressHandler uncompressHandler) {
			this.issueToUncompress = issueToUncompress;
			this.issueUncompressHandler = new WeakReference<IssueUncompressHandler>(
					uncompressHandler);

			this.context = new WeakReference<Context>(context);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			if (issueUncompressHandler.get() != null) {
				issueUncompressHandler.get().onUncompressStarted(
						issueToUncompress.getId());
			}
		}

		@Override
		protected Boolean doInBackground(Void... nothing) {
			try {

				if (!FileUtils.unzip(
						FileUtils.getIssueFileCompletedPath(issueToUncompress,
								context.get()).getAbsolutePath(),
						FileUtils.getIssueUnzippedPath(issueToUncompress,
								context.get()).getAbsolutePath(), this)) {
					return false;
				}
				FileUtils.getIssueFileCompletedPath(issueToUncompress,
						context.get()).delete();
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onCancelled(java.lang.Object)
		 */
		@Override
		protected void onCancelled() {
			Log.i("", "onCancelled");
			currentIssueUncompresser = null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Boolean result) {
			Log.i("", "onPostExecute");
			currentIssueUncompresser = null;
			if (result) {
				if (issueUncompressHandler.get() != null) {
					issueUncompressHandler.get().onUncompressFinished(
							issueToUncompress.getId());
				} else {
					if (issueUncompressHandler.get() != null) {
						issueUncompressHandler.get().onUncompressFailed(
								issueToUncompress.getId());
					}
				}
			}
		}
	}

	@Override
	public void onInAppLayerSucessfullyStarted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInAppLayerFailedToStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInAppLayerItemsAllLoaded() {
		checkItemStatusForIssues();
	}

	@Override
	public void onInAppLayerItemsStatusChanged() {
		checkItemStatusForIssues();
	}

	
	
//	/**
//	 * @author Miguel
//	 * 
//	 *         This class was designed to handle issue item status checking All
//	 *         issues shall be given to it, since it will filter out the free
//	 *         ones
//	 */
//	private class AsyncIssueItemStatusChecker extends
//			AsyncTask<Void, Integer, Boolean> {
//
//		private WeakReference<Context> context = null;
//		private WeakReference<IabHelper> iabHelper = null;
//		private List<Issue> issuesToBeChecked = new ArrayList<Issue>();
//		private List<String> issuesSKUsToBeChecked = new ArrayList<String>();
//
//		public AsyncIssueItemStatusChecker(Context context,
//				List<Issue> completeSetOfIssues, IabHelper iabHelper) {
//			this.context = new WeakReference<Context>(context);
//			this.iabHelper = new WeakReference<IabHelper>(iabHelper);
//			issuesToBeChecked = completeSetOfIssues;
//		}
//
//		@Override
//		protected Boolean doInBackground(Void... nothing) {
//
//			// filter the issues that are free
//
//			List<Issue> paidIssuesToBeChecked = new ArrayList<Issue>();
//			for (Issue issue : issuesToBeChecked) {
//				if (issue.isPaid()) {
//					paidIssuesToBeChecked.add(issue);
//					issuesSKUsToBeChecked.add(issue.getSKU());
//				}
//			}
//			issuesToBeChecked = paidIssuesToBeChecked;
//
//			// check inventory
//			if (iabHelper.get() != null) {
//				IabHelper iabHelperTmpHelper = iabHelper.get();
//				try {
//
//					Inventory itemsInventory = null;
//					try {
//						itemsInventory = iabHelperTmpHelper
//								.queryInventory(true, issuesSKUsToBeChecked);
//					} catch (IllegalStateException e) {
//						// This means that IAB wasn't properly setup
//						return false;
//						
//					}
//					catch (NullPointerException e) {
//						// Internal IAB error
//						return false;
//					}
//					
//
//					if (itemsInventory != null) {
//
//						// for every paid issue, set its price
//						// and check if the user has bought iondet
//						for (Issue issue : issuesToBeChecked) {
//							SkuDetails issueSkuDetails = itemsInventory
//									.getSkuDetails(issue.getSKU());
//
//							if (issueSkuDetails != null) {
//								issue.setPrice(issueSkuDetails.getPrice());
//								issue.setItemStatus(ItemStatus.AVAILABLE_TO_BUY);
//							} else {
//								issue.setItemStatus(ItemStatus.UNAVAILABE);
//							}
//
//							if (itemsInventory.hasPurchase(issue.getSKU())) {
//								issue.setItemStatus(ItemStatus.BOUGHT);
//							}
//						}
//
//						// everything ok
//						return true;
//
//					} else {
//						Log.e(TAG,
//								"Inventory is null even though no exception was thrown");
//						return false;
//					}
//
//				} catch (IabException e) {
//
//					Log.e(TAG, "Problem quering inventory");
//
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//
//					return false;
//				}
//			}
//
//			return false;
//
//		}
//
//		/*
//		 * (non-Javadoc)
//		 * 
//		 * @see android.os.AsyncTask#onCancelled(java.lang.Object)
//		 */
//		@Override
//		protected void onCancelled(Boolean result) {
//			Log.i("", "onCancelled");
//			currentItemStatusChecker = null;
//			super.onCancelled(result);
//		}
//
//		/*
//		 * (non-Javadoc)
//		 * 
//		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
//		 */
//		@Override
//		protected void onPostExecute(Boolean result) {
//			Log.i("", "onPostExecute");
//			currentItemStatusChecker = null;
//			if (result) {
//				// notify issue changes
//				notifyAllIssueListenersThatDataHasChanged(false);
//			} else {
//				Log.e(TAG, "Error getting items billing status");
//				onItemStatusFailedToBeFectched();
//			}
//		}
//	}
//
	/**
	 * Starts the purchase flow for a given issue
	 * 
	 * @param sourceActivity
	 * @param handler
	 *            The handler that will deal with purchase flow progress
	 * @param issueToBePurchased
	 * @return A boolean indicating whether iabHelper is ready to start
	 *         processing requests
	 */
	public boolean startPurchaseFlowForIssue(Activity sourceActivity,
			IssuePurchaseHandler handler, Issue issueToBePurchased) {
//		if (iabHelper != null && isIabHelperReadyToProcessPurchases == true) {
//
////			if (currentIssueBeingBought != null) {
////				handler.onIssueNotPurchasedDueToAPurchaseAlreadyInProgress(currentIssueBeingBought
////						.getId());
////				return true;
////			}
//
//			currentIssueBeingBought = issueToBePurchased;
//			issuePurchaseHandlerReference = new WeakReference<IssuePurchaseHandler>(
//					handler);
//
//			iabHelper.launchPurchaseFlow(sourceActivity,
//					issueToBePurchased.getSKU(), 1001, this);
//		}

		if (mInAppLayer != null) {
			try {
				mInAppLayer.startPurchaseProcessForItem(issueToBePurchased.getSKU());
			} catch (InAppLayerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}

	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		if (mInAppLayer != null) {
			return mInAppLayer.handleActivityResult(requestCode, resultCode, data);
		}
		return false;
	}
//	
//	@Override
//	public void onIabPurchaseFinished(IabResult result, Purchase info) {
//
//		Log.i(TAG, "onIabPurchaseFinished");
//
//		if (result.isFailure()) {
//
//			// check if user cancelled
//			if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED) {
//				if (issuePurchaseHandlerReference.get() != null) {
//					issuePurchaseHandlerReference.get()
//							.onIssueNotPurchasedDueToUserOption(
//									currentIssueBeingBought.getId());
//				}
//			} else if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_ERROR) {
//				Log.e(TAG, "Failure ononIabPurchaseFinished");
//				if (issuePurchaseHandlerReference.get() != null) {
//					issuePurchaseHandlerReference.get()
//							.onIssueNotPurchasedDueToFailure(
//									currentIssueBeingBought.getId());
//				}
//			} else if (result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
//				Log.e(TAG, "BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED");
//
//				// this is weird, user is trying to buy an item he already has
//				// lets make sure that all his items are up to date by
//				// rechecking bought issues
//				checkItemStatusForIssues();
//			}
//
//		} else {
//
//			// success!
//
//			if (info.getSku().equals(currentIssueBeingBought.getSKU())) {
//				if (issuePurchaseHandlerReference.get() != null) {
//					issuePurchaseHandlerReference.get().onIssuePurchased(
//							currentIssueBeingBought.getId());
//				}
//			} else {
//				// this is also weird, we received a different SKU from the one
//				// we were expecting
//
//			}
//
//		}
//
//		// reset properties
//		currentIssueBeingBought = null;
//		issuePurchaseHandlerReference = null;
//	}
//
//	@Override
//	public void onIabSetupFinished(IabResult result) {
//		if (!result.isSuccess()) {
//			// problem happened
//			onIabHelperFailedToSetup(result);
//			return;
//		}
//		// iab fully setup, lets proceed
//		onIabHelperSucessfullySetup();
//	}
//	
//	public void onItemStatusFailedToBeFectched() {
//		iabHelper = null;
//		isIabHelperReadyToProcessPurchases= false;
//		//checkItemStatusForIssues();
//	}

}
