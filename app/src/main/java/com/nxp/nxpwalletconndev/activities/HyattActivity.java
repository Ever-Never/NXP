package com.nxp.nxpwalletconndev.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.databases.MyDbHelper;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.notifications.MyNotification;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.tasks.ActivateVCTask;
import com.nxp.nxpwalletconndev.tasks.AddAndUpdateMDACTask;
import com.nxp.nxpwalletconndev.tasks.CreateVCTask;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.nxpwalletconndev.utils.StatusBytes;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothConnectListener;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothReadListener;

public class HyattActivity extends BaseActivity implements OnBluetoothConnectListener, OnBluetoothReadListener, OnOperationListener, OnTransmitApduListener {
	public static class HotelEntry {	
		public int  id;
		public int iconRes;
		public String hotel;
		public String arrival;
		public String departure;

		public HotelEntry(final int id, final int iconRes, final String hotel, final String arrival, final String departure) {
			this.id = id;
			this.hotel = hotel;
			this.iconRes = iconRes;
			this.arrival = arrival;
			this.departure = departure;
		}
	};
	
	AsyncTask task;
	
	public static final int TEMP_CARD_ID = 0xF0;
	
	public static final int ACTION_CREATE_VC = 0;
    public static final int ACTION_ADD_AND_UPDATE_MDAC_VC = 1;
    public static final int ACTION_ACTIVATE_VC = 2;
    
    public HotelEntry mEntry = null;
    public int mAction = 0;
    
    public int mVcEntry;
	
	private List<HotelEntry> mHotels = new ArrayList<HotelEntry>();
	private ArrayAdapter<HotelEntry> mHotelAdapter;
	
	private ListView list;

	private String CreateVCData_AbuDhabi = "460100A50702020101030100A60705020400060108A11F80080FFFFFFFFFFFFFFF810100820200008301008401008501008603000000A8122010AAAAAAAAAAAA08778F00212121212121";
	private String CreateVCData_Shangai = "460100A50702020101030100A60705020400060108A11F80080FFFFFFFFFFFFFFF810100820200008301008401008501008603000000A8122010AAAAAAAAAAAA08778F00222222222222";
	private String CreateVCData_Boston = "460100A50702020101030100A60705020400060108A11F80080FFFFFFFFFFFFFFF810100820200008301008401008501008603000000A8122010AAAAAAAAAAAA08778F00232323232323";
	
	private String vcMFPassValue_id0 = "9D463774C56ED2BA";
	private String vcMFPassValue_id1 = "A7658019458B37C1";
	private String vcMFPassValue_id2 = "1D84C2CFA63B73AD";

