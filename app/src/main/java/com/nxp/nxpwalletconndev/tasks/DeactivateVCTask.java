package com.nxp.nxpwalletconndev.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.ssdp.btclient.BluetoothTLV;

public class DeactivateVCTask extends AsyncTask<String, Integer, Boolean> {
	public static final int DEACTIVATE_VC_TIMEOUT = 10000;
	
	int vcEntry;
	Context ctx;
	OnTransmitApduListener listener;

	public DeactivateVCTask(Context ctx, int vcEntry) {
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
        byte[] activate = { (byte) vcEntry };
        byte[] dataBT = BluetoothTLV.getTlvCommand(BluetoothTLV.LTSM_DEACTIVATE_VC, activate); 
        
        // Execute the APDU 
     	listener.sendApduToSE(dataBT, DEACTIVATE_VC_TIMEOUT);
     	
     	return false;
	}
}
