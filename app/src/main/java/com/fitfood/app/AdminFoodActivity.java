package com.fitfood.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminFoodActivity extends AppCompatActivity implements AdminFoodAdapter.OnFoodActionListener {

    private AdminFoodAdapter adapter;
    private List<FoodItem> foodList;
    private List<FoodItem> foodListFull;
    private FirebaseFirestore db;
    private TextView tvEmpty;
    private EditText pendingUrlInput;
    private Uri tempImageUri;

    // Executor for background tasks to prevent ANR
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    tempImageUri = result.getData().getData();
                    showUploadConfirmationDialog();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_food);

        db = FirebaseFirestore.getInstance();
        foodList = new ArrayList<>();
        foodListFull = new ArrayList<>();
        tvEmpty = findViewById(R.id.tvEmptyFood);

        RecyclerView recyclerView = findViewById(R.id.rvAdminFoods);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminFoodAdapter(this, foodList, this);
        recyclerView.setAdapter(adapter);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        FloatingActionButton fabAdd = findViewById(R.id.fabAddFood);
        fabAdd.setOnClickListener(v -> showFoodDialog(null));

        loadFoodsRealtime();

        EditText etSearch = findViewById(R.id.etSearchFood);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFoods(s.toString());
            }
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void filterFoods(String query) {
        foodList.clear();
        if (query == null || query.trim().isEmpty()) {
            foodList.addAll(foodListFull);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (FoodItem item : foodListFull) {
                String name = item.getName();
                String category = item.getCategory();
                boolean nameMatches = name != null && name.toLowerCase().contains(lowerQuery);
                boolean categoryMatches = category != null && category.toLowerCase().contains(lowerQuery);
                if (nameMatches || categoryMatches) {
                    foodList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadFoodsRealtime() {
        db.collection("foods_v2").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Sync Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            foodList.clear();
            foodListFull.clear();

            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    try {
                        FoodItem food = doc.toObject(FoodItem.class);
                        food.setId(doc.getId());
                        foodList.add(food);
                        foodListFull.add(food);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            EditText etSearch = findViewById(R.id.etSearchFood);
            if (etSearch.getText().length() > 0) {
                filterFoods(etSearch.getText().toString());
            } else {
                adapter.notifyDataSetChanged();
            }

            tvEmpty.setVisibility(foodList.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onEdit(FoodItem food) {
        showFoodDialog(food);
    }

    @Override
    public void onDelete(FoodItem food) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + food.getName() + "?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    db.collection("foods_v2").document(food.getId()).delete();
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void showFoodDialog(FoodItem foodToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_manage_food, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.etFoodName);
        EditText etDesc = view.findViewById(R.id.etFoodDesc);
        EditText etKcal = view.findViewById(R.id.etFoodKcal);
        EditText etGrams = view.findViewById(R.id.etFoodGrams);
        EditText etIngredients = view.findViewById(R.id.etFoodIngredients);
        Spinner spCategory = view.findViewById(R.id.spFoodCategory);
        EditText etImage = view.findViewById(R.id.etFoodImage);

        ImageView imgPreview = view.findViewById(R.id.imgPreview);
        View btnSelectImageWrapper = view.findViewById(R.id.btnSelectImageWrapper);
        TextView tvTapToUpload = view.findViewById(R.id.tvTapToUpload);

        CheckBox cbBreak = view.findViewById(R.id.cbBreakfast);
        CheckBox cbLunch = view.findViewById(R.id.cbLunch);
        CheckBox cbDinner = view.findViewById(R.id.cbDinner);
        CheckBox cbSnack = view.findViewById(R.id.cbSnack);
        Button btnSave = view.findViewById(R.id.btnSaveFood);

        pendingUrlInput = etImage;

        if (foodToEdit != null) {
            etName.setText(foodToEdit.getName());
            etDesc.setText(foodToEdit.getDescription());
            etKcal.setText(String.valueOf(foodToEdit.getCalories()));
            etGrams.setText(String.valueOf(foodToEdit.getGrams()));
            etIngredients.setText(foodToEdit.getIngredients());
            etImage.setText(foodToEdit.getImage());

            displayImageForDialog(foodToEdit.getImage(), imgPreview, tvTapToUpload);

            String[] cats = getResources().getStringArray(R.array.food_categories);
            for(int i=0; i<cats.length; i++) {
                if(cats[i].equalsIgnoreCase(foodToEdit.getCategory())) spCategory.setSelection(i);
            }
            if(foodToEdit.getMealType() != null) {
                for(String type : foodToEdit.getMealType()) {
                    if(type.equalsIgnoreCase("Breakfast")) cbBreak.setChecked(true);
                    if(type.equalsIgnoreCase("Lunch")) cbLunch.setChecked(true);
                    if(type.equalsIgnoreCase("Dinner")) cbDinner.setChecked(true);
                    if(type.equalsIgnoreCase("Snacks")) cbSnack.setChecked(true);
                }
            }
            btnSave.setText("Update Food");
        }

        etImage.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                displayImageForDialog(s.toString(), imgPreview, tvTapToUpload);
            }
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnSelectImageWrapper.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        AlertDialog dialog = builder.create();
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String kcalStr = etKcal.getText().toString().trim();
            String gramsStr = etGrams.getText().toString().trim();
            String image = etImage.getText().toString().trim();
            String ingredients = etIngredients.getText().toString().trim();
            String category = spCategory.getSelectedItem().toString();

            if (name.isEmpty() || kcalStr.isEmpty() || gramsStr.isEmpty()) {
                Toast.makeText(this, "Fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> mealTypes = new ArrayList<>();
            if(cbBreak.isChecked()) mealTypes.add("Breakfast");
            if(cbLunch.isChecked()) mealTypes.add("Lunch");
            if(cbDinner.isChecked()) mealTypes.add("Dinner");
            if(cbSnack.isChecked()) mealTypes.add("Snacks");

            if(mealTypes.isEmpty()) {
                Toast.makeText(this, "Select at least one meal type", Toast.LENGTH_SHORT).show();
                return;
            }

            FoodItem newFood = new FoodItem(name, Double.parseDouble(gramsStr), Double.parseDouble(kcalStr),
                    category, image, desc, mealTypes, 0, ingredients);

            if (foodToEdit == null) {
                db.collection("foods_v2").add(newFood)
                        .addOnSuccessListener(doc -> {
                            Toast.makeText(this, "Food Added!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
            } else {
                db.collection("foods_v2").document(foodToEdit.getId()).set(newFood)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Food Updated!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
            }
        });
        dialog.show();
    }

    private void showUploadConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Image Upload")
                .setMessage("Do you want to upload and use this image?")
                .setPositiveButton("Use Image", (dialog, which) -> {
                    // Changed: Calls uploadImageToFirebase instead of processImageToBase64
                    if (tempImageUri != null) uploadImageToFirebase(tempImageUri);
                })
                .setNegativeButton("Cancel", (dialog, which) -> tempImageUri = null)
                .show();
    }

    // UPDATED: Uploads to Firebase Storage in Background to prevent ANR
    private void uploadImageToFirebase(Uri imageUri) {
        if (pendingUrlInput == null || imageUri == null) return;

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Compressing & Uploading...");
        pd.setCancelable(false);
        pd.show();

        // 1. Move heavy compression to Background Thread
        executor.execute(() -> {
            try {
                // Resize and compress
                Bitmap bitmap = decodeSampledBitmapFromUri(imageUri);
                if (bitmap == null) {
                    mainHandler.post(() -> {
                        pd.dismiss();
                        Toast.makeText(AdminFoodActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Compress to JPEG, Quality 70% to save space
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] data = baos.toByteArray();
                bitmap.recycle(); // Free memory

                // 2. Switch back to Main Thread for Firebase SDK calls
                mainHandler.post(() -> {
                    String filename = UUID.randomUUID().toString() + ".jpg";
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                            .child("food_images")
                            .child(filename);

                    UploadTask uploadTask = storageRef.putBytes(data);
                    uploadTask.addOnSuccessListener(taskSnapshot -> {
                        // Get the download URL
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            pd.dismiss();
                            // 3. Put URL into the EditText so it saves with the food item
                            pendingUrlInput.setText(uri.toString());
                            Toast.makeText(AdminFoodActivity.this, "Image Uploaded!", Toast.LENGTH_SHORT).show();
                        });
                    }).addOnFailureListener(e -> {
                        pd.dismiss();
                        Toast.makeText(AdminFoodActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    pd.dismiss();
                    Toast.makeText(AdminFoodActivity.this, "Error processing image", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Helper to load bitmap efficiently (prevents OutOfMemoryError before compression)
    private Bitmap decodeSampledBitmapFromUri(Uri fileUri) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream stream = getContentResolver().openInputStream(fileUri);
            BitmapFactory.decodeStream(stream, null, options);
            if (stream != null) stream.close();

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            InputStream finalStream = getContentResolver().openInputStream(fileUri);
            Bitmap bitmap = BitmapFactory.decodeStream(finalStream, null, options);
            if (finalStream != null) finalStream.close();
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width (approx 800px here).
        if (height > 800 || width > 800) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= 800 && (halfWidth / inSampleSize) >= 800) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void displayImageForDialog(String imagePath, ImageView imageView, TextView placeholderText) {
        if (imagePath == null || imagePath.isEmpty()) {
            setDefaultImage(imageView, placeholderText);
            return;
        }

        if (imagePath.startsWith("http")) {
            // It's a URL (Firebase Storage)
            imageView.setPadding(0, 0, 0, 0);
            imageView.setColorFilter(null);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (placeholderText != null) placeholderText.setVisibility(View.GONE);
            Glide.with(this).load(imagePath).placeholder(android.R.drawable.ic_menu_gallery).into(imageView);
        }
        else if (imagePath.length() > 200) {
            // Legacy Base64 support (in case old foods still have it)
            try {
                String cleanBase64 = imagePath;
                if (cleanBase64.contains(",")) cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);

                byte[] decodedString = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                if (decodedBitmap != null) {
                    imageView.setPadding(0, 0, 0, 0);
                    imageView.setColorFilter(null);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setImageBitmap(decodedBitmap);
                    if (placeholderText != null) placeholderText.setVisibility(View.GONE);
                } else {
                    setDefaultImage(imageView, placeholderText);
                }
            } catch (Exception e) {
                setDefaultImage(imageView, placeholderText);
            }
        } else {
            // Drawable resource name
            int resId = 0;
            try { resId = getResources().getIdentifier(imagePath, "drawable", getPackageName()); } catch (Exception ignored) { }
            if (resId != 0) {
                imageView.setPadding(0, 0, 0, 0);
                imageView.setColorFilter(null);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageResource(resId);
                if (placeholderText != null) placeholderText.setVisibility(View.GONE);
            } else {
                setDefaultImage(imageView, placeholderText);
            }
        }
    }

    private void setDefaultImage(ImageView targetImageView, TextView placeholderText) {
        targetImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        int padding = (int) (60 * getResources().getDisplayMetrics().density);
        targetImageView.setPadding(padding, padding, padding, padding);
        targetImageView.setColorFilter(Color.parseColor("#BDBDBD"));
        targetImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        if (placeholderText != null) placeholderText.setVisibility(View.VISIBLE);
    }
}