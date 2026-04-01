package com.example.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.mealmate.databinding.ActivityRegisterBinding;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Button click listeners
        binding.registerButton.setOnClickListener(v -> registerUser());
        binding.loginText.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String fullName = String.valueOf(binding.fullNameEditText.getText()).trim();
        String email = String.valueOf(binding.emailEditText.getText()).trim();
        String password = String.valueOf(binding.passwordEditText.getText()).trim();
        String confirmPassword = String.valueOf(binding.confirmPasswordEditText.getText()).trim();

        // Validate inputs
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        showProgressBar(true);

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign up success, save additional user data to Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Create a new user with fullName
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("fullName", fullName);
                            userData.put("email", email);

                            // Add a new document with user ID
                            db.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        showProgressBar(false);
                                        Toast.makeText(RegisterActivity.this, "Registration successful", 
                                                Toast.LENGTH_SHORT).show();
                                        // Go to login screen
                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        showProgressBar(false);
                                        Toast.makeText(RegisterActivity.this, "Failed to save user data: " + e.getMessage(), 
                                                Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        // If sign up fails, display a message to the user.
                        showProgressBar(false);
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void showProgressBar(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.registerButton.setEnabled(!show);
        binding.fullNameEditText.setEnabled(!show);
        binding.emailEditText.setEnabled(!show);
        binding.passwordEditText.setEnabled(!show);
        binding.confirmPasswordEditText.setEnabled(!show);
        binding.loginText.setEnabled(!show);
    }
} 