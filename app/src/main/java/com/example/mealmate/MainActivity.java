package com.example.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.mealmate.databinding.ActivityMainBinding;
import com.example.mealmate.fragments.GroceryListFragment;
import com.example.mealmate.fragments.HomeFragment;
import com.example.mealmate.fragments.MealPlanFragment;
import com.example.mealmate.fragments.ProfileFragment;
import com.example.mealmate.fragments.RecipesFragment;
import com.example.mealmate.utils.SessionManager;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private DrawerLayout drawer;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Initialize session manager
        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (currentUser == null) {
            // Not logged in, redirect to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Setup drawer
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Setup user info in nav header
        setupNavHeader(navigationView);

        // Show home fragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    private void setupNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderName = headerView.findViewById(R.id.navHeaderName);
        TextView navHeaderEmail = headerView.findViewById(R.id.navHeaderEmail);

        // Set email from Firebase user
        if (currentUser != null && currentUser.getEmail() != null) {
            navHeaderEmail.setText(currentUser.getEmail());
        }

        // Load full name from Firestore
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String fullName = document.getString("fullName");
                                if (fullName != null && !fullName.isEmpty()) {
                                    navHeaderName.setText(fullName);
                                }
                            }
                        }
                    });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
            setTitle(getString(R.string.menu_home));
        } else if (itemId == R.id.nav_recipes) {
            selectedFragment = new RecipesFragment();
            setTitle(getString(R.string.menu_recipes));
        } else if (itemId == R.id.nav_meal_plan) {
            selectedFragment = new MealPlanFragment();
            setTitle(getString(R.string.menu_meal_plan));
        } else if (itemId == R.id.nav_grocery_list) {
            selectedFragment = new GroceryListFragment();
            setTitle(getString(R.string.menu_grocery_list));
        } else if (itemId == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
            setTitle(getString(R.string.menu_profile));
        } else if (itemId == R.id.nav_logout) {
            logout();
            return true;
        }

        // Replace the fragment
        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    selectedFragment).commit();
        }

        // Close the drawer
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handle user logout
     * If remember me is not enabled, clear saved credentials
     */
    private void logout() {
        // Clear saved credentials if remember me is not enabled
        if (!sessionManager.isRememberMeEnabled()) {
            sessionManager.clearCredentials();
        }
        
        // Sign out from Firebase
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        
        // Return to login screen
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        // Close drawer if open, otherwise normal back button behavior
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}