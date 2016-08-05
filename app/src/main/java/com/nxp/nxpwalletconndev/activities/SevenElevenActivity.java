package com.nxp.nxpwalletconndev.activities;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.connections.DataConnection;
import com.nxp.nxpwalletconndev.databases.MyDbHelper;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.notifications.MyNotification;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.tasks.ActivateVCTask;
import com.nxp.nxpwalletconndev.tasks.CreateVCTask;
import com.nxp.nxpwalletconndev.tasks.NfcHttp7ElevenProxyTask;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.nxpwalletconndev.utils.StatusBytes;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothConnectListener;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothReadListener;

public class SevenElevenActivity extends BaseActivity implements OnBluetoothConnectListener, OnBluetoothReadListener, OnOperationListener, OnTransmitApduListener {
	public static final String TAG = "MainActivity";

	private String CreateVCData = "460102A50702020101030100A60705020400060108A11F80080FFFFFFFFFFFFFFF810100820200008301008401008501008603000000A8122010AAAAAAAAAAAA08778F00313131313131";
	private String VCCreatePerso = "4210A0000003964D344D240081DB690200014310A0000003964D344DA40081DB69010001470100";
	
	public static final int ACTION_CREATE_VC = 0;
	public static final int ACTION_PERSO_CARD = 1;
	public static final int ACTION_ACTIVATE_VC = 2;
	
	public static final int TEMP_CARD_ID = 0xF2;

	private Button createCard;
	
	public int mAction = 0;
    public int mVcEntry;
    
    private MyDbHelper mMyDbHelper;
    
    private EditText editFirstName;
	private EditText editLastName;
	private String userName = "";
	
