package com.example.mealmate.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.mealmate.R;
import com.example.mealmate.adapters.RecipeAdapter;
import com.example.mealmate.models.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipesFragment extends Fragment implements RecipeAdapter.OnRecipeListener, RecipeDialogFragment.RecipeDialogListener {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private List<Recipe> recipeList;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipes, container, false);

        // Initialize Firestore and get current user
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize views
        recyclerView = view.findViewById(R.id.recipes_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        FloatingActionButton fabAddRecipe = view.findViewById(R.id.fab_add_recipe);

        // Set up RecyclerView
        recipeList = new ArrayList<>();
        adapter = new RecipeAdapter(getContext(), recipeList, this);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        // Set up FloatingActionButton
        fabAddRecipe.setOnClickListener(v -> {
            showAddRecipeDialog();
        });

        // Load recipes from Firestore
        loadRecipes();

        return view;
    }

    private void loadRecipes() {
        if (currentUser == null) {
            updateEmptyView(true);
            return;
        }

        showLoading(true);
        
        // Create a local variable to store new recipes
        List<Recipe> newRecipeList = new ArrayList<>();

        db.collection("recipes")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Recipe recipe = document.toObject(Recipe.class);
                                if (recipe != null) {
                                    // Make sure ID is set
                                    recipe.setId(document.getId());
                                    newRecipeList.add(recipe);
                                }
                            } catch (Exception e) {
                                // Log any errors during conversion but continue processing other recipes
                                Toast.makeText(getContext(), "Error processing a recipe", Toast.LENGTH_SHORT).show();
                            }
                        }
                        
                        // Update the UI on the main thread
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // Clear and update the list
                                recipeList.clear();
                                recipeList.addAll(newRecipeList);
                                adapter.notifyDataSetChanged();
                                showLoading(false);
                                updateEmptyView(recipeList.isEmpty());
                            });
                        } else {
                            // Fallback if fragment is detached
                            showLoading(false);
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error loading recipes: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                        showLoading(false);
                        updateEmptyView(true);
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar == null || recyclerView == null || emptyView == null || !isAdded()) {
            return;
        }
        
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyView(boolean isEmpty) {
        if (emptyView == null || recyclerView == null || !isAdded()) {
            return;
        }
        
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddRecipeDialog() {
        RecipeDialogFragment dialogFragment = RecipeDialogFragment.newInstance(null);
        dialogFragment.setRecipeDialogListener(this);
        dialogFragment.show(getParentFragmentManager(), "RecipeDialog");
    }

    private void showEditRecipeDialog(Recipe recipe) {
        RecipeDialogFragment dialogFragment = RecipeDialogFragment.newInstance(recipe);
        dialogFragment.setRecipeDialogListener(this);
        dialogFragment.show(getParentFragmentManager(), "RecipeDialog");
    }

    private void deleteRecipe(int position) {
        Recipe recipe = recipeList.get(position);
        
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_recipe)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    db.collection("recipes").document(recipe.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                recipeList.remove(position);
                                adapter.notifyItemRemoved(position);
                                updateEmptyView(recipeList.isEmpty());
                                Toast.makeText(getContext(), R.string.recipe_deleted, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), R.string.error_deleting, Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onRecipeClick(int position) {
        Recipe recipe = recipeList.get(position);
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RecipeDetailsFragment.newInstance(recipe))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onEditClick(int position) {
        showEditRecipeDialog(recipeList.get(position));
    }

    @Override
    public void onDeleteClick(int position) {
        deleteRecipe(position);
    }

    @Override
    public void onRecipeSaved(Recipe recipe, boolean isNew) {
        if (recipe == null) {
            return;
        }
        
        // Immediate UI update first
        if (isNew) {
            // Add to the beginning for better visibility
            recipeList.add(0, recipe);
            adapter.notifyItemInserted(0);
            updateEmptyView(false);
        } else {
            // Update existing recipe
            boolean found = false;
            for (int i = 0; i < recipeList.size(); i++) {
                if (recipeList.get(i).getId().equals(recipe.getId())) {
                    recipeList.set(i, recipe);
                    adapter.notifyItemChanged(i);
                    found = true;
                    break;
                }
            }
            
            // If not found in list (rare case), add it
            if (!found) {
                recipeList.add(0, recipe);
                adapter.notifyItemInserted(0);
            }
        }
        
        // Schedule a delayed refresh to ensure we have the latest data
        // but don't interrupt the user's immediate experience
        new android.os.Handler().postDelayed(this::loadRecipes, 2000);
    }
} 