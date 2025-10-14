package com.example.newEcom.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.model.CartItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

public class CartAdapter extends FirestoreRecyclerAdapter<CartItemModel, CartAdapter.CartViewHolder> {

    private final Context context;
    private AppCompatActivity activity;
    private final int[] stock = new int[1];
    private int totalPrice = 0;
    private boolean sentInitialTotal = false;

    public CartAdapter(@NonNull FirestoreRecyclerOptions<CartItemModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new CartViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull CartItemModel model) {
        // Hiển thị dữ liệu text
        holder.productName.setText(model.getName());
        holder.singleProductPrice.setText("₹ " + model.getPrice());
        holder.productPrice.setText("₹ " + (model.getPrice() * model.getQuantity()));
        holder.originalPrice.setText("₹ " + model.getOriginalPrice());
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.productQuantity.setText(String.valueOf(model.getQuantity()));

        // Load ảnh: luôn có placeholder + error, KHÔNG tắt shimmer ở đây nữa
        String url = model.getImage();
        Picasso.get()
                .load(url == null || url.trim().isEmpty() ? null : url)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(holder.productCartImage);

        // Nếu chưa gửi tổng tiền ban đầu, tính và gửi 1 lần khi có bind đầu tiên
        if (!sentInitialTotal && position == 0) {
            recalcAndBroadcastTotal();
            sentInitialTotal = true;
        }

        // Tăng giảm số lượng
        holder.plusBtn.setOnClickListener(v -> changeQuantity(model, true));
        holder.minusBtn.setOnClickListener(v -> changeQuantity(model, false));
    }

    /** Tính lại tổng tiền từ snapshots và broadcast sang Fragment */
    private void recalcAndBroadcastTotal() {
        totalPrice = 0;
        for (CartItemModel m : getSnapshots()) {
            totalPrice += (m.getPrice() * m.getQuantity());
        }
        Intent intent = new Intent("price");
        intent.putExtra("totalPrice", totalPrice);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void changeQuantity(CartItemModel model, boolean plus) {
        // Lấy stock hiện tại cho product
        FirebaseUtil.getProducts().whereEqualTo("productId", model.getProductId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            stock[0] = (int) (long) document.getData().get("stock");
                        }
                        // Sau khi đã biết stock, đọc cart item để cập nhật
                        FirebaseUtil.getCartItems().whereEqualTo("productId", model.getProductId())
                                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task2) {
                                        if (task2.isSuccessful()) {
                                            for (QueryDocumentSnapshot document : task2.getResult()) {
                                                String docId = document.getId();
                                                int quantity = (int) (long) document.getData().get("quantity");
                                                if (plus) {
                                                    if (quantity < stock[0]) {
                                                        FirebaseUtil.getCartItems().document(docId).update("quantity", quantity + 1);
                                                        totalPrice += model.getPrice();
                                                    } else {
                                                        Toast.makeText(context, "Max stock available: " + stock[0], Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    if (quantity > 1) {
                                                        FirebaseUtil.getCartItems().document(docId).update("quantity", quantity - 1);
                                                        totalPrice -= model.getPrice();
                                                    } else {
                                                        // Xoá item khỏi giỏ
                                                        FirebaseUtil.getCartItems().document(docId).delete();
                                                        totalPrice -= model.getPrice();
                                                    }
                                                }

                                                // Cập nhật badge
                                                if (context instanceof MainActivity) {
                                                    ((MainActivity) context).addOrRemoveBadge();
                                                }

                                                // Phát tổng tiền mới
                                                Intent intent = new Intent("price");
                                                intent.putExtra("totalPrice", Math.max(totalPrice, 0));
                                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                            }
                                        }
                                    }
                                });
                    }
                });
    }

    /** Được gọi khi dữ liệu Firestore thay đổi/đã load xong (kể cả rỗng) */
    @Override
    public void onDataChanged() {
        super.onDataChanged();

        Activity act = (Activity) context;
        ShimmerFrameLayout shimmerLayout = act.findViewById(R.id.shimmerLayout);
        View mainLayout = act.findViewById(R.id.mainLinearLayout);
        View empty = act.findViewById(R.id.emptyCartImageView);

        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
        }
        if (mainLayout != null) mainLayout.setVisibility(View.VISIBLE);

        if (getItemCount() == 0) {
            if (empty != null) empty.setVisibility(View.VISIBLE);
            totalPrice = 0;
            Intent intent = new Intent("price");
            intent.putExtra("totalPrice", 0);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } else {
            if (empty != null) empty.setVisibility(View.INVISIBLE);
            // dữ liệu thay đổi có thể làm tổng tiền đổi ⇒ tính lại
            recalcAndBroadcastTotal();
        }
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, singleProductPrice, productQuantity, minusBtn, plusBtn, originalPrice;
        ImageView productCartImage;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.nameTextView);
            singleProductPrice = itemView.findViewById(R.id.priceTextView1);
            productPrice = itemView.findViewById(R.id.priceTextView);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            productQuantity = itemView.findViewById(R.id.quantityTextView);
            productCartImage = itemView.findViewById(R.id.productImageCart);
            minusBtn = itemView.findViewById(R.id.minusBtn);
            plusBtn = itemView.findViewById(R.id.plusBtn);
        }
    }
}