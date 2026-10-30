
package com.example.rental_system.owner;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.rental_system.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OwnerReviewsActivity extends BaseActivity {

    private ListView listView;
    private FirebaseFirestore db;
    private final List<String> displayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_reviews);

        auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        listView = findViewById(R.id.listReviews);

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReviews();
    }

    private void loadReviews() {
        if (!isLoggedIn()) {
            logout();
            return;
        }

        String ownerId = auth.getCurrentUser().getUid();

        db.collection("reviews")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    displayList.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String roomId = doc.getString("roomId");
                        String comment = doc.getString("comment");
                        String date = doc.getString("date");
                        Object rating = doc.get("rating");

                        displayList.add(
                                "Room ID: " + roomId +
                                        "\nRating: " + rating + " / 5" +
                                        "\nComment: " +
                                        (comment == null ? "No comment" : comment) +
                                        "\nDate: " + date);
                    }

                    adapter.notifyDataSetChanged();

                    if (displayList.isEmpty()) {
                        showMessage("No reviews received yet");
                    }
                })
                .addOnFailureListener(e ->
                        showMessage("Failed to load reviews: "
                                + e.getMessage()));
    }
}
