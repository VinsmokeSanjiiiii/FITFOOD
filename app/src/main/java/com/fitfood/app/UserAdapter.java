package com.fitfood.app;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onMessageClick(User user);
        void onEditClick(User user);
        void onDeleteClick(User user);
    }

    private final Context context;
    private List<User> userList;
    private final OnUserActionListener listener;

    public UserAdapter(Context context, List<User> userList, OnUserActionListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
    }

    public void updateList(List<User> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_admin, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvName.setText(user.getName() != null ? user.getName() : "Unknown");
        holder.tvEmail.setText(user.getEmail());
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";
        holder.tvRole.setText(role);
        if (role.equals("ADMIN")) {
            holder.tvRole.setTextColor(Color.parseColor("#FF5722"));
        } else {
            holder.tvRole.setTextColor(Color.parseColor("#757575"));
        }
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.use2)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.use2);
        }
        holder.btnMessage.setOnClickListener(v -> listener.onMessageClick(user));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(user));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole;
        ImageView imgAvatar;
        ImageButton btnMessage, btnEdit, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRole = itemView.findViewById(R.id.tvUserRole);
            imgAvatar = itemView.findViewById(R.id.imgUserAvatar);

            btnMessage = itemView.findViewById(R.id.btnMessageUser);
            btnEdit = itemView.findViewById(R.id.btnEditUser);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}