	private MyDbHelper mMyDbHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hyatt);
		
		mHotels.add(new HotelEntry(
				0, R.drawable.hotel_abudhabi, "Capital Gate, Abu Dhabi", "12/03/15", "14/03/16"));
		mHotels.add(new HotelEntry(
				1, R.drawable.hotel_shangai, "Grand Hyatt Shangai", "03/04/14", "05/04/16"));
		mHotels.add(new HotelEntry(
				2, R.drawable.hotel_boston, "Hyatt Regency Boston", "04/06/15", "05/06/16"));
		
		list = (ListView) findViewById(R.id.listHotels);
		
		// Get database helper
		mMyDbHelper = new MyDbHelper(this);
	}

	@Override
    protected void onResume(){        
        // Show created Virtual Cards
        displayHotels();
            	
        super.onResume();
    }
    
    protected void displayHotels() {
		list = (ListView) findViewById(R.id.listHotels);	
		mHotelAdapter = new ArrayAdapter<HotelEntry>(this,
				R.layout.hotel, mHotels) {
			@Override
			public View getView(final int position, final View convertView,
					final ViewGroup parent) {
				final HotelEntry entry = getItem(position);

				View root;

				if (convertView == null) {
					final LayoutInflater inflater = LayoutInflater
							.from(HyattActivity.this);

					root = inflater
							.inflate(R.layout.hotel,
									parent, false);
				} else {
					root = convertView;
				}		

				((ImageView) root.findViewById(R.id.hotel_icon))
					.setImageResource(entry.iconRes);
				
				((TextView) root.findViewById(R.id.hotel_hotel))
					.setText(entry.hotel);
				
				((TextView) root.findViewById(R.id.hotel_arrival))
					.setText(entry.arrival);
				
				((TextView) root.findViewById(R.id.hotel_departure))
					.setText(entry.departure);
								
				root.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {					
						// Use the Builder class for convenient dialog construction
				        AlertDialog.Builder builder = new AlertDialog.Builder(HyattActivity.this);
				        builder.setTitle("Download hotel card");
				        
				        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			                   public void onClick(DialogInterface dialog, int id) {
			                	   if(isDeviceConnected() == true) {
			                		   if(MyPreferences.isCardOperationOngoing(HyattActivity.this) == false) {
					                	   	// Set the creation of a fake card to let the user see Creating Card
					           				Card vc = new Card(0, 0, TEMP_CARD_ID, "", "", "", "", "", Card.STATUS_CREATING, false, false, R.drawable.card_blank, Card.MIFARE_HOSPITALITY, Card.TYPE_MIFARE_CLASSIC, 100);			
					           				mMyDbHelper.addCard(vc, getDeviceId());
					           				
					           				mEntry = entry;
					           				mAction = ACTION_CREATE_VC;
					           		    	
					           		    	String CreateVCData = "";
					           				
					           				switch(entry.id) {
					           				case 0:
					           					CreateVCData = CreateVCData_AbuDhabi;
					           					break;
					           				case 1:
					           					CreateVCData = CreateVCData_Shangai;
					           					break;
					           				case 2:
					           					CreateVCData = CreateVCData_Boston;
					           					break;
					           				}
					           			
					           				AsyncTask<?, ?, ?> task = new CreateVCTask(HyattActivity.this, CreateVCData, null).execute();
					           				setRunningAsyncTask(task);
					           				
					           				finish();
			                		   } else
			                			   Toast.makeText(HyattActivity.this, "Operation not possible while card is under process", Toast.LENGTH_LONG).show();
			                	   } else
				                 	   Toast.makeText(HyattActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
			                   }
			               });
				        
				        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
			                   public void onClick(DialogInterface dialog, int id) {
			                       dialog.dismiss();
			                   }
			               });

				        builder.show();
					}
				});
		
				return root;
			}
		};

		list.setAdapter(mHotelAdapter);
	}
    
    @Override
    public void processOperationResult(final byte[] result) {
    	if(result != null) {
	    	switch (mAction) {
			case ACTION_CREATE_VC:
				processStatusCreateVC(result);
				break;
				
			case ACTION_ADD_AND_UPDATE_MDAC_VC:
				processStatusMdac(result);
				break;
				
			case ACTION_ACTIVATE_VC:
				processStatusActivate(result);
				break;
				
			default:
				break;
	    	}
    	} else {
			Toast.makeText(HyattActivity.this, "Error detected in the BLE channel", Toast.LENGTH_LONG).show();
		}
    }
    
    private void processStatusCreateVC(final byte[] result) {
    	Intent broadcast = new Intent();
		short status = Parsers.getSW(result);
		
		// Regardless of the result I remove the temporaty entry
		mMyDbHelper.deleteCard(TEMP_CARD_ID, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
	
		switch (status) {
		case StatusBytes.SW_NO_ERROR:
			int order = mMyDbHelper.getCardToCreateOrder(getDeviceId());
			
			Card vc = Parsers.getVcEntry(result, mEntry.hotel, "999", mEntry.iconRes, Card.MIFARE_HOSPITALITY, Card.TYPE_MIFARE_CLASSIC, order);			
			mMyDbHelper.addCard(vc, getDeviceId());
			
			// Store the VCEntry Identifier
			mVcEntry = vc.getIdVc();
			
			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_PERSONALIZING, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
    		
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZING);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
			
	        // Update action
	        mAction = ACTION_ADD_AND_UPDATE_MDAC_VC;
	        
	        String vcMFPassValue = "";
	        
	        switch(mEntry.id) {
			case 0:
				vcMFPassValue = vcMFPassValue_id0;
				break;
			case 1:
				vcMFPassValue = vcMFPassValue_id1;
				break;
			case 2:
				vcMFPassValue = vcMFPassValue_id2;
				break;
			}
	        
			AsyncTask<?, ?, ?> task = new AddAndUpdateMDACTask(HyattActivity.this, vc.getIdVc(), vcMFPassValue).execute();
			setRunningAsyncTask(task);
			
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
    
    private void processStatusMdac(final byte[] result) {
    	Intent broadcast = new Intent();
					
		short status = Parsers.getSW(result);
			
		switch (status) {
		case StatusBytes.SW_NO_ERROR:
			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_ACTIVATING, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
    		
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_ACTIVATING);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
	        
	        mAction = ACTION_ACTIVATE_VC;
			
			AsyncTask<?, ?, ?> task = new ActivateVCTask(HyattActivity.this, mVcEntry).execute(); 
			setRunningAsyncTask(task);
					
			break;
		default:
			Toast.makeText(getApplicationContext(),
					"Error Occured populating MDAC value",
					Toast.LENGTH_LONG).show();
			
			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_FAILED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
    		
    		broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
			
	        Toast.makeText(HyattActivity.this, "MDAC FAIL", Toast.LENGTH_LONG).show();
	        
			break;
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
//				Toast.makeText(getApplicationContext(), "Card activated",
//						Toast.LENGTH_LONG).show();
				
				broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
		        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
		        sendBroadcast(broadcast);
				
				// Alert the user about the card creation
				MyNotification.show(HyattActivity.this, 
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
			
		case ACTION_ADD_AND_UPDATE_MDAC_VC:
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_FAILED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
    		break;
    		
		case ACTION_ACTIVATE_VC:
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
    		break;
    	}		
	}
}
