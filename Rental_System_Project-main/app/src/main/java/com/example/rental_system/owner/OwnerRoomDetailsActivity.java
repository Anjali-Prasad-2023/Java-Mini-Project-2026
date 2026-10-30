package com.example.rental_system.owner;

import android.os.Bundle;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.rental_system.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class OwnerRoomDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String roomId;
    private DocumentSnapshot room;

    private LinearLayout roomImageContainer;
    private TextView title, rent, location, description, status;
    private Button availabilityButton, deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_room_details);

        roomId = getIntent().getStringExtra("roomId");

        if (roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid property ID",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        roomImageContainer = findViewById(R.id.roomImageContainer);
        title = findViewById(R.id.detailTitle);
        rent = findViewById(R.id.detailRent);
        location = findViewById(R.id.detailLocation);
        description = findViewById(R.id.detailDescription);
        status = findViewById(R.id.detailStatus);
        availabilityButton = findViewById(R.id.availabilityButton);
        deleteButton = findViewById(R.id.deleteButton);

        if (roomImageContainer == null
                || title == null
                || rent == null
                || location == null
                || description == null
                || status == null
                || availabilityButton == null
                || deleteButton == null) {

            Toast.makeText(this,
                    "Missing view ID in activity_owner_room_details.xml",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        availabilityButton.setOnClickListener(v -> toggleAvailability());
        deleteButton.setOnClickListener(v -> deleteRoom());

        loadRoom();
    }

    private void loadRoom() {
        db.collection("rooms")
                .document(roomId)
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (!document.exists()) {
                        Toast.makeText(this, "Property not found",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String uid = FirebaseAuth.getInstance().getUid();

                    if (uid == null
                            || !uid.equals(document.getString("ownerId"))) {
                        Toast.makeText(this, "Access denied",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    room = document;

                    String propertyTitle = document.getString("title");
                    title.setText(
                            propertyTitle == null || propertyTitle.trim().isEmpty()
                                    ? "Untitled property"
                                    : propertyTitle
                    );

                    Object rentValue = document.get("rent");

                    if (!(rentValue instanceof Number)) {
                        rentValue = document.get("monthlyRent");
                    }

                    rent.setText(rentValue instanceof Number
                            ? "₹" + ((Number) rentValue).doubleValue() + " / month"
                            : "Rent not provided");

                    String address = document.getString("address");
                    String city = document.getString("city");
                    String fullAddress = address == null ? "" : address.trim();

                    if (city != null && !city.trim().isEmpty()
                            && !city.trim().equalsIgnoreCase(fullAddress)) {
                        if (!fullAddress.isEmpty()) {
                            fullAddress += ", ";
                        }
                        fullAddress += city.trim();
                    }

                    location.setText(fullAddress.isEmpty()
                            ? "Address not provided"
                            : fullAddress);

                    String desc = document.getString("description");
                    description.setText(
                            desc == null || desc.trim().isEmpty()
                                    ? "No description provided"
                                    : desc
                    );

                    boolean available =
                            Boolean.TRUE.equals(document.getBoolean("available"));

                    status.setText(available
                            ? "Available for rent"
                            : "Currently unavailable");

                    availabilityButton.setText(available
                            ? "Mark unavailable"
                            : "Mark available");

                    loadPropertyImages(document);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;

                    Toast.makeText(this,
                            "Failed to load property: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void loadPropertyImages(DocumentSnapshot document) {
        roomImageContainer.removeAllViews();

        Object imageObject = document.get("encodedImages");

        if (!(imageObject instanceof List)
                || ((List<?>) imageObject).isEmpty()) {
            imageObject = document.get("imageBase64");
        }

        if (imageObject instanceof List<?>) {
            for (Object item : (List<?>) imageObject) {
                if (item instanceof String) {
                    addRoomImage((String) item);
                }
            }
        }

        if (roomImageContainer.getChildCount() == 0) {
            Object urls = document.get("imageUrls");

            if (urls instanceof List<?>) {
                for (Object item : (List<?>) urls) {
                    if (item instanceof String) {
                        addRoomImage((String) item);
                    }
                }
            }
        }

        if (roomImageContainer.getChildCount() == 0) {
            String imageUrl = document.getString("imageUrl");

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                addRoomImage(imageUrl);
            }
        }

        if (roomImageContainer.getChildCount() == 0) {
            TextView noImages = new TextView(this);
            noImages.setText("No room photos available");
            noImages.setTextSize(16);
            noImages.setPadding(16, 24, 16, 24);
            roomImageContainer.addView(noImages);
        }
    }

    private void addRoomImage(String imageData) {
        if (imageData == null || imageData.trim().isEmpty()) return;

        ImageView imageView = new ImageView(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dp(260),
                dp(210)
        );

        params.setMargins(0, 0, dp(12), 0);

        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setContentDescription("Room photo");

        roomImageContainer.addView(imageView);

        if (imageData.startsWith("http://")
                || imageData.startsWith("https://")) {
            Glide.with(this)
                    .load(imageData)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(imageView);
            return;
        }

        try {
            if (imageData.startsWith("data:")) {
                int comma = imageData.indexOf(',');
                if (comma >= 0) {
                    imageData = imageData.substring(comma + 1);
                }
            }

            byte[] bytes = Base64.decode(imageData, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.length
            );

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(
                        android.R.drawable.ic_menu_gallery
                );
            }
        } catch (IllegalArgumentException e) {
            imageView.setImageResource(
                    android.R.drawable.ic_menu_gallery
            );
        }
    }

    private int dp(int value) {
        return (int) (value
                * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toggleAvailability() {
        if (room == null) return;

        boolean currentlyAvailable =
                Boolean.TRUE.equals(room.getBoolean("available"));

        availabilityButton.setEnabled(false);

        db.collection("rooms")
                .document(roomId)
                .update("available", !currentlyAvailable)
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) return;

                    Toast.makeText(this,
                            !currentlyAvailable
                                    ? "Property marked available"
                                    : "Property marked unavailable",
                            Toast.LENGTH_SHORT).show();

                    loadRoom();
                    availabilityButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;

                    availabilityButton.setEnabled(true);

                    Toast.makeText(this,
                            "Update failed: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void deleteRoom() {
        if (room == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete property?")
                .setMessage("This removes the property listing from Firestore.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteButton.setEnabled(false);

                    db.collection("rooms")
                            .document(roomId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                if (isFinishing() || isDestroyed()) return;

                                Toast.makeText(this, "Property deleted",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                if (isFinishing() || isDestroyed()) return;

                                deleteButton.setEnabled(true);

                                Toast.makeText(this,
                                        "Delete failed: " + e.getLocalizedMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .show();
    }
}
