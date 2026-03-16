package com.fitfood.app;

import java.util.List;

public class ComboTemplate {

    private String templateId;
    private String name;
    private String displayName;
    private String category;
    private List<String> requiredCategories;
    private List<String> mealType;
    private int minItems;
    private int maxItems;
    private String imageUrl;
    private boolean isActive;
    private int priority;

    public ComboTemplate() {}

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getRequiredCategories() { return requiredCategories; }

    public void setRequiredCategories(List<String> requiredCategories) {
        this.requiredCategories = requiredCategories;
    }

    public List<String> getMealType() { return mealType; }
    public void setMealType(List<String> mealType) { this.mealType = mealType; }
    public int getMinItems() { return minItems; }
    public void setMinItems(int minItems) { this.minItems = minItems; }
    public int getMaxItems() { return maxItems; }
    public void setMaxItems(int maxItems) { this.maxItems = maxItems; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}