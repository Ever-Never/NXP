package com.nxp.nxpwalletconndev.tasks;

import java.io.IOException;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.utils.Parsers;

public class ReadCreditCardTask {
	public final static byte[] SELECT_PPSE = {
		(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x0E, (byte) 0x32, (byte) 0x50, (byte) 0x41, 
		(byte) 0x59, (byte) 0x2E, (byte) 0x53, (byte) 0x59, (byte) 0x53, (byte) 0x2E, (byte) 0x44, (byte) 0x44, 
		(byte) 0x46, (byte) 0x30, (byte) 0x31, (byte) 0x00};
	
	public final static byte[] SELECT_HEADER = {
		(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00 };
	
	public final static byte[] READ_RECORDS = {
		(byte) 0x00, (byte) 0xB2, (byte) 0x01, (byte) 0x0C, (byte) 0x00 };
	
	public final static String FCI_ISSUER_DISCRETIONARY_DATA = "BF0C";
	
	public final static String APPLICATION_DEDICATED_FILE = "4F";
	public final static String APPLICATION_LABEL = "50";
	public final static String TRACK_EQUIVALENT_DATA = "57";
	public final static String TRACK_EQUIVALENT_DATA_2 = "9F6B";
	
	public Card readCreditCard(Intent intent) {
		Card creditCard = null;
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);		
		
		// Validate card technology
		boolean isValid = false;
		
		String[] techs = tag.getTechList();
		for(int i = 0; i < techs.length; i++)
			if(techs[i].equals("android.nfc.tech.IsoDep") == true) {
				isValid = true;
				break;
			}
		
		if(isValid == false)
			return null;
		
		IsoDep card = IsoDep.get(tag);
		
		try {
			card.connect();
			
			byte[] ppseResponse = card.transceive(SELECT_PPSE);
			String ppseResponseStr = Parsers.arrayToHex(ppseResponse);
			
			Log.d("Read Credit Card test", "ppseResponse: " + ppseResponseStr);
			
			if(ppseResponse[ppseResponse.length - 2] != (byte) 0x90 || ppseResponse[ppseResponse.length - 1] != (byte) 0x00)
				return null;
			
			int issuerDiscDataLength = ppseResponse[(ppseResponseStr.indexOf(FCI_ISSUER_DISCRETIONARY_DATA) / 2) + 2] * 2;
			String issuerDiscData = ppseResponseStr.substring(
					ppseResponseStr.indexOf(FCI_ISSUER_DISCRETIONARY_DATA) + 6, ppseResponseStr.indexOf(FCI_ISSUER_DISCRETIONARY_DATA) + 6 + issuerDiscDataLength);
			byte[] issuerDiscDataByte = Parsers.hexToArray(issuerDiscData); 
			
			int aidLength = issuerDiscDataByte[((issuerDiscData.indexOf(APPLICATION_DEDICATED_FILE) / 2) + 1)] * 2;
			String aid = issuerDiscData.substring(
					issuerDiscData.indexOf(APPLICATION_DEDICATED_FILE) + 4, issuerDiscData.indexOf(APPLICATION_DEDICATED_FILE) + 4 + aidLength);
			
			int nameLength = issuerDiscDataByte[(issuerDiscData.indexOf(APPLICATION_LABEL) / 2) + 1] * 2;
			String name = new String(Parsers.hexToArray(issuerDiscData.substring(
				issuerDiscData.indexOf(APPLICATION_LABEL) + 4, issuerDiscData.indexOf(APPLICATION_LABEL) + 4 + nameLength)));

			byte[] selectAid = createSelect(Parsers.hexToArray(aid));
			
			byte[] selectAidResponse = card.transceive(selectAid);
			String selectAidResponseStr = Parsers.arrayToHex(selectAidResponse);
			
			Log.d("Read Credit Card test", "selectAidResponse: " + selectAidResponseStr);
			
			if(selectAidResponse[selectAidResponse.length - 2] != (byte) 0x90 || selectAidResponse[selectAidResponse.length - 1] != (byte) 0x00)
				return null;
			
			
//			// I Ommit the GPO command since otherwise I need to add fields in the response for every tag in the PDOL ... 
//			
//			// I read Record One where relevant info is likely to be stored
//			byte[] readRecordsResponse = card.transceive(READ_RECORDS);
//			String readRecordsResponseStr = Parsers.arrayToHex(readRecordsResponse);
//			
//			Log.d("Read Credit Card test", "readRecordsResponseStr: " + readRecordsResponseStr);
//			
//			if(readRecordsResponse[readRecordsResponse.length - 2] != (byte) 0x90 || readRecordsResponse[readRecordsResponse.length - 1] != (byte) 0x00)
//				return null;
//					
//			String trackData = null;
//			
//			int pos = 0;
//			
//			pos = readRecordsResponseStr.indexOf(TRACK_EQUIVALENT_DATA_2);
//			if((pos > 0) && (pos % 2 == 00)) {
//				int trackDataLength = readRecordsResponse[(pos / 2) + 2] * 2;
//				trackData = readRecordsResponseStr.substring(pos + 6, pos + 6 + trackDataLength);
//			} else {
//				pos = readRecordsResponseStr.indexOf(TRACK_EQUIVALENT_DATA);
//				if((pos > 0) && (pos % 2 == 00)) {
//					int trackDataLength = readRecordsResponse[(pos / 2) + 1] * 2;
//					trackData = readRecordsResponseStr.substring(pos + 4, pos + 4 + trackDataLength);
//				}
//			}
//			
//			if(trackData == null)
//				return null;
//			
//			String PAN = trackData.substring(0, 16);
//			String year = trackData.substring(17, 19);
//			String month = trackData.substring(19, 21);
			
			creditCard = new Card(0, 0, 0, "", "", "", "", "", 0, false, false, R.drawable.card_blank, 0, Card.TYPE_PAYMENTS, 0);

			card.close();
		} catch (IOException e) {
			e.printStackTrace();
			
			return null;
		}
				
		return creditCard;
	}
	
	private byte[] createSelect(byte[] aid) {
		byte[] selectCommand = new byte[aid.length + 5];
		selectCommand[0] = (byte) 0x00;
		selectCommand[1] = (byte) 0xA4;
		selectCommand[2] = (byte) 0x04;
		selectCommand[3] = (byte) 0x00;
						
		// Lc indicates the length of the aid
		selectCommand[4] = (byte) aid.length;
		
		// Add the AID inserted by the user to the command
		System.arraycopy(aid, 0, selectCommand, 5, aid.length);
		
		return selectCommand;
	}
}
