
package com.example.rental_system;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;
import com.example.rental_system.owner.OwnerHomeActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/*public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private RadioButton rbTenant, rbOwner;
    private Button btnLogin, btnRegister;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("FCM_TEST",
                                "Could not get FCM token",
                                task.getException());
                        return;
                    }

                    String token = task.getResult();

                    Log.d("FCM_TEST",
                            "FCM token: " + token);
                });
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rbTenant = findViewById(R.id.rbTenant);
        rbOwner = findViewById(R.id.rbOwner);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> login());

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            ));
        });
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty()) {
            etEmail.setError("Enter email");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Enter password");
            return;
        }

        if (!rbTenant.isChecked() && !rbOwner.isChecked()) {
            Toast.makeText(this,
                    "Choose Tenant or Owner",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedRole = rbTenant.isChecked()
                ? "tenant" : "owner";

        btnLogin.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        btnLogin.setEnabled(true);
                        return;
                    }

                    String uid = result.getUser().getUid();

                    db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(document -> {
                                btnLogin.setEnabled(true);

                                if (!document.exists()) {
                                    auth.signOut();
                                    Toast.makeText(this,
                                            "User profile not found.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                String savedRole =
                                        document.getString("role");

                                if (savedRole == null
                                        || !savedRole.equalsIgnoreCase(
                                        selectedRole)) {
                                    auth.signOut();
                                    Toast.makeText(this,
                                            "This account is not registered as "
                                                    + selectedRole + ".",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                Intent intent;

                                if (savedRole.equalsIgnoreCase("owner")) {
                                    intent = new Intent(
                                            LoginActivity.this,
                                            OwnerHomeActivity.class
                                    );
                                } else {
                                    intent = new Intent(
                                            LoginActivity.this,
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
                                btnLogin.setEnabled(true);
                                Toast.makeText(this,
                                        "Could not read user profile: "
                                                + error.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(error -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this,
                            "Login failed: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}*/

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView registerLink, forgotPasswordLink;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);

        loginButton.setOnClickListener(v -> login());

        registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        forgotPasswordLink.setOnClickListener(v -> resetPassword());
    }

    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        loginButton.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        loginButton.setEnabled(true);
                        return;
                    }

                    String uid = result.getUser().getUid();

                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(document -> {
                                loginButton.setEnabled(true);

                                if (!document.exists()) {
                                    auth.signOut();
                                    Toast.makeText(this,
                                            "User profile not found. Please register again.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                String role = document.getString("role");

                                if ("owner".equals(role)) {
                                    startActivity(new Intent(this,
                                            OwnerHomeActivity.class));
                                    finish();
                                } else if ("tenant".equals(role)) {
                                    startActivity(new Intent(this,
                                            TenantHomeActivity.class));
                                    finish();
                                } else {
                                    auth.signOut();
                                    Toast.makeText(this,
                                            "Invalid account role",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(error -> {
                                loginButton.setEnabled(true);
                                Toast.makeText(this,
                                        error.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(error -> {
                    loginButton.setEnabled(true);
                    Toast.makeText(this, error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void resetPassword() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Enter your email first");
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this,
                                "Password reset email requested",
                                Toast.LENGTH_LONG).show())
                .addOnFailureListener(error ->
                        Toast.makeText(this, error.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}
