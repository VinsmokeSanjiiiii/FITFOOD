package com.fitfood.app;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
    private final TextView tvNutritionalScore;
    private final ProgressBar progressNutritionalScore;
    private final ImageView imgComboPrimary;
    private final Button btnAddToLog;
    private final Button btnViewDetails;
    private final ImageButton btnRefresh;
    private final ImageButton btnLike;
    private final ImageButton btnDislike;
    private final ChipGroup chipGroupCategories;
    private final LinearLayout layoutMacroBreakdown;
    private final TextView tvProteinPercent;
    private final TextView tvCarbsPercent;
    private final TextView tvFatPercent;
    private final ProgressBar progressProtein;
    private final ProgressBar progressCarbs;
    private final ProgressBar progressFat;
    private ComboMeal currentCombo;
    private OnComboActionListener listener;
    private boolean isExpanded = false;
    private boolean hasDislikeFeature = false;
    public interface OnComboActionListener {
        void onAddComboToLog(ComboMeal combo);
        void onViewComboDetails(ComboMeal combo);
        void onRefreshCombo();
        void onLikeCombo(ComboMeal combo);

        default void onDislikeCombo(ComboMeal combo, String reason) {
        }

        default void onModifyCombo(ComboMeal combo) {
            // Default: do nothing
        }
    }

    public ComboMealView(Context context, View parent) {
        this.context = context;
        LayoutInflater inflater = LayoutInflater.from(context);

        int layoutId = context.getResources().getIdentifier(
                "view_combo_meal_enhanced", "layout", context.getPackageName());
        if (layoutId == 0) {
            layoutId = context.getResources().getIdentifier(
                    "view_combo_meal", "layout", context.getPackageName());
            hasDislikeFeature = false;
        } else {
            hasDislikeFeature = true;
        }

        this.rootView = inflater.inflate(layoutId, (android.view.ViewGroup) parent, false);

        itemsContainer = findViewOrNull(R.id.containerComboItems);
        tvComboName = findViewOrNull(R.id.tvComboName);
        tvComboSubtitle = findViewOrNull(R.id.tvComboSubtitle);
        tvTotalCalories = findViewOrNull(R.id.tvTotalCalories);
        tvTotalGrams = findViewOrNull(R.id.tvTotalGrams);
        imgComboPrimary = findViewOrNull(R.id.imgComboPrimary);
        btnAddToLog = findViewOrNull(R.id.btnAddComboToLog);
        btnViewDetails = findViewOrNull(R.id.btnViewComboDetails);
        btnRefresh = findViewOrNull(R.id.btnRefreshCombo);

        tvNutritionalScore = findViewOrNull(R.id.tvNutritionalScore);
        progressNutritionalScore = findViewOrNull(R.id.progressNutritionalScore);
        btnLike = findViewOrNull(R.id.btnLikeCombo);
        btnDislike = findViewOrNull(R.id.btnDislikeCombo);
        chipGroupCategories = findViewOrNull(R.id.chipGroupCategories);
        layoutMacroBreakdown = findViewOrNull(R.id.layoutMacroBreakdown);
        tvProteinPercent = findViewOrNull(R.id.tvProteinPercent);
        tvCarbsPercent = findViewOrNull(R.id.tvCarbsPercent);
        tvFatPercent = findViewOrNull(R.id.tvFatPercent);
        progressProtein = findViewOrNull(R.id.progressProtein);
        progressCarbs = findViewOrNull(R.id.progressCarbs);
        progressFat = findViewOrNull(R.id.progressFat);

        setupClickListeners();
    }

    private <T extends View> T findViewOrNull(int id) {
        try {
            return rootView.findViewById(id);
        } catch (Exception e) {
            return null;
        }
    }

    private void setupClickListeners() {
        if (btnAddToLog != null) {
            btnAddToLog.setOnClickListener(v -> {
                if (listener != null && currentCombo != null) {
                    animateButtonClick(btnAddToLog);
                    listener.onAddComboToLog(currentCombo);
                }
            });
        }

        if (btnViewDetails != null) {
            btnViewDetails.setOnClickListener(v -> {
                toggleExpandedView();
                if (listener != null && currentCombo != null) {
                    listener.onViewComboDetails(currentCombo);
                }
            });
        }

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                animateRotation(btnRefresh);
                if (listener != null) {
                    listener.onRefreshCombo();
                }
            });
        }

        if (btnLike != null) {
            btnLike.setOnClickListener(v -> {
                if (listener != null && currentCombo != null) {
                    animateButtonClick(btnLike);
                    btnLike.setImageResource(R.drawable.ic_heart_filled);
                    if (btnDislike != null) {
                        btnDislike.setImageResource(R.drawable.ic_thumb_down_outline);
                    }
                    listener.onLikeCombo(currentCombo);
                }
            });
        }

        if (btnDislike != null) {
            btnDislike.setOnClickListener(v -> {
                showDislikeReasonDialog();
            });
        }
    }

    private void toggleExpandedView() {
        if (layoutMacroBreakdown == null) return;

        isExpanded = !isExpanded;

        if (isExpanded) {
            layoutMacroBreakdown.setVisibility(View.VISIBLE);
            layoutMacroBreakdown.setAlpha(0f);
            layoutMacroBreakdown.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        } else {
            layoutMacroBreakdown.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> layoutMacroBreakdown.setVisibility(View.GONE))
                    .start();
        }
    }

    private void showDislikeReasonDialog() {
        if (!hasDislikeFeature) return;

        String[] reasons = {"Too big", "Too small", "Don't like these foods", "Not balanced", "Other"};
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Help us improve")
                .setItems(reasons, (dialog, which) -> {
                    if (listener != null && currentCombo != null) {
                        btnDislike.setImageResource(R.drawable.ic_thumb_down_filled);
                        if (btnLike != null) {
                            btnLike.setImageResource(R.drawable.ic_heart_outline);
                        }
                        listener.onDislikeCombo(currentCombo, reasons[which]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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

        if (tvComboName != null) {
            tvComboName.setText(combo.getDisplayName());
        }

        String subtitle;
        if (hasDislikeFeature && combo.getNutritionalScore() > 0) {
            subtitle = String.format(Locale.getDefault(),
                    "%s • %d items • Score: %.0f/100",
                    combo.getMealType(),
                    combo.getItems().size(),
                    combo.getNutritionalScore());
        } else {
            subtitle = String.format(Locale.getDefault(),
                    "%s • %d items",
                    combo.getMealType(),
                    combo.getItems().size());
        }

        if (tvComboSubtitle != null) {
            tvComboSubtitle.setText(subtitle);
        }

        if (tvTotalCalories != null) {
            tvTotalCalories.setText(String.format(Locale.getDefault(), "%.0f", combo.getTotalCalories()));
        }

        if (tvTotalGrams != null) {
            tvTotalGrams.setText(String.format(Locale.getDefault(), "%.0fg", combo.getTotalGrams()));
        }

        if (hasDislikeFeature) {
            populateEnhancedFeatures(combo);
        }

        loadPrimaryImage(combo.getPrimaryImage());

        if (itemsContainer != null) {
            populateItemsContainer(combo.getItems());
        }

        if (btnAddToLog != null) btnAddToLog.setEnabled(true);
        if (btnViewDetails != null) btnViewDetails.setEnabled(true);

        if (btnLike != null) {
            btnLike.setImageResource(R.drawable.ic_heart_outline);
        }
        if (btnDislike != null) {
            btnDislike.setImageResource(R.drawable.ic_thumb_down_outline);
        }
    }

    private void populateEnhancedFeatures(ComboMeal combo) {
        if (progressNutritionalScore != null && tvNutritionalScore != null) {
            progressNutritionalScore.setProgress((int) combo.getNutritionalScore());
            tvNutritionalScore.setText(String.format(Locale.getDefault(), "%.0f", combo.getNutritionalScore()));

            int scoreColor;
            if (combo.getNutritionalScore() >= 80) {
                scoreColor = context.getResources().getColor(android.R.color.holo_green_dark);
            } else if (combo.getNutritionalScore() >= 60) {
                scoreColor = context.getResources().getColor(android.R.color.holo_orange_dark);
            } else {
                scoreColor = context.getResources().getColor(android.R.color.holo_red_dark);
            }
            progressNutritionalScore.setProgressTintList(android.content.res.ColorStateList.valueOf(scoreColor));
        }

        if (chipGroupCategories != null) {
            populateCategoryChips(combo.getCategoryCoverage());
        }

        if (layoutMacroBreakdown != null) {
            populateMacroBreakdown(combo);
        }
    }

    private void populateMacroBreakdown(ComboMeal combo) {
        if (tvProteinPercent == null || tvCarbsPercent == null || tvFatPercent == null) return;

        double totalCals = combo.getTotalCalories();
        if (totalCals <= 0) return;

        double proteinCals = combo.getTotalProtein() * 4;
        double carbsCals = combo.getTotalCarbs() * 4;
        double fatCals = combo.getTotalFat() * 9;

        int proteinPct = (int) ((proteinCals / totalCals) * 100);
        int carbsPct = (int) ((carbsCals / totalCals) * 100);
        int fatPct = (int) ((fatCals / totalCals) * 100);

        tvProteinPercent.setText(String.format(Locale.getDefault(), "Protein %d%%", proteinPct));
        tvCarbsPercent.setText(String.format(Locale.getDefault(), "Carbs %d%%", carbsPct));
        tvFatPercent.setText(String.format(Locale.getDefault(), "Fat %d%%", fatPct));

        if (progressProtein != null) progressProtein.setProgress(proteinPct);
        if (progressCarbs != null) progressCarbs.setProgress(carbsPct);
        if (progressFat != null) progressFat.setProgress(fatPct);
    }

    private void populateCategoryChips(String[] categories) {
        if (chipGroupCategories == null || categories == null) return;

        chipGroupCategories.removeAllViews();

        for (String category : categories) {
            Chip chip = new Chip(context);
            chip.setText(category);
            int bgColor = context.getResources().getIdentifier("chip_background", "color", context.getPackageName());
            if (bgColor != 0) {
                chip.setChipBackgroundColorResource(bgColor);
            } else {
                chip.setChipBackgroundColorResource(android.R.color.darker_gray);
            }
            chip.setTextColor(context.getResources().getColor(android.R.color.white));

            int heightRes = context.getResources().getIdentifier("chip_height", "dimen", context.getPackageName());
            if (heightRes != 0) {
                chip.setChipMinHeightResource(heightRes);
            }

            chipGroupCategories.addView(chip);
        }
    }

    private void populateItemsContainer(List<ComboMeal.ComboItem> items) {
        if (itemsContainer == null) return;

        itemsContainer.removeAllViews();

        for (int i = 0; i < items.size(); i++) {
            ComboMeal.ComboItem item = items.get(i);
            View itemView = createItemView(item, i + 1);
            itemsContainer.addView(itemView);
            if (hasDislikeFeature) {
                itemView.setAlpha(0f);
                itemView.setTranslationY(50f);
                itemView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setStartDelay(i * 100L)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }
    }

    private View createItemView(ComboMeal.ComboItem item, int number) {
        LayoutInflater inflater = LayoutInflater.from(context);
        int layoutId = context.getResources().getIdentifier(
                "item_combo_food_enhanced", "layout", context.getPackageName());
        if (layoutId == 0) {
            layoutId = context.getResources().getIdentifier(
                    "item_combo_food_mini", "layout", context.getPackageName());
        }

        View view = inflater.inflate(layoutId, itemsContainer, false);

        TextView tvNumber = view.findViewById(R.id.tvItemNumber);
        TextView tvName = view.findViewById(R.id.tvFoodName);
        TextView tvPortion = view.findViewById(R.id.tvPortion);
        TextView tvCalories = view.findViewById(R.id.tvCalories);
        ImageView imgFood = view.findViewById(R.id.imgFood);

        if (tvNumber != null) tvNumber.setText(String.valueOf(number));
        if (tvName != null) tvName.setText(item.getFoodItem().getName());
        if (tvPortion != null) {
            tvPortion.setText(String.format(Locale.getDefault(), "%.0fg", item.getPortionGrams()));
        }
        if (tvCalories != null) {
            tvCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", item.getCalculatedCalories()));
        }

        TextView tvMacros = view.findViewById(R.id.tvMacros);
        ProgressBar itemProgress = view.findViewById(R.id.itemProgress);

        if (tvMacros != null) {
            tvMacros.setText(String.format(Locale.getDefault(),
                    "P:%.1fg • C:%.1fg • F:%.1fg",
                    item.getCalculatedProtein(),
                    item.getCalculatedCarbs(),
                    item.getCalculatedFat()));
        }

        if (itemProgress != null && currentCombo != null) {
            double maxItemCal = 0;
            for (ComboMeal.ComboItem ci : currentCombo.getItems()) {
                maxItemCal = Math.max(maxItemCal, ci.getCalculatedCalories());
            }
            int progress = maxItemCal > 0 ? (int) ((item.getCalculatedCalories() / maxItemCal) * 100) : 0;
            itemProgress.setProgress(progress);
        }

        loadFoodImage(item.getFoodItem().getImage(), imgFood);
        return view;
    }

    private void loadPrimaryImage(String imagePath) {
        if (imgComboPrimary == null) return;

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
            imgComboPrimary.setImageResource(R.drawable.ic_combo_placeholder);
        } else {
            int resId = context.getResources().getIdentifier(imagePath, "drawable", context.getPackageName());
            if (resId != 0) {
                imgComboPrimary.setImageResource(resId);
            } else {
                imgComboPrimary.setImageResource(R.drawable.ic_combo_placeholder);
            }
        }
    }

    private void loadFoodImage(String imagePath, ImageView imageView) {
        if (imageView == null) return;

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
        if (btnRefresh != null) {
            btnRefresh.setEnabled(false);
            btnRefresh.setAlpha(0.5f);
        }
    }

    public void hideLoading() {
        if (btnRefresh != null) {
            btnRefresh.setEnabled(true);
            btnRefresh.setAlpha(1.0f);
        }
    }

    private void animateButtonClick(View view) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }

    private void animateRotation(ImageButton button) {
        ObjectAnimator rotation = ObjectAnimator.ofFloat(button, "rotation", 0f, 360f);
        rotation.setDuration(500);
        rotation.setInterpolator(new DecelerateInterpolator());
        rotation.start();
    }
}