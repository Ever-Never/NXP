package com.nxp.nxpwalletconndev.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.activities.HyattActivity.HotelEntry;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.databases.MyDbHelper;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.loaderservice.SmartcardLoaderServiceResponse;
import com.nxp.nxpwalletconndev.notifications.MyNotification;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.tasks.ActivateVCTask;
import com.nxp.nxpwalletconndev.tasks.AddAndUpdateMDACTask;
import com.nxp.nxpwalletconndev.tasks.CreateVCTask;
import com.nxp.nxpwalletconndev.tasks.ExecuteScriptTask;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.nxpwalletconndev.utils.StatusBytes;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothConnectListener;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothReadListener;

public class TransportKioskActivity extends BaseActivity implements OnBluetoothConnectListener, OnBluetoothReadListener, OnOperationListener, OnTransmitApduListener {
	public static final String TAG = "MainActivity";
	
	public PurchaseSubEntry mEntry = null;
    public int mAction = 0;
    public int mVcEntry;
    private String mScript;
	
    public static final String scriptsOutputFolder = Environment.getExternalStorageDirectory() + "/loaderserviceconndev/";

	public static final int ACTION_EXECUTE_SCRIPT = 0;
	public static final int ACTION_CREATE_VC = 1;
	public static final int ACTION_ADD_AND_UPDATE_MDAC_VC = 2;
	public static final int ACTION_ACTIVATE_VC = 3;

	public static final int TAIPEI_CARD = 0;
	public static final int BOSTON_CARD = 1;
	public static final int SAN_DIEGO_CARD = 2;
	public static final int VALENCIA_CARD = 3;
	
	public static final int TEMP_CARD_ID = 0xF1;

	private String CreateVCData = "460101A50702020101030100A60705020400060108A11F80080FFFFFFFFFFFFFFF810100820200008301008401008501008603000000A8122010FFFFFFFFFFFFFF078069FFFFFFFFFFFF";
	private String vcMFPassValue_Taipei = "79DCC20AF9C44902";
	private String vcMFPassValue_Boston = "9E8A34A19C1F01F3";
	private String vcMFPassValue_SanDiego = "1A083E5F722F0FD3";
	
	private ListView list;

	private MyDbHelper mMyDbHelper;
	
	/*public static class PurchaseEntry {
		public int id;
		public String city;
		

		public PurchaseEntry(final int id, final String city) {
			this.id = id;
			this.city = city;
		}
	};*/

	public static class PurchaseSubEntry {
		public int id;
		public String city;
		public String name;
		public int trips;
		public String price;
		public int icon;
		
		public String loadAppletPath;

		public PurchaseSubEntry(final int id, final String city, final String name, final int trips,
				final String price, final String loadPath, final int icon) {
			this.id = id;
			this.city = city;
			this.name = name;
			this.trips = trips;
			this.price = price;
			this.icon = icon;
			
			this.loadAppletPath = loadPath;
		}
	};
	
	//private List<PurchaseEntry> mPurchases;
	private List<PurchaseSubEntry> mSubPurchasess = new ArrayList<PurchaseSubEntry>();
		
	private List<HotelEntry> mHotels = new ArrayList<HotelEntry>();
			
