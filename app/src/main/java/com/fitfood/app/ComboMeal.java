package com.fitfood.app;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ComboMeal {

    private String comboId;
    private String comboName;
    private String displayName;
    private String mealType;
    private String timePreference;   // "Morning", "Afternoon", "Evening"
    private final List<ComboItem> items;
    private double totalCalories;
    private double totalGrams;
    private double totalProtein;
    private double totalCarbs;
    private double totalFat;
    private String primaryImage;
    private final long generatedAt;
    private String generationStrategy;
    private double nutritionalScore;
    private double calorieAccuracy;
    private String[] categoryCoverage;
    private boolean wasAccepted;
    private boolean wasModified;
    private String userFeedback;
    private int timesShown;

    public static class ComboItem {
        private final FoodItem foodItem;
        private final double portionGrams;
        private double calculatedCalories;
        private double calculatedProtein;
        private double calculatedCarbs;
        private double calculatedFat;
        private boolean isRequired;
        private String portionReason;

        public ComboItem(FoodItem foodItem, double portionGrams) {
            this.foodItem = foodItem;
            this.portionGrams = portionGrams;
            this.isRequired = true;
            calculateNutrition();
        }

        private void calculateNutrition() {
            if (foodItem != null && foodItem.getGrams() > 0) {
                double ratio = portionGrams / foodItem.getGrams();
                this.calculatedCalories = foodItem.getKcal() * ratio;
                this.calculatedProtein = foodItem.getProtein() * ratio;
                this.calculatedCarbs = foodItem.getCarbs() * ratio;
                this.calculatedFat = foodItem.getFat() * ratio;
            }
        }

        public FoodItem getFoodItem() { return foodItem; }
        public double getPortionGrams() { return portionGrams; }
        public double getCalculatedCalories() { return calculatedCalories; }
        public double getCalculatedProtein() { return calculatedProtein; }
        public double getCalculatedCarbs() { return calculatedCarbs; }
        public double getCalculatedFat() { return calculatedFat; }
        public boolean isRequired() { return isRequired; }
        public void setRequired(boolean required) { this.isRequired = required; }
        public String getPortionReason() { return portionReason; }
        public void setPortionReason(String reason) { this.portionReason = reason; }

        public String getDisplayText() {
            return String.format(Locale.getDefault(), "%s (%.0fg) - %.0f kcal",
                    foodItem.getName(), portionGrams, calculatedCalories);
        }

        public String getDetailedDisplayText() {
            return String.format(Locale.getDefault(), "%s\n%.0fg • %.0f kcal • P:%.1fg C:%.1fg F:%.1fg",
                    foodItem.getName(), portionGrams, calculatedCalories,
                    calculatedProtein, calculatedCarbs, calculatedFat);
        }
    }

    public ComboMeal() {
        this.items = new ArrayList<>();
        this.generatedAt = System.currentTimeMillis();
        this.comboId = "combo_" + System.currentTimeMillis();
        this.nutritionalScore = 0;
        this.calorieAccuracy = 0;
        this.timesShown = 0;
        this.timePreference = determineTimePreference();
    }

    private String determineTimePreference() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) return "Morning";
        if (hour >= 11 && hour < 17) return "Afternoon";
        return "Evening";
    }

    public void addItem(ComboItem item) {
        items.add(item);
        recalculateTotals();
    }

    public void recalculateTotals() {
        totalCalories = 0;
        totalGrams = 0;
        totalProtein = 0;
        totalCarbs = 0;
        totalFat = 0;

        Set<String> categories = new HashSet<>();

        for (ComboItem item : items) {
            totalCalories += item.getCalculatedCalories();
            totalGrams += item.getPortionGrams();
            totalProtein += item.getCalculatedProtein();
            totalCarbs += item.getCalculatedCarbs();
            totalFat += item.getCalculatedFat();

            if (item.getFoodItem() != null) {
                categories.add(item.getFoodItem().getCategory());
            }
        }

        this.categoryCoverage = categories.toArray(new String[0]);
    }

    public boolean isValid() {
        return items.size() >= MIN_ITEMS && items.size() <= MAX_ITEMS && totalCalories > 0;
    }

    public boolean isNutritionallyBalanced() {
        if (totalCalories <= 0) return false;

        double proteinRatio = (totalProtein * 4) / totalCalories;
        double carbRatio = (totalCarbs * 4) / totalCalories;
        double fatRatio = (totalFat * 9) / totalCalories;

        return proteinRatio >= 0.10 && proteinRatio <= 0.35 &&
                carbRatio >= 0.45 && carbRatio <= 0.65 &&
                fatRatio >= 0.20 && fatRatio <= 0.35;
    }

    public Map<String, Object> toFeedbackMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("comboId", comboId);
        map.put("wasAccepted", wasAccepted);
        map.put("wasModified", wasModified);
        map.put("userFeedback", userFeedback);
        map.put("nutritionalScore", nutritionalScore);
        map.put("totalCalories", totalCalories);
        map.put("itemCount", items.size());
        map.put("generatedAt", generatedAt);
        return map;
    }

    // Getters and setters
    public String getComboId() { return comboId; }
    public void setComboId(String comboId) { this.comboId = comboId; }
    public String getComboName() { return comboName; }
    public void setComboName(String comboName) { this.comboName = comboName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getTimePreference() { return timePreference; }
    public void setTimePreference(String timePreference) { this.timePreference = timePreference; }
    public List<ComboItem> getItems() { return new ArrayList<>(items); }
    public double getTotalCalories() { return totalCalories; }
    public double getTotalGrams() { return totalGrams; }
    public double getTotalProtein() { return totalProtein; }
    public double getTotalCarbs() { return totalCarbs; }
    public double getTotalFat() { return totalFat; }
    public double getNutritionalScore() { return nutritionalScore; }
    public void setNutritionalScore(double score) { this.nutritionalScore = score; }
    public double getCalorieAccuracy() { return calorieAccuracy; }
    public void setCalorieAccuracy(double accuracy) { this.calorieAccuracy = accuracy; }
    public String[] getCategoryCoverage() { return categoryCoverage; }
    public boolean wasAccepted() { return wasAccepted; }
    public void setAccepted(boolean accepted) { this.wasAccepted = accepted; }
    public String getPrimaryImage() {
        if (primaryImage != null && !primaryImage.isEmpty()) return primaryImage;
        // Prioritize main dish (Protein, Meat, Fish) for cover image
        for (ComboItem item : items) {
            String cat = item.getFoodItem().getCategory();
            if (cat != null && (cat.equalsIgnoreCase("Protein") || cat.equalsIgnoreCase("Meat") || cat.equalsIgnoreCase("Fish"))) {
                String img = item.getFoodItem().getImage();
                if (img != null && !img.isEmpty()) return img;
            }
        }
        // Fallback to first item with image
        for (ComboItem item : items) {
            String img = item.getFoodItem().getImage();
            if (img != null && !img.isEmpty()) return img;
        }
        return null;
    }
    public void setPrimaryImage(String primaryImage) { this.primaryImage = primaryImage; }
    public long getGeneratedAt() { return generatedAt; }
    public String getGenerationStrategy() { return generationStrategy; }
    public void setGenerationStrategy(String strategy) { this.generationStrategy = strategy; }

    private static final int MIN_ITEMS = 2;
    private static final int MAX_ITEMS = 4;
}