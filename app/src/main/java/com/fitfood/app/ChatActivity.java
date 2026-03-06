package com.fitfood.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMessageInput;
    private FirebaseFirestore db;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private String chatRoomId;
    private String currentUserId;
    private boolean amIAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = auth.getUid();
        rvChat = findViewById(R.id.rvChat);
        etMessageInput = findViewById(R.id.etMessageInput);
        ImageButton btnSend = findViewById(R.id.btnSend);
        TextView tvChatHeader = findViewById(R.id.tvChatHeader);
        ImageButton btnBack = findViewById(R.id.btnBack);
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
        if (getIntent().hasExtra("targetUserId")) {
            amIAdmin = true;
            chatRoomId = getIntent().getStringExtra("targetUserId");
            String userName = getIntent().getStringExtra("targetUserName");
            tvChatHeader.setText("Chat with " + userName);
        } else {
            amIAdmin = false;
            chatRoomId = currentUserId;
            tvChatHeader.setText("Admin Support");
        }
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        listenForMessages();
    }

    private void listenForMessages() {
        db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                ChatMessage message = dc.getDocument().toObject(ChatMessage.class);
                                messageList.add(message);
                                chatAdapter.notifyItemInserted(messageList.size() - 1);
                                rvChat.smoothScrollToPosition(messageList.size() - 1);
                            }
                        }
                    }
                });
    }

    private void sendMessage() {
        String msgContent = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(msgContent)) return;
        etMessageInput.setText("");
        ChatMessage message = new ChatMessage(
                currentUserId,
                msgContent,
                new Timestamp(new Date()),
                amIAdmin
        );
        db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .add(message)
                .addOnFailureListener(e ->
                        Toast.makeText(ChatActivity.this, "Failed to send", Toast.LENGTH_SHORT).show());
    }
}