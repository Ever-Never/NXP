package com.nxp.nxpwalletconndev.utils;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Pattern;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.nxp.nxpwalletconndev.classes.Card;

public class Parsers {
	/**
	 * Convert a byte array to an hexadecimal string
	 * 
	 * @param data
	 *            the byte array to be converted
	 * @return the output string
	 */
	public static String arrayToHex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			String bs = Integer.toHexString(data[i] & 0xFF).toUpperCase();
			if (bs.length() == 1) {
				sb.append(0);
			}
			sb.append(bs);
			// sb.append(" ");
		}
		return sb.toString();
	}

	/**
	 * Convert an hexadecimal string into a byte array. Each byte in ethe
	 * hexadecimal string being separated by spaces or not (eg. s = "65 A0 12"
	 * <=> "65A012").
	 * 
	 * @param s
	 *            the string to be converted
	 * @return the corresponding byte array
	 * @throws IllegalArgumentException
	 *             if not a valid string representation of a byte array
	 */
	public static byte[] hexToArray(String s) {

		// check the entry
		Pattern p = Pattern.compile("([a-fA-F0-9]{2}[ ]*)*");
		boolean valid = p.matcher(s).matches();

		if (!valid) {
			throw new IllegalArgumentException(
					"not a valid string representation of a byte array :" + s);
		}

		String hex = s.replaceAll(" ", "");
		byte[] tab = new byte[hex.length() / 2];
		for (int i = 0; i < tab.length; i++) {
			tab[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2),
					16);
		}
		return tab;
	}

	/**
	 * Convert a byte array into ASCII string representation.
	 * 
	 * @param buf
	 *            The bytes to format.
	 * @return ASCII string representation of the specified bytes.
	 * @throws UnsupportedEncodingException
	 */
	public static String toAsciiString(byte[] buf)
			throws UnsupportedEncodingException {
		String ascii = null;
		if (buf != null) {
			ascii = new String(buf, "US-ASCII");
			// Check the characters
			char[] charArray = ascii.toCharArray();
			for (int i = 0; i < charArray.length; i++) {
				// Show null character as blank space
				if (charArray[i] == (char) 0x00) {
					charArray[i] = ' ';
				}
			}
			ascii = new String(charArray);
		}
		return ascii;
	}

	/**
	 * Convert the byte array to an int starting from the given offset.
	 * 
	 * @param b
	 *            The byte array
	 * @param offset
	 *            The array offset
	 * @return The integer
	 */
	public static int byteArrayToInt(byte[] b) {
		if (b.length == 1) {
			return b[0] & 0xFF;
		} else if (b.length == 2) {
			return ((b[0] & 0xFF) << 8) + (b[1] & 0xFF);
		} else if (b.length == 3) {
			return ((b[0] & 0xFF) << 16) + ((b[1] & 0xFF) << 8) + (b[2] & 0xFF);
		} else if (b.length == 4)
			return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8)
					+ (b[3] & 0xFF);
		else
			throw new IndexOutOfBoundsException();
	}

	public static String getTimestampString(byte[] timestamp) {
		if (timestamp == null || timestamp.length < 6) {
			return "?";
		}
		Log.i("TimeStamp Parser",
				"Timestampbytes: " + Parsers.arrayToHex(timestamp));
		String timestampSting = "";
		int day = timestamp[0] & 0xFF;
		int month = timestamp[1] & 0xFF;
		byte[] yearBytes = new byte[2];
		System.arraycopy(timestamp, 2, yearBytes, 0, 2);
		int year = Parsers.byteArrayToInt(yearBytes);
		int hours = timestamp[4] & 0xFF;
		int mins = timestamp[5] & 0xFF;

		// Check for correct Values
		if (day < 1 || day > 31 || month < 1 || month > 12 || year < 1990
				|| hours < 0 || hours > 24 || mins < 0 || mins > 59) {
			return "?";
		} else {
			timestampSting = String.valueOf(day) + "." + String.valueOf(month)
					+ "." + String.valueOf(year) + " " + String.valueOf(hours)
					+ ":" + String.valueOf(mins);
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.y H:m");
		try {
			Date timestampDate = dateFormat.parse(timestampSting);
			dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
			return dateFormat.format(timestampDate);
		} catch (ParseException e) {
			return "?";
		}
	}

	public static int CompareArrays(short[] array1, int array1pos,
			short[] array2, int array2pos, int len) {
		int i, j;
		for (i = array1pos, j = array2pos; j < len; i++, j++) {
			if (array1[i] != array2[j]) {
				return StatusBytes.FAILED;
			}
		}
		if (((i - array1pos) == len) && ((j - array2pos) == len)) {
			return StatusBytes.SUCCESS;
		} else {
			return StatusBytes.FAILED;
		}
	}

	public static int ShorttoByteArraycopy(short[] src, int srcpos,
			byte[] dest, int destpos, int len) {
		String TAG = "LTSMlib:ShorttoIntArraycopy";

		if (dest.length < len) {
			Log.i(TAG, "dest array length less than length to be copied ");
			return StatusBytes.FAILED;
		}
		int i, j;
		for (i = srcpos, j = destpos; j < len; i++, j++) {
			dest[j] = (byte) src[i];
		}
		if (j == len) {
			return StatusBytes.SUCCESS;
		} else {
			return StatusBytes.FAILED;
		}
	}

	public static short byteArrayToShort(byte[] data) {
		return (short) ((data[0] << 8) | (data[1]));
	}

	public static Card getVcEntry(byte[] recvData, String city, String number, int iconRsc, int mifaretype, int cardtype, int order) {
		String TAG = "getVcEntry";

		if (recvData[0] == (byte) 0x4E) {
			Log.i(TAG, " VC Creation Failed ");
			return null;
		} else if (recvData[0] == (byte) 0x40) {
			int vcEntryInt = byteArrayToShort(extract(recvData, 2, 2));
			return new Card(0, 0, vcEntryInt, city, number, "", "", "", Card.STATUS_PERSONALIZING, false, false, iconRsc, mifaretype, cardtype, order);
		} else {
			return null;
		}
	}
	
	public static byte[] getMifareData(byte[] recvData) {
		String TAG = "getMifareData";
		
		if (recvData[0] == (byte) 0x4E) {
			Log.i(TAG, " Read MIFARE Data failed ");
			return null;
		} else if (recvData[0] == (byte) 0x61) {
			byte[] MifareData = new byte[recvData[1]];
			System.arraycopy(recvData, 2, MifareData, 0, recvData[1]);
			return MifareData;
		} else {
			return null;
		}
	}
	
	public static ArrayList<Integer> getVCList(byte[] recvData, Context ctx) {
		String TAG = "getVCList";
		
		ArrayList<Integer> vcListArray = new ArrayList<Integer>();
		
		String PACKAGE_NAME = ctx.getPackageName();	
		String myAppHash = getAppSha1(PACKAGE_NAME).toUpperCase();
		
		if (recvData[0] == (byte) 0x4E) {
			Log.i(TAG, " Read VC List failed ");
			return null;
		} else if (recvData[0] == (byte) 0x61) {
			String vcList = bytArrayToHex(recvData);
			String[] vcListEntry = vcList.split("611E");

			for(int i = 1; i < vcListEntry.length; i++) {
				int vcEntryId = Integer.parseInt(vcListEntry[i].substring(4, 8));
				String vcEntryHash = vcListEntry[i].substring(12, 52);
				
				if(vcEntryHash.equals(myAppHash) == true)
					vcListArray.add(vcEntryId);
			}
			
			return vcListArray;
		} else {
			return null;
		}
	}
	
	private static String getAppSha1(String password)
	{
	    String sha1 = "";
	    
	    try {
	        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
	        crypt.reset();
	        crypt.update(password.getBytes("UTF-8"));
	        sha1 = byteToHex(crypt.digest());
	    } catch(NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    } catch(UnsupportedEncodingException e) {
	        e.printStackTrace();
	    }
	    
	    return sha1;
	}
	
	private static String byteToHex(final byte[] hash)
	{
	    Formatter formatter = new Formatter();
	    for (byte b : hash)
	    {
	        formatter.format("%02x", b);
	    }
	    String result = formatter.toString();
	    formatter.close();
	    return result;
	}

	public static int BytetoShortArraycopy(byte[] src, int srcpos,
			short[] dest, int destpos, int len) {
		String TAG = "LTSMlib:ShorttoIntArraycopy";

		if (dest.length < len) {
			Log.i(TAG, "dest array length less than length to be copied ");
			return StatusBytes.FAILED;
		}
		int i, j;
		for (i = srcpos, j = destpos; j < len; i++, j++) {
			dest[j] = src[i];
		}
		if (j == len) {
			return StatusBytes.SUCCESS;
		} else {
			return StatusBytes.FAILED;
		}
	}

	public static byte[] makeCAPDU(int cla, int ins, int p1, int p2,
			byte[] cdata) {
		if (cdata == null) {
			return new byte[] { (byte) cla, (byte) ins, (byte) p1, (byte) p2, 0 };
		} else {
			return append(new byte[] { (byte) cla, (byte) ins, (byte) p1,
					(byte) p2, (byte) cdata.length }, cdata);
		}
	}

	public static byte[] append(byte[] a, byte[] b) {
		byte[] result = new byte[a.length + b.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	public static byte[] extract(byte[] buffer, int offset, int length) {
		byte[] result = new byte[length];
		System.arraycopy(buffer, offset, result, 0, length);
		return result;
	}

	public static short getSW(byte[] rapdu) {
		byte sw1 = rapdu[rapdu.length - 2];
		byte sw2 = rapdu[rapdu.length - 1];
		return (short) ((sw1 << 8) + (sw2 & 0xFF));
	}

	public static byte[] getRDATA(byte[] rapdu) {
		return extract(rapdu, 0, rapdu.length - 2);
	}

	public static byte adjustCLA(byte cla, byte lcnum) {
		return (byte) ((cla & ~0x03) | (lcnum & 0x03));
	}

	public static String bytArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for (byte b : a)
			sb.append(String.format("%02X", b & 0xff));
		return sb.toString();
	}
	
	public static String bytArrayToAsciiHex(byte[] a) {
		byte[] tempAsc = new byte[a.length];
		
		// Only printable characters are displayed
		for(int i = 0; i < a.length; i++) {
			if (a[i] < 0x20 || a[i] > 0x7D)
				tempAsc[i] = '.';
			else
				tempAsc[i] = a[i];
		}
		
		return new String(tempAsc);
	}

	public static String CreateSHA(String pkg) {
		String TAG = "Utils:CreateSHA";
		String PackageName = "com.nxp.ltsm.Wallet";

		StringBuffer sb = new StringBuffer();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(PackageName.getBytes());

			byte byteData[] = md.digest();

			for (int i = 0; i < byteData.length; i++) {
				sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16)
						.substring(1));
			}
			// Log.i(TAG, "sb.toString()" + sb.toString());
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	public static String getCallingAppPkg(Context context) {
		String TAG = "getCallingAppPkg";
		ActivityManager am = (ActivityManager) context
				.getSystemService(context.ACTIVITY_SERVICE);

		// get the info from the currently running task
		List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);

		Log.d("topActivity", "CURRENT Activity ::"
				+ taskInfo.get(0).topActivity.getClassName());
		String s = taskInfo.get(0).topActivity.getClassName();

		ComponentName componentInfo = taskInfo.get(0).topActivity;
		componentInfo.getPackageName();
		Log.i(TAG,
				"componentInfo.getPackageName()"
						+ componentInfo.getPackageName());
		return componentInfo.getPackageName();

	}

	public static byte[] ShortToByteArr(short vC_Entry) {
		String TAG = "ShortToByteArr";
		Log.i(TAG, "vC_Entry)" + vC_Entry);
		byte[] VC_EntryBytes = new byte[2];
		VC_EntryBytes[1] = (byte) (vC_Entry & 0xff);
		VC_EntryBytes[0] = (byte) ((vC_Entry >> 8) & 0xff);
		return VC_EntryBytes;

	}

	public static byte[] GetValue(byte Tag, byte Len, byte[] rapdu) {
		String TAG = "Utils:GetValue";
		byte[] retData = new byte[Len];
		for (int i = 0; i < rapdu.length - 1; i++) {
			if ((Tag == rapdu[i]) && (Len == rapdu[i + 1])) {

				System.arraycopy(rapdu, i + 2, retData, 0, Len);
				Log.i(TAG, "retData" + bytArrayToHex(retData));

			}
		}
		return retData;
	}

	/*
	 * Originally defined in VCDescriptionFile
	 */
	public static byte[] parseHexProperty(String name, String key) {
		String TAG = "VCDescriptionFile:parseHexProperty";
		String value = key;

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			while (value.length() > 0) {
				out.write(Integer.parseInt(value.substring(0, 2), 16));
				value = value.substring(2);
			}

			return out.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Property %s is malformed" + name);
			System.exit(1);
			return null;
		}
	}

	public static byte[] parseHexString(String s) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			while (!s.isEmpty()) {
				out.write(Integer.parseInt(s.substring(0, 2), 16));
				s = s.substring(2);
			}

			return out.toByteArray();
		} catch (Exception e) {
			return null;
		}
	}
}
