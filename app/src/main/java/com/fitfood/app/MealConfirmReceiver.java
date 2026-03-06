package com.fitfood.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MealConfirmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String mealType = intent.getStringExtra("mealType");
        final String confirmedMealType = (mealType == null) ? "meal" : mealType;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection("mealLogs")
                    .document(userId)
                    .collection("dates")
                    .document(date);

            Map<String, Object> mealData = new HashMap<>();
            mealData.put(confirmedMealType, "confirmed");
            mealData.put(confirmedMealType + "_timestamp", System.currentTimeMillis());

            final Handler handler = new Handler(Looper.getMainLooper());

            docRef.set(mealData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> handler.post(() -> {
                        Toast.makeText(context, confirmedMealType + " confirmed!", Toast.LENGTH_SHORT).show();

                        NotificationManagerCompat.from(context.getApplicationContext())
                                .cancel(confirmedMealType.hashCode());
                    }))
                    .addOnFailureListener(e -> handler.post(() ->
                            Toast.makeText(context, "Failed to log " + confirmedMealType, Toast.LENGTH_SHORT).show()
                    ));
        } else {
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show());
        }
    }
}
