package com.example.mealmate.utils;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.mealmate.models.ActivityItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Utility class for tracking user activities in the app
 */
public class ActivityTracker {

    private static final String COLLECTION_USER_ACTIVITIES = "userActivities";
    
    /**
     * Track a recipe activity
     * @param context Application context
     * @param title Activity title
     * @param description Activity description (optional)
     */
    public static void trackRecipeActivity(Context context, String title, String description) {
        trackActivity(context, title, description, "recipe");
    }
    
    /**
     * Track a meal plan activity
     * @param context Application context
     * @param title Activity title
     * @param description Activity description (optional)
     */
    public static void trackMealPlanActivity(Context context, String title, String description) {
        trackActivity(context, title, description, "meal_plan");
    }
    
    /**
     * Track a grocery list activity
     * @param context Application context
     * @param title Activity title
     * @param description Activity description (optional)
     */
    public static void trackGroceryActivity(Context context, String title, String description) {
        trackActivity(context, title, description, "grocery");
    }
    
    /**
     * Track a user activity
     * @param context Application context
     * @param title Activity title
     * @param description Activity description (optional)
     * @param type Activity type
     */
    private static void trackActivity(Context context, String title, String description, String type) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Create activity timestamp
        Date timestamp = new Date();
        
        // Format time for display
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
        String formattedTime = dateFormat.format(timestamp);
        
        // Create activity item
        ActivityItem activity = new ActivityItem();
        activity.setId(UUID.randomUUID().toString());
        activity.setUserId(currentUser.getUid());
        activity.setTitle(title);
        activity.setDescription(description);
        activity.setType(type);
        activity.setTimestamp(timestamp);
        activity.setFormattedTime(formattedTime);
        
        // Save to Firestore
        db.collection(COLLECTION_USER_ACTIVITIES)
                .document(activity.getId())
                .set(activity)
                .addOnFailureListener(e -> {
                    // Silently fail - activity tracking is non-critical
                });
    }
} 