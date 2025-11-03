# 🔄 Luồng thanh toán MoMo - Cập nhật Order Status

## 📋 Tổng quan

Hệ thống thanh toán MoMo được tích hợp với khả năng:
1. **Tạo order items** với status `Pending` khi user checkout
2. **Gửi orderID** qua MoMo payment URL (dùng `extraData`)
3. **Nhận callback** từ MoMo khi thanh toán xong
4. **Cập nhật status** của order items từ `Pending` → `Confirmed`

---

## 🎯 Luồng hoạt động chi tiết

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. USER CHECKOUT                                                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
        User nhấn "Checkout" → Chọn phương thức "MoMo"
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. TẠO ORDER ITEMS (CheckoutActivity.java:270-293)              │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    • Lấy prevOrderId từ Firebase (dashboard/details)
    • Lấy cart items từ Firebase
    • Tạo OrderItemModel cho mỗi sản phẩm:
        - orderId = prevOrderId + 1
        - status = "Pending"
        - paymentMethod = "PENDING"
        - transactionId = null
    • Lưu vào Firestore: orders/{userId}/items/{autoId}
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. GỌI MOMO API (MoMoPayment.java:32-129)                       │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    • Tạo extraData = Base64(orderId)
        VD: orderId = 123 → extraData = "MTIz"
    • Gửi request đến MoMo:
        POST https://test-payment.momo.vn/v2/gateway/api/create
        {
          "partnerCode": "MOMO",
          "orderId": "ORDER_123_1234567890",
          "amount": 50000,
          "extraData": "MTIz",  ← ORDER ID ENCODED
          "redirectUrl": "ecommerce://payment-result",
          ...
        }
    • Nhận response:
        {
          "resultCode": 0,
          "payUrl": "https://test-payment.momo.vn/pay/...",
          ...
        }
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. MỞ BROWSER (MoMoPayment.java:132-154)                        │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    • Mở Chrome Custom Tab với payUrl
    • User thanh toán trên trang web MoMo
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. MOMO REDIRECT (sau khi thanh toán)                           │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    URL callback:
    ecommerce://payment-result?
        partnerCode=MOMO&
        orderId=ORDER_123_1234567890&
        resultCode=0&                    ← 0 = success
        message=Success&
        transId=99999999&                ← MoMo transaction ID
        extraData=MTIz&                  ← ORDER ID (Base64)
        amount=50000&
        signature=...
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. XỬ LÝ CALLBACK (PaymentResultActivity.java)                  │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    6.1. Parse URL parameters
    6.2. Decode extraData:
         extraData = "MTIz"
         → Base64.decode() → "123"
         → orderId = 123
                            ↓
    6.3. Kiểm tra resultCode:
         • resultCode = 0 → Thanh toán thành công
         • resultCode ≠ 0 → Thanh toán thất bại
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. CẬP NHẬT FIREBASE (PaymentResultActivity.java:86-121)        │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    Query Firestore:
    orders/{userId}/items
        .whereEqualTo("orderId", 123)
        .get()
                            ↓
    Update tất cả order items tìm được:
    {
        "status": "Confirmed",           ← Pending → Confirmed
        "transactionId": "99999999",     ← MoMo transaction ID
        "paymentMethod": "MOMO"          ← PENDING → MOMO
    }
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 8. HOÀN TẤT                                                      │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    • Hiển thị Toast "Thanh toán thành công!"
    • Chuyển về MainActivity
    • User có thể xem order trong "My Orders"
```

---

## 📊 Cấu trúc dữ liệu Firestore

### **Trước khi thanh toán**
```json
orders/{userId}/items/{autoId}
{
  "orderId": 123,
  "productId": 456,
  "name": "iPhone 15 Pro",
  "price": 30000000,
  "quantity": 1,
  "status": "Pending",          ← Chờ thanh toán
  "paymentMethod": "PENDING",   ← Chưa xác định
  "transactionId": null,        ← Chưa có
  "fullName": "Nguyen Van A",
  "email": "test@example.com",
  ...
}
```

### **Sau khi thanh toán thành công**
```json
orders/{userId}/items/{autoId}
{
  "orderId": 123,
  "productId": 456,
  "name": "iPhone 15 Pro",
  "price": 30000000,
  "quantity": 1,
  "status": "Confirmed",        ← ✅ Đã xác nhận
  "paymentMethod": "MOMO",      ← ✅ Thanh toán qua MoMo
  "transactionId": "99999999",  ← ✅ MoMo transaction ID
  "fullName": "Nguyen Van A",
  "email": "test@example.com",
  ...
}
```

---

## 🔑 Các điểm quan trọng

### **1. ExtraData - Truyền OrderID**
```java
// MoMoPayment.java:44-48
String extraDataRaw = String.valueOf(orderId); // "123"
String extraData = android.util.Base64.encodeToString(
    extraDataRaw.getBytes("UTF-8"), 
    android.util.Base64.NO_WRAP
); // "MTIz"
```

**Lý do dùng Base64:**
- MoMo yêu cầu extraData phải được encode Base64
- Có thể gửi JSON object phức tạp nếu cần (VD: `{"orderId":123,"userId":"abc"}`)

### **2. Deep Link - Nhận callback**
```xml
<!-- AndroidManifest.xml -->
<activity android:name=".activities.PaymentResultActivity">
    <intent-filter>
        <data
            android:scheme="ecommerce"
            android:host="payment-result"/>
    </intent-filter>
