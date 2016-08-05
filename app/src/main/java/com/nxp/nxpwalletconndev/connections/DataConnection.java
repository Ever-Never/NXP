package com.nxp.nxpwalletconndev.connections;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class DataConnection {
	public static boolean isConnected(Context ctx) {
	    ConnectivityManager connec = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	    android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    android.net.NetworkInfo mobile = connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

	    // Here if condition check for wifi and mobile network is available or not.
	    // If anyo of them is connected then it will return true, otherwise false;

	    if (wifi.getState() == NetworkInfo.State.CONNECTED || mobile.getState() == NetworkInfo.State.CONNECTED ) {
	        return true;
	    } else
	    	return false;
	}
}
