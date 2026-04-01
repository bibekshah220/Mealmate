package com.example.mealmate.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.mealmate.R;
import com.example.mealmate.adapters.RecentActivityAdapter;
import com.example.mealmate.models.ActivityItem;
import com.example.mealmate.models.GroceryItem;
import com.example.mealmate.models.MealPlan;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    
    // Firebase
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    
    // UI components
    private TextView welcomeText;
    private TextView dateText;
    private TextView recipesCountText;
    private TextView plannedMealsCountText;
    private TextView groceryItemsCountText;
    private TextView groceryCostText;
    private TextView tvBreakfast;
    private TextView tvLunch;
    private TextView tvDinner;
    private TextView tvSnacks;
    private RecyclerView recentActivityRecyclerView;
    private TextView noRecentActivityText;
    
    private RecentActivityAdapter activityAdapter;
    private List<ActivityItem> activityItems;
    
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Initialize formatters
        dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        
        // Initialize UI components
        initializeViews(view);
        setupDateAndUser();
        setupQuickActions(view);
        
        // Load data
        loadAnalytics();
        loadTodaysMeals();
        loadRecentActivity();
    }
    
    private void initializeViews(View view) {
        welcomeText = view.findViewById(R.id.tv_welcome);
        dateText = view.findViewById(R.id.tv_date);
        recipesCountText = view.findViewById(R.id.tv_recipes_count);
        plannedMealsCountText = view.findViewById(R.id.tv_planned_meals_count);
        groceryItemsCountText = view.findViewById(R.id.tv_grocery_items_count);
        groceryCostText = view.findViewById(R.id.tv_grocery_cost);
        tvBreakfast = view.findViewById(R.id.tv_breakfast);
        tvLunch = view.findViewById(R.id.tv_lunch);
        tvDinner = view.findViewById(R.id.tv_dinner);
        tvSnacks = view.findViewById(R.id.tv_snacks);
        recentActivityRecyclerView = view.findViewById(R.id.recycler_recent_activity);
        noRecentActivityText = view.findViewById(R.id.tv_no_recent_activity);
        
        // Setup RecyclerView
        activityItems = new ArrayList<>();
        activityAdapter = new RecentActivityAdapter(requireContext(), activityItems);
        recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentActivityRecyclerView.setAdapter(activityAdapter);
        
        // Plan today button
        view.findViewById(R.id.btn_plan_today).setOnClickListener(v -> {
            // Navigate to MealPlanFragment
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MealPlanFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
    
    private void setupDateAndUser() {
        // Set current date
        dateText.setText(dateFormat.format(new Date()));
        
        // Set user name
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                welcomeText.setText(getString(R.string.welcome_user, displayName));
            } else {
                welcomeText.setText(R.string.welcome);
            }
        }
    }
    
    private void setupQuickActions(View view) {
        // Add Recipe
        view.findViewById(R.id.btn_add_recipe).setOnClickListener(v -> {
            Fragment recipesFragment = new RecipesFragment();
            Bundle args = new Bundle();
            args.putBoolean("show_add_dialog", true);
            recipesFragment.setArguments(args);
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, recipesFragment)
                    .addToBackStack(null)
                    .commit();
        });
        
        // Add to Meal Plan
        view.findViewById(R.id.btn_add_to_meal_plan).setOnClickListener(v -> {
            Fragment mealPlanFragment = new MealPlanFragment();
            Bundle args = new Bundle();
            args.putBoolean("show_add_dialog", true);
            mealPlanFragment.setArguments(args);
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mealPlanFragment)
                    .addToBackStack(null)
                    .commit();
        });
        
        // Add Grocery Item
        view.findViewById(R.id.btn_add_grocery).setOnClickListener(v -> {
            Fragment groceryListFragment = new GroceryListFragment();
            Bundle args = new Bundle();
            args.putBoolean("show_add_dialog", true);
            groceryListFragment.setArguments(args);
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, groceryListFragment)
                    .addToBackStack(null)
                    .commit();
        });
        
        // Share Grocery List
        view.findViewById(R.id.btn_share_grocery).setOnClickListener(v -> {
            Fragment groceryListFragment = new GroceryListFragment();
            Bundle args = new Bundle();
            args.putBoolean("show_share_dialog", true);
            groceryListFragment.setArguments(args);
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, groceryListFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }
    
    private void loadAnalytics() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        
        // Load recipe count
        db.collection("recipes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int recipeCount = queryDocumentSnapshots.size();
                    recipesCountText.setText(String.valueOf(recipeCount));
                });
        
        // Load meal plan count
        db.collection("mealPlans")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalMealCount = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MealPlan mealPlan = doc.toObject(MealPlan.class);
                        if (mealPlan.getBreakfast() != null && !mealPlan.getBreakfast().isEmpty()) totalMealCount++;
                        if (mealPlan.getLunch() != null && !mealPlan.getLunch().isEmpty()) totalMealCount++;
                        if (mealPlan.getDinner() != null && !mealPlan.getDinner().isEmpty()) totalMealCount++;
                        if (mealPlan.getSnacks() != null && !mealPlan.getSnacks().isEmpty()) totalMealCount++;
                    }
                    plannedMealsCountText.setText(String.valueOf(totalMealCount));
                });
        
        // Load grocery data
        db.collection("groceryItems")
                .whereEqualTo("userId", userId)
                .whereEqualTo("purchased", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int groceryCount = queryDocumentSnapshots.size();
                    groceryItemsCountText.setText(String.valueOf(groceryCount));
                    
                    double totalCost = 0.0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GroceryItem item = doc.toObject(GroceryItem.class);
                        totalCost += item.getPrice() * item.getQuantity();
                    }
                    
                    groceryCostText.setText(currencyFormat.format(totalCost));
                });
    }
    
    private void loadTodaysMeals() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        
        // Get today's date in yyyy-MM-dd format for Firestore query
        SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = firestoreDateFormat.format(new Date());
        
        db.collection("mealPlans")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        MealPlan todayMealPlan = queryDocumentSnapshots.getDocuments().get(0).toObject(MealPlan.class);
                        if (todayMealPlan != null) {
                            // Update UI with today's meals
                            if (todayMealPlan.getBreakfast() != null && !todayMealPlan.getBreakfast().isEmpty()) {
                                String firstRecipeName = todayMealPlan.getBreakfast().values().iterator().next();
                                tvBreakfast.setText(firstRecipeName);
                            }
                            if (todayMealPlan.getLunch() != null && !todayMealPlan.getLunch().isEmpty()) {
                                String firstRecipeName = todayMealPlan.getLunch().values().iterator().next();
                                tvLunch.setText(firstRecipeName);
                            }
                            if (todayMealPlan.getDinner() != null && !todayMealPlan.getDinner().isEmpty()) {
                                String firstRecipeName = todayMealPlan.getDinner().values().iterator().next();
                                tvDinner.setText(firstRecipeName);
                            }
                            if (todayMealPlan.getSnacks() != null && !todayMealPlan.getSnacks().isEmpty()) {
                                String firstRecipeName = todayMealPlan.getSnacks().values().iterator().next();
                                tvSnacks.setText(firstRecipeName);
                            }
                        }
                    }
                });
    }
    
    private void loadRecentActivity() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        
        // Clear existing items
        activityItems.clear();
        
        // Get the date 7 days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date oneWeekAgo = calendar.getTime();
        
        // Query recent activities from Firestore
        db.collection("userActivities")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("timestamp", oneWeekAgo)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5) // Limit to the 5 most recent activities
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            ActivityItem activity = doc.toObject(ActivityItem.class);
                            activityItems.add(activity);
                        }
                        activityAdapter.notifyDataSetChanged();
                        updateActivityVisibility();
                    } else {
                        // Create dummy activity items for demo
                        createDummyActivities();
                    }
                });
    }
    
    private void createDummyActivities() {
        // Create some dummy activity items for demonstration
        SimpleDateFormat activityDateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
        
        // Activity 1: Added a recipe
        Calendar cal1 = Calendar.getInstance();
        cal1.add(Calendar.HOUR, -2);
        ActivityItem activity1 = new ActivityItem();
        activity1.setTitle("Added new recipe: Spaghetti Bolognese");
        activity1.setType("recipe");
        activity1.setTimestamp(cal1.getTime());
        activity1.setFormattedTime(activityDateFormat.format(cal1.getTime()));
        
        // Activity 2: Added to meal plan
        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.HOUR, -5);
        ActivityItem activity2 = new ActivityItem();
        activity2.setTitle("Added Chicken Curry to dinner");
        activity2.setType("meal_plan");
        activity2.setTimestamp(cal2.getTime());
        activity2.setFormattedTime(activityDateFormat.format(cal2.getTime()));
        
        // Activity 3: Added grocery items
        Calendar cal3 = Calendar.getInstance();
        cal3.add(Calendar.DAY_OF_YEAR, -1);
        ActivityItem activity3 = new ActivityItem();
        activity3.setTitle("Added 4 items to grocery list");
        activity3.setType("grocery");
        activity3.setTimestamp(cal3.getTime());
        activity3.setFormattedTime(activityDateFormat.format(cal3.getTime()));
        
        activityItems.add(activity1);
        activityItems.add(activity2);
        activityItems.add(activity3);
        
        activityAdapter.notifyDataSetChanged();
        updateActivityVisibility();
    }
    
    private void updateActivityVisibility() {
        if (activityItems.isEmpty()) {
            recentActivityRecyclerView.setVisibility(View.GONE);
            noRecentActivityText.setVisibility(View.VISIBLE);
        } else {
            recentActivityRecyclerView.setVisibility(View.VISIBLE);
            noRecentActivityText.setVisibility(View.GONE);
        }
    }
} 