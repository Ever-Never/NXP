package com.nxp.nxpwalletconndev.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.connections.DataConnection;
import com.nxp.nxpwalletconndev.databases.MyDbHelper;
import com.nxp.nxpwalletconndev.httpcomm.HttpUploadFile;
import com.nxp.nxpwalletconndev.listeners.OnActivateResultListener;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnScriptIdListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.loaderservice.SmartcardLoaderServiceResponse;
import com.nxp.nxpwalletconndev.notifications.MyNotification;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.tasks.ActivateCreditCardTask;
import com.nxp.nxpwalletconndev.tasks.CreateVCTask;
import com.nxp.nxpwalletconndev.tasks.ExecuteScriptTask;
import com.nxp.nxpwalletconndev.tasks.GetVersionTask;
import com.nxp.nxpwalletconndev.tasks.NextScriptIdTask;
import com.nxp.nxpwalletconndev.tasks.NfcHttpProxyPersoTask;
import com.nxp.nxpwalletconndev.tasks.ReadCreditCardTask;
import com.nxp.ssdp.btclient.BluetoothTLV;

public class PaymentCardActivity extends BaseActivity implements OnActivateResultListener, OnOperationListener, OnScriptIdListener, OnTransmitApduListener  {
	public static final String TAG = "PersonalizeCardActivity";
	
	// We limit the cards that can be created to 3. To increase the number new LS Scripts are needed
	public static final int MAX_CARD_ID = 5;
	
//	public static final String URL_UPLOAD_KEYS = "http://192.168.0.13/ssdp/setKeysBLE.php";
	public static final String URL_UPLOAD_KEYS = "http://themobileknowledge.com/ssdp/setKeysBLE.php";
//	public static final String URL_PERSO_SERVLET = "http://192.168.0.13:8080/RemoteMCWPerso/MCWPersoServletMain";
	public static final String URL_PERSO_SERVLET = "http://www.themobileknowledge.com/Servlets/RemoteMCWPerso/MCWPersoServletMain";
	
	public static final int PERSO_OPTION_DEFAULT = 0;
	public static final int PERSO_OPTION_PARAMS = 1;
	
	private final static int ACTION_PERSO_CARD = 10;
	private final static int ACTION_ACTIVATE_CARD = 11;
	private final static int ACTION_EXECUTE_SCRIPT = 12;
	private final static int ACTION_NEXT_ID = 13;
	
	static final int REQUEST_IMAGE_CAPTURE = 1;
	
	private int mAction = 0;
	
	private CheckBox chDefault;	
	private ListView list;
	
	private RelativeLayout layoutCardProfiles;
		
	private NfcAdapter mNfcAdapter;
	private MyDbHelper mMyDbHelper;
	
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;
	
	private String IMEI;
	private String mScript;
	private int mCardId;
		
	public static final String scriptsOutputFolder = Environment.getExternalStorageDirectory() + "/loaderserviceconndev/";

	ArrayList<ProfileEntry> profiles;
	ArrayAdapter<ProfileEntry> adapter;
	
	private int mSelectedProfile = -1;

	public static class ProfileEntry {
		public int  id;
		public int icon;
		public String name;
		public String pan;
		public String exp;
		public String prof_id;
		public String pin;
		public boolean isSelected;

		public ProfileEntry(final int id, final int icon, final String name, final String pan, final String exp, final String pin, final String prof_id) {
			this.id = id;
			this.icon = icon;
			this.name = name;
			this.pan = pan;
			this.exp = exp;
			this.pin = pin;
			this.prof_id = prof_id;
			this.isSelected = false;
		}
		
		public void setSelected (boolean selected) {
			this.isSelected = selected;
		}
		
		public boolean getSelected () {
			return this.isSelected;
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_personalize_card);
		
		/*((Button) findViewById(R.id.personalize_card)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isCardValid() == true) {
					if(isDeviceConnected() == true) {
						if(DataConnection.isConnected(PaymentCardActivity.this) == true) {
							showCreationDialog();	
						} else
							showConnectionRequiredDialog();
					} else
	                 	   Toast.makeText(PaymentCardActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
				} else {
					// Inform the user
					Toast.makeText(getApplicationContext(), "Invalid values for card perso", Toast.LENGTH_LONG).show();
				}			
			}
		});
		*/
		/*
		((ImageView) findViewById(R.id.image_camera_icon)).setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
			    }
			}
		});
		*/
		
