package com.example.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mealmate.R;
import com.example.mealmate.models.Recipe;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private final List<Recipe> recipes;
    private final Context context;
    private final OnRecipeListener onRecipeListener;

    public RecipeAdapter(Context context, List<Recipe> recipes, OnRecipeListener onRecipeListener) {
        this.context = context;
        this.recipes = recipes;
        this.onRecipeListener = onRecipeListener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view, onRecipeListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        if (recipe == null) {
            return;
        }
        
        holder.recipeName.setText(recipe.getName() != null ? recipe.getName() : "");

        // Load recipe image with Glide - with better error handling
        try {
            if (context != null && recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
                Glide.with(holder.recipeImage.getContext())
                        .load(recipe.getImageUrl())
                        .placeholder(R.drawable.ic_recipe)
                        .error(R.drawable.ic_recipe)
                        .centerCrop()
                        .into(holder.recipeImage);
            } else {
                holder.recipeImage.setImageResource(R.drawable.ic_recipe);
            }
        } catch (Exception e) {
            // If Glide fails for any reason, fall back to default image
            holder.recipeImage.setImageResource(R.drawable.ic_recipe);
        }

        // Format ingredients summary
        StringBuilder ingredients = new StringBuilder();
        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            for (int i = 0; i < Math.min(3, recipe.getIngredients().size()); i++) {
                String ingredient = recipe.getIngredients().get(i);
                if (ingredient != null) {
                    ingredients.append("• ").append(ingredient);
                    if (i < Math.min(2, recipe.getIngredients().size() - 1)) {
                        ingredients.append("\n");
                    }
                }
            }
            if (recipe.getIngredients().size() > 3) {
                ingredients.append("\n• ...");
            }
        } else {
            ingredients.append(context.getString(R.string.no_ingredients));
        }
        holder.recipeIngredients.setText(ingredients.toString());
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView recipeImage;
        TextView recipeName;
        TextView recipeIngredients;
        ImageButton btnEdit;
        ImageButton btnDelete;

        public RecipeViewHolder(@NonNull View itemView, final OnRecipeListener onRecipeListener) {
            super(itemView);
            cardView = itemView.findViewById(R.id.recipe_card);
            recipeImage = itemView.findViewById(R.id.recipe_image);
            recipeName = itemView.findViewById(R.id.recipe_name);
            recipeIngredients = itemView.findViewById(R.id.recipe_ingredients);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);

            // Set click listeners
            cardView.setOnClickListener(v -> {
                if (onRecipeListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onRecipeListener.onRecipeClick(position);
                    }
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (onRecipeListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onRecipeListener.onEditClick(position);
                    }
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (onRecipeListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onRecipeListener.onDeleteClick(position);
                    }
                }
            });
        }
    }

    public interface OnRecipeListener {
        void onRecipeClick(int position);
        void onEditClick(int position);
        void onDeleteClick(int position);
    }
} 