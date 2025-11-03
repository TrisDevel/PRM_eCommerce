package com.example.newEcom.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newEcom.R;
import com.example.newEcom.utils.FirebaseUtil;
import com.example.newEcom.utils.ZaloPayment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PaymentResultActivity extends AppCompatActivity {
    private static final String TAG = "PaymentResult";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_result);
        
        // Lấy data từ deep link
        Intent intent = getIntent();
        Uri data = intent.getData();
        
        if (data != null) {
            Log.d(TAG, "Received payment callback URL: " + data);
            
            // Phân biệt payment method
            String partnerCode = data.getQueryParameter("partnerCode");
            String zaloStatus = data.getQueryParameter("status");
            String appTransId = data.getQueryParameter("apptransid");
            
            if (partnerCode != null && "MOMO".equals(partnerCode)) {
                // ✅ MOMO PAYMENT CALLBACK
                handleMoMoCallback(data);
            } else if (zaloStatus != null || appTransId != null) {
                // ✅ ZALOPAY PAYMENT CALLBACK
                handleZaloPayCallback(data);
            } else {
                Log.e(TAG, "Unknown payment method");
                Toast.makeText(this, "Unknown payment method", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Log.e(TAG, "No data in intent");
            finish();
        }
    }
    
    /**
     * ✅ XỬ LÝ MOMO CALLBACK
     * URL: ecommerce://payment-result?partnerCode=MOMO&orderId=xxx&resultCode=0&extraData=xxx&transId=xxx
     */
    private void handleMoMoCallback(Uri data) {
        String momoOrderId = data.getQueryParameter("orderId");
        String resultCode = data.getQueryParameter("resultCode");
        String message = data.getQueryParameter("message");
        String transId = data.getQueryParameter("transId");
        String extraData = data.getQueryParameter("extraData");
        
        Log.d(TAG, "MoMo Payment callback:");
        Log.d(TAG, "  - ResultCode: " + resultCode);
        Log.d(TAG, "  - Message: " + message);
        Log.d(TAG, "  - MoMo OrderId: " + momoOrderId);
        Log.d(TAG, "  - TransId: " + transId);
        Log.d(TAG, "  - ExtraData: " + extraData);
        
        // Decode orderId từ extraData
        int orderId = decodeOrderIdFromExtraData(extraData);
        
        if ("0".equals(resultCode)) {
            // ✅ THANH TOÁN THÀNH CÔNG
            updateOrderStatus(orderId, "Confirmed", transId, "MOMO");
            
            Toast.makeText(this, "MoMo: Thanh toán thành công!", Toast.LENGTH_LONG).show();
            
            Intent resultIntent = new Intent(this, MainActivity.class);
            resultIntent.putExtra("paymentSuccess", true);
            resultIntent.putExtra("orderId", orderId);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(resultIntent);
            finish();
            
        } else {
            // ❌ THANH TOÁN THẤT BẠI
            Toast.makeText(this, "MoMo: Thanh toán thất bại - " + message, Toast.LENGTH_LONG).show();
            
            Intent resultIntent = new Intent(this, CheckoutActivity.class);
            resultIntent.putExtra("paymentSuccess", false);
            resultIntent.putExtra("errorMessage", message);
            startActivity(resultIntent);
            finish();
        }
    }
    
    /**
     * ✅ XỬ LÝ ZALOPAY CALLBACK
     * URL: ecommerce://payment-result?orderId=123&paymentMethod=ZALOPAY&amount=300000&appid=2553&apptransid=251028_559132&status=1
     */
    private void handleZaloPayCallback(Uri data) {
        String orderIdStr = data.getQueryParameter("orderId");
        String appTransId = data.getQueryParameter("apptransid");
        String status = data.getQueryParameter("status");
        String amount = data.getQueryParameter("amount");
        String pmcId = data.getQueryParameter("pmcid");
        String bankCode = data.getQueryParameter("bankcode");
        
        Log.d(TAG, "ZaloPay Payment callback:");
        Log.d(TAG, "  - OrderId: " + orderIdStr);
        Log.d(TAG, "  - AppTransId: " + appTransId);
        Log.d(TAG, "  - Status: " + status);
        Log.d(TAG, "  - Amount: " + amount);
        Log.d(TAG, "  - PmcId: " + pmcId);
        Log.d(TAG, "  - BankCode: " + bankCode);
        
        if (orderIdStr == null || appTransId == null || status == null) {
            Log.e(TAG, "Missing required ZaloPay parameters");
            Toast.makeText(this, "Lỗi: Thiếu thông tin callback ZaloPay", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        int orderId = Integer.parseInt(orderIdStr);
        
        if ("1".equals(status)) {
            // ✅ THANH TOÁN THÀNH CÔNG (status = 1)
            updateOrderStatus(orderId, "Confirmed", appTransId, "ZALOPAY");
            
            Toast.makeText(this, "ZaloPay: Thanh toán thành công!", Toast.LENGTH_LONG).show();
            
            Intent resultIntent = new Intent(this, MainActivity.class);
            resultIntent.putExtra("paymentSuccess", true);
            resultIntent.putExtra("orderId", orderId);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(resultIntent);
            finish();
            
        } else {
            // ❌ THANH TOÁN THẤT BẠI (status != 1)
            Toast.makeText(this, "ZaloPay: Thanh toán thất bại (status=" + status + ")", Toast.LENGTH_LONG).show();
            
            Intent resultIntent = new Intent(this, CheckoutActivity.class);
            resultIntent.putExtra("paymentSuccess", false);
            resultIntent.putExtra("errorMessage", "Payment failed");
            startActivity(resultIntent);
            finish();
        }
    }
    
    /**
     * ✅ DECODE ORDER ID TỪ EXTRA DATA (CHO MOMO)
     */
    private int decodeOrderIdFromExtraData(String extraData) {
        if (extraData == null || extraData.isEmpty()) {
            Log.e(TAG, "ExtraData is empty");
            return 0;
        }
        
        try {
            byte[] decodedBytes = Base64.decode(extraData, Base64.NO_WRAP);
            String decodedString = new String(decodedBytes, "UTF-8");
            int orderId = Integer.parseInt(decodedString);
            Log.d(TAG, "Decoded OrderId: " + orderId);
            return orderId;
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode extraData: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * ✅ CẬP NHẬT STATUS CỦA ORDER TỔNG
     * Chỉ update document order chính tại: orders/{userId}/ordersList/{orderId}
     */
    private void updateOrderStatus(int orderId, String newStatus, String transactionId, String paymentMethod) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Log.e(TAG, "User not authenticated");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Update document order tổng
        db.collection("orders")
            .document(userId)
            .collection("ordersList")
            .document(String.valueOf(orderId))
            .update(
                "status", newStatus,
                "transactionId", transactionId,
                "paymentMethod", paymentMethod
            )
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Order updated successfully:");
                Log.d(TAG, "   - OrderId: " + orderId);
                Log.d(TAG, "   - Status: " + newStatus);
                Log.d(TAG, "   - TransactionId: " + transactionId);
                Log.d(TAG, "   - PaymentMethod: " + paymentMethod);
                
                // ✅ SAU KHI UPDATE ORDER → TRỪ STOCK VÀ XÓA CART
                updateProductStockAndClearCart(userId, orderId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to update order: " + e.getMessage());
                Log.w(TAG, "Order document might not exist, this should not happen in normal flow");
            });
    }

    /**
     * ✅ TRỪ STOCK CÁC PRODUCTS VÀ XÓA CART
     */
    private void updateProductStockAndClearCart(String userId, int orderId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Lấy tất cả items của order
        db.collection("orders")
            .document(userId)
            .collection("ordersList")
            .document(String.valueOf(orderId))
            .collection("items")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "📦 Updating stock for " + querySnapshot.size() + " items");

                for (DocumentSnapshot itemDoc : querySnapshot.getDocuments()) {
                    Long productIdL = itemDoc.getLong("productId");
                    Long quantityL = itemDoc.getLong("quantity");

                    if (productIdL != null && quantityL != null) {
                        int productId = productIdL.intValue();
                        int quantity = quantityL.intValue();

                        // Trừ stock của product
                        updateProductStock(productId, quantity);
                    }
                }

                // Xóa cart items
                clearCart(userId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to get order items: " + e.getMessage());
            });
    }

    /**
     * ✅ TRỪ STOCK CỦA PRODUCT
     */
    private void updateProductStock(int productId, int quantityToSubtract) {
        FirebaseUtil.getProducts()
            .whereEqualTo("productId", productId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    DocumentSnapshot productDoc = querySnapshot.getDocuments().get(0);
                    Long currentStockL = productDoc.getLong("stock");
                    
                    if (currentStockL != null) {
                        int currentStock = currentStockL.intValue();
                        int newStock = currentStock - quantityToSubtract;

                        // Update stock
                        productDoc.getReference()
                            .update("stock", newStock)
                            .addOnSuccessListener(aVoid -> 
                                Log.d(TAG, "✅ Product " + productId + " stock: " + currentStock + " → " + newStock)
                            )
                            .addOnFailureListener(e -> 
                                Log.e(TAG, "❌ Failed to update stock for product " + productId + ": " + e.getMessage())
                            );
                    }
                } else {
                    Log.w(TAG, "⚠️ Product not found: " + productId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to query product " + productId + ": " + e.getMessage());
            });
    }

    /**
     * ✅ XÓA TẤT CẢ ITEMS TRONG CART
     */
    private void clearCart(String userId) {
        FirebaseUtil.getCartItems()
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "🛒 Clearing cart: " + querySnapshot.size() + " items");

                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    doc.getReference().delete()
                        .addOnSuccessListener(aVoid -> 
                            Log.d(TAG, "✅ Deleted cart item: " + doc.getId())
                        )
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "❌ Failed to delete cart item: " + e.getMessage())
                        );
                }

                Log.d(TAG, "✅ Cart cleared successfully");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to clear cart: " + e.getMessage());
            });
    }
}
