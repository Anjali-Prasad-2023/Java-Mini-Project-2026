
package com.example.rental_system;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActiveRentalActivity extends AppCompatActivity {

    private static final String TAG = "ActiveRental";

    private LinearLayout rentalCardsContainer;
    private TextView tvNoRental;

    private FirebaseFirestore db;
    private ListenerRegistration rentalListener;

    private final ExecutorService imageExecutor =
            Executors.newFixedThreadPool(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_rental);

        db = FirebaseFirestore.getInstance();

        rentalCardsContainer = findViewById(R.id.rentalCardsContainer);
        tvNoRental = findViewById(R.id.tvNoRental);

        findViewById(R.id.navHome).setOnClickListener(v ->
                openScreen(TenantHomeActivity.class));

        findViewById(R.id.navBrowse).setOnClickListener(v ->
                openScreen(BrowseRoomsActivity.class));

        findViewById(R.id.navRequests).setOnClickListener(v ->
                openScreen(MyRequestsActivity.class));

        findViewById(R.id.navProfile).setOnClickListener(v ->
                openScreen(ProfileActivity.class));
    }

    private void openScreen(Class<?> screen) {
        startActivity(new Intent(this, screen));
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadActiveRentals();
    }

    private void loadActiveRentals() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            showEmpty("Please log in to view your rentals.");
            return;
        }

        if (rentalListener != null) {
            rentalListener.remove();
        }

        showEmpty("Loading your rental details...");

        rentalListener = db.collection("rentalRequests")
                .whereEqualTo("tenantId", user.getUid())
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Failed to load rentals", error);
                        showEmpty("Unable to load your rentals.");
                        return;
                    }

                    rentalCardsContainer.removeAllViews();

                    if (snapshots == null || snapshots.isEmpty()) {
                        showEmpty("You do not have any rental requests yet.");
                        return;
                    }

                    int activeCount = 0;

                    for (DocumentSnapshot rental : snapshots.getDocuments()) {
                        String status = rental.getString("status");

                        if (status != null
                                && status.trim().equalsIgnoreCase("active")) {
                            addRentalCard(rental);
                            activeCount++;
                        }
                    }

                    if (activeCount == 0) {
                        showEmpty("You do not have an active rental yet.");
                    } else {
                        tvNoRental.setVisibility(View.GONE);
                        rentalCardsContainer.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showEmpty(String message) {
        rentalCardsContainer.removeAllViews();
        rentalCardsContainer.setVisibility(View.GONE);
        tvNoRental.setText(message);
        tvNoRental.setVisibility(View.VISIBLE);
    }

    private void addRentalCard(DocumentSnapshot rental) {
        String requestId = rental.getId();

        String savedRequestId = rental.getString("requestId");
        if (savedRequestId != null && !savedRequestId.trim().isEmpty()) {
            requestId = savedRequestId;
        }

        String ownerId = rental.getString("ownerId");
        String roomId = rental.getString("roomId");

        String roomTitle = rental.getString("roomTitle");
        if (roomTitle == null || roomTitle.trim().isEmpty()) {
            roomTitle = "Your Rental";
        }

        String location = rental.getString("location");
        if (location == null || location.trim().isEmpty()) {
            location = "Location not specified";
        }

        Object rentValue = rental.get("rent");
        double rent = rentValue instanceof Number
                ? ((Number) rentValue).doubleValue() : 0.0;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundColor(0xFFFFFFFF);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        cardParams.bottomMargin = dp(20);
        card.setLayoutParams(cardParams);

        ImageView roomImage = new ImageView(this);
        roomImage.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(190)));
        roomImage.setBackgroundColor(0xFFE8EDF3);
        roomImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        roomImage.setContentDescription("Room image: " + roomTitle);
        roomImage.setImageResource(android.R.drawable.ic_menu_gallery);
        card.addView(roomImage);

        TextView titleView = createText(
                roomTitle, 21, true, 0xFF000000);
        titleView.setPadding(0, dp(16), 0, dp(6));
        card.addView(titleView);

        card.addView(createText(
                "Location: " + location, 15, false, 0xFF333333));

        card.addView(createText(
                "Rental Request ID: " + requestId,
                12, false, 0xFF555555));

        TextView rentView = createText(
                String.format(Locale.getDefault(), "₹%.2f/month", rent),
                24, true, 0xFF1769AA);
        rentView.setPadding(0, dp(10), 0, dp(10));
        card.addView(rentView);

        card.addView(createText(
                "Status: Active", 14, true, 0xFF167447));

        Button payButton = new Button(this);
        payButton.setText("Pay Rent");
        payButton.setAllCaps(false);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(52));

        buttonParams.topMargin = dp(14);
        payButton.setLayoutParams(buttonParams);

        boolean detailsReady = requestId != null
                && !requestId.trim().isEmpty()
                && ownerId != null
                && !ownerId.trim().isEmpty()
                && roomId != null
                && !roomId.trim().isEmpty()
                && rent > 0;

        payButton.setEnabled(detailsReady);

        if (!detailsReady) {
            payButton.setText("Payment Details Unavailable");
        } else {
            final String selectedRequestId = requestId;
            final String selectedOwnerId = ownerId;
            final String selectedRoomId = roomId;
            final String selectedRoomTitle = roomTitle;
            final double selectedRent = rent;

            payButton.setOnClickListener(v ->
                    openPaymentScreen(
                            selectedRequestId,
                            selectedOwnerId,
                            selectedRoomId,
                            selectedRoomTitle,
                            selectedRent));
        }

        card.addView(payButton);
        rentalCardsContainer.addView(card);

        if (roomId != null && !roomId.trim().isEmpty()) {
            loadRoomImage(roomId, roomImage);
        }
    }

    private TextView createText(
            String text, int sizeSp, boolean bold, int color) {

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(sizeSp);
        textView.setTextColor(color);

        if (bold) {
            textView.setTypeface(
                    null, android.graphics.Typeface.BOLD);
        }

        return textView;
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    private void openPaymentScreen(
            String requestId,
            String ownerId,
            String roomId,
            String roomTitle,
            double amount) {

        Intent intent = new Intent(
                ActiveRentalActivity.this,
                RecordPaymentActivity.class);

        intent.putExtra("requestId", requestId);
        intent.putExtra("ownerId", ownerId);
        intent.putExtra("roomId", roomId);
        intent.putExtra("roomTitle", roomTitle);
        intent.putExtra("amount", amount);

        startActivity(intent);
    }

    private void loadRoomImage(String roomId, ImageView imageView) {
        db.collection("rooms")
                .document(roomId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;

                    Bitmap bitmap = decodeImageObject(
                            document.get("imageBase64"));

                    if (bitmap == null) {
                        bitmap = decodeImageObject(
                                document.get("encodedImages"));
                    }

                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        return;
                    }

                    Object imageUrl = document.get("imageUrl");

                    if (imageUrl instanceof String
                            && !((String) imageUrl).trim().isEmpty()) {
                        loadImageFromUrl((String) imageUrl, imageView);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG,
                                "Could not load room image: " + roomId, e));
    }

    private Bitmap decodeImageObject(Object object) {
        if (object == null) return null;

        if (object instanceof String) {
            String data = ((String) object).trim();
            if (data.isEmpty()) return null;

            try {
                if (data.contains(",")) {
                    data = data.substring(data.indexOf(",") + 1);
                }

                byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(
                        bytes, 0, bytes.length);
            } catch (Exception e) {
                Log.e(TAG, "Could not decode room image", e);
                return null;
            }
        }

        if (object instanceof List<?>) {
            for (Object item : (List<?>) object) {
                Bitmap bitmap = decodeImageObject(item);
                if (bitmap != null) return bitmap;
            }
        }

        if (object instanceof Map<?, ?>) {
            for (Object value : ((Map<?, ?>) object).values()) {
                Bitmap bitmap = decodeImageObject(value);
                if (bitmap != null) return bitmap;
            }
        }

        return null;
    }

    private void loadImageFromUrl(
            String imageUrl, ImageView imageView) {

        imageExecutor.execute(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;

            try {
                URL url = new URL(imageUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode()
                        == HttpURLConnection.HTTP_OK) {
                    try (InputStream input = connection.getInputStream()) {
                        bitmap = BitmapFactory.decodeStream(input);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not download room image", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            Bitmap result = bitmap;

            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed() && result != null) {
                    imageView.setImageBitmap(result);
                }
            });
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (rentalListener != null) {
            rentalListener.remove();
            rentalListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageExecutor.shutdownNow();
    }
}
