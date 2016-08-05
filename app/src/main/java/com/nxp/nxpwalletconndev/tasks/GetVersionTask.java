package com.nxp.nxpwalletconndev.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.ssdp.btclient.BluetoothTLV;

public class GetVersionTask extends AsyncTask<String, Integer, Boolean> {	
	public static final int GET_VERSION_TIMEOUT = 10000;
	
	Context ctx;
	OnTransmitApduListener listener;

	public GetVersionTask(Context ctx) {
		this.ctx = ctx;
		this.listener = (OnTransmitApduListener) ctx;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		// Set the operation delegate
		BaseActivity.setOperationDelegate((OnOperationListener) ctx);
		
		// Make sure no other operations are completed until this one is completed
		MyPreferences.setCardOperationOngoing(ctx, true);
	}
	
	@Override
	protected Boolean doInBackground(String... scripts) {
        byte[] dataBT = BluetoothTLV.getTlvCommand(BluetoothTLV.FW_GET_VERSION, new byte[] { 0x00 }); 
        
        // Execute the APDU 
     	listener.sendApduToSE(dataBT, GET_VERSION_TIMEOUT);
     	
     	return false;
	}
}
