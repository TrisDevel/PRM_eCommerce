package com.example.newEcom.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.model.OrderModel;
import com.example.newEcom.model.PaymentMethod;
import com.example.newEcom.utils.FirebaseUtil;
import com.example.newEcom.utils.MoMoPayment;
import com.example.newEcom.utils.ZaloPayment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class CheckoutActivity extends AppCompatActivity {
    private static final String TAG = "CheckoutActivity";

    // UI Components
    TextView subtotalTextView, deliveryTextView, totalTextView;
    Button checkoutBtn;
    ImageView backBtn;
    EditText nameEditText, emailEditText, phoneEditText, addressEditText, commentEditText;

    // Progress Dialog
    SweetAlertDialog progressDialog;

    // Order Data
    int subTotal;
    int totalAmount;
    int cartItemCount = 0;
    String name, email, phone, address, comment;
    int nextOrderId = 0;
    PaymentMethod selectedPaymentMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        initializeViews();
        setupPriceDisplay();
        setupClickListeners();
    }

    /**
     * ✅ KHỞI TẠO VIEWS
     */
    private void initializeViews() {
        subtotalTextView = findViewById(R.id.subtotalTextView);
        deliveryTextView = findViewById(R.id.deliveryTextView);
        totalTextView = findViewById(R.id.totalTextView);
        checkoutBtn = findViewById(R.id.checkoutBtn);
        backBtn = findViewById(R.id.backBtn);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        commentEditText = findViewById(R.id.commentEditText);

        // Setup progress dialog
        progressDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        progressDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        progressDialog.setTitleText("Loading...");
        progressDialog.setCancelable(false);
    }

    /**
     * ✅ HIỂN THỊ GIÁ TIỀN
     */
    private void setupPriceDisplay() {
        subTotal = getIntent().getIntExtra("price" , 0);
        cartItemCount = getIntent().getIntExtra("itemCount", 0);

        subtotalTextView.setText("₹ " + subTotal);

        if (subTotal >= 5000) {
            deliveryTextView.setText("₹ 0");
            totalAmount = subTotal;
        } else {
            deliveryTextView.setText("₹ 500");
            totalAmount = subTotal + 500;
        }

        totalTextView.setText("₹ " + totalAmount);
    }

    /**
     * ✅ SETUP CLICK LISTENERS
     */
    private void setupClickListeners() {
        checkoutBtn.setOnClickListener(v -> {
            if (validateInputs()) {
                showPaymentMethodDialog();
            }
        });

        backBtn.setOnClickListener(v -> onBackPressed());
    }

    /**
     * ✅ VALIDATE THÔNG TIN NGƯỜI DÙNG
     */
    private boolean validateInputs() {
        name = nameEditText.getText().toString().trim();
        email = emailEditText.getText().toString().trim();
        phone = phoneEditText.getText().toString().trim();
        address = addressEditText.getText().toString().trim();
        comment = commentEditText.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            isValid = false;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Email is not valid");
            isValid = false;
        }

        if (phone.isEmpty()) {
            phoneEditText.setError("Phone Number is required");
            isValid = false;
        } else if (phone.length() != 10) {
            phoneEditText.setError("Phone number must be 10 digits");
            isValid = false;
        }

        if (address.isEmpty()) {
            addressEditText.setError("Address is required");
            isValid = false;
        }

        return isValid;
    }

    /**
     * ✅ HIỂN THỊ DIALOG CHỌN PHƯƠNG THỨC THANH TOÁN
     */
    private void showPaymentMethodDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_payment_method);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        CardView cardMoMo = dialog.findViewById(R.id.cardMoMo);
        CardView cardZaloPay = dialog.findViewById(R.id.cardZaloPay);

        // MoMo Payment
        cardMoMo.setOnClickListener(v -> {
            selectedPaymentMethod = PaymentMethod.MOMO;
            dialog.dismiss();
            processCheckout();
        });

        // ZaloPay Payment
        cardZaloPay.setOnClickListener(v -> {
            selectedPaymentMethod = PaymentMethod.ZALOPAY;
            dialog.dismiss();
            processCheckout();
        });

        dialog.show();
    }

    /**
     * ✅ XỬ LÝ CHECKOUT
     * 1. Lấy nextOrderId từ Firebase
     * 2. Tạo Order tổng
     * 3. Gọi payment gateway
     */
    private void processCheckout() {
        showProgressDialog("Creating order...");

        // Lấy nextOrderId từ Firebase
        FirebaseUtil.getDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();
                Long lastOrderIdL = doc.getLong("lastOrderId");
                int prevOrderId = lastOrderIdL != null ? lastOrderIdL.intValue() : 0;
                nextOrderId = prevOrderId + 1;

                Log.d(TAG, "✅ Got nextOrderId: " + nextOrderId);

                // Tạo Order tổng
                createOrderSummary();

            } else {
                dismissProgressDialog();
                showErrorDialog("Failed to retrieve order information");
                Log.e(TAG, "❌ Failed to get lastOrderId from Firebase");
            }
        });
    }

    /**
     * ✅ TẠO ORDER TỔNG + ORDER ITEMS
     * Lưu tại: orders/{userId}/ordersList/{orderId}/
     * Items tại: orders/{userId}/ordersList/{orderId}/items/{autoId}
     */
    private void createOrderSummary() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            dismissProgressDialog();
            showErrorDialog("User not authenticated");
            return;
        }

        // Tạo OrderModel
        OrderModel order = new OrderModel(
            nextOrderId,
            "Pending",                              // status
            null,                                   // transactionId (chưa có)
            selectedPaymentMethod.name(),           // paymentMethod
            totalAmount,                            // totalAmount
            cartItemCount,                          // itemCount
            Timestamp.now(),                        // timestamp
            name,
            email,
            phone,
            address,
            comment
        );

        // Lưu order tổng vào Firestore
        FirebaseFirestore.getInstance()
            .collection("orders")
            .document(userId)
            .collection("ordersList")
            .document(String.valueOf(nextOrderId))
            .set(order)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Order summary created successfully");
                Log.d(TAG, "   - OrderId: " + nextOrderId);
                Log.d(TAG, "   - Status: Pending");
                Log.d(TAG, "   - Payment: " + selectedPaymentMethod.name());
                Log.d(TAG, "   - Total: " + totalAmount);

                // Tạo order items (subcollection)
                createOrderItems(userId);

            })
            .addOnFailureListener(e -> {
                dismissProgressDialog();
                showErrorDialog("Failed to create order: " + e.getMessage());
                Log.e(TAG, "❌ Failed to create order: " + e.getMessage());
            });
    }

    /**
     * ✅ TẠO ORDER ITEMS (SUBCOLLECTION)
     * Lấy cart items và lưu vào: orders/{userId}/ordersList/{orderId}/items/
     * Note: Stock sẽ được trừ SAU KHI thanh toán thành công (trong PaymentResultActivity)
     */
    private void createOrderItems(String userId) {
        FirebaseUtil.getCartItems().get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                int itemCount = 0;

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Long productIdL = document.getLong("productId");
                    int productId = productIdL != null ? productIdL.intValue() : 0;

                    Long priceL = document.getLong("price");
                    int price = priceL != null ? priceL.intValue() : 0;

                    Long qtyL = document.getLong("quantity");
                    int quantity = qtyL != null ? qtyL.intValue() : 0;

                    // Tạo OrderItemModel
                    OrderItemModel item = new OrderItemModel(
                        nextOrderId,
                        productId,
                        document.getString("name"),
                        document.getString("image"),
                        price,
                        quantity,
                        Timestamp.now(),
                        name,
                        email,
                        phone,
                        address,
                        comment
                    );

                    item.setPaymentMethod(selectedPaymentMethod.name());
                    item.setTransactionId(null);

                    // Lưu vào subcollection items
                    FirebaseFirestore.getInstance()
                        .collection("orders")
                        .document(userId)
                        .collection("ordersList")
                        .document(String.valueOf(nextOrderId))
                        .collection("items")  // ✅ Subcollection
                        .add(item)
                        .addOnSuccessListener(docRef -> 
                            Log.d(TAG, "✅ Order item added: " + docRef.getId())
                        )
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "❌ Failed to add order item: " + e.getMessage())
                        );

                    itemCount++;
                }

                Log.d(TAG, "✅ Created " + itemCount + " order items");

                // Cập nhật lastOrderId
                updateLastOrderId();

            } else {
                dismissProgressDialog();
                showErrorDialog("Failed to retrieve cart items");
                Log.e(TAG, "❌ Failed to get cart items");
            }
        });
    }

    /**
     * ✅ CẬP NHẬT LAST ORDER ID
     */
    private void updateLastOrderId() {
        FirebaseUtil.getDetails()
            .update("lastOrderId", nextOrderId)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Updated lastOrderId to: " + nextOrderId);
                // Gọi payment gateway
                processPayment();
            })
            .addOnFailureListener(e -> {
                dismissProgressDialog();
                showErrorDialog("Failed to update order ID");
                Log.e(TAG, "❌ Failed to update lastOrderId: " + e.getMessage());
            });
    }

    /**
     * ✅ XỬ LÝ THANH TOÁN
     */
    private void processPayment() {
        dismissProgressDialog();

        if (selectedPaymentMethod == PaymentMethod.MOMO) {
            processMoMoPayment();
        } else if (selectedPaymentMethod == PaymentMethod.ZALOPAY) {
            processZaloPayPayment();
        }
    }

    /**
     * ✅ XỬ LÝ THANH TOÁN MOMO
     */
    private void processMoMoPayment() {
        Log.d(TAG, "🟣 Starting MoMo payment with orderId: " + nextOrderId);

        MoMoPayment.createPayment(
            this,
            nextOrderId,
            totalAmount,
            "Thanh toán đơn hàng #" + nextOrderId,
            new MoMoPayment.PaymentCallback() {
                @Override
                public void onSuccess(String transactionId, String orderId) {
                    Log.d(TAG, "✅ MoMo payment success: " + transactionId);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ MoMo payment error: " + error);
                    // ❌ XÓA ORDER NẾU PAYMENT FAILED
                    deleteOrderOnPaymentError();
                    showErrorDialog("MoMo Payment failed: " + error);
                }

                @Override
                public void onPaymentUrlReady(String payUrl) {
                    Log.d(TAG, "🌐 Opening MoMo payment URL");
                    Toast.makeText(CheckoutActivity.this,
                        "Đang mở trang thanh toán MoMo...",
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    /**
     * ✅ XỬ LÝ THANH TOÁN ZALOPAY
     */
    private void processZaloPayPayment() {
        Log.d(TAG, "🔵 Starting ZaloPay payment with orderId: " + nextOrderId);

        ZaloPayment.createPayment(
            this,
            nextOrderId,
            totalAmount,
            "Thanh toán đơn hàng #" + nextOrderId,
            new ZaloPayment.PaymentCallback() {
                @Override
                public void onSuccess(String transactionId, String orderId) {
                    Log.d(TAG, "✅ ZaloPay payment success: " + transactionId);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ ZaloPay payment error: " + error);
                    // ❌ XÓA ORDER NẾU PAYMENT FAILED
                    deleteOrderOnPaymentError();
                    showErrorDialog("ZaloPay Payment failed: " + error);
                }

                @Override
                public void onPaymentUrlReady(String payUrl) {
                    Log.d(TAG, "🌐 Opening ZaloPay payment URL");
                    Toast.makeText(CheckoutActivity.this,
                        "Đang mở trang thanh toán ZaloPay...",
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    /**
     * ❌ XÓA ORDER KHI PAYMENT ERROR
     * Xóa order document và tất cả items trong subcollection
     */
    private void deleteOrderOnPaymentError() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Log.e(TAG, "Cannot delete order: User not authenticated");
            return;
        }

        Log.d(TAG, "🗑️ Deleting failed order: " + nextOrderId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Xóa tất cả items trong subcollection trước
        db.collection("orders")
            .document(userId)
            .collection("ordersList")
            .document(String.valueOf(nextOrderId))
            .collection("items")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                // Xóa từng item
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    doc.getReference().delete()
                        .addOnSuccessListener(aVoid -> 
                            Log.d(TAG, "✅ Deleted order item: " + doc.getId())
                        )
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "❌ Failed to delete item: " + e.getMessage())
                        );
                }

                // Sau khi xóa items, xóa order document
                db.collection("orders")
                    .document(userId)
                    .collection("ordersList")
                    .document(String.valueOf(nextOrderId))
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Deleted order document: " + nextOrderId);
                        
                        // Rollback lastOrderId
                        rollbackLastOrderId();
                    })
                    .addOnFailureListener(e -> 
                        Log.e(TAG, "❌ Failed to delete order: " + e.getMessage())
                    );
            })
            .addOnFailureListener(e -> 
                Log.e(TAG, "❌ Failed to get order items for deletion: " + e.getMessage())
            );
    }

    /**
     * ⏮️ ROLLBACK LAST ORDER ID
     */
    private void rollbackLastOrderId() {
        FirebaseUtil.getDetails()
            .update("lastOrderId", nextOrderId - 1)
            .addOnSuccessListener(aVoid -> 
                Log.d(TAG, "✅ Rolled back lastOrderId to: " + (nextOrderId - 1))
            )
            .addOnFailureListener(e -> 
                Log.e(TAG, "❌ Failed to rollback lastOrderId: " + e.getMessage())
            );
    }

    /**
     * ✅ HIỂN THỊ PROGRESS DIALOG
     */
    private void showProgressDialog(String message) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                progressDialog.setTitleText(message);
                progressDialog.show();
            }
        });
    }

    /**
     * ✅ ẨN PROGRESS DIALOG
     */
    private void dismissProgressDialog() {
        runOnUiThread(() -> {
            try {
                if (progressDialog != null && progressDialog.isShowing() &&
                    !isFinishing() && !isDestroyed()) {
                    progressDialog.dismiss();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog: " + e.getMessage());
            }
        });
    }

    /**
     * ✅ HIỂN THỊ ERROR DIALOG
     */
    private void showErrorDialog(String message) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                new SweetAlertDialog(CheckoutActivity.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Order Failed!")
                    .setContentText(message)
                    .setConfirmClickListener(sweetAlertDialog -> {
                        sweetAlertDialog.dismiss();
                    })
                    .show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
