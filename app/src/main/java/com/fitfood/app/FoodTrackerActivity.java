package com.fitfood.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class FoodTrackerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_tracker);
        Button btnMealLog = findViewById(R.id.btnMealLog);
        Button btnCalorieCalculator = findViewById(R.id.btnCalorieCalculator);
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        btnMealLog.setOnClickListener(v -> {
            Intent intent = new Intent(FoodTrackerActivity.this, MealLogActivity.class);
            startActivity(intent);
        });
        btnCalorieCalculator.setOnClickListener(v -> {
            Intent intent = new Intent(FoodTrackerActivity.this, CalorieTrackerActivity.class);
            startActivity(intent);
        });
    }
}
