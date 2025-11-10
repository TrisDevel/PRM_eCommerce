package com.example.newEcom.adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.fragments.OrderDetailsFragment;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.model.OrderModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
// Removed dialog; using PopupWindow for popup-style UI
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;

public class OrderListAdapter extends FirestoreRecyclerAdapter<OrderModel, OrderListAdapter.OrderListViewHolder> {
    private Context context;
    private AppCompatActivity activity;

    public OrderListAdapter(@NonNull FirestoreRecyclerOptions<OrderModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public OrderListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new OrderListAdapter.OrderListViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull OrderListViewHolder holder, int position, @NonNull OrderModel model) {
        holder.productName.setText("Order #" + model.getOrderId() + " · " + model.getItemCount() + " items · " + model.getTotalAmount() + "₫");
        Timestamp timestamp = model.getTimestamp();
        String time = new SimpleDateFormat("dd MMM yyyy").format(timestamp.toDate());
        holder.orderDate.setText(time);
        // Hard image for orders (cart icon)
        holder.productImage.setImageResource(R.drawable.pk);

        holder.itemView.setOnClickListener(v -> showOrderItemsPopup(model, holder.itemView));
    }

    public class OrderListViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, orderDate;

        public OrderListViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImageOrder);
            productName = itemView.findViewById(R.id.nameTextView);
            orderDate = itemView.findViewById(R.id.dateTextView);
        }
    }

    private void showOrderItemsPopup(@NonNull OrderModel order, @NonNull View anchor) {
        LayoutInflater inflater = LayoutInflater.from(context);
        // Root scrollable container for popup content
        android.widget.ScrollView scroll = new android.widget.ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        container.setPadding(padding/2, padding, padding/2, padding);
        scroll.addView(container);

        TextView header = new TextView(context);
        header.setText("Order #" + order.getOrderId() + "\nStatus: " + order.getStatus() + "\nPayment: " + order.getPaymentMethod() + "\nTotal: " + order.getTotalAmount() + "₫");
        header.setTextColor(context.getResources().getColor(R.color.black));
        header.setTextSize(16);
        container.addView(header);

        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * context.getResources().getDisplayMetrics().density)));
        divider.setBackgroundColor(context.getResources().getColor(R.color.grey));
        container.addView(divider);

        FirebaseUtil.getOrderItems(order.getOrderId()).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            QuerySnapshot snapshot = task.getResult();
            if (snapshot == null) return;
            final android.widget.PopupWindow[] popupRef = new android.widget.PopupWindow[1];

            for (QueryDocumentSnapshot doc : snapshot) {
                OrderItemModel item = doc.toObject(OrderItemModel.class);
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, padding / 2, 0, padding / 2);

                ImageView image = new ImageView(context);
                int size = (int) (48 * context.getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(size, size);
                image.setLayoutParams(imgParams);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                try {
                    Picasso.get().load(item.getImage()).placeholder(R.drawable.temp).into(image);
                } catch (Exception e) {
                    image.setImageResource(R.drawable.temp);
                }

                LinearLayout textCol = new LinearLayout(context);
                textCol.setOrientation(LinearLayout.VERTICAL);
                textCol.setPadding(padding/2, 0, 0, 0);
                textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                TextView line1 = new TextView(context);
                line1.setText(item.getName());
                line1.setTextColor(context.getResources().getColor(R.color.black));
                line1.setTextSize(15);

                TextView line2 = new TextView(context);
                int subtotal = item.getPrice() * item.getQuantity();
                line2.setText("Qty: " + item.getQuantity() + " · Price: " + item.getPrice() + "₫");
                line2.setTextColor(context.getResources().getColor(R.color.my_primary));
                line2.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView line3 = new TextView(context);
                line3.setText("Subtotal: " + subtotal + "₫");
                line3.setTextColor(context.getResources().getColor(R.color.my_primary));
                line3.setTypeface(null, android.graphics.Typeface.BOLD);
                textCol.addView(line1);
                textCol.addView(line2);
                textCol.addView(line3);

                row.addView(image);
                row.addView(textCol);

                row.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putInt("orderId", order.getOrderId());
                    bundle.putInt("productId", item.getProductId());
                    OrderDetailsFragment fragment = new OrderDetailsFragment();
                    fragment.setArguments(bundle);
                    if (!fragment.isAdded()) {
                        activity.getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, fragment).addToBackStack(null).commit();
                    }
                    if (popupRef[0] != null && popupRef[0].isShowing()) {
                        popupRef[0].dismiss();
                    }
                });

                container.addView(row);
            }

            // Build PopupWindow
            android.widget.PopupWindow popup = new android.widget.PopupWindow(
                    scroll,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );
            popupRef[0] = popup;
            // Style background with rounded corners and custom color
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(context.getResources().getColor(R.color.backgroundSecondary));
            float radius = 16 * context.getResources().getDisplayMetrics().density;
            bg.setCornerRadius(radius);
            popup.setBackgroundDrawable(bg);
            popup.setElevation(16 * context.getResources().getDisplayMetrics().density);
            popup.setOutsideTouchable(true);

            // Add a close button row
            TextView closeBtn = new TextView(context);
            closeBtn.setText("Close");
            closeBtn.setTextColor(context.getResources().getColor(R.color.my_primary));
            closeBtn.setPadding(0, padding, 0, 0);
            closeBtn.setGravity(android.view.Gravity.END);
            closeBtn.setOnClickListener(v -> popup.dismiss());
            container.addView(closeBtn);

            // Show centered over root view
            popup.showAtLocation(anchor, android.view.Gravity.CENTER, 0, 0);
        });
    }
}