</activity>
```

**URL callback:** `ecommerce://payment-result?resultCode=0&...`

### **3. Query và Update - Cập nhật status**
```java
// PaymentResultActivity.java:89-121
db.collection("orders")
    .document(userId)
    .collection("items")
    .whereEqualTo("orderId", orderId)  // ← Tìm theo orderId
    .get()
    .addOnSuccessListener(querySnapshot -> {
        querySnapshot.forEach(doc -> {
            db.collection("orders")
                .document(userId)
                .collection("items")
                .document(doc.getId())
                .update(
                    "status", "Confirmed",
                    "transactionId", transactionId,
                    "paymentMethod", "MOMO"
                );
        });
    });
```

**Quan trọng:** 
- Một order có thể có **nhiều items** (nhiều sản phẩm)
- Cần update **tất cả items** có cùng `orderId`

---

## 🧪 Test Flow

### **1. Test với MoMo Sandbox**
```
Môi trường: Test (https://test-payment.momo.vn)
Credentials:
  - partnerCode: MOMO
  - accessKey: F8BBA842ECF85
  - secretKey: K951B6PE1waDMi640xX08PD3vg6EkVlz

Test Card:
  - Số thẻ: 9704 0000 0000 0018
  - Tên: NGUYEN VAN A
  - Ngày hết hạn: 03/07
  - OTP: bất kỳ
```

### **2. Test Steps**
1. Chọn sản phẩm → Add to cart
2. Checkout → Chọn MoMo
3. Kiểm tra Firestore: order items có status = "Pending"
4. Thanh toán trên trang MoMo (dùng test card)
5. Sau khi thanh toán → Kiểm tra Firestore: status = "Confirmed"

### **3. Debug Logs**
```
MoMoPayment: Request: {"partnerCode":"MOMO","orderId":"ORDER_123_...",...}
MoMoPayment: Response: {"resultCode":0,"payUrl":"https://...",...}
MoMoPayment: Opened payment URL in Chrome Custom Tab

PaymentResult: Payment callback:
PaymentResult:   - ResultCode: 0
PaymentResult:   - TransId: 99999999
PaymentResult:   - ExtraData: MTIz
PaymentResult: Decoded OrderId: 123
PaymentResult: ✅ Updated 3 order items to status: Confirmed
```

---

## 🚨 Xử lý lỗi

### **Lỗi 1: Thanh toán thất bại**
```
resultCode ≠ 0 → Giữ nguyên status = "Pending"
→ User có thể thử lại
```

### **Lỗi 2: ExtraData rỗng**
```java
if (extraData == null || extraData.isEmpty()) {
    return 0; // Default orderId
}
```

### **Lỗi 3: Không tìm thấy order items**
```java
if (querySnapshot.isEmpty()) {
    Log.w(TAG, "No order items found with orderId: " + orderId);
}
```

---

## 📱 Screenshots (mô phỏng)

```
[Checkout Screen]
┌─────────────────┐
│  Choose Payment │
├─────────────────┤
│ 💵 COD          │
│ 🟣 MoMo         │ ← User chọn
│ 🔵 ZaloPay      │
└─────────────────┘
        ↓
[MoMo Payment Page - Browser]
┌─────────────────┐
│   MoMo Payment  │
│   Amount: 50k   │
│   [Enter Card]  │
│   [   Pay   ]   │
└─────────────────┘
        ↓
[Success Dialog]
┌─────────────────┐
│   ✅ Success!   │
│ Payment Complete│
│   [   OK   ]    │
└─────────────────┘
```

---

## 🔐 Security Notes

⚠️ **QUAN TRỌNG:**
1. **Không hardcode** `secretKey` trong app → Dùng backend
2. **Verify signature** từ MoMo callback (hiện tại chưa làm)
3. **Validate resultCode** trước khi update status
4. **Kiểm tra userId** để tránh update order của người khác

---

## 🎯 Next Steps

### **Nâng cao bảo mật:**
```java
// TODO: Verify MoMo signature trong callback
private boolean verifyMoMoSignature(String signature, Map<String, String> params) {
    // Tạo lại signature từ params
    // So sánh với signature từ MoMo
    // Return true nếu khớp
}
```

### **Thêm IPN (Instant Payment Notification):**
- Tạo backend endpoint nhận IPN từ MoMo
- Cập nhật status từ backend (tin cậy hơn client-side)

### **Thêm retry mechanism:**
- Nếu update status thất bại → Retry 3 lần
- Lưu vào queue để xử lý sau

---

## 📚 Tài liệu tham khảo

- [MoMo Payment Gateway Docs](https://developers.momo.vn/)
- [Firebase Firestore Queries](https://firebase.google.com/docs/firestore/query-data/queries)
- [Android Deep Links](https://developer.android.com/training/app-links/deep-linking)

---

**Tác giả:** PRM_eCommerce Team  
**Ngày cập nhật:** 2025-10-21


