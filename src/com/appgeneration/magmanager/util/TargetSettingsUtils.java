/**
 * 
 */
package com.appgeneration.magmanager.util;

import com.appgeneration.magmanager.inapp.abstractionlayer.InAppLayer;
import com.appgeneration.magmanager.library.R;

import android.content.Context;

/**
 * @author miguelferreira
 * 
 */
public class TargetSettingsUtils {

	public static String REMOTE_URL = null;
	public static long MAGAZINE_ID = -1;
	public static int JSON_VERSION = -1;
	public static String CONTENT_USERNAME = null;
	public static String CONTENT_PASSWORD = null;
	public static InAppLayer.InAppSystem IN_APP_SYSTEM = null;
	
	public static String IN_APP_SAMSUNG_ITEM_GROUP_ID = null;
	

	/**
	 * Loads various setting from the resources files. The settings are not
	 * defined in a single file, they are distributed by Android standards
	 * 
	 * @param context
	 */
	public static void loadSettingsFromResources(Context context) {
		MAGAZINE_ID = Long.decode(context.getResources().getString(
				R.string.magazine_id));
		JSON_VERSION = Integer.decode(context.getResources().getString(
				R.string.json_version));
		REMOTE_URL = FileUtils.getRemoteUrlFromMagazineIdAndApiVersion(
				MAGAZINE_ID, JSON_VERSION);
		CONTENT_USERNAME = context.getResources().getString(R.string.content_username);
		CONTENT_USERNAME = context.getResources().getString(R.string.content_username);
		IN_APP_SYSTEM = InAppLayer.InAppSystem.values()[Integer.parseInt(context.getResources().getString(R.string.iapp_system))];
		try {
			IN_APP_SAMSUNG_ITEM_GROUP_ID = context.getResources().getString(R.string.samsung_iapp_item_group_id);
		} catch (Exception e) {
			// nothing
		}
		
		
		if (CONTENT_USERNAME.equals("null")) {
			throw new AssertionError();
		}
		CONTENT_PASSWORD = context.getResources().getString(R.string.content_password);
		if (CONTENT_PASSWORD.equals("null")) {
			throw new AssertionError();
		}
	}

}
