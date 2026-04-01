package com.example.mealmate;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.example.mealmate.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get the intent extras
        Intent intent = getIntent();
        if (intent != null) {
            String email = intent.getStringExtra("EMAIL");
            String fullName = intent.getStringExtra("FULLNAME");

            // Display user info
            String welcomeMessage = "Welcome, " + fullName + "\nYou are logged in with: " + email;
            binding.welcomeMessage.setText(welcomeMessage);
        }

        // Logout button click listener
        binding.logoutButton.setOnClickListener(v -> {
            // Sign out from Firebase
            mAuth.signOut();
            
            // Navigate back to login screen
            Intent loginIntent = new Intent(WelcomeActivity.this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
        });
    }
} 