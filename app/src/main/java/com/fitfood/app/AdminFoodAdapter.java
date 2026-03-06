package com.fitfood.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class AdminFoodAdapter extends RecyclerView.Adapter<AdminFoodAdapter.ViewHolder> {

    public interface OnFoodActionListener {
        void onEdit(FoodItem food);
        void onDelete(FoodItem food);
    }

    private final Context context;
    private List<FoodItem> list;
    private final OnFoodActionListener listener;

    public AdminFoodAdapter(Context context, List<FoodItem> list, OnFoodActionListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<FoodItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_food_admin, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodItem item = list.get(position);

        holder.tvName.setText(item.getName());
        holder.tvDetails.setText(String.format("%.0f kcal • %.0fg", item.getKcal(), item.getGrams()));
        holder.tvCategory.setText(item.getCategory());

        holder.imgIcon.setPadding(0, 0, 0, 0);
        holder.imgIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.imgIcon.setColorFilter(null);
        holder.imgIcon.setImageTintList(null);

        displayImage(item.getImage(), holder.imgIcon);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    private void displayImage(String imagePath, ImageView imageView) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            setDefaultImage(imageView);
            return;
        }

        String value = imagePath.trim();

        if (value.startsWith("http://") || value.startsWith("https://")) {
            Glide.with(context)
                    .load(value)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.stat_notify_error)
                    .into(imageView);
            return;
        }

        if (value.startsWith("data:") && value.contains("base64,")) {
            String base64 = value.substring(value.indexOf("base64,") + 7);
            setBitmapFromBase64(base64, imageView);
            return;
        }

        if (value.length() > 200 && value.matches("^[A-Za-z0-9+/=\\r\\n]+$")) {
            setBitmapFromBase64(value, imageView);
            return;
        }

        int resId = context.getResources()
                .getIdentifier(value, "drawable", context.getPackageName());

        if (resId != 0) {
            imageView.setImageResource(resId);
        } else {
            setDefaultImage(imageView);
        }
    }

    private void setBitmapFromBase64(String base64, ImageView imageView) {
        try {
            byte[] decoded = Base64.decode(base64, Base64.NO_WRAP);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                setDefaultImage(imageView);
            }
        } catch (Exception e) {
            setDefaultImage(imageView);
        }
    }

    private void setDefaultImage(ImageView imageView) {
        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        int padding = (int) (12 * context.getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails, tvCategory;
        ImageView imgIcon;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvDetails = itemView.findViewById(R.id.tvFoodDetails);
            tvCategory = itemView.findViewById(R.id.tvFoodCat);
            imgIcon = itemView.findViewById(R.id.imgFoodIcon);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
