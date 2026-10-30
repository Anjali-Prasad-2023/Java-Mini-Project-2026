
package com.example.rental_system;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService
        extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        if (message.getNotification() != null) {
            String title = message.getNotification().getTitle();
            String body = message.getNotification().getBody();

            Log.d("RentNotifications",
                    "Title: " + title + ", Message: " + body);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        // A new FCM token was generated.
        // We will register it for the logged-in user
        // in the next step.
        Log.d("RentNotifications", "FCM token updated");
    }
}
