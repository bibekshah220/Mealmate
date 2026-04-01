package com.example.mealmate.models;

import java.util.Date;

public class ActivityItem {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String type; // recipe, meal_plan, grocery
    private Date timestamp;
    private String formattedTime;
    
    // Required empty constructor for Firestore
    public ActivityItem() {
    }
    
    public ActivityItem(String id, String userId, String title, String description, String type, Date timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.timestamp = timestamp;
    }
    
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
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getFormattedTime() {
        return formattedTime;
    }
    
    public void setFormattedTime(String formattedTime) {
        this.formattedTime = formattedTime;
    }
} 