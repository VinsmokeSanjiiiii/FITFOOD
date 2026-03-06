package com.fitfood.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// Added AlertDialog import here
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFeaturesActivity extends AppCompatActivity {

    private LinearLayout cardHealthRisk, cardFoodGuide, cardFoodAndCalorie, cardProgress;
    private String goal = null;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ViewPager2 bannerViewPager;
    private Handler bannerHandler;
    private Runnable bannerRunnable;
    private List<Integer> bannerImages;
    private TextView tvDashCalories, tvDashWeight, tvDashGoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_features);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        cardHealthRisk = findViewById(R.id.cardHealthRisk);
        cardFoodGuide = findViewById(R.id.cardFoodGuide);
        cardFoodAndCalorie = findViewById(R.id.cardTracker);
        cardProgress = findViewById(R.id.cardProgress);
        tvDashCalories = findViewById(R.id.tvDashCalories);
        tvDashWeight = findViewById(R.id.tvDashWeight);
        tvDashGoal = findViewById(R.id.tvDashGoal);
        setCardsEnabled(false);
        SharedPreferences prefs = getSharedPreferences("fitfood_prefs", MODE_PRIVATE);
        goal = prefs.getString("goalType", null);
        if (goal != null) {
            setCardsEnabled(true);
        }
        if (mAuth.getCurrentUser() != null) {
            if (isNetworkAvailable()) {
                loadUserData();
            } else {
                Toast.makeText(this, "No internet connection. Some data may be outdated.", Toast.LENGTH_SHORT).show();
            }
        }
        bannerViewPager = findViewById(R.id.bannerViewPager);
        bannerImages = Arrays.asList(R.drawable.banner_1, R.drawable.banner_2, R.drawable.banner_3);
        BannerAdapter bannerAdapter = new BannerAdapter(bannerImages);
        bannerViewPager.setAdapter(bannerAdapter);
        bannerHandler = new Handler(Looper.getMainLooper());
        bannerRunnable = new Runnable() {
            int currentPosition = 0;
            @Override
            public void run() {
                if (currentPosition == bannerImages.size()) currentPosition = 0;
                bannerViewPager.setCurrentItem(currentPosition++, true);
            }
        };
        bannerHandler.postDelayed(bannerRunnable, 4000);
        cardHealthRisk.setOnClickListener(v -> openActivityWithGoal(HealthRiskInfoActivity.class, "HealthRisk_"));
        cardFoodGuide.setOnClickListener(v -> openActivityWithGoal(FoodListActivity.class, "FoodGuide_"));
        cardFoodAndCalorie.setOnClickListener(v -> openActivityWithGoal(FoodTrackerActivity.class, "FoodAndCalorieTracker_"));
        cardProgress.setOnClickListener(v -> openActivityWithGoal(ProgressReportActivity.class, "ProgressReport_"));
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        TextView tabReminders = findViewById(R.id.tabReminders);
        TextView tabHealthTips = findViewById(R.id.tabHealthTips);
        TextView tabProfile = findViewById(R.id.tabProfile);
        TextView tabLogout = findViewById(R.id.tabLogout);
        TextView[] tabs = {tabReminders, tabHealthTips, tabProfile, tabLogout};
        for (TextView tab : tabs) tab.setTypeface(null, Typeface.BOLD);
        for (TextView tab : tabs) {
            tab.setOnClickListener(v -> {
                for (TextView t : tabs) {
                    t.setBackgroundResource(R.drawable.tab_bg_unselected);
                    t.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                }
                v.setBackgroundResource(R.drawable.tab_bg_selected);
                ((TextView)v).setTextColor(ContextCompat.getColor(this, android.R.color.white));
                int id = v.getId();
                if (id == R.id.tabReminders) startActivity(new Intent(this, RemindersActivity.class));
                else if (id == R.id.tabHealthTips) startActivity(new Intent(this, HealthTipsActivity.class));
                else if (id == R.id.tabProfile) startActivity(new Intent(this, AccountProfileActivity.class));
                else if (id == R.id.tabLogout) showLogoutConfirmationDialog();
            });
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
    }

    private void openActivityWithGoal(Class<?> cls, String analyticsPrefix) {
        if (goal == null) {
            Toast.makeText(this, "Goal not set yet", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, cls);
        i.putExtra("goalType", goal);
        startActivity(i);
        logFirebaseEvent(analyticsPrefix + goal);
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performLogout() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Logout may not sync data.", Toast.LENGTH_SHORT).show();
        }
        saveUserDataToFirestore(() -> {
            SharedPreferences.Editor sessionEditor = getSharedPreferences("fitfood_prefs", MODE_PRIVATE).edit();
            sessionEditor.clear();
            sessionEditor.apply();
            FirebaseAuth.getInstance().signOut();
            Intent logoutIntent = new Intent(this, LoginActivity.class);
            logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(logoutIntent);
            finish();
        });
    }

    private void setCardsEnabled(boolean enabled) {
        cardHealthRisk.setEnabled(enabled);
        cardFoodGuide.setEnabled(enabled);
        cardFoodAndCalorie.setEnabled(enabled);
        cardProgress.setEnabled(enabled);
    }

    private void logFirebaseEvent(String value) {
        Bundle bundle = new Bundle();
        bundle.putString("card", value);
        mFirebaseAnalytics.logEvent("card_click", bundle);
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isDestroyed() || isFinishing()) return;
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role != null && role.trim().equalsIgnoreCase("admin")) {
                            Intent intent = new Intent(MainFeaturesActivity.this, AdminActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                            return;
                        }
                        goal = documentSnapshot.getString("goalType");
                        if (goal == null) goal = "Not Set";
                        Double dailyCal = documentSnapshot.getDouble("dailyCalories");
                        Double weight = documentSnapshot.getDouble("weight");
                        String goalType = documentSnapshot.getString("goalType");

                        if (dailyCal != null && tvDashCalories != null) {
                            tvDashCalories.setText(String.format(java.util.Locale.getDefault(), "%.0f kcal", dailyCal));
                        } else {
                            tvDashCalories.setText("Set Goal");
                        }

                        if (weight != null && tvDashWeight != null) {
                            tvDashWeight.setText(String.format(java.util.Locale.getDefault(), "%.1f kg", weight));
                        }

                        if (goalType != null && tvDashGoal != null) {
                            String cleanGoal = goalType.replace("_", " ").toUpperCase();
                            tvDashGoal.setText("CURRENT FOCUS: " + cleanGoal);
                        }
                        SharedPreferences.Editor editor = getSharedPreferences("fitfood_prefs", MODE_PRIVATE).edit();
                        editor.putString("goalType", goal);
                        String mealLogs = documentSnapshot.getString("mealLogs");
                        editor.putString("mealLogs", mealLogs != null ? mealLogs : "[]");
                        Double weightDouble = documentSnapshot.getDouble("weight");
                        editor.putFloat("weight", weightDouble != null ? weightDouble.floatValue() : 0f);
                        Long stepsLong = documentSnapshot.getLong("steps");
                        editor.putInt("steps", stepsLong != null ? stepsLong.intValue() : 0);
                        Map<String, Object> reminders = (Map<String, Object>) documentSnapshot.get("reminders");
                        if (reminders != null) {
                            editor.putString("breakfastTime", (String) reminders.get("breakfastTime"));
                            editor.putString("lunchTime", (String) reminders.get("lunchTime"));
                            editor.putString("dinnerTime", (String) reminders.get("dinnerTime"));
                            editor.putString("snacksTime", (String) reminders.get("snacksTime"));
                        }
                        editor.apply();
                        setCardsEnabled(true);
                        scheduleAllReminders();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load user data: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void saveUserDataToFirestore(Runnable onComplete) {
        if (mAuth.getCurrentUser() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();
        SharedPreferences prefs = getSharedPreferences("fitfood_prefs", MODE_PRIVATE);
        Map<String, Object> userData = new HashMap<>();
        userData.put("goalType", goal);
        userData.put("mealLogs", prefs.getString("mealLogs", "[]"));
        userData.put("weight", prefs.getFloat("weight", 0f));
        userData.put("steps", prefs.getInt("steps", 0));
        Map<String, Object> reminders = new HashMap<>();
        reminders.put("breakfastTime", prefs.getString("breakfastTime", "07:00"));
        reminders.put("lunchTime", prefs.getString("lunchTime", "12:00"));
        reminders.put("dinnerTime", prefs.getString("dinnerTime", "19:00"));
        reminders.put("snacksTime", prefs.getString("snacksTime", "15:00"));
        userData.put("reminders", reminders);
        db.collection("users").document(uid).set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (onComplete != null) onComplete.run();
                });
    }

    private void scheduleAllReminders() {
        SharedPreferences prefs = getSharedPreferences("fitfood_prefs", MODE_PRIVATE);
        String[] meals = {"breakfast", "lunch", "dinner", "snacks"};
        for (String meal : meals) {
            String time = prefs.getString(meal + "Time", getDefaultMealTime(meal));
            scheduleReminder(meal, time);
        }
    }

    private String getDefaultMealTime(String meal) {
        switch (meal) {
            case "breakfast": return "07:00";
            case "lunch": return "12:00";
            case "dinner": return "19:00";
            case "snacks": return "15:00";
        }
        return "12:00";
    }

    private void scheduleReminder(String mealType, String time) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            Intent intent = new Intent(this, RemindersReceiver.class);
            intent.putExtra("mealType", mealType);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    mealType.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) calendar.add(Calendar.DAY_OF_YEAR, 1);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm != null ? cm.getActiveNetworkInfo() : null;
        return networkInfo != null && networkInfo.isConnected();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.postDelayed(bannerRunnable, 4000);
        }
        if (mAuth.getCurrentUser() != null && isNetworkAvailable()) {
            loadUserData();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }
}