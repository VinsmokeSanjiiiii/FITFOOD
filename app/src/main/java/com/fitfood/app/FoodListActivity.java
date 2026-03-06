package com.fitfood.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FoodListActivity extends AppCompatActivity implements FoodAdapter.OnMealAddedListener {

    private FoodAdapter foodAdapter;
    private List<FoodItem> foodItemList;
    private List<FoodItem> foodItemListFull;
    private TextView tvEmptyState, suggestionText, tvHealthQuote;
    private Spinner mealTypeSpinner;
    private EditText etSearchFood;
    private final String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snacks"};
    private String selectedMealType = "Breakfast";
    private String selectedCategory = "All";
    private String userGoal = "weight_loss";
    private boolean fromMealLog = false;
    private FirebaseFirestore db;
    private final String[] safetyQuotes = {
            "“In case of any health conditions, consume responsibly and monitor your portion.”",
            "“In case of uncertainty, consume in moderation and seek professional advice.”",
            "“In case of possible allergies or sensitivities, review ingredients before consumption.”"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);
        fromMealLog = getIntent().getBooleanExtra("fromMealLog", false);
        RecyclerView recyclerViewFoods = findViewById(R.id.recyclerViewFoods);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        suggestionText = findViewById(R.id.suggestionText);
        tvHealthQuote = findViewById(R.id.tvHealthQuote);
        mealTypeSpinner = findViewById(R.id.mealTypeSpinner);
        etSearchFood = findViewById(R.id.etSearchFood);
        RadioGroup rgCategories = findViewById(R.id.rgCategories);
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        db = FirebaseFirestore.getInstance();
        foodItemList = new ArrayList<>();
        foodItemListFull = new ArrayList<>();
        recyclerViewFoods.setLayoutManager(new LinearLayoutManager(this));
        foodAdapter = new FoodAdapter(foodItemList, this, this, fromMealLog);
        recyclerViewFoods.setAdapter(foodAdapter);
        SharedPreferences prefs = getSharedPreferences("fitfood_prefs", MODE_PRIVATE);
        String goalChoice = prefs.getString("goalType", "loss");
        userGoal = goalChoice.equals("gain") ? "weight_gain" : "weight_loss";
        setupMealSpinner();
        displayRandomQuote();
        loadFoods();
        etSearchFood.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFoods(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });

        rgCategories.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            if (rb != null) {
                selectedCategory = rb.getText().toString();
                filterFoods(etSearchFood.getText().toString());
            }
        });
    }

    private void displayRandomQuote() {
        if (tvHealthQuote != null && safetyQuotes.length > 0) {
            int randomIndex = new Random().nextInt(safetyQuotes.length);
            tvHealthQuote.setText(safetyQuotes[randomIndex]);
        }
    }

    private void setupMealSpinner() {
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, mealTypes);
        mealTypeSpinner.setAdapter(mealAdapter);
        mealTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMealType = mealTypes[position];
                updateSuggestionText(selectedMealType);
                filterFoods(etSearchFood.getText().toString());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateSuggestionText(String mealType) {
        String suggestion;
        if (userGoal.equals("weight_loss")) {
            switch (mealType) {
                case "Breakfast": suggestion = "☀️ Tip: Try Oats & Eggs for sustained energy."; break;
                case "Lunch": suggestion = "🍛 Tip: Focus on fiber-rich veggies and lean meat."; break;
                case "Dinner": suggestion = "🌙 Tip: Light soups or salads aid digestion."; break;
                default: suggestion = "🍎 Tip: Nuts or yogurt make great snacks."; break;
            }
        } else {
            switch (mealType) {
                case "Breakfast": suggestion = "☀️ Tip: Add peanut butter for extra calories."; break;
                case "Lunch": suggestion = "🍛 Tip: Rice and meat build muscle."; break;
                case "Dinner": suggestion = "🌙 Tip: Don't skip carbs if bulking."; break;
                default: suggestion = "🍎 Tip: Protein shakes or eggs are perfect."; break;
            }
        }
        suggestionText.setText(suggestion);
    }

    private void loadFoods() {
        db.collection("foods_v2").get()
                .addOnSuccessListener(querySnapshot -> {
                    foodItemListFull.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            FoodItem food = doc.toObject(FoodItem.class);
                            double baseGrams = food.getGrams();
                            if (userGoal.equals("weight_loss")) food.setSuggestedGrams(baseGrams * 0.8);
                            else if (userGoal.equals("weight_gain")) food.setSuggestedGrams(baseGrams * 1.2);
                            else food.setSuggestedGrams(baseGrams);
                            foodItemListFull.add(food);
                        } catch (Exception e) {
                            Log.e("FoodList", "Parse error: " + e.getMessage());
                        }
                    }
                    filterFoods("");
                });
    }

    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    private void filterFoods(String query) {
        foodItemList.clear();
        String lowerQuery = query.toLowerCase();
        for (FoodItem food : foodItemListFull) {
            boolean matchesSearch = food.getName().toLowerCase().contains(lowerQuery);
            boolean matchesMeal = false;
            if (food.getMealType() != null) {
                for (String m : food.getMealType()) {
                    if (m.equalsIgnoreCase(selectedMealType) || m.equalsIgnoreCase("All")) {
                        matchesMeal = true;
                        break;
                    }
                }
            }
            boolean matchesCategory = selectedCategory.equals("All") ||
                    (food.getCategory() != null && food.getCategory().equalsIgnoreCase(selectedCategory));
            if (matchesSearch && matchesMeal && matchesCategory) {
                foodItemList.add(food);
            }
        }
        foodAdapter.notifyDataSetChanged();
        if (foodItemList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("No " + selectedCategory + " options found for " + selectedMealType);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMealAdded(String foodName, double kcal, double grams) {
        if (!fromMealLog) return;
        Intent result = new Intent();
        result.putExtra("foodName", foodName);
        result.putExtra("totalKcal", kcal);
        result.putExtra("grams", grams);
        result.putExtra("mealType", selectedMealType);
        for (FoodItem item : foodItemListFull) {
            if (item.getName() != null && item.getName().equals(foodName)) {
                result.putExtra("foodImage", item.getImage());
                break;
            }
        }
        setResult(RESULT_OK, result);
        finish();
    }
}