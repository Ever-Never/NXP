package com.nxp.nxpwalletconndev.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.databases.MyDbHelper;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.tasks.CreateVCTask;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.nxpwalletconndev.utils.StatusBytes;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothConnectListener;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothReadListener;

public class PayInActivity extends BaseActivity implements OnBluetoothConnectListener, OnBluetoothReadListener, OnOperationListener, OnTransmitApduListener {
	public static final int TEMP_CARD_ID = 0xF3;
	
	public static final int ACTION_CREATE_VC = 0;
    public static final int ACTION_ADD_AND_UPDATE_MDAC_VC = 1;
    
    public int mAction = 0;
    public int mVcEntry;
    public int mId;

	private String CreateVCData_Valencia = "460100A50702020101030100A60705020400060108A11F80080FFFFFFFFFFFFFFF810100820200008301008401008501008603000000A8122010FFFFFFFFFFFF08778F00FFFFFFFFFFFF";
	private String CreateVCData_Vigo = "460100A50702020101030100A60705020400060108A11F80080FFFFFFFFFFFFFFF810100820200008301008401008501008603000000A8142012020000000000000000000000000000000000F803010203";

	private MyDbHelper mMyDbHelper;
	
	public final static int TYPE_VALENCIA = 0;
	public final static int TYPE_VIGO = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_payin);
				
		// Get database helper
		mMyDbHelper = new MyDbHelper(this);
		
		((ImageView) findViewById(R.id.card_valencia)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				createCardDialog(TYPE_VALENCIA);
			}
		});
		
		((ImageView) findViewById(R.id.card_vigo)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				createCardDialog(TYPE_VIGO);
			}
		});
	}
	
	public void createCardDialog(final int index) {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(PayInActivity.this);
        builder.setTitle("PayIn");
        builder.setMessage("Create transport card?");
        
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
            	   if(isDeviceConnected() == true) {
            		   if(MyPreferences.isCardOperationOngoing(PayInActivity.this) == false) {
		            	   mId = index;
		            	   
		            	   // Set the creation of a fake card to let the user see Creating Card
		            	   Card vc = new Card(0, 0, TEMP_CARD_ID, "", "", "", "", "", Card.STATUS_CREATING, false, false, R.drawable.card_blank, Card.MIFARE_PAYIN, Card.TYPE_MIFARE_CLASSIC, 100);			
		            	   mMyDbHelper.addCard(vc, getDeviceId());
		            	   
		            	   mAction = ACTION_CREATE_VC;
		       	    	
							String CreateVCData = "";
							switch(mId) {
							case TYPE_VALENCIA:
								CreateVCData = CreateVCData_Valencia;
								break;
							case TYPE_VIGO:
								CreateVCData = CreateVCData_Vigo;
								break;
							}
            	   
							AsyncTask<?, ?, ?> task = new CreateVCTask(PayInActivity.this, CreateVCData, null).execute();	
	           				setRunningAsyncTask(task);
		            	   
		            	   finish();		            	   
            		   } else
            			   Toast.makeText(PayInActivity.this, "Operation not possible while card is under process", Toast.LENGTH_LONG).show();
            	   } else
                 	   Toast.makeText(PayInActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
               }
           });
        
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                   dialog.dismiss();
               }
           });

        builder.show();
	}
    
    @Override
    public void processOperationResult(final byte[] result) {
    	if(result != null) {
	    	switch (mAction) {
			case ACTION_CREATE_VC:
				processStatusCreateVC(result);
				break;
				
//			case ACTION_ADD_AND_UPDATE_MDAC_VC:
//				processStatusMdac(result);
//				break;
				
			default:
				break;
	    	}
    	} else {
			Toast.makeText(PayInActivity.this, "Error detected in the BLE channel", Toast.LENGTH_LONG).show();
		}
    }
    
    private void processStatusCreateVC(final byte[] result) {
    	Intent broadcast = new Intent();
		short status = Parsers.getSW(result);

		// Regardless of the result I remove the temporaty entry
		mMyDbHelper.deleteCard(TEMP_CARD_ID, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
		
		switch (status) {
		case StatusBytes.SW_NO_ERROR:
			Card vc = null;
			int order = mMyDbHelper.getCardToCreateOrder(getDeviceId());
			
			switch(mId) {
			case TYPE_VALENCIA:
				vc = Parsers.getVcEntry(result, "Valencia", "10", R.drawable.valencia, Card.MIFARE_PAYIN, Card.TYPE_MIFARE_CLASSIC, order);		
				break;
			case TYPE_VIGO:
				vc = Parsers.getVcEntry(result, "Vigo", "10", R.drawable.vigo, Card.MIFARE_PAYIN, Card.TYPE_MIFARE_DESFIRE, order);		
				break;
			}
		
			mMyDbHelper.addCard(vc, getDeviceId());
			
			// Store the VCEntry Identifier
			mVcEntry = vc.getIdVc();
			
			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
    		
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
			
//			new addAndUpdateMDACTask(vc.getIdVc()).execute();
	        
			break;
		default:
			Toast.makeText(getApplicationContext(),
					"Error occured during virtual card creation",
					Toast.LENGTH_LONG).show();		
			
			Toast.makeText(PayInActivity.this, "Card Creation failed", Toast.LENGTH_LONG).show();
			
    		broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
			
			break;
		}
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

    	}		
	}
}
