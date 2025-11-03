package com.example.newEcom.model;

import com.google.firebase.Timestamp;

/**
 * Model cho ORDER TỔNG (document order chính)
 * Lưu tại: orders/{userId}/ordersList/{orderId}
 */
public class OrderModel {
    private int orderId;
    private String status;              // "Pending", "Confirmed", "Shipping", "Delivered", "Cancelled"
    private String transactionId;       // Transaction ID từ payment gateway
    private String paymentMethod;       // "COD", "MOMO", "ZALOPAY"
    private int totalAmount;            // Tổng tiền đơn hàng
    private int itemCount;              // Số lượng sản phẩm trong đơn
    private Timestamp timestamp;        // Thời gian tạo order
    
    // Thông tin khách hàng
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String comments;

    public OrderModel() {
    }

    public OrderModel(int orderId, String status, String transactionId, String paymentMethod, 
                     int totalAmount, int itemCount, Timestamp timestamp, 
                     String fullName, String email, String phoneNumber, String address, String comments) {
        this.orderId = orderId;
        this.status = status;
        this.transactionId = transactionId;
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
        this.timestamp = timestamp;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.comments = comments;
    }

    // Getters and Setters
    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}

