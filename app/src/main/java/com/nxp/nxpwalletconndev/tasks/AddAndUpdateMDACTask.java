package com.nxp.nxpwalletconndev.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.ssdp.btclient.BluetoothTLV;
import com.nxp.ssdp.encryption.CmacCalculator;
import com.nxp.ssdp.encryption.JSBLEncryption;


public class AddAndUpdateMDACTask extends AsyncTask<String, Integer, Boolean> {
	public static final int ADD_AND_UPDATE_MDAC_TIMEOUT = 20000;
	
	int vcEntry;
	Context ctx;
	OnTransmitApduListener listener;
	
	private String publicMdac = "D0020001D10100D40101D503010007";
	private String privateMdac = "";
	private String addAndUpdateMdacCommand = "";
	private String vcMFPassValue;
	
	private static JSBLEncryption jsblEncryption;
	private static String JSBL_KEY_FILENAME = "";
	
	public AddAndUpdateMDACTask(Context ctx, int vcEntry, String vcMFPassValue) {
		this.ctx = ctx; 
		this.vcEntry = vcEntry;
		this.vcMFPassValue = vcMFPassValue;
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
     	calcPrivateMdac();
    	
    	// Don't forget about adding the vcEntry
    	String vcEntryS = "00".substring(String.valueOf(vcEntry).length()) + String.valueOf(vcEntry);   	
    	byte[] mdac = Parsers.parseHexString(vcEntryS + addAndUpdateMdacCommand);

        byte[] dataBT = BluetoothTLV.getTlvCommand(BluetoothTLV.LTSM_ADD_AND_UPDATE_MDAC, mdac); 

        // Execute the APDU 
     	listener.sendApduToSE(dataBT, ADD_AND_UPDATE_MDAC_TIMEOUT);
        
        return false;
	}
	
	/*
	 * Parses Add and Update MDAC command according to inputs
	 */
	private void calcPrivateMdac() {
		privateMdac = "";

		// The AES Key does not change
		byte[] bAesCmacKey = { (byte) 0x20, (byte) 0x12, (byte) 0xE6, 0x10,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		System.arraycopy(
				Parsers.hexToArray("F0F1F2F3CFCECDCC848586874B4A4948"), 0,
				bAesCmacKey, 4, 16);

		privateMdac = privateMdac.concat(Parsers.arrayToHex(bAesCmacKey));

		byte[] bPrivMdac = { (byte) 0x21, (byte) 0x20, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

		byte[] bPublicMdac = Parsers.hexToArray(publicMdac);

		byte[] bMdacId = { (byte) 0xD0, 0x02, 0x00, 0x00 };
		bMdacId[2] = bPublicMdac[2];
		bMdacId[3] = bPublicMdac[3];
		System.arraycopy(bMdacId, 0, bPrivMdac, 2, bMdacId.length);

		byte[] bMFPass = { (byte) 0xD2, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00 };
		
		System.arraycopy(Parsers.hexToArray(vcMFPassValue), 0, bMFPass, 2, 8);
		System.arraycopy(bMFPass, 0, bPrivMdac, 6, bMFPass.length);

		// 03 is omitted for Oppo phones in order to avoid the bug in the M4M May version
		byte[] bCmac = CmacCalculator.CMAC(
				Parsers.hexToArray("F0F1F2F3CFCECDCC848586874B4A4948"),
				Parsers.hexToArray("00000000000000000000000000000000"),
				Parsers.hexToArray("03".concat(publicMdac)));

		byte[] bPublicMdacEnc = { (byte) 0xD3, 0x10, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00 };
		System.arraycopy(bCmac, 0, bPublicMdacEnc, 2, 16);
		System.arraycopy(bPublicMdacEnc, 0, bPrivMdac, 16,
				bPublicMdacEnc.length);

		privateMdac = privateMdac.concat(Parsers.arrayToHex(bPrivMdac));

		// The PrivateMdac is read. We can calculate the MDAC
		calcAddAndUpdateMdacCommand();
	}

	private void calcAddAndUpdateMdacCommand() {
		addAndUpdateMdacCommand = "";

		addAndUpdateMdacCommand = Parsers.bytArrayToHex(jsblEncryption
				.getEncryptedTLV(Parsers.hexToArray(privateMdac), (byte) 0x62));
	}
}
