package com.fitfood.app;

import java.util.ArrayList;
import java.util.List;

public class UserMealGroup {
    private final String userId;
    private final String userName;
    private List<MealLog> meals;
    private int totalCalories;
    private boolean isExpanded;

    public UserMealGroup(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.meals = new ArrayList<>();
        this.totalCalories = 0;
        this.isExpanded = false;
    }

    public void addMeal(MealLog meal) {
        meals.add(meal);
        totalCalories += meal.getCalories();
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public List<MealLog> getMeals() { return meals; }
    public int getTotalCalories() { return totalCalories; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    public void setMeals(List<MealLog> newMeals) {
        this.meals = newMeals;
        this.totalCalories = 0;
        for(MealLog m : newMeals) totalCalories += m.getCalories();
    }
}