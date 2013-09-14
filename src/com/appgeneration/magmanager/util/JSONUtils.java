/**
 * 
 */
package com.appgeneration.magmanager.util;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

/**
 * @author miguelferreira
 * 
 */
public class JSONUtils {

	/**
	 * Loads a json object from a file. The inputstream is closed here
	 * 
	 * @param stream
	 *            The inputstream where the json will be read from
	 * @return A jsonObject if everything was ok or null if a error happened
	 */
	public static JSONObject getJsonObjectFromFileAndCloseStream(
			InputStream stream) {
		try {

			JSONObject jObject = null;
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(stream, "UTF-8"));
				StringBuilder builder = new StringBuilder();
				for (String line = null; (line = reader.readLine()) != null;) {
					builder.append(line).append("\n");
				}
				JSONTokener tokener = new JSONTokener(builder.toString());
				jObject = new JSONObject(tokener);
			} finally {
				stream.close();
			}
			return jObject;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Writes a json object to a file represented by a file writer. Note that
	 * the file writer is closed here
	 * 
	 * @param obj
	 *            The json object
	 * @param file
	 *            The destination file
	 */
	public static void writeJSONObjectToOutputStreamAndCloseStream(
			JSONObject obj, FileWriter file) {
		try {

			file.write(obj.toString());
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function retrieves a JsonObject from a HttpResponse
	 * 
	 * @param response
	 *            The HttpResponse to be parse
	 * @return The JsonObject response from the HttpResponse
	 * @throws JSONException
	 *             The response is not a valid json
	 * @throws IOException
	 *             It is impossible to read the response contents
	 * @throws IllegalStateException
	 *             Illegal State
	 * @throws UnsupportedEncodingException
	 * @see org.apache.http.HttpResponse
	 * @see org.json.JSONObject
	 */
	public static JSONObject getJsonObjectFromHttpResponse(HttpResponse response)
			throws JSONException, UnsupportedEncodingException,
			IllegalStateException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				response.getEntity().getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
			builder.append(line).append("\n");
		}
		JSONTokener tokener = new JSONTokener(builder.toString());
		JSONObject obj = new JSONObject(tokener);
		return obj;
	}
}
