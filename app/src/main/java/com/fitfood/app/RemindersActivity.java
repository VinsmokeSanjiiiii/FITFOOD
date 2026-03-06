package com.fitfood.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RemindersActivity extends AppCompatActivity {
    private static class ReminderCard {
        View rootView;
        TextView name, status, time;
        SwitchCompat toggle;
        Button btnEdit;
        String mealKey;
        ReminderCard(View view, String key, String title) {
            this.rootView = view;
            this.mealKey = key;
            this.name = view.findViewById(R.id.tvMealName);
            this.status = view.findViewById(R.id.tvMealStatus);
            this.time = view.findViewById(R.id.tvTimeDisplay);
            this.toggle = view.findViewById(R.id.switchReminder);
            this.btnEdit = view.findViewById(R.id.btnEditTime);
            this.name.setText(title);
        }
    }

    private ReminderCard cardBreakfast, cardLunch, cardSnacks, cardDinner;
    private TextView tvNextReminder;
    private Button btnSaveReminders;
    private AlarmManager alarmManager;
    private SharedPreferences prefs;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        prefs = getSharedPreferences("FitFoodReminders", MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        tvNextReminder = findViewById(R.id.tvNextReminder);
        btnSaveReminders = findViewById(R.id.btnSaveReminders);
        cardBreakfast = new ReminderCard(findViewById(R.id.cardBreakfast), "Breakfast", "Breakfast");
        cardLunch = new ReminderCard(findViewById(R.id.cardLunch), "Lunch", "Lunch");
        cardSnacks = new ReminderCard(findViewById(R.id.cardSnacks), "Snacks", "Snacks");
        cardDinner = new ReminderCard(findViewById(R.id.cardDinner), "Dinner", "Dinner");
        setupCardLogic(cardBreakfast);
        setupCardLogic(cardLunch);
        setupCardLogic(cardSnacks);
        setupCardLogic(cardDinner);
        btnSaveReminders.setOnClickListener(v -> saveAllReminders());
        updateUI();
        calculateNextReminder();
        listenForCompletedMeals();
    }

    private void setupCardLogic(ReminderCard card) {
        card.time.setText(formatTo12Hour(getHour(card.mealKey), getMinute(card.mealKey)));
        boolean isEnabled = prefs.getBoolean(card.mealKey + "_enabled", true);
        card.toggle.setChecked(isEnabled);
        updateCardVisuals(card, isEnabled);
        card.btnEdit.setOnClickListener(v -> {
            int h = getHour(card.mealKey);
            int m = getMinute(card.mealKey);
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                saveTime(card.mealKey, hourOfDay, minute);
                card.time.setText(formatTo12Hour(hourOfDay, minute));
                if (card.toggle.isChecked()) {
                    setReminder(card.mealKey, hourOfDay, minute);
                }
                calculateNextReminder();
            }, h, m, false).show();
        });
        card.toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(card.mealKey + "_enabled", isChecked).apply();
            updateCardVisuals(card, isChecked);
            if (!isChecked) {
                cancelReminder(card.mealKey);
                Toast.makeText(this, card.name.getText() + " reminder off", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCardVisuals(ReminderCard card, boolean isEnabled) {
        card.time.setAlpha(isEnabled ? 1.0f : 0.4f);
        card.btnEdit.setEnabled(isEnabled);
        card.btnEdit.setAlpha(isEnabled ? 1.0f : 0.4f);
        card.time.setTextColor(isEnabled ? Color.parseColor("#FF5722") : Color.GRAY);
    }

    private void saveAllReminders() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // CRITICAL FIX: Direct user to settings to allow alarms
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Please allow 'Alarms & Reminders' permission", Toast.LENGTH_LONG).show();
                return;
            }
        }
        checkAndSet(cardBreakfast);
        checkAndSet(cardLunch);
        checkAndSet(cardSnacks);
        checkAndSet(cardDinner);
        syncAllRemindersToFirestore();
        PeriodicWorkRequest backupRequest = new PeriodicWorkRequest.Builder(ReminderWorker.class, 12, TimeUnit.HOURS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("meal_backup", androidx.work.ExistingPeriodicWorkPolicy.UPDATE, backupRequest);
        Toast.makeText(this, "All settings saved!", Toast.LENGTH_SHORT).show();
        calculateNextReminder();
    }

    private void checkAndSet(ReminderCard card) {
        if (card.toggle.isChecked()) {
            setReminder(card.mealKey, getHour(card.mealKey), getMinute(card.mealKey));
        } else {
            cancelReminder(card.mealKey);
        }
    }

    private void setReminder(String mealType, int hour, int minute) {
        Intent intent = new Intent(this, RemindersReceiver.class);
        intent.putExtra("mealType", mealType);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, mealType.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void cancelReminder(String mealType) {
        Intent intent = new Intent(this, RemindersReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, mealType.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void listenForCompletedMeals() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        db.collection("mealLogs").document(userId)
                .collection("dates").document(date)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    verifyMealStatus(cardBreakfast, "Breakfast", date, userId);
                    verifyMealStatus(cardLunch, "Lunch", date, userId);
                    verifyMealStatus(cardSnacks, "Snacks", date, userId);
                    verifyMealStatus(cardDinner, "Dinner", date, userId);
                });
    }

    private void verifyMealStatus(ReminderCard card, String collectionName, String date, String userId) {
        db.collection("mealLogs").document(userId)
                .collection("dates").document(date)
                .collection(collectionName)
                .get().addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        card.status.setText("✅ Logged for today");
                        card.status.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        card.status.setText("⏳ Pending");
                        card.status.setTextColor(Color.parseColor("#FF9800"));
                    }
                });
    }

    private void calculateNextReminder() {
        long now = System.currentTimeMillis();
        long minDiff = Long.MAX_VALUE;
        String nextMeal = "None";
        String nextTime = "";
        ReminderCard[] cards = {cardBreakfast, cardLunch, cardSnacks, cardDinner};
        for (ReminderCard card : cards) {
            if (card.toggle.isChecked()) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, getHour(card.mealKey));
                c.set(Calendar.MINUTE, getMinute(card.mealKey));
                c.set(Calendar.SECOND, 0);
                if (c.getTimeInMillis() < now) c.add(Calendar.DAY_OF_MONTH, 1);
                long diff = c.getTimeInMillis() - now;
                if (diff < minDiff) {
                    minDiff = diff;
                    nextMeal = card.mealKey;
                    nextTime = formatTo12Hour(getHour(card.mealKey), getMinute(card.mealKey));
                }
            }
        }
        if (!nextMeal.equals("None")) {
            tvNextReminder.setText("Next: " + nextMeal + " at " + nextTime);
        } else {
            tvNextReminder.setText("All reminders off");
        }
    }

    private void saveTime(String key, int h, int m) {
        prefs.edit().putInt(key + "_hour", h).putInt(key + "_minute", m).apply();
    }
    private int getHour(String key) { return prefs.getInt(key + "_hour", getDefaultHour(key)); }
    private int getMinute(String key) { return prefs.getInt(key + "_minute", 0); }
    private int getDefaultHour(String key) {
        switch (key) { case "Breakfast": return 7; case "Lunch": return 12; case "Snacks": return 15; default: return 19; }
    }
    private String formatTo12Hour(int h, int m) {
        int hour = h % 12; if(hour == 0) hour = 12;
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour, m, (h < 12) ? "AM" : "PM");
    }
    private void syncAllRemindersToFirestore() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> settings = new HashMap<>();
        addMealToMap(settings, cardBreakfast, "Breakfast");
        addMealToMap(settings, cardLunch, "Lunch");
        addMealToMap(settings, cardSnacks, "Snacks");
        addMealToMap(settings, cardDinner, "Dinner");
        db.collection("users").document(userId)
                .collection("settings")
                .document("reminders")
                .set(settings, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to sync settings online", Toast.LENGTH_SHORT).show()
                );
    }

    private void addMealToMap(Map<String, Object> map, ReminderCard card, String name) {
        map.put(name + "Time", String.format(Locale.getDefault(), "%02d:%02d",
                getHour(card.mealKey), getMinute(card.mealKey)));
        map.put(name + "Enabled", card.toggle.isChecked());
    }
    private void updateUI() {
        calculateNextReminder();
        updateCardVisuals(cardBreakfast, cardBreakfast.toggle.isChecked());
        updateCardVisuals(cardLunch, cardLunch.toggle.isChecked());
        updateCardVisuals(cardSnacks, cardSnacks.toggle.isChecked());
        updateCardVisuals(cardDinner, cardDinner.toggle.isChecked());
    }
}