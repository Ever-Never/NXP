package com.nxp.nxpwalletconndev.notifications;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.nxp.nxpwalletconndev.R;

public class MyNotification {
	public static final int NOTIF_ID_OPERATING = 0x01;
	public static final int NOTIF_ID_COMPLETED = 0x02;
	
	public static void show(Context ctx, String title, String msg, int operation) {
//		Intent resultIntent = new Intent(ctx, MyCardsActivity.class);
//
//		PendingIntent resultPendingIntent =
//		    PendingIntent.getActivity(ctx,
//			    0,
//			    resultIntent,
//			    PendingIntent.FLAG_UPDATE_CURRENT
//		);
		
		NotificationCompat.Builder mBuilder =
			    new NotificationCompat.Builder(ctx)
			    .setSmallIcon(R.drawable.ic_launcher)
			    .setContentTitle(title)
			    .setContentText(msg);
		
		if(operation == NOTIF_ID_COMPLETED) {
			mBuilder.setAutoCancel(true);
//			mBuilder.setContentIntent(resultPendingIntent);
			
			// Delete previois operation in progress notification
			cancel(ctx);
		}
		
		// Sets an ID for the notification
		int mNotificationId = operation;
		// Gets an instance of the NotificationManager service
		NotificationManager mNotifyMgr =  (NotificationManager) ctx.getSystemService(Activity.NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify(mNotificationId, mBuilder.build());
	}
	
	public static void cancel(Context ctx) {
		// Gets an instance of the NotificationManager service
		NotificationManager mNotifyMgr =  (NotificationManager) ctx.getSystemService(Activity.NOTIFICATION_SERVICE);
		// Cancel previous notification
		mNotifyMgr.cancel(NOTIF_ID_OPERATING);
	}
}
