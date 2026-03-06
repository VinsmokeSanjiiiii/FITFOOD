package com.fitfood.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Base64;
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
            case "Breakfast": default: return sectionBreakfast;
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
            if (!imageBase64.startsWith("http")) {
                // Decode in background to avoid lag
                new Thread(() -> {
                    try {
                        byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                        final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        runOnUiThread(() -> imgFood.setImageBitmap(decodedByte));
                    } catch (Exception ignored) {}
                }).start();
            } else {
                Glide.with(this).load(imageBase64).into(imgFood);
            }

             if (imageBase64.length() > 200) {
                try {
                    String cleanBase64 = imageBase64;
                    if (cleanBase64.contains(",")) {
                        cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
                    }
                    byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (decodedByte != null) {
                        imgFood.setImageBitmap(decodedByte);
                    } else {
                        imgFood.setImageResource(R.drawable.food_guide);
                    }
                } catch (Exception e) {
                    imgFood.setImageResource(R.drawable.food_guide);
                }
            }

            else {
                try {
                    int resId = getResources().getIdentifier(imageBase64, "drawable", getPackageName());
                    if (resId != 0) imgFood.setImageResource(resId);
                    else imgFood.setImageResource(R.drawable.food_guide);
                } catch (Exception e) {
                    imgFood.setImageResource(R.drawable.food_guide);
                }
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

        if (saveToDb) {
            saveMealToFirebase(section.name, foodName, grams, kcal, imageBase64);
        }
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
        if (dailyTotal < 500) {
            message = "Your calorie count is very low (" + dailyTotal + " kcal). Are you sure you want to finish the day?";
        }
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
}