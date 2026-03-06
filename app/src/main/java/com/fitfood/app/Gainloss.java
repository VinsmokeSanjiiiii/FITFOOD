package com.fitfood.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class Gainloss extends AppCompatActivity {
    private static final String PREFS = "fitfood_prefs";
    private static final String KEY_GOAL_TYPE = "goalType";
    CardView btnGain, btnLoss;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gainloss);
        btnGain = findViewById(R.id.cardGainWeight);
        btnLoss = findViewById(R.id.cardLoseWeight);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Gainloss.this, LoginActivity.class));
            finish();
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedGoal = prefs.getString(KEY_GOAL_TYPE, null);
        if (savedGoal != null) {
            startActivity(new Intent(Gainloss.this, MainFeaturesActivity.class));
            finish();
            return;
        }
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.contains("goalType")) {
                        String goal = document.getString("goalType");
                        prefs.edit().putString(KEY_GOAL_TYPE, goal).apply();
                        startActivity(new Intent(Gainloss.this, MainFeaturesActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(Gainloss.this,
                                "Failed to check goal: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
        btnGain.setOnClickListener(v -> saveGoalAndGo("gain"));
        btnLoss.setOnClickListener(v -> saveGoalAndGo("loss"));
    }

    private void saveGoalAndGo(String goal) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(KEY_GOAL_TYPE, goal).apply();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("goalType", goal);
                    if (documentSnapshot.exists() && documentSnapshot.contains("dailyCalories")) {
                        updates.put("dailyCalories", documentSnapshot.getLong("dailyCalories"));
                    } else {
                        updates.put("dailyCalories", 0);
                    }
                    db.collection("users").document(userId)
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(Gainloss.this,
                                        "Goal saved successfully!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(Gainloss.this, MainFeaturesActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(Gainloss.this,
                                        "Failed to save goal: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Gainloss.this,
                            "Failed to fetch user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }
}
