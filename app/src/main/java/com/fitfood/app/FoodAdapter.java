package com.fitfood.app;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import android.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {
    public interface OnMealAddedListener {
        void onMealAdded(String foodName, double kcal, double grams);
    }
    private final List<FoodItem> foodList;
    private final Context context;
    private final OnMealAddedListener listener;
    private final boolean fromMealLog;

    public FoodAdapter(List<FoodItem> foodList, Context context,
                       OnMealAddedListener listener, boolean fromMealLog) {
        this.foodList = foodList;
        this.context = context;
        this.listener = listener;
        this.fromMealLog = fromMealLog;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        final FoodItem food = foodList.get(position);
        holder.tvName.setText(food.getName() != null ? food.getName() : "Unknown Food");
        holder.tvDescription.setText(food.getDescription() != null ? food.getDescription() : "");
        holder.tvGramsCalories.setText(String.format("%.0fg - %.0f kcal", food.getSuggestedGrams(), food.getKcal()));

        holder.ivImage.setPadding(0, 0, 0, 0);
        holder.ivImage.setColorFilter(null);
        holder.ivImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        displayImage(food.getImage(), holder.ivImage);

        holder.btnAdd.setVisibility(fromMealLog ? View.VISIBLE : View.GONE);
        holder.btnAdd.setOnClickListener(v -> {
            if (listener != null && fromMealLog) {
                listener.onMealAdded(food.getName(), food.getKcal(), food.getSuggestedGrams());
            }
        });
        holder.cardView.setOnClickListener(v -> showFoodDialog(food));
    }

    private void displayImage(String imagePath, ImageView imageView) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_launcher_background);
            return;
        }

        if (imagePath.startsWith("http")) {
            Glide.with(context)
                    .load(imagePath)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(imageView);
        }

        else if (imagePath.length() > 200) {
            try {
                String cleanBase64 = imagePath;
                if (cleanBase64.contains(",")) {
                    cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
                }

                byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    imageView.setImageBitmap(decodedByte);
                } else {
                    imageView.setImageResource(R.drawable.ic_launcher_background);
                }
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.ic_launcher_background);
            }
        }

        else {
            int resId = 0;
            try {
                resId = context.getResources().getIdentifier(imagePath, "drawable", context.getPackageName());
            } catch (Exception ignored) {}

            if (resId != 0) {
                imageView.setImageResource(resId);
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_background);
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void showFoodDialog(final FoodItem food) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_food_details);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        ImageView imgFood = dialog.findViewById(R.id.imgFoodDialog);
        TextView tvName = dialog.findViewById(R.id.tvFoodNameDialog);
        TextView tvDesc = dialog.findViewById(R.id.tvFoodDescDialog);
        TextView tvCalories = dialog.findViewById(R.id.tvCaloriesDialog);
        TextView tvIngredients = dialog.findViewById(R.id.tvIngredientsDialog);
        TextView tvIngredientsLabel = dialog.findViewById(R.id.tvIngredientsLabel);
        Button btnAdd = dialog.findViewById(R.id.btnAddDialog);
        ImageView btnClose = dialog.findViewById(R.id.btnCloseDialog);

        tvName.setText(food.getName());
        tvDesc.setText(food.getDescription());
        tvCalories.setText(String.format("%.0fg • %.0f kcal", food.getSuggestedGrams(), food.getKcal()));

        if (food.getIngredients() != null && !food.getIngredients().trim().isEmpty()) {
            tvIngredients.setText(food.getIngredients());
            tvIngredients.setVisibility(View.VISIBLE);
            tvIngredientsLabel.setVisibility(View.VISIBLE);
        } else {
            tvIngredients.setVisibility(View.GONE);
            tvIngredientsLabel.setVisibility(View.GONE);
        }

        imgFood.setVisibility(View.VISIBLE);
        displayImage(food.getImage(), imgFood);

        btnAdd.setVisibility(fromMealLog ? View.VISIBLE : View.GONE);
        btnAdd.setOnClickListener(v -> {
            if (listener != null && fromMealLog) {
                listener.onMealAdded(food.getName(), food.getKcal(), food.getSuggestedGrams());
            }
            dialog.dismiss();
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return foodList == null ? 0 : foodList.size();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvGramsCalories;
        ImageView ivImage;
        CardView cardView;
        Button btnAdd;
        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvDescription = itemView.findViewById(R.id.tvFoodDescription);
            tvGramsCalories = itemView.findViewById(R.id.tvGramsCalories);
            ivImage = itemView.findViewById(R.id.imgFood);
            cardView = (CardView) itemView;
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }
}