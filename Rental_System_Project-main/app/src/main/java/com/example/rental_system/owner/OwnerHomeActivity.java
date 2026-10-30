package com.example.rental_system.owner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rental_system.ChatbotActivity;
import com.example.rental_system.LoginActivity;
import com.example.rental_system.NotificationsActivity;
import com.example.rental_system.R;
import com.example.rental_system.RentReminderScheduler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class OwnerHomeActivity extends BaseActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView welcomeText;
    private TextView propertyCount;
    private TextView availableCount;
    private TextView pendingCount;
    private TextView activeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_owner_home);

        findViewById(R.id.btnChatbot).setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            OwnerHomeActivity.this,
                            ChatbotActivity.class
                    );

            intent.putExtra("user_role", "owner");
            startActivity(intent);
        });

        db = FirebaseFirestore.getInstance();

        RentReminderScheduler.schedule(this);

        welcomeText = findViewById(R.id.welcomeText);
        propertyCount = findViewById(R.id.propertyCount);
        availableCount = findViewById(R.id.availableCount);
        pendingCount = findViewById(R.id.pendingCount);
        activeCount = findViewById(R.id.activeCount);

        Button addRoomButton = findViewById(R.id.addRoomButton);
        Button notificationsButton = findViewById(R.id.notificationsButton);

        LinearLayout propertiesButton = findViewById(R.id.propertiesButton);
        LinearLayout requestsButton = findViewById(R.id.requestsButton);
        LinearLayout rentalsButton = findViewById(R.id.rentalsButton);
        LinearLayout paymentsButton = findViewById(R.id.paymentsButton);
        LinearLayout complaintsButton = findViewById(R.id.complaintsButton);
        LinearLayout reviewsButton = findViewById(R.id.reviewsButton);
        LinearLayout profileButton = findViewById(R.id.profileButton);

        addRoomButton.setOnClickListener(v ->
                open(AddRoomActivity.class));

        notificationsButton.setOnClickListener(v ->
                open(NotificationsActivity.class));

        propertiesButton.setOnClickListener(v ->
                open(MyPropertiesActivity.class));

        requestsButton.setOnClickListener(v ->
                open(RentalRequestsActivity.class));

        rentalsButton.setOnClickListener(v ->
                open(ManageRentalActivity.class));

        paymentsButton.setOnClickListener(v ->
                open(OwnerPaymentsActivity.class));

        complaintsButton.setOnClickListener(v ->
                open(OwnerComplaintsActivity.class));

        reviewsButton.setOnClickListener(v ->
                open(OwnerReviewsActivity.class));

        profileButton.setOnClickListener(v ->
                open(OwnerProfileActivity.class));

        loadDashboard();
    }

    private void open(Class<?> activity) {
        startActivity(new Intent(this, activity));
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }

    private void loadDashboard() {
        if (auth == null || auth.getCurrentUser() == null || db == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        // Load owner's name.
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    String name = document.getString("name");

                    welcomeText.setText("Welcome, " +
                            (name == null || name.trim().isEmpty()
                                    ? "Owner" : name));
                });

        // Count all properties and only those marked available.
        db.collection("rooms")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int total = snapshot.size();
                    int available = 0;

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Boolean isAvailable = document.getBoolean("available");

                        if (Boolean.TRUE.equals(isAvailable)) {
                            available++;
                        }
                    }

                    propertyCount.setText(String.valueOf(total));
                    availableCount.setText(String.valueOf(available));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Unable to load properties: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());

        // Count rental requests awaiting owner approval.
        db.collection("rentalRequests")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot ->
                        pendingCount.setText(
                                String.valueOf(snapshot.size())
                        ))
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Unable to load pending requests",
                                Toast.LENGTH_SHORT).show());

        // Count active rentals.
        db.collection("rentals")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snapshot ->
                        activeCount.setText(
                                String.valueOf(snapshot.size())
                        ))
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Unable to load active rentals",
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (auth != null && auth.getCurrentUser() != null
                && db != null && welcomeText != null) {
            loadDashboard();
        }
    }

    public void openProperties(View view) {
        open(MyPropertiesActivity.class);
    }

    public void openRequests(View view) {
        open(RentalRequestsActivity.class);
    }

    public void openRentals(View view) {
        open(ManageRentalActivity.class);
    }

    public void openPayments(View view) {
        open(OwnerPaymentsActivity.class);
    }

    public void openComplaints(View view) {
        open(OwnerComplaintsActivity.class);
    }

    public void openReviews(View view) {
        open(OwnerReviewsActivity.class);
    }

    public void openProfile(View view) {
        open(OwnerProfileActivity.class);
    }

}