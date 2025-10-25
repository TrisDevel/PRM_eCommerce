package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class UserManagementActivity extends AppCompatActivity {
    private static final String TAG = "UserManagement";

    // Views
    ImageView backBtn;
    CardView viewAllUsersBtn, searchUsersBtn, userAnalyticsBtn;
    TextView totalUsers, activeUsers, newUsers, adminUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        bindViews();
        wireClicks();
        loadUserStatistics();
    }

    private void bindViews() {
        backBtn = findViewById(R.id.backBtn);
        viewAllUsersBtn = findViewById(R.id.viewAllUsersBtn);
        searchUsersBtn = findViewById(R.id.searchUsersBtn);
        userAnalyticsBtn = findViewById(R.id.userAnalyticsBtn);
        
        totalUsers = findViewById(R.id.totalUsers);
        activeUsers = findViewById(R.id.activeUsers);
        newUsers = findViewById(R.id.newUsers);
        adminUsers = findViewById(R.id.adminUsers);
    }

    private void wireClicks() {
        backBtn.setOnClickListener(v -> onBackPressed());
        
        viewAllUsersBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserListActivity.class);
            startActivity(intent);
        });
        
        searchUsersBtn.setOnClickListener(v -> {
            // TODO: Implement search functionality
            Toast.makeText(this, "Search functionality coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        userAnalyticsBtn.setOnClickListener(v -> {
            // TODO: Implement analytics
            Toast.makeText(this, "Analytics coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserStatistics() {
        // For demo purposes, show default values
        // In a real app, you would load user data from your backend
        totalUsers.setText("0");
        activeUsers.setText("0");
        adminUsers.setText("0");
        newUsers.setText("0");
    }

    private void showDefaultValues() {
        totalUsers.setText("0");
        activeUsers.setText("0");
        newUsers.setText("0");
        adminUsers.setText("0");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
