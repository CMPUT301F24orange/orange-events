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


/**
 * Handles notifications for entrants, including creating notification channels,
 * sending notifications, and managing FCM notifications.
 *
 * <p>This class is used to create and manage notifications for the application,
 * including lottery status updates and other event-related notifications.</p>
 *
 * @author Brandon Graham
 */
public class EntrantNotifications {

    public static final String TAG = "ORANGE";     //Tag for logging purposes.
    public static final String LOTTERY_CHANNEL_ID = "lottery_channel"; //Notification channel ID for lottery notifications.
    private final String postURL = "https://fcm.googleapis.com/v1/projects/event-lottery-system---orange/messages:send"; //URL for sending Firebase Cloud Messaging (FCM) notifications.
    public String eid = ""; //  Event ID for the notification.

    /**
     * Creates a notification channel for lottery status notifications if it does not already exist.
     *
     * @author Brandon
     * @param context the application context.
     */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && notificationManager.getNotificationChannel(LOTTERY_CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        LOTTERY_CHANNEL_ID,
                        "Event Waiting List Status Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Whether or not you want lottery status notifications");
                channel.enableLights(true);
                channel.setLightColor(Color.BLUE);
                channel.enableVibration(true);

                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Sends a notification with the specified title and message.
     *
     * @author Brandon, Graham
     *
     * @param context the application context.
     * @param title   the title of the notification.
     * @param message the message of the notification.
     */
    public static void sendNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOTTERY_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.app_logo_orange)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(context);
        }
        manager.notify(4255, builder.build());
    }

    /**
     * Sends a notification to a user's phone using Firebase Cloud Messaging (FCM).
     *
     * @author Brandon, Graham
     *
     * @param context      the application context.
     * @param title        the title of the notification.
     * @param message      the message of the notification.
     * @param user         the recipient user.
     * @param notification the notification details.
     */
    public void sendToPhone(Context context, String title, String message, User user, Notification notification) {
        sendFCMNotification(context, title, message, user);
//        createLocalNotification(context, title, message, notification);
    }

    /**
     * Sends a Firebase Cloud Messaging (FCM) notification.
     *
     * @author Brandon, Graham
     *
     * @param context the application context.
     * @param title   the title of the notification.
     * @param message the message of the notification.
     * @param user    the recipient user.
     */
    private void sendFCMNotification(Context context, String title, String message, User user) {
        FirebaseNotifications firebaseNotifications = new FirebaseNotifications();
        firebaseNotifications.onNewToken(user.getFcmToken());
        Log.d(TAG, "Sending FCM notification to token: " + user.getFcmToken());

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JSONObject mainObj = new JSONObject();
        try {
            JSONObject messageObject = new JSONObject();
            JSONObject notificationsObject = new JSONObject();
            notificationsObject.put("title", title);
            notificationsObject.put("body", message);

            messageObject.put("token", user.getFcmToken());
            messageObject.put("notification", notificationsObject);

            mainObj.put("message", messageObject);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    postURL,
                    mainObj,
                    response -> Log.d(TAG, "FCM Notification sent successfully: " + response.toString()),
                    volleyError -> Log.e(TAG, "FCM Notification sending failed: " + volleyError.toString())
            ) {
                @NonNull
                @Override
                public Map<String, String> getHeaders() {
                    AccessToken accessToken = new AccessToken();
                    String accessKey = accessToken.getAccessToken();
                    Map<String, String> header = new HashMap<>();
                    header.put("Content-Type", "application/json");
                    header.put("Authorization", "Bearer " + accessKey);
                    return header;
                }
            };

            requestQueue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a local notification with the specified title, message, and notification details.
     *
     * @author Brandon
     * @param context      the application context.
     * @param title        the title of the notification.
     * @param message      the message of the notification.
     * @param notification the notification details.
     */
    private void createLocalNotification(Context context, String title, String message, Notification notification) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOTTERY_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.app_logo_orange)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (notification.getType() == NotificationType.SELECTED_TO_PARTICIPATE) {
            Intent intent = new Intent(context, entrantEventDetailsActivity.class);
            Bundle idBundle = new Bundle();
            idBundle.putString("event_id", notification.getEventId());
            intent.putExtras(idBundle);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    1,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.setContentIntent(pendingIntent);
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(context);
        }
        manager.notify(new Random().nextInt(), builder.build());
    }
}
