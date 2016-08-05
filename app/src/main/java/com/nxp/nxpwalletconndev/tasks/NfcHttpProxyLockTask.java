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

import java.util.LinkedList;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.nxp.nxpwalletconndev.activities.BaseActivity;
import com.nxp.nxpwalletconndev.activities.MyCardsActivity;
import com.nxp.nxpwalletconndev.httpcomm.HttpProtokollConstants;
import com.nxp.nxpwalletconndev.httpcomm.HttpResponseJson;
import com.nxp.nxpwalletconndev.httpcomm.HttpTransaction;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.listeners.OnTransmitApduListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.ssdp.btclient.BluetoothTLV;

public final class NfcHttpProxyLockTask extends AsyncTask<Intent, Void, StringBuffer> {
	private static final String TAG = "NfcHttpProxyLockTask";

	private StringBuffer console;
	private String serverUrl;
	private String scriptIndex;
	private String profile;
	private boolean lock;
	private String errorCause = "";
	private Context ctx;
	private OnTransmitApduListener apduListener;
	
	static byte seResponse[] = null;

	public NfcHttpProxyLockTask(Context ctx, String serverUrl, String scriptIndex, boolean lock, String profile) {
		this.ctx = ctx;
		this.serverUrl = serverUrl;
		this.scriptIndex = scriptIndex;
		this.profile = profile;
		this.lock = lock;
		console = new StringBuffer("");
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
	protected StringBuffer doInBackground(final Intent... args) {
		byte[] enableWiredModeTLV = BluetoothTLV.getTlvCommand(BluetoothTLV.WIRED_MODE_ENABLE, new byte[] {0x00} ); 
	    apduListener.sendApduToSE(enableWiredModeTLV, 4000);

		seResponse = null;
		while(isCancelled() == false) {
			if(seResponse != null) {
				if(seResponse[seResponse.length - 2] == (byte) 0x90 && seResponse[seResponse.length - 1] == (byte) 0x00) {
					break;
				} else {
					console.append("\nError opening channel\n");
					return console;
				}
			}
		}
		
		// Make sure no other operations are completed until this one is completed
		MyPreferences.setCardOperationOngoing(ctx, true);
 	    
		try {		
			console.append("\nStarting new http Transaction\n");
			Log.i(TAG, "Starting new http Transaction");

			String transactionId = Integer.toHexString(new Random().nextInt());
			LinkedList<NameValuePair> params = new LinkedList<NameValuePair>();

			params.add(new BasicNameValuePair(HttpProtokollConstants.ParameterNameType, HttpProtokollConstants.TypeValueInitTransaction));
			params.add(new BasicNameValuePair(HttpProtokollConstants.ParameterNameTransactionId, transactionId));
			params.add(new BasicNameValuePair(HttpProtokollConstants.ParameterId, scriptIndex));
			params.add(new BasicNameValuePair(HttpProtokollConstants.ParameterCardData, profile));
			
			if(lock == true)
				params.add(new BasicNameValuePair(HttpProtokollConstants.ParameterAction, "block"));
			else
				params.add(new BasicNameValuePair(HttpProtokollConstants.ParameterAction, "unblock"));

			HttpTransaction httpSender = new HttpTransaction(console, ctx);
			String httpServerResponseString = "";

			errorCause = "Error while sending Request to the Remote Web Server";
			httpServerResponseString = httpSender.executeHttpGet(params, serverUrl);
			Log.i(TAG, "HTTP Response String: " + httpServerResponseString);

			HttpResponseJson httpServerResponse = new HttpResponseJson(httpServerResponseString);

			while (httpServerResponse.isValid() && httpServerResponse.getType().equalsIgnoreCase(HttpProtokollConstants.TypeValueCommandApdu)
					&& isCancelled() == false) {
				if (!httpServerResponse.isValid()) {
					errorCause = "Error response is not valid json data";
					throw new JSONException("invalid json data");
				}
				
				if (httpServerResponse.getData() != null) {
					console.append("\n\nCommand from Http-Server: " + httpServerResponse.getData());
					Log.i(TAG, "Command from Http-Server: " + httpServerResponse.getData());
				}
				
				// Make sure no other operations are completed until this one is completed
				MyPreferences.setCardOperationOngoing(ctx, true);
				
				errorCause = "Error while exchange Command APDU: " + Parsers.hexToArray(httpServerResponse.getData());
				sendApduToSE(Parsers.hexToArray(httpServerResponse.getData()));
				
				// Set seResponse to null to force reading it from the PN66T
				seResponse = null;
				
				while(isCancelled() == false) {
					if(seResponse != null) {
						// Make sure no other operations are completed until this one is completed
						MyPreferences.setCardOperationOngoing(ctx, true);
						
						String seResponseString = Parsers.arrayToHex(seResponse);
						params.clear();
						params.add(new BasicNameValuePair(HttpProtokollConstants.ParameterNameType, HttpProtokollConstants.TypeValueResponseApdu));
						params.add(new BasicNameValuePair(HttpProtokollConstants.ParameterNameTransactionId, transactionId));
						params.add(new BasicNameValuePair(HttpProtokollConstants.ParameterCardData, seResponseString));

						httpServerResponseString = null;
						errorCause = "Error while sending Request to the Web Server";
						httpServerResponseString = httpSender.executeHttpGet(params, serverUrl);
						Log.i(TAG, "HTTP Response String: " + httpServerResponseString);
		
						Log.i(TAG, "HTTP Response String: " + httpServerResponseString);
						errorCause = "Failed to parse webserver response " + httpServerResponseString;
						httpServerResponse = new HttpResponseJson(httpServerResponseString);
						if (httpServerResponse.getType().equalsIgnoreCase(HttpProtokollConstants.TypeValueEndTransaction)) {
							if (httpServerResponse.getData().contains("success")) {
								console.append("\n\nTransaction successfull!");
							} else {
								console.append("\n\nTransaction failed!");
							}
						}
						
						break;
					}
				}
			}

		} catch (Exception e) {
			console.append("\n" + errorCause + "\n");
			Log.e(TAG, errorCause);
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
		
		return console;
	}

	@Override
	protected void onPostExecute(final StringBuffer result) {
		super.onPostExecute(result);

		((MyCardsActivity) ctx).proccessTransactionTaskResult(result, lock);
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