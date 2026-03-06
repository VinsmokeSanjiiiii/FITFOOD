package com.fitfood.app;

public class ProgressEntry {
    private String date;
    private double weight;
    private int calories;
    private double bmi;

    public ProgressEntry() {}

    public ProgressEntry(String date, double weight, int calories, double bmi) {
        this.date = date;
        this.weight = weight;
        this.calories = calories;
        this.bmi = bmi;
    }

    public String getDate() { return date; }
    public double getWeight() { return weight; }
    public int getCalories() { return calories; }
    public double getBmi() { return bmi; }

    public void setDate(String date) { this.date = date; }
    public void setWeight(double weight) { this.weight = weight; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setBmi(double bmi) { this.bmi = bmi; }
}
