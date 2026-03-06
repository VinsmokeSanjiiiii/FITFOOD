package com.fitfood.app;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminMealLogActivity extends AppCompatActivity {
    private MealLogAdapter adapter;
    private List<UserMealGroup> userGroupList;
    private List<UserMealGroup> userGroupListFull;
    private TextView tvSelectedDate, tvTotalCaloriesToday, tvEmptyView;
    private EditText etSearchMeal;
    private RadioGroup rgFilter;
    private SwipeRefreshLayout swipeRefresh;
    private FirebaseFirestore db;
    private Calendar selectedDateCalendar;
    private String selectedDateString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_meal_log);
        RecyclerView recyclerView = findViewById(R.id.rvMealLogs);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvTotalCaloriesToday = findViewById(R.id.tvTotalCaloriesToday);
        tvEmptyView = findViewById(R.id.tvEmptyView);
        etSearchMeal = findViewById(R.id.etSearchMeal);
        rgFilter = findViewById(R.id.rgFilter);
        ImageButton btnPickDate = findViewById(R.id.btnPickDate);
        ImageButton btnPrevDate = findViewById(R.id.btnPrevDate);
        ImageButton btnNextDate = findViewById(R.id.btnNextDate);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        db = FirebaseFirestore.getInstance();
        userGroupList = new ArrayList<>();
        userGroupListFull = new ArrayList<>();
        selectedDateCalendar = Calendar.getInstance();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MealLogAdapter(this, userGroupList, this::deleteMealLog);
        recyclerView.setAdapter(adapter);
        updateDateLabel();
        loadLogsForSelectedDate();
        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPrevDate.setOnClickListener(v -> changeDate(-1));
        btnNextDate.setOnClickListener(v -> changeDate(1));
        swipeRefresh.setOnRefreshListener(this::loadLogsForSelectedDate);
        etSearchMeal.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { filterMeals(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });
        rgFilter.setOnCheckedChangeListener((group, checkedId) -> filterMeals(etSearchMeal.getText().toString()));
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDateString = sdf.format(selectedDateCalendar.getTime());
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvSelectedDate.setText(displayFormat.format(selectedDateCalendar.getTime()));
    }

    private void changeDate(int days) {
        selectedDateCalendar.add(Calendar.DAY_OF_YEAR, days);
        updateDateLabel();
        loadLogsForSelectedDate();
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateCalendar.set(year, month, dayOfMonth);
            updateDateLabel();
            loadLogsForSelectedDate();
        },
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadLogsForSelectedDate() {
        if (!swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(true);
        userGroupList.clear();
        Map<String, UserMealGroup> groupsMap = new HashMap<>();
        db.collection("users").get().addOnSuccessListener(usersSnap -> {
            if (usersSnap.isEmpty()) {
                finishLoading(groupsMap);
                return;
            }
            int totalTasks = usersSnap.size() * 4;
            AtomicInteger completedTasks = new AtomicInteger(0);
            for (DocumentSnapshot userDoc : usersSnap.getDocuments()) {
                String userId = userDoc.getId();
                String userName = userDoc.getString("name");
                String displayName = (userName != null && !userName.isEmpty()) ? userName : "User (" + userId.substring(0, 4) + ")";
                String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snacks"};
                for (String type : mealTypes) {
                    db.collection("mealLogs").document(userId)
                            .collection("dates").document(selectedDateString)
                            .collection(type)
                            .get()
                            .addOnSuccessListener(mealSnap -> {
                                for (QueryDocumentSnapshot mealDoc : mealSnap) {
                                    if (mealDoc.getId().equals("meta")) continue;
                                    MealLog log = new MealLog();
                                    log.setId(mealDoc.getId());
                                    log.setMealName(mealDoc.getString("foodName"));
                                    Double cal = mealDoc.getDouble("calories");
                                    log.setCalories(cal != null ? cal.intValue() : 0);
                                    log.setMealType(type);
                                    log.setDate(selectedDateString);
                                    log.setUserId(userId);
                                    log.setUserName(displayName);

                                    String imgString = mealDoc.getString("foodImage");
                                    log.setFoodImage(imgString);

                                    synchronized (groupsMap) {
                                        UserMealGroup group = groupsMap.get(userId);
                                        if (group == null) {
                                            group = new UserMealGroup(userId, displayName);
                                            groupsMap.put(userId, group);
                                        }
                                        group.addMeal(log);
                                    }
                                }
                            })
                            .addOnCompleteListener(task -> {
                                if (completedTasks.incrementAndGet() == totalTasks) {
                                    finishLoading(groupsMap);
                                }
                            });
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void finishLoading(Map<String, UserMealGroup> groupsMap) {
        swipeRefresh.setRefreshing(false);
        userGroupList.addAll(groupsMap.values());
        Collections.sort(userGroupList, (o1, o2) -> o1.getUserName().compareToIgnoreCase(o2.getUserName()));
        userGroupListFull.clear();
        for (UserMealGroup originalGroup : userGroupList) {
            UserMealGroup copy = new UserMealGroup(originalGroup.getUserId(), originalGroup.getUserName());
            for (MealLog m : originalGroup.getMeals()) copy.addMeal(m);
            userGroupListFull.add(copy);
        }
        updateUI();
        filterMeals(etSearchMeal.getText().toString());
    }
    @SuppressLint("SetTextI18n")
    private void updateUI() {
        int totalCal = 0;
        for (UserMealGroup group : userGroupList) {
            totalCal += group.getTotalCalories();
        }
        tvTotalCaloriesToday.setText(String.format(Locale.getDefault(), "%,d kcal", totalCal));
        adapter.updateList(userGroupList);
        if (userGroupList.isEmpty()) {
            tvEmptyView.setVisibility(View.VISIBLE);
            tvEmptyView.setText("No meals found for " + selectedDateString);
        } else {
            tvEmptyView.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void filterMeals(String query) {
        String lowerQuery = query.toLowerCase();
        List<UserMealGroup> filteredGroups = new ArrayList<>();
        String typeFilter = "All";
        int checkedId = rgFilter.getCheckedRadioButtonId();
        if (checkedId == R.id.rbBreakfast) typeFilter = "Breakfast";
        else if (checkedId == R.id.rbLunch) typeFilter = "Lunch";
        else if (checkedId == R.id.rbDinner) typeFilter = "Dinner";
        for (UserMealGroup fullGroup : userGroupListFull) {
            boolean userMatches = fullGroup.getUserName().toLowerCase().contains(lowerQuery);
            List<MealLog> matchingMeals = new ArrayList<>();
            for (MealLog m : fullGroup.getMeals()) {
                boolean mealMatches = userMatches || (m.getMealName() != null && m.getMealName().toLowerCase().contains(lowerQuery));
                boolean typeMatches = typeFilter.equals("All") || m.getMealType().equalsIgnoreCase(typeFilter);
                if (mealMatches && typeMatches) {
                    matchingMeals.add(m);
                }
            }
            if (!matchingMeals.isEmpty()) {
                UserMealGroup newGroup = new UserMealGroup(fullGroup.getUserId(), fullGroup.getUserName());
                newGroup.setMeals(matchingMeals);
                filteredGroups.add(newGroup);
            }
        }
        adapter.updateList(filteredGroups);
        if (filteredGroups.isEmpty()) {
            tvEmptyView.setVisibility(View.VISIBLE);
            tvEmptyView.setText("No matching results.");
        } else {
            tvEmptyView.setVisibility(View.GONE);
        }
    }

    private void deleteMealLog(MealLog meal) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry?")
                .setMessage("Remove " + meal.getMealName() + " from " + meal.getUserName() + "'s log?")
                .setPositiveButton("Delete", (dialog, which) -> db.collection("mealLogs").document(meal.getUserId())
                        .collection("dates").document(meal.getDate())
                        .collection(meal.getMealType()).document(meal.getId())
                        .delete()
                        .addOnSuccessListener(a -> {
                            Toast.makeText(this, "Entry Deleted", Toast.LENGTH_SHORT).show();
                            loadLogsForSelectedDate();
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}