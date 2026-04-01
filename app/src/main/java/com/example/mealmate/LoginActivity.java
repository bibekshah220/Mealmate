package com.example.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.mealmate.databinding.ActivityLoginBinding;
import com.example.mealmate.databinding.DialogResetPasswordBinding;
import com.example.mealmate.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Initialize session manager
        sessionManager = new SessionManager(this);

        // Button click listeners
        binding.loginButton.setOnClickListener(v -> loginUser());
        binding.registerText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        
        // Forgot password click listener
        binding.forgotPasswordText.setOnClickListener(v -> showResetPasswordDialog());
        
        // Check if remember me is enabled and populate fields
        if (sessionManager.isRememberMeEnabled()) {
            binding.emailEditText.setText(sessionManager.getSavedEmail());
            binding.passwordEditText.setText(sessionManager.getSavedPassword());
            binding.rememberMeCheckbox.setChecked(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, go directly to main activity
            showProgressBar(true);
            startMainActivity();
        }
    }

    private void loginUser() {
        String email = String.valueOf(binding.emailEditText.getText()).trim();
        String password = String.valueOf(binding.passwordEditText.getText()).trim();
        boolean rememberMe = binding.rememberMeCheckbox.isChecked();

        // Validate inputs
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        showProgressBar(true);

        // Sign in with email and password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save user credentials if remember me is checked
                        sessionManager.saveUserCredentials(email, password, rememberMe);
                        
                        // Sign in success, go to main activity
                        startMainActivity();
                    } else {
                        // If sign in fails, display a message to the user.
                        showProgressBar(false);
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : getString(R.string.unknown_error);
                        Toast.makeText(LoginActivity.this, 
                                getString(R.string.auth_failed, errorMessage), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void showResetPasswordDialog() {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, null);
        
        // Find views in the dialog layout
        TextInputEditText resetEmailEditText = dialogView.findViewById(R.id.resetEmailEditText);
        ProgressBar resetProgressBar = dialogView.findViewById(R.id.resetProgressBar);
        
        // Pre-fill email if already entered in login screen
        if (binding.emailEditText.getText() != null && !TextUtils.isEmpty(binding.emailEditText.getText())) {
            resetEmailEditText.setText(binding.emailEditText.getText());
        }
        
        // Create and show the dialog
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.reset_password)
                .setView(dialogView)
                .setPositiveButton(R.string.send_reset_link, null) // Set this to null initially
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        
        alertDialog.show();
        
        // Get the positive button after showing the dialog
        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String email = resetEmailEditText.getText() != null ? 
                    resetEmailEditText.getText().toString().trim() : "";
            
            // Validate email
            if (TextUtils.isEmpty(email)) {
                resetEmailEditText.setError(getString(R.string.email_required));
                return;
            }
            
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                resetEmailEditText.setError(getString(R.string.valid_email_required));
                return;
            }
            
            // Show progress and disable button
            resetProgressBar.setVisibility(View.VISIBLE);
            positiveButton.setEnabled(false);
            
            // Send password reset email
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        resetProgressBar.setVisibility(View.GONE);
                        positiveButton.setEnabled(true);
                        
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, R.string.reset_email_sent, 
                                    Toast.LENGTH_LONG).show();
                            alertDialog.dismiss();
                        } else {
                            String errorMessage = task.getException() != null ? 
                                    ": " + task.getException().getMessage() : "";
                            Toast.makeText(LoginActivity.this, 
                                    getString(R.string.reset_email_failed) + errorMessage, 
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void startMainActivity() {
        showProgressBar(false);
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void showProgressBar(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!show);
        binding.emailEditText.setEnabled(!show);
        binding.passwordEditText.setEnabled(!show);
        binding.rememberMeCheckbox.setEnabled(!show);
        binding.registerText.setEnabled(!show);
        binding.forgotPasswordText.setEnabled(!show);
    }
} 