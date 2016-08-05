package com.nxp.ssdp.encryption;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

//import com.nxp.id.crypto.encryption.UpdaterOsHasherSigner;

public class MIFAREPassword {
	private static final byte[] CIPHER_INPUT = { 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00 };

	public static byte[] CalculateMFPassword(byte[] a, byte[] b) {
		byte[] p = new byte[24];
		byte[] cipherText = null;

		byte[] am = mapKeyA(a);
		byte[] bm = mapKeyB(b);

		System.arraycopy(invert(am), 0, p, 0, 8);
		System.arraycopy(invert(bm), 0, p, 8, 8);
		System.arraycopy(invert(am), 0, p, 16, 8);

		try {
			SecretKeyFactory kf = SecretKeyFactory.getInstance("DESede");
			Cipher encipher = Cipher.getInstance("DESede/ECB/NoPadding");
			DESedeKeySpec deskey = new DESedeKeySpec(p);
			SecretKey secretKey = kf.generateSecret(deskey);

			encipher.init(Cipher.ENCRYPT_MODE, secretKey);
			cipherText = encipher.doFinal(CIPHER_INPUT, 0, 8);
			// ciphered = invert(ciphered);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}

		return invert(cipherText);
	}

	private static byte[] mapKeyB(byte[] x) {
		byte[] xm = new byte[8];
		
		xm[7] = (byte) (x[5] << 1);
		xm[6] = (byte) (x[4] << 1);
		xm[5] = (byte) (x[3] << 1);
		xm[4] = (byte) (x[2] << 1);
		xm[3] = (byte) (x[1] << 1);
		xm[2] = (byte) (x[0] << 1);
		xm[1] = (byte) (((x[5] & 0x80) >> 1) | ((x[4] & 0x80) >> 2) | ((x[3] & 0x80) >> 3) | ((x[2] & 0x80) >> 4) | ((x[1] & 0x80) >> 5) | ((x[0] & 0x80) >> 6));  
		xm[0] = 0x00;
		
		return xm;
	}
	
	private static byte[] mapKeyA(byte[] x) {
		byte[] xm = new byte[8];
		
		xm[7] = 0x00;
		xm[6] = (byte) (((x[0] & 0x80) >> 1) | ((x[1] & 0x80) >> 2) | ((x[2] & 0x80) >> 3) | ((x[3] & 0x80) >> 4) | ((x[4] & 0x80) >> 5) | ((x[5] & 0x80) >> 6));  
		xm[5] = (byte) (x[5] << 1);
		xm[4] = (byte) (x[4] << 1);
		xm[3] = (byte) (x[3] << 1);
		xm[2] = (byte) (x[2] << 1);
		xm[1] = (byte) (x[1] << 1);
		xm[0] = (byte) (x[0] << 1);
		
		return xm;
	}
	
	private static byte[] invert(byte[] x) {
		byte[] xi = new byte[8];
		
		for(int j = 0, i = x.length - 1; j < x.length; i--, j++)
			xi[i] = x[j];
		
		return xi;
	}
}