	private ArrayAdapter<PurchaseSubEntry> mPurchaseAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_transportkiosk);
		
		// list with all the applets downloaded
		
		if(forAmotech)
		{
			mSubPurchasess.add(new PurchaseSubEntry(TAIPEI_CARD, "Taipei", "Taipei-1", 1, "0.95$", "load_perso_taipei1_amotech_encrypted.txt", R.drawable.taipei));
			mSubPurchasess.add(new PurchaseSubEntry(TAIPEI_CARD, "Taipei", "Taipei-8", 8, "6.95$", "load_perso_taipei8_amotech_encrypted.txt", R.drawable.taipei));
			mSubPurchasess.add(new PurchaseSubEntry(TAIPEI_CARD, "Taipei", "Taipei-30", 30, "20.95$", "load_perso_taipei30_amotech_encrypted.txt", R.drawable.taipei));
			
			mSubPurchasess.add(new PurchaseSubEntry(BOSTON_CARD, "Boston", "Boston-1", 1, "1.95$", "load_perso_boston1_amotech_encrypted.txt", R.drawable.boston));
			mSubPurchasess.add(new PurchaseSubEntry(BOSTON_CARD, "Boston", "Boston-10", 10, "16.95$", "load_perso_boston10_amotech_encrypted.txt", R.drawable.boston));
			
			mSubPurchasess.add(new PurchaseSubEntry(SAN_DIEGO_CARD, "San Diego", "San Diego-1", 1, "2.95$", "load_perso_sandiego1_amotech_encrypted.txt", R.drawable.sandiego));
			mSubPurchasess.add(new PurchaseSubEntry(SAN_DIEGO_CARD, "San Diego", "San Diego-10", 10, "19.95$", "load_perso_sandiego10_amotech_encrypted.txt", R.drawable.sandiego));
		}
		else
		{
			mSubPurchasess.add(new PurchaseSubEntry(TAIPEI_CARD, "Taipei", "Taipei-1", 1, "0.95$", "load_perso_taipei1_encrypted.txt", R.drawable.taipei));
			mSubPurchasess.add(new PurchaseSubEntry(TAIPEI_CARD, "Taipei", "Taipei-8", 8, "6.95$", "load_perso_taipei8_encrypted.txt", R.drawable.taipei));
			mSubPurchasess.add(new PurchaseSubEntry(TAIPEI_CARD, "Taipei", "Taipei-30", 30, "20.95$", "load_perso_taipei30_encrypted.txt", R.drawable.taipei));
			
			mSubPurchasess.add(new PurchaseSubEntry(BOSTON_CARD, "Boston", "Boston-1", 1, "1.95$", "load_perso_boston1_encrypted.txt", R.drawable.boston));
			mSubPurchasess.add(new PurchaseSubEntry(BOSTON_CARD, "Boston", "Boston-10", 10, "16.95$", "load_perso_boston10_encrypted.txt", R.drawable.boston));
			
			mSubPurchasess.add(new PurchaseSubEntry(SAN_DIEGO_CARD, "San Diego", "San Diego-1", 1, "2.95$", "load_perso_sandiego1_encrypted.txt", R.drawable.sandiego));
			mSubPurchasess.add(new PurchaseSubEntry(SAN_DIEGO_CARD, "San Diego", "San Diego-10", 10, "19.95$", "load_perso_sandiego10_encrypted.txt", R.drawable.sandiego));	
		}
		
		
