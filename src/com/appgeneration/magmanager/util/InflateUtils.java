/**
 * 
 */
package com.appgeneration.magmanager.util;

import android.view.View;

/**
 * @author miguelferreira
 *
 */
public class InflateUtils {
	public static View findViewByIdOrReturnNull(View parentView, int id) {
		try {
			View aView =  parentView.findViewById(id);
			return aView;
		} catch (NoSuchFieldError e) {
			return null;
		}
		
	}
}
