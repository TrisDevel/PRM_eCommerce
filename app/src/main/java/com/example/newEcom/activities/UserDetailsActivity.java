package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.model.UserModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserDetailsActivity extends AppCompatActivity {
    private static final String TAG = "UserDetails";

    // Views
    ImageView backBtn, editBtn, userProfileImage;
    TextView userName, userEmail, userRole, userStatus, userPhone, userAddress, userJoined, userLastLogin;
    TextView totalOrders, totalSpent;
    Button editUserBtn, blockUserBtn, viewOrdersBtn;

    // Data
    String userUid;
    UserModel currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        userUid = getIntent().getStringExtra("userUid");
        if (userUid == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        wireClicks();
        loadUserDetails();
    }

    private void bindViews() {
        backBtn = findViewById(R.id.backBtn);
        editBtn = findViewById(R.id.editBtn);
        userProfileImage = findViewById(R.id.userProfileImage);
        
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        userRole = findViewById(R.id.userRole);
        userStatus = findViewById(R.id.userStatus);
        userPhone = findViewById(R.id.userPhone);
        userAddress = findViewById(R.id.userAddress);
        userJoined = findViewById(R.id.userJoined);
        userLastLogin = findViewById(R.id.userLastLogin);
        
        totalOrders = findViewById(R.id.totalOrders);
        totalSpent = findViewById(R.id.totalSpent);
        
        editUserBtn = findViewById(R.id.editUserBtn);
        blockUserBtn = findViewById(R.id.blockUserBtn);
        viewOrdersBtn = findViewById(R.id.viewOrdersBtn);
    }

    private void wireClicks() {
        backBtn.setOnClickListener(v -> onBackPressed());
        
        View.OnClickListener editListener = v -> {
            if (currentUser == null) return;
            showRoleSelectionDialog();
        };
        editBtn.setOnClickListener(editListener);
        editUserBtn.setOnClickListener(editListener);
        
        blockUserBtn.setOnClickListener(v -> {
            if (currentUser == null) return;

            boolean newStatus = !currentUser.isActive();
            String action = newStatus ? "Unblock" : "Block";

            new cn.pedant.SweetAlert.SweetAlertDialog(this, cn.pedant.SweetAlert.SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Are you sure?")
                    .setContentText("Do you want to " + action.toLowerCase() + " this user?")
                    .setConfirmText("Yes, " + action + " it!")
                    .setConfirmClickListener(sDialog -> {
                        sDialog.dismissWithAnimation();
                        updateUserStatus(newStatus);
                    })
                    .setCancelButton("Cancel", cn.pedant.SweetAlert.SweetAlertDialog::dismissWithAnimation)
                    .show();
        });
        
        viewOrdersBtn.setOnClickListener(v -> {
            // TODO: Implement view user orders functionality
            Toast.makeText(this, "View orders functionality coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserDetails() {
        FirebaseUtil.getUsers().document(userUid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                currentUser = document.toObject(UserModel.class);
                                if (currentUser != null) {
                                    populateUserData();
                                } else {
                                    Log.e(TAG, "Error converting document to UserModel");
                                    Toast.makeText(UserDetailsActivity.this, "Failed to parse user data", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e(TAG, "No such user document with uid: " + userUid);
                                Toast.makeText(UserDetailsActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Log.e(TAG, "Error getting user details: ", task.getException());
                            Toast.makeText(UserDetailsActivity.this, "Failed to load user details", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void populateUserData() {
        // Basic info
        userName.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "No Name");
        userEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "No Email");
        userPhone.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "No Phone");
        userAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "No Address");
        
        // Role and status
        userRole.setText(currentUser.getRole() != null ? currentUser.getRole() : "user");
        if (currentUser.isActive()) {
            userStatus.setText("Active");
            userStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            userStatus.setText("Inactive");
            userStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        
        // Statistics
        totalOrders.setText(String.valueOf(currentUser.getTotalOrders()));
        totalSpent.setText("$" + currentUser.getTotalSpent());
        
        // Dates
        if (currentUser.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            userJoined.setText(sdf.format(currentUser.getCreatedAt().toDate()));
        } else {
            userJoined.setText("Unknown");
        }
        
        if (currentUser.getLastLoginAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            userLastLogin.setText(sdf.format(currentUser.getLastLoginAt().toDate()));
        } else {
            userLastLogin.setText("Never");
        }
        
        // Profile image
        if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
            Picasso.get()
                    .load(currentUser.getProfileImage())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(userProfileImage);
        } else {
            userProfileImage.setImageResource(R.drawable.ic_profile);
        }
        
        // Update button text and color based on user status
        if (currentUser.isActive()) {
            blockUserBtn.setText("Block User");
            blockUserBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D32F2F"))); // Red
        } else {
            blockUserBtn.setText("Unblock User");
            blockUserBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#388E3C"))); // Green
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void showRoleSelectionDialog() {
        final String[] roles = {"user", "admin"};

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select New Role")
                .setItems(roles, (dialog, which) -> {
                    String newRole = roles[which];
                    if (!newRole.equals(currentUser.getRole())) {
                        updateUserRole(newRole);
                    }
                })
                .show();
    }

    private void updateUserRole(String newRole) {
        if (userUid == null) return;

        FirebaseUtil.getUsers().document(userUid)
                .update("role", newRole)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserDetailsActivity.this, "User role updated successfully", Toast.LENGTH_SHORT).show();
                    currentUser.setRole(newRole);
                    populateUserData(); // Refresh UI
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserDetailsActivity.this, "Failed to update user role", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating user role", e);
                });
    }

    private void updateUserStatus(boolean newStatus) {
        if (userUid == null) return;

        FirebaseUtil.getUsers().document(userUid)
                .update("active", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserDetailsActivity.this, "User status updated successfully", Toast.LENGTH_SHORT).show();
                    currentUser.setActive(newStatus);
                    populateUserData(); // Refresh UI
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserDetailsActivity.this, "Failed to update user status", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating user status", e);
                });
    }
}
