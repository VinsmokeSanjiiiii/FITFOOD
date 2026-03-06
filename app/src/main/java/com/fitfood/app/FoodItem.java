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
    private String category;
    private String image;
    private String description;
    private String ingredients;
    private List<String> mealType;
    private transient double suggestedGrams;

    public FoodItem() { }

    public FoodItem(String name, double grams, double kcal,
                    String category, String image, String description,
                    List<String> mealType, double suggestedGrams, String ingredients) {
        this.name = name;
        this.grams = grams;
        this.kcal = kcal;
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
    @PropertyName("sugesstedGrams")
    public void setSugesstedGrams(double val) { this.suggestedGrams = val; }

    @PropertyName("suggesstedGrams")
    public void setSuggesstedGrams(double val) { this.suggestedGrams = val; }
    @Exclude
    public double getSuggestedGrams() { return suggestedGrams; }
    @Exclude
    public void setSuggestedGrams(double val) { this.suggestedGrams = val; }

    @PropertyName("kcal")
    public double getKcal() { return kcal; }

    @PropertyName("kcal")
    public void setKcal(double kcal) { this.kcal = kcal; }

    @PropertyName("calories")
    public void setCalories(double val) { this.kcal = val; }

    public double getCalories() { return kcal; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    @PropertyName("category ")
    public void setCategoryWithSpace(String val) { this.category = val; }
    @PropertyName("categories")
    public void setCategories(String val) { this.category = val; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    @PropertyName("image ")
    public void setImageWithSpace(String val) { this.image = val; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    @PropertyName("description ")
    public void setDescriptionWithSpace(String val) { this.description = val; }
    @PropertyName("descripton")
    public void setDescripton(String val) { this.description = val; }
    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public List<String> getMealType() { return mealType; }
    public void setMealType(List<String> mealType) { this.mealType = mealType; }
}