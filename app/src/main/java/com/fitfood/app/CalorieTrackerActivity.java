package com.fitfood.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.fitfood.app.databinding.ActivityCalorieTrackerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Locale;
import java.util.Objects;

public class CalorieTrackerActivity extends AppCompatActivity {
    private ActivityCalorieTrackerBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private int selectedWeight = 70;
    private int selectedHeight = 170;
    private int selectedAge = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalorieTrackerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        setupSpinners();
        setupPickers();
        if (currentUser != null) {
            loadUserData();
        }
        binding.btnCalculate.setOnClickListener(v -> performCalculation(true));
        binding.btnSaveProfile.setOnClickListener(v -> saveToFirebase());
    }

    private void setupPickers() {
        binding.tvWeightPicker.setOnClickListener(v -> showNumberPickerDialog("Weight (kg)", 30, 300, selectedWeight, val -> {
            selectedWeight = val;
            binding.tvWeightPicker.setText(String.valueOf(selectedWeight));
        }));
        binding.tvHeightPicker.setOnClickListener(v -> showNumberPickerDialog("Height (cm)", 100, 250, selectedHeight, val -> {
            selectedHeight = val;
            binding.tvHeightPicker.setText(String.valueOf(selectedHeight));
        }));
        binding.tvAgePicker.setOnClickListener(v -> showNumberPickerDialog("Age", 10, 100, selectedAge, val -> {
            selectedAge = val;
            binding.tvAgePicker.setText(String.valueOf(selectedAge));
        }));
    }

    private void showNumberPickerDialog(String title, int min, int max, int current, PickerCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_number_picker, null);
        builder.setView(dialogView);
        builder.setTitle(title);
        NumberPicker numberPicker = dialogView.findViewById(R.id.numberPicker);
        numberPicker.setMinValue(min);
        numberPicker.setMaxValue(max);
        numberPicker.setValue(current);
        numberPicker.setWrapSelectorWheel(false);
        builder.setPositiveButton("Set", (dialog, which) -> callback.onValueSelected(numberPicker.getValue()));
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void performCalculation(boolean withDelay) {
        if (!withDelay) {
            calculateAndShowResult();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.resultCard.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            binding.progressBar.setVisibility(View.GONE);
            calculateAndShowResult();
        }, 1000);
    }

    private void calculateAndShowResult() {
        String sex = binding.inputSex.getSelectedItem().toString();
        String activity = binding.inputActivity.getSelectedItem().toString();
        String goal = binding.inputGoal.getSelectedItem().toString();
        double tdee = getTdee(sex, activity);
        long targetCalories = Math.round(tdee);
        if (goal.contains("Loss")) targetCalories -= 500;
        else if (goal.contains("Gain")) targetCalories += 300;
        if (targetCalories < 1200) targetCalories = 1200;
        double heightM = selectedHeight / 100.0;
        double bmi = selectedWeight / (heightM * heightM);
        String bmiStatus;
        int color;
        if (bmi < 18.5) {
            bmiStatus = "Underweight";
            color = Color.parseColor("#FFC107");
        } else if (bmi < 25) {
            bmiStatus = "Normal";
            color = Color.parseColor("#4CAF50");
        } else if (bmi < 30) {
            bmiStatus = "Overweight";
            color = Color.parseColor("#FF9800");
        } else {
            bmiStatus = "Obese";
            color = Color.RED;
        }
        binding.tvCalorieResult.setText(String.format(Locale.getDefault(), "%,d", targetCalories));
        binding.tvBMIResult.setText(String.format(Locale.getDefault(), "BMI: %.1f (%s)", bmi, bmiStatus));
        binding.tvBMIResult.setTextColor(color);
        String warningMessage = "";
        if (selectedAge >= 35) {
            warningMessage = "⚠️ Note: In case you are 35 and above, consider reducing your calorie intake and check with a health expert for proper guidance.";
        }
        else if (bmi >= 30) {
            warningMessage = "⚠️ Note: Your BMI indicates obesity. We recommend consulting a healthcare provider for a personalized plan.";
        }
        else if (bmi < 18.5) {
            warningMessage = "⚠️ Note: You are underweight. Focus on nutrient-dense foods and consult a nutritionist.";
        }
        else if (goal.contains("Extreme")) {
            warningMessage = "⚠️ Note: Extreme weight loss can be risky. Ensure you stay hydrated and don't skip meals.";
        }
        if (!warningMessage.isEmpty()) {
            binding.tvHealthNote.setText(warningMessage);
            binding.tvHealthNote.setVisibility(View.VISIBLE);
        } else {
            binding.tvHealthNote.setVisibility(View.GONE);
        }
        binding.resultCard.setVisibility(View.VISIBLE);
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500);
        binding.resultCard.startAnimation(fadeIn);
    }

    private double getTdee(String sex, String activity) {
        double bmr = sex.equalsIgnoreCase("Male")
                ? 10 * selectedWeight + 6.25 * selectedHeight - 5 * selectedAge + 5
                : 10 * selectedWeight + 6.25 * selectedHeight - 5 * selectedAge - 161;
        double multiplier = 1.2;
        if (activity.contains("Light")) multiplier = 1.375;
        else if (activity.contains("Moderate")) multiplier = 1.55;
        else if (activity.contains("Very active")) multiplier = 1.725;
        else if (activity.contains("Extra active")) multiplier = 1.9;
        return bmr * multiplier;
    }

    private void saveToFirebase() {
        if (currentUser == null) return;
        String calText = binding.tvCalorieResult.getText().toString().replace(",", "").trim();
        if (calText.isEmpty() || !calText.matches("\\d+")) {
            Toast.makeText(this, "Please calculate first.", Toast.LENGTH_SHORT).show();
            return;
        }
        String goal = binding.inputGoal.getSelectedItem().toString();
        int calories = Integer.parseInt(calText);
        db.collection("users").document(currentUser.getUid())
                .update(
                        "weight", selectedWeight,
                        "height", selectedHeight,
                        "age", selectedAge,
                        "sex", binding.inputSex.getSelectedItem().toString(),
                        "activity", binding.inputActivity.getSelectedItem().toString(),
                        "goal", goal,
                        "dailyCalories", calories
                )
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile Updated & Target Saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save.", Toast.LENGTH_SHORT).show());
    }

    private void loadUserData() {
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Double w = document.getDouble("weight");
                        Double h = document.getDouble("height");
                        Long a = document.getLong("age");
                        if (w != null && w > 0) {
                            selectedWeight = w.intValue();
                            binding.tvWeightPicker.setText(String.valueOf(selectedWeight));
                        }
                        if (h != null && h > 0) {
                            selectedHeight = h.intValue();
                            binding.tvHeightPicker.setText(String.valueOf(selectedHeight));
                        }
                        if (a != null && a > 0) {
                            selectedAge = a.intValue();
                            binding.tvAgePicker.setText(String.valueOf(selectedAge));
                        }
                        if (document.contains("sex")) setSpinnerSelection(binding.inputSex, document.getString("sex"));
                        if (document.contains("activity")) setSpinnerSelection(binding.inputActivity, document.getString("activity"));
                        if (document.contains("goal")) setSpinnerSelection(binding.inputGoal, document.getString("goal"));
                        if (w != null && h != null) {
                            performCalculation(false);
                        }
                    }
                });
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> sexAdapter = ArrayAdapter.createFromResource(this, R.array.sex_array, android.R.layout.simple_spinner_item);
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputSex.setAdapter(sexAdapter);
        ArrayAdapter<CharSequence> activityAdapter = ArrayAdapter.createFromResource(this, R.array.activity_array, android.R.layout.simple_spinner_item);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputActivity.setAdapter(activityAdapter);
        ArrayAdapter<CharSequence> goalAdapter = ArrayAdapter.createFromResource(this, R.array.goal_array, android.R.layout.simple_spinner_item);
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputGoal.setAdapter(goalAdapter);
    }

    private void setSpinnerSelection(android.widget.Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (Objects.requireNonNull(adapter.getItem(i)).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
    interface PickerCallback {
        void onValueSelected(int value);
    }
}