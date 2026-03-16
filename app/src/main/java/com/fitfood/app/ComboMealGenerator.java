package com.fitfood.app;

import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ComboMealGenerator {

    private static final int MIN_ITEMS = 2;
    private static final int MAX_ITEMS = 4;
    private static final Random random = new Random();
    private static final Map<String, List<String>> MEAL_CATEGORY_PRIORITIES = new HashMap<>();

    static {
        // Breakfast: Carbs + Protein + Optional side
        MEAL_CATEGORY_PRIORITIES.put("Breakfast",
                List.of("Carbohydrate", "Protein", "Fruit", "Dairy", "Drink"));

        // Lunch: Complete meal - Protein + Carbs + Veggies
        MEAL_CATEGORY_PRIORITIES.put("Lunch",
                List.of("Protein", "Carbohydrate", "Vegetable", "Fruit", "Fat"));

        // Dinner: Protein + Veggies + Light carbs
        MEAL_CATEGORY_PRIORITIES.put("Dinner",
                List.of("Protein", "Vegetable", "Carbohydrate", "Fat", "Fruit"));

        // Snacks: Light + Quick
        MEAL_CATEGORY_PRIORITIES.put("Snacks",
                List.of("Fruit", "Protein", "Dairy", "Carbohydrate", "Fat"));

        // Default fallback
        MEAL_CATEGORY_PRIORITIES.put("Default",
                List.of("Protein", "Carbohydrate", "Vegetable", "Fruit", "Fat"));
    }

    public static ComboMeal generateCombo(List<FoodItem> availableFoods,
                                          String mealType,
                                          String userGoal) {

        if (availableFoods == null || availableFoods.size() < MIN_ITEMS) {
            return null;
        }

        List<FoodItem> compatibleFoods = filterByMealType(availableFoods, mealType);

        if (compatibleFoods.size() < MIN_ITEMS) {
            compatibleFoods = new ArrayList<>(availableFoods);
        }

        if (compatibleFoods.size() < MIN_ITEMS) {
            return null;
        }

        Map<String, List<FoodItem>> foodsByCategory = groupByCategory(compatibleFoods);
        List<String> categoryPriority = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            categoryPriority = MEAL_CATEGORY_PRIORITIES.getOrDefault(mealType,
                    MEAL_CATEGORY_PRIORITIES.get("Default"));
        }

        ComboMeal combo = new ComboMeal();
        combo.setMealType(mealType);
        combo.setGenerationStrategy("category_based");

        assert categoryPriority != null;
        List<FoodItem> selectedFoods = selectDiverseFoods(foodsByCategory, categoryPriority);

        for (FoodItem food : selectedFoods) {
            ComboMeal.ComboItem comboItem = createComboItem(food, userGoal);
            combo.addItem(comboItem);
        }

        return combo.isValid() ? combo : null;
    }

    public static List<ComboMeal> generateMultipleCombos(List<FoodItem> availableFoods,
                                                         String mealType,
                                                         String userGoal,
                                                         int count) {
        List<ComboMeal> combos = new ArrayList<>();
        List<String> usedSignatures = new ArrayList<>();

        int attempts = 0;
        while (combos.size() < count && attempts < count * 3) {
            ComboMeal combo = generateCombo(availableFoods, mealType, userGoal);

            if (combo != null && isUniqueCombo(combo, usedSignatures)) {
                combos.add(combo);
                usedSignatures.add(getComboSignature(combo));
            }
            attempts++;
        }

        return combos;
    }

    public static ComboMeal regenerateCombo(List<FoodItem> availableFoods,
                                            String mealType,
                                            String userGoal,
                                            ComboMeal previousCombo) {

        if (previousCombo == null || previousCombo.getItems().isEmpty()) {
            return generateCombo(availableFoods, mealType, userGoal);
        }

        List<String> excludeIds = new ArrayList<>();
        for (ComboMeal.ComboItem item : previousCombo.getItems()) {
            if (item.getFoodItem() != null && item.getFoodItem().getId() != null) {
                excludeIds.add(item.getFoodItem().getId());
            }
        }

        List<FoodItem> filteredFoods = new ArrayList<>();
        for (FoodItem food : availableFoods) {
            if (food.getId() == null || !excludeIds.contains(food.getId())) {
                filteredFoods.add(food);
            }
        }

        if (filteredFoods.size() < MIN_ITEMS) {
            for (FoodItem food : availableFoods) {
                if (!filteredFoods.contains(food) && filteredFoods.size() < MAX_ITEMS + 2) {
                    filteredFoods.add(food);
                }
            }
        }

        ComboMeal newCombo = generateCombo(filteredFoods, mealType, userGoal);
        if (newCombo == null) {
            List<FoodItem> shuffled = new ArrayList<>(availableFoods);
            Collections.shuffle(shuffled, new Random(System.nanoTime()));
            newCombo = generateCombo(shuffled, mealType, userGoal);
        }

        return newCombo;
    }


    private static List<FoodItem> filterByMealType(List<FoodItem> foods, String mealType) {
        List<FoodItem> compatible = new ArrayList<>();

        for (FoodItem food : foods) {
            List<String> mealTypes = food.getMealType();
            if (mealTypes == null || mealTypes.isEmpty()) {
                compatible.add(food);
                continue;
            }

            for (String type : mealTypes) {
                if (type.equalsIgnoreCase(mealType) || type.equalsIgnoreCase("All")) {
                    compatible.add(food);
                    break;
                }
            }
        }

        return compatible;
    }

    private static Map<String, List<FoodItem>> groupByCategory(List<FoodItem> foods) {
        Map<String, List<FoodItem>> grouped = new HashMap<>();

        for (FoodItem food : foods) {
            String category = food.getCategory();
            if (category == null || category.isEmpty()) {
                category = "Uncategorized";
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(food);
            }
        }

        return grouped;
    }

    private static List<FoodItem> selectDiverseFoods(Map<String, List<FoodItem>> foodsByCategory,
                                                     List<String> categoryPriority) {
        List<FoodItem> selected = new ArrayList<>();
        List<String> usedCategories = new ArrayList<>();

        for (String category : categoryPriority) {
            if (selected.size() >= MAX_ITEMS) break;

            List<FoodItem> foodsInCategory = foodsByCategory.get(category);
            if (foodsInCategory == null || foodsInCategory.isEmpty()) continue;

            FoodItem selectedFood = foodsInCategory.get(
                    random.nextInt(foodsInCategory.size())
            );

            if (!selected.contains(selectedFood)) {
                selected.add(selectedFood);
                usedCategories.add(category);
            }
        }

        if (selected.size() < MIN_ITEMS) {
            List<String> remainingCategories = new ArrayList<>(foodsByCategory.keySet());
            remainingCategories.removeAll(usedCategories);
            Collections.shuffle(remainingCategories);

            for (String category : remainingCategories) {
                if (selected.size() >= MIN_ITEMS) break;

                List<FoodItem> foods = foodsByCategory.get(category);
                if (foods == null || foods.isEmpty()) continue;

                FoodItem food = foods.get(random.nextInt(foods.size()));
                if (!selected.contains(food)) {
                    selected.add(food);
                }
            }
        }

        while (selected.size() < MIN_ITEMS && !selected.isEmpty()) {
            selected.add(selected.get(random.nextInt(selected.size())));
        }

        if (selected.size() > MAX_ITEMS) {
            selected = selected.subList(0, MAX_ITEMS);
        }

        Collections.shuffle(selected);

        return selected;
    }

    private static ComboMeal.ComboItem createComboItem(FoodItem food, String userGoal) {
        double portionGrams;

        if (food.getSuggestedGrams() > 0) {
            portionGrams = food.getSuggestedGrams();
        } else {
            portionGrams = food.getGrams();
        }

        if ("weight_loss".equals(userGoal)) {
            portionGrams *= 0.85;
        } else if ("weight_gain".equals(userGoal)) {
            portionGrams *= 1.15;
        }

        return new ComboMeal.ComboItem(food, portionGrams);
    }

    private static boolean isUniqueCombo(ComboMeal combo, List<String> usedSignatures) {
        String signature = getComboSignature(combo);
        return !usedSignatures.contains(signature);
    }

    private static String getComboSignature(ComboMeal combo) {
        StringBuilder sig = new StringBuilder();
        for (ComboMeal.ComboItem item : combo.getItems()) {
            if (item.getFoodItem() != null && item.getFoodItem().getId() != null) {
                sig.append(item.getFoodItem().getId()).append("|");
            }
        }
        return sig.toString();
    }
}