package com.nxp.nxpwalletconndev.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.ssdp.btclient.BluetoothTLV;

public class DeleteVCTask extends AsyncTask<String, Integer, Boolean> {
	public static final int DELETE_VC_TIMEOUT = 20000;
	
	int vcEntry;
	Context ctx;
	OnTransmitApduListener listener;

	public DeleteVCTask(Context ctx, int vcEntry) {
		this.ctx = ctx;
		this.vcEntry = vcEntry;
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
		byte[] delete = { (byte) vcEntry };
	    byte[] dataBT = BluetoothTLV.getTlvCommand(BluetoothTLV.LTSM_DELETE_VC, delete); 
		        
	    // Execute the APDU 
     	listener.sendApduToSE(dataBT, DELETE_VC_TIMEOUT);

        return false;
	}
}
