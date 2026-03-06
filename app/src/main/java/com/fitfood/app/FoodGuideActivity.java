package com.fitfood.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class FoodGuideActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_guide);

        Button btnProtein = findViewById(R.id.btnProtein);
        Button btnFiber = findViewById(R.id.btnFiber);
        Button btnFats = findViewById(R.id.btnFats);
        Button btnVitamins = findViewById(R.id.btnVitamins);

        btnProtein.setOnClickListener(v -> openFoodList("Protein"));
        btnFiber.setOnClickListener(v -> openFoodList("Fiber"));
        btnFats.setOnClickListener(v -> openFoodList("Fats"));
        btnVitamins.setOnClickListener(v -> openFoodList("Vitamins"));
    }

    private void openFoodList(String category) {
        Intent intent = new Intent(FoodGuideActivity.this, FoodListActivity.class);
        intent.putExtra("CATEGORY", category);
        startActivity(intent);
    }
}
