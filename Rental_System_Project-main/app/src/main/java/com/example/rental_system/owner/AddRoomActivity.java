package com.example.rental_system.owner;

import com.example.rental_system.R;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import android.widget.HorizontalScrollView;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.example.rental_system.model.Room;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.ClipData;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AddRoomActivity extends BaseActivity {

    private EditText etTitle;
    private EditText etAddress;
    private EditText etDescription;
    private EditText etRent;

    private ImageView roomImage;
    private Button btnSelectImage;
    private Button btnAddRoom;
    private ProgressBar progressBar;

    private LinearLayout imagePreviewContainer;
    private HorizontalScrollView imageScrollView;

    private final List<Uri> selectedImageUris = new ArrayList<>();

    private FirebaseFirestore firestore;

    private static final int IMAGE_PICK_CODE = 100;
    private static final int MAX_IMAGES = 3;

    // Keep each compressed image below approximately 75 KB.
    private static final int MAX_IMAGE_BYTES = 75 * 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_room);

        firestore = FirebaseFirestore.getInstance();

        etTitle = findViewById(R.id.etTitle);
        etAddress = findViewById(R.id.etAddress);
        etDescription = findViewById(R.id.etDescription);
        etRent = findViewById(R.id.etRent);

        roomImage = findViewById(R.id.roomImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnAddRoom = findViewById(R.id.btnAddRoom);
        progressBar = findViewById(R.id.progressBar);

        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        imageScrollView = findViewById(R.id.imageScrollView);

        progressBar.setVisibility(ProgressBar.GONE);

        btnSelectImage.setOnClickListener(v -> selectImages());
        btnAddRoom.setOnClickListener(v -> addRoom());
    }

    // Open the gallery and allow selection of multiple images.
    private void selectImages() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        startActivityForResult(
                Intent.createChooser(intent, "Select room photos"),
                IMAGE_PICK_CODE
        );
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != IMAGE_PICK_CODE
                || resultCode != RESULT_OK
                || data == null) {
            return;
        }

        selectedImageUris.clear();

        ClipData clipData = data.getClipData();

        if (clipData != null) {
            int count = Math.min(clipData.getItemCount(), MAX_IMAGES);

            for (int i = 0; i < count; i++) {
                Uri uri = clipData.getItemAt(i).getUri();

                if (uri != null) {
                    selectedImageUris.add(uri);
                }
            }

            if (clipData.getItemCount() > MAX_IMAGES) {
                Toast.makeText(
                        this,
                        "Only the first 3 images will be used",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } else if (data.getData() != null) {
            selectedImageUris.add(data.getData());
        }

        if (!selectedImageUris.isEmpty()) {
            imagePreviewContainer.removeAllViews();

            for (int i = 0; i < selectedImageUris.size(); i++) {
                Uri imageUri = selectedImageUris.get(i);

                ImageView preview = new ImageView(this);

                preview.setImageURI(imageUri);
                preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                preview.setContentDescription("Room photo " + (i + 1));

                GradientDrawable border = new GradientDrawable();
                border.setColor(Color.TRANSPARENT);
                border.setCornerRadius(dp(12));
                border.setStroke(dp(1), Color.rgb(32, 178, 170));
                preview.setBackground(border);
                preview.setClipToOutline(true);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                dp(280),
                                dp(190)
                        );

                params.setMargins(0, 0, dp(10), 0);

                imagePreviewContainer.addView(preview, params);
            }

            imageScrollView.post(() ->
                    imageScrollView.fullScroll(View.FOCUS_LEFT)
            );

            Toast.makeText(
                    this,
                    selectedImageUris.size() + " image(s) selected",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void addRoom() {

        String title = etTitle.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String rentText = etRent.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Enter room title");
            etTitle.requestFocus();
            return;
        }

        if (address.isEmpty()) {
            etAddress.setError("Enter address");
            etAddress.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Enter description");
            etDescription.requestFocus();
            return;
        }

        if (rentText.isEmpty()) {
            etRent.setError("Enter rent");
            etRent.requestFocus();
            return;
        }

        double rent;

        try {
            rent = Double.parseDouble(rentText);

            if (!Double.isFinite(rent) || rent <= 0) {
                etRent.setError("Enter a positive rent amount");
                etRent.requestFocus();
                return;
            }

        } catch (NumberFormatException e) {
            etRent.setError("Enter a valid rent");
            etRent.requestFocus();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String ownerId = auth.getCurrentUser().getUid();

        String roomId = firestore
                .collection("rooms")
                .document()
                .getId();

        progressBar.setVisibility(ProgressBar.VISIBLE);
        btnAddRoom.setEnabled(false);
        btnSelectImage.setEnabled(false);

        // Convert selected images to compressed Base64 strings.
        List<String> encodedImages = new ArrayList<>();

        try {
            for (Uri imageUri : selectedImageUris) {
                String encodedImage = compressImageToBase64(imageUri);
                encodedImages.add(encodedImage);
            }

        } catch (IOException e) {
            resetUploadUI();

            Toast.makeText(
                    this,
                    "Could not process image: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        saveRoom(
                roomId,
                ownerId,
                title,
                address,
                description,
                rent,
                encodedImages
        );
    }

    // Resize and compress an image before encoding it.
    private String compressImageToBase64(Uri imageUri)
            throws IOException {

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        try (InputStream input =
                     getContentResolver().openInputStream(imageUri)) {

            if (input == null) {
                throw new IOException("Cannot open selected image");
            }

            BitmapFactory.decodeStream(input, null, bounds);
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Invalid image");
        }

        // Limit initial decoded dimensions to approximately 1200 pixels.
        int sampleSize = 1;

        while (bounds.outWidth / sampleSize > 1200
                || bounds.outHeight / sampleSize > 1200) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;

        Bitmap original;

        try (InputStream input =
                     getContentResolver().openInputStream(imageUri)) {

            if (input == null) {
                throw new IOException("Cannot read image");
            }

            original = BitmapFactory.decodeStream(input, null, options);
        }

        if (original == null) {
            throw new IOException("Image decoding failed");
        }

        Bitmap working = original;

        try {
            // Reduce JPEG quality first, then shrink dimensions if needed.
            for (int resizeAttempt = 0; resizeAttempt < 6;
                 resizeAttempt++) {

                for (int quality = 75; quality >= 25; quality -= 10) {

                    ByteArrayOutputStream output =
                            new ByteArrayOutputStream();

                    working.compress(
                            Bitmap.CompressFormat.JPEG,
                            quality,
                            output
                    );

                    byte[] bytes = output.toByteArray();

                    if (bytes.length <= MAX_IMAGE_BYTES) {
                        return Base64.encodeToString(
                                bytes,
                                Base64.NO_WRAP
                        );
                    }
                }

                if (working.getWidth() <= 200
                        || working.getHeight() <= 200) {
                    break;
                }

                int newWidth = Math.max(
                        1,
                        (int) (working.getWidth() * 0.75f)
                );

                int newHeight = Math.max(
                        1,
                        (int) (working.getHeight() * 0.75f)
                );

                Bitmap smaller = Bitmap.createScaledBitmap(
                        working,
                        newWidth,
                        newHeight,
                        true
                );

                if (working != original) {
                    working.recycle();
                }

                working = smaller;
            }

        } finally {
            if (working != original) {
                working.recycle();
            }

            original.recycle();
        }

        throw new IOException(
                "Image could not be compressed enough. Choose a smaller photo."
        );
    }

    private void saveRoom(
            String roomId,
            String ownerId,
            String title,
            String address,
            String description,
            double rent,
            List<String> encodedImages) {

        // Keep imageUrl empty for new Base64-based room photos.
        Room room = new Room(
                roomId,
                ownerId,
                title,
                address,
                description,
                rent,
                true,
                "",
                encodedImages
        );

        firestore.collection("rooms")
                .document(roomId)
                .set(room)
                .addOnSuccessListener(unused -> {

                    resetUploadUI();

                    Toast.makeText(
                            AddRoomActivity.this,
                            "Room added successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    resetUploadUI();

                    Toast.makeText(
                            AddRoomActivity.this,
                            "Failed to add room: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void resetUploadUI() {
        progressBar.setVisibility(ProgressBar.GONE);
        btnAddRoom.setEnabled(true);
        btnSelectImage.setEnabled(true);
    }
}