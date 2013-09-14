package com.appgeneration.magmanager.ui.activities;

import java.lang.ref.WeakReference;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.appgeneration.magmanager.interfaces.IssueUIHandler;
import com.appgeneration.magmanager.interfaces.MainActivityInterface;
import com.appgeneration.magmanager.library.R;
import com.appgeneration.magmanager.model.Issue;
import com.appgeneration.magmanager.services.DataManager;
import com.appgeneration.magmanager.tracking.AnalyticsTracking;
import com.appgeneration.magmanager.ui.fragment.InfoDialogFragment;
import com.appgeneration.magmanager.ui.fragment.PreviewIssueFragment;
import com.appgeneration.magmanager.ui.fragment.StoreIssueGridFragment;
import com.appgeneration.magmanager.util.GlobalSettings;
import com.crittercism.app.Crittercism;
import com.google.analytics.tracking.android.EasyTracker;


public class StoreActivity extends FragmentActivity implements
		MainActivityInterface {
	/**
	 * Flag to turn off price for testing
	 */	
public static final boolean turnOffPrice = true; // nhi add to turn off price
	private final String TAG = "StoreActivity";
	private final String TAG_PREVIEW = "PreviewActivity";
	private final String TAG_INFO = "INFO";
	private Intent dataManagerIntent = null;
	private long issueIdBeingDeleted = -1;
	private WeakReference<IssueUIHandler> issueControlleReference;
	private StoreIssueGridFragment storeIssueGridFragment = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Crittercism.init(getApplicationContext(), "517016cec463c23a4900000d");
		dataManagerIntent = new Intent(this, DataManager.class);
		startService(dataManagerIntent);
		if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {

			Log.i(TAG, "Replacing store");
			final FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			storeIssueGridFragment = new StoreIssueGridFragment();
			issueControlleReference = new WeakReference<IssueUIHandler>(
					storeIssueGridFragment);
			ft.replace(R.id.store_fragment_placeholder, storeIssueGridFragment,
					TAG);
			ft.commit();
		}

		// refresh callback
		ImageView refreshImageView = (ImageView) findViewById(R.id.header_refresh_image);
		if (refreshImageView != null) {

		}
		refreshImageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				// analytics tracking
				AnalyticsTracking.trackClickRefresh();

				// broadcast a refresh
				broadcastRefresh();

			}
		});

		// info callback
		ImageView infoImageView = (ImageView) findViewById(R.id.header_info_image);
		if (infoImageView != null) {
			infoImageView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// analytics tracking
					AnalyticsTracking.trackClickInfo();

					showInfo();
				}
			});
		}
	}

	private void broadcastRefresh() {
		LocalBroadcastManager.getInstance(this).sendBroadcast(
				new Intent(GlobalSettings.BROADCAST_REFRESH_ACTION));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		Log.i("", "Destroying Store Activity...");
		stopService(dataManagerIntent);
		dataManagerIntent = null;
		super.onDestroy();

	}

	/**
	 * Issue deletion
	 */

	public void setIssueIdBeingDeleted(long issueIdBeingDeleted) {
		this.issueIdBeingDeleted = issueIdBeingDeleted;
	}

	public void onClickDeleteIssue() {

		// Broadcasts an delete issue intent
		Intent deleteIssueIntent = new Intent(
				GlobalSettings.BROADCAST_DELETE_ACTION);
		deleteIssueIntent.putExtra(GlobalSettings.BROADCAST_DELETE_ID_EXTRA,
				issueIdBeingDeleted);
		LocalBroadcastManager.getInstance(this)
				.sendBroadcast(deleteIssueIntent);

		this.issueIdBeingDeleted = -1;
	}

	public void onClickNotDeleteIssue() {

		this.issueIdBeingDeleted = -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.appgeneration.magmanager.interfaces.MainActivityInterface#previewIssue
	 * (com.appgeneration.magmanager.model.Issue)
	 */
	public void previewIssue(Issue issueToPreview) {

		if (getSupportFragmentManager().findFragmentByTag(TAG_PREVIEW) == null) {
			final FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			ft.addToBackStack(null);
			PreviewIssueFragment issuePreviewFragment = new PreviewIssueFragment();
			issuePreviewFragment.setIssueToPreview(issueToPreview);
			issuePreviewFragment
					.setIssueControllerHandler(issueControlleReference);
			ft.setCustomAnimations(android.R.anim.fade_in,
					android.R.anim.fade_out, android.R.anim.fade_in,
					android.R.anim.fade_out);
			issuePreviewFragment.show(ft, TAG_PREVIEW);
		}
	}

	public void showInfo() {
		if (getSupportFragmentManager().findFragmentByTag(TAG_INFO) == null) {
			final FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			ft.addToBackStack(null);
			InfoDialogFragment infoDialogFragment = new InfoDialogFragment();
			ft.setCustomAnimations(android.R.anim.fade_in,
					android.R.anim.fade_out, android.R.anim.fade_in,
					android.R.anim.fade_out);
			infoDialogFragment.show(ft, TAG_INFO);
		}
	}

	@Override
	public void startReadingBookWithPath(String bookPath) {
		//not used
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

	    // Pass on the activity result to the helper for handling
	    if (!storeIssueGridFragment.handleActivityResult(requestCode, resultCode, data)) {
	        // not handled, so handle it ourselves (here's where you'd
	        // perform any handling of activity results not related to in-app
	        // billing...
	        super.onActivityResult(requestCode, resultCode, data);
	    }
	    else {
	        Log.d(TAG, "onActivityResult handled by IABUtil.");
	    }
	}

}
