package com.fitfood.app;

import android.os.Build;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ComboMealGenerator {

    private static final int MIN_ITEMS = 2;
    private static final int MAX_ITEMS = 4;
    private static final int MAX_GENERATION_ATTEMPTS = 50;
    private static final double PROTEIN_TARGET_MIN = 0.20;
    private static final double PROTEIN_TARGET_MAX = 0.35;
    private static final double CARBS_TARGET_MIN = 0.40;
    private static final double CARBS_TARGET_MAX = 0.60;
    private static final double FAT_TARGET_MIN = 0.15;
    private static final double FAT_TARGET_MAX = 0.30;

    private static final Map<String, Double> MEAL_CALORIE_SHARE = new HashMap<>();
    static {
        MEAL_CALORIE_SHARE.put("Breakfast", 0.25);
        MEAL_CALORIE_SHARE.put("Lunch", 0.35);
        MEAL_CALORIE_SHARE.put("Dinner", 0.30);
        MEAL_CALORIE_SHARE.put("Snacks", 0.10);
    }

    private static final Map<String, List<CategoryPriority>> MEAL_CATEGORY_PRIORITIES = new HashMap<>();

    static {
        MEAL_CATEGORY_PRIORITIES.put("Breakfast", Arrays.asList(
                new CategoryPriority("Carbohydrate", 2, "Primary Energy"),      // Essential
                new CategoryPriority("Protein", 2, "Sustained Fullness"),       // Essential
                new CategoryPriority("Dairy", 1, "Calcium Boost"),              // Optional
                new CategoryPriority("Fruit", 1, "Vitamin C"),                  // Optional
                new CategoryPriority("Fat", 0, "Cooking Medium")                // Avoid heavy fats
        ));

        MEAL_CATEGORY_PRIORITIES.put("Lunch", Arrays.asList(
                new CategoryPriority("Carbohydrate", 2, "Energy Base"),         // Essential
                new CategoryPriority("Protein", 2, "Muscle Maintenance"),       // Essential
                new CategoryPriority("Vegetable", 2, "Fiber & Vitamins"),       // Highly recommended
                new CategoryPriority("Fat", 1, "Satiety"),                      // Moderate
                new CategoryPriority("Fruit", 1, "Digestion")                   // Optional dessert
        ));

        MEAL_CATEGORY_PRIORITIES.put("Dinner", Arrays.asList(
                new CategoryPriority("Protein", 2, "Overnight Repair"),         // Essential
                new CategoryPriority("Vegetable", 2, "Fiber & Low Cal"),        // Essential
                new CategoryPriority("Carbohydrate", 1, "Light Energy"),        // Reduced carbs
                new CategoryPriority("Fat", 1, "Essential Fatty Acids"),        // Moderate
                new CategoryPriority("Fruit", 0, "Optional")                    // Avoid (sugar before sleep)
        ));

        MEAL_CATEGORY_PRIORITIES.put("Snacks", Arrays.asList(
                new CategoryPriority("Fruit", 2, "Natural Sweetness"),          // Preferred
                new CategoryPriority("Protein", 2, "Satiety"),                  // Good option
                new CategoryPriority("Dairy", 1, "Calcium Snack"),              // Moderate
                new CategoryPriority("Carbohydrate", 1, "Quick Energy"),        // Controlled
                new CategoryPriority("Fat", 0, "Avoid heavy")                   // Minimize
        ));
    }

    private static final Map<String, Set<String>> INCOMPATIBLE_PAIRS = new HashMap<>();
    static {
        INCOMPATIBLE_PAIRS.put("Dessert", new HashSet<>(Arrays.asList("Beef", "Pork", "Fish")));
        INCOMPATIBLE_PAIRS.put("Heavy Meat", new HashSet<>(List.of("Clear Soup")));
    }

    private static final Map<String, Set<String>> SYNERGY_PAIRS = new HashMap<>();
    static {
        SYNERGY_PAIRS.put("Rice", new HashSet<>(Arrays.asList("Chicken", "Beef", "Pork", "Fish", "Egg")));
        SYNERGY_PAIRS.put("Bread", new HashSet<>(Arrays.asList("Egg", "Cheese", "Chicken")));
        SYNERGY_PAIRS.put("Noodles", new HashSet<>(Arrays.asList("Beef", "Pork", "Egg", "Vegetables")));
    }

    private static final Random random = new Random();
    private static class CategoryPriority {
        final String category;
        final int priority;
        final String reason;

        CategoryPriority(String category, int priority, String reason) {
            this.category = category;
            this.priority = priority;
            this.reason = reason;
        }
    }

    private static class ScoredCombo {
        final ComboMeal combo;
        final double nutritionalScore;
        final double varietyScore;
        final double synergyScore;
        final double calorieScore;
        final double totalScore;

        ScoredCombo(ComboMeal combo, double nutritionalScore, double varietyScore,
                    double synergyScore, double calorieScore) {
            this.combo = combo;
            this.nutritionalScore = nutritionalScore;
            this.varietyScore = varietyScore;
            this.synergyScore = synergyScore;
            this.calorieScore = calorieScore;
            this.totalScore = (nutritionalScore * 0.35) + (varietyScore * 0.25) +
                    (synergyScore * 0.25) + (calorieScore * 0.15);
        }
    }

    public static ComboMeal generateCombo(List<FoodItem> availableFoods,
                                          String mealType,
                                          String userGoal) {
        return generateOptimalCombo(
                availableFoods,
                mealType,
                userGoal,
                2000,
                null,
                null
        );
    }

    public static ComboMeal generateOptimalCombo(List<FoodItem> availableFoods,
                                                 String mealType,
                                                 String userGoal,
                                                 double dailyCalorieTarget,
                                                 List<String> userPreferences,
                                                 List<String> recentCombos) {

        if (availableFoods == null || availableFoods.size() < MIN_ITEMS) {
            Log.w("ComboGenerator", "Insufficient foods available: " +
                    (availableFoods == null ? "null" : availableFoods.size()));
            return createFallbackCombo(availableFoods);
        }

        List<FoodItem> compatibleFoods = filterByMealType(availableFoods, mealType);
        if (compatibleFoods.size() < MIN_ITEMS) {
            Log.w("ComboGenerator", "Insufficient compatible foods for " + mealType);
            compatibleFoods = new ArrayList<>(availableFoods);
        }

        double mealCalorieShare = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mealCalorieShare = MEAL_CALORIE_SHARE.getOrDefault(mealType, 0.25);
        }
        double targetMealCalories = dailyCalorieTarget * mealCalorieShare;

        targetMealCalories = adjustForGoal(targetMealCalories, userGoal, mealType);
        List<ScoredCombo> candidates = new ArrayList<>();
        Set<String> attemptedSignatures = new HashSet<>();

        for (int i = 0; i < MAX_GENERATION_ATTEMPTS && candidates.size() < 10; i++) {
            ComboMeal combo = generateSingleCombo(compatibleFoods, mealType, userGoal, targetMealCalories);

            if (combo != null && combo.isValid()) {
                String signature = getComboSignature(combo);

                if (!attemptedSignatures.contains(signature) &&
                        (recentCombos == null || !recentCombos.contains(signature))) {

                    attemptedSignatures.add(signature);

                    double nutScore = calculateNutritionalScore(combo, targetMealCalories);
                    double varScore = calculateVarietyScore(combo);
                    double synScore = calculateSynergyScore(combo);
                    double calScore = calculateCalorieScore(combo, targetMealCalories);

                    candidates.add(new ScoredCombo(combo, nutScore, varScore, synScore, calScore));
                }
            }
        }

        if (candidates.isEmpty()) {
            return createFallbackCombo(compatibleFoods);
        }

        Collections.sort(candidates, (a, b) -> Double.compare(b.totalScore, a.totalScore));
        Log.d("ComboGenerator", "Top combo score: " + candidates.get(0).totalScore +
                " (nut=" + candidates.get(0).nutritionalScore +
                ", var=" + candidates.get(0).varietyScore +
                ", syn=" + candidates.get(0).synergyScore +
                ", cal=" + candidates.get(0).calorieScore + ")");

        return candidates.get(0).combo;
    }

    private static ComboMeal generateSingleCombo(List<FoodItem> foods,
                                                 String mealType,
                                                 String userGoal,
                                                 double targetCalories) {

        ComboMeal combo = new ComboMeal();
        combo.setMealType(mealType);
        combo.setGenerationStrategy("intelligent_scored");

        List<CategoryPriority> priorities = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            priorities = MEAL_CATEGORY_PRIORITIES.getOrDefault(
                    mealType, MEAL_CATEGORY_PRIORITIES.get("Lunch"));
        }

        Map<String, List<FoodItem>> foodsByCategory = groupByCategory(foods);
        Set<String> selectedCategories = new HashSet<>();
        Set<String> selectedFoodIds = new HashSet<>();

        assert priorities != null;
        for (CategoryPriority cp : priorities) {
            if (cp.priority >= 2 && combo.getItems().size() < MAX_ITEMS) {
                FoodItem selected = selectBestFromCategory(
                        foodsByCategory.get(cp.category),
                        targetCalories,
                        combo,
                        selectedFoodIds
                );

                if (selected != null) {
                    addToComboWithPortion(combo, selected, userGoal, targetCalories);
                    selectedCategories.add(cp.category);
                    selectedFoodIds.add(selected.getId());
                }
            }
        }

        List<CategoryPriority> optionalPriorities = new ArrayList<>();
        for (CategoryPriority cp : priorities) {
            if (cp.priority == 1 && !selectedCategories.contains(cp.category)) {
                optionalPriorities.add(cp);
            }
        }
        Collections.shuffle(optionalPriorities);

        for (CategoryPriority cp : optionalPriorities) {
            if (combo.getItems().size() >= MAX_ITEMS) break;
            if (combo.getItems().size() >= MIN_ITEMS && random.nextDouble() > 0.7) continue;

            FoodItem selected = selectBestFromCategory(
                    foodsByCategory.get(cp.category),
                    targetCalories,
                    combo,
                    selectedFoodIds
            );

            if (selected != null) {
                addToComboWithPortion(combo, selected, userGoal, targetCalories);
                selectedCategories.add(cp.category);
                selectedFoodIds.add(selected.getId());
            }
        }

        while (combo.getItems().size() < MIN_ITEMS) {
            FoodItem filler = selectRandomCompatible(foods, selectedFoodIds, combo);
            if (filler == null) break;
            addToComboWithPortion(combo, filler, userGoal, targetCalories);
            selectedFoodIds.add(filler.getId());
        }

        if (!combo.isValid()) return null;

        combo.recalculateTotals();
        generateIntelligentName(combo, selectedCategories);

        return combo;
    }

    private static FoodItem selectBestFromCategory(List<FoodItem> categoryFoods,
                                                   double targetCalories,
                                                   ComboMeal currentCombo,
                                                   Set<String> excludeIds) {
        if (categoryFoods == null || categoryFoods.isEmpty()) return null;

        List<FoodItem> candidates = new ArrayList<>();
        for (FoodItem food : categoryFoods) {
            if (excludeIds.contains(food.getId())) continue;
            if (!isCompatibleWithCombo(food, currentCombo)) continue;
            candidates.add(food);
        }

        if (candidates.isEmpty()) return null;

        double currentCalories = currentCombo.getTotalCalories();
        double remaining = targetCalories - currentCalories;
        FoodItem bestMatch = null;
        double bestScore = Double.MAX_VALUE;

        for (FoodItem food : candidates) {
            double foodCalories = estimateComboItemCalories(food);
            double score = Math.abs(foodCalories - (remaining / (MAX_ITEMS - currentCombo.getItems().size())));

            if (food.getImage() != null && !food.getImage().isEmpty()) {
                score *= 0.9;
            }

            if (score < bestScore) {
                bestScore = score;
                bestMatch = food;
            }
        }

        return bestMatch != null ? bestMatch : candidates.get(random.nextInt(candidates.size()));
    }

    private static boolean isCompatibleWithCombo(FoodItem food, ComboMeal combo) {
        for (ComboMeal.ComboItem item : combo.getItems()) {
            FoodItem existing = item.getFoodItem();
            Set<String> banned = INCOMPATIBLE_PAIRS.get(existing.getCategory());

            if (banned != null && banned.contains(food.getName())) return false;
            banned = INCOMPATIBLE_PAIRS.get(food.getCategory());
            if (banned != null && banned.contains(existing.getName())) return false;
        }

        return true;
    }

    private static double calculateNutritionalScore(ComboMeal combo, double targetCalories) {
        Set<String> categories = new HashSet<>();
        boolean hasProtein = false;
        boolean hasCarbs = false;
        boolean hasVeggie = false;

        for (ComboMeal.ComboItem item : combo.getItems()) {
            FoodItem food = item.getFoodItem();
            String cat = food.getCategory();
            categories.add(cat);

            if ("Protein".equals(cat) || "Meat".equals(cat) || "Fish".equals(cat)) hasProtein = true;
            if ("Carbohydrate".equals(cat) || "Grain".equals(cat) || "Rice".equals(cat)) hasCarbs = true;
            if ("Vegetable".equals(cat)) hasVeggie = true;
        }

        double score = categories.size() * 15;
        if (hasProtein) score += 20;
        if (hasCarbs) score += 15;
        if (hasVeggie) score += 15;

        Map<String, Integer> categoryCounts = new HashMap<>();
        for (ComboMeal.ComboItem item : combo.getItems()) {
            String cat = item.getFoodItem().getCategory();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);
            }
        }
        for (int count : categoryCounts.values()) {
            if (count > 1) score -= (count - 1) * 10;
        }

        return Math.min(100, Math.max(0, score));
    }

    private static double calculateVarietyScore(ComboMeal combo) {
        Set<String> uniqueNames = new HashSet<>();
        int hasImageCount = 0;

        for (ComboMeal.ComboItem item : combo.getItems()) {
            FoodItem food = item.getFoodItem();
            uniqueNames.add(food.getName());
            if (food.getImage() != null && !food.getImage().isEmpty()) hasImageCount++;
        }

        double score = uniqueNames.size() * 25;
        score += (hasImageCount * 5);
        return Math.min(100, score);
    }

    private static double calculateSynergyScore(ComboMeal combo) {
        List<ComboMeal.ComboItem> items = combo.getItems();
        if (items.size() < 2) return 0;

        int synergyCount = 0;
        int totalPairs = 0;

        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                String name1 = items.get(i).getFoodItem().getName();
                String name2 = items.get(j).getFoodItem().getName();
                totalPairs++;
                Set<String> synergies = SYNERGY_PAIRS.get(name1);

                if (synergies != null && synergies.contains(name2)) {
                    synergyCount++;
                    continue;
                }

                synergies = SYNERGY_PAIRS.get(name2);
                if (synergies != null && synergies.contains(name1)) {
                    synergyCount++;
                }
            }
        }

        return totalPairs > 0 ? (synergyCount * 100.0 / totalPairs) : 50;
    }

    private static double calculateCalorieScore(ComboMeal combo, double targetCalories) {
        double actualCalories = combo.getTotalCalories();
        double ratio = actualCalories / targetCalories;

        if (ratio >= 0.9 && ratio <= 1.1) return 100;
        if (ratio >= 0.8 && ratio < 0.9) return 80;
        if (ratio > 1.1 && ratio <= 1.2) return 70;
        if (ratio >= 0.7 && ratio < 0.8) return 60;
        return 40;
    }

    private static double adjustForGoal(double baseCalories, String userGoal, String mealType) {
        double adjusted = baseCalories;

        if ("weight_loss".equals(userGoal)) {
            if ("Dinner".equals(mealType)) adjusted *= 0.90;
            else adjusted *= 0.95;
        } else if ("weight_gain".equals(userGoal)) {
            adjusted *= 1.10;
        } else if ("maintenance".equals(userGoal)) {
            adjusted *= 1.0;
        }

        double minCalories = mealType.equals("Snacks") ? 100 : 300;
        return Math.max(minCalories, adjusted);
    }

    private static void addToComboWithPortion(ComboMeal combo, FoodItem food,
                                              String userGoal, double targetCalories) {
        double baseGrams = food.getSuggestedGrams() > 0 ? food.getSuggestedGrams() : food.getGrams();
        double portionGrams = baseGrams;
        double remainingCalories = targetCalories - combo.getTotalCalories();
        double estimatedFoodCalories = (food.getKcal() / food.getGrams()) * baseGrams;

        if (estimatedFoodCalories > remainingCalories * 0.6) {
            portionGrams = (remainingCalories * 0.5) / (food.getKcal() / food.getGrams());
        }

        if ("weight_loss".equals(userGoal)) {
            if ("Carbohydrate".equals(food.getCategory())) {
                portionGrams *= 0.85;
            }
        } else if ("weight_gain".equals(userGoal)) {
            portionGrams *= 1.15;
        }

        portionGrams = Math.max(50, portionGrams);
        ComboMeal.ComboItem item = new ComboMeal.ComboItem(food, portionGrams);
        combo.addItem(item);
    }

    private static void generateIntelligentName(ComboMeal combo, Set<String> categories) {
        List<ComboMeal.ComboItem> items = combo.getItems();
        if (items.isEmpty()) {
            combo.setDisplayName("Simple Combo");
            return;
        }

        StringBuilder nameBuilder = new StringBuilder();
        boolean hasRice = false, hasEgg = false, hasMeat = false;
        String meatType = "";

        for (ComboMeal.ComboItem item : items) {
            String name = item.getFoodItem().getName().toLowerCase();
            if (name.contains("rice")) hasRice = true;
            if (name.contains("egg")) hasEgg = true;
            if (name.contains("chicken") || name.contains("beef") || name.contains("pork") ||
                    name.contains("fish") || name.contains("tapa") || name.contains("tocino")) {
                hasMeat = true;
                meatType = extractMeatType(name);
            }
        }

        if (hasRice && hasEgg && hasMeat) {
            nameBuilder.append(meatType).append("silog");
        } else if (hasRice && hasMeat) {
            nameBuilder.append(meatType).append(" with Rice");
        } else if (categories.contains("Protein") && categories.contains("Vegetable")) {
            nameBuilder.append("Protein & Veggie Plate");
        } else if (categories.contains("Carbohydrate") && categories.contains("Protein")) {
            nameBuilder.append("Energy Meal");
        } else {

            int count = Math.min(2, items.size());
            for (int i = 0; i < count; i++) {
                if (i > 0) nameBuilder.append(" & ");
                String firstWord = items.get(i).getFoodItem().getName().split(" ")[0];
                nameBuilder.append(firstWord);
            }
            nameBuilder.append(" Combo");
        }

        combo.setDisplayName(nameBuilder.toString());
    }

    private static String extractMeatType(String name) {
        if (name.contains("tapa")) return "Tap";
        if (name.contains("tocino")) return "Toci";
        if (name.contains("longganisa")) return "Longsi";
        if (name.contains("bangus")) return "Bang";
        if (name.contains("chicken")) return "Chicken";
        if (name.contains("beef")) return "Beef";
        if (name.contains("pork")) return "Pork";
        return "Meat";
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
            if (category == null || category.isEmpty()) category = "Uncategorized";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(food);
            }
        }
        return grouped;
    }

    private static FoodItem selectRandomCompatible(List<FoodItem> foods,
                                                   Set<String> excludeIds,
                                                   ComboMeal combo) {
        List<FoodItem> candidates = new ArrayList<>();
        for (FoodItem food : foods) {
            if (!excludeIds.contains(food.getId()) && isCompatibleWithCombo(food, combo)) {
                candidates.add(food);
            }
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private static double estimateComboItemCalories(FoodItem food) {
        double grams = food.getSuggestedGrams() > 0 ? food.getSuggestedGrams() : food.getGrams();
        return (food.getKcal() / food.getGrams()) * grams;
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

    private static ComboMeal createFallbackCombo(List<FoodItem> foods) {
        if (foods == null || foods.size() < MIN_ITEMS) return null;

        ComboMeal combo = new ComboMeal();
        combo.setMealType("General");
        combo.setGenerationStrategy("fallback");

        for (int i = 0; i < Math.min(MAX_ITEMS, foods.size()); i++) {
            FoodItem food = foods.get(i);
            double portion = food.getSuggestedGrams() > 0 ? food.getSuggestedGrams() : food.getGrams();
            combo.addItem(new ComboMeal.ComboItem(food, portion));
        }

        combo.setDisplayName("Simple " + combo.getItems().size() + "-Item Combo");
        return combo;
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

        return generateCombo(filteredFoods, mealType, userGoal);
    }

    public static List<ComboMeal> generateComboOptions(List<FoodItem> availableFoods,
                                                       String mealType,
                                                       String userGoal,
                                                       int count) {
        List<ComboMeal> options = new ArrayList<>();
        Set<String> signatures = new HashSet<>();

        int attempts = 0;
        while (options.size() < count && attempts < count * 3) {
            ComboMeal combo = generateCombo(availableFoods, mealType, userGoal);
            if (combo != null) {
                String sig = getComboSignature(combo);
                if (!signatures.contains(sig)) {
                    options.add(combo);
                    signatures.add(sig);
                }
            }
            attempts++;
        }

        return options;
    }
}