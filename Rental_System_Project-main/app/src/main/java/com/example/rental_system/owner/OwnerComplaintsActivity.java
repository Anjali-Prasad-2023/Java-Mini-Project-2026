
package com.example.rental_system.owner;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rental_system.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OwnerComplaintsActivity extends AppCompatActivity {

    private ListView listComplaints;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final ArrayList<String> complaintList = new ArrayList<>();
    private final ArrayList<DocumentSnapshot> complaintDocuments =
            new ArrayList<>();

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_complaints);

        listComplaints = findViewById(R.id.listComplaints);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                complaintList
        );

        listComplaints.setAdapter(adapter);

        listComplaints.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= complaintDocuments.size()) {
                return;
            }

            DocumentSnapshot complaint = complaintDocuments.get(position);
            String status = complaint.getString("status");

            if ("RESOLVED".equalsIgnoreCase(status)) {
                Toast.makeText(
                        this,
                        "This complaint is already resolved.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Resolve Complaint")
                    .setMessage(
                            "Mark this complaint as resolved and notify the tenant?"
                    )
                    .setPositiveButton("Resolve", (dialog, which) ->
                            resolveComplaint(complaint))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        loadComplaints();
    }

    private void loadComplaints() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String ownerId = auth.getCurrentUser().getUid();

        db.collection("complaints")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    complaintList.clear();
                    complaintDocuments.clear();

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        String title = document.getString("title");

                        if (title == null || title.trim().isEmpty()) {
                            title = document.getString("subject");
                        }

                        if (title == null || title.trim().isEmpty()) {
                            title = "Complaint";
                        }

                        String roomId = document.getString("roomId");
                        String description =
                                document.getString("description");
                        String status = document.getString("status");

                        if (roomId == null) {
                            roomId = "Not specified";
                        }

                        if (description == null) {
                            description = "";
                        }

                        if (status == null) {
                            status = "PENDING";
                        }

                        complaintList.add(
                                "Complaint: " + title
                                        + "\nRoom ID: " + roomId
                                        + "\nDescription: " + description
                                        + "\nStatus: " + status
                                        + "\n\nTap to resolve"
                        );

                        complaintDocuments.add(document);
                    }

                    if (complaintList.isEmpty()) {
                        complaintList.add("No complaints found.");
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to load complaints: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void resolveComplaint(DocumentSnapshot complaint) {
        String complaintId = complaint.getId();
        String tenantId = complaint.getString("tenantId");
        String roomId = complaint.getString("roomId");

        if (tenantId == null || tenantId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Tenant ID is missing. Cannot notify tenant.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "RESOLVED");
        updates.put("resolvedAt", System.currentTimeMillis());

        db.collection("complaints")
                .document(complaintId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> notification = new HashMap<>();

                    notification.put("recipientId", tenantId);
                    notification.put("userId", tenantId);
                    notification.put("title", "Complaint Resolved");
                    notification.put(
                            "message",
                            "The owner has resolved your complaint."
                    );
                    notification.put("type", "complaint_resolved");
                    notification.put("complaintId", complaintId);
                    notification.put("roomId", roomId);
                    notification.put("read", false);
                    notification.put(
                            "createdAt",
                            System.currentTimeMillis()
                    );

                    db.collection("notifications")
                            .add(notification)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(
                                        this,
                                        "Complaint resolved. Tenant notified.",
                                        Toast.LENGTH_LONG
                                ).show();

                                loadComplaints();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(
                                        this,
                                        "Complaint resolved, but notification "
                                                + "could not be saved: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                                loadComplaints();
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to resolve complaint: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && auth != null) {
            loadComplaints();
        }
    }
}
