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
import com.example.newEcom.model.UserModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.Date;

public class UserManagementActivity extends AppCompatActivity {
    private static final String TAG = "UserManagement";

    // Views
    ImageView backBtn;
    CardView viewAllUsersBtn;
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
    }

    // Load thống kê người dùng từ Firebase Firestore
    private void loadUserStatistics() {
        // Hiển thị giá trị mặc định trước khi load
        showDefaultValues();
        
        // Lấy dữ liệu từ Firestore collection "users"
        FirebaseUtil.getUsers()
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            // Khởi tạo các biến đếm
                            int total = 0;
                            int active = 0;
                            int admin = 0;
                            int newUsersCount = 0;
                            
                            // Tính toán ngày 30 ngày trước để xác định "new users"
                            Calendar calendar = Calendar.getInstance();
                            calendar.add(Calendar.DAY_OF_YEAR, -30);
                            Date thirtyDaysAgo = calendar.getTime();
                            
                            // Duyệt qua tất cả users và tính toán thống kê
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    UserModel user = document.toObject(UserModel.class);
                                    
                                    // Đếm tổng số users
                                    total++;
                                    
                                    // Đếm active users
                                    if (user.isActive()) {
                                        active++;
                                    }
                                    
                                    // Đếm admin users
                                    if ("admin".equalsIgnoreCase(user.getRole())) {
                                        admin++;
                                    }
                                    
                                    // Đếm new users (tạo trong 30 ngày gần đây)
                                    if (user.getCreatedAt() != null) {
                                        Date createdDate = user.getCreatedAt().toDate();
                                        if (createdDate.after(thirtyDaysAgo)) {
                                            newUsersCount++;
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing user document: " + e.getMessage());
                                }
                            }
                            
                            // Cập nhật UI với dữ liệu thực
                            updateStatisticsUI(total, active, admin, newUsersCount);
                            
                            Log.d(TAG, "User statistics loaded successfully: Total=" + total + 
                                    ", Active=" + active + ", Admin=" + admin + ", New=" + newUsersCount);
                        } else {
                            // Xử lý lỗi khi load dữ liệu
                            Log.e(TAG, "Error loading user statistics", task.getException());
                            Toast.makeText(UserManagementActivity.this, 
                                    "Failed to load user statistics", 
                                    Toast.LENGTH_SHORT).show();
                            showDefaultValues();
                        }
                    }
                });
    }
    
    // Cập nhật UI với dữ liệu thống kê
    private void updateStatisticsUI(int total, int active, int admin, int newUsersCount) {
        totalUsers.setText(String.valueOf(total));
        activeUsers.setText(String.valueOf(active));
        adminUsers.setText(String.valueOf(admin));
        newUsers.setText(String.valueOf(newUsersCount));
    }

    private void showDefaultValues() {
        totalUsers.setText("0");
        activeUsers.setText("0");
        newUsers.setText("0");
        adminUsers.setText("0");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics khi quay lại activity
        loadUserStatistics();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
