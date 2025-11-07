package com.example.newEcom.utils;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.model.CartItemModel;
import com.example.newEcom.model.ProductModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseUtil {
    public static CollectionReference getCategories(){
        return FirebaseFirestore.getInstance().collection("categories");
    }

    public static CollectionReference getProducts() {
        return FirebaseFirestore.getInstance().collection("products");
    }

    public static CollectionReference getBanner(){
        return FirebaseFirestore.getInstance().collection("banners");
    }

    public static CollectionReference getCartItems(){
        return FirebaseFirestore.getInstance().collection("cart").document(FirebaseAuth.getInstance().getUid()).collection("items");
    }

    public static CollectionReference getWishlistItems(){
        return FirebaseFirestore.getInstance().collection("wishlists").document(FirebaseAuth.getInstance().getUid()).collection("items");
    }

    public static CollectionReference getOrderList(){
        return FirebaseFirestore.getInstance().collection("orders").document(FirebaseAuth.getInstance().getUid()).collection("ordersList");
    }
    public static CollectionReference getOrderItems(int orderId){
        // orders/{uid}/ordersList/{orderId}/items
        return FirebaseFirestore.getInstance()
                .collection("orders")
                .document(FirebaseAuth.getInstance().getUid())
                .collection("ordersList")
                .document(String.valueOf(orderId))
                .collection("items");
    }


    public static CollectionReference getReviews(int pid){
        return FirebaseFirestore.getInstance().collection("reviews").document(pid+"").collection("review");
    }

    public static DocumentReference getDetails(){
        return FirebaseFirestore.getInstance().collection("dashboard").document("details");
    }

    public static StorageReference getProductImageReference(String id){
        return FirebaseStorage.getInstance().getReference().child("product_images").child(id);
    }

    public static StorageReference getCategoryImageReference(String id){
        return FirebaseStorage.getInstance().getReference().child("category_images").child(id);
    }

    public static StorageReference getBannerImageReference(String id){
        return FirebaseStorage.getInstance().getReference().child("banner_images").child(id);
    }

    // Phương thức mới: Lấy collection users từ Firestore
    // Sử dụng cho User Management trong Admin Dashboard
    public static CollectionReference getUsers(){
        return FirebaseFirestore.getInstance().collection("users");
    }

    /**
     * @return Query object để lấy tất cả orders
     */
    public static Query getAllOrders() {
        return FirebaseFirestore.getInstance()
                .collectionGroup("ordersList");  // Query tất cả collection có tên "ordersList"
    }

    /**
     * @param status Trạng thái cần filter (VD: "Confirmed", "Pending")
     * @return Query object đã được filter theo status
     */
    public static Query getOrdersByStatus(String status) {
        return FirebaseFirestore.getInstance()
                .collectionGroup("ordersList")
                .whereEqualTo("status", status)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    /**
     * @param paymentMethod Phương thức thanh toán (VD: "MOMO", "COD")
     * @return Query object đã được filter theo payment method
     */
    public static Query getOrdersByPaymentMethod(String paymentMethod) {
        return FirebaseFirestore.getInstance()
                .collectionGroup("ordersList")
                .whereEqualTo("paymentMethod", paymentMethod)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }
}
