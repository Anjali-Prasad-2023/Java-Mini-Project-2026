
package com.example.rental_system;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryActivity extends AppCompatActivity {

    private LinearLayout layoutPayments;
    private TextView tvNoPayments;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration paymentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        layoutPayments = findViewById(R.id.layoutPayments);
        tvNoPayments = findViewById(R.id.tvNoPayments);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadPayments();
    }

    private void loadPayments() {
        if (auth.getCurrentUser() == null) {
            showMessage("Please sign in to view your payments.");
            return;
        }

        if (paymentListener != null) {
            paymentListener.remove();
        }

        String tenantId = auth.getCurrentUser().getUid();
        showMessage("Loading payment history...");

        paymentListener = db.collection("payments")
                .whereEqualTo("tenantId", tenantId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        showMessage("Unable to load payment history.");
                        Toast.makeText(this,
                                "Payment error: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    layoutPayments.removeAllViews();

                    if (snapshot == null || snapshot.isEmpty()) {
                        showMessage("No payment records found yet.");
                        return;
                    }

                    List<DocumentSnapshot> payments =
                            new ArrayList<>(snapshot.getDocuments());

                    Collections.sort(payments,
                            (first, second) ->
                                    Long.compare(
                                            getSortTime(second),
                                            getSortTime(first)));

                    tvNoPayments.setVisibility(View.GONE);

                    for (DocumentSnapshot document : payments) {
                        addPaymentCard(document);
                    }
                });
    }

    private long getSortTime(DocumentSnapshot document) {
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
                    // Try the next date format.
                }
            }
        }

        return 0L;
    }

    private String getText(
            DocumentSnapshot document,
            String field,
            String defaultValue) {

        String value = document.getString(field);

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    private void showMessage(String message) {
        layoutPayments.removeAllViews();
        tvNoPayments.setText(message);
        tvNoPayments.setTextColor(Color.DKGRAY);
        tvNoPayments.setVisibility(View.VISIBLE);
    }

    private void addPaymentCard(DocumentSnapshot document) {

        String paymentId = document.getId();

        String roomTitle = getText(
                document, "roomTitle", "Room details not saved");

        String month = getText(
                document, "rentMonth", "Rent month not specified");

        String date = getText(
                document, "paymentDate", "Date not specified");

        String status = getText(
                document, "status", "Pending");

        String method = getText(
                document, "method", "Not specified");

        Object amountObject = document.get("amount");
        double amount = amountObject instanceof Number
                ? ((Number) amountObject).doubleValue() : 0.0;

        CardView card = new CardView(this);
        card.setRadius(dp(12));
        card.setCardElevation(dp(4));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        cardParams.bottomMargin = dp(16);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView roomView = createText(roomTitle, 18, true, 0xFF000000);
        content.addView(roomView);

        TextView amountView = createText(
                String.format(Locale.getDefault(), "₹%.2f", amount),
                24, true, 0xFF1769AA);
        amountView.setPadding(0, dp(8), 0, dp(8));
        content.addView(amountView);

        content.addView(createText(
                "Rent Month: " + month, 15, false, 0xFF333333));

        content.addView(createText(
                "Payment Date: " + date, 15, false, 0xFF333333));

        content.addView(createText(
                "Payment Method: " + method, 15, false, 0xFF333333));

        TextView statusView = createText(
                "Status: " + status, 16, true, 0xFF333333);

        if ("paid".equalsIgnoreCase(status)
                || "confirmed".equalsIgnoreCase(status)
                || "verified".equalsIgnoreCase(status)) {
            statusView.setTextColor(Color.rgb(40, 130, 70));
        } else if ("pending".equalsIgnoreCase(status)
                || "pending_verification".equalsIgnoreCase(status)) {
            statusView.setTextColor(Color.rgb(180, 110, 0));
        } else if ("rejected".equalsIgnoreCase(status)) {
            statusView.setTextColor(Color.RED);
        }

        content.addView(statusView);

        TextView idView = createText(
                "Payment ID: " + paymentId, 12, false, 0xFF555555);
        idView.setPadding(0, dp(8), 0, 0);
        content.addView(idView);

        card.addView(content);
        layoutPayments.addView(card);
    }

    private TextView createText(
            String text, int size, boolean bold, int color) {

        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        return view;
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (paymentListener != null) {
            paymentListener.remove();
            paymentListener = null;
        }
    }
}
