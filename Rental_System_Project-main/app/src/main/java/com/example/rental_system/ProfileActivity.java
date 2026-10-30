
package com.example.rental_system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private Button btnLogout;

    private LinearLayout navHome;
    private LinearLayout navBrowse;
    private LinearLayout navRequests;
    private LinearLayout navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        btnLogout = findViewById(R.id.btnLogout);

        navHome = findViewById(R.id.navHome);
        navBrowse = findViewById(R.id.navBrowse);
        navRequests = findViewById(R.id.navRequests);
        navProfile = findViewById(R.id.navProfile);

        // Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(
                    ProfileActivity.this,
                    LoginActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });

        // Home
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(
                    ProfileActivity.this,
                    TenantHomeActivity.class
            ));
            finish();
        });

        // Browse
        navBrowse.setOnClickListener(v -> {
            startActivity(new Intent(
                    ProfileActivity.this,
                    BrowseRoomsActivity.class
            ));
            finish();
        });

        // Requests
        navRequests.setOnClickListener(v -> {
            startActivity(new Intent(
                    ProfileActivity.this,
                    MyRequestsActivity.class
            ));
            finish();
        });

        // Profile
        navProfile.setOnClickListener(v -> {
            // Already on Profile.
        });
    }
}
