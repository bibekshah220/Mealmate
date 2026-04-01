package com.example.mealmate.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.mealmate.R;
import com.example.mealmate.models.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecipeDialogFragment extends DialogFragment {

    private static final String ARG_RECIPE = "recipe";

    private Recipe recipe;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private RecipeDialogListener listener;

    private TextInputEditText recipeNameEditText;
    private TextInputEditText imageUrlEditText;
    private LinearLayout ingredientsContainer;
    private LinearLayout stepsContainer;
    private Button addIngredientButton;
    private Button addStepButton;

    private List<View> ingredientViews = new ArrayList<>();
    private List<View> stepViews = new ArrayList<>();

    public interface RecipeDialogListener {
        void onRecipeSaved(Recipe recipe, boolean isNew);
    }

    public static RecipeDialogFragment newInstance(Recipe recipe) {
        RecipeDialogFragment fragment = new RecipeDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RECIPE, recipe);
        fragment.setArguments(args);
        return fragment;
    }

    public void setRecipeDialogListener(RecipeDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            recipe = (Recipe) getArguments().getSerializable(ARG_RECIPE);
        }

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        
        // Inflate and set the layout for the dialog
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_recipe, null);
        builder.setView(view)
                .setTitle(recipe == null ? "Add Recipe" : "Edit Recipe")
                .setPositiveButton("Save", null) // Set the listener later to prevent auto-dismiss
                .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());

        // Initialize views
        recipeNameEditText = view.findViewById(R.id.edit_recipe_name);
        imageUrlEditText = view.findViewById(R.id.edit_image_url);
        ingredientsContainer = view.findViewById(R.id.ingredients_container);
        stepsContainer = view.findViewById(R.id.steps_container);
        addIngredientButton = view.findViewById(R.id.btn_add_ingredient);
        addStepButton = view.findViewById(R.id.btn_add_step);

        // Set up listeners
        addIngredientButton.setOnClickListener(v -> addIngredientField(null));
        addStepButton.setOnClickListener(v -> addStepField(null));

        // If editing, fill in existing data
        if (recipe != null) {
            recipeNameEditText.setText(recipe.getName());
            imageUrlEditText.setText(recipe.getImageUrl());
            
            // Add ingredient fields
            if (recipe.getIngredients() != null) {
                for (String ingredient : recipe.getIngredients()) {
                    addIngredientField(ingredient);
                }
            }
            
            // Add step fields
            if (recipe.getSteps() != null) {
                for (String step : recipe.getSteps()) {
                    addStepField(step);
                }
            }
        }

        // Add at least one of each field if there are none
        if (ingredientViews.isEmpty()) {
            addIngredientField(null);
        }
        
        if (stepViews.isEmpty()) {
            addStepField(null);
        }

        AlertDialog dialog = builder.create();
        
        // Override the positive button click to validate before dismissing
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                if (validateAndSaveRecipe()) {
                    dialog.dismiss();
                }
            });
        });
        
        return dialog;
    }

    private void addIngredientField(String text) {
        View view = getLayoutInflater().inflate(R.layout.item_input_field, ingredientsContainer, false);
        EditText editText = view.findViewById(R.id.edit_text);
        editText.setHint("Ingredient");
        
        if (text != null) {
            editText.setText(text);
        }
        
        view.findViewById(R.id.btn_remove).setOnClickListener(v -> {
            ingredientsContainer.removeView(view);
            ingredientViews.remove(view);
        });
        
        ingredientsContainer.addView(view);
        ingredientViews.add(view);
    }

    private void addStepField(String text) {
        View view = getLayoutInflater().inflate(R.layout.item_input_field, stepsContainer, false);
        EditText editText = view.findViewById(R.id.edit_text);
        editText.setHint("Step " + (stepViews.size() + 1));
        
        if (text != null) {
            editText.setText(text);
        }
        
        view.findViewById(R.id.btn_remove).setOnClickListener(v -> {
            stepsContainer.removeView(view);
            stepViews.remove(view);
            // Update step numbers
            for (int i = 0; i < stepViews.size(); i++) {
                EditText stepEdit = stepViews.get(i).findViewById(R.id.edit_text);
                stepEdit.setHint("Step " + (i + 1));
            }
        });
        
        stepsContainer.addView(view);
        stepViews.add(view);
    }

    private boolean validateAndSaveRecipe() {
        String name = recipeNameEditText.getText().toString().trim();
        String imageUrl = imageUrlEditText.getText().toString().trim();
        
        if (name.isEmpty()) {
            recipeNameEditText.setError("Recipe name is required");
            return false;
        }
        
        // Validate image URL format (basic check)
        if (!imageUrl.isEmpty() && !imageUrl.startsWith("http")) {
            imageUrlEditText.setError("Image URL must start with http:// or https://");
            return false;
        }
        
        // Get ingredients
        List<String> ingredients = new ArrayList<>();
        for (View view : ingredientViews) {
            EditText editText = view.findViewById(R.id.edit_text);
            String ingredient = editText.getText().toString().trim();
            if (!ingredient.isEmpty()) {
                ingredients.add(ingredient);
            }
        }
        
        if (ingredients.isEmpty()) {
            Toast.makeText(requireContext(), "At least one ingredient is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Get steps
        List<String> steps = new ArrayList<>();
        for (View view : stepViews) {
            EditText editText = view.findViewById(R.id.edit_text);
            String step = editText.getText().toString().trim();
            if (!step.isEmpty()) {
                steps.add(step);
            }
        }
        
        if (steps.isEmpty()) {
            Toast.makeText(requireContext(), "At least one step is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Create a weak reference to the dialog to safely dismiss it later
        final AlertDialog progressDialog;
        if (getActivity() != null && !isDetached()) {
            progressDialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Saving Recipe")
                    .setMessage("Please wait...")
                    .setCancelable(false)
                    .create();
            progressDialog.show();
            
            // Add a timeout handler to dismiss dialog if it takes too long
            android.os.Handler timeoutHandler = new android.os.Handler();
            timeoutHandler.postDelayed(() -> {
                if (progressDialog != null && progressDialog.isShowing() && getActivity() != null) {
                    try {
                        progressDialog.dismiss();
                        Toast.makeText(getActivity(), "Save operation timed out, but your recipe might have been saved", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }, 10000); // 10 second timeout
        } else {
            // Can't show dialog if detached
            progressDialog = null;
        }
        
        // Create or update recipe
        boolean isNew = (recipe == null);
        if (isNew) {
            // Create new recipe with smaller data if possible
            String cleanImageUrl = imageUrl.trim();
            recipe = new Recipe(
                    UUID.randomUUID().toString(),
                    name,
                    cleanImageUrl,
                    ingredients,
                    steps,
                    currentUser.getUid()
            );
            
            // Store recipe in final variable to avoid modification issues
            final Recipe savedRecipe = recipe;
            final AlertDialog finalProgressDialog = progressDialog;
            
            // Save to Firestore with a shorter timeout
            db.collection("recipes")
                    .document(recipe.getId())
                    .set(recipe)
                    .addOnSuccessListener(aVoid -> {
                        // Ensure dialog is dismissed first, regardless of fragment state
                        if (finalProgressDialog != null && finalProgressDialog.isShowing()) {
                            try {
                                finalProgressDialog.dismiss();
                            } catch (Exception e) {
                                // Ignore dismiss errors
                            }
                        }
                        
                        // Then update UI if possible
                        if (getActivity() != null && !isDetached() && isAdded()) {
                            if (listener != null) {
                                listener.onRecipeSaved(savedRecipe, true);
                            }
                            Toast.makeText(getActivity(), "Recipe added successfully", Toast.LENGTH_SHORT).show();
                        } else if (listener != null) {
                            // If detached but listener still exists, notify without UI updates
                            listener.onRecipeSaved(savedRecipe, true);
                        }
                        
                        // Ensure the dialog is dismissed
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        // Always dismiss dialog first
                        if (finalProgressDialog != null && finalProgressDialog.isShowing()) {
                            try {
                                finalProgressDialog.dismiss();
                            } catch (Exception ex) {
                                // Ignore dismiss errors
                            }
                        }
                        
                        if (getActivity() != null && !isDetached() && isAdded()) {
                            Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        
                        // Ensure the dialog is dismissed
                        dismiss();
                    });
        } else {
            // Update existing recipe
            recipe.setName(name);
            recipe.setImageUrl(imageUrl.trim());
            recipe.setIngredients(ingredients);
            recipe.setSteps(steps);
            
            // Store recipe in final variable to avoid modification issues
            final Recipe updatedRecipe = recipe;
            final AlertDialog finalProgressDialog = progressDialog;
            
            // Update in Firestore
            db.collection("recipes")
                    .document(recipe.getId())
                    .set(recipe)
                    .addOnSuccessListener(aVoid -> {
                        // Always dismiss dialog first
                        if (finalProgressDialog != null && finalProgressDialog.isShowing()) {
                            try {
                                finalProgressDialog.dismiss();
                            } catch (Exception e) {
                                // Ignore dismiss errors
                            }
                        }
                        
                        // Then update UI if possible
                        if (getActivity() != null && !isDetached() && isAdded()) {
                            if (listener != null) {
                                listener.onRecipeSaved(updatedRecipe, false);
                            }
                            Toast.makeText(getActivity(), "Recipe updated successfully", Toast.LENGTH_SHORT).show();
                        } else if (listener != null) {
                            // If detached but listener still exists, notify without UI updates
                            listener.onRecipeSaved(updatedRecipe, false);
                        }
                        
                        // Ensure the dialog is dismissed
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        // Always dismiss dialog first
                        if (finalProgressDialog != null && finalProgressDialog.isShowing()) {
                            try {
                                finalProgressDialog.dismiss();
                            } catch (Exception ex) {
                                // Ignore dismiss errors
                            }
                        }
                        
                        if (getActivity() != null && !isDetached() && isAdded()) {
                            Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        
                        // Ensure the dialog is dismissed
                        dismiss();
                    });
        }
        
        return true;
    }
} 