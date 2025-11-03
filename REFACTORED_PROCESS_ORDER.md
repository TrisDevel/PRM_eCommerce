# ♻️ Refactored: Dùng lại processOrder() thay vì tạo hàm mới

## ❌ Vấn đề: Duplicate Code

### **Code cũ (duplicate):**

```java
// Hàm 1: prepareOrderForPayment() - 60 dòng
private void prepareOrderForPayment(Runnable onReady) {
    // Lấy prevOrderId
    FirebaseUtil.getDetails().get()...
    
    // Gọi createOrderItemsFromCart()
    createOrderItemsFromCart(() -> {
        onReady.run();
    });
}

// Hàm 2: createOrderItemsFromCart() - 50 dòng
private void createOrderItemsFromCart(Runnable onComplete) {
    // Lấy cart items
    FirebaseUtil.getCartItems().get()...
    
    // Tạo OrderItemModel
    for (doc : cartSnapshot) {
        OrderItemModel item = new OrderItemModel(...);
        FirebaseFirestore.getInstance()...add(item);
    }
    
    onComplete.run();
}

// Hàm 3: processOrder() - 100 dòng (đã có sẵn!)
private void processOrder() {
    // ✅ Lấy prevOrderId (giống prepareOrderForPayment)
    FirebaseUtil.getDetails().get()...
    
    // ✅ Lấy cart items (giống createOrderItemsFromCart)
    FirebaseUtil.getCartItems().get()...
    
    // ✅ Tạo OrderItemModel (giống createOrderItemsFromCart)
    for (doc : cartSnapshot) {
        OrderItemModel item = new OrderItemModel(...);
        FirebaseFirestore.getInstance()...add(item);
    }
    
    // + Check stock
    // + Update details
    // + Delete cart
}
```

