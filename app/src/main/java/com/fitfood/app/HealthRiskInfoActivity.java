package com.fitfood.app;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

public class HealthRiskInfoActivity extends AppCompatActivity {
    private LinearLayout container;
    private final boolean isEnglish = true;
    private FirebaseFirestore db;
    private String collectionName = "healthRisks";
    private String goalType = "loss";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_risk_info);

        ImageView backButton = findViewById(R.id.backButton);
        container = findViewById(R.id.riskButtonContainer);

        db = FirebaseFirestore.getInstance();

        if (getIntent() != null && getIntent().hasExtra("goalType")) {
            goalType = getIntent().getStringExtra("goalType");
        }

        collectionName = "gain".equalsIgnoreCase(goalType) ? "healthRisks_v2" : "healthRisks";

        Log.d("HealthRiskInfo", "GoalType: " + goalType + " | Collection: " + collectionName);

        loadHealthRisks();

        backButton.setOnClickListener(v -> finish());
    }

    private void loadHealthRisks() {
        container.removeAllViews();

        db.collection(collectionName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No health risks found.", Toast.LENGTH_SHORT).show();
                        Log.w("HealthRiskInfo", "No data in " + collectionName);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String riskName = isEnglish ? doc.getString("name") : doc.getString("name_tl");
                        String riskId = doc.getId();

                        if (riskName == null) riskName = "(Unnamed Risk)";

                        Button btn = new Button(this);
                        btn.setText(riskName);
                        btn.setAllCaps(false);
                        btn.setTextSize(18f);
                        btn.setBackgroundResource(R.drawable.button_outline);
                        btn.setPadding(24, 24, 24, 24);

                        btn.setOnClickListener(v -> {
                            Intent intent = new Intent(this, HealthRiskDetailActivity.class);
                            intent.putExtra("risk_id", riskId);
                            intent.putExtra("collection_name", collectionName);
                            intent.putExtra("isEnglish", isEnglish);
                            startActivity(intent);
                        });

                        container.addView(btn);
                    }

                    Log.d("HealthRiskInfo", "Loaded " + querySnapshot.size() + " risks from " + collectionName);
                })
                .addOnFailureListener(e -> {
                    Log.e("HealthRiskInfo", "Error loading health risks: ", e);
                    Toast.makeText(this, "Failed to load health risks.", Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressWarnings("deprecation")
    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = getResources().getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            createConfigurationContext(config);
        } else {
            config.locale = locale;
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        }
    }
}
