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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.listeners.OnActivateResultListener;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.ssdp.btclient.BluetoothTLV;

public final class ActivateCreditCardTask extends AsyncTask<Integer, Void, Boolean> {
	private static final String TAG = "ActivateCreditCardTask";
	
	private Context ctx;
	private OnActivateResultListener listener;
	private OnTransmitApduListener apduListener;
	
	private boolean activate;

	private int mId = 0;

	byte[] selectCRS = {
			(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x09, (byte) 0xA0, (byte) 0x00, (byte) 0x00, 
			(byte) 0x01, (byte) 0x51, (byte) 0x43, (byte) 0x52, (byte) 0x53, (byte) 0x00, (byte) 0x00 };

	byte[] setStatus = {
			(byte) 0x80, (byte) 0xF0, (byte) 0x01, (byte) 0x00, (byte) 0x0F, (byte) 0x4F, (byte) 0x0D, 
			(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x10, (byte) 0x10, 
			(byte) 0x43, (byte) 0x41, (byte) 0x52, (byte) 0x54, (byte) 0x41, (byte) 0x00
	};
	
	static byte[] seResponse;
	
	// /send 00A4040009A0000001514352530000
	// /send 80F001010F4F0DA0000000041010434152544101
	// Updated: A102276F25840DA0000000041010434152544106A514500A4D617374657243617264BF0C059F4D020B0A
		
	public ActivateCreditCardTask(Context ctx, boolean activate) {
		this.ctx = ctx;
		this.activate = activate;
		this.listener = (OnActivateResultListener) ctx;
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
	protected Boolean doInBackground(final Integer... aids) {
		boolean success = false;
	    mId = aids[0].intValue();
	    	    
	    byte[] enableWiredModeTLV = BluetoothTLV.getTlvCommand(BluetoothTLV.WIRED_MODE_ENABLE, new byte[] {0x00} ); 
	    apduListener.sendApduToSE(enableWiredModeTLV, 4000);

	    seResponse = null;
	    while(isCancelled() == false) {
			if(seResponse != null) {
				if(seResponse[seResponse.length - 2] == (byte) 0x90 && seResponse[seResponse.length - 1] == (byte) 0x00) {
					break;
				} else
					return false;
			}
		}
	    
		try {		
			mId = aids[0].intValue();
			
			// Make sure no other operations are completed until this one is completed
			MyPreferences.setCardOperationOngoing(ctx, true);

			// Select the CRS
			sendApduToSE(selectCRS);
			
			// Set seResponse to null to force reading it from the PN66T
			seResponse = null;
			
			while(isCancelled() == false) {
				if(seResponse != null) {
					if(seResponse[seResponse.length - 2] == (byte) 0x90 && seResponse[seResponse.length - 1] == (byte) 0x00) {
						// Before we activate the card we need to deactivate the previous one
						if(activate == true) {
							int index = 1;
							
							while(index <= 5 && isCancelled() == false) {
								// Make sure no other operations are completed until this one is completed
								MyPreferences.setCardOperationOngoing(ctx, true);
								
								if(index == mId) {
									index++;
									continue;
								}
								
								// Update the last byte that identifies the instance
								setStatus[setStatus.length - 1] = (byte) index;
															
								// Set the status of the card to activated
								sendApduToSE(setStatus);
								
								seResponse = null;
				
								while(isCancelled() == false) {
									if(seResponse != null) {
										break;
									}
								}
								
								index++;
							}
						}

						// Make sure no other operations are completed until this one is completed
						MyPreferences.setCardOperationOngoing(ctx, true);
						
						// Update the last byte that identifies the instance
						setStatus[setStatus.length - 1] = aids[0].byteValue();
						
						// See whether I want to activate or deactivate
						setStatus[3] = (byte) (activate ? 0x01 : 0x00);
						
						// Set the status of the card to activated
						sendApduToSE(setStatus);
						
						seResponse = null;
		
						while(isCancelled() == false) {
							if(seResponse != null) {
						
								// Show data on the screen
								if(seResponse[seResponse.length - 2] == (byte) 0x90 && seResponse[seResponse.length - 1] == (byte) 0x00) {
									// Applet successfully activated
									success = true;
								} else {
									success = false;
								}
								
								break;
							}
						}
						
						break;
					} else {
						success = false;
						
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
		
		return success;
	}

	@Override
	protected void onPostExecute(final Boolean result) {
		super.onPostExecute(result);

		if(listener != null)
			listener.onActivateResult(result, activate, mId);
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