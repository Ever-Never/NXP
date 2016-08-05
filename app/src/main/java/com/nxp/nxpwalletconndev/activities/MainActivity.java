package com.nxp.nxpwalletconndev.activities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.listeners.OnCloseListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothConnectListener;

public class MainActivity extends BaseActivity implements OnCloseListener {
	// Loader Service script execution paths
	public static final String scriptsOutputFolder = Environment.getExternalStorageDirectory() + "/loaderserviceconndev/";
	public static final String scriptsOutputFile = Environment.getExternalStorageDirectory() + "/loaderserviceconndev/Readme.txt";
	
	boolean isLogued = false;
	
	private RelativeLayout linearUser;
	private RelativeLayout linearCash;
	private RelativeLayout linearCards;
	
	private TextView textAddUser;
	private TextView textAddUserDesc;
	
	private LinearLayout lwelcome;
	private TextView welcome;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// First time I open the app I want to show the discovery menu
		showDiscoveryDialog = true;
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		linearUser = (RelativeLayout) findViewById(R.id.layoutImage1);
		linearUser.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), LoginActivity.class);				
				startActivity(i);
			}
		});
			
		linearCash = (RelativeLayout) findViewById(R.id.layoutImage2);
		linearCash.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), MyCardsActivity.class);
				startActivity(i);
			}
		});
		
		linearCards = (RelativeLayout) findViewById(R.id.layoutImage3);
		linearCards.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), MyDevicesActivity.class);
				startActivity(i);
			}
		});
		
		lwelcome = (LinearLayout) findViewById(R.id.l_welcome_user);
		welcome = (TextView) findViewById(R.id.welcome_user);
		
		lwelcome.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), LogoutActivity.class);				
				startActivity(i);
			}
		});
		
		textAddUser = (TextView) findViewById(R.id.menu_user);
		textAddUserDesc = (TextView) findViewById(R.id.menu_user_desc);
		
		// We start from the beginning
		MyPreferences.setCardOperationOngoing(getApplicationContext(), false);
		
		// Create folder to store response files
		createOutputScriptsFolder();
		
		// We are about to close the app
		setCloseListener(null);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Update registration info
		showRegistrationInfo();
	}

	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.d("..................", "---------- ON DESTROY");
		
		// We are not longer waiting for incoming establishment
		setWaitingForConnResp(false);
		
		// We are about to close the app
		setCloseListener(this);
		
		// Close the BLuetooth connection
		closeConnection();
	}
	
	@Override
	public void onBackPressed() {
		if(MyPreferences.isCardOperationOngoing(MainActivity.this) == true) {
			// Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	        builder.setTitle(getResources().getString(R.string.close_app));
	        builder.setMessage(getResources().getString(R.string.close_app_msg));
	        
	        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       dialog.dismiss();
	                   }
	               });
	 
	        // Create the AlertDialog object and return it
	        builder.create();
	        builder.show();
		} else
			finish();  
	}
	
	private void createOutputScriptsFolder() {
		// Check if the folder already exists
        File myFolder = new File(scriptsOutputFolder);

        if(myFolder.exists() == false) {
        	myFolder.mkdirs();

        	// Announce to the system that a new folder has been created
        	if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(myFolder)));
        		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        	} else {
        		try {
        			// KitKat does not refresh folder so I have to create a temporary file and then remove it
        			File myFile = new File(scriptsOutputFile);
        			myFile.createNewFile();
        			
        			FileOutputStream fOut = new FileOutputStream(myFile, true);
    		        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
    		        myOutWriter.append("Output files received from the Loader Service feature on Android KitKat");
    		        myOutWriter.close();
    		        fOut.close();
				
        			sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(myFile)));
        			sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        		} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
	}
	
	private void showRegistrationInfo() {
		// Retrieve user login status
		isLogued = MyPreferences.isLogued(getApplicationContext());		
		
		if(isLogued) {
			lwelcome.setVisibility(View.VISIBLE);
			welcome.setText("Hello, " + MyPreferences.getUserName(getApplicationContext()) + " " + MyPreferences.getUserSurname(getApplicationContext()));
			
			linearUser.setVisibility(View.GONE);
			linearCash.setVisibility(View.VISIBLE);
			linearCards.setVisibility(View.VISIBLE);
		} else {
			lwelcome.setVisibility(View.INVISIBLE);
			
			textAddUser.setText(getResources().getString(R.string.menu_login));
			textAddUserDesc.setText(getResources().getString(R.string.menu_login_desc));
			
			linearUser.setVisibility(View.VISIBLE);
			linearCash.setVisibility(View.GONE);
			linearCards.setVisibility(View.GONE);
		}
	}

	@Override
	public void onClose() {
		Log.d("..................", "---------- ON CLOSE");
		
		// Close the BluetoothClient
		closeBluetoothClient();
	}
}
