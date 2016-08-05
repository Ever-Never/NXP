package com.nxp.nxpwalletconndev.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothReadListener;

public class MyMifareCreateVCActivity extends BaseActivity implements OnBluetoothReadListener, OnTransmitApduListener, OnOperationListener, OnItemSelectedListener, TextWatcher, OnCheckedChangeListener {
    private static final String TAG = "CreateVCParamsActivity";
    
    public static final int TEMP_CARD_ID = 0xF4;
    
//    private static final int RESULT_LOAD_IMAGE = 0;
    
    private TextView tDesfCrypto;
    private TextView tDesfApps;
    
    private LinearLayout lDesfCrypto;
    private LinearLayout lDesfApps;
    
    private EditText vcName;
    private EditText vcKeyset;
    private EditText vcKeyVersion;
    private EditText eDesfApps;
    
    private CheckBox vcKeysetDefault;

    private Spinner vcType;
    private Spinner vcUidClassic;
    private Spinner vcUidDESFire;
    private Spinner vcDesfCrypto;
    
//    private ImageView vcIcon;
      
    private String vcTypeValue = "0000";
    private String vcUidValue = "00";
    private String vcKeysetValue = "00000000000000000000000000000000";
    private String vcDesfCryptoValue = "00";
    
    private String VCCreateCommand = "";
//    private String vcIconPath = "";
    
    List<String> listApps = new ArrayList<String>();
    
    private MyDbHelper mMyDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mymifareapp_createvc);
        
        ((Button) findViewById(R.id.button_create)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(vcTypeValue.startsWith("02") && listApps.isEmpty() == true) {
					Toast.makeText(MyMifareCreateVCActivity.this, "List of AIDs missing", Toast.LENGTH_LONG).show();
				} else {
					if(isDeviceConnected() == true) {
						if(MyPreferences.isCardOperationOngoing(MyMifareCreateVCActivity.this) == false) {
							createVC();
						} else
             			   Toast.makeText(MyMifareCreateVCActivity.this, "Operation not possible while card is under process", Toast.LENGTH_LONG).show();
					} else
		              	Toast.makeText(MyMifareCreateVCActivity.this, "There is no valid Bluetooth Connection", Toast.LENGTH_LONG).show();
				}
			}
		});
        
        ((Button) findViewById(R.id.button_return)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				
			}
		});
        
        ((ImageView) findViewById(R.id.imageKeysetInfo)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showKeysetInfoDialog();
				
			}
		});
        
        ((ImageView) findViewById(R.id.imageDesfAids)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String appAid = eDesfApps.getText().toString();
				if(eDesfApps != null && eDesfApps.length() == 6) {
					listApps.add(appAid);
					
					String aids = "";
					for(String aid : listApps)
						aids = aids.concat(aid + " ");
					
					// Show the aids on the screen
					tDesfApps.setText("AIDs: " + aids);
					
					// Reset the field
					eDesfApps.setText("");
					
					// Update the VC Creation Command
					calcVCCreateCommand();
				} else {
					Toast.makeText(getApplicationContext(), "Invalid AID", Toast.LENGTH_LONG).show();
				}
			}
		});
        
        // Get database helper
     	mMyDbHelper = new MyDbHelper(this);
        
//        vcCreatePerso = (TextView) findViewById(R.id.vcCreatePerso);
        
        lDesfCrypto = (LinearLayout) findViewById(R.id.layoutDesfCrypto);
        lDesfApps = (LinearLayout) findViewById(R.id.lDesfApps);
        
        tDesfCrypto = (TextView) findViewById(R.id.textDesfCrypto);
        tDesfApps = (TextView) findViewById(R.id.textDesfAids);
        
        vcName = (EditText) findViewById(R.id.vcName);
        vcKeyset = (EditText) findViewById(R.id.vcKeyset);
        vcKeyVersion = (EditText) findViewById(R.id.vcKeyVersion);
        eDesfApps = (EditText) findViewById(R.id.editDesfAids);
        
        vcKeysetDefault = (CheckBox) findViewById(R.id.vcKeysetDefault);
        
        vcType = (Spinner) findViewById(R.id.vcType);
        vcUidClassic = (Spinner) findViewById(R.id.vcUidClassic);
        vcUidDESFire = (Spinner) findViewById(R.id.vcUidDESFire);
        vcDesfCrypto = (Spinner) findViewById(R.id.vcDesfCrypto);
         
        vcKeyset.addTextChangedListener(this);
        vcKeysetDefault.setOnCheckedChangeListener(this);
        
        vcType.setOnItemSelectedListener(this);
        vcUidClassic.setOnItemSelectedListener(this);
        vcUidDESFire.setOnItemSelectedListener(this);
        vcDesfCrypto.setOnItemSelectedListener(this);
        
