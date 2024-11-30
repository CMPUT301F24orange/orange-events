package com.example.orange.ui.notifications;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.orange.R;
import com.example.orange.data.firebase.FirebaseCallback;
import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.data.model.Event;
import com.example.orange.data.model.Notification;
import com.example.orange.data.model.NotificationType;
import com.example.orange.data.model.User;
import com.example.orange.ui.events.entrantEventDetailsActivity;
import com.example.orange.ui.events.entrantEventDetailsFragment;
import com.example.orange.ui.profile.ProfileFragment;
import com.example.orange.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class EntrantNotifications{
    private static SessionManager sessionManager;
    private static FirebaseService firebaseService;
    public static final String TAG = "ORANGE";
    public static final String LOTTERY_CHANNEL_ID = "lottery_channel";
    private final String postURL = "https://fcm.googleapis.com/v1/projects/event-lottery-system---orange/messages:send";
    public String eid = "";
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
                .setSmallIcon(R.drawable.app_logo_orange)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel(context);
        }
        manager.notify(4255,builder.build());
    }
    public void sendToPhone(Context context, String title, String message, User user, Notification notification){
        FirebaseNotifications firebaseNotifications = new FirebaseNotifications();
        firebaseNotifications.onNewToken(user.getFcmToken());
        Log.d(TAG, context.toString());
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JSONObject mainObj = new JSONObject();
        try {
            JSONObject messageObject = new JSONObject();
            JSONObject notificationsObject = new JSONObject();
            notificationsObject.put("title", title);
            notificationsObject.put("body", message);

            messageObject.put("token",user.getFcmToken());
            messageObject.put("notification", notificationsObject);

            mainObj.put("message", messageObject);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, postURL, mainObj, response -> {
                // code run got response
            },volleyError ->{
                // code run error
            }) {
                @NonNull
                @Override
                public Map<String, String> getHeaders() {
                    AccessToken accessToken = new AccessToken();
                    String accessKey = accessToken.getAccessToken();
                    Map<String, String> header = new HashMap<>();
                    header.put("content-Type", "application/Json");
                    header.put("authorization", "Bearer " + accessKey);
                    return header;
                }
            };

            requestQueue.add(request);
        }catch (JSONException e){
            e.printStackTrace();
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOTTERY_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.app_logo_orange)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        // Pass the eventId as a bundle
        if(notification.getType() == NotificationType.SELECTED_TO_PARTICIPATE){
            Intent intent = new Intent(context, entrantEventDetailsActivity.class);
            Bundle idBundle = new Bundle();
            idBundle.putString("event_id", notification.getEventId());
            intent.putExtras(idBundle);
            PendingIntent pendingIntent;
            pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            builder.setContentIntent(pendingIntent);
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel(context);
        }
        manager.notify(4255,builder.build());
    }

}
