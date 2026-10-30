
package com.example.rental_system;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RentReminderWorker extends Worker {

    private static final String CHANNEL_ID = "rent_reminders";
    private static final String PREFS_NAME = "rent_reminders";

    public RentReminderWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            return Result.success();
        }

        boolean demoRun = getInputData()
                .getBoolean("demo_run", false);

        Calendar calendar = Calendar.getInstance();

        // Demo uses the 5th-day reminder logic.
        // Normal runs use the actual phone date.
        int day = demoRun
                ? 5
                : calendar.get(Calendar.DAY_OF_MONTH);

        if (!demoRun && day != 1 && day != 5 && day != 6) {
            return Result.success();
        }

        String rentMonth = new SimpleDateFormat(
                "yyyy-MM", Locale.US
        ).format(new Date());

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            checkRentals(db, uid, rentMonth, day, true, demoRun);
            checkRentals(db, uid, rentMonth, day, false, demoRun);

            return Result.success();

        } catch (Exception e) {
            return Result.retry();
        }
    }

    private void checkRentals(
            FirebaseFirestore db,
            String uid,
            String rentMonth,
            int day,
            boolean isTenant,
            boolean demoRun) throws Exception {

        String userField = isTenant ? "tenantId" : "ownerId";

        for (DocumentSnapshot rental : Tasks.await(
                db.collection("rentalRequests")
                        .whereEqualTo(userField, uid)
                        .whereEqualTo("status", "active")
                        .get()
        ).getDocuments()) {

            checkRental(
                    db,
                    rental,
                    uid,
                    rentMonth,
                    day,
                    isTenant,
                    demoRun
            );
        }
    }

    private void checkRental(
            FirebaseFirestore db,
            DocumentSnapshot rental,
            String uid,
            String rentMonth,
            int day,
            boolean isTenant,
            boolean demoRun) throws Exception {

        String requestId = rental.getString("requestId");

        if (requestId == null || requestId.isEmpty()) {
            requestId = rental.getId();
        }

        String roomTitle = rental.getString("roomTitle");

        if (roomTitle == null || roomTitle.trim().isEmpty()) {
            roomTitle = "your rented room";
        }

        String paymentStatus = "unpaid";

        for (DocumentSnapshot payment : Tasks.await(
                db.collection("payments")
                        .whereEqualTo("requestId", requestId)
                        .whereEqualTo("rentMonth", rentMonth)
                        .get()
        ).getDocuments()) {

            String status = payment.getString("status");

            if (status == null) {
                continue;
            }

            status = status.toLowerCase(Locale.ROOT);

            if (status.equals("verified")
                    || status.equals("confirmed")
                    || status.equals("paid")) {

                paymentStatus = "paid";
                break;

            } else if (status.equals("pending_verification")
                    || status.equals("pending")
                    || status.equals("pending verification")) {

                paymentStatus = "pending";
            }
        }

        // Never remind about rent that has already been paid.
        if (paymentStatus.equals("paid")) {
            return;
        }

        String title;
        String message;
        String reminderType;

        if (paymentStatus.equals("pending") && isTenant) {
            // The tenant has submitted payment already.
            return;
        }

        if (paymentStatus.equals("pending")) {

            title = "Payment awaiting verification";
            message = "A rent payment for " + roomTitle
                    + " is awaiting your verification.";

            reminderType = "pending_verification";

        } else {

            reminderType = day == 1 ? "upcoming"
                    : day == 5 ? "due" : "overdue";

            if (isTenant) {

                title = day == 1 ? "Rent reminder"
                        : day == 5 ? "Rent due today"
                        : "Rent overdue";

                message = day == 1
                        ? "Your rent for " + roomTitle
                        + " is due on the 5th."
                        : day == 5
                        ? "Your rent for " + roomTitle
                        + " is due today."
                        : "Please check the payment status for "
                        + roomTitle + ".";

            } else {

                title = day == 1 ? "Upcoming rent"
                        : day == 5 ? "Rent due today"
                        : "Check overdue rent";

                message = day == 1
                        ? "Rent for " + roomTitle
                        + " is due on the 5th."
                        : day == 5
                        ? "Check the rent payment for "
                        + roomTitle + "."
                        : "Check whether rent for "
                        + roomTitle + " has been paid.";
            }
        }

        SharedPreferences prefs =
                getApplicationContext().getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );

        String key = uid + "_" + requestId + "_"
                + rentMonth + "_" + reminderType
                + (demoRun ? "_demo" : "");

        if (prefs.getBoolean(key, false)) {
            return;
        }

        // Display a local Android notification.
        showPhoneNotification(
                key.hashCode(),
                title,
                message
        );

        // Create an in-app Firestore notification.
        Map<String, Object> notification = new HashMap<>();

        notification.put("recipientId", uid);
        notification.put("userId", uid);
        notification.put("title", title);
        notification.put("message", message);
        notification.put(
                "type",
                demoRun ? "rent_reminder_demo" : "rent_reminder"
        );
        notification.put("requestId", requestId);
        notification.put("rentMonth", rentMonth);
        notification.put("read", false);
        notification.put("createdAt", System.currentTimeMillis());

        Tasks.await(
                db.collection("notifications").add(notification)
        );

        prefs.edit().putBoolean(key, true).apply();
    }

    private void showPhoneNotification(
            int notificationId,
            String title,
            String message) {

        Context context = getApplicationContext();

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Rent reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            manager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(message)
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_DEFAULT
                        )
                        .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(
                notificationId,
                builder.build()
        );
    }
}
