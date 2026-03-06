package com.fitfood.app;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HealthTipsActivity extends AppCompatActivity {

    private HealthTipsAdapter adapter;
    private List<HealthTip> tipList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_tips);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewTips);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ImageView settingsIcon = findViewById(R.id.settingsIcon);

        tipList = new ArrayList<>();
        adapter = new HealthTipsAdapter(this, tipList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadHealthTips();

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
    }

    private void setLocale(String lang) {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", lang);
        editor.apply();
    }

    private void loadHealthTips() {
        db.collection("healthTips")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        tipList.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            HealthTip tip = doc.toObject(HealthTip.class);
                            tipList.add(tip);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e("Firestore", "Error: ", task.getException());
                        Toast.makeText(HealthTipsActivity.this, "Failed to load health tips", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
