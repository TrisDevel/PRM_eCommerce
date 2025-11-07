package com.example.newEcom.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.OrderDetailsAdminActivity;
import com.example.newEcom.model.OrderModel;
import com.google.firebase.Timestamp;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * ✅ ADMIN ORDER ADAPTER
 * 
 * Adapter riêng cho Admin Dashboard - Hiển thị orders từ TẤT CẢ users
 * Khác với OrderListAdapter (dành cho user xem orders của chính họ)
 * 
 * Tính năng:
 * - Hiển thị thông tin order: Order ID, Customer Name, Total Amount, Status, Payment Method
 * - Color-coded status badges (Pending=Orange, Confirmed=Blue, Delivered=Green, Cancelled=Red)
 * - Click vào order → Mở OrderDetailsAdminActivity để xem chi tiết và cập nhật status
 * - Format tiền tệ và ngày tháng
 */
public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.AdminOrderViewHolder> {
    private static final String TAG = "AdminOrderAdapter";
    
    private List<OrderModel> orders;
    private Context context;

    public AdminOrderAdapter(List<OrderModel> orders, Context context) {
        this.orders = orders;
        this.context = context;
    }

    @NonNull
    @Override
    public AdminOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_order_adapter, parent, false);
        return new AdminOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminOrderViewHolder holder, int position) {
        OrderModel order = orders.get(position);
        
        // ===== ORDER ID =====
        holder.orderIdText.setText("Order #" + order.getOrderId());
        
        // ===== CUSTOMER INFO =====
        String customerInfo = order.getFullName() != null ? order.getFullName() : "Unknown Customer";
        if (order.getPhoneNumber() != null && !order.getPhoneNumber().isEmpty()) {
            customerInfo += " · " + order.getPhoneNumber();
        }
        holder.customerNameText.setText(customerInfo);
        
        // ===== EMAIL =====
        if (order.getEmail() != null && !order.getEmail().isEmpty()) {
            holder.emailText.setText(order.getEmail());
            holder.emailText.setVisibility(View.VISIBLE);
        } else {
            holder.emailText.setVisibility(View.GONE);
        }
        
        // ===== TOTAL AMOUNT =====
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedAmount = currencyFormat.format(order.getTotalAmount());
        holder.totalAmountText.setText(formattedAmount);
        
        // ===== ITEM COUNT =====
        holder.itemCountText.setText(order.getItemCount() + " items");
        
        // ===== ORDER DATE =====
        Timestamp timestamp = order.getTimestamp();
        if (timestamp != null) {
            String dateStr = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(timestamp.toDate());
            holder.orderDateText.setText(dateStr);
        } else {
            holder.orderDateText.setText("—");
        }
        
        // ===== STATUS BADGE =====
        String status = order.getStatus() != null ? order.getStatus() : "Unknown";
        holder.statusText.setText(status);
        
        // Color-coded status
        int statusColor;
        int statusBgColor;
        switch (status.toLowerCase()) {
            case "pending":
                statusColor = context.getResources().getColor(R.color.orange);
                statusBgColor = context.getResources().getColor(R.color.orange_light);
                break;
            case "confirmed":
                statusColor = context.getResources().getColor(R.color.blue);
                statusBgColor = context.getResources().getColor(R.color.blue_light);
                break;
            case "shipping":
                statusColor = context.getResources().getColor(R.color.purple);
                statusBgColor = context.getResources().getColor(R.color.purple_light);
                break;
            case "delivered":
                statusColor = context.getResources().getColor(R.color.green);
                statusBgColor = context.getResources().getColor(R.color.green_light);
                break;
            case "cancelled":
                statusColor = context.getResources().getColor(R.color.red);
                statusBgColor = context.getResources().getColor(R.color.red_light);
                break;
            default:
                statusColor = context.getResources().getColor(R.color.grey);
                statusBgColor = context.getResources().getColor(R.color.grey_light);
        }
        holder.statusText.setTextColor(statusColor);
        holder.statusText.setBackgroundColor(statusBgColor);
        
        // ===== PAYMENT METHOD BADGE =====
        String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod() : "—";
        holder.paymentMethodText.setText(paymentMethod);
        
        // Color-coded payment method
        int paymentColor;
        switch (paymentMethod.toUpperCase()) {
            case "MOMO":
                paymentColor = context.getResources().getColor(R.color.momo_pink);
                break;
            case "ZALOPAY":
                paymentColor = context.getResources().getColor(R.color.zalopay_blue);
                break;
            case "COD":
                paymentColor = context.getResources().getColor(R.color.cod_green);
                break;
            default:
                paymentColor = context.getResources().getColor(R.color.grey);
        }
        holder.paymentMethodText.setTextColor(paymentColor);
        
        // ===== CLICK LISTENER - MỞ CHI TIẾT ORDER =====
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailsAdminActivity.class);
            intent.putExtra("orderId", order.getOrderId());
            intent.putExtra("customerName", order.getFullName());
            intent.putExtra("email", order.getEmail());
            intent.putExtra("phone", order.getPhoneNumber());
            intent.putExtra("address", order.getAddress());
            intent.putExtra("status", order.getStatus());
            intent.putExtra("paymentMethod", order.getPaymentMethod());
            intent.putExtra("transactionId", order.getTransactionId());
            intent.putExtra("totalAmount", order.getTotalAmount());
            intent.putExtra("itemCount", order.getItemCount());
            intent.putExtra("timestamp", order.getTimestamp() != null ? order.getTimestamp().getSeconds() : 0L);
            intent.putExtra("comments", order.getComments());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    /**
     * ✅ VIEW HOLDER
     */
    public static class AdminOrderViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView orderIdText, customerNameText, emailText, totalAmountText, itemCountText;
        TextView orderDateText, statusText, paymentMethodText;
        ImageView orderIcon;

        public AdminOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.orderCardView);
            orderIdText = itemView.findViewById(R.id.orderIdText);
            customerNameText = itemView.findViewById(R.id.customerNameText);
            emailText = itemView.findViewById(R.id.emailText);
            totalAmountText = itemView.findViewById(R.id.totalAmountText);
            itemCountText = itemView.findViewById(R.id.itemCountText);
            orderDateText = itemView.findViewById(R.id.orderDateText);
            statusText = itemView.findViewById(R.id.statusText);
            paymentMethodText = itemView.findViewById(R.id.paymentMethodText);
            orderIcon = itemView.findViewById(R.id.orderIcon);
        }
    }
}

