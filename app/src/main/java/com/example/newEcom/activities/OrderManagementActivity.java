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
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.adapters.AdminOrderAdapter;
import com.example.newEcom.model.OrderModel;
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

/**
 * ORDER MANAGEMENT ACTIVITY - ADMIN DASHBOARD
 * Admin dashboard for managing all orders from all users
 */
public class OrderManagementActivity extends AppCompatActivity {
    private static final String TAG = "OrderManagement";

    ImageView backBtn, searchBtn;
    MaterialSearchBar searchBar;
    LinearLayout searchLinearLayout, emptyStateLayout;
    RecyclerView ordersRecyclerView;
    TextView orderCountText, sortText;
    Chip allOrdersChip, pendingChip, confirmedChip, shippingChip, deliveredChip, cancelledChip;

    AdminOrderAdapter orderAdapter;
    List<OrderModel> allOrders = new ArrayList<>();
    List<OrderModel> filteredOrders = new ArrayList<>();
    
    String currentStatusFilter = "all";
    String currentSearchText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_management);

        bindViews();
        wireClicks();
        setupRecyclerView();
        loadOrders();
    }

    private void bindViews() {
        backBtn = findViewById(R.id.backBtn);
        searchBtn = findViewById(R.id.searchBtn);
        searchBar = findViewById(R.id.searchBar);
        searchLinearLayout = findViewById(R.id.searchLinearLayout);
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        orderCountText = findViewById(R.id.orderCountText);
        sortText = findViewById(R.id.sortText);
        allOrdersChip = findViewById(R.id.allOrdersChip);
        pendingChip = findViewById(R.id.pendingChip);
        confirmedChip = findViewById(R.id.confirmedChip);
        shippingChip = findViewById(R.id.shippingChip);
        deliveredChip = findViewById(R.id.deliveredChip);
        cancelledChip = findViewById(R.id.cancelledChip);
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
                currentSearchText = "";
                applyFilters();
            }
        });

        searchBar.setOnSearchActionListener(new SimpleOnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                super.onSearchStateChanged(enabled);
                if (!enabled) {
                    searchLinearLayout.setVisibility(View.GONE);
                    currentSearchText = "";
                    applyFilters();
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                super.onSearchConfirmed(text);
                currentSearchText = text.toString();
                applyFilters();
            }
        });

        allOrdersChip.setOnClickListener(v -> {
            setActiveStatusChip(allOrdersChip);
            currentStatusFilter = "all";
            sortText.setText("Sort by: Latest");
            applyFilters();
        });

        pendingChip.setOnClickListener(v -> {
            setActiveStatusChip(pendingChip);
            currentStatusFilter = "pending";
            sortText.setText("Filter: Pending");
            applyFilters();
        });

        confirmedChip.setOnClickListener(v -> {
            setActiveStatusChip(confirmedChip);
            currentStatusFilter = "confirmed";
            sortText.setText("Filter: Confirmed");
            applyFilters();
        });

        shippingChip.setOnClickListener(v -> {
            setActiveStatusChip(shippingChip);
            currentStatusFilter = "shipping";
            sortText.setText("Filter: Shipping");
            applyFilters();
        });

        deliveredChip.setOnClickListener(v -> {
            setActiveStatusChip(deliveredChip);
            currentStatusFilter = "delivered";
            sortText.setText("Filter: Delivered");
            applyFilters();
        });

        cancelledChip.setOnClickListener(v -> {
            setActiveStatusChip(cancelledChip);
            currentStatusFilter = "cancelled";
            sortText.setText("Filter: Cancelled");
            applyFilters();
        });
    }

    private void setupRecyclerView() {
        orderAdapter = new AdminOrderAdapter(filteredOrders, this);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersRecyclerView.setAdapter(orderAdapter);
    }

    private void loadOrders() {
        allOrders.clear();
        filteredOrders.clear();
        orderAdapter.notifyDataSetChanged();
        
        Log.d(TAG, "Loading all orders from Firestore...");
        
        FirebaseUtil.getAllOrders()
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            allOrders.clear();
                            
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    OrderModel order = document.toObject(OrderModel.class);
                                    
                                    // ✅ Extract userId from document path
                                    // Path format: orders/{userId}/ordersList/{orderId}
                                    String documentPath = document.getReference().getPath();
                                    String[] pathParts = documentPath.split("/");
                                    if (pathParts.length >= 2) {
                                        String userId = pathParts[1]; // orders/{userId}/...
                                        order.setUserId(userId);
                                        Log.d(TAG, "Extracted userId: " + userId + " for orderId: " + order.getOrderId());
                                    } else {
                                        Log.w(TAG, "Could not extract userId from path: " + documentPath);
                                    }
                                    
                                    allOrders.add(order);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing order document: " + e.getMessage());
                                }
                            }
                            java.util.Collections.sort(allOrders, new java.util.Comparator<OrderModel>() {
                                @Override
                                public int compare(OrderModel o1, OrderModel o2) {
                                    if (o1.getTimestamp() == null || o2.getTimestamp() == null) {
                                        return 0;
                                    }
                                    return o2.getTimestamp().compareTo(o1.getTimestamp());
                                }
                            });

                            applyFilters();
                            Log.d(TAG, "Loaded and sorted " + allOrders.size() + " orders from Firestore");
                        } else {
                            Log.e(TAG, "Error loading orders", task.getException());
                            Toast.makeText(OrderManagementActivity.this, 
                                    "Failed to load orders. Check Firestore index.", 
                                    Toast.LENGTH_LONG).show();
                            
                            filteredOrders.clear();
                            orderAdapter.notifyDataSetChanged();
                            updateOrderCount();
                            checkEmptyState();
                        }
                    }
                });
    }

    private void applyFilters() {
        filteredOrders.clear();
        
        for (OrderModel order : allOrders) {
            boolean matchesFilters = true;
            
            if (!currentStatusFilter.equals("all")) {
                String orderStatus = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
                matchesFilters = orderStatus.equals(currentStatusFilter.toLowerCase());
            }
            
            if (matchesFilters && !currentSearchText.isEmpty()) {
                String searchLower = currentSearchText.toLowerCase();
                boolean matchesSearch = false;
                
                if (String.valueOf(order.getOrderId()).contains(searchLower)) {
                    matchesSearch = true;
                }
                if (order.getFullName() != null && order.getFullName().toLowerCase().contains(searchLower)) {
                    matchesSearch = true;
                }
                if (order.getEmail() != null && order.getEmail().toLowerCase().contains(searchLower)) {
                    matchesSearch = true;
                }
                if (order.getPhoneNumber() != null && order.getPhoneNumber().contains(searchLower)) {
                    matchesSearch = true;
                }
                
                matchesFilters = matchesSearch;
            }
            
            if (matchesFilters) {
                filteredOrders.add(order);
            }
        }
        
        orderAdapter.notifyDataSetChanged();
        updateOrderCount();
        checkEmptyState();
        
        Log.d(TAG, "Filters applied: " + filteredOrders.size() + "/" + allOrders.size() + " orders shown");
    }
    private void setActiveStatusChip(Chip activeChip) {
        allOrdersChip.setChecked(false);
        pendingChip.setChecked(false);
        confirmedChip.setChecked(false);
        shippingChip.setChecked(false);
        deliveredChip.setChecked(false);
        cancelledChip.setChecked(false);
        
        activeChip.setChecked(true);
    }

    private void updateOrderCount() {
        orderCountText.setText(filteredOrders.size() + " orders found");
    }

    private void checkEmptyState() {
        if (filteredOrders.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        ordersRecyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        ordersRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

