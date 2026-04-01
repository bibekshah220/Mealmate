package com.example.mealmate.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.mealmate.R;
import com.example.mealmate.adapters.MealAdapter;
import com.example.mealmate.models.MealPlan;
import com.example.mealmate.models.Recipe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MealPlanFragment extends Fragment implements AddMealDialogFragment.AddMealListener, MealAdapter.OnMealListener {

    // Constants
    private static final String BREAKFAST = "Breakfast";
    private static final String LUNCH = "Lunch";
    private static final String DINNER = "Dinner";
    private static final String SNACKS = "Snacks";
    
    // Views
    private TextView monthYearText;
    private TextView selectedDateText;
    private LinearLayout weekdaysContainer;
    private LinearLayout breakfastContainer;
    private LinearLayout lunchContainer;
    private LinearLayout dinnerContainer;
    private LinearLayout snacksContainer;
    private TextView emptyBreakfast;
    private TextView emptyLunch;
    private TextView emptyDinner;
    private TextView emptySnacks;
    
    // Calendar data
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEE", Locale.getDefault());
    private Date selectedDate;
    private List<CardView> dayCards = new ArrayList<>();
    
    // Firebase
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    
    // Data
    private List<MealPlan> mealPlans = new ArrayList<>();
    private List<Recipe> recipes = new ArrayList<>();
    private Map<String, MealPlan> dateToMealPlanMap = new HashMap<>(); // "yyyy-MM-dd" -> MealPlan
    private MealPlan currentMealPlan;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_meal_plan, container, false);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Initialize views
        monthYearText = view.findViewById(R.id.month_year_text);
        selectedDateText = view.findViewById(R.id.selected_date_text);
        weekdaysContainer = view.findViewById(R.id.weekdays_container);
        breakfastContainer = view.findViewById(R.id.breakfast_container);
        lunchContainer = view.findViewById(R.id.lunch_container);
        dinnerContainer = view.findViewById(R.id.dinner_container);
        snacksContainer = view.findViewById(R.id.snacks_container);
        emptyBreakfast = view.findViewById(R.id.empty_breakfast);
        emptyLunch = view.findViewById(R.id.empty_lunch);
        emptyDinner = view.findViewById(R.id.empty_dinner);
        emptySnacks = view.findViewById(R.id.empty_snacks);
        
        // Set up add meal buttons
        view.findViewById(R.id.add_breakfast_btn).setOnClickListener(v -> showAddMealDialog(BREAKFAST));
        view.findViewById(R.id.add_lunch_btn).setOnClickListener(v -> showAddMealDialog(LUNCH));
        view.findViewById(R.id.add_dinner_btn).setOnClickListener(v -> showAddMealDialog(DINNER));
        view.findViewById(R.id.add_snacks_btn).setOnClickListener(v -> showAddMealDialog(SNACKS));
        
        // Initialize with today's date
        selectedDate = Calendar.getInstance().getTime();
        updateDateHeader();
        
        // Load data
        loadRecipes();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        setupWeekView();
        loadMealPlans();
    }
    
    private void updateDateHeader() {
        selectedDateText.setText(String.format("Selected Date: %s", dateFormat.format(selectedDate)));
        monthYearText.setText(monthYearFormat.format(selectedDate));
    }
    
    private void setupWeekView() {
        weekdaysContainer.removeAllViews();
        dayCards.clear();
        
        // Get the current date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        
        // Find the start of the week (Sunday)
        Calendar startOfWeek = (Calendar) calendar.clone();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        
        // Create 7 day views
        for (int i = 0; i < 7; i++) {
            final int dayIndex = i;
            Calendar dayCalendar = (Calendar) startOfWeek.clone();
            dayCalendar.add(Calendar.DAY_OF_WEEK, dayIndex);
            
            View dayView = getLayoutInflater().inflate(R.layout.item_calendar_day, weekdaysContainer, false);
            CardView cardView = (CardView) dayView;
            dayCards.add(cardView);
            
            TextView dayName = dayView.findViewById(R.id.day_name);
            TextView dayNumber = dayView.findViewById(R.id.day_number);
            TextView todayIndicator = dayView.findViewById(R.id.today_indicator);
            
            // Set day of week
            dayName.setText(dayOfWeekFormat.format(dayCalendar.getTime()).toUpperCase());
            
            // Set day number
            dayNumber.setText(String.valueOf(dayCalendar.get(Calendar.DAY_OF_MONTH)));
            
            // Check if this is today
            Calendar today = Calendar.getInstance();
            if (today.get(Calendar.YEAR) == dayCalendar.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == dayCalendar.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == dayCalendar.get(Calendar.DAY_OF_MONTH)) {
                todayIndicator.setVisibility(View.VISIBLE);
            }
            
            // Check if this is the selected date
            if (calendar.get(Calendar.YEAR) == dayCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == dayCalendar.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == dayCalendar.get(Calendar.DAY_OF_MONTH)) {
                dayNumber.setBackgroundResource(R.drawable.selected_day_background);
                dayNumber.setTextColor(getResources().getColor(android.R.color.white));
            }
            
            // Set click listener
            final Date finalDate = dayCalendar.getTime();
            cardView.setOnClickListener(v -> {
                // Update selected date
                selectedDate = finalDate;
                updateDateHeader();
                setupWeekView(); // Refresh the week view with the new selection
                updateMealPlanUI(); // Refresh meal plan UI
            });
            
            weekdaysContainer.addView(dayView);
        }
        
        // Update the meal plan indicators
        updateCalendarIndicators();
    }
    
    private void loadRecipes() {
        if (currentUser == null) return;
        
        db.collection("recipes")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        recipes.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Recipe recipe = document.toObject(Recipe.class);
                            recipe.setId(document.getId());
                            recipes.add(recipe);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error loading recipes", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadMealPlans() {
        if (currentUser == null) return;
        
        db.collection("mealPlans")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mealPlans.clear();
                        dateToMealPlanMap.clear();
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            MealPlan mealPlan = document.toObject(MealPlan.class);
                            mealPlan.setId(document.getId());
                            mealPlans.add(mealPlan);
                            
                            // Map date to meal plan for easier lookup
                            if (mealPlan.getDate() != null) {
                                SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                String dateKey = keyFormat.format(mealPlan.getDate());
                                dateToMealPlanMap.put(dateKey, mealPlan);
                            }
                        }
                        
                        // Update UI elements
                        updateCalendarIndicators();
                        updateMealPlanUI();
                    } else {
                        Toast.makeText(getContext(), "Error loading meal plans", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void updateCalendarIndicators() {
        // For each day card in the week view, check if there's a meal plan
        for (int i = 0; i < dayCards.size(); i++) {
            final int dayIndex = i;
            CardView card = dayCards.get(dayIndex);
            View breakfastIndicator = card.findViewById(R.id.breakfast_indicator);
            View lunchIndicator = card.findViewById(R.id.lunch_indicator);
            View dinnerIndicator = card.findViewById(R.id.dinner_indicator);
            View snacksIndicator = card.findViewById(R.id.snacks_indicator);
            
            // Get the date for this card
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate);
            calendar.add(Calendar.DAY_OF_WEEK, dayIndex - calendar.get(Calendar.DAY_OF_WEEK) + Calendar.SUNDAY);
            
            // Get the meal plan for this date
            SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateKey = keyFormat.format(calendar.getTime());
            MealPlan mealPlan = dateToMealPlanMap.get(dateKey);
            
            // Set indicator visibility based on meal plan
            if (mealPlan != null) {
                breakfastIndicator.setVisibility(mealPlan.getBreakfast().isEmpty() ? View.GONE : View.VISIBLE);
                lunchIndicator.setVisibility(mealPlan.getLunch().isEmpty() ? View.GONE : View.VISIBLE);
                dinnerIndicator.setVisibility(mealPlan.getDinner().isEmpty() ? View.GONE : View.VISIBLE);
                snacksIndicator.setVisibility(mealPlan.getSnacks().isEmpty() ? View.GONE : View.VISIBLE);
            } else {
                breakfastIndicator.setVisibility(View.GONE);
                lunchIndicator.setVisibility(View.GONE);
                dinnerIndicator.setVisibility(View.GONE);
                snacksIndicator.setVisibility(View.GONE);
            }
        }
    }
    
    private void updateMealPlanUI() {
        // Get the meal plan for the selected date
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateKey = keyFormat.format(selectedDate);
        currentMealPlan = dateToMealPlanMap.get(dateKey);
        
        if (currentMealPlan == null) {
            // No meal plan for this date, show empty views
            emptyBreakfast.setVisibility(View.VISIBLE);
            emptyLunch.setVisibility(View.VISIBLE);
            emptyDinner.setVisibility(View.VISIBLE);
            emptySnacks.setVisibility(View.VISIBLE);
            
            // Clear any existing meal views
            breakfastContainer.removeAllViews();
            lunchContainer.removeAllViews();
            dinnerContainer.removeAllViews();
            snacksContainer.removeAllViews();
            
            breakfastContainer.addView(emptyBreakfast);
            lunchContainer.addView(emptyLunch);
            dinnerContainer.addView(emptyDinner);
            snacksContainer.addView(emptySnacks);
        } else {
            // Show meal plan data
            updateMealSection(breakfastContainer, emptyBreakfast, currentMealPlan.getBreakfast());
            updateMealSection(lunchContainer, emptyLunch, currentMealPlan.getLunch());
            updateMealSection(dinnerContainer, emptyDinner, currentMealPlan.getDinner());
            updateMealSection(snacksContainer, emptySnacks, currentMealPlan.getSnacks());
        }
    }
    
    private void updateMealSection(LinearLayout container, TextView emptyView, Map<String, String> meals) {
        container.removeAllViews();
        
        if (meals == null || meals.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            container.addView(emptyView);
        } else {
            emptyView.setVisibility(View.GONE);
            
            for (Map.Entry<String, String> entry : meals.entrySet()) {
                final String recipeId = entry.getKey();
                final String recipeName = entry.getValue();
                
                View mealView = getLayoutInflater().inflate(R.layout.item_meal, container, false);
                TextView mealNameView = mealView.findViewById(R.id.meal_name);
                mealNameView.setText(recipeName);
                
                // Find recipe to display image
                Recipe recipe = findRecipeById(recipeId);
                if (recipe != null && getContext() != null) {
                    // The MealAdapter would handle this normally, but we're using individual views here
                    ImageView mealImageView = mealView.findViewById(R.id.meal_image);
                    com.bumptech.glide.Glide.with(getContext())
                        .load(recipe.getImageUrl())
                        .placeholder(R.drawable.ic_recipe)
                        .error(R.drawable.ic_recipe)
                        .centerCrop()
                        .into(mealImageView);
                }
                
                // Store a reference to the container to use in lambda
                final LinearLayout finalContainer = container;
                
                // Set remove button click listener
                mealView.findViewById(R.id.btn_remove_meal).setOnClickListener(v -> {
                    if (finalContainer == breakfastContainer) {
                        removeMeal(BREAKFAST, recipeId);
                    } else if (finalContainer == lunchContainer) {
                        removeMeal(LUNCH, recipeId);
                    } else if (finalContainer == dinnerContainer) {
                        removeMeal(DINNER, recipeId);
                    } else if (finalContainer == snacksContainer) {
                        removeMeal(SNACKS, recipeId);
                    }
                });
                
                container.addView(mealView);
            }
        }
    }
    
    private Recipe findRecipeById(String recipeId) {
        for (Recipe recipe : recipes) {
            if (recipe.getId().equals(recipeId)) {
                return recipe;
            }
        }
        return null;
    }
    
    private void showAddMealDialog(String mealType) {
        AddMealDialogFragment dialogFragment = AddMealDialogFragment.newInstance(selectedDate, mealType);
        dialogFragment.setRecipes(recipes);
        dialogFragment.setAddMealListener(this);
        dialogFragment.show(getParentFragmentManager(), "AddMealDialog");
    }
    
    @Override
    public void onMealAdded(Date date, String mealType, String recipeId, String recipeName) {
        try {
            // Safety check for null parameters
            if (date == null || mealType == null || recipeId == null || recipeName == null || currentUser == null) {
                Toast.makeText(getContext(), "Error: Invalid meal data", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Format date for map lookup
            SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateKey = keyFormat.format(date);
            
            // Get or create meal plan for this date
            MealPlan mealPlan = dateToMealPlanMap.get(dateKey);
            boolean isNewMealPlan = false;
            
            if (mealPlan == null) {
                // Create a new meal plan
                String newId = UUID.randomUUID().toString();
                mealPlan = new MealPlan(newId, currentUser.getUid(), date);
                isNewMealPlan = true;
            }
            
            // Add the recipe to the appropriate meal
            if (BREAKFAST.equals(mealType)) {
                mealPlan.addBreakfastRecipe(recipeId, recipeName);
            } else if (LUNCH.equals(mealType)) {
                mealPlan.addLunchRecipe(recipeId, recipeName);
            } else if (DINNER.equals(mealType)) {
                mealPlan.addDinnerRecipe(recipeId, recipeName);
            } else if (SNACKS.equals(mealType)) {
                mealPlan.addSnackRecipe(recipeId, recipeName);
            }
            
            // Need final variables for the lambda
            final MealPlan finalMealPlan = mealPlan;
            final boolean finalIsNewMealPlan = isNewMealPlan;
            final String finalDateKey = dateKey;
            
            // Save meal plan to Firestore
            db.collection("mealPlans")
                    .document(finalMealPlan.getId())
                    .set(finalMealPlan)
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() == null) return; // Fragment might be detached
                        
                        Toast.makeText(getContext(), R.string.meal_added, Toast.LENGTH_SHORT).show();
                        
                        // Update local data
                        if (finalIsNewMealPlan) {
                            mealPlans.add(finalMealPlan);
                            dateToMealPlanMap.put(finalDateKey, finalMealPlan);
                        }
                        
                        // If this is the currently selected date, update the UI
                        try {
                            if (keyFormat.format(selectedDate).equals(finalDateKey)) {
                                currentMealPlan = finalMealPlan;
                                updateMealPlanUI();
                            }
                            
                            // Update calendar indicators
                            updateCalendarIndicators();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() == null) return; // Fragment might be detached
                        Toast.makeText(getContext(), R.string.error_adding_meal, Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error adding meal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        }
    }
    
    private void removeMeal(String mealType, String recipeId) {
        try {
            if (currentMealPlan == null) return;
            
            // Remove the recipe from the appropriate meal
            if (BREAKFAST.equals(mealType)) {
                currentMealPlan.removeBreakfastRecipe(recipeId);
            } else if (LUNCH.equals(mealType)) {
                currentMealPlan.removeLunchRecipe(recipeId);
            } else if (DINNER.equals(mealType)) {
                currentMealPlan.removeDinnerRecipe(recipeId);
            } else if (SNACKS.equals(mealType)) {
                currentMealPlan.removeSnackRecipe(recipeId);
            }
            
            // Need final reference for lambda
            final MealPlan planToUpdate = currentMealPlan;
            
            // Save the updated meal plan to Firestore
            db.collection("mealPlans")
                    .document(planToUpdate.getId())
                    .set(planToUpdate)
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() == null) return; // Fragment might be detached
                        
                        Toast.makeText(getContext(), R.string.meal_removed, Toast.LENGTH_SHORT).show();
                        
                        // Update UI
                        updateMealPlanUI();
                        updateCalendarIndicators();
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() == null) return; // Fragment might be detached
                        Toast.makeText(getContext(), R.string.error_removing_meal, Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error removing meal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        }
    }
    
    @Override
    public void onMealClick(String recipeId) {
        // Navigate to recipe details
        Recipe recipe = findRecipeById(recipeId);
        if (recipe != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, RecipeDetailsFragment.newInstance(recipe))
                    .addToBackStack(null)
                    .commit();
        }
    }
    
    @Override
    public void onRemoveMeal(String recipeId) {
        // This is handled in the individual meal view click listeners
    }
} 