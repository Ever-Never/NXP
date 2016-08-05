package com.nxp.nxpwalletconndev.activities;

import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.connections.DataConnection;
import com.nxp.nxpwalletconndev.databases.MyDbHelper;
import com.nxp.nxpwalletconndev.listeners.OnActivateResultListener;
import com.nxp.nxpwalletconndev.listeners.OnFavClickListener;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.loaderservice.SmartcardLoaderServiceResponse;
import com.nxp.nxpwalletconndev.notifications.MyNotification;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.tasks.ActivateCreditCardTask;
import com.nxp.nxpwalletconndev.tasks.ActivateVCTask;
import com.nxp.nxpwalletconndev.tasks.CreateVCTask;
import com.nxp.nxpwalletconndev.tasks.DeactivateVCTask;
import com.nxp.nxpwalletconndev.tasks.DeleteVCTask;
import com.nxp.nxpwalletconndev.tasks.ExecuteScriptTask;
import com.nxp.nxpwalletconndev.tasks.NfcHttpProxyLockTask;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.nxpwalletconndev.utils.StatusBytes;
import com.nxp.nxpwalletconndev.views.LinearLayoutListView;

public class MyCardsActivity extends BaseActivity implements OnFavClickListener, OnActivateResultListener, OnOperationListener, OnTransmitApduListener {
	public static final String TAG = "InfoCardsActivity";

	public static String BROADCAST_ACTION = "com.nxp.nxpwalletconndev.BROADCAST_UPDATE";
	public static String BROADCAST_EXTRA = "com.nxp.nxpwalletconndev.BROADCAST_STATUS";
		
	// We limit the cards that can be created to 5. To increase the number new LS Scripts are needed
	public static final int MAX_CARD_ID = 5;
	
	private final static int ACTION_DELETE = 0;
	private final static int ACTION_LOCK = 1;
	
	private final static int ACTION_EXECUTE_SCRIPT = 10;
	private final static int ACTION_ACTIVATE_CARD = 11;
	private final static int ACTION_BLOCK_CARD = 12;
	
	public static final int ACTION_DELETE_VC = 20;
	public static final int ACTION_ACTIVATE_VC = 21;
	public static final int ACTION_DEACTIVATE_VC = 22;
	
	private int mAction = 0;
	
	public static final int ACTION_SERVICES_PAYMENTS = 0;
	public static final int ACTION_SERVICES_HOSPITALITY = 1;
	public static final int ACTION_SERVICES_TICKETING = 2;
	public static final int ACTION_SERVICES_LOYALTY = 3;
	public static final int ACTION_SERVICES_MYMIFARE = 4;
//	public static final int ACTION_SERVICES_PAYIN = 5;
	public static final int ACTION_SERVICES_OTHERS = 5;
	
	private List<Card> mCards;
	private ListView mList;
	private LinearLayoutListView area1;
	private MyCardsAdapter mAdapter;

	private MyDbHelper mMyDbHelper;
	private int idLock;
	
	// Loader Service script execution paths
	public static final String scriptsOutputFolder = Environment.getExternalStorageDirectory() + "/loaderserviceconndev/";
	public static final String scriptsOutputFile = Environment.getExternalStorageDirectory() + "/loaderserviceconndev/Readme.txt";
	
	public static final String URL_PERSO_SERVLET = "http://www.themobileknowledge.com/Servlets/RemoteMCWPerso/MCWPersoServletMain";
//	public static final String URL_PERSO_SERVLET = "http://192.168.0.13:8080/RemoteMCWPerso/MCWPersoServletMain";
	
	private String mScript;
	private int mId;
	private int mType;
	
	private Button btnCreateCard;
	private LinearLayout llProgressBar;
	
	private ImageView block;
	private ImageView trash;
	
	private TextView textCardsEmpty;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_cards);
		
		btnCreateCard = (Button) findViewById(R.id.create_card);
		llProgressBar = (LinearLayout) findViewById(R.id.progress_create_card);
		
		btnCreateCard.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				showServicesDialog();
			}
		});
		
		// Get database helper
		mMyDbHelper = new MyDbHelper(this);

		mList = (ListView) findViewById(R.id.list_cards);
		
		area1 = (LinearLayoutListView) findViewById(R.id.pane1);
		area1.setOnDragListener(myOnDragListener);
		area1.setListView(mList);
		
		trash = (ImageView) findViewById(R.id.trash);
		trash.setOnDragListener(new MyDragListener());
		
		block = (ImageView) findViewById(R.id.block_p);
		block.setOnDragListener(new MyDragListener());
		
		textCardsEmpty = (TextView) findViewById(R.id.my_cards_empty);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		showCardsInfo();
		
		boolean isCardOperationOngoing = MyPreferences.isCardOperationOngoing(MyCardsActivity.this);
		if(isCardOperationOngoing == true) {
			btnCreateCard.setVisibility(View.GONE);
			llProgressBar.setVisibility(View.VISIBLE);
		} else {
			btnCreateCard.setVisibility(View.VISIBLE);
			llProgressBar.setVisibility(View.GONE);
		}	
		
		IntentFilter filter = new IntentFilter();
	    filter.addAction(MyCardsActivity.BROADCAST_ACTION);
	    registerReceiver(receiver, filter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		unregisterReceiver(receiver);
	}
		
	private void showCardsInfo() {
		int devId = isDeviceConnected() ? getDeviceId() : 0;
		
		mCards = mMyDbHelper.getAllCards(devId);
		mAdapter = new MyCardsAdapter(getApplicationContext(), mCards, this);
		mList.setAdapter(mAdapter);
		
		if(devId == 0) {
			textCardsEmpty.setVisibility(View.GONE);
		} else {
			if(mCards.isEmpty())
				textCardsEmpty.setVisibility(View.VISIBLE);
			else
				textCardsEmpty.setVisibility(View.GONE);
		}			
		
		mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				if(mCards.get(pos).getStatus() == Card.STATUS_PERSONALIZED
						|| mCards.get(pos).getStatus() == Card.STATUS_ACTIVATING
						|| mCards.get(pos).getStatus() == Card.STATUS_READING) {
					Intent i = new Intent(MyCardsActivity.this, InfoCardActivity.class);
					
					if(mCards.get(pos).getType() == Card.TYPE_PAYMENTS)
						i.putExtra(InfoCardActivity.EXTRA_CARD_ID, mCards.get(pos).getIdScript());
					else if(mCards.get(pos).getType() == Card.TYPE_MIFARE_CLASSIC 
							|| mCards.get(pos).getType() == Card.TYPE_MIFARE_DESFIRE)
						i.putExtra(InfoCardActivity.EXTRA_CARD_ID, mCards.get(pos).getIdVc());
					
					i.putExtra(InfoCardActivity.EXTRA_CARD_TYPE, mCards.get(pos).getType());
					startActivity(i);
				} else {
					Toast.makeText(MyCardsActivity.this, "Card under process", Toast.LENGTH_LONG).show();
				}
			}
		});
		
		// Register for actions
