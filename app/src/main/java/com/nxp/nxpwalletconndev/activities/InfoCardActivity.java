package com.nxp.nxpwalletconndev.activities;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.adapters.MyTxAdapter;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.classes.Transaction;
import com.nxp.nxpwalletconndev.databases.MyDbHelper;
import com.nxp.nxpwalletconndev.listeners.OnActivateResultListener;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnReadRecordResultListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.tasks.ActivateCreditCardTask;
import com.nxp.nxpwalletconndev.tasks.ActivateVCTask;
import com.nxp.nxpwalletconndev.tasks.CreateVCTask;
import com.nxp.nxpwalletconndev.tasks.DeactivateVCTask;
import com.nxp.nxpwalletconndev.tasks.ReadMifareDataTask;
import com.nxp.nxpwalletconndev.tasks.ReadTransactionLogTask;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.nxpwalletconndev.utils.StatusBytes;

public class InfoCardActivity extends BaseActivity implements OnActivateResultListener, OnOperationListener, OnReadRecordResultListener, OnTransmitApduListener {
	public static final String EXTRA_CARD_ID = "extra_card_id";
	public static final String EXTRA_CARD_TYPE = "extra_card_type";

	private MyDbHelper mMyDbHelper;
	
	private TextView textCless; 
	private ImageView imageCless;
	
	public int cardStatus;
	public boolean isCardDefault;
	public int mId;
	public int mType;

	private ListView mList;
	private MyTxAdapter mAdapter;
	
	private ArrayList<Transaction> mTxs;
	
