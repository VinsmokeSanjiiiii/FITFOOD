package com.fitfood.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ComboMeal {

    private String comboId;
    private String comboName;
    private String displayName;
    private String mealType;
    private final List<ComboItem> items;
    private double totalCalories;
    private double totalGrams;
    private double totalProtein;
    private String primaryImage;
    private final long generatedAt;
    private String generationStrategy;

    public static class ComboItem {
        private final FoodItem foodItem;
        private final double portionGrams;
        private double calculatedCalories;
        private boolean isRequired;

        public ComboItem(FoodItem foodItem, double portionGrams) {
            this.foodItem = foodItem;
            this.portionGrams = portionGrams;
            this.isRequired = true;
            calculateCalories();
        }

        private void calculateCalories() {
            if (foodItem != null && foodItem.getGrams() > 0) {
                double caloriesPerGram = foodItem.getKcal() / foodItem.getGrams();
                this.calculatedCalories = portionGrams * caloriesPerGram;
            }
        }

        public FoodItem getFoodItem() { return foodItem; }
        public double getPortionGrams() { return portionGrams; }
        public double getCalculatedCalories() { return calculatedCalories; }
        public boolean isRequired() { return isRequired; }
        public void setRequired(boolean required) { this.isRequired = required; }

        public String getDisplayText() {
            return String.format(Locale.getDefault(), "%s (%.0fg) - %.0f kcal",
                    foodItem.getName(), portionGrams, calculatedCalories);
        }
    }

    public ComboMeal() {
        this.items = new ArrayList<>();
        this.generatedAt = System.currentTimeMillis();
        this.comboId = "combo_" + System.currentTimeMillis();
    }

    public void addItem(ComboItem item) {
        items.add(item);
        recalculateTotals();
        generateDisplayName();
    }

    public void recalculateTotals() {
        totalCalories = 0;
        totalGrams = 0;
        for (ComboItem item : items) {
            totalCalories += item.getCalculatedCalories();
            totalGrams += item.getPortionGrams();
        }
    }

    private void generateDisplayName() {
        if (items.isEmpty()) {
            this.displayName = "Empty Combo";
            return;
        }

        StringBuilder nameBuilder = new StringBuilder();

        int count = Math.min(3, items.size());
        for (int i = 0; i < count; i++) {
            if (i > 0) nameBuilder.append(" + ");
            String itemName = items.get(i).getFoodItem().getName();
            String firstWord = itemName.split(" ")[0];
            nameBuilder.append(firstWord);
        }

        if (items.size() > 3) {
            nameBuilder.append(" +").append(items.size() - 3).append(" more");
        }

        nameBuilder.append(" Combo");
        this.displayName = nameBuilder.toString();

        generateShortName();
    }

    private void generateShortName() {
        StringBuilder shortName = new StringBuilder();
        for (ComboItem item : items) {
            String name = item.getFoodItem().getName().toLowerCase();
            if (name.contains("tapa")) shortName.append("Tapsi");
            else if (name.contains("tocino")) shortName.append("Toci");
            else if (name.contains("longganisa")) shortName.append("Longsi");
            else if (name.contains("hotdog")) shortName.append("Hot");
            else if (name.contains("bangus")) shortName.append("Bang");
            else if (name.contains("egg")) shortName.append("log");
            else if (name.contains("rice")) shortName.append("log");
        }

        if (shortName.length() > 0) {
            this.comboName = shortName.toString();
        } else {
            this.comboName = displayName;
        }
    }

    public boolean isValid() {
        return items.size() >= 2 && items.size() <= 4 && totalCalories > 0;
    }

    public String getComboId() { return comboId; }
    public void setComboId(String comboId) { this.comboId = comboId; }
    public String getComboName() { return comboName; }
    public void setComboName(String comboName) { this.comboName = comboName; }
    public String getDisplayName() { return displayName; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public List<ComboItem> getItems() { return new ArrayList<>(items); }
    public double getTotalCalories() { return totalCalories; }
    public double getTotalGrams() { return totalGrams; }

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
}