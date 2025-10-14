package com.example.newEcom.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log; // << add
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.fragments.ProductFragment;
import com.example.newEcom.model.ProductModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ProductAdapter extends FirestoreRecyclerAdapter<ProductModel, ProductAdapter.ProductViewHolder> {
    private static final String TAG = "ProductAdapter";
    private final Context context;
    private AppCompatActivity activity;

    public ProductAdapter(@NonNull FirestoreRecyclerOptions<ProductModel> options, Context context){
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position, @NonNull ProductModel product) {
        // --- price / labels ---
        holder.productLabel.setText(product.getName());
        holder.productPrice.setText("Rs. " + product.getPrice());
        holder.originalPrice.setText("Rs. " + product.getOriginalPrice());
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        int op = Math.max(product.getOriginalPrice(), 1); // tránh chia 0
        int discountPerc = (product.getDiscount() * 100) / op;
        holder.discountPercentage.setText(discountPerc + "% OFF");

        // --- image ---
        String url = product.getImage();
        Log.d(TAG, "Loading image url=" + url);

        // placeholder + error để tránh ô trắng
        Picasso.get()
                .load(url)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder) // tạo vector này rồi
                .error(R.drawable.ic_broken_image)            // và cái này nữa
                .into(holder.productImage, new Callback() {
                    private void stopShimmerIfNeeded() {
                        ShimmerFrameLayout shimmerLayout = activity.findViewById(R.id.shimmerLayout);
                        View main = activity.findViewById(R.id.mainLinearLayout);
                        if (shimmerLayout != null && main != null) {
                            shimmerLayout.stopShimmer();
                            shimmerLayout.setVisibility(View.GONE);
                            main.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override public void onSuccess() {
                        stopShimmerIfNeeded();
                    }

                    @Override public void onError(Exception e) {
                        Log.e(TAG, "Picasso load error: " + e.getMessage(), e);
                        stopShimmerIfNeeded();
                    }
                });

        holder.itemView.setOnClickListener(v -> {
            Fragment fragment = ProductFragment.newInstance(product);
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_frame_layout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder{
        TextView productLabel, productPrice, originalPrice, discountPercentage;
        ImageView productImage;
        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productListImage);
            productLabel = itemView.findViewById(R.id.productLabel);
            productPrice = itemView.findViewById(R.id.productPrice);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            discountPercentage = itemView.findViewById(R.id.discountPercentage);
        }
    }
}