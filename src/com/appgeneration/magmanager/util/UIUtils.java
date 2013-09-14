package com.appgeneration.magmanager.util;

import android.app.Activity;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;

public class UIUtils {

	public static Point getWindowSize(Activity sourceActivity) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			Display display = sourceActivity.getWindowManager()
					.getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			return size;
		} else {
			Display display = sourceActivity.getWindowManager()
					.getDefaultDisplay();
			int width = display.getWidth(); // deprecated
			int height = display.getHeight(); // deprecated
			return new Point(width, height);
		}

	}
}
