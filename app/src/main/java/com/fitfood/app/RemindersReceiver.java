package com.fitfood.app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class RemindersReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(context.getPackageName())) {
                Intent i = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        }

        String mealType = intent.getStringExtra("mealType");
        if (mealType == null) mealType = "meal";
        final String finalMealType = mealType;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("mealLogs").document(userId)
                .collection("dates").document(date)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String content = "Time for your " + finalMealType;
                    if (snapshot.exists() && snapshot.contains(finalMealType)) {
                        content = finalMealType + " status: " + snapshot.getString(finalMealType);
                    }

                    autoConfirmMeal(db, userId, date, finalMealType);

                    Intent confirmIntent = new Intent(context, MealConfirmReceiver.class);
                    confirmIntent.putExtra("mealType", finalMealType);
                    PendingIntent confirmPendingIntent = PendingIntent.getBroadcast(
                            context.getApplicationContext(),
                            finalMealType.hashCode(),
                            confirmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context.getApplicationContext(), "fitfood_reminder")
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle("Meal Reminder 🍽️ - " + finalMealType)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)
                            .addAction(R.drawable.ic_check, "Confirm", confirmPendingIntent);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(context.getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED) {
                            Log.w("Reminder", "POST_NOTIFICATIONS permission not granted, skipping notification for " + finalMealType);
                            return;
                        }
                    }

                    try {
                        NotificationManagerCompat.from(context.getApplicationContext()).notify(finalMealType.hashCode(), builder.build());
                        Log.d("Reminder", "Notification sent for " + finalMealType);
                    } catch (SecurityException e) {
                        Log.e("Reminder", "Failed to send notification: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> Log.e("Reminder", "Failed to fetch meal items for " + finalMealType + ": " + e.getMessage()));
    }

    private void autoConfirmMeal(FirebaseFirestore db, String userId, String date, String mealType) {
        Map<String, Object> mealData = new HashMap<>();
        mealData.put(mealType, "confirmed");

        db.collection("mealLogs").document(userId)
                .collection("dates")
                .document(date)
                .set(mealData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    updateAdherence(db, userId, date);
                });
    }

    private void updateAdherence(FirebaseFirestore db, String userId, String date) {
        db.collection("mealLogs").document(userId)
                .collection("dates")
                .document(date)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        int confirmed = 0;
                        String[] meals = {"Breakfast", "Lunch", "Snacks", "Dinner"};
                        for (String meal : meals) {
                            if ("confirmed".equals(snapshot.getString(meal))) confirmed++;
                        }
                        double adherence = (confirmed / 4.0) * 100.0;

                        Map<String, Object> progressData = new HashMap<>();
                        progressData.put("adherence", adherence);
                        progressData.put("lastUpdated", date);

                        db.collection("users").document(userId)
                                .collection("progress")
                                .document(date)
                                .set(progressData, SetOptions.merge());
                    }
                });
    }
}
