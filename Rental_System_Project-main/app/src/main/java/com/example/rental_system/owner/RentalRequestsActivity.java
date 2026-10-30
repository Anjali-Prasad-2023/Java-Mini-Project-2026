package com.example.rental_system.owner;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.rental_system.R;
import com.example.rental_system.model.RentalRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RentalRequestsActivity extends BaseActivity {

    private ListView listView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final List<RentalRequest> requests = new ArrayList<>();
    private final List<String> displayList = new ArrayList<>();

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rental_request);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        listView = findViewById(R.id.listRequests);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayList
        );

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < requests.size()) {
                showRequestActions(requests.get(position));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    private void loadRequests() {
        if (auth.getCurrentUser() == null) {
            showMessage("Please sign in again");
            finish();
            return;
        }

        String ownerId = auth.getCurrentUser().getUid();

        db.collection("rentalRequests")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    requests.clear();
                    displayList.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        RentalRequest request =
                                document.toObject(RentalRequest.class);

                        if (request == null) {
                            continue;
                        }

                        request.setRequestId(document.getId());
                        requests.add(request);

                        displayList.add(
                                "Property: " + safeText(request.getRoomTitle())
                                        + "\nTenant: "
                                        + safeText(request.getTenantName())
                                        + "\nStatus: "
                                        + safeText(request.getStatus())
                                        .toUpperCase()
                        );
                    }

                    adapter.notifyDataSetChanged();

                    if (requests.isEmpty()) {
                        showMessage("No rental requests found");
                    }
                })
                .addOnFailureListener(e ->
                        showMessage("Failed to load requests: "
                                + e.getLocalizedMessage()));
    }

    private void showRequestActions(RentalRequest request) {
        String status = request.getStatus();

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Rental Request")
                .setMessage(
                        "Tenant: " + safeText(request.getTenantName())
                                + "\nProperty: "
                                + safeText(request.getRoomTitle())
                                + "\nCurrent status: "
                                + safeText(status)
                )
                .setNeutralButton("Close", null);

        if (status != null && status.equalsIgnoreCase("pending")) {
            builder.setPositiveButton(
                    "Accept",
                    (dialog, which) -> acceptRequest(request)
            );

            builder.setNegativeButton(
                    "Reject",
                    (dialog, which) -> rejectRequest(request)
            );
        }

        builder.show();
    }

    private void acceptRequest(RentalRequest request) {
        if (auth.getCurrentUser() == null) {
            showMessage("Please sign in again");
            return;
        }

        String requestId = request.getRequestId();

        if (requestId == null || requestId.trim().isEmpty()) {
            showMessage("Request ID is missing");
            return;
        }

        String currentOwnerId = auth.getCurrentUser().getUid();

        // Read the latest request before accepting it.
        db.collection("rentalRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(requestDoc -> {

                    if (!requestDoc.exists()) {
                        showMessage("Rental request no longer exists");
                        return;
                    }

                    String status = requestDoc.getString("status");
                    String ownerId = requestDoc.getString("ownerId");
                    String tenantId = requestDoc.getString("tenantId");
                    String roomId = requestDoc.getString("roomId");

                    if (status == null
                            || !status.equalsIgnoreCase("pending")) {
                        showMessage("This request has already been processed");
                        loadRequests();
                        return;
                    }

                    if (!currentOwnerId.equals(ownerId)) {
                        showMessage("You cannot accept this request");
                        return;
                    }

                    if (tenantId == null || tenantId.trim().isEmpty()
                            || roomId == null || roomId.trim().isEmpty()) {
                        showMessage("Tenant ID or room ID is missing");
                        return;
                    }

                    Object rentValue = requestDoc.get("rent");

                    double rent = rentValue instanceof Number
                            ? ((Number) rentValue).doubleValue()
                            : 0.0;

                    if (rent <= 0) {
                        showMessage("Valid monthly rent was not found");
                        return;
                    }

                    String tenantName = requestDoc.getString("tenantName");
                    String roomTitle = requestDoc.getString("roomTitle");
                    String location = requestDoc.getString("location");

                    if (tenantName == null || tenantName.trim().isEmpty()) {
                        tenantName = "Tenant";
                    }

                    if (roomTitle == null || roomTitle.trim().isEmpty()) {
                        roomTitle = "Rental property";
                    }

                    final String finalTenantName = tenantName;
                    final String finalRoomTitle = roomTitle;
                    final String finalLocation =
                            location == null ? "" : location;
                    final String finalTenantId = tenantId;
                    final String finalRoomId = roomId;
                    final String finalOwnerId = ownerId;
                    final double finalRent = rent;

                    // First try the room document ID.
                    DocumentReference roomRef =
                            db.collection("rooms").document(finalRoomId);

                    roomRef.get()
                            .addOnSuccessListener(roomDoc -> {
                                if (roomDoc.exists()) {
                                    activateRental(
                                            requestId,
                                            requestDoc,
                                            roomRef,
                                            roomDoc,
                                            finalOwnerId,
                                            finalTenantId,
                                            finalTenantName,
                                            finalRoomId,
                                            finalRoomTitle,
                                            finalLocation,
                                            finalRent
                                    );
                                } else {
                                    // Fallback: find the room by its roomId field.
                                    findRoomByField(
                                            requestId,
                                            requestDoc,
                                            finalOwnerId,
                                            finalTenantId,
                                            finalTenantName,
                                            finalRoomId,
                                            finalRoomTitle,
                                            finalLocation,
                                            finalRent
                                    );
                                }
                            })
                            .addOnFailureListener(e ->
                                    showMessage("Could not read room: "
                                            + e.getLocalizedMessage()));
                })
                .addOnFailureListener(e ->
                        showMessage("Could not read request: "
                                + e.getLocalizedMessage()));
    }

    private void findRoomByField(
            String requestId,
            DocumentSnapshot requestDoc,
            String ownerId,
            String tenantId,
            String tenantName,
            String roomId,
            String roomTitle,
            String location,
            double rent) {

        db.collection("rooms")
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("ownerId", ownerId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        showMessage(
                                "Room not found. The roomId in this request "
                                        + "does not match a room owned by you."
                        );
                        return;
                    }

                    DocumentSnapshot roomDoc =
                            snapshot.getDocuments().get(0);

                    activateRental(
                            requestId,
                            requestDoc,
                            roomDoc.getReference(),
                            roomDoc,
                            ownerId,
                            tenantId,
                            tenantName,
                            roomId,
                            roomTitle,
                            location,
                            rent
                    );
                })
                .addOnFailureListener(e ->
                        showMessage("Could not find room: "
                                + e.getLocalizedMessage()));
    }

    private void activateRental(
            String requestId,
            DocumentSnapshot requestDoc,
            DocumentReference roomRef,
            DocumentSnapshot roomDoc,
            String ownerId,
            String tenantId,
            String tenantName,
            String roomId,
            String roomTitle,
            String location,
            double rent) {

        if (!roomDoc.exists()) {
            showMessage("Room no longer exists");
            return;
        }

        String roomOwnerId = roomDoc.getString("ownerId");

        if (roomOwnerId == null || !roomOwnerId.equals(ownerId)) {
            showMessage("This room does not belong to your account");
            return;
        }

        Boolean available = roomDoc.getBoolean("available");

        if (Boolean.FALSE.equals(available)) {
            showMessage("This room is already rented or unavailable");
            return;
        }

        // Recheck the request status immediately before committing.
        db.collection("rentalRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(latestRequest -> {

                    if (!latestRequest.exists()
                            || !"pending".equalsIgnoreCase(
                            safeText(latestRequest.getString("status")))) {
                        showMessage("This request has already been processed");
                        loadRequests();
                        return;
                    }

                    long now = System.currentTimeMillis();

                    WriteBatch batch = db.batch();

                    // 1. Mark the request as active.
                    batch.update(
                            db.collection("rentalRequests").document(requestId),
                            "status", "active"
                    );

                    // 2. Automatically mark the actual room document unavailable.
                    batch.update(roomRef, "available", false);

                    // 3. Save the active rental.
                    Map<String, Object> rentalData = new HashMap<>();
                    rentalData.put("requestId", requestId);
                    rentalData.put("tenantId", tenantId);
                    rentalData.put("tenantName", tenantName);
                    rentalData.put("ownerId", ownerId);
                    rentalData.put("roomId", roomId);
                    rentalData.put("roomTitle", roomTitle);
                    rentalData.put("location", location);
                    rentalData.put("rent", rent);
                    rentalData.put("rentDueDay", 5);
                    rentalData.put("status", "active");
                    rentalData.put("createdAt", now);

                    batch.set(
                            db.collection("rentals").document(requestId),
                            rentalData
                    );

                    // 4. Notify the tenant in the app.
                    Map<String, Object> tenantNotification = new HashMap<>();
                    tenantNotification.put("userId", tenantId);
                    tenantNotification.put("recipientId", tenantId);
                    tenantNotification.put("title", "Rental Request Accepted");
                    tenantNotification.put(
                            "message",
                            "Your request for " + roomTitle
                                    + " has been accepted. Monthly rent: ₹"
                                    + rent + ". Rent is due on the 5th."
                    );
                    tenantNotification.put("type", "rental_accepted");
                    tenantNotification.put("requestId", requestId);
                    tenantNotification.put("roomId", roomId);
                    tenantNotification.put("read", false);
                    tenantNotification.put("createdAt", now);

                    batch.set(
                            db.collection("notifications")
                                    .document("tenant_accepted_" + requestId),
                            tenantNotification
                    );

                    // 5. Notify the owner in the app.
                    Map<String, Object> ownerNotification = new HashMap<>();
                    ownerNotification.put("userId", ownerId);
                    ownerNotification.put("recipientId", ownerId);
                    ownerNotification.put("title", "Rental Activated");
                    ownerNotification.put(
                            "message",
                            "The rental for " + roomTitle
                                    + " and tenant " + tenantName
                                    + " is now active."
                    );
                    ownerNotification.put("type", "rental_accepted");
                    ownerNotification.put("requestId", requestId);
                    ownerNotification.put("roomId", roomId);
                    ownerNotification.put("read", false);
                    ownerNotification.put("createdAt", now);

                    batch.set(
                            db.collection("notifications")
                                    .document("owner_accepted_" + requestId),
                            ownerNotification
                    );

                    // Save all changes together.
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                showMessage(
                                        "Rental activated! Room is now unavailable."
                                );
                                loadRequests();
                            })
                            .addOnFailureListener(e ->
                                    showMessage(
                                            "Could not activate rental: "
                                                    + e.getLocalizedMessage()
                                    ));
                })
                .addOnFailureListener(e ->
                        showMessage("Could not verify request: "
                                + e.getLocalizedMessage()));
    }

    private void rejectRequest(RentalRequest request) {
        if (auth.getCurrentUser() == null) {
            showMessage("Please sign in again");
            return;
        }

        String requestId = request.getRequestId();

        if (requestId == null || requestId.trim().isEmpty()) {
            showMessage("Request ID is missing");
            return;
        }

        db.collection("rentalRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()
                            || !"pending".equalsIgnoreCase(
                            safeText(document.getString("status")))) {
                        showMessage("Only pending requests can be rejected");
                        loadRequests();
                        return;
                    }

                    String ownerId = document.getString("ownerId");

                    if (!auth.getCurrentUser().getUid().equals(ownerId)) {
                        showMessage("You cannot reject this request");
                        return;
                    }

                    db.collection("rentalRequests")
                            .document(requestId)
                            .update(
                                    "status", "rejected",
                                    "updatedAt", FieldValue.serverTimestamp()
                            )
                            .addOnSuccessListener(unused -> {
                                showMessage("Rental request rejected");
                                loadRequests();
                            })
                            .addOnFailureListener(e ->
                                    showMessage("Could not reject request: "
                                            + e.getLocalizedMessage()));
                })
                .addOnFailureListener(e ->
                        showMessage("Could not read request: "
                                + e.getLocalizedMessage()));
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty()
                ? "Not provided"
                : value;
    }

    protected void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
