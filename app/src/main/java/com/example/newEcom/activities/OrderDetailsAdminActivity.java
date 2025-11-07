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
    Spinner statusSpinner;
    Button updateStatusBtn;
    RecyclerView orderItemsRecyclerView;
    androidx.cardview.widget.CardView commentsLayout;

    int orderId;
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
            Log.d(TAG, "🚀 Step 1: Setting content view...");
            setContentView(R.layout.activity_order_details_admin);
            Log.d(TAG, "✅ Step 1: Content view set successfully");

            Log.d(TAG, "🚀 Step 2: Binding views...");
            bindViews();
            Log.d(TAG, "✅ Step 2: Views bound successfully");
            
            Log.d(TAG, "🚀 Step 3: Getting intent data...");
            getIntentData();
            Log.d(TAG, "✅ Step 3: Intent data retrieved successfully");

            // ✅ Validate Intent data
            if (orderId == 0) {
                Log.e(TAG, "❌ Invalid orderId received: " + orderId);
                Toast.makeText(this, "Error: Invalid order data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "🚀 Step 4: Displaying order info...");
            displayOrderInfo();
            Log.d(TAG, "✅ Step 4: Order info displayed successfully");

            Log.d(TAG, "🚀 Step 5: Setting up status spinner...");
            setupStatusSpinner();
            Log.d(TAG, "✅ Step 5: Status spinner setup successfully");

            Log.d(TAG, "🚀 Step 6: Wiring click listeners...");
            wireClicks();
            Log.d(TAG, "✅ Step 6: Click listeners wired successfully");

            Log.d(TAG, "🚀 Step 7: Loading order items...");
            loadOrderItems();
            Log.d(TAG, "✅ Step 7: Order items loading initiated");
            
            Log.d(TAG, "✅✅✅ OrderDetailsAdminActivity initialized successfully for Order #" + orderId);
            
        } catch (Exception e) {
            Log.e(TAG, "❌❌❌ CRASH in onCreate", e);
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
     * ✅ BIND VIEWS
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
            statusSpinner = findViewById(R.id.statusSpinner);
            updateStatusBtn = findViewById(R.id.updateStatusBtn);
            orderItemsRecyclerView = findViewById(R.id.orderItemsRecyclerView);
            emptyItemsText = findViewById(R.id.emptyItemsText);

            // ✅ Validate critical views
            if (backBtn == null || orderIdText == null || statusText == null) {
                throw new RuntimeException("Critical views not found in layout");
            }

            // Setup progress dialog
            progressDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
            progressDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
            progressDialog.setTitleText("Updating...");
            progressDialog.setCancelable(false);
            
            Log.d(TAG, "✅ All views bound successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error binding views: " + e.getMessage(), e);
            throw e; // Re-throw để onCreate catch
        }
    }

    /**
     * ✅ LẤY DỮ LIỆU TỪ INTENT
     */
    private void getIntentData() {
        try {
            orderId = getIntent().getIntExtra("orderId", 0);
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
            
            Log.d(TAG, "📥 Intent data received:");
            Log.d(TAG, "   - orderId: " + orderId);
            Log.d(TAG, "   - customerName: " + customerName);
            Log.d(TAG, "   - status: " + status);
            Log.d(TAG, "   - paymentMethod: " + paymentMethod);
            Log.d(TAG, "   - totalAmount: " + totalAmount);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting intent data: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ HIỂN THỊ THÔNG TIN ORDER
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
     * ✅ SETUP STATUS SPINNER
     */
    private void setupStatusSpinner() {
        // Danh sách status có thể chọn
        String[] statuses = {"Pending", "Confirmed", "Shipping", "Delivered", "Cancelled"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, 
                android.R.layout.simple_spinner_item, 
                statuses
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);
        
        // Set selected item = current status
        if (status != null) {
            int position = adapter.getPosition(status);
            if (position >= 0) {
                statusSpinner.setSelection(position);
            }
        }
    }

    /**
     * ✅ WIRE CLICK LISTENERS
     */
    private void wireClicks() {
        backBtn.setOnClickListener(v -> onBackPressed());
        
        updateStatusBtn.setOnClickListener(v -> {
            String newStatus = statusSpinner.getSelectedItem().toString();
            updateOrderStatus(newStatus);
        });
    }

    /**
     * ✅ LOAD ORDER ITEMS TỪ FIRESTORE
     * Lấy danh sách sản phẩm trong order này
     */
    private void loadOrderItems() {
        try {
            // Setup RecyclerView
            orderItemAdapter = new OrderItemAdapter(orderItems, this);
            orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            orderItemsRecyclerView.setAdapter(orderItemAdapter);
            
            Log.d(TAG, "Loading order items for orderId: " + orderId);
            
            // ✅ Kiểm tra itemCount trước khi query
            if (itemCount == 0) {
                Log.w(TAG, "Order has 0 items. Skipping Firestore query.");
                orderItemsRecyclerView.setVisibility(View.GONE);
                emptyItemsText.setVisibility(View.VISIBLE);
                return;
            }
            
            // Query Firestore để lấy order items
            // ✅ Sửa lỗi: Query collection group "ordersList" để tìm đúng document cha,
            // sau đó mới lấy collection "items" con.
            FirebaseFirestore.getInstance()
                    .collectionGroup("ordersList")
                    .whereEqualTo("orderId", orderId)
                    .limit(1) // Chỉ cần tìm 1 order summary duy nhất
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            // Lấy document cha (order summary)
                            QueryDocumentSnapshot orderSummaryDoc = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                            
                            // Lấy collection "items" từ document cha
                            orderSummaryDoc.getReference().collection("items").get()
                                    .addOnCompleteListener(itemsTask -> {
                                        if (itemsTask.isSuccessful() && itemsTask.getResult() != null) {
                                            orderItems.clear();
                                            
                                            if (itemsTask.getResult().isEmpty()) {
                                                Log.w(TAG, "No items found in subcollection");
                                                orderItemsRecyclerView.setVisibility(View.GONE);
                                                emptyItemsText.setVisibility(View.VISIBLE);
                                                return;
                                            }
                                            
                                            for (QueryDocumentSnapshot document : itemsTask.getResult()) {
                                                try {
                                                    OrderItemModel item = document.toObject(OrderItemModel.class);
                                                    orderItems.add(item);
                                                    Log.d(TAG, "Item: " + item.getName() + " x" + item.getQuantity());
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error parsing order item: " + e.getMessage(), e);
                                                }
                                            }
                                            orderItemAdapter.notifyDataSetChanged();
                                            Log.d(TAG, "Loaded " + orderItems.size() + " order items");
                                        } else {
                                            Log.e(TAG, "Error loading order items", itemsTask.getException());
                                            Toast.makeText(OrderDetailsAdminActivity.this, "Failed to load order items", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Log.e(TAG, "Error finding order summary for items", task.getException());
                            Toast.makeText(OrderDetailsAdminActivity.this, "Could not find order to load items.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    
        } catch (Exception e) {
            Log.e(TAG, "Error in loadOrderItems: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✅ CẬP NHẬT ORDER STATUS
     * Cập nhật status trong Firestore cho TẤT CẢ items có cùng orderId
     */
    private void updateOrderStatus(String newStatus) {
        if (newStatus.equals(status)) {
            Toast.makeText(this, "Status unchanged", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        Log.d(TAG, "Updating order #" + orderId + " status: " + status + " → " + newStatus);

        // ✅ Sửa lỗi: Thống nhất logic query. Luôn tìm order summary trước.
        FirebaseFirestore.getInstance()
                .collectionGroup("ordersList")
                .whereEqualTo("orderId", orderId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        QueryDocumentSnapshot orderSummaryDoc = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        
                        // Bắt đầu cập nhật items và sau đó là summary
                        updateAllSubCollections(orderSummaryDoc, newStatus);
                    } else {
                        progressDialog.dismiss();
                        showErrorDialog("Could not find the order to update.");
                        Log.e(TAG, "Error finding order summary for update", task.getException());
                    }
                });
    }

    /**
     * ✅ Cập nhật tất cả các collection con (items) trước, sau đó mới cập nhật document cha (order summary)
     */
    private void updateAllSubCollections(QueryDocumentSnapshot orderSummaryDoc, String newStatus) {
        // 1. Cập nhật collection con "items"
        orderSummaryDoc.getReference().collection("items").get().addOnCompleteListener(itemsTask -> {
            if (itemsTask.isSuccessful() && itemsTask.getResult() != null) {
                int totalItems = itemsTask.getResult().size();
                if (totalItems == 0) {
                    // Nếu không có item nào, cập nhật summary luôn
                    updateOrderSummary(orderSummaryDoc, newStatus);
                    return;
                }

                int[] updatedCount = {0};
                for (QueryDocumentSnapshot itemDoc : itemsTask.getResult()) {
                    itemDoc.getReference().update("status", newStatus).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            updatedCount[0]++;
                            // Nếu đã update hết tất cả items, thì update summary
                            if (updatedCount[0] == totalItems) {
                                updateOrderSummary(orderSummaryDoc, newStatus);
                            }
                        } else {
                            // Nếu một item bị lỗi, dừng lại và báo lỗi
                            progressDialog.dismiss();
                            showErrorDialog("Failed to update one of the order items.");
                            Log.e(TAG, "Failed to update item: " + itemDoc.getId(), updateTask.getException());
                        }
                    });
                }
            } else {
                progressDialog.dismiss();
                showErrorDialog("Could not read order items to update.");
                Log.e(TAG, "Error getting items sub-collection", itemsTask.getException());
            }
        });
    }

    /**
     * ✅ Cập nhật document cha (order summary)
     */
    private void updateOrderSummary(QueryDocumentSnapshot orderSummaryDoc, String newStatus) {
        orderSummaryDoc.getReference().update("status", newStatus).addOnCompleteListener(summaryTask -> {
            progressDialog.dismiss();
            if (summaryTask.isSuccessful()) {
                Log.d(TAG, "Updated order summary status");
                status = newStatus;
                statusText.setText(newStatus);
                setStatusColor(statusText, newStatus);
                showSuccessDialog("Order status updated to: " + newStatus);
            } else {
                showErrorDialog("Failed to update the main order status.");
                Log.e(TAG, "Failed to update order summary", summaryTask.getException());
            }
        });
    }



    /**
     * ✅ SET STATUS COLOR
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
     * ✅ SET PAYMENT METHOD COLOR
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
     * ✅ SHOW SUCCESS DIALOG
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
     * ✅ SHOW ERROR DIALOG
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

