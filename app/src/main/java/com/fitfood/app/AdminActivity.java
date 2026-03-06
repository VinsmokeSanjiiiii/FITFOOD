package com.fitfood.app;

import androidx.appcompat.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.*;

public class AdminActivity extends AppCompatActivity {
    private UserAdapter userAdapter;
    private List<User> userList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView tvTotalUsers;
    private TextView tvTotalAdmins;
    private TextView tvAtRiskCount;
    private TextView tvGainCount;
    private TextView tvEmptyView, tvAdminName, tvAdminSubtitle;
    private ImageView imgAdminAvatar;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalAdmins = findViewById(R.id.tvTotalAdmins);
        tvAtRiskCount = findViewById(R.id.tvLossCount);
        tvGainCount = findViewById(R.id.tvGainCount);
        tvEmptyView = findViewById(R.id.tvEmptyView);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        LinearLayout headerProfileSection = findViewById(R.id.headerProfileSection);
        tvAdminName = findViewById(R.id.tvAdminNameHeader);
        tvAdminSubtitle = findViewById(R.id.tvAdminSubtitle);
        imgAdminAvatar = findViewById(R.id.imgAdminAvatarHeader);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        RecyclerView recyclerView = findViewById(R.id.rvUsers);
        Button btnManageFood = findViewById(R.id.btnManageFood);

