/**
 * 
 */
package com.appgeneration.magmanager.util;

/**
 * @author miguelferreira
 *
 * This class provides a wide range of string-based utility helper functions
 *
 */
public class StringUtils {

	public static String getFileTypeFromFileURIAsString(String uri) {
		return uri.substring(uri.lastIndexOf("."));
	}
}
