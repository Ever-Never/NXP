package com.nxp.nxpwalletconndev.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.ssdp.btclient.BluetoothTLV;

public class ReadMifareDataTask extends AsyncTask<String, Integer, Boolean> {
	public static final int READ_MIFARE_DATA_TIMEOUT = 20000;
	
	int vcEntry;
	private String publicMdac = "D0020001D10100D40101D503010007";
	private String readMifareDataCommand = "";
	private Context ctx;
	private OnTransmitApduListener listener;
	
	public ReadMifareDataTask(Context ctx, int vcEntry) {
		this.ctx = ctx;
		this.vcEntry = vcEntry;
		this.listener = (OnTransmitApduListener) ctx;
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
		calcReadMifareDataCommand();
 
        // Don't forget about adding the vcEntry
		String vcEntryS = "00".substring(String.valueOf(vcEntry).length()) + String.valueOf(vcEntry); 
		
    	byte[] read = Parsers.parseHexString(vcEntryS + readMifareDataCommand);
        byte[] dataBT = BluetoothTLV.getTlvCommand(BluetoothTLV.LTSM_READ_MIFARE_DATA, read); 

        // Execute the APDU 
        listener.sendApduToSE(dataBT, READ_MIFARE_DATA_TIMEOUT);
        
        return false;
	}
	
	private void calcReadMifareDataCommand() {
		readMifareDataCommand = "";

		byte[] bMdacId = { (byte) 0xE7, 0x0F, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

		System.arraycopy(Parsers.hexToArray(publicMdac), 0, bMdacId, 2, 15);

		readMifareDataCommand = readMifareDataCommand.concat(Parsers
				.bytArrayToHex(bMdacId));

		// Perso available in blocks 0 and 1
		int vcBlockValue = 0x07;

		byte[] bBlockBitmap = { (byte) 0xB0, 0x04, (byte) 0xD5, 0x02, 0x00,
				0x00 };
		bBlockBitmap[4] = (byte) ((vcBlockValue >> 8) & 0xFF);
		bBlockBitmap[5] = (byte) (vcBlockValue & 0xFF);
		readMifareDataCommand = readMifareDataCommand.concat(Parsers
				.bytArrayToHex(bBlockBitmap));
	}
}