	private final static int ACTION_ACTIVATE = 10;
	private final static int ACTION_READ_TXS = 11;
	public static final int ACTION_ACTIVATE_VC = 21;
	public static final int ACTION_DEACTIVATE_VC = 22;
	public static final int ACTION_READ_MIFARE_DATA = 23;
	private int mAction = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info_card);

		// Get database helper
		mMyDbHelper = new MyDbHelper(this);

		mId = getIntent().getIntExtra(EXTRA_CARD_ID, 0);
		mType = getIntent().getIntExtra(EXTRA_CARD_TYPE, 0);
		
		showCardInfo();
		
		// Get the reference to the listview and adapter
		mList = (ListView) findViewById(R.id.list_transactions);
		
		mTxs = new ArrayList<Transaction>();
		mAdapter = new MyTxAdapter(this, mTxs);
		mList.setAdapter(mAdapter);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
			
		IntentFilter filter = new IntentFilter();
	    filter.addAction(MyCardsActivity.BROADCAST_ACTION);
	    registerReceiver(receiver, filter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		unregisterReceiver(receiver);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.card, menu);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_refresh) {
			// Show the transaction log only when I initialize the app
			if(mType == Card.TYPE_PAYMENTS)
				showTransactionsInfo();
			else if(mType == Card.TYPE_MIFARE_CLASSIC)
				readMifareData();
			
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onActivateResult(boolean result, boolean activate, int id) {
		// Set the activating status
		mMyDbHelper.updateCardStatus(id, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_PAYMENTS);
		
		if(result == true) {
			mMyDbHelper.removeFav(getDeviceId(), Card.TYPE_PAYMENTS);
			
			if(activate == true) {
				mMyDbHelper.makeCardFav(id, getDeviceId(), Card.TYPE_PAYMENTS);
				
				// Update the info on the screen
				imageCless.setBackgroundResource(R.drawable.cless_active);
				textCless.setText(getResources().getString(R.string.card_cless_active));
		       	
//		       	Toast.makeText(InfoCardActivity.this, getResources().getString(R.string.applet_activated), Toast.LENGTH_LONG).show();
			} else {
				// Update the info on the screen
				imageCless.setBackgroundResource(R.drawable.cless_notactive);
				textCless.setText(getResources().getString(R.string.card_cless_not_active));
		       	
//		       	Toast.makeText(InfoCardActivity.this, getResources().getString(R.string.applet_deactivated), Toast.LENGTH_LONG).show();
			}
			
			Intent broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
			
			// Store the activation status
			isCardDefault = activate;
		} else {
			Toast.makeText(InfoCardActivity.this, getResources().getString(R.string.error_applet_activated), Toast.LENGTH_LONG).show();			
		}
	}
	
	private void readMifareData() {
		if(isDeviceConnected()) {
			if(MyPreferences.isCardOperationOngoing(InfoCardActivity.this) == false) {
				// Update action
				mAction = ACTION_READ_MIFARE_DATA;
				
				// We start reading the content
				((TextView) findViewById(R.id.loading_content)).setVisibility(View.VISIBLE);
				
				mMyDbHelper.updateCardStatus(mId, Card.STATUS_READING, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
				
				// read the content
				AsyncTask<?, ?, ?> task = new ReadMifareDataTask(InfoCardActivity.this, mId).execute();
   				setRunningAsyncTask(task);
			} else
				Toast.makeText(InfoCardActivity.this, "Operation not possible while card is under process", Toast.LENGTH_LONG).show();
		} else
     	   Toast.makeText(InfoCardActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
	}
	
	private void showTransactionsInfo() {
		Card card = mMyDbHelper.getCard(mId, getDeviceId(), Card.TYPE_PAYMENTS);
		
		if(isDeviceConnected()) {
			if(MyPreferences.isCardOperationOngoing(InfoCardActivity.this) == false) {
				mAction = ACTION_READ_TXS;
				
				// We start reading the content
				((TextView) findViewById(R.id.loading_content)).setVisibility(View.VISIBLE);
				
				// Reset the transaction list
				mTxs.clear();
				
				mMyDbHelper.updateCardStatus(mId, Card.STATUS_READING, getDeviceId(), Card.TYPE_PAYMENTS);
				
				AsyncTask<?, ?, ?> task = new ReadTransactionLogTask(InfoCardActivity.this, card).execute(mId);
   				setRunningAsyncTask(task);
			} else
				Toast.makeText(InfoCardActivity.this, "Operation not possible while card is under process", Toast.LENGTH_LONG).show();
		} else
     	   Toast.makeText(InfoCardActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
	}
	
	public void showCardInfo() {
		if(isDeviceConnected() == false)
			return;
		
		final Card card = mMyDbHelper.getCard(mId, getDeviceId(), mType);
		
		((ImageView) findViewById(R.id.card_image)).setImageResource(card.getIconRsc());
		((TextView) findViewById(R.id.card_name)).setText(card.getCardName());
				
		if(card.getStatus() == Card.STATUS_READING)
			((TextView) findViewById(R.id.loading_content)).setVisibility(View.VISIBLE);
		else
			((TextView) findViewById(R.id.loading_content)).setVisibility(View.GONE);
		
		if(mType == Card.TYPE_PAYMENTS) {
			((TextView) findViewById(R.id.card_number)).setText("XXXX XXXX XXXX " + card.getCardNumber().substring(card.getCardNumber().length() - 4));
			((TextView) findViewById(R.id.card_exp_date)).setText(card.getCardExpMonth() + " / " + card.getCardExpYear());
			((TextView) findViewById(R.id.user_name)).setText(MyPreferences
					.getUserName(getApplicationContext()).toUpperCase() + " " + MyPreferences.getUserSurname(getApplicationContext()).toUpperCase());
		} else if(mType == Card.TYPE_MIFARE_CLASSIC) {
			if(card.getMifareType() == Card.MIFARE_HOSPITALITY) {
	        	if(card.getCardNumber().equals("999") == true)
	        		((TextView) findViewById(R.id.card_room)).setText("To complete check in please tap your card against a check-in post!");
	        	else
	        		((TextView) findViewById(R.id.card_room)).setText("Room: " + card.getCardNumber());
	        } else if(card.getMifareType() == Card.MIFARE_LOYALTY) {
	        	if(card.getCardNumber().equals("999") == true)
	        		((TextView) findViewById(R.id.card_room)).setText("Error reading points");
	        	else
	        		((TextView) findViewById(R.id.card_room)).setText("Points: " + card.getCardNumber());
	        } else if(card.getMifareType() == Card.MIFARE_TICKETING) {
	        	if(card.getCardNumber().equals("999") == true)
	        		((TextView) findViewById(R.id.card_room)).setText("Error reading trips");
	        	else
	        		((TextView) findViewById(R.id.card_room)).setText("Trips: " + card.getCardNumber());
	        }
		} else if(mType == Card.TYPE_MIFARE_DESFIRE) {
			
		}
		
		textCless = (TextView) findViewById(R.id.cless_text);
		imageCless = (ImageView) findViewById(R.id.cless_image);
		
		isCardDefault = card.isFav();
		cardStatus = card.getStatus();
		
		if(cardStatus == Card.STATUS_PERSONALIZED || cardStatus == Card.STATUS_READING) {
			if(isCardDefault == true) {
				imageCless.setBackgroundResource(R.drawable.cless_active);
				textCless.setText(getResources().getString(R.string.card_cless_active));
			} else {
				imageCless.setBackgroundResource(R.drawable.cless_notactive);
				textCless.setText(getResources().getString(R.string.card_cless_not_active));
			}
		} else if(cardStatus == Card.STATUS_ACTIVATING) {
			imageCless.setBackgroundResource(R.drawable.cless_activating);
			textCless.setText(getResources().getString(R.string.card_cless_activating));
		}
		
		imageCless.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isDeviceConnected()) {
					if(MyPreferences.isCardOperationOngoing(InfoCardActivity.this) == false) {

						if(mType == Card.TYPE_PAYMENTS) {
							mAction = ACTION_ACTIVATE;
							mMyDbHelper.updateCardStatus(mId, Card.STATUS_ACTIVATING, getDeviceId(), Card.TYPE_PAYMENTS);
							
							AsyncTask<?, ?, ?> task = new ActivateCreditCardTask(InfoCardActivity.this, isCardDefault ? false : true).execute(mId);
	           				setRunningAsyncTask(task);
						} else if(mType == Card.TYPE_MIFARE_CLASSIC || mType == Card.TYPE_MIFARE_DESFIRE) {
							mMyDbHelper.updateCardStatus(mId, Card.STATUS_ACTIVATING, getDeviceId(), mType);
								
							if(isCardDefault) {
								mAction = ACTION_DEACTIVATE_VC;

						    	AsyncTask<?, ?, ?> task = new DeactivateVCTask(InfoCardActivity.this, mId).execute();
		           				setRunningAsyncTask(task);
							} else {
								mAction = ACTION_ACTIVATE_VC;
								
								AsyncTask<?, ?, ?> task = new ActivateVCTask(InfoCardActivity.this, mId).execute();
		           				setRunningAsyncTask(task);
							}
						}
						
						// Update the icon
						if(isCardDefault) {
							imageCless.setBackgroundResource(R.drawable.cless_activating);
							textCless.setText(getResources().getString(R.string.card_cless_deactivating));
						}
						else{
							imageCless.setBackgroundResource(R.drawable.cless_activating);
							textCless.setText(getResources().getString(R.string.card_cless_activating));
						}
						
					} else
						Toast.makeText(InfoCardActivity.this, "Operation not possible while card is under process", Toast.LENGTH_LONG).show();
				} else
             	   Toast.makeText(InfoCardActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
			}
		});
	}
	
	public void processOperationResult(byte[] mBufferDataCmd) {
		if(mBufferDataCmd != null) {
			switch (mAction) {
			case ACTION_ACTIVATE:
				ActivateCreditCardTask.receiveApduFromSE(mBufferDataCmd);
				break;
			case ACTION_READ_TXS:
				ReadTransactionLogTask.receiveApduFromSE(mBufferDataCmd);
				break;
				
			case ACTION_ACTIVATE_VC:
				processStatusActivate(mBufferDataCmd);
				break;
				
			case ACTION_DEACTIVATE_VC:
				processStatusDeactivate(mBufferDataCmd);
				break;
				
			case ACTION_READ_MIFARE_DATA:
				processStatusReadMifareData(mBufferDataCmd);
				break;
			}
		} else {
			Toast.makeText(InfoCardActivity.this, "Error detected in the BLE channel", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void sendApduToSE(byte[] dataBT, int timeout) {
		// Send the data via Bluetooth
        writeBluetooth(dataBT, timeout);
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	// Update info on the screen
        	showCardInfo();
        }
    };

	@Override
	public void onReadRecordResult(ArrayList<Transaction> txs, int id) {
		// We are done reading the content
		((TextView) findViewById(R.id.loading_content)).setVisibility(View.GONE);
		
		for(Transaction t : txs)
			mTxs.add(t);
		
		Intent broadcast = new Intent();
		broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
        sendBroadcast(broadcast);
        
        // Update the status
        mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_PAYMENTS);
		
		// Update the list of transactions on the screen
		mAdapter.notifyDataSetChanged();
	}
	
	private void processStatusActivate(final byte[] result) {
		Intent broadcast = new Intent();
		broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
        sendBroadcast(broadcast);

		// Set the activating status
		mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
					
		if(result != null) {
			short status = Parsers.getSW(result);
			
			switch (status) {
			case StatusBytes.SW_NO_ERROR:
//				Toast.makeText(getApplicationContext(), "Card activated",
//						Toast.LENGTH_LONG).show();
				
				mMyDbHelper.removeFav(getDeviceId(), mType);
				mMyDbHelper.makeCardFav(mId, getDeviceId(), mType);
				
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
	    
    private void processStatusDeactivate(final byte[] result) {
		// Set the activating status
		mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
		
		Intent broadcast = new Intent();
		broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
        sendBroadcast(broadcast);

		if(result != null) {
			short status = Parsers.getSW(result);
	
			switch (status) {
			case StatusBytes.SW_NO_ERROR:
//				Toast.makeText(getApplicationContext(), "Card deactivated",
//						Toast.LENGTH_LONG).show();
				
				mMyDbHelper.removeCardFav(mId, getDeviceId(), mType);
											
				break;
			default:
				Toast.makeText(getApplicationContext(),
						"Error deactivating card", Toast.LENGTH_LONG).show();
				break;
			}
		} else {
			Toast.makeText(getApplicationContext(), "Bluetooth connection not established", Toast.LENGTH_LONG).show();
		}
    }
    
    private void processStatusReadMifareData(final byte[] result) {
		Intent broadcast = new Intent();
		broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
        sendBroadcast(broadcast);
		
		// Set the activating status
		mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
		
		// We are done reading the content
		((TextView) findViewById(R.id.loading_content)).setVisibility(View.GONE);
		
		if(result != null) {
			Card card = mMyDbHelper.getCard(mId, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);

			if(card.getMifareType() == Card.MIFARE_HOSPITALITY)
				processReadMifareHospitality(card, result);
			else if(card.getMifareType() == Card.MIFARE_LOYALTY)
				processReadMifareLoyalty(card, result);
			else if(card.getMifareType() == Card.MIFARE_TICKETING)
				processReadMifareTicketing(card, result);
			
		} else {
			Toast.makeText(getApplicationContext(), "Bluetooth connection not established", Toast.LENGTH_LONG).show();
		}
    }
    
    private void processReadMifareHospitality(Card card, final byte[] result) {
    	short status = Parsers.getSW(result);
    	
		switch (status) {
		case StatusBytes.SW_NO_ERROR:
			byte[] MifareData = Parsers.getMifareData(result);

//			int lengthName = 0;
//			for (int i = 16; i < 32; i++) {
//				if (MifareData[i] == 0x00) {
//					lengthName = i;
//					break;
//				}
//			}

			int lengthRoom = 0;
			for (int i = 32, j = 0; i < 48; i++, j++) {
				if (MifareData[i] == 0x00) {
					lengthRoom = j;
					break;
				}
			}
			
			if(lengthRoom > 0) {
				String room = new String(MifareData, 32, lengthRoom);
				card.setCardNumber(room);
				
				// Show the room to the user
				((TextView) findViewById(R.id.card_room)).setText("Room: " + card.getCardNumber());
				
				// Store the room number in the database
				mMyDbHelper.updateCardNumber(mId, room, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
			} else {
				Toast.makeText(getApplicationContext(),
						"Please personalize your card", Toast.LENGTH_LONG)
						.show();
			}
			
			break;
		case StatusBytes.AUTHENTICATION_ERROR:
			Toast.makeText(getApplicationContext(),
					"Please personalize your card", Toast.LENGTH_LONG)
					.show();
			break;
		default:
			Toast.makeText(getApplicationContext(),
					"Error Occurred retrieving room number", Toast.LENGTH_LONG)
					.show();
			break;
		}
    }
    
    private void processReadMifareLoyalty(Card card, final byte[] result) {
    	short status = Parsers.getSW(result);
    	
		switch (status) {
		case StatusBytes.SW_NO_ERROR:
			byte[] MifareData = Parsers.getMifareData(result);
			
			// This is for the new memory map
			int points = ((MifareData[17] << 8) & 0xff00) + (MifareData[16] & 0x00ff);
			
			// Update the trips for further reading
			card.setCardNumber(String.valueOf(points));
			
			// Show the room to the user
			((TextView) findViewById(R.id.card_room)).setText("Points: " + card.getCardNumber());
			
			// Store the room number in the database
			mMyDbHelper.updateCardNumber(mId, String.valueOf(points), getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
			
			break;
		default:
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_card_read_points), Toast.LENGTH_LONG).show();
			break;
		}
    }
    
    private void processReadMifareTicketing(Card card, final byte[] result) {
    	short status = Parsers.getSW(result);
    	
		switch (status) {
		case StatusBytes.SW_NO_ERROR:
			byte[] MifareData = Parsers.getMifareData(result);
			
			// This is for the new memory map
			final int trips = MifareData[16];
			
			// Update the trips for further reading
			card.setCardNumber(String.valueOf(trips));
			
			// Show the room to the user
			((TextView) findViewById(R.id.card_room)).setText("Trips: " + card.getCardNumber());
			
			// Store the room number in the database
			mMyDbHelper.updateCardNumber(mId, String.valueOf(trips), getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
			
			break;
		default:
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_card_read_trips), Toast.LENGTH_LONG).show();
			break;
		}
    }

	@Override
	public void processOperationNotCompleted() {	
		Toast.makeText(getApplicationContext(), "InfoCard Error detected in BLE channel", Toast.LENGTH_LONG).show();
		
		// We are done reading the content
		((TextView) findViewById(R.id.loading_content)).setVisibility(View.GONE);
		
		Intent broadcast = new Intent();
		broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
        sendBroadcast(broadcast);

		switch (mAction) {
		case ACTION_READ_MIFARE_DATA:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
			break;
			
		case ACTION_READ_TXS:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
			break;
			
		case ACTION_ACTIVATE:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
			break;
		
		case ACTION_ACTIVATE_VC:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
			break;
			
		case ACTION_DEACTIVATE_VC:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
			break;
		}
		
		final Card card = mMyDbHelper.getCard(mId, getDeviceId(), mType);
		
		textCless = (TextView) findViewById(R.id.cless_text);
		imageCless = (ImageView) findViewById(R.id.cless_image);
		
		isCardDefault = card.isFav();
		
		if(isCardDefault == true) {
			imageCless.setBackgroundResource(R.drawable.cless_active);
			textCless.setText(getResources().getString(R.string.card_cless_active));
		} else {
			imageCless.setBackgroundResource(R.drawable.cless_notactive);
			textCless.setText(getResources().getString(R.string.card_cless_not_active));
		}
	}
}
