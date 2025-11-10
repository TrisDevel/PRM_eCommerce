package com.example.newEcom.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newEcom.R;
import com.example.newEcom.activities.ModifyProductActivity;
import com.example.newEcom.model.ProductModel;
import com.squareup.picasso.Picasso;
import java.util.List;

public class ProductAdminAdapter extends RecyclerView.Adapter<ProductAdminAdapter.ProductAdminViewHolder> {
    private Context context;
    private List<ProductModel> productList;
    private List<String> documentIds;

    public ProductAdminAdapter(Context context, List<ProductModel> productList, List<String> documentIds) {
        this.context = context;
        this.productList = productList;
        this.documentIds = documentIds;
    }

    @NonNull
    @Override
    public ProductAdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_admin, parent, false);
        return new ProductAdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductAdminViewHolder holder, int position) {
        ProductModel product = productList.get(position);
        String docId = documentIds.get(position);

        holder.productName.setText(product.getName());
        holder.productId.setText("ID: " + product.getProductId());
        holder.productPrice.setText("Price: " + product.getPrice());
        holder.productStock.setText("Stock: " + product.getStock());

        if (product.getImage() != null && !product.getImage().isEmpty()) {
            Picasso.get().load(product.getImage()).into(holder.productImage);
        }

        holder.modifyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, ModifyProductActivity.class);
            intent.putExtra("documentId", docId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductAdminViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productId, productPrice, productStock;
        Button modifyBtn;

        public ProductAdminViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImageView);
            productName = itemView.findViewById(R.id.productNameTextView);
            productId = itemView.findViewById(R.id.productIdTextView);
            productPrice = itemView.findViewById(R.id.productPriceTextView);
            productStock = itemView.findViewById(R.id.productStockTextView);
            modifyBtn = itemView.findViewById(R.id.modifyBtn);
        }
    }
}
