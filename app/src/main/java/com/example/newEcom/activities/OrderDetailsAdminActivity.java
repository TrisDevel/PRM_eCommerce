package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.adapters.OrderItemAdapter;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * ORDER DETAILS ADMIN ACTIVITY
 * Admin interface for viewing and updating order details
 */
public class OrderDetailsAdminActivity extends AppCompatActivity {
    private static final String TAG = "OrderDetailsAdmin";

    ImageView backBtn;
    TextView orderIdText, customerNameText, emailText, phoneText, addressText;
    TextView statusText, paymentMethodText, transactionIdText, totalAmountText, itemCountText, orderDateText;
    TextView commentsText, emptyItemsText;

    RecyclerView orderItemsRecyclerView;
    androidx.cardview.widget.CardView commentsLayout;

    int orderId;
    String userId; // ✅ User ID để query items
    String customerName, email, phone, address, status, paymentMethod, transactionId, comments;
    int totalAmount, itemCount;
    long timestampSeconds;
    
    List<OrderItemModel> orderItems = new ArrayList<>();
    OrderItemAdapter orderItemAdapter;
    SweetAlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_order_details_admin);
            bindViews();
            getIntentData();

            //Validate Intent data
            if (orderId == 0) {
                Log.e(TAG, "Invalid orderId received: " + orderId);
                Toast.makeText(this, "Error: Invalid order data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            displayOrderInfo();
            loadOrderItems();
            backBtn.setOnClickListener(v -> onBackPressed());
            
        } catch (Exception e) {
            Log.e(TAG, "CRASH in onCreate", e);
            Log.e(TAG, "Exception type: " + e.getClass().getName());
            Log.e(TAG, "Exception message: " + e.getMessage());
            
            // Print full stack trace to logcat
            e.printStackTrace();
            
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            
            Toast.makeText(this, "Error loading order details: " + errorMsg, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * BIND VIEWS
     */
    private void bindViews() {
        try {
            backBtn = findViewById(R.id.backBtn);
            orderIdText = findViewById(R.id.orderIdText);
            customerNameText = findViewById(R.id.customerNameText);
            emailText = findViewById(R.id.emailText);
            phoneText = findViewById(R.id.phoneText);
            addressText = findViewById(R.id.addressText);
            statusText = findViewById(R.id.statusText);
            paymentMethodText = findViewById(R.id.paymentMethodText);
            transactionIdText = findViewById(R.id.transactionIdText);
            totalAmountText = findViewById(R.id.totalAmountText);
            itemCountText = findViewById(R.id.itemCountText);
            orderDateText = findViewById(R.id.orderDateText);
            commentsText = findViewById(R.id.commentsText);
            commentsLayout = findViewById(R.id.commentsLayout);
            orderItemsRecyclerView = findViewById(R.id.orderItemsRecyclerView);
            emptyItemsText = findViewById(R.id.emptyItemsText);

            //Validate critical views
            if (backBtn == null || orderIdText == null || statusText == null) {
                throw new RuntimeException("Critical views not found in layout");
            }

            // Setup progress dialog
            progressDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
            progressDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
            progressDialog.setTitleText("Updating...");
            progressDialog.setCancelable(false);
            
        } catch (Exception e) {
            Log.e(TAG, "Error binding views: " + e.getMessage(), e);
            throw e; // Re-throw để onCreate catch
        }
    }

    /**
     *LẤY DỮ LIỆU TỪ INTENT
     */
    private void getIntentData() {
        try {
            orderId = getIntent().getIntExtra("orderId", 0);
            userId = getIntent().getStringExtra("userId"); // ✅ Nhận userId từ Intent
            customerName = getIntent().getStringExtra("customerName");
            email = getIntent().getStringExtra("email");
            phone = getIntent().getStringExtra("phone");
            address = getIntent().getStringExtra("address");
            status = getIntent().getStringExtra("status");
            paymentMethod = getIntent().getStringExtra("paymentMethod");
            transactionId = getIntent().getStringExtra("transactionId");
            totalAmount = getIntent().getIntExtra("totalAmount", 0);
            itemCount = getIntent().getIntExtra("itemCount", 0);
            timestampSeconds = getIntent().getLongExtra("timestamp", 0);
            comments = getIntent().getStringExtra("comments");
            
            // ✅ Log để debug
            Log.d(TAG, "Received Intent data - orderId: " + orderId + ", userId: " + userId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting intent data: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     *HIỂN THỊ THÔNG TIN ORDER
     */
    private void displayOrderInfo() {
        // Order ID
        orderIdText.setText("Order #" + orderId);
        
        // Customer Info
        customerNameText.setText(customerName != null ? customerName : "—");
        emailText.setText(email != null ? email : "—");
        phoneText.setText(phone != null ? phone : "—");
        addressText.setText(address != null ? address : "—");
        
        // Status
        statusText.setText(status != null ? status : "Unknown");
        setStatusColor(statusText, status);
        
        // Payment Method
        paymentMethodText.setText(paymentMethod != null ? paymentMethod : "—");
        setPaymentMethodColor(paymentMethodText, paymentMethod);
        
        // Transaction ID
        if (transactionId != null && !transactionId.isEmpty()) {
            transactionIdText.setText(transactionId);
        } else {
            transactionIdText.setText("—");
        }
        
        // Total Amount
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        totalAmountText.setText(currencyFormat.format(totalAmount));
        
        // Item Count
        itemCountText.setText(itemCount + " items");
        
        // Order Date
        if (timestampSeconds > 0) {
            Date date = new Date(timestampSeconds * 1000);
            String dateStr = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date);
            orderDateText.setText(dateStr);
        } else {
            orderDateText.setText("—");
        }
        
        // Comments
        if (comments != null && !comments.isEmpty()) {
            commentsText.setText(comments);
            commentsLayout.setVisibility(View.VISIBLE);
        } else {
            commentsLayout.setVisibility(View.GONE);
        }
    }



    /**
     * ✅ LOAD ORDER ITEMS TỪ FIRESTORE (FIXED VERSION)
     * Lấy danh sách sản phẩm trong order này bằng cách query trực tiếp path
     * Path: orders/{userId}/ordersList/{orderId}/items
     */
    private void loadOrderItems() {
        try {
            // Setup RecyclerView
            orderItemAdapter = new OrderItemAdapter(orderItems, this);
            orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            orderItemsRecyclerView.setAdapter(orderItemAdapter);
            
            // ✅ Kiểm tra itemCount trước khi query
            if (itemCount == 0) {
                Log.w(TAG, "Order has 0 items. Skipping Firestore query.");
                orderItemsRecyclerView.setVisibility(View.GONE);
                emptyItemsText.setVisibility(View.VISIBLE);
                return;
            }
            
            // ✅ Kiểm tra userId có hợp lệ không
            if (userId == null || userId.isEmpty()) {
                Log.e(TAG, "userId is null or empty. Cannot load items. Falling back to collection group query...");
                loadOrderItemsFallback(); // Fallback về phương pháp cũ
                return;
            }
            
            // ✅ Query trực tiếp với path đầy đủ
            String firebasePath = "orders/" + userId + "/ordersList/" + orderId + "/items";
            Log.d(TAG, "Loading order items from path: " + firebasePath);
            
            FirebaseFirestore.getInstance()
                    .collection("orders")
                    .document(userId)
                    .collection("ordersList")
                    .document(String.valueOf(orderId))
                    .collection("items")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            orderItems.clear();
                            
                            if (task.getResult().isEmpty()) {
                                Log.w(TAG, "No items found in subcollection: " + firebasePath);
                                orderItemsRecyclerView.setVisibility(View.GONE);
                                emptyItemsText.setVisibility(View.VISIBLE);
                                return;
                            }
                            
                            // ✅ Parse items
                            Log.d(TAG, "Found " + task.getResult().size() + " items");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    OrderItemModel item = document.toObject(OrderItemModel.class);
                                    orderItems.add(item);
                                    Log.d(TAG, "Loaded item: " + item.getName() + " (qty: " + item.getQuantity() + ")");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing order item: " + e.getMessage(), e);
                                }
                            }
                            
                            // ✅ Update UI
                            orderItemAdapter.notifyDataSetChanged();
                            orderItemsRecyclerView.setVisibility(View.VISIBLE);
                            emptyItemsText.setVisibility(View.GONE);
                            
                            Log.d(TAG, "Successfully loaded " + orderItems.size() + " items");
                            
                        } else {
                            Log.e(TAG, "Error loading order items from path: " + firebasePath, task.getException());
                            Toast.makeText(OrderDetailsAdminActivity.this, 
                                    "Failed to load order items: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                                    Toast.LENGTH_SHORT).show();
                            
                            orderItemsRecyclerView.setVisibility(View.GONE);
                            emptyItemsText.setVisibility(View.VISIBLE);
                        }
                    });
                    
        } catch (Exception e) {
            Log.e(TAG, "Error in loadOrderItems: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            orderItemsRecyclerView.setVisibility(View.GONE);
            emptyItemsText.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * ✅ FALLBACK METHOD - Dùng collection group query nếu userId null
     * Giữ lại logic cũ để đảm bảo backward compatibility
     */
    private void loadOrderItemsFallback() {
        Log.w(TAG, "Using fallback collection group query method");
        
        FirebaseFirestore.getInstance()
                .collectionGroup("ordersList")
                .whereEqualTo("orderId", orderId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        QueryDocumentSnapshot orderSummaryDoc = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        
                        orderSummaryDoc.getReference().collection("items").get()
                                .addOnCompleteListener(itemsTask -> {
                                    if (itemsTask.isSuccessful() && itemsTask.getResult() != null) {
                                        orderItems.clear();
                                        
                                        if (itemsTask.getResult().isEmpty()) {
                                            Log.w(TAG, "No items found in subcollection (fallback)");
                                            orderItemsRecyclerView.setVisibility(View.GONE);
                                            emptyItemsText.setVisibility(View.VISIBLE);
                                            return;
                                        }
                                        
                                        for (QueryDocumentSnapshot document : itemsTask.getResult()) {
                                            try {
                                                OrderItemModel item = document.toObject(OrderItemModel.class);
                                                orderItems.add(item);
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error parsing order item (fallback): " + e.getMessage(), e);
                                            }
                                        }
                                        orderItemAdapter.notifyDataSetChanged();
                                        orderItemsRecyclerView.setVisibility(View.VISIBLE);
                                        emptyItemsText.setVisibility(View.GONE);
                                    } else {
                                        Log.e(TAG, "Error loading order items (fallback)", itemsTask.getException());
                                        Toast.makeText(OrderDetailsAdminActivity.this, "Failed to load order items", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Log.e(TAG, "Error finding order summary for items (fallback)", task.getException());
                        Toast.makeText(OrderDetailsAdminActivity.this, "Could not find order to load items.", Toast.LENGTH_SHORT).show();
                    }
                });
    }





    /**
     * SET STATUS COLOR
     */
    private void setStatusColor(TextView textView, String status) {
        if (textView == null || status == null) {
            Log.w(TAG, "setStatusColor called with null textView or status");
            return;
        }
        
        try {
            int color;
            switch (status.toLowerCase()) {
                case "pending":
                    color = getResources().getColor(R.color.orange);
                    break;
                case "confirmed":
                    color = getResources().getColor(R.color.blue);
                    break;
                case "shipping":
                    color = getResources().getColor(R.color.purple);
                    break;
                case "delivered":
                    color = getResources().getColor(R.color.green);
                    break;
                case "cancelled":
                    color = getResources().getColor(R.color.red);
                    break;
                default:
                    color = getResources().getColor(R.color.grey);
            }
            textView.setTextColor(color);
        } catch (Exception e) {
            Log.e(TAG, "Error setting status color: " + e.getMessage());
        }
    }

    /**
     *SET PAYMENT METHOD COLOR
     */
    private void setPaymentMethodColor(TextView textView, String paymentMethod) {
        if (textView == null || paymentMethod == null) {
            Log.w(TAG, "setPaymentMethodColor called with null textView or paymentMethod");
            return;
        }
        
        try {
            int color;
            switch (paymentMethod.toUpperCase()) {
                case "MOMO":
                    color = getResources().getColor(R.color.momo_pink);
                    break;
                case "ZALOPAY":
                    color = getResources().getColor(R.color.zalopay_blue);
                    break;
                case "COD":
                    color = getResources().getColor(R.color.cod_green);
                    break;
                default:
                    color = getResources().getColor(R.color.grey);
            }
            textView.setTextColor(color);
        } catch (Exception e) {
            Log.e(TAG, "Error setting payment method color: " + e.getMessage());
        }
    }

    /**
     *SHOW SUCCESS DIALOG
     */
    private void showSuccessDialog(String message) {
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Success!")
                .setContentText(message)
                .setConfirmText("OK")
                .setConfirmClickListener(dialog -> {
                    dialog.dismissWithAnimation();
                    finish(); // Quay lại OrderManagementActivity
                })
                .show();
    }

    /**
     *SHOW ERROR DIALOG
     */
    private void showErrorDialog(String message) {
        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Error!")
                .setContentText(message)
                .setConfirmText("OK")
                .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

