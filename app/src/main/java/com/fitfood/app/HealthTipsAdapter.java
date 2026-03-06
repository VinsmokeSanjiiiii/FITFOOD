package com.fitfood.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HealthTipsAdapter extends RecyclerView.Adapter<HealthTipsAdapter.ViewHolder> {

    private final List<HealthTip> tipList;
    private final String selectedLanguage;

    public HealthTipsAdapter(Context context, List<HealthTip> tipList) {
        this.tipList = tipList;
        SharedPreferences prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        selectedLanguage = prefs.getString("language", "en");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tip_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HealthTip tip = tipList.get(position);

        if (selectedLanguage.equals("tl")) {
            holder.tvTitle.setText(tip.getTitle_tl());
            holder.tvDescription.setText(tip.getContent_tl());
        } else {
            holder.tvTitle.setText(tip.getTitle_en());
            holder.tvDescription.setText(tip.getContent_en());
        }

        holder.tvIcon.setText(tip.getIcon());

        holder.tvDescription.setVisibility(View.GONE);
        holder.imgArrow.setRotation(0);

        holder.layoutHeader.setOnClickListener(v -> {
            if (holder.tvDescription.getVisibility() == View.GONE) {
                holder.tvDescription.setVisibility(View.VISIBLE);
                holder.tvDescription.setAlpha(0f);
                holder.tvDescription.animate().alpha(1f).setDuration(200).start();
                holder.imgArrow.animate().rotation(180).setDuration(200).start();
            } else {
                holder.tvDescription.animate().alpha(0f).setDuration(150)
                        .withEndAction(() -> holder.tvDescription.setVisibility(View.GONE)).start();
                holder.imgArrow.animate().rotation(0).setDuration(200).start();
            }
        });
    }

    @Override
    public int getItemCount() {
        return tipList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTitle, tvDescription;
        ImageView imgArrow;
        LinearLayout layoutHeader;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            imgArrow = itemView.findViewById(R.id.imgArrow);
            layoutHeader = itemView.findViewById(R.id.layoutHeader);
        }
    }
}
