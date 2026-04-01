package com.example.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mealmate.R;
import com.example.mealmate.models.Recipe;

import java.util.List;
import java.util.Map;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private final Context context;
    private final Map<String, String> meals; // recipeId -> recipeName
    private final List<Recipe> recipes;
    private final OnMealListener onMealListener;

    public MealAdapter(Context context, Map<String, String> meals, List<Recipe> recipes, OnMealListener onMealListener) {
        this.context = context;
        this.meals = meals;
        this.recipes = recipes;
        this.onMealListener = onMealListener;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meal, parent, false);
        return new MealViewHolder(view, onMealListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        String recipeId = getRecipeIdAtPosition(position);
        String recipeName = meals.get(recipeId);
        holder.mealName.setText(recipeName);
        
        // Find the corresponding recipe object to get the image URL
        Recipe recipe = findRecipeById(recipeId);
        
        if (recipe != null && recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            try {
                Glide.with(context)
                        .load(recipe.getImageUrl())
                        .placeholder(R.drawable.ic_recipe)
                        .error(R.drawable.ic_recipe)
                        .centerCrop()
                        .into(holder.mealImage);
            } catch (Exception e) {
                holder.mealImage.setImageResource(R.drawable.ic_recipe);
            }
        } else {
            holder.mealImage.setImageResource(R.drawable.ic_recipe);
        }
    }

    @Override
    public int getItemCount() {
        return meals != null ? meals.size() : 0;
    }

    private Recipe findRecipeById(String recipeId) {
        if (recipes != null) {
            for (Recipe recipe : recipes) {
                if (recipe.getId().equals(recipeId)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    private String getRecipeIdAtPosition(int position) {
        if (meals != null && position < meals.size()) {
            int index = 0;
            for (String recipeId : meals.keySet()) {
                if (index == position) {
                    return recipeId;
                }
                index++;
            }
        }
        return null;
    }

    public class MealViewHolder extends RecyclerView.ViewHolder {
        final ImageView mealImage;
        final TextView mealName;
        final ImageButton btnRemoveMeal;

        public MealViewHolder(@NonNull View itemView, final OnMealListener onMealListener) {
            super(itemView);
            mealImage = itemView.findViewById(R.id.meal_image);
            mealName = itemView.findViewById(R.id.meal_name);
            btnRemoveMeal = itemView.findViewById(R.id.btn_remove_meal);

            btnRemoveMeal.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onMealListener != null) {
                    onMealListener.onRemoveMeal(getRecipeIdAtPosition(position));
                }
            });

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onMealListener != null) {
                    onMealListener.onMealClick(getRecipeIdAtPosition(position));
                }
            });
        }
    }

    public interface OnMealListener {
        void onMealClick(String recipeId);
        void onRemoveMeal(String recipeId);
    }
} 