package com.example.newEcom.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.model.OrderItemModel;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * ✅ ORDER ITEM ADAPTER
 * 
 * Adapter đơn giản để hiển thị danh sách sản phẩm trong một order
 * Sử dụng trong OrderDetailsAdminActivity
 */
public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private List<OrderItemModel> orderItems;
    private Context context;

    public OrderItemAdapter(List<OrderItemModel> orderItems, Context context) {
        this.orderItems = orderItems;
        this.context = context;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_item_adapter, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItemModel item = orderItems.get(position);
        
        // Product Name
        holder.productNameText.setText(item.getName());
        
        // Quantity
        holder.quantityText.setText("Qty: " + item.getQuantity());
        
        // Price
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.priceText.setText(currencyFormat.format(item.getPrice()));
        
        // Subtotal
        int subtotal = item.getPrice() * item.getQuantity();
        holder.subtotalText.setText("Subtotal: " + currencyFormat.format(subtotal));
        
        // Product Image
        if (item.getImage() != null && !item.getImage().isEmpty()) {
            Picasso.get()
                    .load(item.getImage())
                    .placeholder(R.drawable.temp)
                    .error(R.drawable.temp)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.drawable.temp);
        }
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productNameText, quantityText, priceText, subtotalText;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            
            productImage = itemView.findViewById(R.id.productImage);
            productNameText = itemView.findViewById(R.id.productNameText);
            quantityText = itemView.findViewById(R.id.quantityText);
            priceText = itemView.findViewById(R.id.priceText);
            subtotalText = itemView.findViewById(R.id.subtotalText);
        }
    }
}

