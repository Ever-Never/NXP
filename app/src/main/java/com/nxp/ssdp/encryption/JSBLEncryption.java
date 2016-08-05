package com.nxp.ssdp.encryption;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import junit.framework.Assert;
import android.content.Context;
import android.content.res.AssetManager;

import com.nxp.id.crypto.encryption.CryptoException;
import com.nxp.id.crypto.encryption.UpdaterOsEncryptor;
import com.nxp.nxpwalletconndev.utils.Parsers;


//import com.nxp.id.crypto.encryption.UpdaterOsHasherSigner;

public class JSBLEncryption {
	private UpdaterOsEncryptor encryptor;
	// private UpdaterOsHasherSigner hasher;

	// Private key
	// private byte[] S;

	// Public key
	private byte[] W;

	public JSBLEncryption(String filename, Context ctx) {
		boolean error = false;
		Properties props = new Properties();
		
		AssetManager assManager = ctx.getAssets();

		try {
			props.load(assManager.open(filename));
		} catch (FileNotFoundException e) {
			System.err.println("M4MLTSMClient: File not found: " + filename);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("M4MLTSMClient: I/O error: " + filename);
			System.exit(1);
		}

		// JSBL Private Key – S parameter (32-byte)
		// S=9b9086ef15b6586f866ff88fd2d0c7393728b01ab5e09e8128754d0e2dbf5d17
		// byte[] S = Util.parseHexString(props.getProperty("S", ""));

		// JSBL Public Key – W parameter (32-byte x and y)
		// W.x=60ad3fd1dad108ec4c758e246434667106c2722063fd58725d6ed3c3e220f678
		// W.y=143f99f0b14d5dcfdfdd427e683d4c350fb69f9d1ef61b286128461e91b6a108
		byte[] Wx = Parsers.parseHexString(props.getProperty("W.x", ""));
		byte[] Wy = Parsers.parseHexString(props.getProperty("W.y", ""));

		// if ((S == null) || (S.length != 32))
		// {
		// System.err.println("M4MLTSMClient: Bad S in " + filename);
		// error = true;
		// }

		if ((Wx == null) || (Wx.length != 32)) {
			System.err.println("M4MLTSMClient: Bad W.x in " + filename);
			error = true;
		}

		if ((Wy == null) || (Wy.length != 32)) {
			System.err.println("M4MLTSMClient: Bad W.y in " + filename);
			error = true;
		}

		if (error) {
			System.exit(1);
		}

		// this.S = S;
		this.W = makePoint(Wx, Wy);
	}

	public byte[][] encrypt(byte[] data) {
		try {
			// Initialize JSBL encryptor
			encryptor = new UpdaterOsEncryptor(this.W);
			encryptor.initialize(1);

			data = encryptor.encryptData(data);

			// Generate Rx and Ry
			byte[] encryptedK1 = encryptor.encryptHeader();

			// Append EncryptedK1 and encryptedData
			byte[] encrypted = new byte[encryptedK1.length + data.length];
			System.arraycopy(encryptedK1, 0, encrypted, 0, encryptedK1.length);
			System.arraycopy(data, 0, encrypted, encryptedK1.length,
					data.length);

			return new byte[][] {
					makePoint(encryptor.getRx(), encryptor.getRy()), encrypted };
		} catch (CryptoException e) {
			return new byte[][] { null, null };
		}
	}

	private static byte[] makePoint(byte[] x, byte[] y) {
		if ((x == null) || (x.length != 32) || (y == null) || (y.length != 32)) {
			System.err
					.println("Warning: Invalid coordinates passed to makePoint()");
			System.exit(1);
			return new byte[0];
		}

		byte[] p = new byte[1 + 32 + 32];

		p[0] = 0x04;
		System.arraycopy(x, 0, p, 1, 32);
		System.arraycopy(y, 0, p, 33, 32);

		return p;
	}
	
	public byte[] getEncryptedTLV(byte[] keyset, byte id) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			byte[][] result = encrypt(keyset);
			out.reset();

			out.write(createTLV(0x60, result[0]));
			out.write(createTLV(id, result[1]));

			return out.toByteArray();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}
	
	private static byte[] createTLV(int tag, byte[] value) {
		if (value == null) {
			return new byte[0];
		} else {
			byte[] t, l;

			if ((tag & 0xFF00) == 0x0000) {
				t = new byte[] { (byte) tag };
			} else {
				t = new byte[] { (byte) (tag >> 8), (byte) tag };
			}

			if (value.length < 128) {
				l = new byte[] { (byte) value.length };
			} else if (value.length < 256) {
				l = new byte[] { (byte) (0x81), (byte) value.length };
			} else {
				l = new byte[] { (byte) (0x82), (byte) (value.length >> 8),
						(byte) value.length };
			}

			return append(t, l, value);
		}
	}
	
	private static byte[] append(byte[]... arrays) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(256);

		for (byte[] array : arrays) {
			if (array != null) {
				try {
					out.write(array);
				} catch (Exception e) {
				}
			}
		}

		return out.toByteArray();
	}

	/*
	 * private static byte[] pad(byte[] data) { byte[] result = new
	 * byte[(data.length + 15) & ~15]; System.arraycopy(data, 0, result, 0,
	 * data.length); return result; }
	 */
}
