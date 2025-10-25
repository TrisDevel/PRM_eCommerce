# 🔧 Sửa Race Condition - Lấy OrderID trước khi thanh toán

## ❌ Vấn đề ban đầu

### **Code cũ:**
```java
cardMoMo.setOnClickListener(v -> {
    paymentMethod = PaymentMethod.MOMO;
    processOrder();              // ← Async, lấy prevOrderId từ Firebase
    dialog.dismiss();
    processOrderWithMoMo();      // ← Chạy ngay, nhưng prevOrderId chưa có!
});
```

### **Tại sao bị lỗi?**

```
Timeline:
T0: User click "MoMo"
T1: processOrder() started (async)
    └─ Gọi Firebase.getDetails().get() → Đợi response...
T2: processOrderWithMoMo() started ❌
    └─ Dùng prevOrderId + 1 → NHƯNG prevOrderId = 0 (chưa được set!)
T3: Firebase response về → prevOrderId = 123 ✅ (quá muộn!)
```

**Kết quả:** MoMo nhận `orderId = 1` thay vì `124`!

---

## ✅ Giải pháp mới

### **Code mới:**
```java
cardMoMo.setOnClickListener(v -> {
    paymentMethod = PaymentMethod.MOMO;
    dialog.dismiss();
    
    // ✅ Bước 1: Chuẩn bị order (lấy prevOrderId, tạo order items)
    prepareOrderForPayment(() -> {
        // ✅ Bước 2: Sau khi xong → Gọi MoMo
        processOrderWithMoMo();
    });
});
```

### **Luồng hoạt động:**

```
┌──────────────────────────────────────────────────────────────┐
│ 1. User click "MoMo"                                         │
└──────────────────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────┐
│ 2. prepareOrderForPayment()                                  │
│    - Validate input                                          │
│    - Đọc user info (name, email, phone, address)            │
│    - Khởi tạo lists                                          │
└──────────────────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────┐
│ 3. Lấy prevOrderId từ Firebase                               │
│    FirebaseUtil.getDetails().get()                           │
│    → prevOrderId = 123 ✅                                    │
└──────────────────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────┐
│ 4. createOrderItemsFromCart()                                │
│    - Lấy cart items từ Firebase                             │
│    - Tạo OrderItemModel cho mỗi sản phẩm:                   │
│        orderId = prevOrderId + 1 = 124 ✅                   │
│        status = "Pending"                                    │
│        paymentMethod = "MOMO"                                │
│    - Lưu vào Firebase: orders/{userId}/items/{autoId}       │
└──────────────────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────┐
│ 5. Gọi callback: onReady.run()                               │
└──────────────────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────┐
│ 6. processOrderWithMoMo()                                    │
│    - Tạo MoMo payment với orderId = 124 ✅                  │
│    - extraData = Base64("124") = "MTI0"                     │
│    - Mở browser thanh toán                                   │
└──────────────────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────┐
│ 7. User thanh toán trên MoMo                                 │
└──────────────────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────────┐
│ 8. MoMo redirect về app với orderId = 124 ✅                │
│    PaymentResultActivity decode → orderId = 124              │
│    Update status của order items có orderId = 124            │
└──────────────────────────────────────────────────────────────┘
```

---

## 📝 Chi tiết các method

### **1. prepareOrderForPayment(Runnable onReady)**

**Mục đích:** Lấy `prevOrderId` và tạo order items TRƯỚC khi gọi MoMo

```java
private void prepareOrderForPayment(Runnable onReady) {
    if (!validate()) return;
    
    // 1. Đọc user info
    name = nameEditText.getText().toString().trim();
    email = emailEditText.getText().toString().trim();
    // ...
    
    // 2. Lấy prevOrderId từ Firebase
    FirebaseUtil.getDetails().get().addOnCompleteListener(task -> {
        prevOrderId = doc.getLong("lastOrderId");  // ✅ Lấy được rồi!
        
        // 3. Tạo order items
        createOrderItemsFromCart(() -> {
            // 4. Gọi callback khi xong
            onReady.run();  // ← processOrderWithMoMo() sẽ được gọi ở đây
        });
    });
}
```

