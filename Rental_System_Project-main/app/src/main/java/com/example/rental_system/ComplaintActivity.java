
package com.example.rental_system;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ComplaintActivity extends AppCompatActivity {

    private EditText etComplaintTitle, etComplaintRoomId;
    private EditText etComplaintDescription;
    private Button btnSubmitComplaint;
    private LinearLayout layoutComplaints;
    private TextView tvNoComplaints;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String activeRoomId;
    private String activeOwnerId;
    private String activeRoomTitle;
    private String activeLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etComplaintTitle = findViewById(R.id.etComplaintTitle);
        etComplaintRoomId = findViewById(R.id.etComplaintRoomId);
        etComplaintDescription =
                findViewById(R.id.etComplaintDescription);
        btnSubmitComplaint = findViewById(R.id.btnSubmitComplaint);
        layoutComplaints = findViewById(R.id.layoutComplaints);
        tvNoComplaints = findViewById(R.id.tvNoComplaints);

        // The room ID is supplied automatically.
        etComplaintRoomId.setFocusable(false);
        etComplaintRoomId.setClickable(false);
        etComplaintRoomId.setLongClickable(false);
        etComplaintRoomId.setHint("Your active room ID");

        btnSubmitComplaint.setEnabled(false);
        loadActiveRental();
        loadComplaints();

        btnSubmitComplaint.setOnClickListener(v -> submitComplaint());
    }

    private void loadActiveRental() {
        if (auth.getCurrentUser() == null) {
            showMessage("Please log in first.");
            return;
        }

        String tenantId = auth.getCurrentUser().getUid();

        db.collection("rentalRequests")
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        activeRoomId = null;
                        activeOwnerId = null;
                        etComplaintRoomId.setText("");
                        btnSubmitComplaint.setEnabled(false);

                        tvNoComplaints.setText(
                                "You need an active rental to submit a complaint."
                        );
                        tvNoComplaints.setVisibility(TextView.VISIBLE);
                        return;
                    }

                    DocumentSnapshot rental =
                            snapshot.getDocuments().get(0);

                    activeRoomId = rental.getString("roomId");
                    activeOwnerId = rental.getString("ownerId");
                    activeRoomTitle = rental.getString("roomTitle");
                    activeLocation = rental.getString("location");

                    if (activeRoomId == null || activeOwnerId == null) {
                        btnSubmitComplaint.setEnabled(false);
                        showMessage(
                                "Room or owner details are missing from your active rental."
                        );
                        return;
                    }

                    etComplaintRoomId.setText(activeRoomId);
                    btnSubmitComplaint.setEnabled(true);

                    showMessage(
                            "Active room loaded: "
                                    + safe(activeRoomTitle, activeRoomId)
                    );
                })
                .addOnFailureListener(e -> {
                    btnSubmitComplaint.setEnabled(false);
                    showMessage(
                            "Could not load your active rental: "
                                    + e.getMessage()
                    );
                });
    }

    private void submitComplaint() {
        if (auth.getCurrentUser() == null) {
            showMessage("Please log in first.");
            return;
        }

        String title = etComplaintTitle.getText().toString().trim();
        String description =
                etComplaintDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etComplaintTitle.setError("Enter complaint title");
            return;
        }

        if (description.isEmpty()) {
            etComplaintDescription.setError(
                    "Enter complaint description"
            );
            return;
        }

        if (activeRoomId == null || activeOwnerId == null) {
            showMessage("No active rental found. Cannot submit complaint.");
            loadActiveRental();
            return;
        }

        String tenantId = auth.getCurrentUser().getUid();
        btnSubmitComplaint.setEnabled(false);

        String complaintId =
                db.collection("complaints").document().getId();

        String date = new SimpleDateFormat(
                "dd/MM/yyyy", Locale.getDefault()
        ).format(new Date());

        Map<String, Object> complaint = new HashMap<>();
        complaint.put("complaintId", complaintId);
        complaint.put("tenantId", tenantId);
        complaint.put("ownerId", activeOwnerId);
        complaint.put("roomId", activeRoomId);
        complaint.put("roomTitle", activeRoomTitle);
        complaint.put("location", activeLocation);
        complaint.put("title", title);
        complaint.put("subject", title);
        complaint.put("description", description);
        complaint.put("date", date);
        complaint.put("status", "PENDING");
        complaint.put("createdAt", System.currentTimeMillis());

        db.collection("complaints")
                .document(complaintId)
                .set(complaint)
                .addOnSuccessListener(unused -> {
                    btnSubmitComplaint.setEnabled(true);
                    etComplaintTitle.setText("");
                    etComplaintDescription.setText("");

                    showMessage("Complaint submitted successfully.");
                    loadComplaints();
                })
                .addOnFailureListener(e -> {
                    btnSubmitComplaint.setEnabled(true);
                    showMessage(
                            "Could not submit complaint: " + e.getMessage()
                    );
                });
    }

    private void loadComplaints() {
        if (auth.getCurrentUser() == null) {
            tvNoComplaints.setText("Please log in to view complaints.");
            tvNoComplaints.setVisibility(TextView.VISIBLE);
            return;
        }

        String tenantId = auth.getCurrentUser().getUid();

        db.collection("complaints")
                .whereEqualTo("tenantId", tenantId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    layoutComplaints.removeAllViews();

                    if (snapshot.isEmpty()) {
                        tvNoComplaints.setText(
                                "No complaints submitted yet."
                        );
                        tvNoComplaints.setVisibility(TextView.VISIBLE);
                        return;
                    }

                    tvNoComplaints.setVisibility(TextView.GONE);

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        addComplaintCard(
                                doc.getString("title"),
                                doc.getString("roomId"),
                                doc.getString("description"),
                                doc.getString("date"),
                                doc.getString("status")
                        );
                    }
                })
                .addOnFailureListener(e ->
                        showMessage(
                                "Could not load complaints: "
                                        + e.getMessage()
                        )
                );
    }

    private void addComplaintCard(
            String title,
            String roomId,
            String description,
            String date,
            String status
    ) {
        CardView card = new CardView(this);
        card.setRadius(20);
        card.setCardElevation(6);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.setMargins(0, 8, 0, 16);
        card.setLayoutParams(params);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(25, 25, 25, 25);

        addText(content, title == null ? "Complaint" : title, 19, true);
        addText(content, "Room ID: " + safe(roomId, "Not available"), 14, false);
        addText(content, description, 15, false);
        addText(content, "Date: " + safe(date, "Not available"), 14, false);
        addText(content, "Status: " + safe(status, "PENDING"), 16, true);

        card.addView(content);
        layoutComplaints.addView(card);
    }

    private void addText(
            LinearLayout parent,
            String value,
            int size,
            boolean bold
    ) {
        TextView text = new TextView(this);
        text.setText(value == null ? "" : value);
        text.setTextSize(size);
        text.setTextColor(android.graphics.Color.BLACK);

        if (bold) {
            text.setTypeface(null, Typeface.BOLD);
        }

        text.setPadding(0, 5, 0, 5);
        parent.addView(text);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback : value;
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && auth != null) {
            loadActiveRental();
            loadComplaints();
        }
    }
}
