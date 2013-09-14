/**
 * 
 */
package com.appgeneration.magmanager.util;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * @author miguelferreira
 * 
 */
public class RemoteConnUtils {
	/**
	 * This functions executes a POST request to a given url Currently it sends
	 * content of type json and it expects a json in return
	 * 
	 * @param url
	 *            The String representing the URL of the REST web API to connect
	 *            to
	 * @param params
	 *            The JSONObject representing the parameters to be posted
	 * @return The HttpResponse of the request made
	 * @throws IOException
	 *             The parameters are not well defined
	 * @throws ClientProtocolException
	 *             There was an error retrieving the response from the server
	 *             Connection exceptions
	 */
	public static HttpResponse executeGetRequestAndGetResponse(Context context,
			String url) throws ClientProtocolException, IOException {
		// instantiates httpclient to make request
		DefaultHttpClient httpclient = new DefaultHttpClient();

		final HttpParams httpParameters = httpclient.getParams();

		HttpConnectionParams.setConnectionTimeout(httpParameters, 15000);
		HttpConnectionParams.setSoTimeout(httpParameters, 15000);

		// url with the post data
		HttpGet httpGet = new HttpGet(url);

		// sets a request header so the page receving the request
		// will know what to do with it
		httpGet.setHeader("Accept", "application/json");

		// Handles what is returned from the page
		HttpResponse response = httpclient.execute(httpGet);

		return response;
	}

	public static boolean isThereAnyActiveInternetConnection(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
			return true;
		}
		return false;
	}
}
