package com.fitfood.app;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.util.List;

public class FoodItem {
    @Exclude
    private String id;
    private String name;
    private double grams;
    private double kcal;
    private double protein;   // in grams
    private double carbs;     // in grams
    private double fat;       // in grams
    private String category;
    private String image;
    private String description;
    private String ingredients;
    private List<String> mealType;
    private transient double suggestedGrams;

    public FoodItem() { }

    public FoodItem(String name, double grams, double kcal, double protein, double carbs, double fat,
                    String category, String image, String description,
                    List<String> mealType, double suggestedGrams, String ingredients) {
        this.name = name;
        this.grams = grams;
        this.kcal = kcal;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.category = category;
        this.image = image;
        this.description = description;
        this.mealType = mealType;
        this.suggestedGrams = suggestedGrams;
        this.ingredients = ingredients;
    }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getGrams() { return grams; }
    public void setGrams(double grams) { this.grams = grams; }

    @PropertyName("kcal")
    public double getKcal() { return kcal; }
    @PropertyName("kcal")
    public void setKcal(double kcal) { this.kcal = kcal; }

    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }

    public double getCarbs() { return carbs; }
    public void setCarbs(double carbs) { this.carbs = carbs; }

    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public List<String> getMealType() { return mealType; }
    public void setMealType(List<String> mealType) { this.mealType = mealType; }

    @Exclude
    public double getSuggestedGrams() { return suggestedGrams; }
    @Exclude
    public void setSuggestedGrams(double suggestedGrams) { this.suggestedGrams = suggestedGrams; }

    // Legacy field mappings (keep for backward compatibility)
    @PropertyName("calories")
    public void setCalories(double val) { this.kcal = val; }
    public double getCalories() { return kcal; }

    @PropertyName("sugesstedGrams")
    public void setSugesstedGrams(double val) { this.suggestedGrams = val; }
    @PropertyName("suggesstedGrams")
    public void setSuggesstedGrams(double val) { this.suggestedGrams = val; }

    @PropertyName("category ")
    public void setCategoryWithSpace(String val) { this.category = val; }
    @PropertyName("categories")
    public void setCategories(String val) { this.category = val; }

    @PropertyName("image ")
    public void setImageWithSpace(String val) { this.image = val; }
    @PropertyName("description ")
    public void setDescriptionWithSpace(String val) { this.description = val; }
    @PropertyName("descripton")
    public void setDescripton(String val) { this.description = val; }
}