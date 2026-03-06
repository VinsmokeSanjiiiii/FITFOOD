package com.fitfood.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final List<ChatMessage> messageList;
    private final String currentUserId;

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messageList.get(position);
        boolean isMe = msg.getSenderId().equals(currentUserId);
        if (isMe) {
            holder.layoutRight.setVisibility(View.VISIBLE);
            holder.layoutLeft.setVisibility(View.GONE);
            holder.tvMsgRight.setText(msg.getMessage());
            if(msg.getTimestamp() != null)
                holder.tvTimeRight.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(msg.getTimestamp().toDate()));
        } else {
            holder.layoutRight.setVisibility(View.GONE);
            holder.layoutLeft.setVisibility(View.VISIBLE);
            holder.tvMsgLeft.setText(msg.getMessage());
            if(msg.getTimestamp() != null)
                holder.tvTimeLeft.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(msg.getTimestamp().toDate()));
        }
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutLeft, layoutRight;
        TextView tvMsgLeft, tvTimeLeft, tvMsgRight, tvTimeRight;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutLeft = itemView.findViewById(R.id.layoutLeft);
            layoutRight = itemView.findViewById(R.id.layoutRight);
            tvMsgLeft = itemView.findViewById(R.id.tvMsgLeft);
            tvTimeLeft = itemView.findViewById(R.id.tvTimeLeft);
            tvMsgRight = itemView.findViewById(R.id.tvMsgRight);
            tvTimeRight = itemView.findViewById(R.id.tvTimeRight);
        }
    }
}