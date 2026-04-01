package com.example.mealmate.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mealmate.R;
import com.example.mealmate.models.Recipe;

public class RecipeDetailsFragment extends Fragment {

    private static final String ARG_RECIPE = "recipe";
    
    private Recipe recipe;
    private ImageView recipeImage;
    private TextView recipeName;
    private LinearLayout ingredientsContainer;
    private LinearLayout stepsContainer;

    public static RecipeDetailsFragment newInstance(Recipe recipe) {
        RecipeDetailsFragment fragment = new RecipeDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RECIPE, recipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            recipe = (Recipe) getArguments().getSerializable(ARG_RECIPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_details, container, false);
        
        // Initialize views
        recipeImage = view.findViewById(R.id.recipe_image);
        recipeName = view.findViewById(R.id.recipe_name);
        ingredientsContainer = view.findViewById(R.id.ingredients_container);
        stepsContainer = view.findViewById(R.id.steps_container);
        
        // Display recipe details
        if (recipe != null) {
            displayRecipe();
        }
        
        return view;
    }
    
    private void displayRecipe() {
        // Set recipe name
        recipeName.setText(recipe.getName());
        
        // Load recipe image with better error handling
        try {
            if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty() && isAdded()) {
                Glide.with(requireContext())
                        .load(recipe.getImageUrl())
                        .placeholder(R.drawable.ic_recipe)
                        .error(R.drawable.ic_recipe)
                        .centerCrop()
                        .into(recipeImage);
            } else {
                recipeImage.setImageResource(R.drawable.ic_recipe);
            }
        } catch (Exception e) {
            // If Glide fails for any reason, fall back to default image
            recipeImage.setImageResource(R.drawable.ic_recipe);
        }
        
        // Display ingredients
        ingredientsContainer.removeAllViews();
        if (recipe.getIngredients() != null) {
            for (String ingredient : recipe.getIngredients()) {
                addIngredientView(ingredient);
            }
        }
        
        // Display steps
        stepsContainer.removeAllViews();
        if (recipe.getSteps() != null) {
            for (int i = 0; i < recipe.getSteps().size(); i++) {
                addStepView(i + 1, recipe.getSteps().get(i));
            }
        }
    }
    
    private void addIngredientView(String ingredient) {
        TextView textView = new TextView(requireContext());
        textView.setText("• " + ingredient);
        textView.setTextSize(16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        textView.setLayoutParams(params);
        ingredientsContainer.addView(textView);
    }
    
    private void addStepView(int stepNumber, String step) {
        TextView textView = new TextView(requireContext());
        textView.setText(stepNumber + ". " + step);
        textView.setTextSize(16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        textView.setLayoutParams(params);
        stepsContainer.addView(textView);
    }
} 