//		mList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//			public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
//				final int type = mCards.get(position).getType();
//				final int vcEntry = mCards.get(position).getIdVc();
//				final int index = mCards.get(position).getIdScript();
//				final String name = mCards.get(position).getCardName();
//				final boolean isCardLocked = mCards.get(position).isLocked();
//				
//				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
//						MyCardsActivity.this,
//						android.R.layout.select_dialog_item);
//				
//				arrayAdapter.add(getResources().getString(R.string.delete_card));
//				
//				if(type == Card.TYPE_PAYMENTS) {
//					if(isCardLocked)
//						arrayAdapter.add(getResources().getString(R.string.unlock_card));
//					else
//						arrayAdapter.add(getResources().getString(R.string.lock_card));
//				}
//				
//				AlertDialog contextMenu = new AlertDialog.Builder(
//						MyCardsActivity.this)
//						.setAdapter(arrayAdapter,
//								new DialogInterface.OnClickListener() {
//									@Override
//									public void onClick(DialogInterface dialog,	int which) {
//										switch (which) {
//											case ACTION_DELETE:
//												if(type == Card.TYPE_PAYMENTS)
//													showCardRemoveDialog(name, index);
//												else if(type == Card.TYPE_MIFARE_CLASSIC
//														|| type == Card.TYPE_MIFARE_DESFIRE) {
//													btnCreateCard.setVisibility(View.GONE);
//													llProgressBar.setVisibility(View.VISIBLE);
//
//											    	 // delete the card instance
//											        if(isDeviceConnected()) {
//											     		// Alert the user about the card creation
//											     		MyNotification.show(MyCardsActivity.this, 
//											     			   getResources().getString(R.string.notif_card_deleting_title), getResources().getString(R.string.notif_card_deleting_msg),
//											     			   MyNotification.NOTIF_ID_OPERATING);
//	
//														mAction = ACTION_DELETE_VC;
//												    	mId = vcEntry;
//												    	mType = type;
//												    	
//												    	// Set the new status
//														mMyDbHelper.updateCardStatus(mId, Card.STATUS_DELETING, getDeviceId(), type);
//														
//														new DeleteVCTask(MyCardsActivity.this, vcEntry).execute();
//											        } else
//											        	Toast.makeText(MyCardsActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();   
//													
//													// Update cards status
//							               			showCardsInfo();
//												}
//												
//												break;
//												
//											case ACTION_LOCK:
//												if(isCardLocked)
//													showCardLockDialog(name, index, false);
//												else
//													showCardLockDialog(name, index, true);
//												
//												break;
//											
//											default:
//												break;
//										}
//									}
//								})
//						.create();
//				
//				contextMenu.setTitle(name);
//				contextMenu.show();
//				
//				return true;
//			};
//		});
		
		mList.setOnItemLongClickListener(myOnItemLongClickListener);
	}	
	
	private void showCardLockDialog(final String name, final int id, final boolean lock) {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(MyCardsActivity.this);
        builder.setTitle("Lock Credit Card");
        builder.setMessage(
        		String.format("Are you sure you want to %s " + name + " card?", lock ? "block" : "unblock"));
        
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int index) {
        		dialog.dismiss();
        		
        		// Save the id for further blocking information
        		idLock = id;
        		
        		if(isDeviceConnected()) {
        			if(MyPreferences.isCardOperationOngoing(getApplicationContext()) == false) {
		        		if(DataConnection.isConnected(MyCardsActivity.this) == false) {	
		        			// Store the action to do
		        			mAction = ACTION_BLOCK_CARD;
		        			
		        			try {
		        				AsyncTask<?, ?, ?> task = new NfcHttpProxyLockTask(MyCardsActivity.this, URL_PERSO_SERVLET, String.valueOf(id), lock, "Prof_04").execute();
		           				setRunningAsyncTask(task);
		        			} catch (Exception e) {
		        				e.printStackTrace();
		        					
		        				Log.e(TAG, "Error executing blocking");
		        			}
		        		} else
							showConnectionRequiredDialog();
        			} else
                		Toast.makeText(MyCardsActivity.this, "Operation in progress", Toast.LENGTH_LONG).show();
        		} else
    				Toast.makeText(MyCardsActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
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
	public void onFavClick(int position) {
		if(mCards.get(position).getStatus() == Card.STATUS_PERSONALIZED) {
			if(isDeviceConnected()) {	
				if(MyPreferences.isCardOperationOngoing(MyCardsActivity.this) == false) {
					btnCreateCard.setVisibility(View.GONE);
        			llProgressBar.setVisibility(View.VISIBLE);
										
					if(mCards.get(position).getType() == Card.TYPE_PAYMENTS) {
						// Store the action to do
						mAction = ACTION_ACTIVATE_CARD;
						
						// Store the vcEntry ID
						mId = mCards.get(position).getIdScript();
						mType = Card.TYPE_PAYMENTS;
						
						mMyDbHelper.updateCardStatus(mCards.get(position).getIdScript(), Card.STATUS_ACTIVATING, getDeviceId(), Card.TYPE_PAYMENTS);
						AsyncTask<?, ?, ?> task =  new ActivateCreditCardTask(MyCardsActivity.this, mCards.get(position).isFav() ? false : true).execute(mCards.get(position).getIdScript());
						setRunningAsyncTask(task);
					} else if(mCards.get(position).getType() == Card.TYPE_MIFARE_CLASSIC || mCards.get(position).getType() == Card.TYPE_MIFARE_DESFIRE) {
						mMyDbHelper.updateCardStatus(mCards.get(position).getIdVc(), Card.STATUS_ACTIVATING, getDeviceId(), mCards.get(position).getType());

						// Store the vcEntry ID
						mId = mCards.get(position).getIdVc();
						mType = mCards.get(position).getType();
						
						if(mCards.get(position).isFav()) {
							mAction = ACTION_DEACTIVATE_VC;

							AsyncTask<?, ?, ?> task = new DeactivateVCTask(MyCardsActivity.this, mCards.get(position).getIdVc()).execute();
		       				setRunningAsyncTask(task);
						} else {
							mAction = ACTION_ACTIVATE_VC;

							AsyncTask<?, ?, ?> task = new ActivateVCTask(MyCardsActivity.this, mCards.get(position).getIdVc()).execute();
		       				setRunningAsyncTask(task);
						}
					}

					// Updated values
					mCards = mMyDbHelper.getAllCards(getDeviceId());
					
					// Update info on the screen
					mAdapter.updateCards(mCards);
			       	mAdapter.notifyDataSetChanged();
				} else
					Toast.makeText(MyCardsActivity.this, "Operation not possible while card is under process", Toast.LENGTH_LONG).show();
			} else
				Toast.makeText(MyCardsActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
		} else {
       		Toast.makeText(MyCardsActivity.this, "This Credit Card is not personalized", Toast.LENGTH_LONG).show();
       	}
	}
	
	@Override
	public void onActivateResult(boolean result, boolean activate, int id) {
		// Set the activating status
		mMyDbHelper.updateCardStatus(id, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_PAYMENTS);

		if(result == true) {
			mMyDbHelper.removeFav(getDeviceId(), Card.TYPE_PAYMENTS);
			
			if(activate == true) {
				mMyDbHelper.makeCardFav(id, getDeviceId(), Card.TYPE_PAYMENTS);
//		       	Toast.makeText(MyCardsActivity.this, getResources().getString(R.string.applet_activated), Toast.LENGTH_LONG).show();
			} else {
//				Toast.makeText(MyCardsActivity.this, getResources().getString(R.string.applet_deactivated), Toast.LENGTH_LONG).show();
			}
			
			Intent broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
		} else {
			Intent broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
			
			Toast.makeText(MyCardsActivity.this, getResources().getString(R.string.error_applet_activated), Toast.LENGTH_LONG).show();			
		}
		
		// Updated values
		mCards = mMyDbHelper.getAllCards(getDeviceId());
		
		// Update info on the screen
		mAdapter.updateCards(mCards);
       	mAdapter.notifyDataSetChanged();
	}
		
	public void proccessTransactionTaskResult(StringBuffer result, boolean lock){
		if(result.indexOf("successful") != -1){
			Toast.makeText(this, String.format(getResources().getString(R.string.block_ok), lock ? "block" : "unblock"),
					Toast.LENGTH_LONG).show();
			
			// Remove the card from the database
			mMyDbHelper.lockCard(idLock, lock, getDeviceId());

			mCards = mMyDbHelper.getAllCards(getDeviceId());
			mAdapter.updateCards(mCards);
          	
			// Update info on the screen
			mAdapter.notifyDataSetChanged();
		} else {
			Toast.makeText(this, String.format(getResources().getString(R.string.block_error), lock ? "block" : "unblock"),
					Toast.LENGTH_LONG).show();
		}
	}
	
	private void showServicesDialog() {
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				MyCardsActivity.this,
				android.R.layout.select_dialog_item);

		arrayAdapter.add(getResources().getString(R.string.service_payments));
		arrayAdapter.add(getResources().getString(R.string.service_hospitality));
		arrayAdapter.add(getResources().getString(R.string.service_ticketing));
		arrayAdapter.add(getResources().getString(R.string.service_loyalty));
		arrayAdapter.add(getResources().getString(R.string.service_mymifare));
//		arrayAdapter.add(getResources().getString(R.string.service_payin));
		arrayAdapter.add(getResources().getString(R.string.service_others));
		
		AlertDialog contextMenu = new AlertDialog.Builder(
				MyCardsActivity.this).setAdapter(arrayAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						Intent i = null;
						
						switch (which) {
						case ACTION_SERVICES_PAYMENTS:
							i = new Intent(MyCardsActivity.this, PaymentCardActivity.class);
							startActivity(i);
							
							break;

						case ACTION_SERVICES_HOSPITALITY:
							i = new Intent(MyCardsActivity.this, HyattActivity.class);
							startActivity(i);
							
							break;
							
						case ACTION_SERVICES_TICKETING:
							i = new Intent(MyCardsActivity.this, TransportKioskActivity.class);
							startActivity(i);
							
							break;

						case ACTION_SERVICES_LOYALTY:
							i = new Intent(MyCardsActivity.this, LoyaltyActivity.class);
							startActivity(i);
							
							break;
							
						case ACTION_SERVICES_MYMIFARE:
							i = new Intent(MyCardsActivity.this, MyMifareCreateVCActivity.class);
							startActivity(i);
							
							break;
							
//						case ACTION_SERVICES_PAYIN:
//							i = new Intent(MyCardsActivity.this, PayInActivity.class);
//							startActivity(i);
//							
//							break;
							
						case ACTION_SERVICES_OTHERS:
							Toast.makeText(MyCardsActivity.this, "To be available soon", Toast.LENGTH_LONG).show();
							break;

						default:
							break;
						}
					}
				}).create();

		contextMenu.setTitle("Wallet Services");
		contextMenu.show();
	}

	private void showConnectionRequiredDialog() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(MyCardsActivity.this);
        builder.setTitle(getResources().getString(R.string.internet_error));
        builder.setMessage(getResources().getString(R.string.internet_lock_msg));
        
        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.dismiss();
                   }
               });
 
        // Create the AlertDialog object and return it
        builder.create();
        builder.show();
	}
	
	public void processOperationResult(byte[] mBufferDataCmd) {
		if(mBufferDataCmd != null) {
			switch (mAction) {
			case ACTION_EXECUTE_SCRIPT:
				processStatusScript(mBufferDataCmd);
				
				break;
				
			case ACTION_ACTIVATE_CARD:
				ActivateCreditCardTask.receiveApduFromSE(mBufferDataCmd);
				
				break;
			
			case ACTION_BLOCK_CARD:
				NfcHttpProxyLockTask.receiveApduFromSE(mBufferDataCmd);
				
				break;
				
			case ACTION_DELETE_VC:
				processStatusDelete(mBufferDataCmd);
				break;
				
			case ACTION_ACTIVATE_VC:
				processStatusActivate(mBufferDataCmd);
				break;
				
			case ACTION_DEACTIVATE_VC:
				processStatusDeactivate(mBufferDataCmd);
				break;
			}
		} else {
			Toast.makeText(MyCardsActivity.this, "MyCards Error detected in the BLE channel", Toast.LENGTH_LONG).show();
		}
	}
	
	public void processStatusScript(byte[] mBufferDataCmd) {
		int bufferLength = mBufferDataCmd.length;
		
		btnCreateCard.setVisibility(View.VISIBLE);
		llProgressBar.setVisibility(View.GONE);
		
		String resp = mScript.replace(".txt", "_" + String.valueOf(getDeviceId()) + "_ConnDevOutput.txt");
		
		// Set the name for the output String
		String out = scriptsOutputFolder + resp;
		SmartcardLoaderServiceResponse.writeOutputFile(MyCardsActivity.this, out, new String(mBufferDataCmd));
		    		
		if(mBufferDataCmd != null) {
    		if (mBufferDataCmd[bufferLength - 2] == '0' && mBufferDataCmd[bufferLength - 3] == '0' 
    					&& mBufferDataCmd[bufferLength - 4] == '0' && mBufferDataCmd[bufferLength - 5] == '9') {
    			// Remove the card from the database
               	mMyDbHelper.deleteCard(mId, getDeviceId(), Card.TYPE_PAYMENTS);
               	
     			// Show notification
    			MyNotification.show(MyCardsActivity.this, 
    					getResources().getString(R.string.notif_card_deleted_title), getResources().getString(R.string.notif_card_deleted_msg),
    					MyNotification.NOTIF_ID_COMPLETED);
    			
    			// Remove the card and update the adapter
               	mCards = mMyDbHelper.getAllCards(getDeviceId());
               	mAdapter.updateCards(mCards);
               	
               	// Update info on the screen
               	mAdapter.notifyDataSetChanged();
            } else {
           		Toast.makeText(MyCardsActivity.this, "Card deletion failed", Toast.LENGTH_LONG).show();
            }	
		}
		
		if(mCards.isEmpty())
			textCardsEmpty.setVisibility(View.VISIBLE);
		else
			textCardsEmpty.setVisibility(View.GONE);
	}
	
	private void processStatusDelete(final byte[] result) {
		btnCreateCard.setVisibility(View.VISIBLE);
		llProgressBar.setVisibility(View.GONE);

		if(result != null) {
			short status = Parsers.getSW(result);
			
			switch (status) {
			case StatusBytes.SW_NO_ERROR:
//				Toast.makeText(getApplicationContext(), "Card deleted",
//						Toast.LENGTH_LONG).show();
//				
				// Remove the card from the database
               	mMyDbHelper.deleteCard(mId, getDeviceId(), mType);

     			// Show notification
    			MyNotification.show(MyCardsActivity.this, 
    					getResources().getString(R.string.notif_card_deleted_title), getResources().getString(R.string.notif_card_deleted_msg),
    					MyNotification.NOTIF_ID_COMPLETED);

				break;
			default:
				Toast.makeText(getApplicationContext(),
						"Error deleting card", Toast.LENGTH_LONG).show();
				
				// Since the card wsa not deleted we are back to personalized
				mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
				
				break;
			}
		} else {
			Toast.makeText(getApplicationContext(), "Bluetooth connection not established", Toast.LENGTH_LONG).show();
		}
		
		// Remove the card and update the adapter
       	mCards = mMyDbHelper.getAllCards(getDeviceId());
       	mAdapter.updateCards(mCards);
       	
       	// Update info on the screen
       	mAdapter.notifyDataSetChanged();
       	
       	if(mCards.isEmpty())
			textCardsEmpty.setVisibility(View.VISIBLE);
		else
			textCardsEmpty.setVisibility(View.GONE);
    }

	 private void processStatusActivate(final byte[] result) {
		btnCreateCard.setVisibility(View.VISIBLE);
		llProgressBar.setVisibility(View.GONE);
		
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
				
				Intent broadcast = new Intent();
				broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
		        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
		        sendBroadcast(broadcast);

				break;
			default:
				Toast.makeText(getApplicationContext(),
						"Error activating card", Toast.LENGTH_LONG).show();
				break;
			}
		} else {
			Toast.makeText(getApplicationContext(), "Bluetooth connection not established", Toast.LENGTH_LONG).show();
		}
		
		// Updated values
		mCards = mMyDbHelper.getAllCards(getDeviceId());
		
		// Update info on the screen
		mAdapter.updateCards(mCards);
       	mAdapter.notifyDataSetChanged();
	}
	    
    private void processStatusDeactivate(final byte[] result) {
		btnCreateCard.setVisibility(View.VISIBLE);
		llProgressBar.setVisibility(View.GONE);
		
		// Set the activating status
		mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
		 
		if(result != null) {
			short status = Parsers.getSW(result);
	
			switch (status) {
			case StatusBytes.SW_NO_ERROR:
//				Toast.makeText(getApplicationContext(), "Card deactivated",
//						Toast.LENGTH_LONG).show();
				
				mMyDbHelper.removeCardFav(mId, getDeviceId(), mType);
				
				Intent broadcast = new Intent();
				broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
		        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
		        sendBroadcast(broadcast);
				
				break;
			default:
				Toast.makeText(getApplicationContext(),
						"Error activating card", Toast.LENGTH_LONG).show();
				break;
			}
		} else {
			Toast.makeText(getApplicationContext(), "Bluetooth connection not established", Toast.LENGTH_LONG).show();
		}
		
		// Updated values
		mCards = mMyDbHelper.getAllCards(getDeviceId());
		
		// Update info on the screen
		mAdapter.updateCards(mCards);
       	mAdapter.notifyDataSetChanged();
    }
	
    @Override
	public void sendApduToSE(byte[] dataBT, int timeout) {
		// Send the data via Bluetooth
        writeBluetooth(dataBT, timeout);
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	int status = intent.getIntExtra(MyCardsActivity.BROADCAST_EXTRA, 0);

        	if(status == Card.STATUS_PERSONALIZED || status == Card.STATUS_FAILED) { 
	            btnCreateCard.setVisibility(View.VISIBLE);
				llProgressBar.setVisibility(View.GONE);
        	}
        	
        	// Update info on the screen
        	showCardsInfo();
        }
    };

	@Override
	public void processOperationNotCompleted() {
		Toast.makeText(getApplicationContext(), "Operation not completed - Error detected in BLE channel", Toast.LENGTH_LONG).show();
		
		switch (mAction) {
		case ACTION_BLOCK_CARD:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_PAYMENTS);
			break;
		
		case ACTION_EXECUTE_SCRIPT:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_PAYMENTS);
			break;
			
		case ACTION_ACTIVATE_CARD:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_PAYMENTS);
			break;
			
		case ACTION_DELETE_VC:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
			break;
		
		case ACTION_ACTIVATE_VC:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
			break;
			
		case ACTION_DEACTIVATE_VC:
			mMyDbHelper.updateCardStatus(mId, Card.STATUS_PERSONALIZED, getDeviceId(), mType);
			break;
		}
		
		// Updated values
		mCards = mMyDbHelper.getAllCards(getDeviceId());
		
		// Update info on the screen
		mAdapter.updateCards(mCards);
       	mAdapter.notifyDataSetChanged();
       	
       	btnCreateCard.setVisibility(View.VISIBLE);
		llProgressBar.setVisibility(View.GONE);
		
		MyPreferences.setCardOperationOngoing(getApplicationContext(), false);
	}
	
	public class MyDragListener implements OnDragListener {
	    @Override
	    public boolean onDrag(View v, DragEvent event) {
	    	switch (event.getAction()) {
	    	case DragEvent.ACTION_DRAG_ENTERED:
	    		switch(v.getId()) {
				case R.id.trash:
					trash.setColorFilter(Color.RED);
					break;
				case R.id.block_p:
					block.setColorFilter(Color.RED);
					break;
				}
	    		break;
	      case DragEvent.ACTION_DRAG_EXITED:
	    	  	trash.setColorFilter(null);
	    	  	block.setColorFilter(null);
//	        	v.setBackgroundDrawable(normalShape);
	    	  	break;
	      case DragEvent.ACTION_DROP:
	    	  	trash.setColorFilter(null);
	    	  	block.setColorFilter(null);
	    	  	
	        	// Dropped, delete card
	    	  	PassObject passObj = (PassObject)event.getLocalState();
				Card passedItem = passObj.card;
				
				switch(v.getId()) {
				case R.id.trash:
					startDeleteCard(passedItem);
					break;
				case R.id.block_p:
					startBlockCard(passedItem);
				}
				
	        break;
	      default:
	        break;
	      }
	      	return true;
	    }
	}
	
	public void startDeleteCard(Card card) {
		final int type = card.getType();
		final int vcEntry = card.getIdVc();
		final int index = card.getIdScript();
		final String name = card.getCardName();

		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(MyCardsActivity.this);
        builder.setTitle("Delete Credit Card");
        builder.setMessage("Are you sure you want to delete " + name + " card?");
        
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                
                // delete the card instance
                if(isDeviceConnected()) {
                	if(MyPreferences.isCardOperationOngoing(MyCardsActivity.this) == false) {
	                	if(type == Card.TYPE_PAYMENTS) {
		             	   // Alert the user about the card creation
		             	   MyNotification.show(MyCardsActivity.this, 
		             			   getResources().getString(R.string.notif_card_deleting_title), getResources().getString(R.string.notif_card_deleting_msg),
		             			   MyNotification.NOTIF_ID_OPERATING);
		             	   
		             	   btnCreateCard.setVisibility(View.GONE);
	        			   llProgressBar.setVisibility(View.VISIBLE);
	        			   
	        			   	// Set the new status
	        			   	mMyDbHelper.updateCardStatus(index, Card.STATUS_DELETING, getDeviceId(), Card.TYPE_PAYMENTS);
	        			   
	            			// Store the action to do
	            			mAction = ACTION_EXECUTE_SCRIPT;
	            				
	            			// The script to be launched
	            			String script = String.format("delete_mmpp_000%s_encrypted.txt", String.valueOf(index));
	            			
	            			// Save values for the processResult
	            			mScript = script;
	            			mId = index;
	        			   
	        			   	AsyncTask<?, ?, ?> task = new ExecuteScriptTask(MyCardsActivity.this, script).execute();
	           				setRunningAsyncTask(task);
	                	} else if(type == Card.TYPE_MIFARE_CLASSIC || type == Card.TYPE_MIFARE_DESFIRE) {
							btnCreateCard.setVisibility(View.GONE);
							llProgressBar.setVisibility(View.VISIBLE);
	
						     // Alert the user about the card creation
					     	MyNotification.show(MyCardsActivity.this, 
					     			   getResources().getString(R.string.notif_card_deleting_title), getResources().getString(R.string.notif_card_deleting_msg),
					     			   MyNotification.NOTIF_ID_OPERATING);
	
							mAction = ACTION_DELETE_VC;
						    mId = vcEntry;
						    mType = type;
						    	
						    // Set the new status
							mMyDbHelper.updateCardStatus(mId, Card.STATUS_DELETING, getDeviceId(), type);
								
							AsyncTask<?, ?, ?> task = new DeleteVCTask(MyCardsActivity.this, vcEntry).execute();
	           				setRunningAsyncTask(task);
	                	}
	                	
	                	// Update cards status
	    			   	showCardsInfo();
                	} else
                		Toast.makeText(MyCardsActivity.this, "Operation in progress", Toast.LENGTH_LONG).show();
                } else
                	Toast.makeText(MyCardsActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();                      
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
	
	public void startBlockCard(Card card) {
		final int index = card.getIdScript();
		final String name = card.getCardName();
		final boolean isCardLocked = card.isLocked();

		if(isCardLocked)
			showCardLockDialog(name, index, false);
		else
			showCardLockDialog(name, index, true);
	}
	
	OnDragListener myOnDragListener = new OnDragListener() {
		@Override
		public boolean onDrag(View v, DragEvent event) {
			switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					break;	
				case DragEvent.ACTION_DRAG_ENTERED:
					break;	
				case DragEvent.ACTION_DRAG_EXITED:
					break;	
				case DragEvent.ACTION_DROP:
					PassObject passObj = (PassObject)event.getLocalState();
					View view = passObj.view;
					Card passedItem = passObj.card;
					List<Card> srcList = passObj.srcList;
					ListView oldParent = (ListView)view.getParent();
					
					if(oldParent != null) {
						MyCardsAdapter srcAdapter = (MyCardsAdapter)(oldParent.getAdapter());
						
						LinearLayoutListView newParent = (LinearLayoutListView)v;
						MyCardsAdapter destAdapter = (MyCardsAdapter)(newParent.listView.getAdapter());
					    List<Card> destList = destAdapter.getList();
						
						if(removeItemToList(srcList, passedItem)){
							addItemToList(destList, passedItem);
						}
						
						srcAdapter.notifyDataSetChanged();
						destAdapter.notifyDataSetChanged();
						
						//smooth scroll to bottom
						newParent.listView.smoothScrollToPosition(destAdapter.getCount()-1);
					}
					
					break;
			   case DragEvent.ACTION_DRAG_ENDED:
			   default:
				   break;	   
			}
			   
			return true;
		}
		
	};
	
	public class ItemOnDragListener implements OnDragListener {
		Card  me;
		
		public ItemOnDragListener(Card i){
			me = i;
		}

		@Override
		public boolean onDrag(View v, DragEvent event) {
			PassObject passObj = (PassObject)event.getLocalState();
			View view = passObj.view;
			Card passedItem = passObj.card;
			
			switch (event.getAction()) {
			case DragEvent.ACTION_DRAG_STARTED:
				// Set the trash visible
				trash.setVisibility(View.VISIBLE);
				
				if(passedItem.getType() == Card.TYPE_PAYMENTS)
					block.setVisibility(View.VISIBLE);
				
				break;	
			case DragEvent.ACTION_DRAG_ENTERED:
				v.setBackgroundColor(0x30000000);
				break;	
			case DragEvent.ACTION_DRAG_EXITED:
				v.setBackgroundColor(0x00000000);
				break;	
			case DragEvent.ACTION_DROP:
				List<Card> srcList = passObj.srcList;
				ListView oldParent = (ListView)view.getParent();
				MyCardsAdapter srcAdapter = (MyCardsAdapter)(oldParent.getAdapter());
				
				ListView newParent = (ListView)v.getParent();
				MyCardsAdapter destAdapter = (MyCardsAdapter)(newParent.getAdapter());
				List<Card> destList = destAdapter.getList();
				
				int removeLocation = srcList.indexOf(passedItem);
				int insertLocation = destList.indexOf(me);
				
				/*
				 * If drag and drop on the same list, same position,
				 * ignore
				 */
				if(srcList != destList || removeLocation != insertLocation){
					if(removeItemToList(srcList, passedItem)){
						destList.add(insertLocation, passedItem);
					}

					// Update items order in the list for further cards consulting
					mMyDbHelper.updateCardsOrder(passedItem.getId(), passedItem.getOrder(), me.getId(), me.getOrder(), getDeviceId());
					
					srcAdapter.notifyDataSetChanged();
					destAdapter.notifyDataSetChanged();
				}

				v.setBackgroundColor(0x00000000);
				
				break;
		   case DragEvent.ACTION_DRAG_ENDED:
			   // Remove the trash
			   trash.setVisibility(View.GONE);
			   block.setVisibility(View.GONE);
			   
			   v.setBackgroundColor(0x00000000);
		   default:
			   break;	   
		}
		   
		return true;
		}
		
	}
	
	private boolean removeItemToList(List<Card> l, Card it){
		boolean result = l.remove(it);
		return result;
	}
	
	private boolean addItemToList(List<Card> l, Card it){
		boolean result = l.add(it);
		return result;
	}
	
	//objects passed in Drag and Drop operation
	class PassObject{
		View view;
		Card card;
		List<Card> srcList;
		
		PassObject(View v, Card c, List<Card> s){
			view = v;
			card = c;
			srcList = s;
		}
	}
	
	OnItemLongClickListener myOnItemLongClickListener = new OnItemLongClickListener(){
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			Card selectedItem = mCards.get(position);
			
			MyCardsAdapter associatedAdapter = (MyCardsAdapter)(parent.getAdapter());
		    List<Card> associatedList = associatedAdapter.getList();
			
			PassObject passObj = new PassObject(view, selectedItem, associatedList);

			ClipData data = ClipData.newPlainText("", "");
			DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
			view.startDrag(data, shadowBuilder, passObj, 0);

			return true;
		}
	};
	
	public class MyCardsAdapter extends BaseAdapter {
		private OnFavClickListener mClickListener = null;
		
	    private LayoutInflater inflater=null; 
	    private List<Card> cards;
	 
	    public MyCardsAdapter(Context c, List<Card> cards, OnFavClickListener listener) {
	    	this.cards = cards;
	        inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        mClickListener = listener;
	    }
	 
	    public int getCount() {
	        return cards.size();
	    }
	 
	    public Object getItem(int position) {
	        return position;
	    }
	 
	    public long getItemId(int position) {
	        return position;
	    }
	 
	    public View getView(final int position, View convertView, ViewGroup parent) {
	    	View vi = convertView;
	        
	        if(vi == null)
	            vi = inflater.inflate(R.layout.list_card_entry, null);
	        
	        TextView name = (TextView) vi.findViewById(R.id.card_name);
	        TextView number = (TextView) vi.findViewById(R.id.card_number);
	        TextView exp = (TextView) vi.findViewById(R.id.card_exp);
	        
	        ImageView cardFav = (ImageView) vi.findViewById(R.id.card_fav);
	        ImageView cardIcon = (ImageView) vi.findViewById(R.id.card_icon);
	        ImageView cardLocked = (ImageView) vi.findViewById(R.id.card_locked); 
	        
	        Log.d("MyCardsAdapter", "My Cards. ID: " + cards.get(position).getId() + " + Script ID: " + cards.get(position).getIdScript() + " VC Entry: " + cards.get(position).getIdVc() +
	        		" Status: " + cards.get(position).getStatus() + " Fav: " + cards.get(position).isFav() + " Order: " + cards.get(position).getOrder());
	        
	        switch(cards.get(position).getStatus()) {
	        	case Card.STATUS_PERSONALIZED:
	        	case Card.STATUS_READING:
	        		if(cards.get(position).getType() == Card.TYPE_PAYMENTS) {
		        		name.setText(cards.get(position).getCardName());
				        number.setText("XXXX XXXX XXXX " + cards.get(position).getCardNumber().substring(cards.get(position).getCardNumber().length() - 4));
				        exp.setText(cards.get(position).getCardExpMonth() + " / " + cards.get(position).getCardExpYear());
				        
				        cardFav.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if(mClickListener != null)
						            mClickListener.onFavClick(position); 
							}
						});
				
				        if(cards.get(position).isFav() == true) {
				        	cardFav.setImageResource(R.drawable.fav);
				        } else {
				        	cardFav.setImageResource(R.drawable.no_fav);
				        }
				        
				        if(cards.get(position).isLocked() == true) {
				        	cardLocked.setVisibility(View.VISIBLE);
				        } else {
				        	cardLocked.setVisibility(View.GONE);
				        }
	        		} else if(cards.get(position).getType() == Card.TYPE_MIFARE_CLASSIC
	        				|| cards.get(position).getType() == Card.TYPE_MIFARE_DESFIRE) {
	        			name.setText(cards.get(position).getCardName());
	        			cardFav.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if(mClickListener != null)
						            mClickListener.onFavClick(position); 
							}
						});
				
				        if(cards.get(position).isFav() == true) {
				        	cardFav.setImageResource(R.drawable.fav);
				        } else {
				        	cardFav.setImageResource(R.drawable.no_fav);
				        }
				        
				        if(cards.get(position).getMifareType() == Card.MIFARE_HOSPITALITY) {
				        	if(cards.get(position).getCardNumber().equals("999") == true)
				        		number.setText("Room not personalized");
				        	else
				        		number.setText("Room: " + cards.get(position).getCardNumber());
				        } else if(cards.get(position).getMifareType() == Card.MIFARE_LOYALTY) {
			        		number.setText("Points: " + cards.get(position).getCardNumber());
				        } else if(cards.get(position).getMifareType() == Card.MIFARE_TICKETING) {
			        		number.setText("Trips: " + cards.get(position).getCardNumber());
				        } else if(cards.get(position).getMifareType() == Card.MIFARE_MYMIFAREAPP) {

				        }
	        		}
			        	        
			        // Show the card icon for this particular credit card
			        cardIcon.setImageResource(cards.get(position).getIconRsc());
			        
			        break;
			        
	        	case Card.STATUS_ACTIVATING:
	        		name.setText(cards.get(position).getCardName());
	        		
	        		if(cards.get(position).getType() == Card.TYPE_PAYMENTS) {
				        number.setText("XXXX XXXX XXXX " + cards.get(position).getCardNumber().substring(cards.get(position).getCardNumber().length() - 4));
				        exp.setText(cards.get(position).getCardExpMonth() + " / " + cards.get(position).getCardExpYear());
				        		        
				        if(cards.get(position).isLocked() == true) {
				        	cardLocked.setVisibility(View.VISIBLE);
				        } else {
				        	cardLocked.setVisibility(View.GONE);
				        }
	        		} else if(cards.get(position).getType() == Card.TYPE_MIFARE_CLASSIC
	        				|| cards.get(position).getType() == Card.TYPE_MIFARE_DESFIRE) {
	        			if(cards.get(position).getMifareType() == Card.MIFARE_HOSPITALITY) {
	        				 if(cards.get(position).getCardNumber().equals("999") == true)
	        					 number.setText("Room not personalized");
	        				 else
	        					 number.setText("Room: " + cards.get(position).getCardNumber());
	        			} else if(cards.get(position).getMifareType() == Card.MIFARE_LOYALTY) {
	        				 number.setText("Points: " + cards.get(position).getCardNumber());
	        			} else if(cards.get(position).getMifareType() == Card.MIFARE_TICKETING) {
	        				 number.setText("Trips: " + cards.get(position).getCardNumber());
					    } else if(cards.get(position).getMifareType() == Card.MIFARE_MYMIFAREAPP) {
	
					    }
	        		}
			        	        
			        // Show the card icon for this particular card
			        cardIcon.setImageResource(cards.get(position).getIconRsc());
			        cardFav.setImageResource(R.drawable.activating);
			        
			        break;

	        	case Card.STATUS_CREATING:
	        		name.setText(vi.getResources().getString(R.string.my_cards_creating));
	        		
	        		// Is not personalized, thus cannot be fav 
	    	        cardFav.setImageResource(R.drawable.no_fav);
	    	        cardIcon.setImageResource(R.drawable.card_blank);
	        		
	        		break;
	        	case Card.STATUS_PERSONALIZING:
	        		name.setText(vi.getResources().getString(R.string.my_cards_personalizing));
	        		
	        		// Is not personalized, thus cannot be fav 
	    	        cardFav.setImageResource(R.drawable.no_fav);
	    	        cardIcon.setImageResource(R.drawable.card_blank);
	        		
	        		break;
	        	case Card.STATUS_DELETING:
	        		name.setText(vi.getResources().getString(R.string.my_cards_deleting));
	        		
	        		// Is not personalized, thus cannot be fav 
	    	        cardFav.setImageResource(R.drawable.no_fav);
	    	        cardIcon.setImageResource(cards.get(position).getIconRsc());
	        		
	        		break;
	        		
	        	case Card.STATUS_FAILED:
	        		name.setText(vi.getResources().getString(R.string.my_cards_failed));
	        		
	        		// Is not personalized, thus cannot be fav 
	    	        cardFav.setImageResource(R.drawable.no_fav);
	    	        cardIcon.setImageResource(R.drawable.card_blank);
	        		
	        		break;
	        		
	        	case Card.STATUS_SERVER_FAILED:
	        		name.setText(vi.getResources().getString(R.string.my_cards_perso_failed));
	        		
	        		// Is not personalized, thus cannot be fav 
	    	        cardFav.setImageResource(R.drawable.no_fav);
	    	        cardIcon.setImageResource(R.drawable.card_blank);
	        		
	        		break;
	        }
	        
	        vi.setOnDragListener(new ItemOnDragListener(cards.get(position)));
	                       
	        return vi;
	    }
	    
	    /**
	     * Method used to refresh the listView
	     */
	    public void updateCards(List<Card> cards) {
	    	this.cards = cards;
	    }
	    
	    public List<Card> getList(){
			return this.cards;
		}
	}
	
	@Override
	public void onConnect(final boolean connected) {
		super.onConnect(connected);
		
		// I run on the UI Thread in order to be able to show the dialog
		runOnUiThread(new Runnable() {
		    public void run() {
	
				// Update cards info
				showCardsInfo();
		    }
		});	
	}
}
