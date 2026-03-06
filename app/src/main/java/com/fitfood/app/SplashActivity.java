package com.fitfood.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        TextView overviewText = findViewById(R.id.overviewText);
        Button btnProceed = findViewById(R.id.btnProceed);
        Button btnInstructions = findViewById(R.id.btnInstructions);
        btnInstructions.setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, InstructionsActivity.class));
        });
        progressBar = findViewById(R.id.progressBar);
        overviewText.setText("FITFOOD helps adults aged 35 and above manage their nutrition, "
                + "track calories, and prevent obesity-related diseases for a healthier lifestyle.");
        btnProceed.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            btnProceed.setEnabled(false);
            SharedPreferences prefs = getSharedPreferences("FitFoodPrefs", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
            String role = prefs.getString("userRole", "user");
            String goal = prefs.getString("goal", null);
            Intent intent;
            if (isLoggedIn) {
                if ("admin".equalsIgnoreCase(role)) {
                    intent = new Intent(SplashActivity.this, AdminActivity.class);
                }
                else if (goal != null) {
                    intent = new Intent(SplashActivity.this, MainFeaturesActivity.class);
                    intent.putExtra("goal", goal);
                } else {
                    intent = new Intent(SplashActivity.this, Gainloss.class);
                }
            } else {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}