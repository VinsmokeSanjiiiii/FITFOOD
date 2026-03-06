package com.fitfood.app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProgressReportActivity extends AppCompatActivity {
    private TextView tvBMI, tvFeedback, tvSummary, tvTips;
    private ListView listProgress;
    private LineChart lineChart;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> displayList = new ArrayList<>();
    private final List<ProgressEntry> fullDataList = new ArrayList<>();
    private double height = 1.65;
    private double currentWeight = 70.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_report);

        tvBMI = findViewById(R.id.tvBMI);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvSummary = findViewById(R.id.tvSummaryStats);
        tvTips = findViewById(R.id.tvTips);
        listProgress = findViewById(R.id.listProgress);
        lineChart = findViewById(R.id.lineChart);
        ImageButton btnBack = findViewById(R.id.btnBack);
        RadioGroup chartToggleGroup = findViewById(R.id.chartToggleGroup);

        btnBack.setOnClickListener(v -> onBackPressed());

        setupChartStyle();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(Color.parseColor("#424242"));
                text.setTextSize(14);
                return view;
            }
        };
        listProgress.setAdapter(adapter);
        chartToggleGroup.setOnCheckedChangeListener((group, checkedId) -> updateChartDisplay(checkedId));

        fetchUserProfile();
        loadProgress();
    }

    private void setupChartStyle() {
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setExtraBottomOffset(10f);
        lineChart.getAxisRight().setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.DKGRAY);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setTextColor(Color.DKGRAY);
    }

    private void fetchUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(document -> {
            if (!document.exists()) return;
            Double h = getNumberAsDouble(document, "height");
            Double w = getNumberAsDouble(document, "weight");
            if (h != null && h > 0) {
                height = h > 3.0 ? h / 100.0 : h;
            }
            if (w != null && w > 0) currentWeight = w;

            recalculateBMIForAll();
            updateBMIDisplay(currentWeight);

            RadioGroup group = findViewById(R.id.chartToggleGroup);
            updateChartDisplay(group.getCheckedRadioButtonId());
        });
    }

    private void updateBMIDisplay(double weight) {
        if (height <= 0) return;
        double bmi = weight / (height * height);
        String feedback;
        int colorRes;
        if (bmi < 18.5) {
            feedback = "Underweight";
            colorRes = Color.parseColor("#FFCA28");
        } else if (bmi <= 22.9) {
            feedback = "Normal Weight";
            colorRes = Color.parseColor("#66BB6A");
        } else if (bmi <= 24.9) {
            feedback = "Overweight";
            colorRes = Color.parseColor("#FF7043");
        } else {
            feedback = "Obese";
            colorRes = Color.parseColor("#EF5350");
        }
        tvBMI.setText(String.format(Locale.getDefault(), "BMI: %.1f", bmi));
        tvFeedback.setText(feedback);
        tvFeedback.setTextColor(colorRes);
    }

    private void loadProgress() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .collection("progress")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullDataList.clear();
                    displayList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ProgressEntry p = doc.toObject(ProgressEntry.class);

                        if (p.getDate() == null || p.getDate().trim().isEmpty()) {
                            p.setDate(doc.getId());
                        }

                        if (p.getWeight() <= 0 && p.getCalories() <= 0) {

                            Double w = getNumberAsDouble(doc, "weight");
                            Integer c = getNumberAsInt(doc, "calories");
                            if ((w == null || w <= 0) && (c == null || c <= 0)) continue;
                            if (w != null) p.setWeight(w);
                            if (c != null) p.setCalories(c);
                        }
                        fullDataList.add(p);
                    }

                    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Collections.sort(fullDataList, (o1, o2) -> {
                        try {
                            Date d1 = sdf.parse(o1.getDate());
                            Date d2 = sdf.parse(o2.getDate());
                            if (d1 != null && d2 != null) return d1.compareTo(d2);
                        } catch (ParseException e) {
                            return o1.getDate().compareTo(o2.getDate());
                        }
                        return 0;
                    });

                    recalculateBMIForAll();

                    for (ProgressEntry p : fullDataList) {
                        @SuppressLint("DefaultLocale")
                        String row = String.format("%s   |   %.1f kg   |   %d kcal",
                                p.getDate(), p.getWeight(), p.getCalories());
                        displayList.add(row);
                    }

                    Collections.reverse(displayList);
                    adapter.notifyDataSetChanged();
                    fixListViewHeight(listProgress);
                    updateSummaryAndTips(fullDataList);

                    if (!fullDataList.isEmpty()) {
                        ProgressEntry latest = fullDataList.get(fullDataList.size() - 1);
                        if (latest.getWeight() > 0) {
                            currentWeight = latest.getWeight();
                            updateBMIDisplay(currentWeight);
                        }
                    }

                    RadioGroup group = findViewById(R.id.chartToggleGroup);
                    updateChartDisplay(group.getCheckedRadioButtonId());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    private Integer getNumberAsInt(DocumentSnapshot doc, String key) {
        try {
            Object o = doc.get(key);
            if (o instanceof Number) {
                return ((Number) o).intValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void recalculateBMIForAll() {
        if (height <= 0) return;
        for (ProgressEntry p : fullDataList) {
            if (p.getWeight() > 0) p.setBmi(p.getWeight() / (height * height));
        }
    }

    private void updateChartDisplay(int checkedId) {
        lineChart.clear();
        if (fullDataList.isEmpty()) {
            lineChart.setNoDataText("No progress data available yet.");
            lineChart.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        String label = "";

        int primaryColor = Color.BLACK;
        int fillColorStart = Color.GRAY;
        int fillColorEnd = Color.TRANSPARENT;

        for (int i = 0; i < fullDataList.size(); i++) {
            ProgressEntry p = fullDataList.get(i);
            float val = 0f;
            if (checkedId == R.id.rbWeight) {
                val = (float) p.getWeight();
                label = "Weight (kg)";
                primaryColor = Color.parseColor("#FF5722");
                fillColorStart = Color.parseColor("#FF8A65");
                fillColorEnd = Color.parseColor("#ffffff");
            } else if (checkedId == R.id.rbCalories) {
                val = (float) p.getCalories();
                label = "Calories (kcal)";
                primaryColor = Color.parseColor("#1E88E5");
                fillColorStart = Color.parseColor("#64B5F6");
                fillColorEnd = Color.parseColor("#ffffff");
            } else if (checkedId == R.id.rbBMI) {
                val = (float) p.getBmi();
                label = "BMI";
                primaryColor = Color.parseColor("#43A047");
                fillColorStart = Color.parseColor("#81C784");
                fillColorEnd = Color.parseColor("#ffffff");
            }

            if (val > 0) {
                String dateStr = (p.getDate() != null && p.getDate().length() >= 5)
                        ? p.getDate().substring(5)
                        : p.getDate() != null ? p.getDate() : "??";

                entries.add(new Entry(i, val));
                labels.add(dateStr);
            } else {
                if (p.getDate() != null) {
                    labels.add(p.getDate().length() >= 5 ? p.getDate().substring(5) : p.getDate());
                }
            }
        }

        if (entries.isEmpty()) {
            lineChart.setNoDataText("No data for selected metric.");
            lineChart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, label);

        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setColor(primaryColor);
        dataSet.setCircleColor(primaryColor);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setDrawValues(false);

        dataSet.setDrawFilled(true);
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{fillColorStart, fillColorEnd}
        );
        dataSet.setFillDrawable(drawable);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(Math.min(labels.size(), 6), false);
        xAxis.setGranularity(1f);

        try {
            CustomMarkerView mv = new CustomMarkerView(this, R.layout.custom_marker_view, labels);
            mv.setChartView(lineChart);
            lineChart.setMarker(mv);
        } catch (Exception e) {
            // Fail gracefully
        }

        if (checkedId == R.id.rbCalories) {
            lineChart.getAxisLeft().setAxisMinimum(0f);
        } else {
            lineChart.getAxisLeft().resetAxisMinimum();
        }
        lineChart.animateY(1000);
        lineChart.invalidate();
    }

    private void updateSummaryAndTips(List<ProgressEntry> entries) {
        if (entries.isEmpty()) return;
        double totalWeight = 0;
        double totalCalories = 0;
        int weightCount = 0;
        int calorieCount = 0;

        for (ProgressEntry p : entries) {
            if (p.getWeight() > 0) {
                totalWeight += p.getWeight();
                weightCount++;
            }
            if (p.getCalories() > 0) {
                totalCalories += p.getCalories();
                calorieCount++;
            }
        }

        double avgWeight = weightCount > 0 ? totalWeight / weightCount : 0;
        double avgCalories = calorieCount > 0 ? totalCalories / calorieCount : 0;

        tvSummary.setText(String.format(Locale.getDefault(),
                "Avg Weight: %.1f kg\nAvg Intake: %.0f kcal/day\nTotal Logs: %d",
                avgWeight, avgCalories, entries.size()));

        ProgressEntry latest = entries.get(entries.size() - 1);
        double latestBMI = latest.getWeight() > 0 ? latest.getWeight() / (height * height) : 0;

        String tips;
        if (latestBMI == 0) tips = "Keep logging your weight to get health insights!";
        else if (latestBMI < 18.5) tips = "You are currently Underweight. \nTip: Focus on nutrient-dense foods like nuts, avocados, and healthy oils to gain mass safely.";
        else if (latestBMI <= 22.9) tips = "Great job! You are in the Normal/Healthy range. \nTip: Maintain your routine with balanced meals and regular activity.";
        else if (latestBMI <= 24.9) tips = "You are in the Overweight range. \nTip: Try increasing water intake before meals and incorporating 20 mins of daily walking.";
        else tips = "Your BMI indicates Obesity. \nTip: Consider a structured meal plan with high protein and fiber to keep you full longer.";

        tvTips.setText(tips);
    }

    public static void fixListViewHeight(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) return;
        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
