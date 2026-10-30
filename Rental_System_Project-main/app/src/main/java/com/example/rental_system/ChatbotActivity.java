package com.example.rental_system;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class ChatbotActivity extends AppCompatActivity {

    private static final String TAG = "RentalChatbot";

    private static final int NAVY = Color.rgb(20, 45, 78);
    private static final int TEAL = Color.rgb(32, 178, 170);
    private static final int DARK_TEXT = Color.rgb(31, 41, 55);

    private static final String GREETING =
            "Hello! I'm your Rental Assistant. Ask me about rooms, rental requests, payments, reviews, or complaints.";

    private LinearLayout messagesContainer;
    private ScrollView chatScrollView;
    private EditText messageInput;
    private Button sendButton;
    private ProgressBar chatProgress;

    private FirebaseFirestore db;
    private String userId;
    private final String chatId = "default";

    private GenerativeModelFutures model;

    private String userRole = "tenant";
    private boolean requestInProgress = false;
    private boolean historyLoaded = false;

    private static final String SYSTEM_INSTRUCTIONS =
            "You are Rental Assistant, a helpful chatbot inside a room rental Android app. "
                    + "Help tenants with finding rooms, rental requests, reviews, complaints, "
                    + "and monthly payment reminders. Help owners with adding rooms, editing "
                    + "property details, managing rental requests, reviews, and complaints. "
                    + "The current user's app role is ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize the existing chat UI.
        messagesContainer = findViewById(R.id.messagesContainer);
        chatScrollView = findViewById(R.id.chatScrollView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatProgress = findViewById(R.id.chatProgress);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        sendButton.setOnClickListener(v -> sendMessage());

        // Prevent sending before saved history has loaded.
        sendButton.setEnabled(false);

        String role = getIntent().getStringExtra("user_role");
        if ("owner".equalsIgnoreCase(role)) {
            userRole = "owner";
        }

        // Require Firebase Authentication.
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            messageInput.setEnabled(false);
            sendButton.setEnabled(false);
            finish();
            return;
        }

        userId = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        db = FirebaseFirestore.getInstance();

        // Initialize Gemini.
        GenerativeModel generativeModel =
                FirebaseAI.getInstance(GenerativeBackend.googleAI())
                        .generativeModel("gemini-3.8-flash");

        model = GenerativeModelFutures.from(generativeModel);

        // Load previous messages from Firestore.
        loadChatHistory();
    }

    // Save a message to this user's conversation.
    private void saveMessage(String message, String sender) {
        if (userId == null || message == null
                || message.trim().isEmpty()) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("text", message);
        data.put("sender", sender);
        data.put("timestamp", FieldValue.serverTimestamp());

        db.collection("chatHistory")
                .document(userId)
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .add(data)
                .addOnFailureListener(error ->
                        Log.e(TAG, "Could not save message", error));
    }

    // Retrieve saved messages in chronological order.
    private void loadChatHistory() {
        db.collection("chatHistory")
                .document(userId)
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {

                    messagesContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        // Show the greeting only for a new conversation.
                        addMessage(GREETING, false);
                    } else {
                        for (DocumentSnapshot document
                                : snapshot.getDocuments()) {

                            String text = document.getString("text");
                            String sender = document.getString("sender");

                            if (text != null && sender != null) {
                                addMessage(
                                        text,
                                        "user".equals(sender)
                                );
                            }
                        }
                    }

                    historyLoaded = true;
                    sendButton.setEnabled(true);
                    scrollToBottom();
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Could not load chat history", error);

                    addMessage(
                            "I couldn't load your saved chat history. "
                                    + "You can still send a new message.",
                            false
                    );

                    historyLoaded = true;
                    sendButton.setEnabled(true);
                });
    }

    private void sendMessage() {
        if (!historyLoaded || requestInProgress) {
            return;
        }

        String message = messageInput.getText()
                .toString()
                .trim();

        if (message.isEmpty()) {
            messageInput.setError("Please enter a message");
            return;
        }

        if (message.length() > 2000) {
            messageInput.setError("Maximum 2000 characters");
            return;
        }

        // Display and save the user's message.
        addMessage(message, true);
        saveMessage(message, "user");
        messageInput.setText("");

        requestInProgress = true;
        sendButton.setEnabled(false);
        chatProgress.setVisibility(View.VISIBLE);

        String promptText =
                SYSTEM_INSTRUCTIONS + userRole + ". "
                        + "Give a friendly, concise and practical answer. "
                        + "Do not claim to have accessed the live Firebase database "
                        + "or know actual room availability, prices, bookings, "
                        + "or payment status. "
                        + "Do not ask for passwords, authentication codes, "
                        + "or secret keys. "
                        + "The user's message is: " + message;

        Content prompt = new Content.Builder()
                .addText(promptText)
                .build();

        ListenableFuture<GenerateContentResponse> future =
                model.generateContent(prompt);

        Futures.addCallback(
                future,
                new FutureCallback<GenerateContentResponse>() {

                    @Override
                    public void onSuccess(GenerateContentResponse response) {
                        String reply = response.getText();

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            if (reply == null || reply.trim().isEmpty()) {
                                addMessage(
                                        "I couldn't create an answer. Please try again.",
                                        false
                                );
                            } else {
                                String cleanReply = reply.trim();

                                // Display and save Gemini's response.
                                addMessage(cleanReply, false);
                                saveMessage(cleanReply, "bot");
                            }

                            finishRequest();
                        });
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        Log.e(TAG, "Gemini request failed", error);

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            addMessage(
                                    "Sorry, I couldn't connect to Gemini. "
                                            + "Check your internet connection "
                                            + "and Firebase AI Logic setup, then try again.",
                                    false
                            );

                            finishRequest();
                        });
                    }
                },
                MoreExecutors.directExecutor()
        );
    }

    private void finishRequest() {
        requestInProgress = false;
        chatProgress.setVisibility(View.GONE);
        sendButton.setEnabled(historyLoaded);
    }

    // Display a chat bubble.
    private void addMessage(String text, boolean isUser) {
        TextView bubble = new TextView(this);

        bubble.setText(text);
        bubble.setTextSize(15);
        bubble.setTextColor(isUser ? Color.WHITE : DARK_TEXT);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setTextIsSelectable(true);

        GradientDrawable background = new GradientDrawable();
        background.setColor(isUser ? NAVY : Color.WHITE);
        background.setCornerRadius(dp(18));

        if (!isUser) {
            background.setStroke(dp(1), Color.rgb(224, 247, 245));
        }

        bubble.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.gravity = isUser ? Gravity.END : Gravity.START;
        params.setMargins(dp(8), dp(5), dp(8), dp(5));

        messagesContainer.addView(bubble, params);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (chatScrollView != null) {
            chatScrollView.post(() ->
                    chatScrollView.fullScroll(View.FOCUS_DOWN)
            );
        }
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}