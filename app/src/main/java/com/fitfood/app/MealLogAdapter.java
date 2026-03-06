package com.fitfood.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import java.util.List;

public class MealLogAdapter extends RecyclerView.Adapter<MealLogAdapter.UserViewHolder> {
    public interface OnMealActionListener {
        void onDeleteClick(MealLog mealLog);
    }
    private final Context context;
    private List<UserMealGroup> userGroups;
    private final OnMealActionListener listener;
    private RecyclerView recyclerView;
    public MealLogAdapter(Context context, List<UserMealGroup> userGroups, OnMealActionListener listener) {
        this.context = context;
        this.userGroups = userGroups;
        this.listener = listener;
    }
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_meal_group, parent, false);
        return new UserViewHolder(view);
    }
    @SuppressLint({"SetTextI18n", "InflateParams"})
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserMealGroup group = userGroups.get(position);
        holder.tvUserName.setText(group.getUserName());
        holder.tvUserCalories.setText(group.getTotalCalories() + " kcal");
        holder.tvMealCount.setText(group.getMeals().size() + " meals");
        if (group.isExpanded()) {
            holder.layoutMealsContainer.setVisibility(View.VISIBLE);
            holder.ivExpandArrow.setRotation(180);
        } else {
            holder.layoutMealsContainer.setVisibility(View.GONE);
            holder.ivExpandArrow.setRotation(0);
        }
        holder.layoutMealsContainer.removeAllViews();
        for (MealLog meal : group.getMeals()) {
            View mealRow = LayoutInflater.from(context).inflate(R.layout.item_meal_row_admin, null);
            TextView tvName = mealRow.findViewById(R.id.tvMealName);
            TextView tvDetails = mealRow.findViewById(R.id.tvMealDetails);
            ImageButton btnDelete = mealRow.findViewById(R.id.btnDeleteMeal);
            tvName.setText(meal.getMealName());
            tvDetails.setText(meal.getMealType() + " • " + meal.getCalories() + " kcal");
            if("Breakfast".equalsIgnoreCase(meal.getMealType())) tvDetails.setTextColor(Color.parseColor("#FF9800"));
            else if("Lunch".equalsIgnoreCase(meal.getMealType())) tvDetails.setTextColor(Color.parseColor("#4CAF50"));
            else if("Dinner".equalsIgnoreCase(meal.getMealType())) tvDetails.setTextColor(Color.parseColor("#3F51B5"));
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(meal);
            });
            holder.layoutMealsContainer.addView(mealRow);
        }

        holder.cardHeader.setOnClickListener(v -> {
            boolean expanded = group.isExpanded();
            group.setExpanded(!expanded);
            TransitionManager.beginDelayedTransition(recyclerView, new AutoTransition());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return userGroups.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<UserMealGroup> newList) {
        this.userGroups = newList;
        notifyDataSetChanged();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserCalories, tvMealCount;
        ImageView ivExpandArrow;
        LinearLayout layoutMealsContainer, cardHeader;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserCalories = itemView.findViewById(R.id.tvUserCalories);
            tvMealCount = itemView.findViewById(R.id.tvMealCount);
            ivExpandArrow = itemView.findViewById(R.id.ivExpandArrow);
            layoutMealsContainer = itemView.findViewById(R.id.layoutMealsContainer);
            cardHeader = itemView.findViewById(R.id.cardHeader);
        }
    }
}