
package com.example.rental_system;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TenantHomeActivity extends AppCompatActivity {

    LinearLayout layoutNotifications;
    LinearLayout layoutBrowseRooms;
    LinearLayout layoutMyRequests;
    LinearLayout layoutCurrentRental;
    LinearLayout layoutComplaints;
    LinearLayout layoutReviews;

    LinearLayout navHome;
    LinearLayout navBrowse;
    LinearLayout navRequests;
    LinearLayout navProfile;

    TextView tvUserName;
    TextView tvViewRooms;
    TextView tvCurrentRentalTitle;
    TextView tvCurrentRentalDetails;
    TextView tvCurrentRentalAction;

    TextView tvRecentPaymentTitle;
    TextView tvRecentPaymentDetails;

    FirebaseFirestore db;

    ListenerRegistration paymentListener;
    ListenerRegistration rentalListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RentReminderScheduler.schedule(this);
        RentReminderScheduler.runDemoTest(this);

        setContentView(R.layout.activity_tenant_home);

        findViewById(R.id.btnChatbot).setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            TenantHomeActivity.this,
                            ChatbotActivity.class
                    );

            intent.putExtra("user_role", "tenant");
            startActivity(intent);
        });

        db = FirebaseFirestore.getInstance();

        tvUserName = findViewById(R.id.tvUserName);

        layoutNotifications = findViewById(R.id.layoutNotifications);
        layoutBrowseRooms = findViewById(R.id.layoutBrowseRooms);
        layoutMyRequests = findViewById(R.id.layoutMyRequests);
        layoutCurrentRental = findViewById(R.id.layoutCurrentRental);
        layoutComplaints = findViewById(R.id.layoutComplaints);
        layoutReviews = findViewById(R.id.layoutReviews);

        tvViewRooms = findViewById(R.id.tvViewRooms);

        tvCurrentRentalTitle = findViewById(R.id.tvCurrentRentalTitle);
        tvCurrentRentalDetails = findViewById(R.id.tvCurrentRentalDetails);
        tvCurrentRentalAction = findViewById(R.id.tvCurrentRentalAction);

        tvRecentPaymentTitle = findViewById(R.id.tvRecentPaymentTitle);
        tvRecentPaymentDetails = findViewById(R.id.tvRecentPaymentDetails);

        navHome = findViewById(R.id.navHome);
        navBrowse = findViewById(R.id.navBrowse);
        navRequests = findViewById(R.id.navRequests);
        navProfile = findViewById(R.id.navProfile);

        loadTenantName();

        layoutNotifications.setOnClickListener(v ->
                openScreen(NotificationsActivity.class));

        layoutBrowseRooms.setOnClickListener(v ->
                openScreen(BrowseRoomsActivity.class));

        layoutMyRequests.setOnClickListener(v ->
                openScreen(MyRequestsActivity.class));

        tvViewRooms.setOnClickListener(v ->
                openScreen(BrowseRoomsActivity.class));

        navHome.setOnClickListener(v -> {
            // Already on Home.
        });

        navBrowse.setOnClickListener(v ->
                openScreen(BrowseRoomsActivity.class));

        navRequests.setOnClickListener(v ->
                openScreen(MyRequestsActivity.class));

        navProfile.setOnClickListener(v ->
                openScreen(ProfileActivity.class));

        layoutCurrentRental.setOnClickListener(v ->
                openScreen(ActiveRentalActivity.class));

        tvCurrentRentalTitle.setOnClickListener(v ->
                openScreen(ActiveRentalActivity.class));

        tvCurrentRentalDetails.setOnClickListener(v ->
                openScreen(ActiveRentalActivity.class));

        tvCurrentRentalAction.setOnClickListener(v ->
                openScreen(ActiveRentalActivity.class));

        layoutComplaints.setOnClickListener(v ->
                openScreen(ComplaintActivity.class));

        layoutReviews.setOnClickListener(v ->
                openScreen(ReviewActivity.class));

        // Tapping either part of Recent Payments opens all payment records.
        tvRecentPaymentTitle.setOnClickListener(v ->
                openScreen(PaymentHistoryActivity.class));

        tvRecentPaymentDetails.setOnClickListener(v ->
                openScreen(PaymentHistoryActivity.class));
    }

    private void loadTenantName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            tvUserName.setText("");
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");

                        if (name == null || name.trim().isEmpty()) {
                            name = document.getString("fullName");
                        }

                        if (name != null && !name.trim().isEmpty()) {
                            tvUserName.setText(name.trim());
                        } else {
                            tvUserName.setText(
                                    user.getDisplayName() != null
                                            ? user.getDisplayName() : "");
                        }
                    }
                })
                .addOnFailureListener(error ->
                        tvUserName.setText(
                                user.getDisplayName() != null
                                        ? user.getDisplayName() : ""));
    }

    private void openScreen(Class<?> screen) {
        startActivity(new Intent(TenantHomeActivity.this, screen));
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadActiveRental();
        loadRecentPayment();
    }

    private void loadActiveRental() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            showNoActiveRental("Please log in to view your rentals.");
            return;
        }

        if (rentalListener != null) {
            rentalListener.remove();
            rentalListener = null;
        }

        rentalListener = db.collection("rentalRequests")
                .whereEqualTo("tenantId", user.getUid())
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        showNoActiveRental("Unable to load rental details.");
                        Toast.makeText(this,
                                "Rental error: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        showNoActiveRental("Find a room to get started.");
                        return;
                    }

                    DocumentSnapshot firstActiveRental = null;
                    int activeCount = 0;

                    for (DocumentSnapshot document : snapshots.getDocuments()) {
                        String status = document.getString("status");

                        if (status != null
                                && status.trim().equalsIgnoreCase("active")) {
                            activeCount++;

                            if (firstActiveRental == null) {
                                firstActiveRental = document;
                            }
                        }
                    }

                    if (firstActiveRental == null) {
                        showNoActiveRental(
                                "Your accepted rental will appear here.");
                        return;
                    }

                    displayActiveRental(firstActiveRental, activeCount);
                });
    }

    private void displayActiveRental(
            DocumentSnapshot rental, int activeCount) {

        String roomTitle = rental.getString("roomTitle");
        String location = rental.getString("location");

        Object rentValue = rental.get("rent");
        double rent = rentValue instanceof Number
                ? ((Number) rentValue).doubleValue() : 0.0;

        if (roomTitle == null || roomTitle.trim().isEmpty()) {
            roomTitle = "Your Rental";
        }

        if (location == null || location.trim().isEmpty()) {
            location = "Location not specified";
        }

        String rentText = String.format(
                Locale.getDefault(), "₹%.2f/month", rent);

        tvCurrentRentalTitle.setText(
                activeCount == 1
                        ? "🏠 " + roomTitle
                        : "🏠 Your Active Rentals (" + activeCount + " rooms)");

        tvCurrentRentalDetails.setText(
                roomTitle + "\n" + location + "\n" + rentText
                        + (activeCount > 1
                        ? "\n+" + (activeCount - 1) + " more rented room(s)"
                        : ""));

        tvCurrentRentalAction.setText(
                "View All Rentals →");

        tvCurrentRentalTitle.setTextColor(
                android.graphics.Color.parseColor("#000000"));

        tvCurrentRentalDetails.setTextColor(
                android.graphics.Color.parseColor("#333333"));

        tvCurrentRentalAction.setTextColor(
                android.graphics.Color.parseColor("#1769AA"));
    }

    private void showNoActiveRental(String message) {
        tvCurrentRentalTitle.setText("No active rental");
        tvCurrentRentalDetails.setText(message);
        tvCurrentRentalAction.setText("View Available Rooms →");

        tvCurrentRentalTitle.setTextColor(
                android.graphics.Color.parseColor("#333333"));

        tvCurrentRentalDetails.setTextColor(
                android.graphics.Color.parseColor("#555555"));

        tvCurrentRentalAction.setTextColor(
                android.graphics.Color.parseColor("#1769AA"));
    }

    private void loadRecentPayment() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            tvRecentPaymentTitle.setText("No payments yet");
            tvRecentPaymentDetails.setText(
                    "Please log in to view your payments.");
            return;
        }

        if (paymentListener != null) {
            paymentListener.remove();
            paymentListener = null;
        }

        paymentListener = db.collection("payments")
                .whereEqualTo("tenantId", user.getUid())
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        tvRecentPaymentTitle.setText(
                                "Unable to load payments");
                        tvRecentPaymentDetails.setText(
                                "Tap here to open payment history.");
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        tvRecentPaymentTitle.setText("No payments yet");
                        tvRecentPaymentDetails.setText(
                                "Tap here to view your payment history.");
                        return;
                    }

                    DocumentSnapshot latestPayment = null;
                    long latestTime = Long.MIN_VALUE;

                    for (DocumentSnapshot document : snapshots.getDocuments()) {
                        long time = getPaymentTime(document);

                        if (latestPayment == null || time > latestTime) {
                            latestPayment = document;
                            latestTime = time;
                        }
                    }

                    if (latestPayment != null) {
                        displayRecentPayment(latestPayment);
                    }
                });
    }

    private long getPaymentTime(DocumentSnapshot document) {
        Object createdAt = document.get("createdAt");

        if (createdAt instanceof Timestamp) {
            return ((Timestamp) createdAt).toDate().getTime();
        }

        if (createdAt instanceof Number) {
            return ((Number) createdAt).longValue();
        }

        Object paymentDate = document.get("paymentDate");

        if (paymentDate instanceof Timestamp) {
            return ((Timestamp) paymentDate).toDate().getTime();
        }

        if (paymentDate instanceof Number) {
            return ((Number) paymentDate).longValue();
        }

        if (paymentDate instanceof String) {
            String dateText = ((String) paymentDate).trim();

            String[] patterns = {
                    "dd/MM/yyyy HH:mm:ss",
                    "dd/MM/yyyy",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd",
                    "dd-MM-yyyy"
            };

            for (String pattern : patterns) {
                try {
                    SimpleDateFormat format =
                            new SimpleDateFormat(pattern, Locale.getDefault());
                    format.setLenient(false);

                    Date date = format.parse(dateText);

                    if (date != null) {
                        return date.getTime();
                    }
                } catch (Exception ignored) {
                    // Try another supported date format.
                }
            }
        }

        return 0L;
    }

    private void displayRecentPayment(DocumentSnapshot payment) {
        Object amountValue = payment.get("amount");

        double amount = amountValue instanceof Number
                ? ((Number) amountValue).doubleValue() : 0.0;

        String rentMonth = payment.getString("rentMonth");
        String paymentDate = payment.getString("paymentDate");
        String status = payment.getString("status");
        String method = payment.getString("method");
        String roomTitle = payment.getString("roomTitle");

        if (rentMonth == null || rentMonth.trim().isEmpty()) {
            rentMonth = "Month not specified";
        }

        if (status == null || status.trim().isEmpty()) {
            status = "Status not specified";
        }

        if (roomTitle == null || roomTitle.trim().isEmpty()) {
            roomTitle = "Room details not saved";
        }

        tvRecentPaymentTitle.setText(
                String.format(Locale.getDefault(), "₹%.2f", amount)
                        + " - " + rentMonth);

        StringBuilder details = new StringBuilder();
        details.append(roomTitle).append("\n");
        details.append("Status: ").append(status);

        if (paymentDate != null && !paymentDate.isEmpty()) {
            details.append("\nDate: ").append(paymentDate);
        }

        if (method != null && !method.isEmpty()) {
            details.append("\nMethod: ").append(method);
        }

        details.append("\nTap to view all payments →");

        tvRecentPaymentDetails.setText(details.toString());
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (paymentListener != null) {
            paymentListener.remove();
            paymentListener = null;
        }

        if (rentalListener != null) {
            rentalListener.remove();
            rentalListener = null;
        }
    }
}
