
package com.example.rental_system;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RoomDetailsActivity extends AppCompatActivity {

    private TextView tvRoomType, tvLocation, tvRent, tvRoomId, tvAvailability;
    private LinearLayout roomImageContainer;
    private Button btnRequestRent;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String roomId;
    private String ownerId;
    private String roomType;
    private String location;
    private double rent;
    private boolean available;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvRoomType = findViewById(R.id.tvRoomType);
        tvLocation = findViewById(R.id.tvLocation);
        tvRent = findViewById(R.id.tvRent);
        tvRoomId = findViewById(R.id.tvRoomId);
        tvAvailability = findViewById(R.id.tvAvailability);
        roomImageContainer = findViewById(R.id.roomImageContainer);
        btnRequestRent = findViewById(R.id.btnRequestRent);

        if (tvRoomType == null || tvLocation == null || tvRent == null
                || tvRoomId == null || tvAvailability == null
                || roomImageContainer == null || btnRequestRent == null) {
            Toast.makeText(this, "Room details layout is missing required views.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent intent = getIntent();

        roomId = intent.getStringExtra("roomId");
        ownerId = intent.getStringExtra("ownerId");
        roomType = intent.getStringExtra("roomType");
        location = intent.getStringExtra("location");
        rent = intent.getDoubleExtra("rent", 0);
        available = intent.getBooleanExtra("available", false);

        tvRoomId.setText("Room ID: " + safeText(roomId));
        tvRoomType.setText(safeText(roomType));
        tvLocation.setText("Location: " + safeText(location));
        tvRent.setText("₹" + rent + " / month");

        updateAvailability();
        displayRoomImages();

        btnRequestRent.setOnClickListener(v -> submitRentalRequest());

        // If the listing did not pass complete details, fetch the room.
        if (roomId != null && !roomId.trim().isEmpty()
                && (ownerId == null || roomType == null || location == null)) {
            loadRoomFromFirestore();
        }
    }

    private void loadRoomFromFirestore() {
        db.collection("rooms").document(roomId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "Room not found.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ownerId = document.getString("ownerId");

                    String title = document.getString("title");
                    if (title == null) title = document.getString("roomType");
                    if (title != null) roomType = title;

                    String address = document.getString("address");
                    if (address == null) address = document.getString("location");
                    if (address != null) location = address;

                    Number amount = document.getDouble("rent");
                    if (amount != null) rent = amount.doubleValue();

                    Boolean isAvailable = document.getBoolean("available");
                    if (isAvailable != null) available = isAvailable;

                    tvRoomId.setText("Room ID: " + safeText(roomId));
                    tvRoomType.setText(safeText(roomType));
                    tvLocation.setText("Location: " + safeText(location));
                    tvRent.setText("₹" + rent + " / month");

                    updateAvailability();
                    displayFirestoreImages(document);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load room details.",
                                Toast.LENGTH_LONG).show());
    }

    private void updateAvailability() {
        tvAvailability.setText(available ? "Available" : "Not Available");
        btnRequestRent.setEnabled(available);
        btnRequestRent.setText(available ? "REQUEST TO RENT" : "NOT AVAILABLE");
    }

    private void submitRentalRequest() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in before requesting a room.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (roomId == null || roomId.trim().isEmpty()
                || ownerId == null || ownerId.trim().isEmpty()) {
            Toast.makeText(this, "Room or owner information is missing.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!available) {
            Toast.makeText(this, "This room is no longer available.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String tenantId = auth.getCurrentUser().getUid();
        btnRequestRent.setEnabled(false);
        btnRequestRent.setText("Submitting...");

        db.collection("users").document(tenantId).get()
                .addOnSuccessListener(document ->
                        saveRentalRequest(tenantId, getTenantName(document)))
                .addOnFailureListener(e ->
                        saveRentalRequest(tenantId, getTenantName(null)));
    }

    private String getTenantName(DocumentSnapshot document) {
        if (document != null && document.exists()) {
            String[] fields = {"name", "fullName", "tenantName", "username"};

            for (String field : fields) {
                String value = document.getString(field);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }

        if (auth.getCurrentUser() != null) {
            String name = auth.getCurrentUser().getDisplayName();
            if (name != null && !name.trim().isEmpty()) {
                return name.trim();
            }
        }

        return "Tenant";
    }

    private void saveRentalRequest(String tenantId, String tenantName) {
        String requestId = db.collection("rentalRequests").document().getId();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("roomId", roomId);
        request.put("ownerId", ownerId);
        request.put("tenantId", tenantId);
        request.put("tenantName", tenantName);
        request.put("roomTitle", safeText(roomType));
        request.put("roomType", safeText(roomType));
        request.put("location", safeText(location));
        request.put("rent", rent);
        request.put("status", "pending");
        request.put("createdAt", System.currentTimeMillis());

        db.collection("rentalRequests").document(requestId).set(request)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Rental request submitted successfully.",
                            Toast.LENGTH_SHORT).show();

                    Intent next = new Intent(this, MyRequestsActivity.class);
                    next.putExtra("requestId", requestId);
                    startActivity(next);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRequestRent.setEnabled(available);
                    btnRequestRent.setText("REQUEST TO RENT");
                    Toast.makeText(this, "Request failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void displayRoomImages() {
        ArrayList<String> images = getIntent().getStringArrayListExtra("encodedImages");

        if (images == null || images.isEmpty()) {
            images = getIntent().getStringArrayListExtra("imageBase64");
        }

        if (images == null || images.isEmpty()) {
            images = getIntent().getStringArrayListExtra("imageUrls");
        }

        roomImageContainer.removeAllViews();

        if (images == null || images.isEmpty()) {
            addPlaceholderImage();
            return;
        }

        int count = 0;
        for (String encoded : images) {
            if (encoded == null || encoded.trim().isEmpty()) continue;

            try {
                if (encoded.startsWith("data:")) {
                    int comma = encoded.indexOf(',');
                    if (comma >= 0) encoded = encoded.substring(comma + 1);
                }

                byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                if (bitmap != null) {
                    addBitmapImage(bitmap, ++count);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid Base64 image data.
            }
        }

        if (count == 0) addPlaceholderImage();
    }

    private void displayFirestoreImages(DocumentSnapshot document) {
        ArrayList<String> images = (ArrayList<String>) document.get("encodedImages");

        if (images == null || images.isEmpty()) {
            images = (ArrayList<String>) document.get("imageBase64");
        }

        if (images == null || images.isEmpty()) {
            images = (ArrayList<String>) document.get("imageUrls");
        }

        roomImageContainer.removeAllViews();

        if (images == null || images.isEmpty()) {
            String singleImage = document.getString("imageUrl");
            if (singleImage != null && singleImage.startsWith("http")) {
                addPlaceholderImage();
            } else {
                addPlaceholderImage();
            }
            return;
        }

        int count = 0;
        for (String encoded : images) {
            if (encoded == null || encoded.trim().isEmpty()) continue;

            try {
                if (encoded.startsWith("http://") || encoded.startsWith("https://")) {
                    addPlaceholderImage();
                    continue;
                }

                if (encoded.startsWith("data:")) {
                    int comma = encoded.indexOf(',');
                    if (comma >= 0) encoded = encoded.substring(comma + 1);
                }

                byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                if (bitmap != null) addBitmapImage(bitmap, ++count);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed image entries.
            }
        }

        if (count == 0 && roomImageContainer.getChildCount() == 0) {
            addPlaceholderImage();
        }
    }

    private void addBitmapImage(Bitmap bitmap, int number) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setContentDescription("Room photo " + number);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(280), ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(dpToPx(4), 0, dpToPx(12), 0);

        roomImageContainer.addView(imageView, params);
    }

    private void addPlaceholderImage() {
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(280), ViewGroup.LayoutParams.MATCH_PARENT);
        roomImageContainer.addView(imageView, params);
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "Not provided" : value;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
