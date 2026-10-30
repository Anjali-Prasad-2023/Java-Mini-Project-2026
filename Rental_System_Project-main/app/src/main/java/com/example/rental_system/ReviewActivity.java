
package com.example.rental_system;

import android.os.Bundle;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
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

public class ReviewActivity extends AppCompatActivity {

    private EditText etReviewRoomId, etReviewComment;
    private RatingBar ratingBar;
    private Button btnSubmitReview;
    private LinearLayout layoutReviews;
    private TextView tvNoReviews;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String activeRoomId;
    private String activeOwnerId;
    private String activeRoomTitle;
    private String activeLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etReviewRoomId = findViewById(R.id.etReviewRoomId);
        etReviewComment = findViewById(R.id.etReviewComment);
        ratingBar = findViewById(R.id.ratingBar);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        layoutReviews = findViewById(R.id.layoutReviews);
        tvNoReviews = findViewById(R.id.tvNoReviews);

        // The room ID is supplied automatically.
        etReviewRoomId.setFocusable(false);
        etReviewRoomId.setClickable(false);
        etReviewRoomId.setLongClickable(false);
        etReviewRoomId.setHint("Your active room ID");

        btnSubmitReview.setEnabled(false);

        loadActiveRental();
        loadReviews();

        btnSubmitReview.setOnClickListener(v -> submitReview());
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
                        etReviewRoomId.setText("");
                        btnSubmitReview.setEnabled(false);

                        tvNoReviews.setText(
                                "You need an active rental to submit a review."
                        );
                        tvNoReviews.setVisibility(TextView.VISIBLE);
                        return;
                    }

                    DocumentSnapshot rental =
                            snapshot.getDocuments().get(0);

                    activeRoomId = rental.getString("roomId");
                    activeOwnerId = rental.getString("ownerId");
                    activeRoomTitle = rental.getString("roomTitle");
                    activeLocation = rental.getString("location");

                    if (activeRoomId == null || activeOwnerId == null) {
                        btnSubmitReview.setEnabled(false);
                        showMessage(
                                "Room or owner details are missing from your active rental."
                        );
                        return;
                    }

                    etReviewRoomId.setText(activeRoomId);
                    btnSubmitReview.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    btnSubmitReview.setEnabled(false);
                    showMessage(
                            "Could not load your active rental: "
                                    + e.getMessage()
                    );
                });
    }

    private void submitReview() {
        if (auth.getCurrentUser() == null) {
            showMessage("Please log in first.");
            return;
        }

        String comment = etReviewComment.getText().toString().trim();
        float rating = ratingBar.getRating();

        if (comment.isEmpty()) {
            etReviewComment.setError("Write your review");
            return;
        }

        if (rating == 0) {
            showMessage("Please select a rating.");
            return;
        }

        if (activeRoomId == null || activeOwnerId == null) {
            showMessage("No active rental found. Cannot submit review.");
            loadActiveRental();
            return;
        }

        String tenantId = auth.getCurrentUser().getUid();

        btnSubmitReview.setEnabled(false);

        String reviewId = db.collection("reviews").document().getId();

        String date = new SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
        ).format(new Date());

        Map<String, Object> review = new HashMap<>();
        review.put("reviewId", reviewId);
        review.put("tenantId", tenantId);
        review.put("ownerId", activeOwnerId);
        review.put("roomId", activeRoomId);
        review.put("roomTitle", activeRoomTitle);
        review.put("location", activeLocation);
        review.put("rating", (int) rating);
        review.put("comment", comment);
        review.put("date", date);
        review.put("createdAt", System.currentTimeMillis());

        db.collection("reviews")
                .document(reviewId)
                .set(review)
                .addOnSuccessListener(unused -> {
                    btnSubmitReview.setEnabled(true);
                    etReviewComment.setText("");
                    ratingBar.setRating(5);

                    showMessage("Review submitted successfully.");
                    loadReviews();
                })
                .addOnFailureListener(e -> {
                    btnSubmitReview.setEnabled(true);
                    showMessage(
                            "Could not submit review: " + e.getMessage()
                    );
                });
    }

    private void loadReviews() {
        if (auth.getCurrentUser() == null) {
            tvNoReviews.setText("Please log in to view reviews.");
            tvNoReviews.setVisibility(TextView.VISIBLE);
            return;
        }

        String tenantId = auth.getCurrentUser().getUid();

        db.collection("reviews")
                .whereEqualTo("tenantId", tenantId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    layoutReviews.removeAllViews();

                    if (snapshot.isEmpty()) {
                        tvNoReviews.setText("No reviews submitted yet.");
                        tvNoReviews.setVisibility(TextView.VISIBLE);
                        return;
                    }

                    tvNoReviews.setVisibility(TextView.GONE);

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        addReviewCard(
                                doc.getString("roomId"),
                                doc.get("rating"),
                                doc.getString("comment"),
                                doc.getString("date")
                        );
                    }
                })
                .addOnFailureListener(e ->
                        showMessage(
                                "Could not load reviews: " + e.getMessage()
                        )
                );
    }

    private void addReviewCard(
            String roomId,
            Object rating,
            String comment,
            String date
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

        addText(
                content,
                "Room ID: " + safe(roomId, "Not available"),
                16,
                true
        );

        addText(content, "Rating: " + rating + " / 5", 16, true);
        addText(content, comment, 15, false);
        addText(
                content,
                "Date: " + safe(date, "Not available"),
                14,
                false
        );

        card.addView(content);
        layoutReviews.addView(card);
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
            loadReviews();
        }
    }
}
