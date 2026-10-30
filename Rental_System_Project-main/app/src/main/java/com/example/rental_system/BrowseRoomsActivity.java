
package com.example.rental_system;

import java.util.Collections;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BrowseRoomsActivity extends AppCompatActivity {

    private EditText etRoomSearch;
    private TextView tvRoomCount;
    private LinearLayout roomListContainer;
    private LinearLayout layoutFilter;

    private FirebaseFirestore firestore;
    private ListenerRegistration roomListener;

    private final List<RoomItem> allRooms = new ArrayList<>();

    private String selectedRoomType = "All";
    private String selectedLocation = "All";
    private double maxRent = 0;

    private final String[] roomTypes = {
            "All", "Single Room", "Shared Room", "1 BHK"
    };

    private final String[] locations = {
            "All", "Panjim", "Margao", "Vasco"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_rooms);

        etRoomSearch = findViewById(R.id.etRoomSearch);
        tvRoomCount = findViewById(R.id.tvRoomCount);
        layoutFilter = findViewById(R.id.layoutFilter);

        roomListContainer = findViewById(R.id.roomListContainer);

        firestore = FirebaseFirestore.getInstance();

        Log.d("ROOM_DEBUG",
                "Firebase app name: "
                        + FirebaseApp.getInstance().getName());

        Log.d("ROOM_DEBUG",
                "Firebase project: "
                        + FirebaseApp.getInstance()
                        .getOptions().getProjectId());

        Log.d("ROOM_DEBUG",
                "Firebase app ID: "
                        + FirebaseApp.getInstance()
                        .getOptions().getApplicationId());

        // Load room listings from Firebase.
        loadRooms();

        // Search rooms by title or address.
        etRoomSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                displayRooms();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Open the filter dialog.
        layoutFilter.setOnClickListener(v -> showFilterDialog());

        // Bottom navigation.
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(
                    BrowseRoomsActivity.this,
                    TenantHomeActivity.class
            ));
        });

        findViewById(R.id.navBrowse).setOnClickListener(v -> {
            // Already on Browse Rooms.
        });

        findViewById(R.id.navRequests).setOnClickListener(v -> {
            startActivity(new Intent(
                    BrowseRoomsActivity.this,
                    MyRequestsActivity.class
            ));
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(
                    BrowseRoomsActivity.this,
                    ProfileActivity.class
            ));
        });
    }


    private void loadRooms() {
        FirebaseFirestore.getInstance()
                .collection("rooms")
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("ROOM_DEBUG", "Read failed",
                                task.getException());
                        Toast.makeText(
                                this,
                                "Firebase error: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    allRooms.clear();

                    for (DocumentSnapshot doc :
                            task.getResult().getDocuments()) {

                        Log.d("ROOM_DEBUG",
                                "Document: " + doc.getId()
                                        + " Data: " + doc.getData());

                        String title = doc.getString("title");
                        String address = doc.getString("address");
                        String ownerId = doc.getString("ownerId");

                        if (title == null || address == null) {
                            continue;
                        }

                        String description = doc.getString("description");
                        Double rent = doc.getDouble("rent");
                        Boolean available = doc.getBoolean("available");

                        // Read the photos saved by the owner as Base64 strings.
                        ArrayList<String> imageBase64 = new ArrayList<>();

                        Object imagesObject = doc.get("imageBase64");

                        if (imagesObject instanceof List<?>) {
                            for (Object image : (List<?>) imagesObject) {
                                if (image instanceof String) {
                                    imageBase64.add((String) image);
                                }
                            }
                        }

                        allRooms.add(new RoomItem(
                                doc.getId(),
                                ownerId == null ? "" : ownerId,
                                title,
                                address,
                                description == null ? "" : description,
                                rent == null ? 0 : rent,
                                available != null && available,
                                imageBase64
                        ));
                    }

                    Log.d("ROOM_DEBUG",
                            "Total rooms loaded: " + allRooms.size());

                    displayRooms();
                });
    }


    private void displayRooms() {

        roomListContainer.removeAllViews();

        String searchText = etRoomSearch.getText()
                .toString().trim().toLowerCase(Locale.ROOT);

        int displayedCount = 0;

        for (RoomItem room : allRooms) {

            boolean matchesSearch =
                    room.title.toLowerCase(Locale.ROOT).contains(searchText)
                            || room.address.toLowerCase(Locale.ROOT).contains(searchText);

            boolean matchesType =
                    selectedRoomType.equals("All")
                            || room.title.equalsIgnoreCase(selectedRoomType);

            boolean matchesLocation =
                    selectedLocation.equals("All")
                            || room.address.toLowerCase(Locale.ROOT)
                            .contains(selectedLocation.toLowerCase(Locale.ROOT));

            boolean matchesRent =
                    maxRent <= 0 || room.rent <= maxRent;

            if (matchesSearch && matchesType
                    && matchesLocation && matchesRent) {

                addRoomCard(room);
                displayedCount++;
            }
        }

        tvRoomCount.setText(displayedCount
                + (displayedCount == 1 ? " Room" : " Rooms"));

        if (displayedCount == 0) {

            TextView emptyMessage = new TextView(this);
            emptyMessage.setText(
                    "No available rooms found.\nPlease try another search or filter."
            );
            emptyMessage.setTextSize(16);
            emptyMessage.setTextColor(Color.DKGRAY);
            emptyMessage.setGravity(Gravity.CENTER);
            emptyMessage.setPadding(20, 40, 20, 40);

            roomListContainer.addView(emptyMessage);
        }
    }

    private void addRoomCard(RoomItem room) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(16);
        background.setStroke(1, Color.rgb(230, 230, 230));
        card.setBackground(background);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = 24;
        card.setLayoutParams(cardParams);

        TextView title = new TextView(this);
        title.setText(room.title);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(34, 34, 34));
        card.addView(title);

        TextView address = new TextView(this);
        address.setText("📍 " + room.address);
        address.setTextSize(14);
        address.setTextColor(Color.DKGRAY);

        LinearLayout.LayoutParams addressParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        addressParams.topMargin = 8;
        card.addView(address, addressParams);

        TextView rent = new TextView(this);
        rent.setText("₹" + NumberFormat.getNumberInstance(
                Locale.forLanguageTag("en-IN")
        ).format(room.rent) + " / month");
        rent.setTextSize(16);
        rent.setTypeface(null, Typeface.BOLD);
        rent.setTextColor(Color.rgb(25, 118, 210));

        LinearLayout.LayoutParams rentParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        rentParams.topMargin = 10;
        card.addView(rent, rentParams);

        if (!room.description.isEmpty()) {
            TextView description = new TextView(this);
            description.setText(room.description);
            description.setTextSize(13);
            description.setTextColor(Color.DKGRAY);

            LinearLayout.LayoutParams descriptionParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
            descriptionParams.topMargin = 8;
            card.addView(description, descriptionParams);
        }

        Button detailsButton = new Button(this);
        detailsButton.setText("View Details");

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        buttonParams.topMargin = 12;
        card.addView(detailsButton, buttonParams);

        detailsButton.setOnClickListener(v -> openRoomDetails(room));

        roomListContainer.addView(card);
    }

    private void openRoomDetails(RoomItem room) {

        Intent intent = new Intent(
                BrowseRoomsActivity.this,
                RoomDetailsActivity.class
        );

        intent.putExtra("roomId", room.roomId);
        intent.putExtra("ownerId", room.ownerId);
        intent.putExtra("roomType", room.title);
        intent.putExtra("location", room.address);
        intent.putExtra("rent", room.rent);
        intent.putExtra("available", room.available);
        intent.putExtra("description", room.description);
        intent.putStringArrayListExtra("imageBase64", room.imageBase64);

        startActivity(intent);
    }

    private void showFilterDialog() {

        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_room_filter, null
        );

        Spinner spinnerRoomType =
                dialogView.findViewById(R.id.spinnerRoomType);

        Spinner spinnerLocation =
                dialogView.findViewById(R.id.spinnerLocation);

        EditText etMaxRent =
                dialogView.findViewById(R.id.etMaxRent);

        Button btnClearFilter =
                dialogView.findViewById(R.id.btnClearFilter);

        Button btnApplyFilter =
                dialogView.findViewById(R.id.btnApplyFilter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                roomTypes
        );
        typeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerRoomType.setAdapter(typeAdapter);

        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                locations
        );
        locationAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerLocation.setAdapter(locationAdapter);

        spinnerRoomType.setSelection(findOption(
                roomTypes, selectedRoomType
        ));
        spinnerLocation.setSelection(findOption(
                locations, selectedLocation
        ));

        if (maxRent > 0) {
            etMaxRent.setText(String.valueOf(maxRent));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Filter Rooms")
                .setView(dialogView)
                .create();

        btnClearFilter.setOnClickListener(v -> {
            selectedRoomType = "All";
            selectedLocation = "All";
            maxRent = 0;

            etRoomSearch.setText("");
            displayRooms();
            dialog.dismiss();
        });

        btnApplyFilter.setOnClickListener(v -> {

            selectedRoomType =
                    spinnerRoomType.getSelectedItem().toString();

            selectedLocation =
                    spinnerLocation.getSelectedItem().toString();

            String rentText = etMaxRent.getText()
                    .toString().trim();

            if (rentText.isEmpty()) {
                maxRent = 0;
            } else {
                try {
                    maxRent = Double.parseDouble(rentText);

                    if (maxRent < 0) {
                        Toast.makeText(
                                this,
                                "Maximum rent cannot be negative",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    etMaxRent.setError("Enter a valid rent");
                    return;
                }
            }

            displayRooms();
            dialog.dismiss();
        });

        dialog.show();
    }

    private int findOption(String[] options, String value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (roomListener != null) {
            roomListener.remove();
        }
    }

    // Holds the room information received from Firestore.
    private static class RoomItem {

        String roomId;
        String ownerId;
        String title;
        String address;
        String description;
        double rent;
        boolean available;
        ArrayList<String> imageBase64;

        RoomItem(
                String roomId,
                String ownerId,
                String title,
                String address,
                String description,
                double rent,
                boolean available,
                ArrayList<String> imageBase64
        ) {
            this.roomId = roomId;
            this.ownerId = ownerId;
            this.title = title;
            this.address = address;
            this.description = description;
            this.rent = rent;
            this.available = available;
            this.imageBase64 = imageBase64;
        }
    }
}
