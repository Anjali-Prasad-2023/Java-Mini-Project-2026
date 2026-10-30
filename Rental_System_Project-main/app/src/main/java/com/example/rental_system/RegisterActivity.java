
package com.example.rental_system;

import com.example.rental_system.owner.OwnerHomeActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private EditText etPassword, etConfirmPassword;
    private RadioButton rbTenant, rbOwner;
    private Button btnRegister, btnBackToLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        rbTenant = findViewById(R.id.rbTenant);
        rbOwner = findViewById(R.id.rbOwner);

        btnRegister = findViewById(R.id.btnRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        btnRegister.setOnClickListener(v -> register());

        btnBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(
                    RegisterActivity.this,
                    LoginActivity.class
            ));
            finish();
        });
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this,
                    "Please complete every field",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must have at least 6 characters");
            return;
        }

        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        if (!rbTenant.isChecked() && !rbOwner.isChecked()) {
            Toast.makeText(this,
                    "Choose Tenant or Owner",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String role = rbTenant.isChecked() ? "tenant" : "owner";

        btnRegister.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        btnRegister.setEnabled(true);
                        return;
                    }

                    String uid = result.getUser().getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("uid", uid);
                    user.put("name", name);
                    user.put("email", email);
                    user.put("phone", phone);
                    user.put("role", role);
                    user.put("createdAt",
                            com.google.firebase.Timestamp.now());

                    db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                btnRegister.setEnabled(true);

                                Toast.makeText(this,
                                        "Account created successfully",
                                        Toast.LENGTH_SHORT).show();

                                Intent intent;

                                if (role.equals("owner")) {
                                    intent = new Intent(
                                            RegisterActivity.this,
                                            OwnerHomeActivity.class
                                    );
                                } else {
                                    intent = new Intent(
                                            RegisterActivity.this,
                                            TenantHomeActivity.class
                                    );
                                }

                                intent.setFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                );

                                startActivity(intent);
                            })
                            .addOnFailureListener(error -> {
                                btnRegister.setEnabled(true);
                                Toast.makeText(this,
                                        "Account created, but profile saving failed: "
                                                + error.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(error -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this,
                            "Registration failed: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