**Kết quả:** Khi `onReady.run()` được gọi, `prevOrderId` đã có giá trị chính xác!

---

### **2. createOrderItemsFromCart(Runnable onComplete)**

**Mục đích:** Tạo order items với `orderId` chính xác

```java
private void createOrderItemsFromCart(Runnable onComplete) {
    FirebaseUtil.getCartItems().get().addOnCompleteListener(task -> {
        for (QueryDocumentSnapshot doc : cartSnapshot) {
            // Tạo OrderItemModel
            OrderItemModel item = new OrderItemModel(
                prevOrderId + 1,  // ✅ prevOrderId đã có từ bước trước!
                productId,
                name,
                // ...
                "Pending"
            );
            
            item.setPaymentMethod("MOMO");
            item.setTransactionId(null);
            
            // Lưu vào Firebase
            FirebaseFirestore.getInstance()
                .collection("orders")
                .document(userId)
                .collection("items")
                .add(item);
        }
        
        // Gọi callback khi xong
        onComplete.run();
    });
}
```

---

### **3. processOrderWithMoMo()**

**Mục đích:** Gọi MoMo API với `orderId` đã chính xác

```java
private void processOrderWithMoMo() {
    int totalAmount = subTotal >= 5000 ? subTotal : subTotal + 500;
    
    Log.d(TAG, "🟣 Starting MoMo with orderId: " + (prevOrderId + 1));
    //                                              ↑
    //                                    Đã có giá trị đúng!
    
    MoMoPayment.createPayment(
        this,
        prevOrderId + 1,  // ✅ 124 (chính xác!)
        totalAmount,
        "Thanh toán đơn hàng #" + (prevOrderId + 1),
        callback
    );
}
```

---

## 📊 So sánh

| Aspect | Code cũ ❌ | Code mới ✅ |
|--------|-----------|------------|
| **Timing** | `processOrder()` và `processOrderWithMoMo()` chạy song song | `processOrderWithMoMo()` chỉ chạy sau khi có `prevOrderId` |
| **OrderID** | `prevOrderId = 0` (mặc định) khi gọi MoMo | `prevOrderId = 123` (từ DB) khi gọi MoMo |
| **Consistency** | OrderID trong Firebase ≠ OrderID gửi MoMo | OrderID trong Firebase = OrderID gửi MoMo |
| **Callback** | MoMo không tìm thấy order items | MoMo tìm thấy và update đúng order items |

---

## 🧪 Test

### **Scenario:**
1. Database có `lastOrderId = 100`
2. User có 2 sản phẩm trong cart
3. User chọn thanh toán MoMo

### **Kết quả mong đợi:**

#### **Bước 1: Tạo order items**
```
Firestore: orders/{userId}/items/
  - {autoId1}: { orderId: 101, name: "Product A", status: "Pending" }
  - {autoId2}: { orderId: 101, name: "Product B", status: "Pending" }
```

#### **Bước 2: Gọi MoMo**
```
Request to MoMo:
{
  "orderId": "ORDER_101_1234567890",
  "extraData": "MTAxIA==" // Base64("101")
}
```

#### **Bước 3: MoMo callback**
```
URL: ecommerce://payment-result?extraData=MTAxIA==&resultCode=0&...

PaymentResultActivity:
  - Decode: orderId = 101 ✅
  - Query: orders/{userId}/items WHERE orderId = 101
  - Found: 2 items ✅
  - Update: status = "Confirmed", transactionId = "99999"
```

---

## 🎯 Kết luận

### **Vấn đề đã giải quyết:**
✅ **Race Condition** - `prevOrderId` được lấy TRƯỚC khi gọi MoMo  
✅ **Consistency** - OrderID trong DB khớp với OrderID gửi MoMo  
✅ **Callback** - PaymentResultActivity tìm được order items để update  

### **Pattern sử dụng:**
- **Callback Pattern** để xử lý async operations
- **Sequential Execution** thay vì parallel

### **Áp dụng tương tự cho:**
- ZaloPay payment
- VNPay payment
- Bất kỳ payment gateway nào khác

---

**Tác giả:** PRM_eCommerce Team  
**Ngày sửa:** 2025-10-21