	private String mRandId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_seveneleven);
		
		createCard = (Button) findViewById(R.id.button_create_seleven);
		
		editFirstName = (EditText) findViewById(R.id.edit_first_name);
		editLastName = (EditText) findViewById(R.id.edit_last_name);
		
		editFirstName.setText(MyPreferences.getUserName(getApplicationContext()), TextView.BufferType.EDITABLE);
		editLastName.setText(MyPreferences.getUserSurname(getApplicationContext()), TextView.BufferType.EDITABLE);
		
		userName = editFirstName.getText().toString() + " " + editLastName.getText().toString();
		
		// MIFARE Classic sector is only 16 bytes
		if(userName.length() > 16)
			userName = userName.substring(0, 16);
		
		editFirstName.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				userName = editFirstName.getText().toString() + " " + editLastName.getText().toString();
				
				// MIFARE Classic sector is only 16 bytes
				if(userName.length() > 16)
					userName = userName.substring(0, 16);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
		
		editLastName.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				userName = editFirstName.getText().toString() + " " + editLastName.getText().toString();
				
				// MIFARE Classic sector is only 16 bytes
				if(userName.length() > 16)
					userName = userName.substring(0, 16);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
		
		// Get database helper
		mMyDbHelper = new MyDbHelper(this);
		
		// Store the Random identifier for the SP SD AID
		Random rand = new Random();
		int r = rand.nextInt(99);
		
		NumberFormat formatter = new DecimalFormat("00"); 
		mRandId = formatter.format(r);
		
		Log.d("..........", "-- RAND ID: " + mRandId);
	}
	
	@Override
	protected void onResume() {
		// Set the GUI appropriately
		setGUI();
		
		super.onResume();
	}
	
	public void setGUI() {
		createCard.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isCardValid() == true) {
					if(isDeviceConnected() == true) {
						if(MyPreferences.isCardOperationOngoing(SevenElevenActivity.this) == false) {
							if(DataConnection.isConnected(SevenElevenActivity.this) == true) {
								showCreationDialog();	
							} else
								showConnectionRequiredDialog();
						} else
             			   Toast.makeText(SevenElevenActivity.this, "Operation not possible while card is under process", Toast.LENGTH_LONG).show();
					} else
	                 	   Toast.makeText(SevenElevenActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
				} else {
					// Inform the user
					Toast.makeText(getApplicationContext(), "Invalid values for card perso", Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	
	private boolean isCardValid() {
		return !userName.isEmpty();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	private void initPerso() {
		// Store the action to do
		mAction = ACTION_PERSO_CARD;

		try {
			Log.d("..........", "-- PERSO RAND ID: " + mRandId);
			AsyncTask<?, ?, ?> task = new NfcHttp7ElevenProxyTask(SevenElevenActivity.this, "http://www.themobileknowledge.com/Servlets/RemotePerso/M4mServletMain", "7Eleven", userName, mRandId).execute();
			setRunningAsyncTask(task);
		} catch (Exception e) {
			e.printStackTrace();
			
			Log.e(TAG, "Error executing personalization");
		}
	}
	
	private String getVCCreatePersogetRandomAID(String perso) {
		return perso.substring(0, perso.indexOf("4310A0000003964D344DA40081DB69010001") - 2)
				+ mRandId
				+ perso.substring(perso.indexOf("4310A0000003964D344DA40081DB69010001"), perso.indexOf("470100") - 2)
				+ mRandId
				+ perso.substring(perso.indexOf("470100"));
	}
	
	public void showCreationDialog() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(SevenElevenActivity.this);
        builder.setTitle(getResources().getString(R.string.init_create_card));
        builder.setMessage(getResources().getString(R.string.init_create_card_msg));
        
        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
        	   // Set the action
				mAction = ACTION_CREATE_VC;
        	   
				// Set the creation of a fake card to let the user see Creating Card
				Card vc = new Card(0, 0, TEMP_CARD_ID, "", "", "", "", "", Card.STATUS_CREATING, false, false, R.drawable.card_blank, Card.MIFARE_LOYALTY, Card.TYPE_MIFARE_CLASSIC, 100);			
				mMyDbHelper.addCard(vc, getDeviceId());
				
					// Alert the user about the card creation
				MyNotification.show(SevenElevenActivity.this, 
						getResources().getString(R.string.notif_card_creating_title), getResources().getString(R.string.notif_card_creating_msg),
						MyNotification.NOTIF_ID_OPERATING);
				
				// Make the AID Random so that we can create multiple Loyalty Cards
				String VCCreatePersoAIDRand = getVCCreatePersogetRandomAID(VCCreatePerso);

				AsyncTask<?, ?, ?> task = new CreateVCTask(SevenElevenActivity.this, CreateVCData, VCCreatePersoAIDRand).execute();
   				setRunningAsyncTask(task);

				finish();
           }
       });
 
        // Create the AlertDialog object and return it
        builder.create();
        builder.show();
		
	}
		
	@Override
    public void processOperationResult(final byte[] result) {
		if(result != null) {
	    	switch (mAction) {
			case ACTION_CREATE_VC:
				processStatusCreateVC(result);
				break;
				
			case ACTION_PERSO_CARD:
				NfcHttp7ElevenProxyTask.receiveApduFromSE(result);
				break;
				
			case ACTION_ACTIVATE_VC:
				processStatusActivate(result);
				break;
				
			default:
				break;
	    	}
		} else {
			Toast.makeText(SevenElevenActivity.this, "Error detected in the BLE channel", Toast.LENGTH_LONG).show();
		}
    }
	
	private void processStatusCreateVC(final byte[] result) {
    	Intent broadcast = new Intent();
		short status = Parsers.getSW(result);
	
		// Regardless of the result I remove the temporary entry
		mMyDbHelper.deleteCard(TEMP_CARD_ID, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
				
		switch (status) {
		case StatusBytes.SW_NO_ERROR:
			int order = mMyDbHelper.getCardToCreateOrder(getDeviceId());
			Card vc = Parsers.getVcEntry(result, "7-Eleven", "0", R.drawable.loyalty_green, Card.MIFARE_LOYALTY, Card.TYPE_MIFARE_CLASSIC, order);			
			mMyDbHelper.addCard(vc, getDeviceId());
					
			// Store the VCEntry Identifier
			mVcEntry = vc.getIdVc();

			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_PERSONALIZING, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
    		
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZING);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
	        
	        // Personalize the card
	        initPerso();
	        
			break;
		default:
			Toast.makeText(getApplicationContext(),
					"Error occured during virtual card creation",
					Toast.LENGTH_LONG).show();		
			
    		broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
			
			break;
		}
    }
	
	public void proccessTransactionTaskResult(StringBuffer result){
		Intent broadcast = new Intent();
		
		if(result.indexOf("successful") != -1){
//			Toast.makeText(this, getResources().getString(R.string.trans_succeed), Toast.LENGTH_LONG).show();
			
			// Update the points
			mMyDbHelper.updateCardNumber(mVcEntry, "100", getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
			
			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_ACTIVATING, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
			
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_ACTIVATING);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
	        
	        mAction = ACTION_ACTIVATE_VC;
			
			AsyncTask<?, ?, ?> task = new ActivateVCTask(SevenElevenActivity.this, mVcEntry).execute(); 
			setRunningAsyncTask(task);
		} else {
			Toast.makeText(this, getResources().getString(R.string.trans_failed), Toast.LENGTH_LONG).show();
			
			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_FAILED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
			
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
		}
	}
	
	private void processStatusActivate(final byte[] result) {
    	Intent broadcast = new Intent();
    	
		// Set the activating status
		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
					
		if(result != null) {
			short status = Parsers.getSW(result);
			
			switch (status) {
			case StatusBytes.SW_NO_ERROR:
//					Toast.makeText(getApplicationContext(), "Card activated",
//							Toast.LENGTH_LONG).show();
				
				broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
		        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
		        sendBroadcast(broadcast);
				
				// Alert the user about the card creation
				MyNotification.show(SevenElevenActivity.this, 
						getResources().getString(R.string.notif_card_created_title), getResources().getString(R.string.notif_card_created_msg),
						MyNotification.NOTIF_ID_COMPLETED);
				
				mMyDbHelper.removeFav(getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
				mMyDbHelper.makeCardFav(mVcEntry, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
				
				break;
			default:
				Toast.makeText(getApplicationContext(),
						"Error activating card", Toast.LENGTH_LONG).show();
				break;
			}
		} else {
			Toast.makeText(getApplicationContext(), "Bluetooth connection not established", Toast.LENGTH_LONG).show();
		}
	}
	
	private void showConnectionRequiredDialog() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(SevenElevenActivity.this);
        builder.setTitle(getResources().getString(R.string.internet_error));
        builder.setMessage(getResources().getString(R.string.internet_perso_msg));
        
        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
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
		Intent broadcast = new Intent();
		broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
        sendBroadcast(broadcast);
		
        Toast.makeText(getApplicationContext(), "Error detected in BLE channel", Toast.LENGTH_LONG).show();
		
    	switch (mAction) {
		case ACTION_CREATE_VC:
			mMyDbHelper.deleteCard(TEMP_CARD_ID, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
			break;
			
		case ACTION_PERSO_CARD:
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_FAILED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
    		break;
    		
		case ACTION_ACTIVATE_VC:
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
    		break;
    	}		
	}
}
