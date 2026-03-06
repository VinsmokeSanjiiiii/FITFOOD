package com.fitfood.app;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etBirthday, etEmail, etPassword, etConfirmPassword;
    private ImageView ivShowHidePass;
    private ImageView ivShowHideConfirm;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etBirthday = findViewById(R.id.etBirthday);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvAlreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount);
        ivShowHidePass = findViewById(R.id.ivShowHidePass);
        ivShowHideConfirm = findViewById(R.id.ivShowHideConfirm);

        etBirthday.setInputType(InputType.TYPE_NULL);
        etBirthday.setOnClickListener(v -> showDatePicker());

        ivShowHidePass.setOnClickListener(v -> {
            if (etPassword.getInputType() ==
                    (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivShowHidePass.setImageResource(R.drawable.ic_eye_open);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivShowHidePass.setImageResource(R.drawable.ic_eye_close);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        ivShowHideConfirm.setOnClickListener(v -> {
            if (etConfirmPassword.getInputType() ==
                    (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivShowHideConfirm.setImageResource(R.drawable.ic_eye_open);
            } else {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivShowHideConfirm.setImageResource(R.drawable.ic_eye_close);
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        btnRegister.setOnClickListener(v -> registerUser());

        tvAlreadyHaveAccount.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int defaultYear = currentYear - 25;
        int defaultMonth = calendar.get(Calendar.MONTH);
        int defaultDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    @SuppressLint("DefaultLocale") String formattedDate = String.format("%02d/%02d/%04d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    etBirthday.setText(formattedDate);
                },
                defaultYear, defaultMonth, defaultDay
        );

        Calendar maxDate = Calendar.getInstance();
        maxDate.set(currentYear - 18, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        Calendar minDate = Calendar.getInstance();
        minDate.set(1900, 0, 1);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void registerUser() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String birthday = etBirthday.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || birthday.isEmpty() ||
                email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("name", firstName + " " + lastName);
                            userMap.put("email", email);
                            userMap.put("birthday", birthday);
                            userMap.put("role", "user");
                            userMap.put("isVerified", false);

                            db.collection("users").document(uid)
                                    .set(userMap, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(verifyTask -> {
                                                    if (verifyTask.isSuccessful()) {
                                                        Toast.makeText(this,
                                                                "Verification email sent. Please check your inbox.",
                                                                Toast.LENGTH_LONG).show();
                                                        mAuth.signOut();
                                                        startActivity(new Intent(this, LoginActivity.class));
                                                        finish();
                                                    } else {
                                                        Toast.makeText(this,
                                                                "Failed to send verification email: "
                                                                        + verifyTask.getException().getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this,
                                                "Firestore error: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                        user.delete();
                                    });
                        }
                    } else {
                        Toast.makeText(this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e("RegisterActivity", "Auth failed", task.getException());
                    }
                });
    }
}
