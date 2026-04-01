package com.example.mealmate.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Recipe implements Serializable {
    private String id;
    private String name;
    private String imageUrl;
    private List<String> ingredients;
    private List<String> steps;
    private String userId;
    
    // Empty constructor for Firestore
    public Recipe() {
        ingredients = new ArrayList<>();
        steps = new ArrayList<>();
    }
    
    public Recipe(String id, String name, String imageUrl, List<String> ingredients, List<String> steps, String userId) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        this.steps = steps != null ? steps : new ArrayList<>();
        this.userId = userId;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public List<String> getIngredients() {
        return ingredients;
    }
    
    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }
    
    public List<String> getSteps() {
        return steps;
    }
    
    public void setSteps(List<String> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
} 