package com.fitfood.app;

import android.app.ProgressDialog;
import android.content.Context;
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
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public class AdminProfileActivity extends AppCompatActivity {
    private EditText etName, etEmail;
    private TextView tvAdminNameDisplay, tvAdminEmailDisplay;
    private ImageView imgAvatar;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admin Profile");
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        imgAvatar = findViewById(R.id.imgAvatar);
        tvAdminNameDisplay = findViewById(R.id.tvAdminNameDisplay);
        tvAdminEmailDisplay = findViewById(R.id.tvAdminEmailDisplay);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        FloatingActionButton fabSave = findViewById(R.id.fabSave);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) uploadImageToFirebase(uri);
                }
        );
        imgAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        fabSave.setOnClickListener(v -> saveProfile());
        loadAdminProfile();
    }

    private void loadAdminProfile() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String photoUrl = doc.getString("profileImageUrl");
                tvAdminNameDisplay.setText(name != null ? name : "Admin");
                tvAdminEmailDisplay.setText(email);
                etName.setText(name);
                etEmail.setText(email);
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this).load(photoUrl).circleCrop().placeholder(R.drawable.use2).into(imgAvatar);
                }
            }
        });
    }

    private void saveProfile() {
        hideKeyboard();
        String newName = etName.getText().toString().trim();
        if (newName.isEmpty()) {
            etName.setError("Name required");
            return;
        }
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("name", newName);
        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    tvAdminNameDisplay.setText(newName);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show());
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Updating Avatar...");
        pd.show();
        StorageReference ref = storage.getReference().child("profile_images/" + uid + ".jpg");
        ref.putFile(imageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    db.collection("users").document(uid).update("profileImageUrl", uri.toString());
                    Glide.with(this).load(uri).circleCrop().into(imgAvatar);
                    pd.dismiss();
                    Toast.makeText(this, "Avatar Updated", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
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