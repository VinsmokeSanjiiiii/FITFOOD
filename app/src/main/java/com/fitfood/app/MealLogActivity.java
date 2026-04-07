package com.fitfood.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MealLogActivity extends AppCompatActivity {
    private static class MealSection {
        LinearLayout container;
        TextView title, subtotal;
        int currentCalories = 0;
        String name;
        MealSection(View includedLayout, String name) {
            this.name = name;
            this.container = includedLayout.findViewById(R.id.llContainer);
            this.title = includedLayout.findViewById(R.id.tvSectionTitle);
            this.subtotal = includedLayout.findViewById(R.id.tvSubtotal);
            this.title.setText(name);
        }
    }
    private MealSection sectionBreakfast, sectionLunch, sectionDinner, sectionSnacks;
    private TextView dailyTotalText, tvProgressText;
    private ProgressBar progressBarCalories;
    private int dailyTotal = 0;
    private double dailyCalorieLimit = 2000;
    private double currentWeight = 70.0;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> addMealLauncher;
    private String userGoal = "weight_loss";
    private ComboMealView comboMealView;
    private List<FoodItem> availableFoodsCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_log);
        sectionBreakfast = new MealSection(findViewById(R.id.sectionBreakfast), "Breakfast");
        sectionLunch = new MealSection(findViewById(R.id.sectionLunch), "Lunch");
        sectionDinner = new MealSection(findViewById(R.id.sectionDinner), "Dinner");
        sectionSnacks = new MealSection(findViewById(R.id.sectionSnacks), "Snacks");
        dailyTotalText = findViewById(R.id.dailyTotalCalories);
        progressBarCalories = findViewById(R.id.progressBarCalories);
        tvProgressText = findViewById(R.id.tvProgressText);
        Button addMealBtn = findViewById(R.id.addMealBtn);
        Button btnFinishDay = findViewById(R.id.btnFinishDay);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fetchUserData();
        loadMealsFromFirebase();
        initializeComboMealSection();
        loadFoodsForComboGeneration();
        addMealLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String mealType = data.getStringExtra("mealType");
                        String foodName = data.getStringExtra("foodName");
                        double kcal = data.getDoubleExtra("totalKcal", 0);
                        double grams = data.getDoubleExtra("grams", 0);
                        String imageBase64 = data.getStringExtra("foodImage");
                        if (mealType == null) mealType = "Breakfast";
                        double adjustedKcal = userGoal.equals("weight_loss") ? kcal * 0.8 : kcal * 1.2;
                        addMealToSection(getSectionByName(mealType), foodName, adjustedKcal, grams, imageBase64, true);
                    }
                }
        );
        addMealBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MealLogActivity.this, FoodListActivity.class);
            intent.putExtra("fromMealLog", true);
            intent.putExtra("selectedMealType", "Breakfast");
            addMealLauncher.launch(intent);
        });
        btnFinishDay.setOnClickListener(v -> attemptFinishDay());
    }

    private void fetchUserData() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        if (doc.contains("dailyCalories")) {
                            Double fetchedLimit = doc.getDouble("dailyCalories");
                            if (fetchedLimit != null && fetchedLimit > 0) dailyCalorieLimit = fetchedLimit;
                        }
                        if (doc.contains("weight")) currentWeight = doc.getDouble("weight");
                        String goal = getSharedPreferences("fitfood_prefs", MODE_PRIVATE).getString("goalType", "loss");
                        userGoal = goal.equals("gain") ? "weight_gain" : "weight_loss";
                        updateDailyTotalUI();
                    }
                });
    }

    private MealSection getSectionByName(String name) {
        switch (name) {
            case "Lunch": return sectionLunch;
            case "Dinner": return sectionDinner;
            case "Snacks": return sectionSnacks;
            default: return sectionBreakfast;
        }
    }

    @SuppressLint("SetTextI18n")
    private void addMealToSection(MealSection section, String foodName, double kcal, double grams, String imageBase64, boolean saveToDb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 16, 0, 16);
        CardView cardImage = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(120, 120);
        cardParams.setMargins(0, 0, 24, 0);
        cardImage.setLayoutParams(cardParams);
        cardImage.setRadius(60);
        cardImage.setCardElevation(0);
        ImageView imgFood = new ImageView(this);
        imgFood.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        imgFood.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            if (imageBase64.startsWith("http")) {
                Glide.with(this).load(imageBase64).into(imgFood);
            } else {
                new Thread(() -> {
                    try {
                        byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                        final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        runOnUiThread(() -> imgFood.setImageBitmap(decodedByte));
                    } catch (Exception ignored) {}
                }).start();
            }
        } else {
            imgFood.setImageResource(R.drawable.food_guide);
        }
        cardImage.addView(imgFood);
        row.addView(cardImage);
        TextView tvFood = new TextView(this);
        tvFood.setText(String.format(Locale.getDefault(), "%s\n%.0fg", foodName, grams));
        tvFood.setTextSize(16f);
        tvFood.setTextColor(Color.parseColor("#424242"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvFood.setLayoutParams(params);
        TextView tvCal = new TextView(this);
        tvCal.setText(String.format(Locale.getDefault(), "%.0f kcal", kcal));
        tvCal.setTextSize(14f);
        tvCal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvCal.setTextColor(Color.parseColor("#FF5722"));
        tvCal.setPadding(16, 0, 16, 0);
        ImageView btnDelete = new ImageView(this);
        btnDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnDelete.setColorFilter(Color.parseColor("#BDBDBD"));
        btnDelete.setPadding(16, 8, 8, 8);
        row.addView(tvFood);
        row.addView(tvCal);
        row.addView(btnDelete);
        section.container.addView(row);
        section.currentCalories += (int) Math.round(kcal);
        section.subtotal.setText(section.currentCalories + " kcal");
        updateDailyTotalUI();
        if (saveToDb) saveMealToFirebase(section.name, foodName, grams, kcal, imageBase64);
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Remove Item?")
                .setMessage("Delete " + foodName + " from log?")
                .setPositiveButton("Delete", (d, w) -> {
                    section.container.removeView(row);
                    section.currentCalories -= (int) Math.round(kcal);
                    section.subtotal.setText(section.currentCalories + " kcal");
                    deleteMealFromFirebase(section.name, foodName);
                    updateDailyTotalUI();
                })
                .setNegativeButton("Cancel", null)
                .show());
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateDailyTotalUI() {
        dailyTotal = sectionBreakfast.currentCalories + sectionLunch.currentCalories +
                sectionDinner.currentCalories + sectionSnacks.currentCalories;
        double safeLimit = (dailyCalorieLimit > 0) ? dailyCalorieLimit : 2000;
        dailyTotalText.setText(String.format(Locale.getDefault(), "Total: %d / %.0f kcal", dailyTotal, safeLimit));
        int progress = (int) ((dailyTotal / safeLimit) * 100);
        progressBarCalories.setProgress(Math.min(progress, 100));
        tvProgressText.setText(progress + "%");
        if (progress > 100) {
            progressBarCalories.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
            tvProgressText.setTextColor(Color.RED);
        } else if (progress > 85) {
            progressBarCalories.getProgressDrawable().setColorFilter(Color.parseColor("#FFC107"), PorterDuff.Mode.SRC_IN);
            tvProgressText.setTextColor(Color.WHITE);
        } else {
            progressBarCalories.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            tvProgressText.setTextColor(Color.WHITE);
        }
    }

    private void attemptFinishDay() {
        String message = "Are you done eating for today? This will save your progress.";
        if (dailyTotal < 500) message = "Your calorie count is very low (" + dailyTotal + " kcal). Are you sure you want to finish the day?";
        new AlertDialog.Builder(this)
                .setTitle("Complete Day")
                .setMessage(message)
                .setPositiveButton("I'm Done", (dialog, which) -> saveDailySummary())
                .setNegativeButton("Not yet", null)
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void saveDailySummary() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Map<String, Object> progressEntry = new HashMap<>();
        progressEntry.put("date", date);
        progressEntry.put("calories", dailyTotal);
        progressEntry.put("weight", currentWeight);
        double heightM = 1.70;
        progressEntry.put("bmi", currentWeight / (heightM * heightM));
        db.collection("users").document(userId)
                .collection("progress").document(date)
                .set(progressEntry)
                .addOnSuccessListener(aVoid -> {
                    sectionBreakfast.container.removeAllViews();
                    sectionLunch.container.removeAllViews();
                    sectionDinner.container.removeAllViews();
                    sectionSnacks.container.removeAllViews();
                    sectionBreakfast.currentCalories = 0;
                    sectionLunch.currentCalories = 0;
                    sectionDinner.currentCalories = 0;
                    sectionSnacks.currentCalories = 0;
                    sectionBreakfast.subtotal.setText("0 kcal");
                    sectionLunch.subtotal.setText("0 kcal");
                    sectionDinner.subtotal.setText("0 kcal");
                    sectionSnacks.subtotal.setText("0 kcal");
                    updateDailyTotalUI();
                    Map<String, Object> status = new HashMap<>();
                    status.put("status", "completed");
                    db.collection("mealLogs").document(userId)
                            .collection("dates").document(date)
                            .set(status, SetOptions.merge());
                    new AlertDialog.Builder(this)
                            .setTitle("Good Job!")
                            .setMessage("Daily log saved. See your results in the Progress Report.")
                            .setPositiveButton("OK", (d,w) -> finish())
                            .show();
                });
    }

    private void saveMealToFirebase(String type, String food, double grams, double kcal, String imageBase64) {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Map<String, Object> meal = new HashMap<>();
        meal.put("foodName", food);
        meal.put("grams", grams);
        meal.put("calories", kcal);
        meal.put("foodImage", imageBase64);
        db.collection("mealLogs").document(userId)
                .collection("dates").document(date)
                .collection(type).add(meal);
    }

    private void deleteMealFromFirebase(String type, String foodName) {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("mealLogs").document(userId)
                .collection("dates").document(date)
                .collection(type)
                .whereEqualTo("foodName", foodName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            doc.getReference().delete();
                            break;
                        }
                        Toast.makeText(this, "Removed from history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMealsFromFirebase() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("mealLogs").document(userId)
                .collection("dates").document(date)
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists() && "completed".equals(doc.getString("status"))) {
                        findViewById(R.id.addMealBtn).setEnabled(false);
                        findViewById(R.id.addMealBtn).setAlpha(0.5f);
                        Button btnFinish = findViewById(R.id.btnFinishDay);
                        btnFinish.setText("Completed");
                        btnFinish.setEnabled(false);
                        btnFinish.setBackgroundColor(Color.GRAY);
                        return;
                    }
                    String[] types = {"Breakfast", "Lunch", "Dinner", "Snacks"};
                    for (String type : types) {
                        db.collection("mealLogs").document(userId)
                                .collection("dates").document(date)
                                .collection(type).get()
                                .addOnSuccessListener(query -> {
                                    for (DocumentSnapshot d : query.getDocuments()) {
                                        String name = d.getString("foodName");
                                        Double kcal = d.getDouble("calories");
                                        Double grams = d.getDouble("grams");
                                        String img = d.getString("foodImage");
                                        if (name != null && kcal != null) {
                                            addMealToSection(getSectionByName(type), name, kcal, grams != null ? grams : 0, img, false);
                                        }
                                    }
                                });
                    }
                });
    }

    private void initializeComboMealSection() {
        LinearLayout comboContainer = findViewById(R.id.mealLogContent);
        comboMealView = new ComboMealView(this, comboContainer, true);
        comboMealView.setOnComboActionListener(new ComboMealView.OnComboActionListener() {
            @Override
            public void onAddComboToLog(ComboMeal combo) {
                // Legacy fallback - ask for meal type
                showMealTypeDialogAndAddCombo(combo);
            }
            @Override
            public void onAddComboToLogWithMealType(ComboMeal combo, String mealType) {
                addComboToMealLog(combo, mealType);
            }
            @Override
            public void onViewComboDetails(ComboMeal combo) {
                showComboDetailsDialog(combo);
            }
            @Override
            public void onRefreshCombo() {
                generateNewComboSuggestion();
            }
            @Override
            public void onLikeCombo(ComboMeal combo) {
                saveLikedCombo(combo);
            }
        });
        if (comboMealView.getView().getParent() == null) {
            comboContainer.addView(comboMealView.getView(), 0);
        }
    }

    private void showMealTypeDialogAndAddCombo(ComboMeal combo) {
        final String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snacks"};
        new AlertDialog.Builder(this)
                .setTitle("Add to which meal?")
                .setItems(mealTypes, (dialog, which) -> addComboToMealLog(combo, mealTypes[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadFoodsForComboGeneration() {
        SharedPreferences prefs = getSharedPreferences("fitfood_prefs", MODE_PRIVATE);
        String goalChoice = prefs.getString("goalType", "loss");
        userGoal = goalChoice.equals("gain") ? "weight_gain" : "weight_loss";
        FirebaseFirestore.getInstance()
                .collection("foods_v2")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    availableFoodsCache = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            FoodItem food = doc.toObject(FoodItem.class);
                            food.setId(doc.getId());
                            double baseGrams = food.getGrams();
                            if (userGoal.equals("weight_loss")) food.setSuggestedGrams(baseGrams * 0.85);
                            else if (userGoal.equals("weight_gain")) food.setSuggestedGrams(baseGrams * 1.15);
                            else food.setSuggestedGrams(baseGrams);
                            availableFoodsCache.add(food);
                        } catch (Exception e) {
                            Log.e("MealLog", "Error parsing food: " + e.getMessage());
                        }
                    }
                    generateNewComboSuggestion();
                })
                .addOnFailureListener(e -> {
                    Log.e("MealLog", "Failed to load foods for combo: " + e.getMessage());
                    comboMealView.getView().setVisibility(View.GONE);
                });
    }

    private void generateNewComboSuggestion() {
        if (availableFoodsCache == null || availableFoodsCache.isEmpty()) {
            comboMealView.getView().setVisibility(View.GONE);
            return;
        }
        String currentMealType = determineCurrentMealType();
        ComboMeal combo = ComboMealGenerator.generateCombo(
                availableFoodsCache,
                currentMealType,
                userGoal
        );
        if (combo != null && combo.isValid()) {
            comboMealView.setComboMeal(combo);
            comboMealView.getView().setVisibility(View.VISIBLE);
        } else {
            comboMealView.getView().setVisibility(View.GONE);
        }
    }

    private String determineCurrentMealType() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) return "Breakfast";
        if (hour >= 11 && hour < 15) return "Lunch";
        if (hour >= 15 && hour < 18) return "Snacks";
        return "Dinner";
    }

    private void addComboToMealLog(ComboMeal combo, String mealType) {
        if (combo == null || combo.getItems().isEmpty()) return;
        MealSection targetSection = getSectionByName(mealType);
        for (ComboMeal.ComboItem item : combo.getItems()) {
            FoodItem food = item.getFoodItem();
            addMealToSection(
                    targetSection,
                    food.getName(),
                    item.getCalculatedCalories(),
                    item.getPortionGrams(),
                    food.getImage(),
                    true
            );
        }
        Toast.makeText(this,
                String.format(Locale.getDefault(), "Added %d items to %s",
                        combo.getItems().size(), mealType),
                Toast.LENGTH_SHORT).show();
    }

    private void showComboDetailsDialog(ComboMeal combo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(combo.getDisplayName());
        StringBuilder message = new StringBuilder();
        message.append("Meal Type: ").append(combo.getMealType()).append("\n\n");
        message.append("Items:\n");
        for (ComboMeal.ComboItem item : combo.getItems()) {
            message.append("• ").append(item.getFoodItem().getName()).append("\n");
            message.append("  ").append(item.getDisplayText()).append("\n\n");
        }
        message.append("\nTotal: ").append(String.format(Locale.getDefault(), "%.0f kcal, %.0fg",
                combo.getTotalCalories(), combo.getTotalGrams()));
        builder.setMessage(message.toString());
        builder.setPositiveButton("Add to Log", (dialog, which) -> showMealTypeDialogAndAddCombo(combo));
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void saveLikedCombo(ComboMeal combo) {
        // Optional: Save to Firestore for personalized suggestions
    }
}