		// Get database helper
		mMyDbHelper = new MyDbHelper(this);
		
		// Get the reference to Loader Service for the execution of scripts
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
		// The IMEI is used to identify the phone and retrieve its  SD Keys
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		IMEI = telephonyManager.getDeviceId();
		
		// TODO Just for demo, consider replacing
		IMEI = IMEI.replace("86", "CC");
		
		layoutCardProfiles = (RelativeLayout) findViewById(R.id.layoutCardProfiles);
		//chDefault = (CheckBox) findViewById(R.id.card_default);
		
		
		// Set NFC Foreground
		if(mNfcAdapter != null)
			setNfcForeground();
		
		// Get Params just in case the user does not select an option
//		updateGUIParams();
		
		// Let the user select between the different card perso options
//		showCardPersoOptionsDialog();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (mNfcAdapter != null) {
			mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
		}
		
		updateGUIDefault();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (mNfcAdapter != null) {
			mNfcAdapter.disableForegroundDispatch(this);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//	        Bundle extras = data.getExtras();
//	        Bitmap imageBitmap = (Bitmap) extras.get("data");
//	        mImageView.setImageBitmap(imageBitmap);
	    	
	    	// Set the selected profile to Prof 04
	    	mSelectedProfile = 0;
	    	
	    	if(DataConnection.isConnected(PaymentCardActivity.this) == true) {
				showCreationDialog();
			} else
				showConnectionRequiredDialog();	
	    }
	}
	
	@Override
	protected void onNewIntent(Intent nfc_intent) {
		super.onNewIntent(nfc_intent);
		doProcess(nfc_intent);
	}
	
	private void doProcess(Intent i) {
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(i.getAction()) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(i.getAction())) {
			Card card = new ReadCreditCardTask().readCreditCard(i);
			
			if(card != null)  {
				Toast.makeText(PaymentCardActivity.this, "Card successfully processed", Toast.LENGTH_LONG).show();
				
				// Show retrieved values on the screen
//				showCreditCardValues(card);
				
				// Set the selected profile to Prof 04
		    	mSelectedProfile = 0;
		    	
		    	if(DataConnection.isConnected(PaymentCardActivity.this) == true) {
					showCreationDialog();
				} else
					showConnectionRequiredDialog();	
			} else
				Toast.makeText(PaymentCardActivity.this, "Error reading card", Toast.LENGTH_LONG).show();
		}
	}
	
	public void showCreationDialog() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(PaymentCardActivity.this);
        builder.setTitle(getResources().getString(R.string.init_create_card));
        builder.setMessage(getResources().getString(R.string.init_create_card_msg));
        
        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   //createCard();
					   //////////////////////////////////////////////////////////////
					   //new GetVersionTask(AboutActivity.this).execute();
					   String script = "createSD.txt";
					   new GetVersionTask(PaymentCardActivity.this).execute();
					   byte[] enableWiredModeTLV = BluetoothTLV.getTlvCommand(BluetoothTLV.WIRED_MODE_ENABLE, new byte[] {0x00} );
					   //apduListener.sendApduToSE(enableWiredModeTLV, 4000);
					   StringBuilder sb = new StringBuilder();
					   for (byte b : enableWiredModeTLV) {
						   sb.append(String.format("%02X ", b));
					   }
					   Log.e(TAG, "RECEIVED DATA+++++++++++++++++++++++++++++++= "+sb.toString());
					   AsyncTask<?, ?, ?> task = new ExecuteScriptTask(PaymentCardActivity.this, script).execute();
					   setRunningAsyncTask(task);
					   //byte[] disableWiredModeTLV = BluetoothTLV.getTlvCommand(BluetoothTLV.WIRED_MODE_DISABLE, new byte[] {0x00} );

					   //////////////////////////////////////////////////////////////
                       dialog.dismiss();
                   }
               });
 
        // Create the AlertDialog object and return it
        builder.create();
        builder.show();
		
	}
	
	private void createCard() {	
		mAction = ACTION_NEXT_ID;
		
		// Get already used ids to avoid sending these selects as we know that the instance exists
		List<Integer> ids = mMyDbHelper.getAllPaymentCardsIds(getDeviceId());

		AsyncTask<?, ?, ?> task = new NextScriptIdTask(PaymentCardActivity.this, ids).execute();
		setRunningAsyncTask(task);
	}

	private void updateGUIDefault() {
		layoutCardProfiles.setVisibility(View.VISIBLE);

		list = (ListView) findViewById(R.id.list_profiles);
		
		profiles = new ArrayList<ProfileEntry>();
		
		String profile_string;
		
		if(versionJCOP3dot3){				//For JCOP Version 3.3
			profile_string = "Prof_04_JCOP_33";
		}
		else{							//For JCOP Version 3.1
			profile_string = "Prof_04";
		}
//		profiles.add(new ProfileEntry(
//				0, R.drawable.card_payment_chinese, 
//				"Demo Card", "5413339000001513", "12/20", "1234", "Prof_04"));
		profiles.add(new ProfileEntry(
				1, R.drawable.card_payment_mastercard, 
				" Demo Card", " 5413339000001513", " 12/20", "1234", profile_string));
		profiles.add(new ProfileEntry(
				2, R.drawable.camera_icon, 
				" Demo Card", " 5413339000001513", " 12/20", "1234", profile_string));
			
		adapter = new ArrayAdapter<ProfileEntry>(this,
				R.layout.list_profile_entry, profiles) {
			@Override
			public View getView(final int position, final View convertView,
					final ViewGroup parent) {
				final ProfileEntry entry = getItem(position);

				View root;

				if (convertView == null) {
					final LayoutInflater inflater = LayoutInflater
							.from(PaymentCardActivity.this);

					root = inflater
							.inflate(R.layout.list_profile_entry,
									parent, false);
				} else {
					root = convertView;
				}
				
				//final RadioButton radio = (RadioButton) root.
				//		findViewById(R.id.profile_radio);
				final ImageView image = (ImageView) root.
						findViewById(R.id.profile_card);
				final TextView name = (TextView) root.
						findViewById(R.id.profile_name);
				final TextView pan = (TextView) root.
						findViewById(R.id.profile_pan);
				final TextView exp = (TextView) root.
						findViewById(R.id.profile_exp);
//				final TextView pin = (TextView) root.
//						findViewById(R.id.profile_pin);
				final TextView txt_name = (TextView) root.
						findViewById(R.id.profile_fixed_name);
				final TextView txt_pan = (TextView) root.
						findViewById(R.id.profile_fixed_pan);
				final TextView txt_exp = (TextView) root.
						findViewById(R.id.profile_fixed_exp);
				
				
				//radio.setChecked(entry.isSelected);
				
				image.setImageResource(entry.icon);
				name.setText(entry.name);
				pan.setText(entry.pan);
				exp.setText(entry.exp);
				
				if(entry.id==2){
					pan.setVisibility(android.view.View.INVISIBLE);
					exp.setVisibility(android.view.View.INVISIBLE);
					txt_name.setVisibility(android.view.View.INVISIBLE);
					txt_exp.setVisibility(android.view.View.INVISIBLE);
					name.setVisibility(android.view.View.INVISIBLE);
					txt_pan.setText(R.string.perso_scan_text);
					
				}
				else{
					pan.setVisibility(android.view.View.VISIBLE);
					exp.setVisibility(android.view.View.VISIBLE);
					txt_name.setVisibility(android.view.View.VISIBLE);
					txt_exp.setVisibility(android.view.View.VISIBLE);
					name.setVisibility(android.view.View.VISIBLE);
					txt_pan.setText(R.string.profile_pan);
				}
				
				
				root.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if(isDeviceConnected() == true) {
							if(MyPreferences.isCardOperationOngoing(PaymentCardActivity.this) == false) {
								mSelectedProfile = entry.id;
								if(DataConnection.isConnected(PaymentCardActivity.this) == true) {
									if(mSelectedProfile == 2){
										Toast.makeText(getApplicationContext(), "To be available soon", Toast.LENGTH_SHORT).show();
									}
									else
										showCreationDialog();	
								} else
									showConnectionRequiredDialog();
							} else
	                			   Toast.makeText(PaymentCardActivity.this, "Operation not possible while card is under process", Toast.LENGTH_LONG).show();
						} else
	                	   Toast.makeText(PaymentCardActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
								
					}
				});
				
				
				return root;
			}
		};
		
		list.setAdapter(adapter);
		
		// In this option we are not interested in reading contactless cards anymore
