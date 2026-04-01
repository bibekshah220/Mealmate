package com.example.mealmate.models;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MealPlan implements Serializable {
    private String id;
    private String userId;
    private Date date;
    private Map<String, String> breakfast; // recipeId -> recipeName
    private Map<String, String> lunch;
    private Map<String, String> dinner;
    private Map<String, String> snacks;
    
    // Empty constructor for Firestore
    public MealPlan() {
        breakfast = new HashMap<>();
        lunch = new HashMap<>();
        dinner = new HashMap<>();
        snacks = new HashMap<>();
    }
    
    public MealPlan(String id, String userId, Date date) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.breakfast = new HashMap<>();
        this.lunch = new HashMap<>();
        this.dinner = new HashMap<>();
        this.snacks = new HashMap<>();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public Map<String, String> getBreakfast() {
        return breakfast;
    }
    
    public void setBreakfast(Map<String, String> breakfast) {
        this.breakfast = breakfast != null ? breakfast : new HashMap<>();
    }
    
    public Map<String, String> getLunch() {
        return lunch;
    }
    
    public void setLunch(Map<String, String> lunch) {
        this.lunch = lunch != null ? lunch : new HashMap<>();
    }
    
    public Map<String, String> getDinner() {
        return dinner;
    }
    
    public void setDinner(Map<String, String> dinner) {
        this.dinner = dinner != null ? dinner : new HashMap<>();
    }
    
    public Map<String, String> getSnacks() {
        return snacks;
    }
    
    public void setSnacks(Map<String, String> snacks) {
        this.snacks = snacks != null ? snacks : new HashMap<>();
    }
    
    // Helper methods
    public void addBreakfastRecipe(String recipeId, String recipeName) {
        if (breakfast == null) {
            breakfast = new HashMap<>();
        }
        breakfast.put(recipeId, recipeName);
    }
    
    public void addLunchRecipe(String recipeId, String recipeName) {
        if (lunch == null) {
            lunch = new HashMap<>();
        }
        lunch.put(recipeId, recipeName);
    }
    
    public void addDinnerRecipe(String recipeId, String recipeName) {
        if (dinner == null) {
            dinner = new HashMap<>();
        }
        dinner.put(recipeId, recipeName);
    }
    
    public void addSnackRecipe(String recipeId, String recipeName) {
        if (snacks == null) {
            snacks = new HashMap<>();
        }
        snacks.put(recipeId, recipeName);
    }
    
    public void removeBreakfastRecipe(String recipeId) {
        if (breakfast != null) {
            breakfast.remove(recipeId);
        }
    }
    
    public void removeLunchRecipe(String recipeId) {
        if (lunch != null) {
            lunch.remove(recipeId);
        }
    }
    
    public void removeDinnerRecipe(String recipeId) {
        if (dinner != null) {
            dinner.remove(recipeId);
        }
    }
    
    public void removeSnackRecipe(String recipeId) {
        if (snacks != null) {
            snacks.remove(recipeId);
        }
    }
} 