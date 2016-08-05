package com.nxp.nxpwalletconndev.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.databases.MyDbHelper;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.loaderservice.SmartcardLoaderServiceResponse;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.tasks.ExecuteScriptTask;
import com.nxp.nxpwalletconndev.tasks.GetVersionTask;

public class AboutActivity extends BaseActivity implements OnOperationListener, OnTransmitApduListener  {
	
	public static final int ACTION_GET_VERSION = 0;
    public static final int ACTION_EXECUTE_SCRIPT = 1;
    public int mAction = 0;
    
	private TextView apkVersion;
	private TextView fwVersion;
	private TextView jcopVersion;
	
	private MyDbHelper mMyDbHelper;
	private ProgressDialog progressdialog;
	
	public static final String scriptsOutputFolder = Environment.getExternalStorageDirectory() + "/loaderserviceconndev/";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		
		apkVersion = (TextView) findViewById(R.id.textApkVersion);
		fwVersion = (TextView) findViewById(R.id.textFwVersion);
		jcopVersion = (TextView) findViewById(R.id.textJCOPVersion);
		
		String text_jcopversion;
		if(forAmotech)
		{
			if(versionJCOP3dot3)
				text_jcopversion = "JCOP v3.3-A";
			else
				text_jcopversion = "JCOP v3.1-A";
		}
		else
		{
			if(versionJCOP3dot3)
				text_jcopversion = "JCOP v3.3";
			else
				text_jcopversion = "JCOP v3.1";
		}
		jcopVersion.setText(text_jcopversion);
		
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
			apkVersion.setText(String.format(getResources().getString(R.string.apk_version), pInfo.versionName));
			
			if(isDeviceConnected() == true) {
				// Update the preferences
				if(MyPreferences.isCardOperationOngoing(AboutActivity.this) == false) {
					mAction = ACTION_GET_VERSION;
					
					new GetVersionTask(AboutActivity.this).execute();
				} else {
					fwVersion.setText(String.format(getResources().getString(R.string.fw_version_operating)));
				}
			} else {
				fwVersion.setText(String.format(getResources().getString(R.string.fw_version_not_connected)));
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		// Get database helper
		mMyDbHelper = new MyDbHelper(this);
		
		((ImageView) findViewById(R.id.image_reset)).setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				showFactoryResetWarning();
			}
		});
	}
	
	@Override
	public void processOperationResult(byte[] result) {
		if(result != null) {
			switch (mAction) {
			case ACTION_GET_VERSION:
				processStatusGetVersion(result);
				break;
				
			case ACTION_EXECUTE_SCRIPT:
				processStatusScript(result);
				break;
				
			default:
				break;
	    	}
		} else
			Toast.makeText(AboutActivity.this, "Error detected in the BLE channel", Toast.LENGTH_LONG).show();
	}
	
	private void processStatusGetVersion(byte[] result) {
		if(result != null && result.length == 2) {
			fwVersion.setText(String.format(getResources().getString(R.string.fw_version), 
					String.format("%02d", result[0]) + "." + String.format("%02d", result[1])));
		} else if (result != null && result.length == 3) {
			fwVersion.setText(String.format(getResources().getString(R.string.fw_version), 
					String.format("%02d", result[0]) + "." + String.format("%02d", result[1]) + "." + String.format("%02d", result[2])));
		} else
			fwVersion.setText(String.format(getResources().getString(R.string.fw_version_not_available)));
	}
	
	public void processStatusScript(byte[] result) {   		
		// Action completed
		progressdialog.dismiss();
		
		String resp = "test_factoryreset_encrypted_ConnDevOutput.txt";
		
		// Set the name for the output String
		int bufferLength = result.length;
		String out = scriptsOutputFolder + resp;
		SmartcardLoaderServiceResponse.writeOutputFile(AboutActivity.this, out, new String(result));
		
		if (result[bufferLength - 2] == '0' && result[bufferLength - 3] == '0' 
					&& result[bufferLength - 4] == '0' && result[bufferLength - 5] == '9') {
			Toast.makeText(AboutActivity.this, "eSE Reset completed", Toast.LENGTH_LONG).show();
			
			// Clear the database for this device id
			mMyDbHelper.clearCards(getDeviceId());
        } else {
        	Toast.makeText(AboutActivity.this, "eSE Reset failed", Toast.LENGTH_LONG).show();

        }	
	}
	
	private void showFactoryResetWarning() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(AboutActivity.this);
        builder.setTitle(getResources().getString(R.string.dialog_reset_title));
        builder.setMessage(getResources().getString(R.string.dialog_reset_msg));
        
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int index) {
            	   if(isDeviceConnected() == true) {
   						// Update the preferences
   						if(MyPreferences.isCardOperationOngoing(AboutActivity.this) == false) {
	   						mAction = ACTION_EXECUTE_SCRIPT;
	   						
	   						// Show the progress dialog on the screen to inform about the action
	   						progressdialog = ProgressDialog.show(AboutActivity.this, 
	   								getResources().getString(R.string.dialog_resetting), 
	   								getResources().getString(R.string.dialog_waiting), 
	   								false, false);
	   						
	   						String script;
	   						if(forAmotech)
	   						{
	   							if(versionJCOP3dot3)
	   	   							script = "factory_reset_jcop_33_amotech_encrypted.txt";
	   	   						else
	   	   							script = "test_factoryreset_encrypted.txt";
	   						}
	   						else
	   						{
	   							if(versionJCOP3dot3)
	   	   							script = "test_factoryreset_jcop_33_encrypted.txt";
	   	   						else
	   	   							script = "test_factoryreset_encrypted.txt";
	   						}
	   						   						
	   						new ExecuteScriptTask(AboutActivity.this, script).execute();
   						} else {
   							Toast.makeText(AboutActivity.this, "Operation in progress", Toast.LENGTH_LONG).show();
   						}
   					} else {
   						Toast.makeText(AboutActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
   					}
               	}
           });
        
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                	dialog.dismiss();
            }
        });
        
        // Create the AlertDialog object and return it
        builder.create();
        builder.show();
	}
	
	@Override
	public void sendApduToSE(byte[] dataBT, int timeout) {
		// Send the data via Bluetooth
        writeBluetooth(dataBT, timeout);
	}

	@Override
	public void processOperationNotCompleted() {
		switch (mAction) {
		case ACTION_GET_VERSION:
			fwVersion.setText(String.format(getResources().getString(R.string.fw_version_not_available)));
			break;
			
		case ACTION_EXECUTE_SCRIPT:
			// Close the dialog
			progressdialog.dismiss();
			
			break;
		}
		
		Intent broadcast = new Intent();
		broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
        sendBroadcast(broadcast);
        
        Toast.makeText(getApplicationContext(), "About Error detected in BLE channel", Toast.LENGTH_LONG).show();
	}
}