**Vấn đề:**
- ❌ **110 dòng code duplicate** (prepareOrderForPayment + createOrderItemsFromCart)
- ❌ Phải maintain 2 nơi: nếu sửa logic tạo order → phải sửa cả 2
- ❌ Khó debug khi có lỗi
- ❌ Vi phạm DRY principle (Don't Repeat Yourself)

---

## ✅ Giải pháp: Dùng lại processOrder()

### **Code mới (refactored):**

```java
// ✅ Chỉ cần 1 hàm: processOrder() với optional callback
private void processOrder(Runnable onOrderCreated) {
    // 1. Lấy prevOrderId
    FirebaseUtil.getDetails().get()...
    
    // 2. Lấy cart items
    FirebaseUtil.getCartItems().get()...
    
    // 3. Tạo OrderItemModel
    for (doc : cartSnapshot) {
        OrderItemModel item = new OrderItemModel(...);
        FirebaseFirestore.getInstance()...add(item);
    }
    
    // 4. ✅ NẾU CÓ CALLBACK → GỌI VÀ RETURN (MoMo/ZaloPay)
    if (onOrderCreated != null) {
        onOrderCreated.run();  // → processOrderWithMoMo()
        return;
    }
    
    // 5. NẾU KHÔNG CÓ CALLBACK → TIẾP TỤC (COD)
    // Check stock
    // Update details
    // Delete cart
}

// Overload method cho COD (không cần callback)
private void processOrder() {
    processOrder(null);
}
```

**Ưu điểm:**
- ✅ **Xóa 110 dòng duplicate code**
- ✅ Single source of truth - chỉ 1 nơi tạo order items
- ✅ Dễ maintain - sửa 1 lần áp dụng cho cả COD và MoMo
- ✅ Tuân thủ DRY principle
- ✅ Code ngắn gọn, dễ đọc hơn

---

## 🔄 Luồng hoạt động

### **Scenario 1: COD Payment**

```java
// User chọn COD
processOrder(); // Không có callback
                ↓
1. Lấy prevOrderId = 123
2. Tạo order items với orderId = 124
3. onOrderCreated == null → Tiếp tục
4. Check stock
5. Update stock
6. Update details
7. Delete cart
8. Show success dialog
```

### **Scenario 2: MoMo Payment**

```java
// User chọn MoMo
processOrder(() -> {
    processOrderWithMoMo();
});
                ↓
1. Lấy prevOrderId = 123
2. Tạo order items với orderId = 124
3. onOrderCreated != null → Gọi callback
                ↓
4. processOrderWithMoMo()
   - Gửi MoMo với orderId = 124
   - Mở browser
                ↓
5. User thanh toán trên MoMo
                ↓
6. PaymentResultActivity nhận callback
   - Update status = "Confirmed"
   - Update transactionId
```

---

## 📊 So sánh

| Aspect | Code cũ (duplicate) | Code mới (refactored) |
|--------|---------------------|----------------------|
| **Số dòng code** | ~260 dòng | ~150 dòng ✅ |
| **Số hàm** | 3 hàm | 2 hàm ✅ |
| **Duplicate logic** | 110 dòng duplicate ❌ | 0 dòng duplicate ✅ |
| **Maintainability** | Phải sửa 2 nơi ❌ | Sửa 1 nơi ✅ |
| **DRY principle** | Vi phạm ❌ | Tuân thủ ✅ |
| **Readability** | Khó follow ❌ | Dễ hiểu ✅ |

---

## 💡 Design Pattern: Template Method với Callback

### **Concept:**

```java
private void processOrder(Runnable onOrderCreated) {
    // ===== PHẦN CHUNG (Template) =====
    step1_getOrderId();
    step2_createOrderItems();
    
    // ===== PHẦN KHÁC NHAU (Hook) =====
    if (onOrderCreated != null) {
        onOrderCreated.run();  // Hook cho MoMo/ZaloPay
        return;
    }
    
    // ===== PHẦN CHUNG (Template tiếp) =====
    step3_checkStock();
    step4_updateDatabase();
    step5_showSuccess();
}
```

**Giải thích:**
- **Template**: Phần logic giống nhau (lấy ID, tạo order)
- **Hook**: Phần logic khác nhau (callback cho payment gateway)
- **Flexibility**: Dễ mở rộng cho ZaloPay, VNPay, etc.

---

## 🎯 Cách sử dụng

### **1. COD Payment (không callback)**

```java
checkoutBtn.setOnClickListener(v -> {
    processOrder(); // Simple!
});
```

### **2. MoMo Payment (có callback)**

```java
cardMoMo.setOnClickListener(v -> {
    paymentMethod = PaymentMethod.MOMO;
    
    processOrder(() -> {
        // Callback: đã có prevOrderId và order items
        processOrderWithMoMo();
    });
});
```

### **3. ZaloPay Payment (tương tự)**

```java
cardZaloPay.setOnClickListener(v -> {
    paymentMethod = PaymentMethod.ZALOPAY;
    
    processOrder(() -> {
        // Callback: đã có prevOrderId và order items
        processOrderWithZaloPay();
    });
});
```

---

## 🔧 Chi tiết implementation

### **Method 1: processOrder() - Overload (COD)**

```java
/**
 * Xử lý đơn hàng COD (không callback)
 */
private void processOrder() {
    processOrder(null); // Gọi method chính với callback = null
}
```

### **Method 2: processOrder(Runnable) - Main (All payments)**

```java
/**
 * Xử lý đơn hàng với optional callback
 * 
 * @param onOrderCreated Callback sau khi tạo order items.
 *                       - null: COD payment → check stock, update DB
 *                       - not null: MoMo/ZaloPay → gọi callback, return
 */
private void processOrder(Runnable onOrderCreated) {
    validate();
    
    // 1. Lấy prevOrderId
    FirebaseUtil.getDetails().get().addOnCompleteListener(task -> {
        prevOrderId = task.getResult().getLong("lastOrderId");
        
        // 2. Lấy cart items và tạo order items
        FirebaseUtil.getCartItems().get().addOnCompleteListener(cartTask -> {
            for (doc : cartSnapshot) {
                // Tạo OrderItemModel với orderId = prevOrderId + 1
                createAndSaveOrderItem(doc);
            }
            
            // 3. ✅ HOOK: Nếu có callback → gọi và return
            if (onOrderCreated != null) {
                onOrderCreated.run();
                return;
            }
            
            // 4. Tiếp tục cho COD
            checkStock();
            updateDatabase();
            showSuccess();
        });
    });
}
```

### **Method 3: processOrderWithMoMo() - MoMo specific**

```java
/**
 * Thanh toán MoMo (được gọi từ callback của processOrder)
 */
private void processOrderWithMoMo() {
    int totalAmount = calculateTotal();
    
    // ✅ prevOrderId đã có từ processOrder()
    MoMoPayment.createPayment(
        this,
        prevOrderId + 1,  // Chính xác!
        totalAmount,
        "Thanh toán #" + (prevOrderId + 1),
        callback
    );
}
```

---

## 🧪 Test Cases

### **Test 1: COD Payment**

```
Input: User chọn COD, cart có 2 sản phẩm
Expected:
  1. ✅ Order items created: orderId = 124
  2. ✅ Stock checked and updated
  3. ✅ Cart deleted
  4. ✅ Success dialog shown
```

### **Test 2: MoMo Payment**

```
Input: User chọn MoMo, cart có 2 sản phẩm
Expected:
  1. ✅ Order items created: orderId = 124
  2. ✅ processOrderWithMoMo() called with orderId = 124
  3. ✅ MoMo payment page opened
  4. ✅ After payment: status updated to "Confirmed"
```

### **Test 3: Multiple payments**

```
Input: User thanh toán COD, sau đó thanh toán MoMo
Expected:
  1. ✅ COD order: orderId = 124, status = "Delivered"
  2. ✅ MoMo order: orderId = 125, status = "Confirmed"
  3. ✅ No duplicate code executed
```

---

## 📚 Best Practices Applied

### **1. DRY (Don't Repeat Yourself)**
✅ Một logic chỉ viết một lần

### **2. Single Responsibility**
✅ `processOrder()`: Tạo order items  
✅ `processOrderWithMoMo()`: Xử lý MoMo payment  
✅ `PaymentResultActivity`: Cập nhật status

### **3. Open/Closed Principle**
✅ Mở rộng dễ dàng (thêm ZaloPay) mà không sửa code cũ

### **4. Callback Pattern**
✅ Xử lý async operations hiệu quả

---

## 🎯 Kết luận

### **Trước refactor:**
❌ 260 dòng code, 110 dòng duplicate  
❌ 3 methods làm việc giống nhau  
❌ Khó maintain, dễ bug

### **Sau refactor:**
✅ 150 dòng code, 0 dòng duplicate  
✅ 2 methods, tái sử dụng logic  
✅ Dễ maintain, dễ mở rộng

### **Bài học:**
> "Trước khi viết code mới, hãy xem có thể dùng lại code cũ không!"

---

**Tác giả:** PRM_eCommerce Team  
**Ngày refactor:** 2025-10-21  
**Pattern:** Template Method + Callback


