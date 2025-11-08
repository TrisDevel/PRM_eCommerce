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
import com.example.newEcom.activities.ModifyBannerActivity;
import com.example.newEcom.model.BannerModel;
import com.squareup.picasso.Picasso;
import java.util.List;

public class BannerAdminAdapter extends RecyclerView.Adapter<BannerAdminAdapter.BannerAdminViewHolder> {
    private Context context;
    private List<BannerModel> bannerList;
    private List<String> documentIds;

    public BannerAdminAdapter(Context context, List<BannerModel> bannerList, List<String> documentIds) {
        this.context = context;
        this.bannerList = bannerList;
        this.documentIds = documentIds;
    }

    @NonNull
    @Override
    public BannerAdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner_admin, parent, false);
        return new BannerAdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerAdminViewHolder holder, int position) {
        BannerModel banner = bannerList.get(position);
        String docId = documentIds.get(position);

        holder.bannerId.setText("ID: " + banner.getBannerId());

        if (banner.getBannerImage() != null && !banner.getBannerImage().isEmpty()) {
            Picasso.get().load(banner.getBannerImage()).into(holder.bannerImage);
        }

        holder.modifyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, ModifyBannerActivity.class);
            intent.putExtra("documentId", docId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    static class BannerAdminViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImage;
        TextView bannerId;
        Button modifyBtn;

        public BannerAdminViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImage = itemView.findViewById(R.id.bannerImageView);
            bannerId = itemView.findViewById(R.id.bannerIdTextView);
            modifyBtn = itemView.findViewById(R.id.modifyBtn);
        }
    }
}
