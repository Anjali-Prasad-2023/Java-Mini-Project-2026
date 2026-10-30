
package com.example.rental_system;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RecordPaymentActivity extends AppCompatActivity {

    private TextView tvPaymentRoom, tvPaymentAmount;
    private EditText etRentMonth, etPaymentDate, etReference;
    private Spinner spinnerMethod;
    private Button btnSubmitPayment;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String requestId, ownerId, roomId, roomTitle;
    private double amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_payment);

        tvPaymentRoom = findViewById(R.id.tvPaymentRoom);
        tvPaymentAmount = findViewById(R.id.tvPaymentAmount);
        etRentMonth = findViewById(R.id.etRentMonth);
        etPaymentDate = findViewById(R.id.etPaymentDate);
        etReference = findViewById(R.id.etReference);
        spinnerMethod = findViewById(R.id.spinnerMethod);
        btnSubmitPayment = findViewById(R.id.btnSubmitPayment);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        requestId = getIntent().getStringExtra("requestId");
        ownerId = getIntent().getStringExtra("ownerId");
        roomId = getIntent().getStringExtra("roomId");
        roomTitle = getIntent().getStringExtra("roomTitle");
        amount = getIntent().getDoubleExtra("amount", 0);

        if (roomTitle == null) {
            roomTitle = "Rental property";
        }

        tvPaymentRoom.setText("Property: " + roomTitle);
        tvPaymentAmount.setText(
                "Monthly rent: ₹" +
                        String.format(Locale.getDefault(), "%.2f", amount)
        );

        Date today = new Date();

        etPaymentDate.setText(
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(today)
        );

        etRentMonth.setText(
                new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                        .format(today)
        );

        String[] methods = {
                "Cash",
                "UPI paid directly to owner",
                "Bank transfer"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                methods
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerMethod.setAdapter(adapter);

        btnSubmitPayment.setOnClickListener(v -> submitPayment());
    }

    private void submitPayment() {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please sign in first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String tenantId = auth.getCurrentUser().getUid();
        String rentMonth = etRentMonth.getText().toString().trim();
        String paymentDate = etPaymentDate.getText().toString().trim();
        String reference = etReference.getText().toString().trim();
        String selectedMethod =
                spinnerMethod.getSelectedItem().toString();

        if (requestId == null || ownerId == null || roomId == null
                || amount <= 0) {
            Toast.makeText(
                    this,
                    "Rental details are missing.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!rentMonth.matches("\\d{4}-\\d{2}")
                || !paymentDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(
                    this,
                    "Enter the month as yyyy-MM and date as yyyy-MM-dd.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        btnSubmitPayment.setEnabled(false);

        db.collection("rentalRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()
                            || !tenantId.equals(
                            document.getString("tenantId"))
                            || !ownerId.equals(
                            document.getString("ownerId"))
                            || !roomId.equals(
                            document.getString("roomId"))
                            || !"active".equalsIgnoreCase(
                            document.getString("status"))) {

                        showError(
                                "Your active rental could not be verified."
                        );
                        return;
                    }

                    Object rentObject = document.get("rent");

                    if (!(rentObject instanceof Number)
                            || Math.abs(
                            ((Number) rentObject).doubleValue()
                                    - amount
                    ) > 0.001) {

                        showError(
                                "Rent details have changed. Reopen My Rent."
                        );
                        return;
                    }

                    // Do not create a second payment record for the
                    // same rental and month if one already exists.
                    checkExistingPayment(
                            tenantId,
                            rentMonth,
                            paymentDate,
                            reference,
                            selectedMethod
                    );
                })
                .addOnFailureListener(e ->
                        showError(
                                "Could not verify rental: " + e.getMessage()
                        )
                );
    }

    private void checkExistingPayment(
            String tenantId,
            String rentMonth,
            String paymentDate,
            String reference,
            String selectedMethod) {

        db.collection("payments")
                .whereEqualTo("requestId", requestId)
                .whereEqualTo("rentMonth", rentMonth)
                .get()
                .addOnSuccessListener(query -> {

                    if (!query.isEmpty()) {
                        showError(
                                "A payment record already exists for "
                                        + rentMonth
                                        + ". Check Payment History."
                        );
                        return;
                    }

                    savePayment(
                            tenantId,
                            rentMonth,
                            paymentDate,
                            reference,
                            selectedMethod
                    );
                })
                .addOnFailureListener(e ->
                        showError(
                                "Could not check existing payments: "
                                        + e.getMessage()
                        )
                );
    }

    private void savePayment(
            String tenantId,
            String rentMonth,
            String paymentDate,
            String reference,
            String selectedMethod) {

        String paymentId =
                db.collection("payments").document().getId();

        long now = System.currentTimeMillis();

        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentId", paymentId);
        payment.put("tenantId", tenantId);
        payment.put("ownerId", ownerId);
        payment.put("roomId", roomId);
        payment.put("requestId", requestId);
        payment.put("roomTitle", roomTitle);
        payment.put("amount", amount);
        payment.put("rentMonth", rentMonth);
        payment.put("paymentDate", paymentDate);
        payment.put("method", selectedMethod);
        payment.put("transactionReference", reference);
        payment.put("status", "pending_verification");
        payment.put("createdAt", now);

        // Create the payment and its in-app notifications together.
        WriteBatch batch = db.batch();

        batch.set(
                db.collection("payments").document(paymentId),
                payment
        );

        // Notification for the tenant.
        String tenantNotificationId =
                db.collection("notifications").document().getId();

        Map<String, Object> tenantNotification = new HashMap<>();
        tenantNotification.put("recipientId", tenantId);
        tenantNotification.put("userId", tenantId);
        tenantNotification.put("title", "Payment submitted");
        tenantNotification.put(
                "message",
                "Your rent payment of ₹"
                        + String.format(Locale.getDefault(), "%.2f", amount)
                        + " for " + rentMonth
                        + " (" + roomTitle
                        + ") is awaiting owner verification."
        );
        tenantNotification.put("type", "payment_submitted");
        tenantNotification.put("paymentId", paymentId);
        tenantNotification.put("requestId", requestId);
        tenantNotification.put("roomId", roomId);
        tenantNotification.put("rentMonth", rentMonth);
        tenantNotification.put("read", false);
        tenantNotification.put("createdAt", now);

        batch.set(
                db.collection("notifications")
                        .document(tenantNotificationId),
                tenantNotification
        );

        // Notification for the owner.
        String ownerNotificationId =
                db.collection("notifications").document().getId();

        Map<String, Object> ownerNotification = new HashMap<>();
        ownerNotification.put("recipientId", ownerId);
        ownerNotification.put("userId", ownerId);
        ownerNotification.put("title", "Payment needs verification");
        ownerNotification.put(
                "message",
                "A tenant submitted ₹"
                        + String.format(Locale.getDefault(), "%.2f", amount)
                        + " for " + roomTitle
                        + " (" + rentMonth
                        + "). Please review the payment."
        );
        ownerNotification.put("type", "payment_submitted");
        ownerNotification.put("paymentId", paymentId);
        ownerNotification.put("requestId", requestId);
        ownerNotification.put("roomId", roomId);
        ownerNotification.put("rentMonth", rentMonth);
        ownerNotification.put("read", false);
        ownerNotification.put("createdAt", now);

        batch.set(
                db.collection("notifications")
                        .document(ownerNotificationId),
                ownerNotification
        );

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            this,
                            "Payment submitted for owner verification.",
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitPayment.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Payment could not be saved: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void showError(String message) {
        btnSubmitPayment.setEnabled(true);

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
