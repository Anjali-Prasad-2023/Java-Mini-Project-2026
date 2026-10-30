
package com.example.rental_system;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MyRequestsActivity extends AppCompatActivity {

    LinearLayout layoutRequests;
    TextView tvNoRequests;

    LinearLayout navHome;
    LinearLayout navBrowse;
    LinearLayout navRequests;
    LinearLayout navProfile;

    FirebaseFirestore db;
    FirebaseAuth auth;

    ListenerRegistration requestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        layoutRequests = findViewById(R.id.layoutRequests);
        tvNoRequests = findViewById(R.id.tvNoRequests);

        navHome = findViewById(R.id.navHome);
        navBrowse = findViewById(R.id.navBrowse);
        navRequests = findViewById(R.id.navRequests);
        navProfile = findViewById(R.id.navProfile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Bottom navigation.
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(
                    MyRequestsActivity.this,
                    TenantHomeActivity.class
            ));
            finish();
        });

        navBrowse.setOnClickListener(v -> {
            startActivity(new Intent(
                    MyRequestsActivity.this,
                    BrowseRoomsActivity.class
            ));
            finish();
        });

        navRequests.setOnClickListener(v -> {
            // Already on My Requests.
        });

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(
                    MyRequestsActivity.this,
                    ProfileActivity.class
            ));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadRequests();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop listening when the screen is no longer visible.
        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }
    }

    // Load requests belonging to the signed-in tenant.
    private void loadRequests() {

        if (auth.getCurrentUser() == null) {
            showEmptyMessage("Please sign in to view your requests.");
            return;
        }

        String tenantId = auth.getCurrentUser().getUid();

        showEmptyMessage("Loading your requests...");

        requestListener = db.collection("rentalRequests")
                .whereEqualTo("tenantId", tenantId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        showEmptyMessage(
                                "Could not load requests. Please try again."
                        );

                        Toast.makeText(
                                MyRequestsActivity.this,
                                "Error: " + error.getLocalizedMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        showEmptyMessage(
                                "You have no rental requests yet."
                        );
                        return;
                    }

                    // Remove previously displayed cards.
                    layoutRequests.removeAllViews();

                    tvNoRequests.setVisibility(TextView.GONE);

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {

                        String requestId = document.getId();

                        String roomId = document.getString("roomId");

                        String roomType =
                                document.getString("roomTitle");

                        if (roomType == null || roomType.trim().isEmpty()) {
                            roomType = document.getString("roomType");
                        }

                        String location = document.getString("location");

                        String status = document.getString("status");

                        double rent = 0;

                        Number rentValue =
                                (Number) document.get("rent");

                        if (rentValue != null) {
                            rent = rentValue.doubleValue();
                        }

                        createRequestCard(
                                requestId,
                                roomId,
                                roomType,
                                location,
                                rent,
                                status
                        );
                    }
                });
    }

    // Display the empty message.
    private void showEmptyMessage(String message) {

        layoutRequests.removeAllViews();

        tvNoRequests.setText(message);
        tvNoRequests.setVisibility(TextView.VISIBLE);

        // Keep the existing empty-message TextView inside the layout.
        if (tvNoRequests.getParent() == null) {
            layoutRequests.addView(tvNoRequests);
        }
    }

    // Create a card for each Firestore request.
    private void createRequestCard(
            String requestId,
            String roomId,
            String roomType,
            String location,
            double rent,
            String status) {

        CardView card = new CardView(this);

        card.setRadius(20);
        card.setCardElevation(6);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 8, 0, 20);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 24, 24, 24);

        // Room title.
        TextView roomName = new TextView(this);
        roomName.setText(safeText(roomType, "Room"));
        roomName.setTextSize(20);
        roomName.setTextColor(Color.rgb(34, 34, 34));
        roomName.setTypeface(null, 1);

        // Location.
        TextView locationText = new TextView(this);
        locationText.setText("📍 " + safeText(location, "Not provided"));
        locationText.setTextSize(15);
        locationText.setTextColor(Color.DKGRAY);

        LinearLayout.LayoutParams locationParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        locationParams.setMargins(0, 8, 0, 0);
        locationText.setLayoutParams(locationParams);

        // Monthly rent.
        TextView rentText = new TextView(this);
        rentText.setText("₹" + rent + " / month");
        rentText.setTextSize(16);
        rentText.setTypeface(null, 1);

        LinearLayout.LayoutParams rentParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rentParams.setMargins(0, 10, 0, 0);
        rentText.setLayoutParams(rentParams);

        // Request ID.
        TextView requestText = new TextView(this);
        requestText.setText("Request ID: " + requestId);
        requestText.setTextSize(13);
        requestText.setTextColor(Color.DKGRAY);

        LinearLayout.LayoutParams requestParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        requestParams.setMargins(0, 10, 0, 0);
        requestText.setLayoutParams(requestParams);

        // Room ID.
        TextView roomIdText = new TextView(this);
        roomIdText.setText("Room ID: " + safeText(roomId, "Not provided"));
        roomIdText.setTextSize(13);
        roomIdText.setTextColor(Color.DKGRAY);

        // Request status.
        TextView statusText = new TextView(this);

        String actualStatus = status == null
                ? "pending"
                : status.trim().toLowerCase();

        String displayStatus;

        if (actualStatus.equals("active")) {
            displayStatus = "Accepted";
            statusText.setTextColor(Color.rgb(46, 125, 50));

        } else if (actualStatus.equals("rejected")) {
            displayStatus = "Rejected";
            statusText.setTextColor(Color.rgb(198, 40, 40));

        } else if (actualStatus.equals("pending")) {
            displayStatus = "Pending";
            statusText.setTextColor(Color.rgb(245, 124, 0));

        } else {
            displayStatus = status == null
                    ? "Unknown"
                    : status;
            statusText.setTextColor(Color.DKGRAY);
        }

        statusText.setText("Status: " + displayStatus);
        statusText.setTextSize(15);
        statusText.setTypeface(null, 1);
        statusText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        statusParams.setMargins(0, 15, 0, 0);
        statusText.setLayoutParams(statusParams);

        // Add details to the card.
        content.addView(roomName);
        content.addView(locationText);
        content.addView(rentText);
        content.addView(requestText);
        content.addView(roomIdText);
        content.addView(statusText);

        card.addView(content);
        layoutRequests.addView(card);
    }

    private String safeText(String value, String defaultValue) {
        return value == null || value.trim().isEmpty()
                ? defaultValue
                : value;
    }
}
