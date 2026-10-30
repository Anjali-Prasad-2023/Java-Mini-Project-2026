package com.example.rental_system.owner;

import com.example.rental_system.R;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.example.rental_system.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

public class OwnerProfileActivity extends BaseActivity {

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText addressEditText;

    private Button saveButton;
    private Button logoutButton;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_owner_profile
        );

        auth = com.google.firebase.auth.FirebaseAuth
                .getInstance();

        db = FirebaseFirestore.getInstance();

        nameEditText =
                findViewById(R.id.editName);

        emailEditText =
                findViewById(R.id.editEmail);

        phoneEditText =
                findViewById(R.id.editPhone);

        addressEditText =
                findViewById(R.id.editAddress);

        saveButton =
                findViewById(R.id.btnSaveProfile);

        logoutButton =
                findViewById(R.id.btnLogout);

        emailEditText.setEnabled(false);

        saveButton.setOnClickListener(
                v -> saveProfile()
        );

        logoutButton.setOnClickListener(
                v -> logout()
        );

        loadProfile();
    }

    private void loadProfile() {

        if (!isLoggedIn()) {

            logout();

            return;
        }

        String uid =
                auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        showMessage(
                                "Profile not found"
                        );

                        return;
                    }

                    User user =
                            document.toObject(
                                    User.class
                            );

                    if (user == null) {
                        return;
                    }

                    nameEditText.setText(
                            user.getName()
                    );

                    emailEditText.setText(
                            user.getEmail()
                    );

                    phoneEditText.setText(
                            user.getPhone()
                    );

                    addressEditText.setText(
                            user.getAddress()
                    );
                })
                .addOnFailureListener(e ->
                        showMessage(
                                "Failed to load profile: " +
                                        e.getMessage()
                        )
                );
    }

    private void saveProfile() {

        String uid =
                auth.getCurrentUser().getUid();

        String name =
                nameEditText.getText()
                        .toString()
                        .trim();

        String phone =
                phoneEditText.getText()
                        .toString()
                        .trim();

        String address =
                addressEditText.getText()
                        .toString()
                        .trim();

        if (name.isEmpty()) {

            nameEditText.setError(
                    "Name cannot be empty"
            );

            return;
        }

        db.collection("users")
                .document(uid)
                .update(
                        "name",
                        name,
                        "phone",
                        phone,
                        "address",
                        address
                )
                .addOnSuccessListener(
                        unused ->
                                showMessage(
                                        "Profile updated"
                                )
                )
                .addOnFailureListener(e ->
                        showMessage(
                                "Update failed: " +
                                        e.getMessage()
                        )
                );
    }
}