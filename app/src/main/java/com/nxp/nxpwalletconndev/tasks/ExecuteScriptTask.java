package com.nxp.nxpwalletconndev.tasks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.ssdp.btclient.BluetoothTLV;

public class ExecuteScriptTask extends AsyncTask<String, Integer, Boolean> {
	public static final int EXECUTE_SCRIPT_TIMEOUT = 120000;
	
	String script;
	OnTransmitApduListener listener;
	Context ctx;

	public ExecuteScriptTask(Context ctx, String script) {
		this.script = script;
		this.listener = (OnTransmitApduListener) ctx;
		this.ctx = ctx;
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
		InputStream is_load = null;

		AssetManager assManager = ctx.getAssets();

		try {
			is_load = assManager.open(script);
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + script);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("I/O error: " + script);
			System.exit(1);
		}
		
		if (is_load == null) {
			Log.e("ExecuteScript", "Invalid reference to script");
		} else {
			try {
				int numBytes = is_load.available();
				
				byte[] buffer = new byte[numBytes];  // buffer store for the stream
				
				// Read from the InputStream
                is_load.read(buffer);

				//  ReadData.setText("");
		        byte[] dataBT = BluetoothTLV.getTlvCommand(BluetoothTLV.LS_EXECUTE_SCRIPT, buffer); 

				// Send the data via Bluetooth
		     	listener.sendApduToSE(dataBT, EXECUTE_SCRIPT_TIMEOUT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
}
