package com.example.rental_system.owner;

import com.example.rental_system.R;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyPropertiesActivity extends BaseActivity {

    private FirebaseFirestore db;
    private LinearLayout listContainer;
    private ProgressBar progress;
    private TextView emptyText;

    // Prevent an older request from overwriting a newer result.
    private int loadRequestId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_properties);

        listContainer = findViewById(R.id.listContainer);
        progress = findViewById(R.id.progress);
        emptyText = findViewById(R.id.emptyText);
        db = FirebaseFirestore.getInstance();

        findViewById(R.id.addPropertyButton).setOnClickListener(v -> {
            startActivity(new Intent(
                    MyPropertiesActivity.this,
                    AddRoomActivity.class
            ));
        });

        // Load once when this screen is first created.
        loadProperties();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Do not load again here. onCreate() already loads the list.
        // When returning from AddRoomActivity, Android normally resumes
        // this existing activity, so refresh it only when appropriate.
    }

    private void loadProperties() {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        // Identify this request so an older response cannot overwrite
        // a newer refresh.
        final int requestId = ++loadRequestId;

        progress.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
        listContainer.removeAllViews();

        db.collection("rooms")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    // Ignore an outdated request.
                    if (requestId != loadRequestId || isFinishing()) {
                        return;
                    }

                    progress.setVisibility(View.GONE);
                    listContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        emptyText.setText(
                                "You haven't added any properties yet."
                        );
                        emptyText.setVisibility(View.VISIBLE);
                        return;
                    }

                    emptyText.setVisibility(View.GONE);

                    // Extra protection: render each Firestore document ID
                    // only once, even if duplicate results are ever supplied.
                    Set<String> displayedRoomIds = new HashSet<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (displayedRoomIds.add(doc.getId())) {
                            addPropertyCard(doc);
                        }
                    }

                })
                .addOnFailureListener(error -> {

                    if (requestId != loadRequestId || isFinishing()) {
                        return;
                    }

                    progress.setVisibility(View.GONE);

                    Toast.makeText(
                            MyPropertiesActivity.this,
                            "Failed to load properties: "
                                    + error.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void addPropertyCard(DocumentSnapshot doc) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(14, 14, 14, 14);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setElevation(3f);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 16);
        listContainer.addView(card, cardParams);

        ImageView photo = new ImageView(this);
        photo.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                190
        ));
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setImageResource(android.R.drawable.ic_menu_gallery);
        card.addView(photo);

        loadPropertyImage(doc, photo);

        String propertyTitle = doc.getString("title");

        TextView title = new TextView(this);
        title.setText(propertyTitle == null
                ? "Untitled property" : propertyTitle);
        title.setTextSize(19);
        title.setTextColor(0xFF18392B);
        title.setPadding(0, 12, 0, 5);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        Object rentObject = doc.get("rent");
        Number rentNumber = rentObject instanceof Number
                ? (Number) rentObject : null;

        TextView rentText = new TextView(this);
        rentText.setText("₹" +
                (rentNumber == null
                        ? "0"
                        : rentNumber.doubleValue())
                + " / month");
        rentText.setTextSize(17);
        rentText.setTextColor(0xFF216B50);
        card.addView(rentText);

        String addressValue = doc.getString("address");

        TextView location = new TextView(this);
        location.setText(addressValue == null
                || addressValue.trim().isEmpty()
                ? "Address not provided"
                : addressValue);
        location.setTextColor(0xFF65736B);
        location.setPadding(0, 6, 0, 6);
        card.addView(location);

        String descriptionValue = doc.getString("description");

        TextView description = new TextView(this);
        description.setText(descriptionValue == null
                || descriptionValue.trim().isEmpty()
                ? "No description provided"
                : descriptionValue);
        description.setTextSize(14);
        description.setTextColor(0xFF65736B);
        description.setPadding(0, 4, 0, 8);
        card.addView(description);

        boolean isAvailable =
                Boolean.TRUE.equals(doc.getBoolean("available"));

        TextView status = new TextView(this);
        status.setText(isAvailable ? "AVAILABLE" : "UNAVAILABLE");
        status.setTextColor(isAvailable
                ? 0xFF216B50 : 0xFFB3261E);
        status.setTypeface(null, Typeface.BOLD);
        status.setPadding(0, 6, 0, 10);
        card.addView(status);

        TextView details = new TextView(this);
        details.setText("View property details  →");
        details.setGravity(Gravity.CENTER);
        details.setTextColor(0xFFFFFFFF);
        details.setPadding(10, 14, 10, 14);
        details.setBackgroundColor(0xFF216B50);
        card.addView(details);

        View.OnClickListener openDetails = v -> {
            Intent intent = new Intent(
                    MyPropertiesActivity.this,
                    OwnerRoomDetailsActivity.class
            );
            intent.putExtra("roomId", doc.getId());
            startActivity(intent);
        };

        card.setOnClickListener(openDetails);
        details.setOnClickListener(openDetails);
    }

    private void loadPropertyImage(
            DocumentSnapshot doc,
            ImageView photo) {

        Object imageObject = doc.get("imageBase64");

        if (imageObject instanceof List<?>) {
            List<?> images = (List<?>) imageObject;

            if (!images.isEmpty() && images.get(0) instanceof String) {
                String encodedImage = (String) images.get(0);

                if (!encodedImage.isEmpty()) {
                    try {
                        byte[] imageBytes = Base64.decode(
                                encodedImage,
                                Base64.DEFAULT
                        );

                        Bitmap bitmap = BitmapFactory.decodeByteArray(
                                imageBytes,
                                0,
                                imageBytes.length
                        );

                        if (bitmap != null) {
                            photo.setImageBitmap(bitmap);
                            return;
                        }
                    } catch (IllegalArgumentException e) {
                        // Try the older image fields below.
                    }
                }
            }
        }

        String imageUrl = doc.getString("imageUrl");

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(photo);
            return;
        }

        Object oldImagesObject = doc.get("imageUrls");

        if (oldImagesObject instanceof List<?>) {
            List<?> oldImages = (List<?>) oldImagesObject;

            if (!oldImages.isEmpty()
                    && oldImages.get(0) instanceof String) {

                String oldUrl = (String) oldImages.get(0);

                if (!oldUrl.trim().isEmpty()) {
                    Glide.with(this)
                            .load(oldUrl)
                            .centerCrop()
                            .placeholder(
                                    android.R.drawable.ic_menu_gallery
                            )
                            .error(
                                    android.R.drawable.ic_menu_gallery
                            )
                            .into(photo);
                    return;
                }
            }
        }

        photo.setImageResource(android.R.drawable.ic_menu_gallery);
    }
}