//		mPurchases.add(new PurchaseEntry(VALENCIA_CARD, "Valencia"));
//		mSubPurchases.add(new PurchaseSubEntry(VALENCIA_CARD, "Valencia", "Valencia-10", 10, "9.95$", "load_perso_boston10_encrypted.txt", R.drawable.valencia));
		
		list = (ListView) findViewById(R.id.layoutCardsPurchase);
		
		// Get database helper
		mMyDbHelper = new MyDbHelper(this);
	}

	@Override
	protected void onResume() {
		displayApplets();

		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	protected void displayApplets() {
		list = (ListView) findViewById(R.id.layoutCardsPurchase);
		mPurchaseAdapter = new ArrayAdapter<PurchaseSubEntry>(this,
				R.layout.purchase, mSubPurchasess) {
			@Override
			public View getView(final int position, final View convertView,
					final ViewGroup parent) {
				final PurchaseSubEntry entry = getItem(position);

				View root;

				if (convertView == null) {
					final LayoutInflater inflater = LayoutInflater
							.from(TransportKioskActivity.this);

					root = inflater.inflate(R.layout.ticketing,	parent, false);
					Log.d("Checkpoint", "convertView");
					
				}else {
					Log.d("Checkpoint", "Else");
					root = convertView;
				}
				
				((ImageView) root.findViewById(R.id.city_icon))
				.setImageResource(entry.icon);
			
				((TextView) root.findViewById(R.id.city_name))
				.setText(String.valueOf(entry.name));
			
				((TextView) root.findViewById(R.id.number_of_tickets))
				.setText(String.valueOf(entry.trips));
				
				((TextView) root.findViewById(R.id.ticket_price))
				.setText(String.valueOf(entry.price));
				
				root.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {					
						// Use the Builder class for convenient dialog construction
				        AlertDialog.Builder builder = new AlertDialog.Builder(TransportKioskActivity.this);
				        builder.setTitle(getResources().getString(R.string.purchase) + " " + entry.name + " " + getResources().getString(R.string.card_purchase));
				        builder.setMessage(getResources().getString(R.string.sure));
				        
				        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			                   public void onClick(DialogInterface dialog, int id) {
			                	   if(isDeviceConnected() == true) {
			                		   	if(MyPreferences.isCardOperationOngoing(TransportKioskActivity.this) == false) {
					                	   	// Set the creation of a fake card to let the user see Creating Card
				                		   	Card vc = new Card(0, 0, TEMP_CARD_ID, "", "", "", "", "", Card.STATUS_CREATING, false, false, R.drawable.card_blank, Card.MIFARE_TICKETING, Card.TYPE_MIFARE_CLASSIC, 100);			
					           				mMyDbHelper.addCard(vc, getDeviceId());
					           				
					           				// Alert the user about the card creation
					    					MyNotification.show(TransportKioskActivity.this, 
					    							getResources().getString(R.string.notif_card_creating_title), getResources().getString(R.string.notif_card_creating_msg),
					    							MyNotification.NOTIF_ID_OPERATING);
					    					
					    					Intent broadcast = new Intent();
					    					broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_CREATING);
					    			        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
					    			        sendBroadcast(broadcast);
					    					
					    					// The script to be launched
					    					String script = entry.loadAppletPath;
					    					
					    					mAction = ACTION_EXECUTE_SCRIPT;
					    					
					    					mScript = script;
					    					mEntry = entry;

					    					AsyncTask<?, ?, ?> task = new ExecuteScriptTask(TransportKioskActivity.this, script).execute();
					           				setRunningAsyncTask(task);
					    					
					    					finish();
			                		   	} else
			                		   		Toast.makeText(TransportKioskActivity.this, "Operation not possible while card is under process", Toast.LENGTH_LONG).show();
			                	   } else
				                 	   Toast.makeText(TransportKioskActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
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

		list.setAdapter(mPurchaseAdapter);
	}
	
	@Override
    public void processOperationResult(final byte[] result) {
		if(result != null) {
	    	switch (mAction) {
			case ACTION_EXECUTE_SCRIPT:
				processStatusScript(result);
				break;
	    	
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
			Toast.makeText(TransportKioskActivity.this, "Error detected in the BLE channel", Toast.LENGTH_LONG).show();
		}
    }

	private void addAndUpdateMDAC(PurchaseSubEntry entry, int vcEntry) {
		mAction = ACTION_ADD_AND_UPDATE_MDAC_VC;
		
		String vcMFPassValue = "";
		
		switch(entry.id) {
		case 0:
			vcMFPassValue = vcMFPassValue_Taipei;
			break;
		case 1:
			vcMFPassValue = vcMFPassValue_Boston;
			break;
		case 2:
			vcMFPassValue = vcMFPassValue_SanDiego;
			break;
		}
		
		AsyncTask<?, ?, ?> task = new AddAndUpdateMDACTask(TransportKioskActivity.this, vcEntry, vcMFPassValue).execute();
		setRunningAsyncTask(task);
	}
	
	public void processStatusScript(byte[] mBufferDataCmd) {	
		int bufferLength = mBufferDataCmd.length;
		
		String resp = mScript.replace(".txt", "_" + String.valueOf(getDeviceId()) + "_ConnDevOutput.txt");
		
		// Set the name for the output String
		String out = scriptsOutputFolder + resp;
		SmartcardLoaderServiceResponse.writeOutputFile(TransportKioskActivity.this, out, new String(mBufferDataCmd));
		    		
		if (mBufferDataCmd[bufferLength - 2] == '0' && mBufferDataCmd[bufferLength - 3] == '0' 
					&& mBufferDataCmd[bufferLength - 4] == '0' && mBufferDataCmd[bufferLength - 5] == '9') {
 			Intent broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZING);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
	        
	        // Set the action
	     	mAction = ACTION_CREATE_VC;
 			
 			// Now let's go for the card creation
	        AsyncTask<?, ?, ?> task = new CreateVCTask(TransportKioskActivity.this, CreateVCData, null).execute();
			setRunningAsyncTask(task);
        } else {
        	Toast.makeText(TransportKioskActivity.this, "Applet load failed", Toast.LENGTH_LONG).show();
    		
    		Intent broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
	        
	        // The execution failed
			mMyDbHelper.deleteCard(TEMP_CARD_ID, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
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
			Card vc = Parsers.getVcEntry(result, mEntry.city, String.valueOf(mEntry.trips), mEntry.icon, Card.MIFARE_TICKETING, Card.TYPE_MIFARE_CLASSIC, order);			
			mMyDbHelper.addCard(vc, getDeviceId());

			// Store the VCEntry Identifier
			mVcEntry = vc.getIdVc();

			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_PERSONALIZING, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
    		
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZING);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
	        
			addAndUpdateMDAC(mEntry,mVcEntry);

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
			
			AsyncTask<?, ?, ?> task = new ActivateVCTask(TransportKioskActivity.this, mVcEntry).execute(); 
			setRunningAsyncTask(task);
			
			break;
		default:
			Toast.makeText(getApplicationContext(),
					"Error occurred adding MDAC",
					Toast.LENGTH_LONG).show();	
			
			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_FAILED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
			
    		broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
			
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
//					Toast.makeText(getApplicationContext(), "Card activated",
//							Toast.LENGTH_LONG).show();
				
				broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
		        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
		        sendBroadcast(broadcast);
				
				// Alert the user about the card creation
				MyNotification.show(TransportKioskActivity.this, 
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

	private void showPurchaseDialog(final PurchaseSubEntry s) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(
				TransportKioskActivity.this);
		builder.setTitle(getResources().getString(R.string.purchase) + " " + s.name + " " + getResources().getString(R.string.card_purchase));
		builder.setMessage(getResources().getString(R.string.sure));
		
		builder.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if(isDeviceConnected() == true) {
					// Set the creation of a fake card to let the user see Creating Card
	   				Card vc = new Card(0, 0, TEMP_CARD_ID, "", "", "", "", "", Card.STATUS_CREATING, false, false, R.drawable.card_blank, Card.MIFARE_TICKETING, Card.TYPE_MIFARE_CLASSIC, 100);			
	   				mMyDbHelper.addCard(vc, getDeviceId());
					
	   				// Alert the user about the card creation
					MyNotification.show(TransportKioskActivity.this, 
							getResources().getString(R.string.notif_card_creating_title), getResources().getString(R.string.notif_card_creating_msg),
							MyNotification.NOTIF_ID_OPERATING);
					
					Intent broadcast = new Intent();
					broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_CREATING);
			        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
			        sendBroadcast(broadcast);
					
					// The script to be launched
					String script = s.loadAppletPath;
					
					mAction = ACTION_EXECUTE_SCRIPT;
					
					mScript = script;
					mEntry = s;
					
					AsyncTask<?, ?, ?> task = new ExecuteScriptTask(TransportKioskActivity.this, script).execute();
       				setRunningAsyncTask(task);
	
					finish();
				} else
              	   Toast.makeText(TransportKioskActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
			}
		});

		builder.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
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
    	case ACTION_EXECUTE_SCRIPT:
			mMyDbHelper.deleteCard(TEMP_CARD_ID, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
			break;
    	
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
