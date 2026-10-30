
package com.example.rental_system.owner;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.example.rental_system.R;
import com.example.rental_system.model.RentalRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class ManageRentalActivity extends BaseActivity {

    private ListView listView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ListenerRegistration rentalListener;

    private final List<RentalRequest> rentals = new ArrayList<>();
    private final List<String> displayList = new ArrayList<>();

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_rental);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        listView = findViewById(R.id.listActiveRentals);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayList
        );

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < rentals.size()) {
                terminateRental(rentals.get(position));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadActiveRentals();
    }

    private void loadActiveRentals() {

        if (auth.getCurrentUser() == null) {
            showMessage("Please sign in again.");
            finish();
            return;
        }

        String ownerId = auth.getCurrentUser().getUid();

        rentalListener = db.collection("rentalRequests")
                .whereEqualTo("ownerId", ownerId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        showMessage(
                                "Could not load rentals: "
                                        + error.getLocalizedMessage()
                        );
                        return;
                    }

                    rentals.clear();
                    displayList.clear();

                    if (snapshot != null) {
                        for (DocumentSnapshot document
                                : snapshot.getDocuments()) {

                            String status = document.getString("status");

                            // Only show currently active rentals.
                            if (status == null
                                    || !status.equalsIgnoreCase("active")) {
                                continue;
                            }

                            RentalRequest rental =
                                    document.toObject(RentalRequest.class);

                            if (rental == null) {
                                continue;
                            }

                            // Use the real Firestore document ID.
                            rental.setRequestId(document.getId());
                            rentals.add(rental);

                            String roomTitle = rental.getRoomTitle();
                            String tenantName = rental.getTenantName();
                            String location = rental.getLocation();

                            displayList.add(
                                    "Property: "
                                            + safeText(roomTitle)
                                            + "\nTenant: "
                                            + safeText(tenantName)
                                            + "\nLocation: "
                                            + safeText(location)
                                            + "\nMonthly Rent: ₹"
                                            + rental.getRent()
                                            + "\nStatus: ACTIVE"
                                            + "\n\nTap to end this rental"
                            );
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (rentals.isEmpty()) {
                        displayList.add("No active rentals found.");
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void terminateRental(RentalRequest rental) {

        new AlertDialog.Builder(this)
                .setTitle("End Rental")
                .setMessage(
                        "End the rental for "
                                + safeText(rental.getTenantName())
                                + "?"
                )
                .setPositiveButton("Yes", (dialog, which) ->
                        completeRental(rental))
                .setNegativeButton("No", null)
                .show();
    }

    private void completeRental(RentalRequest rental) {

        String requestId = rental.getRequestId();
        String roomId = rental.getRoomId();

        if (requestId == null || requestId.isEmpty()
                || roomId == null || roomId.isEmpty()) {
            showMessage("Request ID or room ID is missing.");
            return;
        }

        WriteBatch batch = db.batch();

        batch.update(
                db.collection("rentalRequests").document(requestId),
                "status",
                "completed"
        );

        batch.update(
                db.collection("rooms").document(roomId),
                "available",
                true
        );

        batch.commit()
                .addOnSuccessListener(unused -> {
                    showMessage("Rental completed successfully.");
                })
                .addOnFailureListener(e ->
                        showMessage(
                                "Could not complete rental: "
                                        + e.getLocalizedMessage()
                        ));
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty()
                ? "Not provided"
                : value;
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (rentalListener != null) {
            rentalListener.remove();
            rentalListener = null;
        }
    }
}
