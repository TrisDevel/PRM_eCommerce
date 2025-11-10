package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

public class AdminActivity extends AppCompatActivity {
    private static final String TAG = "AdminScreen";

    LinearLayout logoutBtn;
    CardView addProductBtn, modifyProductBtn, addCategoryBtn, modifyCategoryBtn, addBannerBtn, modifyBannerBtn, userManagementBtn, orderManagementBtn;
    TextView countOrders, priceOrders; // usersTextView;

    CardView sendNotificationBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.wtf(TAG, "onCreate ENTER");
        setContentView(R.layout.activity_admin);

        bindViews();
        wireClicks();

        sendNotificationBtn = findViewById(R.id.sendNotificationBtn);
        if (sendNotificationBtn != null)
            sendNotificationBtn.setOnClickListener(v -> startActivity(new Intent(this, SendNotificationActivity.class)));


        getDetails();
    }

    private void bindViews() {
        logoutBtn         = findViewById(R.id.logoutBtn);
        addProductBtn     = findViewById(R.id.addProductBtn);
        modifyProductBtn  = findViewById(R.id.modifyProductBtn);
        addCategoryBtn    = findViewById(R.id.addCategoryBtn);
        modifyCategoryBtn = findViewById(R.id.modifyCategoryBtn);
        addBannerBtn      = findViewById(R.id.addBannerBtn);
        modifyBannerBtn   = findViewById(R.id.modifyBannerBtn);
        userManagementBtn = findViewById(R.id.userManagementBtn);
        orderManagementBtn = findViewById(R.id.orderManagementBtn);
        countOrders       = findViewById(R.id.countOrders);
        priceOrders       = findViewById(R.id.priceOrders);
        // usersTextView  = findViewById(R.id.usersTextView);

        // Log để xác nhận view có tồn tại trong layout không
        Log.wtf(TAG, "bindViews: "
                + "logoutBtn=" + (logoutBtn!=null) + ", addProductBtn=" + (addProductBtn!=null)
                + ", modifyProductBtn=" + (modifyProductBtn!=null)
                + ", addCategoryBtn=" + (addCategoryBtn!=null)
                + ", modifyCategoryBtn=" + (modifyCategoryBtn!=null)
                + ", addBannerBtn=" + (addBannerBtn!=null)
                + ", modifyBannerBtn=" + (modifyBannerBtn!=null)
                + ", countOrders=" + (countOrders!=null)
                + ", priceOrders=" + (priceOrders!=null));

        // Nếu thiếu ID nào trong layout -> cảnh báo rõ
        if (logoutBtn == null || addProductBtn == null || modifyProductBtn == null
                || addCategoryBtn == null || modifyCategoryBtn == null
                || addBannerBtn == null || modifyBannerBtn == null
                || countOrders == null || priceOrders == null) {
            Toast.makeText(this, "activity_admin thiếu 1 số view ID. Kiểm tra các id trong layout!", Toast.LENGTH_LONG).show();
        }
    }

    private void wireClicks() {
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
                Log.wtf(TAG, "Logout clicked");
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(this, SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        if (addProductBtn != null)     addProductBtn.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
        if (modifyProductBtn != null)  modifyProductBtn.setOnClickListener(v -> startActivity(new Intent(this, ProductListAdminActivity.class)));
        if (addCategoryBtn != null)    addCategoryBtn.setOnClickListener(v -> startActivity(new Intent(this, AddCategoryActivity.class)));
        if (modifyCategoryBtn != null) modifyCategoryBtn.setOnClickListener(v -> startActivity(new Intent(this, CategoryListAdminActivity.class)));
        if (addBannerBtn != null)      addBannerBtn.setOnClickListener(v -> startActivity(new Intent(this, AddBannerActivity.class)));
        if (modifyBannerBtn != null)   modifyBannerBtn.setOnClickListener(v -> startActivity(new Intent(this, BannerListAdminActivity.class)));
        if (userManagementBtn != null) userManagementBtn.setOnClickListener(v -> startActivity(new Intent(this, UserManagementActivity.class)));
        if (orderManagementBtn != null) orderManagementBtn.setOnClickListener(v -> startActivity(new Intent(this, OrderManagementActivity.class)));
    }

    private void getDetails() {
        Log.wtf(TAG, "getDetails: fetching admin dashboard numbers...");
        FirebaseUtil.getDetails().get()
                .addOnCompleteListener(this::onDetailsLoaded)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getDetails FAILED", e);
                    safeSetText(countOrders, "—");
                    safeSetText(priceOrders, "—");
                });
    }

    private void onDetailsLoaded(@NonNull Task<DocumentSnapshot> task) {
        Log.wtf(TAG, "getDetails onComplete: success=" + task.isSuccessful());
        if (!task.isSuccessful()) {
            Exception e = task.getException();
            Log.e(TAG, "Firestore task not successful", e);
            safeSetText(countOrders, "—");
            safeSetText(priceOrders, "—");
            return;
        }

        DocumentSnapshot doc = task.getResult();
        if (doc == null) {
            Log.e(TAG, "DocumentSnapshot is null");
            safeSetText(countOrders, "—");
            safeSetText(priceOrders, "—");
            return;
        }

        Log.wtf(TAG, "doc.exists=" + doc.exists() + ", id=" + doc.getId());
        Object countObj = doc.get("countOfOrderedItems");
        Object priceObj = doc.get("priceOfOrders");

        Log.wtf(TAG, "countOfOrderedItems=" + countObj + " (" + (countObj!=null ? countObj.getClass().getSimpleName() : "null") + ")");
        Log.wtf(TAG, "priceOfOrders=" + priceObj + " (" + (priceObj!=null ? priceObj.getClass().getSimpleName() : "null") + ")");

        // Đổi sang text an toàn theo kiểu dữ liệu thực tế trong Firestore
        String countText = toNumberString(countObj);
        String priceText = toNumberString(priceObj);

        safeSetText(countOrders, countText);
        safeSetText(priceOrders, priceText);
    }

    private void safeSetText(TextView tv, String text) {
        if (tv == null) {
            Log.e(TAG, "safeSetText called with null TextView. text=" + text);
            return;
        }
        tv.setText(text != null ? text : "—");
    }

    private String toNumberString(Object o) {
        if (o == null) return "0";
        try {
            if (o instanceof Long)   return String.valueOf((Long) o);
            if (o instanceof Integer)return String.valueOf((Integer) o);
            if (o instanceof Double) return String.valueOf(((Double) o).longValue());
            if (o instanceof Float)  return String.valueOf(((Float) o).longValue());
            return o.toString();
        } catch (Exception e) {
            Log.e(TAG, "toNumberString parse error for value=" + o, e);
            return o.toString();
        }
    }
}