package com.example.newEcom.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.newEcom.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SendNotificationActivity extends AppCompatActivity {
    EditText titleEditText, bodyEditText, imageEditText;
    Button sendBtn;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_notification);

        titleEditText = findViewById(R.id.titleEditText);
        bodyEditText = findViewById(R.id.bodyEditText);
        imageEditText = findViewById(R.id.imageEditText);
        sendBtn = findViewById(R.id.sendBtn);
        progressBar = findViewById(R.id.progressBar);

        sendBtn.setOnClickListener(v -> sendNotificationToFirestore());
    }

    private void sendNotificationToFirestore() {
        String title = titleEditText.getText().toString().trim();
        String body = bodyEditText.getText().toString().trim();
        String image = imageEditText.getText().toString().trim();

        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Please fill title and body", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        sendBtn.setEnabled(false);

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("image", image);
        notification.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    sendBtn.setEnabled(true);
                    Toast.makeText(this, "Notification sent to all users!", Toast.LENGTH_SHORT).show();
                    titleEditText.setText("");
                    bodyEditText.setText("");
                    imageEditText.setText("");
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    sendBtn.setEnabled(true);
                    Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show();
                });
    }
}
