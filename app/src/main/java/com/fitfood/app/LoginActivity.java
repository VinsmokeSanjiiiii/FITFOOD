package com.fitfood.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView forgotPasswordTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        emailEditText = findViewById(R.id.etemail);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);
        forgotPasswordTextView = findViewById(R.id.forgot_password);
        TextView noAccountTextView = findViewById(R.id.no_account);
        setupPasswordToggle();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
            fetchRoleAndRedirect(currentUser);
        } else if (currentUser != null && !currentUser.isEmailVerified()) {
            mAuth.signOut();
        }
        loginButton.setOnClickListener(v -> {
            hideKeyboard();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Please enter email");
                emailEditText.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError("Please enter password");
                passwordEditText.requestFocus();
                return;
            }
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                return;
            }
            setLoadingState(true);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                if (user.isEmailVerified()) {
                                    fetchRoleAndRedirect(user);
                                } else {
                                    setLoadingState(false);
                                    Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                }
                            }
                        } else {
                            setLoadingState(false);
                            String error = task.getException() != null ? task.getException().getMessage() : "Login failed";
                            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        });
        forgotPasswordTextView.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Enter email to reset");
                emailEditText.requestFocus();
                return;
            }
            forgotPasswordTextView.setEnabled(false);
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        forgotPasswordTextView.setEnabled(true);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Reset link sent! Check your inbox.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
        noAccountTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void fetchRoleAndRedirect(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        String safeRole = (role != null) ? role.trim() : "user";
                        Log.d("LoginActivity", "User Role Found: " + safeRole);
                        SharedPreferences prefs = getSharedPreferences("FitFoodPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("isLoggedIn", true);
                        editor.putString("userId", user.getUid());
                        editor.putString("userRole", safeRole);
                        editor.apply();
                        if (safeRole.equalsIgnoreCase("admin")) {
                            Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(LoginActivity.this, Gainloss.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        setLoadingState(false);
                        Toast.makeText(LoginActivity.this, "User data not found in database.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Toast.makeText(LoginActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                });
    }

    @SuppressLint("SetTextI18n")
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            loginButton.setText("Logging in...");
            loginButton.setEnabled(false);
            loginButton.setAlpha(0.7f);
        } else {
            loginButton.setText("Login");
            loginButton.setEnabled(true);
            loginButton.setAlpha(1.0f);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordToggle() {
        passwordEditText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (passwordEditText.getCompoundDrawables()[DRAWABLE_END] != null) {
                    int leftEdgeOfDrawable = passwordEditText.getRight() - passwordEditText.getCompoundPaddingRight();
                    if (event.getRawX() >= leftEdgeOfDrawable) {
                        int selection = passwordEditText.getSelectionStart();
                        boolean isVisible = (passwordEditText.getInputType() & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                                == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                        int newInputType;
                        int newIcon;
                        if (isVisible) {
                            newInputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
                            newIcon = R.drawable.ic_eye_close;
                        } else {
                            newInputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                            newIcon = R.drawable.ic_eye_open;
                        }
                        passwordEditText.setInputType(newInputType);
                        passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                                null, null, ContextCompat.getDrawable(this, newIcon), null);
                        passwordEditText.setSelection(selection);
                        return true;
                    }
                }
            }
            return false;
        });
    }
}