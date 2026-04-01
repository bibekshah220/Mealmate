package com.example.mealmate.models;

import java.io.Serializable;

public class GroceryItem implements Serializable {
    private String id;
    private String name;
    private double quantity;
    private String unit;
    private double price;
    private boolean purchased;
    private String userId;
    private boolean addedManually;
    private String recipeId; // Optional - if this item came from a recipe
    
    // Empty constructor for Firestore
    public GroceryItem() {
    }
    
    public GroceryItem(String id, String name, double quantity, String unit, double price, boolean purchased, String userId, String recipeId) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.purchased = purchased;
        this.userId = userId;
        this.recipeId = recipeId;
        this.addedManually = false;
    }
    
    // Getters and Setters
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
    
    public double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public boolean isPurchased() {
        return purchased;
    }
    
    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getRecipeId() {
        return recipeId;
    }
    
    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }
    
    public boolean isAddedManually() {
        return addedManually;
    }
    
    public void setAddedManually(boolean addedManually) {
        this.addedManually = addedManually;
    }
} 