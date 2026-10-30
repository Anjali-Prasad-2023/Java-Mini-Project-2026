
package com.example.rental_system;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout notificationList;
    private TextView emptyMessage;
    private ListenerRegistration notificationListener;

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247, 248, 250));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));
        header.setBackgroundColor(Color.WHITE);

        Button backButton = new Button(this);
        backButton.setText("‹ Back");
        backButton.setAllCaps(false);
        backButton.setTextColor(Color.rgb(33, 107, 80));
        backButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.WHITE)
        );
        backButton.setOnClickListener(v -> finish());

        TextView heading = new TextView(this);
        heading.setText("Notifications");
        heading.setTextSize(22);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(Color.rgb(30, 55, 45));

        header.addView(backButton);
        header.addView(heading);

        root.addView(header);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        notificationList = new LinearLayout(this);
        notificationList.setOrientation(LinearLayout.VERTICAL);
        notificationList.setPadding(dp(16), dp(16), dp(16), dp(20));

        emptyMessage = new TextView(this);
        emptyMessage.setText("Loading notifications...");
        emptyMessage.setTextSize(15);
        emptyMessage.setTextColor(Color.DKGRAY);
        emptyMessage.setGravity(Gravity.CENTER);
        emptyMessage.setPadding(dp(12), dp(30), dp(12), dp(30));

        notificationList.addView(emptyMessage);
        scrollView.addView(notificationList);

        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1
        ));

        setContentView(root);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first.",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        if (notificationListener != null) {
            notificationListener.remove();
        }

        notificationListener = db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        showMessage("Unable to load notifications.");
                        Toast.makeText(this,
                                error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    notificationList.removeAllViews();

                    if (snapshot == null || snapshot.isEmpty()) {
                        showMessage("You're all caught up!\n\n"
                                + "Your rental updates will appear here.");
                        return;
                    }

                    emptyMessage = null;

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {
                        addNotificationCard(document);
                    }
                });
    }

    private void showMessage(String message) {
        notificationList.removeAllViews();

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(15);
        text.setTextColor(Color.DKGRAY);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(12), dp(30), dp(12), dp(30));

        notificationList.addView(text);
    }

    private void addNotificationCard(DocumentSnapshot document) {
        String title = document.getString("title");
        String message = document.getString("message");
        String type = document.getString("type");
        Boolean readValue = document.getBoolean("read");

        boolean isRead = Boolean.TRUE.equals(readValue);

        if (title == null || title.trim().isEmpty()) {
            title = "Rental Update";
        }

        if (message == null || message.trim().isEmpty()) {
            message = "You have a new update.";
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable background = new GradientDrawable();
        background.setColor(isRead ? Color.WHITE : Color.rgb(232, 242, 255));
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(225, 230, 235));
        card.setBackground(background);
        card.setElevation(dp(2));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        cardParams.bottomMargin = dp(12);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(Color.rgb(30, 55, 45));
        titleView.setTypeface(null, Typeface.BOLD);

        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setTextSize(14);
        messageView.setTextColor(Color.rgb(75, 80, 85));
        messageView.setPadding(0, dp(8), 0, dp(8));

        card.addView(titleView);
        card.addView(messageView);

        Object createdAt = document.get("createdAt");

        if (createdAt instanceof Number) {
            long timestamp = ((Number) createdAt).longValue();

            TextView dateView = new TextView(this);
            dateView.setText(new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            ).format(new Date(timestamp)));
            dateView.setTextSize(12);
            dateView.setTextColor(Color.DKGRAY);

            card.addView(dateView);
        }

        if (type != null && !type.trim().isEmpty()) {
            // The type is available if you later want to style
            // rental, payment or complaint notifications differently.
        }

        if (!isRead) {
            Button markReadButton = new Button(this);
            markReadButton.setText("Mark as read");
            markReadButton.setAllCaps(false);
            markReadButton.setTextColor(Color.WHITE);
            markReadButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.rgb(33, 107, 80)
                    )
            );

            markReadButton.setOnClickListener(v ->
                    db.collection("notifications")
                            .document(document.getId())
                            .update("read", true)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(
                                            this,
                                            "Marked as read",
                                            Toast.LENGTH_SHORT
                                    ).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Could not update notification: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show())
            );

            card.addView(markReadButton);
        } else {
            TextView readLabel = new TextView(this);
            readLabel.setText("✓ Read");
            readLabel.setTextSize(12);
            readLabel.setTextColor(Color.rgb(33, 107, 80));
            readLabel.setPadding(0, dp(8), 0, 0);
            card.addView(readLabel);
        }

        notificationList.addView(card, cardParams);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }
}
