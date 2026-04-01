package com.example.mealmate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Session manager to save user login credentials and session status
 */
public class SessionManager {
    
    private static final String TAG = "SessionManager";
    
    // Shared preferences file name
    private static final String PREF_NAME = "MealMateLoginPrefs";
    
    // Shared preferences keys
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    
    // Shared preferences
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    
    /**
     * Constructor
     */
    public SessionManager(Context context) {
        // Create shared preferences and editor
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }
    
    /**
     * Save user credentials if remember me is enabled
     */
    public void saveUserCredentials(String email, String password, boolean rememberMe) {
        Log.d(TAG, "Saving user credentials with remember me: " + rememberMe);
        
        // Set values in shared preferences
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        
        // Only save credentials if remember me is enabled
        if (rememberMe) {
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
        } else {
            // Clear credentials if remember me is disabled
            editor.remove(KEY_EMAIL);
            editor.remove(KEY_PASSWORD);
        }
        
        // Commit changes to shared preferences
        editor.apply();
    }
    
    /**
     * Get remember me status
     */
    public boolean isRememberMeEnabled() {
        return pref.getBoolean(KEY_REMEMBER_ME, false);
    }
    
    /**
     * Get saved email
     */
    public String getSavedEmail() {
        return pref.getString(KEY_EMAIL, "");
    }
    
    /**
     * Get saved password
     */
    public String getSavedPassword() {
        return pref.getString(KEY_PASSWORD, "");
    }
    
    /**
     * Clear saved credentials
     */
    public void clearCredentials() {
        Log.d(TAG, "Clearing saved credentials");
        
        // Clear all data from shared preferences
        editor.remove(KEY_REMEMBER_ME);
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_PASSWORD);
        editor.apply();
    }
} 