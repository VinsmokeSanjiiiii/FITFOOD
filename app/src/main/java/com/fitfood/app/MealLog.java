package com.fitfood.app;

public class MealLog {

    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String mealType;
    private String mealName;
    private int calories;
    private String mealDate;
    private String foodImage;

    public MealLog() { }

    public MealLog(String id, String userId, String userName, String mealType, String mealName, int calories, String mealDate) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.mealType = mealType;
        this.mealName = mealName;
        this.calories = calories;
        this.mealDate = mealDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }
    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }
    public String getMealDate() { return mealDate; }
    public void setMealDate(String mealDate) { this.mealDate = mealDate; }
    public void setDate(String date) { this.mealDate = date; }
    public String getDate() { return mealDate; }
    public String getFoodImage() { return foodImage; }
    public void setFoodImage(String foodImage) { this.foodImage = foodImage; }
}