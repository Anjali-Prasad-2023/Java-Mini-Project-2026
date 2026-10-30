package com.example.rental_system.owner;

import com.example.rental_system.LoginActivity;
import com.example.rental_system.R;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public abstract class BaseActivity extends AppCompatActivity {

    protected FirebaseAuth auth;

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onStart() {
        super.onStart();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public void setContentView(int layoutResID) {

        // Main vertical container
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 247, 242));

        // Screen content occupies the area above navigation
        FrameLayout contentFrame = new FrameLayout(this);

        LinearLayout.LayoutParams contentParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                );

        root.addView(contentFrame, contentParams);

        View screenContent = getLayoutInflater().inflate(
                layoutResID,
                contentFrame,
                false
        );

        contentFrame.addView(screenContent);

        // Create bottom navigation
        bottomNavigation = new BottomNavigationView(this);
        bottomNavigation.setVisibility(View.VISIBLE);
        bottomNavigation.setBackgroundColor(Color.WHITE);
        bottomNavigation.setElevation(dp(8));

        bottomNavigation.setLabelVisibilityMode(
                BottomNavigationView.LABEL_VISIBILITY_LABELED
        );

        bottomNavigation.setItemHorizontalTranslationEnabled(false);
        bottomNavigation.setItemIconSize(dp(24));

        // IMPORTANT: Add the menu items before selecting a tab.
        bottomNavigation.inflateMenu(
                R.menu.owner_bottom_nav_menu
        );

        // Selected item: amber; unselected items: grey-green.
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
        };

        int[] colors = new int[] {
                Color.rgb(185, 120, 25),
                Color.rgb(101, 115, 107)
        };

        ColorStateList navigationColors =
                new ColorStateList(states, colors);

        bottomNavigation.setItemIconTintList(navigationColors);
        bottomNavigation.setItemTextColor(navigationColors);

        // Keep the menu background simple while testing.
        bottomNavigation.setItemBackgroundResource(
                android.R.color.transparent
        );

        LinearLayout.LayoutParams navigationParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        root.addView(bottomNavigation, navigationParams);

        // Attach the complete screen to the activity.
        super.setContentView(root);

        // Set the correct tab for the current activity.
        int selectedItem = getSelectedNavigationItem();

        if (selectedItem != 0) {
            bottomNavigation.setSelectedItemId(selectedItem);
        }

        // Handle navigation clicks.
        bottomNavigation.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();
            Class<?> destination;

            if (itemId == R.id.nav_home) {
                destination = OwnerHomeActivity.class;

            } else if (itemId == R.id.nav_properties) {
                destination = MyPropertiesActivity.class;

            } else if (itemId == R.id.nav_requests) {
                destination = RentalRequestsActivity.class;

            } else if (itemId == R.id.nav_rentals) {
                destination = ManageRentalActivity.class;

            } else if (itemId == R.id.nav_profile) {
                destination = OwnerProfileActivity.class;

            } else {
                return false;
            }

            // Already on this screen: keep the tab selected.
            if (destination.equals(getClass())) {
                return true;
            }

            Intent intent = new Intent(
                    BaseActivity.this,
                    destination
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            overridePendingTransition(0, 0);

            return true;
        });
    }

    private int getSelectedNavigationItem() {

        if (this instanceof OwnerHomeActivity) {
            return R.id.nav_home;
        }

        if (this instanceof MyPropertiesActivity) {
            return R.id.nav_properties;
        }

        if (this instanceof RentalRequestsActivity) {
            return R.id.nav_requests;
        }

        if (this instanceof ManageRentalActivity) {
            return R.id.nav_rentals;
        }

        if (this instanceof OwnerProfileActivity) {
            return R.id.nav_profile;
        }

        return 0;
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
        );
    }

    protected void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    protected boolean isLoggedIn() {
        return auth != null
                && auth.getCurrentUser() != null;
    }

    protected void logout() {

        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }

        auth.signOut();

        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bottomNavigation != null) {
            int selectedItem = getSelectedNavigationItem();

            if (selectedItem != 0
                    && bottomNavigation.getSelectedItemId()
                    != selectedItem) {

                bottomNavigation.setSelectedItemId(selectedItem);
            }
        }
    }
}