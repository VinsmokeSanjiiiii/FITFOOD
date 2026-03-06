package com.fitfood.app;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class HealthRiskDetailActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView tvTitle, tvDescription;
    private boolean isEnglish = true;

    private FirebaseFirestore db;
    private String collectionName;
    private String riskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_risk_detail);

        ImageView settingsIcon = findViewById(R.id.settingsIcon);
        ImageView backButton = findViewById(R.id.backButton);
        imageView = findViewById(R.id.image);
        tvTitle = findViewById(R.id.title);
        tvDescription = findViewById(R.id.description);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if (intent != null) {
            riskId = intent.getStringExtra("risk_id");
            collectionName = intent.getStringExtra("collection_name");
        }

        if (collectionName == null || collectionName.isEmpty()) {
            collectionName = "healthRisks";
        }

        Locale currentLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            currentLocale = getResources().getConfiguration().getLocales().get(0);
        } else {
            currentLocale = getResources().getConfiguration().locale;
        }
        isEnglish = currentLocale.getLanguage().equals("en");

        Log.d("HealthRiskDetail", "riskId: " + riskId + " | Collection: " + collectionName + " | English: " + isEnglish);

        loadHealthRisk();

        settingsIcon.setOnClickListener(v -> {
            String[] languages = {"English", "Tagalog"};
            new AlertDialog.Builder(this)
                    .setTitle("Select Language / Piliin ang Wika")
                    .setItems(languages, (dialog, which) -> {
                        setLocale(which == 0 ? "en" : "tl");
                        recreate();
                    })
                    .show();
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void loadHealthRisk() {
        if (riskId == null || riskId.isEmpty()) {
            Toast.makeText(this, "Invalid health risk ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection(collectionName).document(riskId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Health risk not found.", Toast.LENGTH_SHORT).show();
                        Log.w("HealthRiskDetail", "Missing document for id: " + riskId);
                        return;
                    }

                    String title = isEnglish ? doc.getString("name") : doc.getString("name_tl");
                    tvTitle.setText(title != null ? title : "(No title)");

                    String desc1 = isEnglish ? doc.getString("desc_1_explain") : doc.getString("desc_1_tl");
                    String desc2 = isEnglish ? doc.getString("desc_2_symptoms") : doc.getString("desc_2_tl");
                    String desc3 = isEnglish ? doc.getString("desc_3_note") : doc.getString("desc_3_tl");

                    StringBuilder fullDesc = new StringBuilder();
                    if (desc1 != null) fullDesc.append(desc1).append("\n\n");
                    if (desc2 != null) fullDesc.append(desc2).append("\n\n");
                    if (desc3 != null) fullDesc.append(desc3);
                    tvDescription.setText(fullDesc.toString().trim());

                    String imageName = doc.getString("image");
                    int resId = (imageName != null && !imageName.isEmpty())
                            ? getResources().getIdentifier(imageName, "drawable", getPackageName())
                            : 0;
                    imageView.setImageResource(resId != 0 ? resId : R.drawable.high_blood_preassure);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("HealthRiskDetail", "Error loading document", e);
                });
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = getResources().getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            createConfigurationContext(config);
        } else {

            config.setLocale(locale);
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        }
    }
}
