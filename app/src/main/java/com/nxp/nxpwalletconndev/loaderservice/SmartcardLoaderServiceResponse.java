package com.nxp.nxpwalletconndev.loaderservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

public class SmartcardLoaderServiceResponse {
	public static void writeOutputFile(Context c, String outputFile, String text) {
		try {
			// KitKat does not refresh folder so I have to create a
			// temporary file and then remove it
			File myFile = new File(outputFile);
			
			if(myFile.exists())
				myFile.delete();
			
			myFile.createNewFile();

			FileOutputStream fOut = new FileOutputStream(myFile, true);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			myOutWriter.append(text);
			myOutWriter.close();
			fOut.close();

			c.sendBroadcast(new Intent(
					Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
					Uri.fromFile(myFile)));
			c.sendBroadcast(new Intent(
					Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
					Uri.parse("file://"
							+ Environment.getExternalStorageDirectory())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
