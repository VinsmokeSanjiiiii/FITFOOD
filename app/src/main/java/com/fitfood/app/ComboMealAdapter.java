package com.fitfood.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ComboMealAdapter extends RecyclerView.Adapter<ComboMealAdapter.ViewHolder> {
    public interface OnComboClickListener {
        void onComboClick(ComboMeal combo);
        void onAddComboClick(ComboMeal combo);
    }

    private List<ComboMeal> combos = new ArrayList<>();
    private OnComboClickListener listener;

    public void setCombos(List<ComboMeal> combos) {
        this.combos = combos != null ? combos : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnComboClickListener(OnComboClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_combo_meal_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComboMeal combo = combos.get(position);

        holder.tvName.setText(combo.getDisplayName());
        holder.tvMealType.setText(combo.getMealType());
        holder.tvCalories.setText(String.format(Locale.getDefault(),
                "%.0f kcal", combo.getTotalCalories()));
        holder.tvItemCount.setText(String.format(Locale.getDefault(),
                "%d items", combo.getItems().size()));

        String imageUrl = combo.getPrimaryImage();
        if (imageUrl != null && imageUrl.startsWith("http")) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .into(holder.imgCombo);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onComboClick(combo);
        });

        holder.btnQuickAdd.setOnClickListener(v -> {
            if (listener != null) listener.onAddComboClick(combo);
        });
    }

    @Override
    public int getItemCount() {
        return combos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCombo;
        TextView tvName, tvMealType, tvCalories, tvItemCount;
        Button btnQuickAdd;

        ViewHolder(View itemView) {
            super(itemView);
            imgCombo = itemView.findViewById(R.id.imgCombo);
            tvName = itemView.findViewById(R.id.tvComboName);
            tvMealType = itemView.findViewById(R.id.tvMealType);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            btnQuickAdd = itemView.findViewById(R.id.btnQuickAdd);
        }
    }
}