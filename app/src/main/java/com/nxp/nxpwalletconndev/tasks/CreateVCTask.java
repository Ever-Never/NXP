package com.nxp.nxpwalletconndev.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.notifications.MyNotification;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.ssdp.btclient.BluetoothTLV;
import com.nxp.ssdp.encryption.JSBLEncryption;


public class CreateVCTask extends AsyncTask<String, Integer, Boolean> {
	public static final int CREATE_VC_TIMEOUT = 40000;
	
	public static final byte SP_SD_KVN1 = 0x48; // Must not be 0x00
	public static final byte[] SP_SD_ENC1 = Parsers.hexToArray("606162636465666768696A6B6C6D6E6F");
	public static final byte[] SP_SD_MAC1 = Parsers.hexToArray("606162636465666768696A6B6C6D6E6F");
	public static final byte[] SP_SD_DEK1 = Parsers.hexToArray("606162636465666768696A6B6C6D6E6F");
	
	String createVCData;
	String persoVCData;
	OnTransmitApduListener listener;
	Context ctx;
	
	private static JSBLEncryption jsblEncryption;
	public static String JSBL_KEY_FILENAME = "";

	public CreateVCTask(Context ctx, String createVCData, String persoVCData) {
		this.ctx = ctx;
		this.createVCData = createVCData;
		this.persoVCData = persoVCData;
		this.listener = (OnTransmitApduListener) ctx;
		
		// Set the keyfile with the keys
		JSBL_KEY_FILENAME = "keyfile.txt";
		
		// Load the JSBL keys
		jsblEncryption = new JSBLEncryption(JSBL_KEY_FILENAME, ctx);
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		// Set the operation delegate
		BaseActivity.setOperationDelegate((OnOperationListener) ctx);
		
		// Make sure no other operations are completed until this one is completed
		MyPreferences.setCardOperationOngoing(ctx, true);
	}

	@Override
	protected Boolean doInBackground(String... scripts) {
		// Alert the user about the card creation
		MyNotification.show(ctx, 
				ctx.getResources().getString(R.string.notif_card_creating_title), ctx.getResources().getString(R.string.notif_card_creating_msg),
				MyNotification.NOTIF_ID_OPERATING);
    	
        byte[] VC_Data = Parsers.parseHexProperty("VCCreateCommand", createVCData);
        
        if(persoVCData != null) {
	        byte[] keySet = new byte[1 + 16 + 16 + 16];
			keySet[0] = SP_SD_KVN1;
			System.arraycopy(SP_SD_ENC1, 0, keySet, 1 + 16 * 0, 16);
			System.arraycopy(SP_SD_MAC1, 0, keySet, 1 + 16 * 1, 16);
			System.arraycopy(SP_SD_DEK1, 0, keySet, 1 + 16 * 2, 16);
	
			byte[] persoKeys = jsblEncryption.getEncryptedTLV(keySet, (byte) 0x61);
			persoVCData = persoVCData.concat(Parsers.bytArrayToHex(persoKeys));
			byte[] PersonalizeData = Parsers.hexToArray(persoVCData);
			
			// VC Creation + Perso data
			byte[] vcCommand = new byte[VC_Data.length + PersonalizeData.length];
			System.arraycopy(VC_Data, 0, vcCommand, 0, VC_Data.length);
			System.arraycopy(PersonalizeData, 0, vcCommand, VC_Data.length, PersonalizeData.length);
			
			// Store the final commands and the VC Creation Data
			VC_Data = vcCommand;
        }
        
        byte[] dataBT = BluetoothTLV.getTlvCommand(BluetoothTLV.LTSM_CREATE_VC, VC_Data); 
        
        // Execute the APDU 
     	listener.sendApduToSE(dataBT, CREATE_VC_TIMEOUT);
        
        return false;
	}
}
