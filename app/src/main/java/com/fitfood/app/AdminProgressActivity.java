package com.fitfood.app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdminProgressActivity extends AppCompatActivity {
    private static class AdminProgressEntry {
        String date;
        float weight;
        float calories;
        float bmi;
        AdminProgressEntry(String d, float w, float c, float b) {
            date = d; weight = w; calories = c; bmi = b;
        }
    }
    private Spinner spinnerUsers;
    private LineChart lineChartProgress;
    private RadioGroup chartToggleGroup;
    private TextView tvCurrentBMI, tvCurrentStatus, tvLastLogged, tvNoData;
    private FirebaseFirestore db;
    private final Map<String, String> userMap = new HashMap<>();
    private ArrayAdapter<String> spinnerAdapter;
    private final List<AdminProgressEntry> currentDataList = new ArrayList<>();
    private float currentUserHeight = 1.65f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_progress);

        spinnerUsers = findViewById(R.id.spinnerUsers);
        lineChartProgress = findViewById(R.id.lineChartProgress);
        chartToggleGroup = findViewById(R.id.chartToggleGroup);
        tvCurrentBMI = findViewById(R.id.tvCurrentBMI);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        tvLastLogged = findViewById(R.id.tvLastLogged);
        tvNoData = findViewById(R.id.tvNoData);

        db = FirebaseFirestore.getInstance();

        setupChartStyle();
        loadUsers();

        spinnerUsers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String name = spinnerAdapter.getItem(position);
                String uid = userMap.get(name);
                if (uid != null) fetchUserProfile(uid);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        chartToggleGroup.setOnCheckedChangeListener((group, checkedId) -> updateChartDisplay(checkedId));
    }

    private void setupChartStyle() {
        lineChartProgress.getDescription().setEnabled(false);
        lineChartProgress.getLegend().setEnabled(false);
        lineChartProgress.setExtraBottomOffset(10f);
        lineChartProgress.getAxisRight().setEnabled(false);
        lineChartProgress.setDragEnabled(true);
        lineChartProgress.setScaleEnabled(true);
        lineChartProgress.setPinchZoom(true);

        XAxis xAxis = lineChartProgress.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.DKGRAY);

        lineChartProgress.getAxisLeft().setDrawGridLines(true);
        lineChartProgress.getAxisLeft().enableGridDashedLine(10f, 10f, 0f);
        lineChartProgress.getAxisLeft().setTextColor(Color.DKGRAY);
        lineChartProgress.setNoDataText("Select a user to view progress");
    }

    private void loadUsers() {
        db.collection("users").get().addOnSuccessListener(snapshots -> {
            List<String> names = new ArrayList<>();
            userMap.clear();
            for (DocumentSnapshot doc : snapshots) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String display = (name != null && !name.isEmpty()) ? name : (email != null ? email : "Unknown");
                int count = 1;
                String original = display;
                while (userMap.containsKey(display)) {
                    count++;
                    display = original + " (" + count + ")";
                }
                userMap.put(display, doc.getId());
                names.add(display);
            }
            Collections.sort(names);
            spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
            spinnerUsers.setAdapter(spinnerAdapter);
        });
    }

    private void fetchUserProfile(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    Double h = getNumberAsDouble(doc, "height");
                    if (h != null && h > 0) {
                        currentUserHeight = h > 3.0 ? (float)(h / 100.0) : h.floatValue();
                    }
                    loadUserProgress(userId);
                })
                .addOnFailureListener(e -> loadUserProgress(userId));
    }

    private void loadUserProgress(String userId) {
        db.collection("users").document(userId)
                .collection("progress")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    currentDataList.clear();
                    if (querySnapshot.isEmpty()) {
                        clearChart();
                        resetSummary();
                        return;
                    }
                    for (DocumentSnapshot doc : querySnapshot) {
                        Double w = getNumberAsDouble(doc, "weight");
                        Number cnum = doc.get("calories") instanceof Number ? (Number)doc.get("calories") : null;
                        Double caloriesDouble = cnum != null ? cnum.doubleValue() : null;
                        String date = doc.getString("date");
                        if (date == null || date.trim().isEmpty()) {
                            date = doc.getId();
                        }
                        float weight = w != null ? w.floatValue() : 0f;
                        float cal = caloriesDouble != null ? caloriesDouble.floatValue() : 0f;
                        float bmi = 0f;
                        if (weight > 0 && currentUserHeight > 0) {
                            bmi = weight / (currentUserHeight * currentUserHeight);
                        }
                        if (weight <= 0 && cal <= 0) continue;
                        currentDataList.add(new AdminProgressEntry(date, weight, cal, bmi));
                    }
                    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Collections.sort(currentDataList, (o1, o2) -> {
                        try {
                            Date d1 = sdf.parse(o1.date);
                            Date d2 = sdf.parse(o2.date);
                            if (d1 != null && d2 != null) return d1.compareTo(d2);
                        } catch (ParseException e) {
                            return o1.date.compareTo(o2.date);
                        }
                        return 0;
                    });

                    updateSummaryCard();
                    updateChartDisplay(chartToggleGroup.getCheckedRadioButtonId());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show());
    }

    private Double getNumberAsDouble(DocumentSnapshot doc, String key) {
        try {
            Object o = doc.get(key);
            if (o instanceof Number) {
                return ((Number) o).doubleValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateSummaryCard() {
        if (currentDataList.isEmpty()) {
            resetSummary();
            return;
        }
        AdminProgressEntry latest = currentDataList.get(currentDataList.size() - 1);
        tvCurrentBMI.setText(String.format("%.1f", latest.bmi));
        tvLastLogged.setText("Last Logged: " + latest.date + " (" + currentDataList.size() + " entries)");
        String status;
        int color;
        if (latest.bmi < 18.5) { status = "Underweight"; color = Color.parseColor("#FFC107"); }
        else if (latest.bmi <= 22.9) { status = "Normal"; color = Color.parseColor("#4CAF50"); }
        else if (latest.bmi <= 24.9) { status = "Overweight"; color = Color.parseColor("#FF9800"); }
        else { status = "Obese"; color = Color.parseColor("#F44336"); }
        tvCurrentStatus.setText(status);
        tvCurrentStatus.setTextColor(color);
    }

    private void updateChartDisplay(int checkedId) {
        if (currentDataList.isEmpty()) {
            clearChart();
            return;
        }
        tvNoData.setVisibility(View.GONE);
        lineChartProgress.setVisibility(View.VISIBLE);

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int primaryColor = Color.BLUE;
        int fillStart = Color.GRAY;
        int fillEnd = Color.TRANSPARENT;

        for (int i = 0; i < currentDataList.size(); i++) {
            AdminProgressEntry p = currentDataList.get(i);
            float val = 0;
            if (checkedId == R.id.rbWeight) {
                val = p.weight;
                primaryColor = Color.parseColor("#FF5722");
                fillStart = Color.parseColor("#FF8A65");
                fillEnd = Color.parseColor("#FFFFFF");
            } else if (checkedId == R.id.rbCalories) {
                val = p.calories;
                primaryColor = Color.parseColor("#2196F3");
                fillStart = Color.parseColor("#64B5F6");
                fillEnd = Color.parseColor("#FFFFFF");
            } else {
                val = p.bmi;
                primaryColor = Color.parseColor("#4CAF50");
                fillStart = Color.parseColor("#81C784");
                fillEnd = Color.parseColor("#FFFFFF");
            }

            if (val > 0) {
                String label = p.date.length() >= 5 ? p.date.substring(5) : p.date;
                labels.add(label);
                entries.add(new Entry(entries.size(), val));
            }
        }

        if (entries.isEmpty()) {
            clearChart();
            return;
        }

        LineDataSet set = new LineDataSet(entries, "Data");

        set.setColor(primaryColor);
        set.setCircleColor(primaryColor);
        set.setLineWidth(2.5f);
        set.setCircleRadius(4f);
        set.setDrawCircleHole(true);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        set.setDrawFilled(true);
        if (Build.VERSION.SDK_INT >= 18) {
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{fillStart, fillEnd}
            );
            set.setFillDrawable(gradientDrawable);
        } else {
            set.setFillColor(fillStart);
            set.setFillAlpha(100);
        }

        lineChartProgress.setData(new LineData(set));
        try {
            CustomMarkerView mv = new CustomMarkerView(this, R.layout.custom_marker_view, labels);
            mv.setChartView(lineChartProgress);
            lineChartProgress.setMarker(mv);
        } catch (Exception e) {
            // fallback
        }
        XAxis xAxis = lineChartProgress.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        int labelCount = Math.min(labels.size(), 6);
        xAxis.setLabelCount(labelCount, false);
        xAxis.setGranularity(1f);

        if (checkedId == R.id.rbCalories) {
            lineChartProgress.getAxisLeft().setAxisMinimum(0f);
        } else {
            lineChartProgress.getAxisLeft().resetAxisMinimum();
        }
        lineChartProgress.animateY(1000);
        lineChartProgress.invalidate();
    }

    private void clearChart() {
        lineChartProgress.clear();
        lineChartProgress.setVisibility(View.INVISIBLE);
        tvNoData.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void resetSummary() {
        tvCurrentBMI.setText("--");
        tvCurrentStatus.setText("--");
        tvLastLogged.setText("Last Logged: --");
    }
}
