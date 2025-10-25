package com.example.newEcom.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newEcom.R;
import com.google.firebase.auth.FirebaseAuth;
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
            // Parse parameters từ URL
            // URL format: ecommerce://payment-result?partnerCode=MOMO&orderId=ORDER_123_xxx&resultCode=0&extraData=xxx&...
            
            String partnerCode = data.getQueryParameter("partnerCode");
            String momoOrderId = data.getQueryParameter("orderId");
            String resultCode = data.getQueryParameter("resultCode");
            String message = data.getQueryParameter("message");
            String transId = data.getQueryParameter("transId");
            String amount = data.getQueryParameter("amount");
            String extraData = data.getQueryParameter("extraData");
            
            Log.d(TAG, "Payment callback:");
            Log.d(TAG, "  - ResultCode: " + resultCode);
            Log.d(TAG, "  - Message: " + message);
            Log.d(TAG, "  - MoMo OrderId: " + momoOrderId);
            Log.d(TAG, "  - TransId: " + transId);
            Log.d(TAG, "  - ExtraData: " + extraData);
            
            // ✅ DECODE ORDER ID TỪ EXTRA DATA
            int orderId = decodeOrderIdFromExtraData(extraData);
            
            if ("0".equals(resultCode)) {
                // ✅ THANH TOÁN THÀNH CÔNG → CẬP NHẬT STATUS
                updateOrderStatus(orderId, "Confirmed", transId);
                
                Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show();
                
                // Quay về MainActivity
                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.putExtra("paymentSuccess", true);
                resultIntent.putExtra("orderId", orderId);
                resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(resultIntent);
                finish();
                
            } else {
                // ❌ THANH TOÁN THẤT BẠI → GIỮ NGUYÊN STATUS PENDING
                Toast.makeText(this, "Thanh toán thất bại: " + message, Toast.LENGTH_LONG).show();
                
                Intent resultIntent = new Intent(this, CheckoutActivity.class);
                resultIntent.putExtra("paymentSuccess", false);
                resultIntent.putExtra("errorMessage", message);
                startActivity(resultIntent);
                finish();
            }
        } else {
            Log.e(TAG, "No data in intent");
            finish();
        }
    }
    
    /**
     * ✅ DECODE ORDER ID TỪ EXTRA DATA
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
     * ✅ CẬP NHẬT STATUS CỦA TẤT CẢ ORDER ITEMS CÓ ORDERID
     */
    private void updateOrderStatus(int orderId, String newStatus, String transactionId) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Log.e(TAG, "User not authenticated");
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Query tất cả order items có orderId này
        db.collection("orders")
            .document(userId)
            .collection("items")
            .whereEqualTo("orderId", orderId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    Log.w(TAG, "No order items found with orderId: " + orderId);
                    return;
                }
                
                // Cập nhật status cho tất cả items
                querySnapshot.forEach(documentSnapshot -> {
                    db.collection("orders")
                        .document(userId)
                        .collection("items")
                        .document(documentSnapshot.getId())
                        .update(
                            "status", newStatus,
                            "transactionId", transactionId,
                            "paymentMethod", "MOMO"
                        )
                        .addOnSuccessListener(aVoid -> 
                            Log.d(TAG, "✅ Updated order item: " + documentSnapshot.getId() + " to " + newStatus)
                        )
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "❌ Failed to update order item: " + e.getMessage())
                        );
                });
                
                Log.d(TAG, "✅ Updated " + querySnapshot.size() + " order items to status: " + newStatus);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to query order items: " + e.getMessage());
            });
    }
}
