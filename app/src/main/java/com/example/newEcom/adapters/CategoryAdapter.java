package com.example.newEcom.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.fragments.CategoryFragment;
import com.example.newEcom.model.CategoryModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.squareup.picasso.Picasso;

public class CategoryAdapter extends FirestoreRecyclerAdapter<CategoryModel, CategoryAdapter.CategoryViewHolder> {
    private final Context context;
    private AppCompatActivity activity;

    public CategoryAdapter(@NonNull FirestoreRecyclerOptions<CategoryModel> options, Context context){
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position, @NonNull CategoryModel category) {
        // Tên danh mục
        holder.categoryLabel.setText(category.getName() == null ? "" : category.getName());

        // Ảnh icon + placeholder để tránh crash khi URL null/invalid
        String iconUrl = category.getIcon();
        if (iconUrl == null || iconUrl.trim().isEmpty()) {
            holder.categoryImage.setImageResource(R.drawable.ic_image_placeholder); // tạo/đổi sang 1 vector trong res
        } else {
            Picasso.get()
                    .load(iconUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(holder.categoryImage);
        }

        // Màu nền an toàn (fallback nếu null/sai định dạng)
        holder.categoryImage.setBackgroundColor(safeParseColor(category.getColor()));

        // Điều hướng tới CategoryFragment
        Bundle bundle = new Bundle();
        bundle.putString("categoryName", category.getName());
        CategoryFragment fragment = new CategoryFragment();
        fragment.setArguments(bundle);

        holder.itemView.setOnClickListener(v -> {
            if (!fragment.isAdded() && activity != null && !activity.isFinishing()) {
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_frame_layout, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    /** Parse màu an toàn – hỗ trợ thiếu '#', null, hoặc sai định dạng. */
    private int safeParseColor(String color) {
        try {
            if (color == null) return Color.parseColor("#FF9800"); // fallback
            String v = color.trim();
            if (v.isEmpty()) return Color.parseColor("#FF9800");
            if (!v.startsWith("#")) v = "#" + v;
            return Color.parseColor(v);
        } catch (Exception e) {
            return Color.parseColor("#FF9800"); // fallback nếu sai định dạng
        }
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryLabel;
        ImageView categoryImage;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.categoryImage);
            categoryLabel = itemView.findViewById(R.id.categoryLabel);
        }
    }
}