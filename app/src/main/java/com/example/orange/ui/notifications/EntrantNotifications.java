package com.example.orange.ui.notifications;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.orange.R;


public class EntrantNotifications {

    public static final String LOTTERY_CHANNEL_ID = "lottery_channel";

    public static void createChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(notificationManager != null && notificationManager.getNotificationChannel(LOTTERY_CHANNEL_ID) == null){
                NotificationChannel channel = new NotificationChannel(LOTTERY_CHANNEL_ID, "Event Waiting List Status Notifications", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Whether or not you want lottery status notifications");
                channel.enableLights(true);
                channel.setLightColor(Color.BLUE);
                channel.enableVibration(true);

                notificationManager.createNotificationChannel(channel);

            }
        }
    }

    public static void sendNotification(Context context, String title, String message){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOTTERY_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.app_logo)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManagerCompat.notify(4255,builder.build());
    }
}
