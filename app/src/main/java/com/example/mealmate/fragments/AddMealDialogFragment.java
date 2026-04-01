package com.example.mealmate.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.mealmate.R;
import com.example.mealmate.models.Recipe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddMealDialogFragment extends DialogFragment {

    private static final String ARG_DATE = "date";
    private static final String ARG_MEAL_TYPE = "meal_type";
    
    private Date selectedDate;
    private String preselectedMealType;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
    
    private TextInputEditText dateInput;
    private AutoCompleteTextView mealTypeDropdown;
    private AutoCompleteTextView recipeDropdown;
    private List<Recipe> recipes;
    private Map<String, Recipe> recipeMap; // Name -> Recipe
    
    private AddMealListener listener;

    public interface AddMealListener {
        void onMealAdded(Date date, String mealType, String recipeId, String recipeName);
    }
    
    public static AddMealDialogFragment newInstance(Date date, String mealType) {
        AddMealDialogFragment fragment = new AddMealDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        args.putString(ARG_MEAL_TYPE, mealType);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes != null ? recipes : new ArrayList<>();
        this.recipeMap = new HashMap<>();
        if (recipes != null) {
            for (Recipe recipe : recipes) {
                if (recipe != null && recipe.getName() != null) {
                    recipeMap.put(recipe.getName(), recipe);
                }
            }
        }
    }
    
    public void setAddMealListener(AddMealListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedDate = (Date) getArguments().getSerializable(ARG_DATE);
            preselectedMealType = getArguments().getString(ARG_MEAL_TYPE);
        }
        
        if (selectedDate == null) {
            selectedDate = Calendar.getInstance().getTime();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        
        // Inflate and set the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_meal, null);
        builder.setView(view);
        
        // Initialize views
        dateInput = view.findViewById(R.id.date_input);
        mealTypeDropdown = view.findViewById(R.id.meal_type_dropdown);
        recipeDropdown = view.findViewById(R.id.recipe_dropdown);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnAdd = view.findViewById(R.id.btn_add);
        
        // Find input layouts for validation
        final TextInputLayout mealTypeLayout = view.findViewById(R.id.meal_type_input_layout);
        final TextInputLayout recipeLayout = view.findViewById(R.id.recipe_input_layout);
        
        // Set up date input
        dateInput.setText(dateFormat.format(selectedDate));
        dateInput.setOnClickListener(v -> showDatePicker());
        
        // Set up meal type dropdown
        String[] mealTypes = {
            getString(R.string.breakfast),
            getString(R.string.lunch),
            getString(R.string.dinner),
            getString(R.string.snacks)
        };
        ArrayAdapter<String> mealTypeAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mealTypes
        );
        mealTypeDropdown.setAdapter(mealTypeAdapter);
        
        if (preselectedMealType != null) {
            mealTypeDropdown.setText(preselectedMealType, false);
        }
        
        // Set up recipe dropdown
        if (recipes != null && !recipes.isEmpty()) {
            List<String> recipeNames = new ArrayList<>();
            for (Recipe recipe : recipes) {
                if (recipe != null && recipe.getName() != null) {
                    recipeNames.add(recipe.getName());
                }
            }
            
            if (!recipeNames.isEmpty()) {
                ArrayAdapter<String> recipeAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    recipeNames
                );
                recipeDropdown.setAdapter(recipeAdapter);
                
                // Set the first recipe as default selection if there are recipes
                if (!recipeNames.isEmpty()) {
                    recipeDropdown.setText(recipeNames.get(0), false);
                }
            } else {
                // No valid recipes found
                Toast.makeText(requireContext(), "No recipes available. Please add recipes first.", Toast.LENGTH_LONG).show();
                new android.os.Handler().postDelayed(this::dismiss, 1000);
            }
        } else {
            // No recipes found
            Toast.makeText(requireContext(), "No recipes available. Please add recipes first.", Toast.LENGTH_LONG).show();
            new android.os.Handler().postDelayed(this::dismiss, 1000);
        }
        
        // Set up button listeners
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnAdd.setOnClickListener(v -> {
            try {
                // Validate inputs directly here instead of using validateInputs method
                String mealType = mealTypeDropdown.getText().toString();
                String recipeName = recipeDropdown.getText().toString();
                
                boolean isValid = true;
                
                if (mealType.isEmpty()) {
                    mealTypeLayout.setError("Please select a meal type");
                    isValid = false;
                } else {
                    mealTypeLayout.setError(null);
                }
                
                if (recipeName.isEmpty()) {
                    recipeLayout.setError("Please select a recipe");
                    isValid = false;
                } else if (!recipeMap.containsKey(recipeName)) {
                    recipeLayout.setError("Please select a valid recipe");
                    isValid = false;
                } else {
                    recipeLayout.setError(null);
                }
                
                if (isValid) {
                    if (recipeMap.containsKey(recipeName)) {
                        Recipe selectedRecipe = recipeMap.get(recipeName);
                        if (listener != null && selectedRecipe != null) {
                            listener.onMealAdded(
                                selectedDate,
                                mealType,
                                selectedRecipe.getId(),
                                selectedRecipe.getName()
                            );
                        }
                        dismiss();
                    } else {
                        Toast.makeText(requireContext(), "Please select a valid recipe", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error adding meal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
        
        return builder.create();
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, month, dayOfMonth);
                selectedDate = newDate.getTime();
                dateInput.setText(dateFormat.format(selectedDate));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.show();
    }
} 