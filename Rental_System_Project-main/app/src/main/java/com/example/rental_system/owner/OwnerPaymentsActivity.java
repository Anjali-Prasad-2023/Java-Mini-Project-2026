package com.example.rental_system.owner;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.app.AlertDialog;

import com.example.rental_system.R;
import com.example.rental_system.model.Payment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerPaymentsActivity extends BaseActivity {

    private ListView listView;
    private FirebaseFirestore db;

    private final List<Payment> payments = new ArrayList<>();
    private final List<String> displayList = new ArrayList<>();

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_payments);

        auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        listView = findViewById(R.id.listPayments);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayList
        );

        listView.setAdapter(adapter);

        // Tap a pending payment to confirm it.
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= payments.size()) {
                return;
            }

            Payment payment = payments.get(position);
            String status = payment.getStatus();

            if (isPending(status)) {
                new AlertDialog.Builder(this)
                        .setTitle("Confirm payment")
                        .setMessage(
                                "Confirm that you have received ₹"
                                        + payment.getAmount()
                                        + "?"
                        )
                        .setPositiveButton("Confirm", (dialog, which) ->
                                updatePaymentStatus(payment, "verified"))
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                showMessage("This payment is not awaiting verification.");
            }
        });

        // Long-press a pending payment to reject it.
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= payments.size()) {
                return true;
            }

            Payment payment = payments.get(position);

            if (isPending(payment.getStatus())) {
                new AlertDialog.Builder(this)
                        .setTitle("Reject payment")
                        .setMessage(
                                "Are you sure you want to reject this payment?"
                        )
                        .setPositiveButton("Reject", (dialog, which) ->
                                updatePaymentStatus(payment, "rejected"))
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                showMessage("Only pending payments can be rejected.");
            }

            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPayments();
    }

    private boolean isPending(String status) {
        if (status == null) {
            return false;
        }

        return status.equalsIgnoreCase("PENDING")
                || status.equalsIgnoreCase("pending_verification");
    }

    private void loadPayments() {
        if (!isLoggedIn()) {
            logout();
            return;
        }

        String ownerId = auth.getCurrentUser().getUid();

        db.collection("payments")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    payments.clear();
                    displayList.clear();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Payment payment = document.toObject(Payment.class);

                        if (payment == null) {
                            continue;
                        }

                        payment.setPaymentId(document.getId());
                        payments.add(payment);

                        String method = payment.getMethod() == null
                                ? "Not specified"
                                : payment.getMethod();

                        String status = payment.getStatus() == null
                                ? "Unknown"
                                : payment.getStatus();

                        String roomTitle = document.getString("roomTitle");
                        String rentMonth = document.getString("rentMonth");

                        displayList.add(
                                "Room: "
                                        + (roomTitle == null ? "Rental room" : roomTitle)
                                        + "\nRent month: "
                                        + (rentMonth == null ? "Not specified" : rentMonth)
                                        + "\nAmount: ₹"
                                        + payment.getAmount()
                                        + "\nMethod: "
                                        + method
                                        + "\nStatus: "
                                        + status
                                        + (isPending(status)
                                        ? "\nTap to confirm | Long-press to reject"
                                        : "")
                        );
                    }

                    adapter.notifyDataSetChanged();

                    if (payments.isEmpty()) {
                        showMessage("No payment records found.");
                    }
                })
                .addOnFailureListener(e ->
                        showMessage("Failed to load payments: " + e.getMessage())
                );
    }

    private void updatePaymentStatus(Payment payment, String newStatus) {
        String paymentId = payment.getPaymentId();

        if (paymentId == null || paymentId.trim().isEmpty()) {
            showMessage("Payment ID is missing.");
            return;
        }

        String ownerId = auth.getCurrentUser().getUid();

        db.collection("payments")
                .document(paymentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        showMessage("Payment record was not found.");
                        loadPayments();
                        return;
                    }

                    String savedOwnerId = document.getString("ownerId");
                    String tenantId = document.getString("tenantId");

                    // Make sure this payment belongs to the logged-in owner.
                    if (savedOwnerId == null || !savedOwnerId.equals(ownerId)) {
                        showMessage("You cannot update this payment.");
                        return;
                    }

                    if (tenantId == null || tenantId.trim().isEmpty()) {
                        showMessage("Tenant ID is missing. Payment was not updated.");
                        return;
                    }

                    String currentStatus = document.getString("status");

                    if (!isPending(currentStatus)) {
                        showMessage("This payment has already been processed.");
                        loadPayments();
                        return;
                    }

                    String roomTitle = document.getString("roomTitle");
                    String rentMonth = document.getString("rentMonth");
                    String requestId = document.getString("requestId");
                    String roomId = document.getString("roomId");

                    String title;
                    String message;
                    String type;

                    if ("verified".equals(newStatus)) {
                        title = "Rent payment confirmed";
                        message = "Your payment of ₹"
                                + document.get("amount")
                                + " for "
                                + (roomTitle == null ? "your room" : roomTitle)
                                + (rentMonth == null ? "" : " (" + rentMonth + ")")
                                + " has been confirmed by the owner.";
                        type = "payment_verified";
                    } else {
                        title = "Rent payment rejected";
                        message = "Your payment of ₹"
                                + document.get("amount")
                                + " for "
                                + (roomTitle == null ? "your room" : roomTitle)
                                + (rentMonth == null ? "" : " (" + rentMonth + ")")
                                + " was rejected. Please contact the owner.";
                        type = "payment_rejected";
                    }

                    WriteBatch batch = db.batch();

                    batch.update(
                            db.collection("payments").document(paymentId),
                            "status", newStatus
                    );

                    // Create an in-app notification for the tenant.
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("recipientId", tenantId);
                    notification.put("userId", tenantId);
                    notification.put("title", title);
                    notification.put("message", message);
                    notification.put("type", type);
                    notification.put("paymentId", paymentId);
                    notification.put("requestId", requestId);
                    notification.put("roomId", roomId);
                    notification.put("rentMonth", rentMonth);
                    notification.put("read", false);
                    notification.put("createdAt", System.currentTimeMillis());

                    batch.set(
                            db.collection("notifications").document(),
                            notification
                    );

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                showMessage(
                                        "Payment "
                                                + ("verified".equals(newStatus)
                                                ? "confirmed"
                                                : "rejected")
                                                + ". Tenant notified."
                                );
                                loadPayments();
                            })
                            .addOnFailureListener(e ->
                                    showMessage(
                                            "Could not update payment: "
                                                    + e.getMessage()
                                    )
                            );
                })
                .addOnFailureListener(e ->
                        showMessage("Could not read payment: " + e.getMessage())
                );
    }
}
