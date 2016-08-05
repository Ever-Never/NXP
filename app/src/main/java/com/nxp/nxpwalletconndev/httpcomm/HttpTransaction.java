package com.nxp.nxpwalletconndev.httpcomm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class HttpTransaction {

	private static final String TAG = "HTTP Transaction";
	private StringBuffer console;
	private HttpClient client;

	public HttpTransaction(StringBuffer console, Context ctx) {
		this.console = console;
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		int timeout;
		try{
		timeout = Integer.parseInt(sharedPrefs.getString("server_timeout", "60")) * 1000;
		} catch(NumberFormatException e){
			timeout = 60 * 1000;
		}
		Log.i(TAG, "Timeout Value: " + timeout);
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used.
		int timeoutConnection = timeout;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = timeout;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

		client = new DefaultHttpClient(httpParameters);
	}

	public String executeHttpGet(List<NameValuePair> params, String urlString) throws Exception {
		BufferedReader in = null;
		String page = null;
		try {
			// try to avoid ECONN Reset issue
			System.setProperty("http.keepAlive", "false");
			HttpGet request = new HttpGet(urlString + "?" + URLEncodedUtils.format(params, "UTF-8"));
			console.append("\n\nSend Request to http Server:\n" + request.getURI() + "\n");
			HttpResponse response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			page = sb.toString();
			//console.append("\nResponse from http Server:\n" + page + "\n");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return page;
	}
	
	public void safeClose() {
	    if(client != null && client.getConnectionManager() != null)
	    {
	        client.getConnectionManager().shutdown();
	    }
	}
}