//		vcIcon = (ImageView) findViewById(R.id.vcIconImage);
//		vcIcon.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//		        startActivityForResult(i, RESULT_LOAD_IMAGE);		
//			}
//		});
    }
    
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
//            Uri selectedImage = data.getData();
//            String[] filePathColumn = { MediaStore.Images.Media.DATA };
//            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
//            cursor.moveToFirst();
//            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//            String picturePath = cursor.getString(columnIndex);
//            cursor.close();
//            
//            vcIcon.setImageBitmap(BitmapFactory.decodeFile(picturePath));
//            vcIconPath = picturePath;
//        }
//    }
    
    public void createVC() {
        Log.d("MyMifareApp", "CreateVC Command: " + VCCreateCommand);
        
        // Set the creation of a fake card to let the user see Creating Card
		Card vc = new Card(0, 0, TEMP_CARD_ID, "", "", "", "", "", Card.STATUS_CREATING, false, false, R.drawable.card_blank, Card.MIFARE_HOSPITALITY, Card.TYPE_MIFARE_CLASSIC, 100);			
		mMyDbHelper.addCard(vc, getDeviceId());
 
        AsyncTask<?, ?, ?> task = new CreateVCTask(MyMifareCreateVCActivity.this, VCCreateCommand, null).execute();	
		setRunningAsyncTask(task);
        
		finish();
	}
        
    @Override
    public void processOperationResult(final byte[] result) {		
    	Intent broadcast = new Intent();
		short status = Parsers.getSW(result);
		
		// Regardless of the result I remove the temporary entry
		mMyDbHelper.deleteCard(TEMP_CARD_ID, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);

        switch (status) {
	        case StatusBytes.SW_NO_ERROR:
	        	int order = mMyDbHelper.getCardToCreateOrder(getDeviceId());
	        	Card vcEntry;
	        	
	        	if(vcTypeValue.startsWith("01")) {
	        		vcEntry = Parsers.getVcEntry(result, vcName.getText().toString(), "", R.drawable.card_classic, Card.MIFARE_MYMIFAREAPP, Card.TYPE_MIFARE_CLASSIC, order);
	        	} else {
	        		vcEntry = Parsers.getVcEntry(result, vcName.getText().toString(), "", R.drawable.card_desfire, Card.MIFARE_MYMIFAREAPP, Card.TYPE_MIFARE_DESFIRE, order);		
	        	}
	        		        		
				mMyDbHelper.addCard(vcEntry, getDeviceId());
				
				// Store the VCEntry Identifier
//				mVcEntry = vc.getIdVc();
				
				// Card created
				if(vcTypeValue.startsWith("01")) {
					mMyDbHelper.updateCardStatus(vcEntry.getIdVc(), Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
				} else {
					mMyDbHelper.updateCardStatus(vcEntry.getIdVc(), Card.STATUS_PERSONALIZED, getDeviceId(), Card.TYPE_MIFARE_DESFIRE);
				}
				
				broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_PERSONALIZED);
		        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
		        sendBroadcast(broadcast);
	        	
	            break;
	        default:
	        	Toast.makeText(getApplicationContext(), "Error Occured :" +  Parsers.bytArrayToHex(result), Toast.LENGTH_LONG).show();
	        	
	        	broadcast = new Intent();
				broadcast.putExtra(MyCardsActivity.BROADCAST_EXTRA, Card.STATUS_FAILED);
		        broadcast.setAction(MyCardsActivity.BROADCAST_ACTION);
		        sendBroadcast(broadcast);
	        	
	            break;
        }
	    
        Log.i(TAG, "CreateVC Received Data" + Parsers.bytArrayToHex(result));
    }
    
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		position = vcType.getSelectedItemPosition();
		vcTypeValue = view.getResources().getStringArray(R.array.vcTypeValues)[position];
		
		if(vcTypeValue.startsWith("01")) { // This is MIFARE Classic
			vcUidClassic.setVisibility(View.VISIBLE);
			vcUidDESFire.setVisibility(View.GONE);
			
			lDesfCrypto.setVisibility(View.GONE);
			tDesfCrypto.setVisibility(View.GONE);
			lDesfApps.setVisibility(View.GONE);
			
			position = vcUidClassic.getSelectedItemPosition();
		} else {
			vcUidClassic.setVisibility(View.GONE);
			vcUidDESFire.setVisibility(View.VISIBLE);
			
			lDesfCrypto.setVisibility(View.VISIBLE);
			tDesfCrypto.setVisibility(View.VISIBLE);			
			lDesfApps.setVisibility(View.VISIBLE);
			
			position = vcDesfCrypto.getSelectedItemPosition();
			vcDesfCryptoValue = view.getResources().getStringArray(R.array.vcDesfCryptoValues)[position];
			
			if(vcDesfCryptoValue.equals("02"))
				vcKeyVersion.setVisibility(View.VISIBLE);
			else
				vcKeyVersion.setVisibility(View.GONE);
			
			position = vcUidDESFire.getSelectedItemPosition();
		}
		
		// The same array of values is valid for both Classic and DESFire
		vcUidValue = view.getResources().getStringArray(R.array.vcUidValues)[position];
		
		calcVCCreateCommand();
	}

	/*
	 * Parses Create Virtual Card command according to inputs
	 */
	private void calcVCCreateCommand() {
		// Reset VC Create Command
		VCCreateCommand = "";
		
		byte[] bVcConf = {(byte) 0x46, 0x01, 0x00 };
//		System.arraycopy(Parsers.hexToArray(vcConfValue), 0, bVcConf, 2, 1);
			
		VCCreateCommand = VCCreateCommand.concat(Parsers.arrayToHex(bVcConf));
		
		byte[] bVcType = {(byte) 0xA5, 0x07, 0x02, 0x02, 0x00, 0x00, 0x03, 0x01, 0x00};
		System.arraycopy(Parsers.hexToArray(vcTypeValue), 0, bVcType, 4, 2);
		System.arraycopy(Parsers.hexToArray(vcUidValue), 0, bVcType, 8, 1);
		
		VCCreateCommand = VCCreateCommand.concat(Parsers.arrayToHex(bVcType));
					
		// SAK Value
		if(vcTypeValue.equals("0101") == true) // MIFARE Classic 1kB
			VCCreateCommand = VCCreateCommand.concat("A60705020400060108");
		else if(vcTypeValue.equals("0104") == true) // MIFARE Classic 4kB 
			VCCreateCommand = VCCreateCommand.concat("A60705020200060118");
		else if(vcTypeValue.equals("0202") == true) // MIFARE DESFire 2kB
			VCCreateCommand = VCCreateCommand.concat("A60705024403060120");
		else if(vcTypeValue.equals("0204") == true) // MIFARE DESFire 4kB
			VCCreateCommand = VCCreateCommand.concat("A60705024403060120");
		else if(vcTypeValue.equals("0208") == true) // MIFARE DESFire 8kB
			VCCreateCommand = VCCreateCommand.concat("A60705024403060120");
		
		// Add string for the UID detection
		if(vcUidValue.equals("00") == true) {
			VCCreateCommand = VCCreateCommand.concat("A11F80080FFFFFFFFFFFFFFF810100820200008301008401008501008603000000");
		} else {
			VCCreateCommand = VCCreateCommand.concat("A11C80050FFFFFFFFF81011F8202FFFF8301008401008501008603000000");
		}

		String cryptoValue = "";
		
		if(vcTypeValue.startsWith("01")) {
			byte[] bVcKeysetPlain = {(byte) 0xA8, 0x12, 0x20, 0x10, 
					0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
			
			System.arraycopy(Parsers.hexToArray(vcKeysetValue), 0, bVcKeysetPlain, 4, Parsers.hexToArray(vcKeysetValue).length);
			cryptoValue = Parsers.arrayToHex(bVcKeysetPlain);
			VCCreateCommand = VCCreateCommand.concat(cryptoValue);
		} else if(vcTypeValue.startsWith("02")) {
			if(vcDesfCryptoValue.equals("00")) {
				byte[] bVcKeysetPlain = {(byte) 0xA8, 0x13, 0x20, 0x11, 
						0x00, 
						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
				
				bVcKeysetPlain[4] = 0x00;
				System.arraycopy(Parsers.hexToArray(vcKeysetValue), 0, bVcKeysetPlain, 5, Parsers.hexToArray(vcKeysetValue).length);
				cryptoValue = Parsers.arrayToHex(bVcKeysetPlain);
			} else if(vcDesfCryptoValue.equals("01")) {
				byte[] bVcKeysetPlain = {(byte) 0xA8, 0x21, 0x20, 0x19, 
						0x00, 
						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
				
				bVcKeysetPlain[4] = 0x01;
				System.arraycopy(Parsers.hexToArray(vcKeysetValue), 0, bVcKeysetPlain, 5, Parsers.hexToArray(vcKeysetValue).length);
				cryptoValue = Parsers.arrayToHex(bVcKeysetPlain);
			} else if(vcDesfCryptoValue.equals("02")) {
				byte[] bVcKeysetPlain = {(byte) 0xA8, 0x14, 0x20, 0x12, 
						0x00, 
						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
						0x00};
				
				bVcKeysetPlain[4] = 0x02;
				System.arraycopy(Parsers.hexToArray(vcKeysetValue), 0, bVcKeysetPlain, 5, Parsers.hexToArray(vcKeysetValue).length);
				
				String vcKeyVersionValue = vcKeyVersion.getText().toString();
				if(vcKeyVersionValue != null && vcKeyVersionValue.length() == 2)
					bVcKeysetPlain[21] = Parsers.hexToArray(vcKeyVersionValue)[0];
				else
					bVcKeysetPlain[21] = 0x00;
				
				cryptoValue = Parsers.arrayToHex(bVcKeysetPlain);
			}
			
			VCCreateCommand = VCCreateCommand.concat(cryptoValue);
		}	
		
		if(vcTypeValue.startsWith("02")) {
			String concurrentActivation = "E2020001";
			VCCreateCommand = VCCreateCommand.concat(concurrentActivation);
			
			String desfAids = "F8";
						
			desfAids = desfAids.concat("00".substring(
					Integer.toHexString(listApps.size() * 3).length()) + Integer.toHexString(listApps.size() * 3));
				
			for(String aid : listApps)
				desfAids = desfAids.concat(aid);
				
			VCCreateCommand = VCCreateCommand.concat(desfAids);
		}
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void afterTextChanged(Editable s) {
		String sKeyset = vcKeyset.getText().toString();
		
		if(sKeyset.length() == 32)
			vcKeysetValue = sKeyset;
		
		calcVCCreateCommand();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO : Check if we have desfire or classic
		
		switch (buttonView.getId()) {
			case R.id.vcKeysetDefault:
				if(isChecked) {
					if(vcTypeValue.startsWith("01"))
						vcKeyset.setText(getResources().getString(R.string.vcClassicKeysetDefaultValue));
					else {
						if(vcDesfCryptoValue.equals("00")) {
							vcKeyset.setText(getResources().getString(R.string.vc3DESAESDesfireKeysetDefaultValue));
						} else if(vcDesfCryptoValue.equals("01")) {
							vcKeyset.setText(getResources().getString(R.string.vc3K3DESDesfireKeysetDefaultValue));
						} else if(vcDesfCryptoValue.equals("02")) {
							vcKeyset.setText(getResources().getString(R.string.vc3DESAESDesfireKeysetDefaultValue));
						}
					}
				} else
					vcKeyset.setText("");
				
				break;
		}
		
		calcVCCreateCommand();
	}
	
	private void showKeysetInfoDialog() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(MyMifareCreateVCActivity.this);
        builder.setTitle(R.string.vcClassicKeysetInfoDialogTitle);
        builder.setMessage(R.string.vcClassicKeysetInfoDialogMsg);
        
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {                      
                       dialog.dismiss();
                   }
               });
        
        // Create the AlertDialog object and return it
        builder.create();
        builder.show();
	}

	private void showCommandDialog() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(MyMifareCreateVCActivity.this);
        builder.setTitle(R.string.vcCommand);
        builder.setMessage(VCCreateCommand);
        
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.dismiss();
                   }
               });

        // Create the AlertDialog object and return it
        builder.create();
        builder.show();
	}
    
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//    
//    @Override
//	public boolean onOptionsItemSelected(MenuItem item){
//	    switch(item.getItemId()){
//	    case R.id.action_about:
//	    	showCommandDialog();
//	        return true;            
//	    }
//	    return super.onOptionsItemSelected(item);
//	}

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
		
        Toast.makeText(getApplicationContext(), "MyMifare Error detected in BLE channel", Toast.LENGTH_LONG).show();
		
//    	switch (mAction) {
//		case ACTION_CREATE_VC:
			mMyDbHelper.deleteCard(TEMP_CARD_ID, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
//			break;
//			
//		case ACTION_ADD_AND_UPDATE_MDAC_VC:
//    		mMyDbHelper.updateCardStatus(mVcEntry, Card.STATUS_FAILED, getDeviceId(), Card.TYPE_MIFARE_CLASSIC);
//    		break;
//    	}		
		
	}
	
	
}
