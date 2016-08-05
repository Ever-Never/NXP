/*                                                                                                                                                      
 * =============================================================================     
 *                                                                                   
 *                    Copyright (c), NXP Semiconductors                              
 *                                                                                   
 *                       (C)NXP Electronics N.V.2011                                 
 *         All rights are reserved. Reproduction in whole or in part is              
 *        prohibited without the written consent of the copyright owner.             
 *    NXP reserves the right to make changes without notice at any time.             
 *   NXP makes no warranty, expressed, implied or statutory, including but           
 *   not limited to any implied warranty of merchantability or fitness for any       
 *  particular purpose, or that the use will not infringe any third party patent,    
 *   copyright or trademark. NXP must not be liable for any loss or damage           
 *                            arising from its use.                                  
 *                                                                                   
 * =============================================================================     
 */

package com.nxp.nxpwalletconndev.tasks;

import java.util.LinkedHashMap;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnScriptIdListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.ssdp.btclient.BluetoothTLV;

public final class NextScriptIdTask extends AsyncTask<Integer, Void, Integer> {
	private static final String TAG = "ReadTransactionLogTask";

	private List<Integer> ids;
	private Context ctx; 
	private OnScriptIdListener listener;
	private OnTransmitApduListener apduListener;
	
	byte[] selectMMPP = {
			(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x0D, (byte) 0xA0, (byte) 0x00, (byte) 0x00, 
			(byte) 0x00, (byte) 0x04, (byte) 0x10, (byte) 0x10, (byte) 0x43, (byte) 0x41, (byte) 0x52, (byte) 0x54, 
			(byte) 0x41, (byte) 0x00 };
	
	public LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
	
	static byte seResponse[] = null;
	
	public NextScriptIdTask(Context ctx, List<Integer> ids) {
		this.ctx = ctx;
		this.ids = ids;
		this.listener = (OnScriptIdListener) ctx;
		this.apduListener = (OnTransmitApduListener) ctx;
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
	protected Integer doInBackground(final Integer... params) {
		byte[] enableWiredModeTLV = BluetoothTLV.getTlvCommand(BluetoothTLV.WIRED_MODE_ENABLE, new byte[] {0x00} ); 
	    apduListener.sendApduToSE(enableWiredModeTLV, 4000);

	    seResponse = null;
	    while(isCancelled() == false) {
			if(seResponse != null) {
				if(seResponse[seResponse.length - 2] == (byte) 0x90 && seResponse[seResponse.length - 1] == (byte) 0x00) {
					break;
				} else
					return 6;
			}
		}
	    
	    // We test ids from 1 to 5
	    int id = 1;
	    
	    try {
	    	boolean isIdAvailable = false;
	    		
	    	while(isIdAvailable == false && id <= 5) {
	    		// Make sure no other operations are completed until this one is completed
	    		MyPreferences.setCardOperationOngoing(ctx, true);
	    		
	    		// Skip already used AIDs
	    		if(ids != null && ids.isEmpty() == false && ids.contains(id)) {
	    			id = id + 1;
	    		} else {
			    	// Update the AID with the last byte representing the id of the card
					selectMMPP[selectMMPP.length - 1] = (byte) id;
					
					// Select the CRS
					sendApduToSE(selectMMPP);
					
					// Set seResponse to null to force reading it from the PN66T
					seResponse = null;
					
					while(isCancelled() == false) {
						if(seResponse != null) {
							if(seResponse[seResponse.length - 2] != (byte) 0x90 || seResponse[seResponse.length - 1] != (byte) 0x00) {
								isIdAvailable = true;
							} else {
								id = id + 1;
							}
							
							break;						
						}
					}
	    		}
	    	}
		} catch (Exception e) {
			e.printStackTrace();
		}
	    
	    // Make sure no other operations are completed until this one is completed
	 	MyPreferences.setCardOperationOngoing(ctx, true);
		
	    byte[] disableWiredModeTLV = BluetoothTLV.getTlvCommand(BluetoothTLV.WIRED_MODE_DISABLE, new byte[] {0x00} ); 
	    apduListener.sendApduToSE(disableWiredModeTLV, 4000);
		
		seResponse = null;
		while(isCancelled() == false) {
			if(seResponse != null) {
				break;
			}
		}
		
		return id;
	}

	@Override
	protected void onPostExecute(final Integer id) {
		super.onPostExecute(id);
		
		// Action completed	
		listener.onScriptId(id);
	}

	@Override
	protected void onCancelled() {	

	}
	
	public void sendApduToSE(byte[] apdu)  {
		Log.d(TAG, "before excange, apdu from Server: " + Parsers.arrayToHex(apdu));

		// Execute the APDU
		byte[] dataBT = BluetoothTLV.getTlvCommand(BluetoothTLV.SEND_RAW_APDU, apdu); 
		apduListener.sendApduToSE(dataBT, 4000);
	}
	
	public static void receiveApduFromSE(byte[] apdu) {
		Log.i(TAG, "after excange, apdu from SE: " + Parsers.arrayToHex(apdu));
		
		seResponse = apdu;
	}
}