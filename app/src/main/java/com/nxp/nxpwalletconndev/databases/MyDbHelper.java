package com.nxp.nxpwalletconndev.databases;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.classes.Device;

public class MyDbHelper extends SQLiteOpenHelper {
	// Database Version
	private static final int DATABASE_VERSION = 11;

	// Database Name
	private static final String DATABASE_NAME = "mwcwalletconndev";
	
	private static final String TABLE_CARDS = "cardsconndev";
	private static final String TABLE_DEVICES = "devicesconndev";

	private static final String KEY_ID = "id";
	private static final String KEY_ID_SCRIPT = "id_script";
	private static final String KEY_ID_VC = "id_vc";
	private static final String KEY_ID_DEV = "id_dev";
	private static final String KEY_ICON = "icon";
	private static final String KEY_NAME = "name";
	private static final String KEY_NUMBER = "number";
	private static final String KEY_CVC = "cvc";
	private static final String KEY_EXP_YEAR = "year";
	private static final String KEY_EXP_MONTH = "month";
	private static final String KEY_FAV = "fav";
	private static final String KEY_STATUS = "status";
	private static final String KEY_LOCK = "lock";
	private static final String KEY_MIFARE_TYPE = "mifare_type";
	private static final String KEY_TYPE = "type";
	private static final String KEY_MAC = "mac";
	private static final String KEY_ORDER = "listOrder";

	
	public MyDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_TABLE_CARDS = "CREATE TABLE " + TABLE_CARDS + " ( "
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "id_script INTEGER, " + "id_vc INTEGER, " + "id_dev INTEGER, " + "name TEXT, " + "number TEXT, " + "cvc TEXT, " + "month TEXT, " + "year TEXT, " 
				+ "status INTEGER, "+ "fav INTEGER, " + "lock INTEGER, " + "icon INTEGER, " + "mifare_type INTEGER, " + "type INTEGER, " + "listOrder INTEGER)";
		
		String CREATE_TABLE_DEVICES = "CREATE TABLE " + TABLE_DEVICES + " ( "
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "name TEXT, " + "mac TEXT)";
		
		db.execSQL(CREATE_TABLE_CARDS);
		db.execSQL(CREATE_TABLE_DEVICES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARDS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEVICES);
		
