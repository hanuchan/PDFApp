package com.library.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.appgeneration.magmanager.library.R;
import com.artifex.mupdf.domain.StorageUtils;
import com.google.analytics.tracking.android.EasyTracker;



public class BaseActivity extends Activity  {
	private static final String TAG = "BaseActivity";

	public static final String TEST_INIT_COMPLETE = "TEST_INIT_COMPLETE";
	protected static final int CONNECTION_ALERT = 1;
	protected static final int SERVER_ALERT = 2;
	protected static final int DOWNLOAD_ALERT = 3;
	protected static final int IO_EXCEPTION = 4;

	private SharedPreferences sharedPreferences;

	/**
	 * A {@link PurchaseObserver} is used to get callbacks when Android Market
	 * sends messages to this application so that we can update the UI.
	 */

	/**
	 * Replaces the language and/or country of the device into the given string.
	 * The pattern "%lang%" will be replaced by the device's language code and
	 * the pattern "%region%" will be replaced with the device's country code.
	 * 
	 * @param str
	 *            the string to replace the language/country within
	 * @return a string containing the local language and region codes
	 */
	protected String replaceLanguageAndRegion(String str) {
		// Substitute language and or region if present in string
		if (str.contains("%lang%") || str.contains("%region%")) {
			Locale locale = Locale.getDefault();
			str = str.replace("%lang%", locale.getLanguage().toLowerCase(Locale.getDefault()));
			str = str.replace("%region%", locale.getCountry().toLowerCase(Locale.getDefault()));
		}
		return str;
	}
	
	protected int getUpdatePeriod() {
		return 1800000;
	}


	public boolean isOnline() {
		ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo i = conMgr.getActiveNetworkInfo();
		if (i == null) {
			return false;
		}
		if (!i.isConnected()) {
			return false;
		}
		if (!i.isAvailable()) {
			return false;
		}
		return true;
	}

	public SharedPreferences getPreferences() {
		if (null == sharedPreferences) {
			sharedPreferences = getSharedPreferences("LIBRARY_SHARED_PREFERENCES", MODE_PRIVATE); 
		}
		return sharedPreferences;
	}

	/**
	 * Creates storage directories if necessary
	 */
	protected void initStorage(String... folders) {
        final String storagePath = StorageUtils.getStoragePath(this);
        File f = new File(storagePath);
		if (!f.exists()) {
			Log.d(TAG, storagePath + " was create");
			f.mkdirs();
		}
		f = new File(StorageUtils.getExternalPath(this));
		if (!f.exists()) {
			Log.d(TAG, StorageUtils.getExternalPath(this) + " was create");
			f.mkdirs();
		}
		if (null != folders && folders.length != 0) {
			for (String folder : folders) {
				File dir = new File(storagePath + folder);
				if (!dir.exists()) {
					dir.mkdir();
				}
			}
		}
	}

	/**
	 * Copy files from android assets directory
	 * 
	 * @param src
	 *            the source target
	 * @param dst
	 *            the destination target
	 */
	protected int copyFromAssets(String src, String dst){
		int count = -1;
		Log.d(TAG, "copyFromAssets " + src + " => " + dst);
		try {
			InputStream input = getAssets().open(src);
			OutputStream output = new FileOutputStream(dst);
			byte data[] = new byte[1024];

			while ((count = input.read(data)) != -1) {
				output.write(data, 0, count);
			}
			output.flush();
			output.close();
			input.close();
		} catch (IOException e) {
			Log.e(TAG, "copyFromAssets failed", e);
		}
		return count;
	}

	protected void enableRotation(boolean isEnable) {
		android.provider.Settings.System.putInt(
				getContentResolver(), android.provider.Settings.System.ACCELEROMETER_ROTATION, isEnable ? 1 : 0);
	}

	protected boolean hasTestMagazine() {
		return getResources().getBoolean(R.bool.enable_test_magazine);
	}
	
	protected void showAlertDialog(int id){
		int msg_id = 0;
		final int fId = id;
		switch (id) {
		case CONNECTION_ALERT:{
			msg_id = R.string.connection_failed;
			break;
		}
		case SERVER_ALERT:{
			msg_id = R.string.server_error;
			break;
		}
		case DOWNLOAD_ALERT:{
			msg_id = R.string.download_failed_please_check_your_connection;
			break;
		}
		case IO_EXCEPTION:{
			msg_id = R.string.no_space_on_device;
			break;
		}
		}
		String message = getResources().getString(msg_id);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder
			.setMessage(message)
			.setCancelable(false)
			.setPositiveButton(R.string.ok, new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(fId!= IO_EXCEPTION){
						finish();
					}
				}
			});
 		AlertDialog alertDialog = alertDialogBuilder.create();
 		alertDialog.show();
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
		
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

    @Override
    protected void onResume() {
        super.onResume();
       // EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
       // EventBus.getDefault().unregister(this);
    }

}
