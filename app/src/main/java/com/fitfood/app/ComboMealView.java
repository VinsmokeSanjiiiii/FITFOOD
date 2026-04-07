package com.fitfood.app;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
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
    private final boolean hasDislikeFeature;

    public interface OnComboActionListener {
        void onAddComboToLog(ComboMeal combo);
        default void onAddComboToLogWithMealType(ComboMeal combo, String mealType) {
            onAddComboToLog(combo);
        }
        void onViewComboDetails(ComboMeal combo);
        void onRefreshCombo();
        void onLikeCombo(ComboMeal combo);
        default void onDislikeCombo(ComboMeal combo, String reason) {}
        default void onModifyCombo(ComboMeal combo) {}
    }

    public ComboMealView(Context context, View parent) {
        this(context, parent, false);
    }

    public ComboMealView(Context context, View parent, boolean enableDislikeFeature) {
        this.context = context;
        this.hasDislikeFeature = enableDislikeFeature;

        int layoutId = context.getResources().getIdentifier(
                "view_combo_meal_enhanced", "layout", context.getPackageName());
        if (layoutId == 0) {
            layoutId = context.getResources().getIdentifier(
                    "view_combo_meal", "layout", context.getPackageName());
        }
        this.rootView = LayoutInflater.from(context).inflate(layoutId, (android.view.ViewGroup) parent, false);

        itemsContainer = rootView.findViewById(R.id.containerComboItems);
        tvComboName = rootView.findViewById(R.id.tvComboName);
        tvComboSubtitle = rootView.findViewById(R.id.tvComboSubtitle);
        tvTotalCalories = rootView.findViewById(R.id.tvTotalCalories);
        tvTotalGrams = rootView.findViewById(R.id.tvTotalGrams);
        imgComboPrimary = rootView.findViewById(R.id.imgComboPrimary);
        btnAddToLog = rootView.findViewById(R.id.btnAddComboToLog);
        btnViewDetails = rootView.findViewById(R.id.btnViewComboDetails);
        btnRefresh = rootView.findViewById(R.id.btnRefreshCombo);
        tvNutritionalScore = rootView.findViewById(R.id.tvNutritionalScore);
        progressNutritionalScore = rootView.findViewById(R.id.progressNutritionalScore);
        btnLike = rootView.findViewById(R.id.btnLikeCombo);
        btnDislike = rootView.findViewById(R.id.btnDislikeCombo);
        chipGroupCategories = rootView.findViewById(R.id.chipGroupCategories);
        layoutMacroBreakdown = rootView.findViewById(R.id.layoutMacroBreakdown);
        tvProteinPercent = rootView.findViewById(R.id.tvProteinPercent);
        tvCarbsPercent = rootView.findViewById(R.id.tvCarbsPercent);
        tvFatPercent = rootView.findViewById(R.id.tvFatPercent);
        progressProtein = rootView.findViewById(R.id.progressProtein);
        progressCarbs = rootView.findViewById(R.id.progressCarbs);
        progressFat = rootView.findViewById(R.id.progressFat);

        setupClickListeners();
    }

    private void setupClickListeners() {
        if (btnAddToLog != null) {
            btnAddToLog.setOnClickListener(v -> {
                if (listener != null && currentCombo != null) {
                    animateButtonClick(btnAddToLog);
                    showMealTypeChooserDialog();
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
                if (listener != null) listener.onRefreshCombo();
            });
        }
        if (btnLike != null) {
            btnLike.setOnClickListener(v -> {
                if (listener != null && currentCombo != null) {
                    animateButtonClick(btnLike);
                    btnLike.setImageResource(R.drawable.ic_heart_filled);
                    if (btnDislike != null) btnDislike.setImageResource(R.drawable.ic_thumb_down_outline);
                    listener.onLikeCombo(currentCombo);
                }
            });
        }
        if (btnDislike != null && hasDislikeFeature) {
            btnDislike.setOnClickListener(v -> showDislikeReasonDialog());
        }
    }

    private void showMealTypeChooserDialog() {
        final String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snacks"};
        new AlertDialog.Builder(context)
                .setTitle("Add to which meal?")
                .setItems(mealTypes, (dialog, which) -> {
                    if (listener != null && currentCombo != null) {
                        listener.onAddComboToLogWithMealType(currentCombo, mealTypes[which]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleExpandedView() {
        if (layoutMacroBreakdown == null) return;
        isExpanded = !isExpanded;
        if (isExpanded) {
            layoutMacroBreakdown.setVisibility(View.VISIBLE);
            layoutMacroBreakdown.setAlpha(0f);
            layoutMacroBreakdown.animate().alpha(1f).setDuration(300).start();
        } else {
            layoutMacroBreakdown.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> layoutMacroBreakdown.setVisibility(View.GONE)).start();
        }
    }

    private void showDislikeReasonDialog() {
        if (!hasDislikeFeature) return;
        String[] reasons = {"Too big", "Too small", "Don't like these foods", "Not balanced", "Other"};
        new AlertDialog.Builder(context)
                .setTitle("Help us improve")
                .setItems(reasons, (dialog, which) -> {
                    if (listener != null && currentCombo != null) {
                        if (btnDislike != null) btnDislike.setImageResource(R.drawable.ic_thumb_down_filled);
                        if (btnLike != null) btnLike.setImageResource(R.drawable.ic_heart_outline);
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
            rootView.setVisibility(View.GONE);
            return;
        }
        rootView.setVisibility(View.VISIBLE);
        if (tvComboName != null) tvComboName.setText(combo.getDisplayName());
        String subtitle = String.format(Locale.getDefault(), "%s • %d items",
                combo.getMealType(), combo.getItems().size());
        if (hasDislikeFeature && combo.getNutritionalScore() > 0) {
            subtitle = String.format(Locale.getDefault(), "%s • %d items • Score: %.0f/100",
                    combo.getMealType(), combo.getItems().size(), combo.getNutritionalScore());
        }
        if (tvComboSubtitle != null) tvComboSubtitle.setText(subtitle);
        if (tvTotalCalories != null) tvTotalCalories.setText(String.format(Locale.getDefault(), "%.0f", combo.getTotalCalories()));
        if (tvTotalGrams != null) tvTotalGrams.setText(String.format(Locale.getDefault(), "%.0fg", combo.getTotalGrams()));

        if (hasDislikeFeature) populateEnhancedFeatures(combo);
        loadPrimaryImage(combo.getPrimaryImage());
        if (itemsContainer != null) populateItemsContainer(combo.getItems());

        if (btnAddToLog != null) btnAddToLog.setEnabled(true);
        if (btnViewDetails != null) btnViewDetails.setEnabled(true);
        if (btnLike != null) btnLike.setImageResource(R.drawable.ic_heart_outline);
        if (btnDislike != null) btnDislike.setImageResource(R.drawable.ic_thumb_down_outline);
    }

    @SuppressLint("ResourceAsColor")
    private void populateEnhancedFeatures(ComboMeal combo) {
        if (progressNutritionalScore != null && tvNutritionalScore != null) {
            progressNutritionalScore.setProgress((int) combo.getNutritionalScore());
            tvNutritionalScore.setText(String.format(Locale.getDefault(), "%.0f", combo.getNutritionalScore()));
            int scoreColor = combo.getNutritionalScore() >= 80 ? android.R.color.holo_green_dark :
                    (combo.getNutritionalScore() >= 60 ? android.R.color.holo_orange_dark : android.R.color.holo_red_dark);
            progressNutritionalScore.setProgressTintList(android.content.res.ColorStateList.valueOf(scoreColor));
        }
        if (chipGroupCategories != null) populateCategoryChips(combo.getCategoryCoverage());
        if (layoutMacroBreakdown != null) populateMacroBreakdown(combo);
    }

    private void populateMacroBreakdown(ComboMeal combo) {
        if (tvProteinPercent == null) return;
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

    // FIXED: Replaced Chip with TextView to avoid Material theme requirement
    private void populateCategoryChips(String[] categories) {
        if (chipGroupCategories == null || categories == null) return;
        chipGroupCategories.removeAllViews();
        for (String category : categories) {
            TextView chip = new TextView(context);
            chip.setText(category);
            chip.setTextColor(Color.WHITE);
            chip.setPadding(24, 12, 24, 12);
            chip.setBackground(createChipBackground());
            chip.setTextSize(12);
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);
            chipGroupCategories.addView(chip);
        }
    }

    private GradientDrawable createChipBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(48f);
        drawable.setColor(Color.parseColor("#757575"));
        return drawable;
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
                itemView.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay(i * 100L).start();
            }
        }
    }

    private View createItemView(ComboMeal.ComboItem item, int number) {
        LayoutInflater inflater = LayoutInflater.from(context);
        int layoutId = context.getResources().getIdentifier("item_combo_food_enhanced", "layout", context.getPackageName());
        if (layoutId == 0) layoutId = context.getResources().getIdentifier("item_combo_food_mini", "layout", context.getPackageName());
        View view = inflater.inflate(layoutId, itemsContainer, false);
        TextView tvNumber = view.findViewById(R.id.tvItemNumber);
        TextView tvName = view.findViewById(R.id.tvFoodName);
        TextView tvPortion = view.findViewById(R.id.tvPortion);
        TextView tvCalories = view.findViewById(R.id.tvCalories);
        ImageView imgFood = view.findViewById(R.id.imgFood);
        if (tvNumber != null) tvNumber.setText(String.valueOf(number));
        if (tvName != null) tvName.setText(item.getFoodItem().getName());
        if (tvPortion != null) tvPortion.setText(String.format(Locale.getDefault(), "%.0fg", item.getPortionGrams()));
        if (tvCalories != null) tvCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", item.getCalculatedCalories()));
        TextView tvMacros = view.findViewById(R.id.tvMacros);
        ProgressBar itemProgress = view.findViewById(R.id.itemProgress);
        if (tvMacros != null) {
            tvMacros.setText(String.format(Locale.getDefault(), "P:%.1fg • C:%.1fg • F:%.1fg",
                    item.getCalculatedProtein(), item.getCalculatedCarbs(), item.getCalculatedFat()));
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
            Glide.with(context).load(imagePath).placeholder(R.drawable.ic_combo_placeholder).centerCrop().into(imgComboPrimary);
        } else {
            imgComboPrimary.setImageResource(R.drawable.ic_combo_placeholder);
        }
    }

    private void loadFoodImage(String imagePath, ImageView imageView) {
        if (imageView == null) return;
        if (imagePath == null || imagePath.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_food_placeholder);
            return;
        }
        if (imagePath.startsWith("http")) {
            Glide.with(context).load(imagePath).placeholder(R.drawable.ic_food_placeholder).into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_food_placeholder);
        }
    }

    public void showLoading() {
        if (btnRefresh != null) { btnRefresh.setEnabled(false); btnRefresh.setAlpha(0.5f); }
    }

    public void hideLoading() {
        if (btnRefresh != null) { btnRefresh.setEnabled(true); btnRefresh.setAlpha(1.0f); }
    }

    private void animateButtonClick(View view) {
        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private void animateRotation(ImageButton button) {
        ObjectAnimator rotation = ObjectAnimator.ofFloat(button, "rotation", 0f, 360f);
        rotation.setDuration(500);
        rotation.setInterpolator(new DecelerateInterpolator());
        rotation.start();
    }
}