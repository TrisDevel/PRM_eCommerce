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
import com.example.newEcom.activities.ModifyCategoryActivity;
import com.example.newEcom.model.CategoryModel;
import com.squareup.picasso.Picasso;
import java.util.List;

public class CategoryAdminAdapter extends RecyclerView.Adapter<CategoryAdminAdapter.CategoryAdminViewHolder> {
    private Context context;
    private List<CategoryModel> categoryList;
    private List<String> documentIds;

    public CategoryAdminAdapter(Context context, List<CategoryModel> categoryList, List<String> documentIds) {
        this.context = context;
        this.categoryList = categoryList;
        this.documentIds = documentIds;
    }

    @NonNull
    @Override
    public CategoryAdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_admin, parent, false);
        return new CategoryAdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryAdminViewHolder holder, int position) {
        CategoryModel category = categoryList.get(position);
        String docId = documentIds.get(position);

        holder.categoryName.setText(category.getName());
        holder.categoryId.setText("ID: " + category.getCategoryId());

        if (category.getIcon() != null && !category.getIcon().isEmpty()) {
            Picasso.get().load(category.getIcon()).into(holder.categoryImage);
        }

        holder.modifyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, ModifyCategoryActivity.class);
            intent.putExtra("documentId", docId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class CategoryAdminViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryImage;
        TextView categoryName, categoryId;
        Button modifyBtn;

        public CategoryAdminViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.categoryImageView);
            categoryName = itemView.findViewById(R.id.categoryNameTextView);
            categoryId = itemView.findViewById(R.id.categoryIdTextView);
            modifyBtn = itemView.findViewById(R.id.modifyBtn);
        }
    }
}
