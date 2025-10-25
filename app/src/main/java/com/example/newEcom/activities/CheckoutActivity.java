package com.example.newEcom.activities;

import androidx.annotation.NonNull;
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
import com.example.newEcom.model.PaymentMethod;
import com.example.newEcom.utils.EmailSender;
import com.example.newEcom.utils.FirebaseUtil;
import com.example.newEcom.utils.MoMoPayment;
import com.example.newEcom.utils.ZaloPayment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class CheckoutActivity extends AppCompatActivity {
    private static final String TAG = "CheckoutActivity";
    private PaymentMethod paymentMethod;
    private static final int MOMO_REQUEST_CODE = 1001;

    TextView subtotalTextView, deliveryTextView, totalTextView, stockErrorTextView;
    Button checkoutBtn;
    ImageView backBtn;

    SweetAlertDialog dialog;

    int subTotal, count = 0;
    boolean adequateStock = true;

    EditText nameEditText, emailEditText, phoneEditText, addressEditText, commentEditText;
    String name, email, phone, address, comment;

    int prevOrderId = 0;
    int countOfOrderedItems = 0;
    int priceOfOrders = 0;

    // Lists (không dùng trick with final array)
    List<String> productDocId;
    List<Integer> oldStock;
    List<Integer> quan;
    List<String> lessStock;

    List<String> cartDocument;
    List<String> productName;
    List<Integer> productPrice;
    List<Integer> productQuantity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        subtotalTextView = findViewById(R.id.subtotalTextView);
        deliveryTextView = findViewById(R.id.deliveryTextView);
        totalTextView = findViewById(R.id.totalTextView);
        stockErrorTextView = findViewById(R.id.stockErrorTextView);
        checkoutBtn = findViewById(R.id.checkoutBtn);
        backBtn = findViewById(R.id.backBtn);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        commentEditText = findViewById(R.id.commentEditText);

        subTotal = getIntent().getIntExtra("price", 10000);
        subtotalTextView.setText("₹ " + subTotal);
        if (subTotal >= 5000) {
            deliveryTextView.setText("₹ 0");
            totalTextView.setText("₹ " + subTotal);
        } else {
            deliveryTextView.setText("₹ 500");
            totalTextView.setText("₹ " + (subTotal + 500));
        }

        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Loading...");
        dialog.setCancelable(false);

        checkoutBtn.setOnClickListener(v -> {
            showPaymentMethodDialog();
        });

        backBtn.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Bắt đầu quy trình xử lý đơn hàng
     * - validate
     * - lấy details (prevOrderId, countOfOrderedItems, priceOfOrders)
     * - lấy cart items, lưu order items vào collection "orders"
     * - kiểm tra stock cho từng sản phẩm (song song nhưng đếm completed)
     * - nếu đủ: update details + update stock + xóa cart items (chờ tất cả task hoàn tất)
     * - gửi email (ở background) và show success dialog
     */
    private void showPaymentMethodDialog() {
        // Hiển thị dialog chọn phương thức thanh toán
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_payment_method);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        CardView cardMoMo = dialog.findViewById(R.id.cardMoMo);
        CardView cardZaloPay = dialog.findViewById(R.id.cardZaloPay);

        // MoMo
        cardMoMo.setOnClickListener(v -> {
            paymentMethod = PaymentMethod.MOMO;
            dialog.dismiss();
            
            // ✅ SỬ DỤNG LẠI processOrder() với callback
            processOrder(() -> {
                // Callback sau khi đã lấy prevOrderId và tạo order items
                processOrderWithMoMo();
            });
        });

        // ZaloPay
        cardZaloPay.setOnClickListener(v -> {
            paymentMethod = PaymentMethod.ZALOPAY;
            dialog.dismiss();
            processOrder(() -> {
                // Callback sau khi đã lấy prevOrderId và tạo order items
                processOrderWithZaloPay();
            });
        });

        dialog.show();
    }

    /**
     * ✅ XỬ LÝ THANH TOÁN MOMO (đã có prevOrderId từ processOrder)
     */
    private void processOrderWithZaloPay(){
        int totalAmount = subTotal >= 5000 ? subTotal : subTotal + 500;

        ZaloPayment.createPayment(this , prevOrderId + 1, totalAmount,
                "Thanh toán đơn hàng #" + (prevOrderId + 1),
                new ZaloPayment.PaymentCallback() {
                    @Override
                    public void onSuccess(String transactionId, String orderId) {
                        // Thanh toán thành công qua callback từ PaymentResultActivity
                        Log.d(TAG, "✅ ZaloPay payment success: " + transactionId);
                        dismissDialogSafely();
                    }

                    @Override
                    public void onError(String error) {
                        dismissDialogSafely();
                        Log.e(TAG, "❌ ZaloPay payment error: " + error);
                        showErrorDialog("Payment failed: " + error);
                    }

                    @Override
                    public void onPaymentUrlReady(String payUrl) {
                        dismissDialogSafely();
                        Log.d(TAG, "🌐 Opening ZaloPay payment URL");
                        Toast.makeText(CheckoutActivity.this,
                                "Đang mở trang thanh toán ZaloPay...",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    private void processOrderWithMoMo() {
        int totalAmount = subTotal >= 5000 ? subTotal : subTotal + 500;
        
        Log.d(TAG, "🟣 Starting MoMo payment with orderId: " + (prevOrderId + 1));

        MoMoPayment.createPayment(
                this,
                prevOrderId + 1,  // ✅ Đã có orderId từ processOrder()
                totalAmount,
                "Thanh toán đơn hàng #" + (prevOrderId + 1),
                new MoMoPayment.PaymentCallback() {
                    @Override
                    public void onSuccess(String transactionId, String orderId) {
                        // Thanh toán thành công qua callback từ PaymentResultActivity
                        Log.d(TAG, "✅ MoMo payment success: " + transactionId);
                        dismissDialogSafely();
                    }

                    @Override
                    public void onError(String error) {
                        dismissDialogSafely();
                        Log.e(TAG, "❌ MoMo payment error: " + error);
                        showErrorDialog("Payment failed: " + error);
                    }

                    @Override
                    public void onPaymentUrlReady(String payUrl) {
                        dismissDialogSafely();
                        Log.d(TAG, "🌐 Opening MoMo payment URL");
                        Toast.makeText(CheckoutActivity.this, 
                            "Đang mở trang thanh toán MoMo...", 
                            Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    
    /**
     * Bắt đầu quy trình xử lý đơn hàng (COD payment - không có callback)
     */
    private void processOrder() {
        processOrder(null); // Gọi overload method với callback = null
    }
    
    /**
     * Bắt đầu quy trình xử lý đơn hàng
     * - validate
     * - lấy details (prevOrderId, countOfOrderedItems, priceOfOrders)
     * - lấy cart items, lưu order items vào collection "orders"
     * - kiểm tra stock cho từng sản phẩm (song song nhưng đếm completed)
     * - nếu đủ: update details + update stock + xóa cart items (chờ tất cả task hoàn tất)
     * - gửi email (ở background) và show success dialog
     * 
     * @param onOrderCreated Callback được gọi sau khi tạo xong order items (có prevOrderId).
     *                       Nếu null, sẽ tiếp tục check stock và update Firebase (COD payment).
     *                       Nếu không null, gọi callback và return (MoMo/ZaloPay payment).
     */
    private void processOrder(Runnable onOrderCreated) {
        if (!validate()) return;

        // đọc thông tin người dùng
        name = nameEditText.getText().toString().trim();
        email = emailEditText.getText().toString().trim();
        phone = phoneEditText.getText().toString().trim();
        address = addressEditText.getText().toString().trim();
        comment = commentEditText.getText().toString().trim();

        // khởi tạo lists
        productDocId = new ArrayList<>();
        oldStock = new ArrayList<>();
        quan = new ArrayList<>();
        lessStock = new ArrayList<>();

        cartDocument = new ArrayList<>();
        productName = new ArrayList<>();
        productPrice = new ArrayList<>();
        productQuantity = new ArrayList<>();

        // show loading
        if (!isFinishing() && !isDestroyed()) dialog.show();

        // 1) lấy details trước
        FirebaseUtil.getDetails().get().addOnCompleteListener(taskDetails -> {
            if (taskDetails.isSuccessful() && taskDetails.getResult() != null) {
                DocumentSnapshot doc = taskDetails.getResult();
                Long lastOrderIdL = doc.getLong("lastOrderId");
                Long countItemsL = doc.getLong("countOfOrderedItems");
                Long priceOrdersL = doc.getLong("priceOfOrders");

                prevOrderId = lastOrderIdL != null ? lastOrderIdL.intValue() : 0;
                countOfOrderedItems = countItemsL != null ? countItemsL.intValue() : 0;
                priceOfOrders = priceOrdersL != null ? priceOrdersL.intValue() : 0;

                // 2) lấy cart items
                FirebaseUtil.getCartItems().get().addOnCompleteListener(taskCart -> {
                    if (taskCart.isSuccessful() && taskCart.getResult() != null) {
                        QuerySnapshot cartSnapshot = taskCart.getResult();
                        if (cartSnapshot.isEmpty()) {
                            // giỏ hàng trống
                            dismissDialogSafely();
                            showErrorDialog("Your cart is empty.");
                            return;
                        }

                        count = 0;
                        // sẽ dùng atomic để đếm các product-check hoàn tất
                        AtomicInteger completedChecks = new AtomicInteger(0);
                        int totlIatems = cartSnapshot.size();
                        adequateStock = true;

                        // Lưu tạm thông tin cart items để xử lý
                        List<CartItemInfo> cartItems = new ArrayList<>();

                        for (QueryDocumentSnapshot document : cartSnapshot) {
                            count++;
                            cartDocument.add(document.getId());

                            String prodName = document.getString("name");
                            productName.add(prodName != null ? prodName : "Unknown");

                            Long priceL = document.getLong("price");
                            int price = priceL != null ? priceL.intValue() : 0;
                            productPrice.add(price);

                            Long qtyL = document.getLong("quantity");
                            int quantity = qtyL != null ? qtyL.intValue() : 0;
                            productQuantity.add(quantity);

                            count++; // Not necessary but original code increments count per item; keep consistent if needed
                            // tạo OrderItemModel và lưu vào Firestore orders collection
                            Long productIdL = document.getLong("productId");
                            int productId = productIdL != null ? productIdL.intValue() : 0;

                            OrderItemModel item = new OrderItemModel(prevOrderId + 1,
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
                                    comment,
                                    "Pending");
                            
                            // ✅ Set payment method mặc định (sẽ được update sau khi thanh toán)
                            item.setPaymentMethod(paymentMethod != null ? paymentMethod.name() : "PENDING");
                            item.setTransactionId(null); // Chưa có transaction

                            FirebaseFirestore.getInstance()
                                    .collection("orders")
                                    .document(FirebaseAuth.getInstance().getUid())
                                    .collection("items")
                                    .add(item)
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to add order item: " + e.getMessage()));

                            // Lưu thông tin tạm để check stock
                            cartItems.add(new CartItemInfo(productId, document.getString("name"), quantity));
                        }

                        // nếu không có cart items thì thoát
                        if (cartItems.isEmpty()) {
                            dismissDialogSafely();
                            showErrorDialog("No items in cart.");
                            return;
                        }

                        // ✅ NẾU CÓ CALLBACK (thanh toán MoMo/ZaloPay) → GỌI CALLBACK VÀ RETURN
                        if (onOrderCreated != null) {
                            dismissDialogSafely();
                            Log.d(TAG, "✅ Order items created with orderId: " + (prevOrderId + 1) + ", calling callback");
                            onOrderCreated.run(); // → processOrderWithMoMo() sẽ được gọi
                            return; // Không check stock ở đây, sẽ check sau khi thanh toán
                        }

                        // 3) kiểm tra stock cho từng cart item (chỉ cho COD payment)
                        for (CartItemInfo ci : cartItems) {
                            // Nếu productId = 0 (không hợp lệ), coi là lỗi
                            if (ci.productId == 0) {
                                adequateStock = false;
                                lessStock.add(ci.name != null ? ci.name : "Unknown");
                                oldStock.add(0);
                                quan.add(ci.quantity);
                                productDocId.add(""); // placeholder
                                int done = completedChecks.incrementAndGet();
                                if (done == cartItems.size()) {
                                    onAllProductChecksCompleted();
                                }
                                continue;
                            }

                            FirebaseUtil.getProducts()
                                    .whereEqualTo("productId", ci.productId)
                                    .get()
                                    .addOnCompleteListener(taskProd -> {
                                        if (taskProd.isSuccessful() && taskProd.getResult() != null && !taskProd.getResult().isEmpty()) {
                                            DocumentSnapshot prodDoc = taskProd.getResult().getDocuments().get(0);
                                            String docId = prodDoc.getId();
                                            Long stockL = prodDoc.getLong("stock");
                                            int stock = stockL != null ? stockL.intValue() : 0;

                                            productDocId.add(docId);
                                            oldStock.add(stock);
                                            quan.add(ci.quantity);

                                            if (stock < ci.quantity) {
                                                adequateStock = false;
                                                lessStock.add(ci.name != null ? ci.name : "Unknown");
                                            }
                                        } else {
                                            // product not found or error
                                            Log.e(TAG, "Product lookup failed for productId=" + ci.productId);
                                            adequateStock = false;
                                            lessStock.add(ci.name != null ? ci.name : "Unknown");
                                            productDocId.add(""); // placeholder
                                            oldStock.add(0);
                                            quan.add(ci.quantity);
                                        }

                                        int done = completedChecks.incrementAndGet();
                                        if (done == cartItems.size()) {
                                            onAllProductChecksCompleted();
                                        }
                                    });
                        }

                    } else {
                        // Lỗi lấy cart
                        dismissDialogSafely();
                        showErrorDialog("Something went wrong retrieving cart items.");
                    }
                });

            } else {
                // Lỗi lấy details
                dismissDialogSafely();
                showErrorDialog("Something went wrong retrieving app details.");
            }
        });
    }

    private void onAllProductChecksCompleted() {
        // tất cả product đã được check
        dismissDialogSafely();

        FirebaseUtil.getCartItems().get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Long productIdL = document.getLong("productId");
                    int productId = productIdL != null ? productIdL.intValue() : 0;

                    Long priceL = document.getLong("price");
                    int price = priceL != null ? priceL.intValue() : 0;

                    Long qtyL = document.getLong("quantity");
                    int quantity = qtyL != null ? qtyL.intValue() : 0;

                    OrderItemModel item = new OrderItemModel(
                            prevOrderId + 1,
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
                            comment,
                            "Pending"
                    );

                    FirebaseFirestore.getInstance()
                            .collection("orders")
                            .document(FirebaseAuth.getInstance().getUid())
                            .collection("items")
                            .add(item)
                            .addOnSuccessListener(docRef ->
                                    Log.d(TAG, "✅ Order item saved: " + docRef.getId())
                            )
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "❌ Failed to save order item: " + e.getMessage())
                            );
                }
            }
        });

        // Nếu đủ stock -> thực hiện cập nhật lên Firebase
        changeToFirebase();
    }

    private void changeToFirebase() {
        // Chuẩn bị dữ liệu cập nhật chi tiết đơn hàng
        Map<String, Object> detailsUpdate = new HashMap<>();
        detailsUpdate.put("lastOrderId", prevOrderId + 1);
        detailsUpdate.put("countOfOrderedItems", countOfOrderedItems + count);
        detailsUpdate.put("priceOfOrders", priceOfOrders + subTotal);

        List<Task<?>> tasks = new ArrayList<>();

        // ✅ Cập nhật hoặc tạo mới document "dashboard/details"
        Task<Void> detailsTask = FirebaseUtil.getDetails()
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        // Nếu document đã tồn tại → update
                        return FirebaseUtil.getDetails().update(detailsUpdate);
                    } else {
                        // Nếu chưa có → tạo mới
                        return FirebaseUtil.getDetails().set(detailsUpdate);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi khi cập nhật hoặc tạo details: " + e.getMessage()));
        tasks.add(detailsTask);

        // ✅ Cập nhật tồn kho sản phẩm
        for (int i = 0; i < productDocId.size(); i++) {
            String prodDocId = productDocId.get(i);
            if (prodDocId == null || prodDocId.isEmpty()) continue;

            int newStock = oldStock.get(i) - quan.get(i);
            Task<Void> updateStockTask = FirebaseUtil.getProducts()
                    .document(prodDocId)
                    .update("stock", newStock)
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi khi cập nhật tồn kho sản phẩm: " + e.getMessage()));
            tasks.add(updateStockTask);
        }

        // ✅ Xóa sản phẩm trong giỏ hàng
        for (String docId : cartDocument) {
            if (docId == null || docId.isEmpty()) continue;

            Task<Void> deleteCartTask = FirebaseUtil.getCartItems()
                    .document(docId)
                    .delete()
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi khi xóa sản phẩm trong giỏ hàng: " + e.getMessage()));
            tasks.add(deleteCartTask);
        }

        // ✅ Chờ tất cả tác vụ hoàn tất
//        Tasks.whenAll(tasks)
//                .addOnCompleteListener(allTasks -> {
//                    // Gửi email xác nhận
//                    sendOrderConfirmationEmail();
//
//                    // Hiển thị thông báo thành công
//                    runOnUiThread(() -> {
//                        if (!isFinishing() && !isDestroyed()) {
//                            new SweetAlertDialog(CheckoutActivity.this, SweetAlertDialog.SUCCESS_TYPE)
//                                    .setTitleText("Order placed Successfully!")
//                                    .setContentText("You will shortly receive an email confirming the order details.")
//                                    .setConfirmClickListener(dialog -> {
//                                        Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
//                                        intent.putExtra("orderPlaced", true);
//                                        startActivity(intent);
//                                        finish();
//                                    })
//                                    .show();
//                        }
//                    });
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(TAG, "❌ Một số tác vụ thất bại: " + e.getMessage());
//                    dismissDialogSafely();
//                    showErrorDialog("Something went wrong while finalizing your order. Please try again.");
//                });
    }


    // ----------------------------
// Hàm con để gửi email xác nhận
// ----------------------------
    private void sendOrderConfirmationEmail() {
        String subject = "Your Order is successfully placed with ShopEase!";
        StringBuilder body = new StringBuilder();

        body.append("Dear ").append(name).append(",\n\n")
                .append("Thank you for placing your order with ShopEase. We are excited to inform you that your order has been successfully placed.\n\n")
                .append("Order Details:\n")
                .append("------------------------------------------------------------\n")
                .append(String.format("%-40s %-10s %-10s\n", "Product Name", "Quantity", "Price"))
                .append("------------------------------------------------------------\n");

        for (int i = 0; i < productName.size(); i++) {
            String pName = productName.get(i);
            int pQty = i < productQuantity.size() ? productQuantity.get(i) : 0;
            int pPrice = i < productPrice.size() ? productPrice.get(i) : 0;
            body.append(String.format("%-40s %-10d ₹%-10d\n", pName, pQty, pPrice));
        }

        body.append("------------------------------------------------------------\n")
                .append(String.format("%-60s ₹%-10d\n", "Total:", subTotal))
                .append("------------------------------------------------------------\n\n")
                .append("Thank you for choosing our service. If you have any questions or concerns, feel free to contact our customer support.\n\n")
                .append("Best Regards,\n")
                .append("ShopEase Team");

        // Gửi email trong luồng riêng
        new Thread(() -> {
            try {
                EmailSender emailSender = new EmailSender(subject, body.toString(), email);
                emailSender.sendEmail();
                Log.i(TAG, "✅ Email xác nhận đã được gửi thành công.");
            } catch (Exception e) {
                Log.e(TAG, "❌ Gửi email thất bại: " + e.getMessage());
            }
        }).start();
    }


    private void dismissDialogSafely() {
        runOnUiThread(() -> {
            try {
                if (dialog != null && dialog.isShowing() && !isFinishing() && !isDestroyed()) {
                    dialog.dismiss();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog: " + e.getMessage());
            }
        });
    }

    private void showErrorDialog(String message) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                new SweetAlertDialog(CheckoutActivity.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Order Failed!")
                        .setContentText(message)
                        .setConfirmClickListener(sweetAlertDialog -> {
                            Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }).show();
            }
        });
    }

    private boolean validate() {
        boolean isValid = true;
        if (nameEditText.getText().toString().trim().length() == 0) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        if (emailEditText.getText().toString().trim().length() == 0) {
            emailEditText.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailEditText.getText().toString().trim()).matches()) {
            emailEditText.setError("Email is not valid");
            isValid = false;
        }
        if (phoneEditText.getText().toString().trim().length() == 0) {
            phoneEditText.setError("Phone Number is required");
            isValid = false;
        } else if (phoneEditText.getText().toString().trim().length() != 10) {
            phoneEditText.setError("Phone number is not valid");
            isValid = false;
        }
        if (addressEditText.getText().toString().trim().length() == 0) {
            addressEditText.setError("Address is required");
            isValid = false;
        }
        return isValid;
    }

    // Class helper để lưu tạm thông tin cart item
    private static class CartItemInfo {
        int productId;
        String name;
        int quantity;

        CartItemInfo(int productId, String name, int quantity) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
