package io.kristal.fcmplugin;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

import org.cobaltians.cobalt.Cobalt;


public class FcmPluginMessagingService extends Service{

    protected final static String TAG = FcmPluginMessagingService.class.getSimpleName();

    public FcmPluginMessagingService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public static void onMessageReceived(RemoteMessage remoteMessage, Context context) {
        // TODO(developer): Handle FCM messages here.

        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Notification Message Body: " + remoteMessage.getNotification().getBody());

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setContentTitle(remoteMessage.getNotification().getTitle())
                        .setContentText(remoteMessage.getNotification().getBody());

        // Get the defautl activity from cobalt.conf to create intent
        Bundle bundle = Cobalt.getInstance(context).getConfigurationForController("default");
        String activityName = bundle.getString(Cobalt.kActivity);
        Class<?> pClass;
        try {
            pClass = Class.forName(activityName);

            // Creating intent to redirect user tapping the notification
            // By default, we're redirecting on the main activity
            Intent resultIntent = new Intent(context, pClass);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(pClass);
            stackBuilder.addNextIntent(resultIntent);

            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


            mNotificationManager.notify(0, mBuilder.build());
        }
        catch (ClassNotFoundException e){
            if (Cobalt.DEBUG){
                Log.e(TAG, "Default activity class from cobalt.conf not found");
                e.printStackTrace();
            }
        }
    }

    public static void onMessageReceived(Notification notif, Context context) {
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        mNotificationManager.notify(0, notif);
    }

}