//		if (mNfcAdapter != null) {
//			mNfcAdapter.disableForegroundDispatch(this);
//		}
	}
		
	private void uploadSDKeys() {
		String outputFile;
		if(versionJCOP3dot3){	//JCOP Version 3.3
			outputFile = String.format("install_mmpp_000%s_jcop_33_encrypted_%s_ConnDevOutput.txt", String.valueOf(mCardId), String.valueOf(getDeviceId()));
		}
		else{	//JCOP Version 3.1
			outputFile = String.format("install_mmpp_000%s_encrypted_%s_ConnDevOutput.txt", String.valueOf(mCardId), String.valueOf(getDeviceId()));
		}
		
		new uploadFileTask().execute(outputFile);
	}
	
	private void personalizeMMPPCard() {
		String profile = "";
		
		String number = "";
		String exp = "";
				
		profile = profiles.get(mSelectedProfile).prof_id;
				
		// Store the action to do
		mAction = ACTION_PERSO_CARD;
		
		try {
			AsyncTask<?, ?, ?> task = new NfcHttpProxyPersoTask(PaymentCardActivity.this, URL_PERSO_SERVLET, IMEI, String.valueOf(mCardId), String.valueOf(getDeviceId()), true, profile, number, exp).execute();
			setRunningAsyncTask(task);
		} catch (Exception e) {
			e.printStackTrace();
				
			Log.e(TAG, "Error executing personalization");
		}
	}
	
	private class uploadFileTask extends AsyncTask<String, Integer, Integer> {		
		@Override
		protected void onPostExecute(Integer result) {
			// Action completed   		
    		if(result == 200) {
//    			Toast.makeText(getApplicationContext(), "Keys uploaded to the server", Toast.LENGTH_LONG).show();
    			
    			// Now that the keys are in the server we can continue with the personalization
    			personalizeMMPPCard();
    		} else {
    			Toast.makeText(getApplicationContext(), "Error uploading keys to the server", Toast.LENGTH_LONG).show();
    			
    			// There was an error during creation so we remove it from the database
        		mMyDbHelper.updateCardStatus(mCardId, Card.STATUS_FAILED, getDeviceId(), Card.TYPE_PAYMENTS);
        		
        		Intent broadcast = new Intent();
    			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
    	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
    	        sendBroadcast(broadcast);
    		}
        }
        
		@Override
		protected Integer doInBackground(String... files) {
			return HttpUploadFile.uploadFile(URL_UPLOAD_KEYS, mCardId, getDeviceId(), IMEI, scriptsOutputFolder, files[0]);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
	}

	private boolean isCardValid() {
		if(mSelectedProfile >= 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public void proccessTransactionTaskResult(StringBuffer result){
		if(result.indexOf("successful") != -1){
//			Toast.makeText(this, getResources().getString(R.string.perso_ok), Toast.LENGTH_LONG).show();
			
			// By default the card is generated as no fav and no locked
			mMyDbHelper.personalizeCreditCard(mCardId, profiles.get(mSelectedProfile).name + " " + mCardId, profiles.get(mSelectedProfile).pan, "", 
					profiles.get(mSelectedProfile).exp.substring(0, profiles.get(mSelectedProfile).exp.indexOf("/")), 
					profiles.get(mSelectedProfile).exp.substring(profiles.get(mSelectedProfile).exp.indexOf("/") + 1), 
					false, false, R.drawable.card_payment_mastercard, getDeviceId());
			
			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mCardId, Card.STATUS_ACTIVATING, getDeviceId(), Card.TYPE_PAYMENTS);
 			
 			Intent broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_ACTIVATING);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
	        
			boolean set_default = true;
			// boolean set_default = chDefault.isChecked();
			if(set_default) {
				// Store the action to do
				mAction = ACTION_ACTIVATE_CARD;
				
				mMyDbHelper.updateCardStatus(mCardId, Card.STATUS_ACTIVATING, getDeviceId(), Card.TYPE_PAYMENTS);
				
				AsyncTask<?, ?, ?> task = new ActivateCreditCardTask(PaymentCardActivity.this, true).execute(mCardId);
   				setRunningAsyncTask(task);
			} else {
				// Alert the user about the card creation
				MyNotification.show(PaymentCardActivity.this, 
						getResources().getString(R.string.notif_card_created_title), getResources().getString(R.string.notif_card_created_msg),
						MyNotification.NOTIF_ID_COMPLETED);
			}
		} else {
			Toast.makeText(this, getResources().getString(R.string.server_perso_error), Toast.LENGTH_LONG).show();
			
			// There was an error during creation so we remove it from the database
//    		mMyDbHelper.deleteCreditCard(mCardId, getDeviceId());
			
			// There was an error during creation so we remove it from the database
    		mMyDbHelper.updateCardStatus(mCardId, Card.STATUS_SERVER_FAILED, getDeviceId(), Card.TYPE_PAYMENTS);

 			Intent broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);
		}
	}
	
	private void showConnectionRequiredDialog() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(PaymentCardActivity.this);
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
	public void onActivateResult(boolean result, boolean activate, int id) {
		if(result == true) {
			mMyDbHelper.removeFav(getDeviceId(), Card.TYPE_PAYMENTS);
			mMyDbHelper.makeCardFav(id, getDeviceId(), Card.TYPE_PAYMENTS);
	
//		    Toast.makeText(CreatePaymentCardActivity.this, getResources().getString(R.string.applet_activated), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(PaymentCardActivity.this, getResources().getString(R.string.error_applet_activated), Toast.LENGTH_LONG).show();			
		}
		
		// Set the activating status
		mMyDbHelper.updateCardStatus(id, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_PAYMENTS);
		
		Intent broadcast = new Intent();
		broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
        sendBroadcast(broadcast);
			
		// Alert the user about the card creation
		MyNotification.show(PaymentCardActivity.this, 
				getResources().getString(R.string.notif_card_created_title), getResources().getString(R.string.notif_card_created_msg),
				MyNotification.NOTIF_ID_COMPLETED);
	}
	
	public void processStatusScript(byte[] mBufferDataCmd) {   		
		if(mBufferDataCmd != null) {
			int bufferLength = mBufferDataCmd.length;
			
			String resp = mScript.replace(".txt", "_" + String.valueOf(getDeviceId()) + "_ConnDevOutput.txt");
			
			// Set the name for the output String
			String out = scriptsOutputFolder + resp;
			SmartcardLoaderServiceResponse.writeOutputFile(PaymentCardActivity.this, out, new String(mBufferDataCmd));
			
    		if (mBufferDataCmd[bufferLength - 2] == '0' && mBufferDataCmd[bufferLength - 3] == '0' 
    					&& mBufferDataCmd[bufferLength - 4] == '0' && mBufferDataCmd[bufferLength - 5] == '9') {
     			// Set the new status
        		mMyDbHelper.updateCardStatus(mCardId, Card.STATUS_PERSONALIZING, getDeviceId(), Card.TYPE_PAYMENTS);
        		     			
     			Intent broadcast = new Intent();
    			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZING);
    	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
    	        sendBroadcast(broadcast);
     			
     			// Now let's go for the perso
     			uploadSDKeys();
            } else {
            	Toast.makeText(PaymentCardActivity.this, "Card creation failed", Toast.LENGTH_LONG).show();
        		
        		Intent broadcast = new Intent();
    			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
    	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
    	        sendBroadcast(broadcast);
    			
        		// There was an error during creation so we remove it from the database
        		mMyDbHelper.deleteCard(mCardId, getDeviceId(), Card.TYPE_PAYMENTS);
            }	
		} else
			Toast.makeText(PaymentCardActivity.this, "Error detected in the BLE channel", Toast.LENGTH_LONG).show();
	}

	@Override
	public void sendApduToSE(byte[] dataBT, int timeout) {
		// Send the data via Bluetooth
        writeBluetooth(dataBT, timeout);
	}

	public void processOperationResult(byte[] mBufferDataCmd) {
		if(mBufferDataCmd != null) {
			switch (mAction) {
			case ACTION_NEXT_ID:
				NextScriptIdTask.receiveApduFromSE(mBufferDataCmd);
				break;
			
			case ACTION_EXECUTE_SCRIPT:
				processStatusScript(mBufferDataCmd);
				
				break;
			
			case ACTION_PERSO_CARD:
				NfcHttpProxyPersoTask.receiveApduFromSE(mBufferDataCmd);
				
				break;
				
			case ACTION_ACTIVATE_CARD:
				ActivateCreditCardTask.receiveApduFromSE(mBufferDataCmd);
				
				break;
			}
		} else {
			Toast.makeText(PaymentCardActivity.this, "Error detected in the BLE channel", Toast.LENGTH_LONG).show();
		}
	}
	
	public void setNfcForeground() {
		// Create a generic PendingIntent that will be delivered to this
		// activity. The NFC stack will fill
		// in the intent with the details of the discovered tag before
		// delivering it to this activity.
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(
				getApplicationContext(), getClass())
				.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// Setup an intent filter for all NDEF based dispatches
		mFilters = new IntentFilter[] {
				
		// new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
		new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED) };

		// Setup a tech list for all NFC tags
		mTechLists = new String[][] { new String[] { NfcA.class.getName() },
				new String[] { NfcB.class.getName() } };
	}

	@Override
	public void onScriptId(int id) {
		// ID for the card to be created
		mCardId = id;
		
		if(mCardId <= MAX_CARD_ID) {
			// Store the action to do
			mAction = ACTION_EXECUTE_SCRIPT;
			
			// The script to be launched
			String script;
			if(versionJCOP3dot3){	//JCOP Version 3.3
				script = String.format("install_mmpp_000%s_jcop_33_encrypted.txt", String.valueOf(id));	
			}
			else{	//JCOP Version 3.1
				script = String.format("install_mmpp_000%s_encrypted.txt", String.valueOf(id));
			}
			
			
			// Save values for the processResult
			mScript = script;
			
			// Get the position in the list
			int order = mMyDbHelper.getCardToCreateOrder(getDeviceId());
			
			// Create the instance to be stored in the database
			Card card = new Card(0, id, 0, "", "", "", "", "", Card.STATUS_CREATING, false, false, R.drawable.card_blank, 0, Card.TYPE_PAYMENTS, order);
			mMyDbHelper.addCard(card, getDeviceId());		

			// Alert the user about the card creation
			MyNotification.show(PaymentCardActivity.this, 
					getResources().getString(R.string.notif_card_creating_title), getResources().getString(R.string.notif_card_creating_msg),
					MyNotification.NOTIF_ID_OPERATING);
			
			Intent broadcast = new Intent();
			broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_CREATING);
	        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
	        sendBroadcast(broadcast);

			AsyncTask<?, ?, ?> task = new ExecuteScriptTask(PaymentCardActivity.this, script).execute();
			setRunningAsyncTask(task);
			
			// Finish the view to let the user continue his work while the thread is working
			finish();
		} else {
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.card_max_number), Toast.LENGTH_LONG).show();
		}
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
	        // There was an error during creation so we remove it from the database
    		mMyDbHelper.deleteCard(mCardId, getDeviceId(), Card.TYPE_PAYMENTS);
			
			break;
		
		case ACTION_PERSO_CARD:
			// Error during personalization
    		mMyDbHelper.updateCardStatus(mCardId, Card.STATUS_FAILED, getDeviceId(), Card.TYPE_PAYMENTS);
			
			break;
			
		case ACTION_ACTIVATE_CARD:
			// Error during personalization
    		mMyDbHelper.updateCardStatus(mCardId, Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_PAYMENTS);
			
			break;
		}
	}
}
