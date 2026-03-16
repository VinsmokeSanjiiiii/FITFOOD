package com.fitfood.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

public class ComboMealView {
    private final Context context;
    private final View rootView;
    private final LinearLayout itemsContainer;
    private final TextView tvComboName;
    private final TextView tvComboSubtitle;
    private final TextView tvTotalCalories;
    private final TextView tvTotalGrams;
    private final ImageView imgComboPrimary;
    private final Button btnAddToLog;
    private final Button btnViewDetails;
    private final ImageButton btnRefresh;
    private final ImageButton btnLike;
    private ComboMeal currentCombo;
    private OnComboActionListener listener;

    public interface OnComboActionListener {
        void onAddComboToLog(ComboMeal combo);
        void onViewComboDetails(ComboMeal combo);
        void onRefreshCombo();
        void onLikeCombo(ComboMeal combo);
    }

    public ComboMealView(Context context, View parent) {
        this.context = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        this.rootView = inflater.inflate(R.layout.view_combo_meal, (android.view.ViewGroup) parent, false);

        itemsContainer = rootView.findViewById(R.id.containerComboItems);
        tvComboName = rootView.findViewById(R.id.tvComboName);
        tvComboSubtitle = rootView.findViewById(R.id.tvComboSubtitle);
        tvTotalCalories = rootView.findViewById(R.id.tvTotalCalories);
        tvTotalGrams = rootView.findViewById(R.id.tvTotalGrams);
        imgComboPrimary = rootView.findViewById(R.id.imgComboPrimary);
        btnAddToLog = rootView.findViewById(R.id.btnAddComboToLog);
        btnViewDetails = rootView.findViewById(R.id.btnViewComboDetails);
        btnRefresh = rootView.findViewById(R.id.btnRefreshCombo);
        btnLike = rootView.findViewById(R.id.btnLikeCombo);

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnAddToLog.setOnClickListener(v -> {
            if (listener != null && currentCombo != null) {
                listener.onAddComboToLog(currentCombo);
            }
        });

        btnViewDetails.setOnClickListener(v -> {
            if (listener != null && currentCombo != null) {
                listener.onViewComboDetails(currentCombo);
            }
        });

        btnRefresh.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRefreshCombo();
            }
        });

        btnLike.setOnClickListener(v -> {
            if (listener != null && currentCombo != null) {
                listener.onLikeCombo(currentCombo);
                btnLike.setImageResource(R.drawable.ic_heart_filled);
            }
        });
    }

    public View getView() {
        return rootView;
    }

    public void setOnComboActionListener(OnComboActionListener listener) {
        this.listener = listener;
    }

    public void setComboMeal(ComboMeal combo) {
        this.currentCombo = combo;

        if (combo == null || !combo.isValid()) {
            showEmptyState();
            return;
        }

        rootView.setVisibility(View.VISIBLE);
        tvComboName.setText(combo.getDisplayName());
        tvComboSubtitle.setText(String.format(Locale.getDefault(),
                "%s • %d items", combo.getMealType(), combo.getItems().size()));

        tvTotalCalories.setText(String.format(Locale.getDefault(), "%.0f", combo.getTotalCalories()));
        tvTotalGrams.setText(String.format(Locale.getDefault(), "%.0fg", combo.getTotalGrams()));

        loadPrimaryImage(combo.getPrimaryImage());
        populateItemsContainer(combo.getItems());

        btnAddToLog.setEnabled(true);
        btnViewDetails.setEnabled(true);
    }

    private void populateItemsContainer(List<ComboMeal.ComboItem> items) {
        itemsContainer.removeAllViews();

        for (int i = 0; i < items.size(); i++) {
            ComboMeal.ComboItem item = items.get(i);
            View itemView = createItemView(item, i + 1);
            itemsContainer.addView(itemView);
        }
    }

    private View createItemView(ComboMeal.ComboItem item, int number) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_combo_food_mini, itemsContainer, false);

        TextView tvNumber = view.findViewById(R.id.tvItemNumber);
        TextView tvName = view.findViewById(R.id.tvFoodName);
        TextView tvPortion = view.findViewById(R.id.tvPortion);
        TextView tvCalories = view.findViewById(R.id.tvCalories);
        ImageView imgFood = view.findViewById(R.id.imgFood);

        tvNumber.setText(String.valueOf(number));
        tvName.setText(item.getFoodItem().getName());
        tvPortion.setText(String.format(Locale.getDefault(), "%.0fg", item.getPortionGrams()));
        tvCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", item.getCalculatedCalories()));

        loadFoodImage(item.getFoodItem().getImage(), imgFood);
        return view;
    }

    private void loadPrimaryImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            imgComboPrimary.setImageResource(R.drawable.ic_combo_placeholder);
            return;
        }

        if (imagePath.startsWith("http")) {
            Glide.with(context)
                    .load(imagePath)
                    .placeholder(R.drawable.ic_combo_placeholder)
                    .centerCrop()
                    .into(imgComboPrimary);
        } else if (imagePath.length() > 200) {
            // Base64 image
        } else {
            int resId = context.getResources().getIdentifier(imagePath, "drawable",
                    context.getPackageName());
            if (resId != 0) {
                imgComboPrimary.setImageResource(resId);
            } else {
                imgComboPrimary.setImageResource(R.drawable.ic_combo_placeholder);
            }
        }
    }

    private void loadFoodImage(String imagePath, ImageView imageView) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_food_placeholder);
            return;
        }

        if (imagePath.startsWith("http")) {
            Glide.with(context)
                    .load(imagePath)
                    .placeholder(R.drawable.ic_food_placeholder)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_food_placeholder);
        }
    }

    private void showEmptyState() {
        rootView.setVisibility(View.GONE);
    }

    public void showLoading() {
        btnRefresh.setEnabled(false);
    }

    public void hideLoading() {
        btnRefresh.setEnabled(true);
    }
}