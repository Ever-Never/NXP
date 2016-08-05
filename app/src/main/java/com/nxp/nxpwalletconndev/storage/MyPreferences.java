package com.nxp.nxpwalletconndev.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class MyPreferences {
	// Preferences name
	private final static String PREF_FILE_NAME = "NXPWalletConnDevPref";

	private final static String USER_NAME = "user_name";
	private final static String USER_SURNAME = "user_surname";
//	private final static String USER_ADDRESS = "user_address";
//	private final static String USER_EMAIL = "user_email";
//	private final static String USER_PASSWORD = "user_password";
//	private final static String ID = "id";
	private final static String LOGUED = "logued";
	
	private final static String CARD_OPERATING = "card_operating";
	

	
	private static SharedPreferences getPreferences(Context ctx) {
		SharedPreferences preferencias = ctx.getSharedPreferences(PREF_FILE_NAME, 0);
		return preferencias;
	}

	////////////////////////////////////////////////////////////////////////
	/////// GET
	////////////////////////////////////////////////////////////////////////
	
	public static String getUserName(final Context ctx) {
		SharedPreferences prefer = getPreferences(ctx);
		return prefer.getString(USER_NAME, "");
	}
	
	public static String getUserSurname(final Context ctx) {
		SharedPreferences prefer = getPreferences(ctx);
		return prefer.getString(USER_SURNAME, "");
	}
	
//	public static String getUserAddress(final Context ctx) {
//		SharedPreferences prefer = getPreferences(ctx);
//		return prefer.getString(USER_ADDRESS, "");
//	}
//	
//	public static String getUserEmail(final Context ctx) {
//		SharedPreferences prefer = getPreferences(ctx);
//		return prefer.getString(USER_EMAIL, "");
//	}
//	
//	public static String getUserPassword(final Context ctx) {
//		SharedPreferences prefer = getPreferences(ctx);
//		return prefer.getString(USER_PASSWORD, "");
//	}
//
//	public static int getUserId(final Context ctx) {
//		SharedPreferences prefer = getPreferences(ctx);
//		return prefer.getInt(ID, 0);
//	}
		
	public static boolean isLogued(final Context ctx) {
		SharedPreferences prefer = getPreferences(ctx);
		return prefer.getBoolean(LOGUED, false);
	}
	
	public static boolean isCardOperationOngoing(final Context ctx) {
		SharedPreferences prefer = getPreferences(ctx);
		return prefer.getBoolean(CARD_OPERATING, false);
	}
		
	////////////////////////////////////////////////////////////////////////
	/////// SET
	////////////////////////////////////////////////////////////////////////

	public static void setUserName(final Context ctx, final String name) {
		SharedPreferences.Editor editor = getPreferences(ctx).edit();
		editor.putString(USER_NAME, name);
		editor.commit();
	}
	
	public static void setUserSurname(final Context ctx, final String surname) {
		SharedPreferences.Editor editor = getPreferences(ctx).edit();
		editor.putString(USER_SURNAME, surname);
		editor.commit();
	}
	
//	public static void setUserAddress(final Context ctx, final String address) {
//		SharedPreferences.Editor editor = getPreferences(ctx).edit();
//		editor.putString(USER_ADDRESS, address);
//		editor.commit();
//	}
//	
//	public static void setUserEmail(final Context ctx, final String email) {
//		SharedPreferences.Editor editor = getPreferences(ctx).edit();
//		editor.putString(USER_EMAIL, email);
//		editor.commit();
//	}
//	
//	public static void setUserPassword(final Context ctx, final String password) {
//		SharedPreferences.Editor editor = getPreferences(ctx).edit();
//		editor.putString(USER_PASSWORD, password);
//		editor.commit();
//	}
//
//	public static void setUserId(final Context ctx, final int id) {
//		SharedPreferences.Editor editor = getPreferences(ctx).edit();
//		editor.putInt(ID, id);
//		editor.commit();
//	}
	
	public static void setLogued(final Context ctx, final boolean logued) {
		SharedPreferences.Editor editor = getPreferences(ctx).edit();
		editor.putBoolean(LOGUED, logued);
		editor.commit();
	}
	
	public static void setCardOperationOngoing(final Context ctx, final boolean operating) {
		SharedPreferences.Editor editor = getPreferences(ctx).edit();
		editor.putBoolean(CARD_OPERATING, operating);
		editor.commit();
	}
}
