package com.fitfood.app;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.io.ByteArrayOutputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.appcompat.app.AlertDialog;

public class AccountProfileActivity extends AppCompatActivity {
    private EditText etName, etEmail, etWeight, etHeight;
    private TextView tvDailyCalories, tvUserNameDisplay, tvUserEmailDisplay, tvNotificationCount;
    private androidx.cardview.widget.CardView cardLiveStatus;
    private TextView tvLiveBMI, tvLiveStatus, tvLiveAdvice;
    private ImageView imgAvatar;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private View viewUnreadDot;
    private final List<String> notificationList = new ArrayList<>();
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri tempImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        imgAvatar = findViewById(R.id.imgAvatar);
        tvUserNameDisplay = findViewById(R.id.tvUserNameDisplay);
        tvUserEmailDisplay = findViewById(R.id.tvUserEmailDisplay);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        tvDailyCalories = findViewById(R.id.tvDailyCalories);
        tvNotificationCount = findViewById(R.id.tvNotificationCount);
        viewUnreadDot = findViewById(R.id.viewUnreadDot);
        cardLiveStatus = findViewById(R.id.cardLiveStatus);
        tvLiveBMI = findViewById(R.id.tvLiveBMI);
        tvLiveStatus = findViewById(R.id.tvLiveStatus);
        tvLiveAdvice = findViewById(R.id.tvLiveAdvice);
        setupLiveAnalysis();
        CardView cardNotifications = findViewById(R.id.cardNotifications);
        FloatingActionButton fabSave = findViewById(R.id.fabSave);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        loadUserProfile();
        listenForNotifications();
        fabSave.setOnClickListener(v -> saveUserProfile());
        cardNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(AccountProfileActivity.this, ChatActivity.class);
            startActivity(intent);
        });
        listenForUnreadMessages();
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        tempImageUri = uri;
                        showUploadConfirmationDialog();
                    }
                }
        );
        imgAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void setupLiveAnalysis() {
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateLiveBMI(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        etWeight.addTextChangedListener(watcher);
        etHeight.addTextChangedListener(watcher);
    }

    @SuppressLint("SetTextI18n")
    private void calculateLiveBMI() {
        String wStr = etWeight.getText().toString();
        String hStr = etHeight.getText().toString();
        if (wStr.isEmpty() || hStr.isEmpty()) {
            cardLiveStatus.setVisibility(View.GONE);
            return;
        }
        try {
            double weight = Double.parseDouble(wStr);
            double height = Double.parseDouble(hStr);
            if (weight <= 0 || height <= 0) return;
            if (height > 3.0) {
                height = height / 100.0;
            }
            double bmi = weight / (height * height);
            cardLiveStatus.setVisibility(View.VISIBLE);
            tvLiveBMI.setText(String.format(Locale.getDefault(), "BMI: %.1f", bmi));
            updateStatusCardColor(bmi);
        } catch (NumberFormatException e) {
            cardLiveStatus.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateStatusCardColor(double bmi) {
        if (bmi < 18.5) {
            tvLiveStatus.setText("Underweight");
            tvLiveStatus.setTextColor(android.graphics.Color.parseColor("#FFA726"));
            tvLiveAdvice.setText("Consider a surplus calorie diet to gain mass.");
            cardLiveStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"));
        } else if (bmi < 25) {
            tvLiveStatus.setText("Normal Weight");
            tvLiveStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            tvLiveAdvice.setText("Great job! Maintain your balanced diet.");
            cardLiveStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"));
        } else if (bmi < 30) {
            tvLiveStatus.setText("Overweight");
            tvLiveStatus.setTextColor(android.graphics.Color.parseColor("#FF7043"));
            tvLiveAdvice.setText("Try reducing daily calories and adding cardio.");
            cardLiveStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FBE9E7"));
        } else {
            tvLiveStatus.setText("Obese");
            tvLiveStatus.setTextColor(android.graphics.Color.parseColor("#EF5350"));
            tvLiveAdvice.setText("Consult a specialist for a health plan.");
            cardLiveStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"));
        }
    }

    private void showUploadConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Profile Picture")
                .setMessage("Are you sure you want to use this image as your new profile picture?")
                .setPositiveButton("Upload", (dialog, which) -> {
                    if (tempImageUri != null) {
                        uploadImageToFirebase(tempImageUri);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    tempImageUri = null;
                    Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void listenForUnreadMessages() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        db.collection("chats").document(userId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || snapshots.isEmpty()) {
                        tvNotificationCount.setText("No messages");
                        viewUnreadDot.setVisibility(View.GONE);
                        return;
                    }
                    Map<String, Object> lastMsgData = snapshots.getDocuments().get(0).getData();
                    if (lastMsgData != null) {
                        String message = (String) lastMsgData.get("message");
                        Boolean isAdmin = (Boolean) lastMsgData.get("sentByAdmin");
                        if (message != null && isAdmin != null) {
                            tvNotificationCount.setText(isAdmin ? "Admin: " + message : "You: " + message);
                            if (isAdmin) {
                                viewUnreadDot.setVisibility(View.VISIBLE);
                                tvNotificationCount.setTextColor(android.graphics.Color.RED);
                            } else {
                                viewUnreadDot.setVisibility(View.GONE);
                                tvNotificationCount.setTextColor(android.graphics.Color.GRAY);
                            }
                        }
                    }
                });
    }

    private void loadUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isDestroyed() || isFinishing()) return;
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        Double weight = doc.getDouble("weight");
                        Double height = doc.getDouble("height");
                        Double dailyCalories = doc.getDouble("dailyCalories");
                        String profileImageBase64 = doc.getString("profileImageBase64");
                        tvUserNameDisplay.setText(name != null ? name : "User");
                        tvUserEmailDisplay.setText(email != null ? email : "");
                        etName.setText(name);
                        etEmail.setText(email);
                        if (weight != null) etWeight.setText(String.valueOf(weight));
                        if (height != null) etHeight.setText(String.valueOf(height));
                        if (dailyCalories != null) {
                            tvDailyCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", dailyCalories));
                        }
                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            displayImageFromBase64(profileImageBase64);
                        } else {
                            imgAvatar.setImageResource(R.drawable.use2);
                        }
                    }
                });
    }

    private void saveUserProfile() {
        hideKeyboard();
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        double weight = 0, height = 0;
        try {
            if (!etWeight.getText().toString().isEmpty())
                weight = Double.parseDouble(etWeight.getText().toString());
            if (!etHeight.getText().toString().isEmpty())
                height = Double.parseDouble(etHeight.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = etName.getText().toString().trim();
        Map<String, Object> userData = new HashMap<>();
        userData.put("weight", weight);
        userData.put("height", height);
        userData.put("name", name);
        final double finalWeight = weight;
        final double finalHeight = height;
        db.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    tvUserNameDisplay.setText(name);
                    if (finalWeight > 0) {
                        syncWeightToHistory(userId, finalWeight, finalHeight);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show());
    }

    private void syncWeightToHistory(String userId, double weight, double height) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("weight", weight);
        progressData.put("date", todayDate);
        if (height > 0) {
            double bmi = weight / (height * height);
            progressData.put("bmi", bmi);
        }
        db.collection("users").document(userId)
                .collection("progress").document(todayDate)
                .set(progressData, SetOptions.merge());
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (auth.getCurrentUser() == null) return;
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing and compressing image...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        tempImageUri = null;
        new Thread(() -> {
            String base64Image = compressAndConvertUriToBase64(imageUri);
            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (base64Image != null) {
                    updateProfileImageInFirestore(base64Image);
                } else {
                    Toast.makeText(this, "Failed to process image.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void updateProfileImageInFirestore(String base64Image) {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("profileImageBase64", base64Image);
        data.put("profileImageUrl", null);
        db.collection("users").document(userId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (isDestroyed()) return;
                    Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                    displayImageFromBase64(base64Image);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile image data.", Toast.LENGTH_SHORT).show();
                });
    }

    private String compressAndConvertUriToBase64(Uri imageUri) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            if (bitmap == null) return null;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output);
            byte[] bytes = output.toByteArray();
            bitmap.recycle();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void displayImageFromBase64(String base64Image) {
        try {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            Glide.with(this)
                    .load(decodedBitmap)
                    .circleCrop()
                    .placeholder(R.drawable.use2)
                    .into(imgAvatar);
        } catch (Exception e) {
            e.printStackTrace();
            imgAvatar.setImageResource(R.drawable.use2);
        }
    }

    @SuppressLint("SetTextI18n")
    private void listenForNotifications() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    notificationList.clear();
                    if (snapshots != null && !snapshots.isEmpty()) {
                        tvNotificationCount.setText(snapshots.size() + " messages available");
                        tvNotificationCount.setTextColor(android.graphics.Color.parseColor("#FF5722"));
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String title = doc.getString("title");
                            String msg = doc.getString("message");
                            if (title != null) {
                                notificationList.add("📢 " + title + "\n" + msg);
                            }
                        }
                    } else {
                        tvNotificationCount.setText("No new messages");
                        tvNotificationCount.setTextColor(android.graphics.Color.GRAY);
                    }
                });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}