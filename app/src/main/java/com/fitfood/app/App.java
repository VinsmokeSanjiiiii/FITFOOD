package com.fitfood.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {
    public static final String CHANNEL_MEAL_REMINDERS = "fitfood_reminder";
    public static final String CHANNEL_ADMIN_ALERTS = "fitfood_admin";
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;
            NotificationChannel mealChannel = new NotificationChannel(
                    CHANNEL_MEAL_REMINDERS,
                    "Meal Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            mealChannel.setDescription("Reminds you to log your Breakfast, Lunch, Dinner, and Snacks");
            NotificationChannel adminChannel = new NotificationChannel(
                    CHANNEL_ADMIN_ALERTS,
                    "Admin Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            adminChannel.setDescription("Important warnings and updates from the Administrator");
            manager.createNotificationChannel(mealChannel);
            manager.createNotificationChannel(adminChannel);
        }
    }
}