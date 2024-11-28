package com.example.orange.ui.notifications;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.orange.data.firebase.FirebaseService;
import com.example.orange.utils.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

public class FirebaseNotifications extends FirebaseMessagingService {
    Context context;
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        updateNewToken(token);
        Log.d("TOKEN", token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String title = message.getNotification().getTitle();
        String content = message.getNotification().getBody();
        String data = new Gson().toJson(message.getData());
        EntrantNotifications.sendNotification(this, title, content);

        Log.d(EntrantNotifications.TAG, data);


    }
    private void updateNewToken(String token){
        //update db
    }
}