		this.onCreate(db);
	}
	
	public List<Device> getAllDevices() {
		SQLiteDatabase db = this.getReadableDatabase();
		List<Device> devicesList = new LinkedList<Device>();

		String query = "SELECT * FROM " + TABLE_DEVICES;		
		Cursor cursor = db.rawQuery(query, null);

		Device device = null;
		if (cursor.moveToFirst()) {
			do {
				// By default I create the device without services and then I add this value based on the request to Cards database
				device = new Device(cursor.getInt(0), cursor.getString(1), cursor.getString(2), 0);
				
				String queryServices = "SELECT * FROM " + TABLE_CARDS + " WHERE " + KEY_ID_DEV + " = " + device.getId();	
				Cursor cursorServices = db.rawQuery(queryServices, null);
				
				// Update the count
				device.setDeviceServices(cursorServices.getCount());
				
				devicesList.add(device);
			} while (cursor.moveToNext());
		}
		
		return devicesList;
	}
		
	public int addDevice(String name, String mac){
		SQLiteDatabase db = this.getWritableDatabase();
		int id = 0;

		String query = "SELECT id FROM " + TABLE_DEVICES + " WHERE " + KEY_NAME + " = '" + name + "' AND " + KEY_MAC + " = '" + mac + "'";
		Cursor cursor = db.rawQuery(query, null);

		if (cursor.moveToFirst()) {
			do {
				id = cursor.getInt(0);
			} while (cursor.moveToNext());
		}
		
		if(id <= 0) {
			ContentValues values = new ContentValues();
			values.put(KEY_NAME, name);
			values.put(KEY_MAC, mac);
			
			id = (int) db.insert(TABLE_DEVICES,
			        null,	
			        values);
		}
		
		db.close(); 
		return id;
	}

	public List<Card> getAllCards(int idDevice) {
		List<Card> cardsList = new LinkedList<Card>();

		String query = "SELECT * FROM " + TABLE_CARDS + " WHERE " + KEY_ID_DEV + " = " + idDevice + " ORDER BY " + KEY_ORDER;;

		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(query, null);

		Card card = null;
		if (cursor.moveToFirst()) {
			do {
				card = new Card(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getString(4), cursor.getString(5), cursor.getString(6),
						cursor.getString(7), cursor.getString(8), cursor.getInt(9), cursor.getInt(10) > 0, 
						cursor.getInt(11) > 0, cursor.getInt(12), cursor.getInt(13), cursor.getInt(14), cursor.getInt(15));
				
				cardsList.add(card);
			} while (cursor.moveToNext());
		}

		return cardsList;
	}
	
	public void clearCards(int idDevice) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Remove all the cards associated to this device
       	db.delete(TABLE_CARDS, KEY_ID_DEV + " = ?", new String[] { String.valueOf(idDevice) });
 
        db.close();
    }
	
	public void clearCardsOperating(int idDevice) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Remove all the cards associated to this device
       	db.delete(TABLE_CARDS, KEY_ID_DEV + " = ? AND " + KEY_STATUS + " = ?", new String[] { String.valueOf(idDevice), String.valueOf(Card.STATUS_CREATING) });
       	db.delete(TABLE_CARDS, KEY_ID_DEV + " = ? AND " + KEY_STATUS + " = ?", new String[] { String.valueOf(idDevice), String.valueOf(Card.STATUS_PERSONALIZING) });
       	db.delete(TABLE_CARDS, KEY_ID_DEV + " = ? AND " + KEY_STATUS + " = ?", new String[] { String.valueOf(idDevice), String.valueOf(Card.STATUS_DELETING) });    	
 
        db.close();
    }
	
	public List<Integer> getAllPaymentCardsIds(int idDevice) {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " + KEY_ID_SCRIPT + " FROM " + TABLE_CARDS + " WHERE " + KEY_ID_DEV + " = " + idDevice + " AND " + KEY_TYPE + " = " + Card.TYPE_PAYMENTS;
		Cursor cursor = db.rawQuery(query, null);
		
		if(cursor.getCount() == 0)
			return null;
		else {
			if (cursor.moveToFirst()) {
				do {
					ids.add(Integer.parseInt(cursor.getString(0)));					
				} while (cursor.moveToNext());
			}
		}
		
		cursor.close();
        db.close();

		return ids;
	}
	
	public void addCard(Card card, int idDevice){
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_ID_SCRIPT, card.getIdScript());
		values.put(KEY_ID_VC, card.getIdVc());
		values.put(KEY_ID_DEV, idDevice);
		values.put(KEY_NAME, card.getCardName());
		values.put(KEY_NUMBER, card.getCardNumber());
		values.put(KEY_CVC, card.getCardCVC());
		values.put(KEY_EXP_MONTH, card.getCardExpMonth());
		values.put(KEY_EXP_YEAR, card.getCardExpYear());
		values.put(KEY_STATUS, card.getStatus());
		values.put(KEY_FAV, card.isFav());
		values.put(KEY_LOCK, card.isLocked());
		values.put(KEY_ICON, card.getIconRsc());
		values.put(KEY_MIFARE_TYPE, card.getMifareType());
		values.put(KEY_TYPE, card.getType());
		values.put(KEY_ORDER, card.getOrder());
				
		db.insert(TABLE_CARDS,
		        null,	
		        values);

		db.close(); 
	}
	
	public void personalizeCreditCard(int id, String name, String number, String cvc, String exp_month, String exp_year, boolean fav, boolean lock, int iconRsc, int idDevice) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		args.put(KEY_NUMBER, number);
		args.put(KEY_CVC, cvc);
		args.put(KEY_EXP_MONTH, exp_month);
		args.put(KEY_EXP_YEAR, exp_year);		
		args.put(KEY_STATUS, Card.STATUS_PERSONALIZED);
		args.put(KEY_FAV, fav);
		args.put(KEY_LOCK, lock);
		args.put(KEY_ICON, iconRsc);
		args.put(KEY_TYPE, Card.TYPE_PAYMENTS);
		
		db.update(TABLE_CARDS, args, KEY_ID_SCRIPT + " = ? AND " + KEY_ID_DEV + " = ?",
                new String[] { String.valueOf(id), String.valueOf(idDevice) });
		
        db.close();
    }
	
	public void deleteCard(int id, int idDevice, int type) {
        SQLiteDatabase db = this.getWritableDatabase();

        if(type == Card.TYPE_PAYMENTS)
        	db.delete(TABLE_CARDS, KEY_ID_SCRIPT+" = ? AND " + KEY_ID_DEV + " = ?",
                new String[] { String.valueOf(id), String.valueOf(idDevice) });
        else if(type == Card.TYPE_MIFARE_CLASSIC || type == Card.TYPE_MIFARE_DESFIRE)
        	db.delete(TABLE_CARDS, KEY_ID_VC+" = ? AND " + KEY_ID_DEV + " = ?",
                    new String[] { String.valueOf(id), String.valueOf(idDevice) });
 
        db.close();
    }
	
	public void deleteDevice(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

    	db.delete(TABLE_DEVICES, KEY_ID+" = ?",
            new String[] { String.valueOf(id) });
        
        db.close();
    }
	
	public void lockCard(int id, boolean lock, int idDevice) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues args = new ContentValues();
		args.put(KEY_LOCK, lock);
		
		db.update(TABLE_CARDS, args, KEY_ID_SCRIPT+" = ? AND " + KEY_ID_DEV + " = ?",
                new String[] { String.valueOf(id), String.valueOf(idDevice) });
		
        db.close();
    }
	
	public int getCardToCreateId(int idDevice) {
		int nextId = 1;
		SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT id_script FROM " + TABLE_CARDS + " WHERE " + KEY_ID_DEV + " = " + idDevice;
		Cursor cursor = db.rawQuery(query, null);
		
		if(cursor.getCount() == 0)
			return nextId;
		else {
			if (cursor.moveToFirst()) {
				ArrayList<Integer> ids = new ArrayList<Integer>();
				
				do {
					ids.add(Integer.parseInt(cursor.getString(0)));					
				} while (cursor.moveToNext());
				
				for(int i = 1; i <= ids.size() + 1; i++) {
					if(ids.contains(i) == false) {
						nextId = i;
						break;
					}
				}
			}
		}
		
		cursor.close();
        db.close();
        
        return nextId;
    }
	
	public Card getCard(int id, int idDevice, int type) {
		Card card = null;
		SQLiteDatabase db = this.getReadableDatabase();

		String query = "";

		if(type == Card.TYPE_PAYMENTS) {
			query = "SELECT * FROM " + TABLE_CARDS + " WHERE " + KEY_ID_SCRIPT + " = " + id + " AND " + KEY_ID_DEV + " = " + idDevice;
		} else if(type == Card.TYPE_MIFARE_CLASSIC || type == Card.TYPE_MIFARE_DESFIRE) {
			query = "SELECT * FROM " + TABLE_CARDS + " WHERE " + KEY_ID_VC + " = " + id + " AND " + KEY_ID_DEV + " = " + idDevice;
		}
		
		Cursor cursor = db.rawQuery(query, null);
			
		if(cursor.getCount() == 0)
			return null;
		
		cursor.moveToFirst();
		card = new Card(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getString(4), cursor.getString(5), cursor.getString(6),
				cursor.getString(7), cursor.getString(8), cursor.getInt(9), cursor.getInt(10) > 0, 
				cursor.getInt(11) > 0, cursor.getInt(12), cursor.getInt(13), cursor.getInt(14), cursor.getInt(15));

		cursor.close();
        db.close();
        
        return card;
    }
	
	public void removeFav(int idDevice, int type) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues args = new ContentValues();
		args.put(KEY_FAV, false);
		
		if(type == Card.TYPE_PAYMENTS || type == Card.TYPE_MIFARE_CLASSIC) {
			db.update(TABLE_CARDS, args, KEY_ID_DEV + " = ? AND " + KEY_TYPE + " = ?",
				new String[] { String.valueOf(idDevice), String.valueOf(type) });
		}
		
		db.close();
    }
	
	public void removeCardFav(int id, int idDevice, int type) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues args = new ContentValues();
		args.put(KEY_FAV, false);
		
		db.update(TABLE_CARDS, args, KEY_ID_VC + " = ? AND " + KEY_ID_DEV + " = ? AND " + KEY_TYPE + " = ?",
				new String[] { String.valueOf(id), String.valueOf(idDevice), String.valueOf(type) });
		
		db.close();
    }
	
	public void makeCardFav(int id, int idDevice, int type) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues args = new ContentValues();
		args.put(KEY_FAV, true);
		
		if(type == Card.TYPE_PAYMENTS) {
			db.update(TABLE_CARDS, args, KEY_ID_SCRIPT + " = ? AND " + KEY_ID_DEV + " = ? AND " + KEY_TYPE + " = ?",
                new String[] { String.valueOf(id), String.valueOf(idDevice), String.valueOf(type) });
		} else if(type == Card.TYPE_MIFARE_CLASSIC || type == Card.TYPE_MIFARE_DESFIRE) {
			db.update(TABLE_CARDS, args, KEY_ID_VC + " = ? AND " + KEY_ID_DEV + " = ? AND " + KEY_TYPE + " = ?",
	                new String[] { String.valueOf(id), String.valueOf(idDevice), String.valueOf(type) });
		}
		
        db.close();
    }
	
	public void updateCardStatus(int id, int status, int idDevice, int type) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues args = new ContentValues();
		args.put(KEY_STATUS, status);
		
		if(type == Card.TYPE_PAYMENTS) {
			db.update(TABLE_CARDS, args, KEY_ID_SCRIPT + " = ? AND " + KEY_ID_DEV + " = ? AND " + KEY_TYPE + " = ?",
                new String[] { String.valueOf(id), String.valueOf(idDevice), String.valueOf(type) });
		} else if(type == Card.TYPE_MIFARE_CLASSIC || type == Card.TYPE_MIFARE_DESFIRE) {
			db.update(TABLE_CARDS, args, KEY_ID_VC + " = ? AND " + KEY_ID_DEV + " = ? AND " + KEY_TYPE + " = ?",
	                new String[] { String.valueOf(id), String.valueOf(idDevice), String.valueOf(type) });
		}
		
        db.close();
	}
	
	public void updateCardNumber(int id, String room, int idDevice, int type) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues args = new ContentValues();
		args.put(KEY_NUMBER, room);
		
		if(type == Card.TYPE_MIFARE_CLASSIC || type == Card.TYPE_MIFARE_DESFIRE) {
			db.update(TABLE_CARDS, args, KEY_ID_VC + " = ? AND " + KEY_ID_DEV + " = ? AND " + KEY_TYPE + " = ?",
	                new String[] { String.valueOf(id), String.valueOf(idDevice), String.valueOf(type) });
		}
		
        db.close();
	}
	
	public void updateCardsOrder(int srcId, int srcOrder, int destId, int destOrder, int idDevice) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		String query = "SELECT id, listOrder FROM " + TABLE_CARDS;
		Cursor cursor = db.rawQuery(query, null);

		if (cursor.moveToFirst()) {
			do {
				int id = Integer.parseInt(cursor.getString(0));
				int order = Integer.parseInt(cursor.getString(1));

				if(destOrder > srcOrder) {
					if(order > srcOrder && order <= destOrder) {
						ContentValues args = new ContentValues();
						args.put(KEY_ORDER, order - 1);
						
						db.update(TABLE_CARDS, args, KEY_ID+" = ? AND " + KEY_ID_DEV + " = ?",
				                new String[] { String.valueOf(id), String.valueOf(idDevice) });
					}
				} else {
					if(order < srcOrder && order >= destOrder) {
						ContentValues args = new ContentValues();
						args.put(KEY_ORDER, order + 1);
						
						db.update(TABLE_CARDS, args, KEY_ID+" = ? AND " + KEY_ID_DEV + " = ?",
				                new String[] { String.valueOf(id), String.valueOf(idDevice) });
					}
				}
			} while (cursor.moveToNext());
		}
		
		// Update the order for the source
		ContentValues args = new ContentValues();
		args.put(KEY_ORDER, destOrder);

		db.update(TABLE_CARDS, args, KEY_ID+" = ? AND " + KEY_ID_DEV + " = ?",
                new String[] { String.valueOf(srcId), String.valueOf(idDevice) });
		
		cursor.close();
        db.close();
	}
	
	public int getCardToCreateOrder(int idDevice) {
		SQLiteDatabase db = this.getReadableDatabase();

		String queryServices = "SELECT * FROM " + TABLE_CARDS + " WHERE " + KEY_ID_DEV + " = " + idDevice;	
		Cursor cursorServices = db.rawQuery(queryServices, null);
				
		// Update the count
		return cursorServices.getCount() + 1;
	}
}