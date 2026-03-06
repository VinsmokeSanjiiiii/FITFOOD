package com.fitfood.app;

public class User {
    private String uid;
    private String name;
    private String email;
    private String birthday;
    private String role;
    private String profileImageUrl;
    private String goal;
    private Double weight;
    private Double height;
    private Double dailyCalories;

    public User() { }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }
    public Double getDailyCalories() { return dailyCalories; }
    public void setDailyCalories(Double dailyCalories) { this.dailyCalories = dailyCalories; }
}