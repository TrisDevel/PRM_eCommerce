package com.example.newEcom.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.UserDetailsActivity;
import com.example.newEcom.model.UserModel;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<UserModel> userList;
    private Context context;

    public UserAdapter(List<UserModel> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_adapter, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);
        
        // Set user data
        holder.userName.setText(user.getFullName() != null ? user.getFullName() : "No Name");
        holder.userEmail.setText(user.getEmail() != null ? user.getEmail() : "No Email");
        holder.userRole.setText(user.getRole() != null ? user.getRole() : "user");
        holder.userOrders.setText("Orders: " + user.getTotalOrders());
        holder.userSpent.setText("Spent: $" + user.getTotalSpent());
        
        // Set status
        if (user.isActive()) {
            holder.userStatus.setText("Active");
            holder.userStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.userStatus.setText("Inactive");
            holder.userStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        }
        
        // Set last login
        if (user.getLastLoginAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.userLastLogin.setText("Last login: " + sdf.format(user.getLastLoginAt().toDate()));
        } else {
            holder.userLastLogin.setText("Never logged in");
        }
        
        // Load profile image
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Picasso.get()
                    .load(user.getProfileImage())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(holder.userProfileImage);
        } else {
            holder.userProfileImage.setImageResource(R.drawable.ic_profile);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserDetailsActivity.class);
            intent.putExtra("userUid", user.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateUserList(List<UserModel> newUserList) {
        this.userList = newUserList;
        notifyDataSetChanged();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImage;
        TextView userName, userEmail, userRole, userStatus, userOrders, userSpent, userLastLogin;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfileImage = itemView.findViewById(R.id.userProfileImage);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
            userRole = itemView.findViewById(R.id.userRole);
            userStatus = itemView.findViewById(R.id.userStatus);
            userOrders = itemView.findViewById(R.id.userOrders);
            userSpent = itemView.findViewById(R.id.userSpent);
            userLastLogin = itemView.findViewById(R.id.userLastLogin);
        }
    }
}
