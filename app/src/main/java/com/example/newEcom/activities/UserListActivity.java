package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.newEcom.R;
import com.example.newEcom.adapters.UserAdapter;
import com.example.newEcom.model.UserModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.SimpleOnSearchActionListener;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {
    private static final String TAG = "UserList";

    // Views
    ImageView backBtn, searchBtn;
    MaterialSearchBar searchBar;
    LinearLayout searchLinearLayout, emptyStateLayout;
    RecyclerView usersRecyclerView;
    TextView userCountText, sortText;
    Chip allUsersChip, activeUsersChip, adminUsersChip, newUsersChip;

    // Data
    UserAdapter userAdapter;
    List<UserModel> allUsers = new ArrayList<>();
    List<UserModel> filteredUsers = new ArrayList<>();
    String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        bindViews();
        wireClicks();
        setupRecyclerView();
        loadUsers();
    }

    private void bindViews() {
        backBtn = findViewById(R.id.backBtn);
        searchBtn = findViewById(R.id.searchBtn);
        searchBar = findViewById(R.id.searchBar);
        searchLinearLayout = findViewById(R.id.searchLinearLayout);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        userCountText = findViewById(R.id.userCountText);
        sortText = findViewById(R.id.sortText);
        
        allUsersChip = findViewById(R.id.allUsersChip);
        activeUsersChip = findViewById(R.id.activeUsersChip);
        adminUsersChip = findViewById(R.id.adminUsersChip);
        newUsersChip = findViewById(R.id.newUsersChip);
    }

    private void wireClicks() {
        backBtn.setOnClickListener(v -> onBackPressed());
        
        searchBtn.setOnClickListener(v -> {
            if (searchLinearLayout.getVisibility() == View.GONE) {
                searchLinearLayout.setVisibility(View.VISIBLE);
                searchBar.openSearch();
            } else {
                searchLinearLayout.setVisibility(View.GONE);
                searchBar.closeSearch();
            }
        });

        searchBar.setOnSearchActionListener(new SimpleOnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                super.onSearchStateChanged(enabled);
                if (!enabled) {
                    searchLinearLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                super.onSearchConfirmed(text);
                filterUsers(text.toString());
            }
        });

        // Filter chips
        allUsersChip.setOnClickListener(v -> {
            setActiveChip(allUsersChip);
            currentFilter = "all";
            applyFilter();
        });

        activeUsersChip.setOnClickListener(v -> {
            setActiveChip(activeUsersChip);
            currentFilter = "active";
            applyFilter();
        });

        adminUsersChip.setOnClickListener(v -> {
            setActiveChip(adminUsersChip);
            currentFilter = "admin";
            applyFilter();
        });

        newUsersChip.setOnClickListener(v -> {
            setActiveChip(newUsersChip);
            currentFilter = "new";
            applyFilter();
        });
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(filteredUsers, this);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(userAdapter);
    }

    private void loadUsers() {
        // For demo purposes, show empty state
        // In a real app, you would load user data from your backend
        allUsers.clear();
        filteredUsers.clear();
        userAdapter.notifyDataSetChanged();
        updateUserCount();
        checkEmptyState();
    }

    private void filterUsers(String searchText) {
        filteredUsers.clear();
        
        for (UserModel user : allUsers) {
            boolean matchesSearch = searchText.isEmpty() ||
                    (user.getFullName() != null && user.getFullName().toLowerCase().contains(searchText.toLowerCase())) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchText.toLowerCase())) ||
                    (user.getRole() != null && user.getRole().toLowerCase().contains(searchText.toLowerCase()));
            
            if (matchesSearch) {
                filteredUsers.add(user);
            }
        }
        
        userAdapter.notifyDataSetChanged();
        updateUserCount();
        checkEmptyState();
    }

    private void applyFilter() {
        filteredUsers.clear();
        
        for (UserModel user : allUsers) {
            boolean matchesFilter = false;
            
            switch (currentFilter) {
                case "all":
                    matchesFilter = true;
                    break;
                case "active":
                    matchesFilter = user.isActive();
                    break;
                case "admin":
                    matchesFilter = "admin".equals(user.getRole());
                    break;
                case "new":
                    // For demo purposes, consider users created in last 30 days as "new"
                    matchesFilter = true; // You can implement date logic here
                    break;
            }
            
            if (matchesFilter) {
                filteredUsers.add(user);
            }
        }
        
        userAdapter.notifyDataSetChanged();
        updateUserCount();
        checkEmptyState();
    }

    private void setActiveChip(Chip activeChip) {
        allUsersChip.setChecked(false);
        activeUsersChip.setChecked(false);
        adminUsersChip.setChecked(false);
        newUsersChip.setChecked(false);
        
        activeChip.setChecked(true);
    }

    private void updateUserCount() {
        userCountText.setText(filteredUsers.size() + " users found");
    }

    private void checkEmptyState() {
        if (filteredUsers.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        usersRecyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        usersRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