        headerProfileSection.setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, AdminProfileActivity.class));
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();

        userAdapter = new UserAdapter(this, userList, new UserAdapter.OnUserActionListener() {
            @Override
            public void onMessageClick(User user) {
                Intent intent = new Intent(AdminActivity.this, ChatActivity.class);
                intent.putExtra("targetUserId", user.getUid());
                intent.putExtra("targetUserName", user.getName());
                startActivity(intent);
            }

            @Override
            public void onEditClick(User user) {
                showUpdateUserDialog(user);
            }

            @Override
            public void onDeleteClick(User user) {
                showDeleteConfirmationDialog(user);
            }
        });

        recyclerView.setAdapter(userAdapter);

        Button btnAddUser = findViewById(R.id.btnAddUser);
        Button btnRefreshUsers = findViewById(R.id.btnRefreshUsers);
        Button btnProgressMonitoring = findViewById(R.id.btnProgressMonitoring);
        Button btnMealLogs = findViewById(R.id.btnMealLogs);

        btnAddUser.setOnClickListener(v -> showAddUserDialog());
        btnRefreshUsers.setOnClickListener(v -> loadUsers());
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        swipeRefresh.setOnRefreshListener(this::loadUsers);

        btnProgressMonitoring.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, AdminProgressActivity.class)));
        btnMealLogs.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, AdminMealLogActivity.class)));
        btnManageFood.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, AdminFoodActivity.class)));

        EditText etSearchUser = findViewById(R.id.etSearchUser);
        etSearchUser.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { filterUsersSafe(s.toString()); }
            public void afterTextChanged(Editable s) { }
        });

        loadUsers();
        loadAdminHeaderData();
    }

    private void loadAdminHeaderData() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).addSnapshotListener((doc, e) -> {
            if (e != null || doc == null || !doc.exists()) return;
            String name = doc.getString("name");
            String photoUrl = doc.getString("profileImageUrl");
            tvAdminName.setText(name != null ? "Hi, " + name : "Hi, Admin");
            tvAdminSubtitle.setText("Tap to edit profile");
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(AdminActivity.this)
                        .load(photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.use2)
                        .into(imgAdminAvatar);
            }
        });
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to exit the admin panel?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences("FitFoodPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        auth.signOut();
        Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showDeleteConfirmationDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This action is permanent.\n\nAre you sure you want to delete the user '" + user.getName() + "' and all their data?")
                .setPositiveButton("Delete", (dialog, which) -> performDeleteUser(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDeleteUser(User user) {
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(user.getUid())) {
            Toast.makeText(this, "You cannot delete your own admin account!", Toast.LENGTH_LONG).show();
            return;
        }
        if (user.getRole() != null && user.getRole().equalsIgnoreCase("admin")) {
            Toast.makeText(this, "Security Restriction: You cannot delete another Admin account.", Toast.LENGTH_LONG).show();
            return;
        }
        swipeRefresh.setRefreshing(true);
        db.collection("users").document(user.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User removed successfully.", Toast.LENGTH_SHORT).show();
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUsers() {
        if (!swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(true);
        }
        db.collection("users").get()
                .addOnCompleteListener(task -> {
                    swipeRefresh.setRefreshing(false);
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            User user = doc.toObject(User.class);
                            user.setUid(doc.getId());
                            userList.add(user);
                        }
                        sortUsers(userList);
                        userAdapter.updateList(new ArrayList<>(userList));
                        updateAnalytics(userList);
                        updateEmptyView(userList.isEmpty());
                    } else {
                        Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyView(boolean isEmpty) {
        tvEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void updateAnalytics(List<User> users) {
        int total = users.size();
        int admins = 0;
        int weightLossGoals = 0;
        int gainWeightGoals = 0;
        for (User u : users) {
            if ("admin".equalsIgnoreCase(u.getRole())) admins++;
            if (u.getGoal() != null && u.getGoal().toLowerCase().contains("loss")) weightLossGoals++;
            if (u.getGoal() != null && u.getGoal().toLowerCase().contains("gain")) gainWeightGoals++;
        }
        tvTotalUsers.setText(String.valueOf(total));
        tvTotalAdmins.setText(String.valueOf(admins));
        tvAtRiskCount.setText(String.valueOf(weightLossGoals));
        tvGainCount.setText(String.valueOf(gainWeightGoals));
    }

    private void sortUsers(List<User> list) {
        Collections.sort(list, (u1, u2) -> {
            boolean isAdmin1 = "admin".equals(u1.getRole());
            boolean isAdmin2 = "admin".equals(u2.getRole());
            if (isAdmin1 && !isAdmin2) return -1;
            if (!isAdmin1 && isAdmin2) return 1;
            return (u1.getName() == null ? "" : u1.getName())
                    .compareToIgnoreCase(u2.getName() == null ? "" : u2.getName());
        });
    }

    private void filterUsersSafe(String query) {
        List<User> filteredList = new ArrayList<>();
        for (User user : userList) {
            if ((user.getName() != null && user.getName().toLowerCase().contains(query.toLowerCase())) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(query.toLowerCase()))) {
                filteredList.add(user);
            }
        }
        userAdapter.updateList(filteredList);
        updateEmptyView(filteredList.isEmpty());
    }

    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null);
        builder.setView(view);
        EditText etName = view.findViewById(R.id.etName);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPassword = view.findViewById(R.id.etPassword);
        EditText etBirthday = view.findViewById(R.id.etBirthday);
        Spinner spinnerRole = view.findViewById(R.id.spinnerRole);
        Button btnSave = view.findViewById(R.id.btnSaveUser);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"user", "admin"});
        spinnerRole.setAdapter(adapter);
        etBirthday.setInputType(InputType.TYPE_NULL);
        etBirthday.setOnClickListener(v -> showDatePicker(etBirthday));
        AlertDialog dialog = builder.create();
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String birthday = etBirthday.getText().toString().trim();
            String role = spinnerRole.getSelectedItem().toString();
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            createUser(name, email, password, birthday, role, dialog);
        });
        dialog.show();
    }

    private void createUser(String name, String email, String password, String birthday, String role, AlertDialog dialog) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser newUser = authResult.getUser();
                    if (newUser != null) {
                        String uid = newUser.getUid();
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("name", name);
                        userMap.put("email", email);
                        userMap.put("birthday", birthday);
                        userMap.put("role", role);
                        db.collection("users").document(uid)
                                .set(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "User added successfully!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    loadUsers();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Auth error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showUpdateUserDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null);
        builder.setView(view);
        EditText etName = view.findViewById(R.id.etName);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPassword = view.findViewById(R.id.etPassword);
        EditText etBirthday = view.findViewById(R.id.etBirthday);
        Spinner spinnerRole = view.findViewById(R.id.spinnerRole);
        Button btnSave = view.findViewById(R.id.btnSaveUser);
        etPassword.setVisibility(View.GONE);
        etName.setText(user.getName());
        etEmail.setText(user.getEmail());
        etBirthday.setText(user.getBirthday());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"user", "admin"});
        spinnerRole.setAdapter(adapter);
        spinnerRole.setSelection(user.getRole() != null && user.getRole().equals("admin") ? 1 : 0);
        etBirthday.setInputType(InputType.TYPE_NULL);
        etBirthday.setOnClickListener(v -> showDatePicker(etBirthday));
        AlertDialog dialog = builder.create();
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String birthday = etBirthday.getText().toString().trim();
            String role = spinnerRole.getSelectedItem().toString();
            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill required fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> updatedMap = new HashMap<>();
            updatedMap.put("name", name);
            updatedMap.put("email", email);
            updatedMap.put("birthday", birthday);
            updatedMap.put("role", role);
            db.collection("users").document(user.getUid())
                    .update(updatedMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "User updated!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadUsers();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_LONG).show());
        });
        dialog.show();
    }

    private void showDatePicker(EditText et) {
        final Calendar calendar = Calendar.getInstance();
        int defaultYear = calendar.get(Calendar.YEAR) - 18;
        int defaultMonth = calendar.get(Calendar.MONTH);
        int defaultDay = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dpd = new DatePickerDialog(this,
                (view1, year, month, dayOfMonth) -> {
                    String formatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    et.setText(formatted);
                }, defaultYear, defaultMonth, defaultDay);
        dpd.show();
    }
}