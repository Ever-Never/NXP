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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.classes.Transaction;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnReadRecordResultListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.ssdp.btclient.BluetoothTLV;

public final class ReadTransactionLogTask extends AsyncTask<Integer, Void, ArrayList<Transaction>> {
	private static final String TAG = "ReadTransactionLogTask";
	
	private Context ctx;
	private OnReadRecordResultListener listener;
	private OnTransmitApduListener apduListener;
	private Card card;
	
	private int mId = 0;

	byte[] selectMMPP = {
			(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x0D, (byte) 0xA0, (byte) 0x00, (byte) 0x00, 
			(byte) 0x00, (byte) 0x04, (byte) 0x10, (byte) 0x10, (byte) 0x43, (byte) 0x41, (byte) 0x52, (byte) 0x54, 
			(byte) 0x41, (byte) 0x00 };

	byte[] readRecord = {
			(byte) 0x00, (byte) 0xB2, (byte) 0x00, (byte) 0x5C, (byte) 0x00 };	
	
	public LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
	
	static byte seResponse[] = null;
	
	public ReadTransactionLogTask(Context ctx, Card card) {
		this.ctx = ctx;
		this.listener = (OnReadRecordResultListener) ctx;
		this.card = card;
		this.apduListener = (OnTransmitApduListener) ctx;
		
		initCurrencyHashMap();
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		// Set the operation delegate
		BaseActivity.setOperationDelegate((OnOperationListener) ctx);
		
		// Make sure no other operations are completed until this one is completed
		MyPreferences.setCardOperationOngoing(ctx, true);
	}
	
	private void initCurrencyHashMap() {
		map.put("0840", "$");
		map.put("0978", "€");
	}

	@Override
	protected ArrayList<Transaction> doInBackground(final Integer... ids) {
		ArrayList<Transaction> txs = new ArrayList<Transaction>();
		mId = ids[0].intValue();
		
		// Update the AID with the last byte representing the id of the card
		selectMMPP[selectMMPP.length - 1] = (byte) mId;

		byte[] enableWiredModeTLV = BluetoothTLV.getTlvCommand(BluetoothTLV.WIRED_MODE_ENABLE, new byte[] {0x00} ); 
	    apduListener.sendApduToSE(enableWiredModeTLV, 4000);
		
	    seResponse = null;
	    while(isCancelled() == false) {
			if(seResponse != null) {
				if(seResponse[seResponse.length - 2] == (byte) 0x90 && seResponse[seResponse.length - 1] == (byte) 0x00) {
					break;
				} else {
					return txs;
				}
			}
		}
	    
	    // Make sure no other operations are completed until this one is completed
	 	MyPreferences.setCardOperationOngoing(ctx, true);
	    
	    try {
			// Select the CRS
			sendApduToSE(selectMMPP);
			
			// Set seResponse to null to force reading it from the PN66T
			seResponse = null;
			
			while(isCancelled() == false) {
				if(seResponse != null) {
					// Make sure no other operations are completed until this one is completed
					MyPreferences.setCardOperationOngoing(ctx, true);
					
					if(seResponse[seResponse.length - 2] == (byte) 0x90 && seResponse[seResponse.length - 1] == (byte) 0x00) {
						boolean readNextRecord = true;

				        // Read the transaction log for every record in the applet
						for(byte i = 1; i < 4 && readNextRecord == true; i++) {
							readRecord[2] = i;

							// Set the status of the card to activated
							sendApduToSE(readRecord);
							
							seResponse = null;
			
							while(isCancelled() == false) {
								if(seResponse != null) {
									// Make sure no other operations are completed until this one is completed
									MyPreferences.setCardOperationOngoing(ctx, true);
									
									// Show data on the screen
									if(seResponse[seResponse.length - 2] == (byte) 0x90 && seResponse[seResponse.length - 1] == (byte) 0x00) {
										// Applet successfully activated
										txs.add(parseTransaction(seResponse, mId));		
									} else 
										readNextRecord = false;
										
									break;
								}
							}
						}
						
						break;
					} else {
						break;
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
		
		return txs;
	}

	@Override
	protected void onPostExecute(final ArrayList<Transaction> txs) {
		super.onPostExecute(txs);
		
		// Action completed	
		if(listener != null && txs != null) {
			listener.onReadRecordResult(txs, mId);
		}
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
	
	private Transaction parseTransaction(byte[] resp, int id) {
		String date = "00".substring(Integer.toHexString(resp[9]).length()) + Integer.toHexString(resp[9])
				+ "/" + "00".substring(Integer.toHexString(resp[10]).length()) + Integer.toHexString(resp[10])
				+ "/" + "00".substring(Integer.toHexString(resp[11]).length()) + Integer.toHexString(resp[11]);

		byte[] bAmount = new byte[5];
		System.arraycopy(resp, 1, bAmount, 0, 5);
		String amount = Parsers.arrayToHex(bAmount).replaceFirst("^0+(?!$)", "") 
				+ "." + "00".substring(Integer.toHexString(resp[6] & 0xFF).length()) + Integer.toHexString(resp[6] & 0xFF);
		
		byte[] bCurrency = new byte[2];
		System.arraycopy(resp, 7, bCurrency, 0, 2);
		String currency = Parsers.arrayToHex(bCurrency);
		String currencyValue = map.get(currency);
		
		return new Transaction(card.getIconRsc(), card.getCardName(), date, amount, currencyValue);
	}
}