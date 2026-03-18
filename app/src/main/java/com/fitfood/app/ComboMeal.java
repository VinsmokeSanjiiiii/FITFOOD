package com.fitfood.app;

import java.util.ArrayList;
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
                this.calculatedProtein = estimateProtein() * ratio;
                this.calculatedCarbs = estimateCarbs() * ratio;
                this.calculatedFat = estimateFat() * ratio;
            }
        }

        private double estimateProtein() {
            String cat = foodItem.getCategory();
            if ("Protein".equals(cat) || "Meat".equals(cat) || "Fish".equals(cat)) {
                return foodItem.getKcal() * 0.25 / 4;
            } else if ("Dairy".equals(cat)) {
                return foodItem.getKcal() * 0.20 / 4;
            } else if ("Carbohydrate".equals(cat)) {
                return foodItem.getKcal() * 0.10 / 4;
            }
            return foodItem.getKcal() * 0.05 / 4;
        }

        private double estimateCarbs() {
            String cat = foodItem.getCategory();
            if ("Carbohydrate".equals(cat) || "Grain".equals(cat) || "Fruit".equals(cat)) {
                return foodItem.getKcal() * 0.60 / 4;
            }
            return foodItem.getKcal() * 0.30 / 4;
        }

        private double estimateFat() {
            String cat = foodItem.getCategory();
            if ("Fat".equals(cat) || "Nuts".equals(cat)) {
                return foodItem.getKcal() * 0.80 / 9;
            } else if ("Protein".equals(cat) && foodItem.getName().toLowerCase().contains("pork")) {
                return foodItem.getKcal() * 0.40 / 9;
            }
            return foodItem.getKcal() * 0.20 / 9;
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

    public String getComboId() { return comboId; }
    public void setComboId(String comboId) { this.comboId = comboId; }
    public String getComboName() { return comboName; }
    public void setComboName(String comboName) { this.comboName = comboName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
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
        if (primaryImage != null) return primaryImage;
        if (!items.isEmpty() && items.get(0).getFoodItem() != null) {
            return items.get(0).getFoodItem().getImage();
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