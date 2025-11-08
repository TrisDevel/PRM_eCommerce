package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.adapters.BannerAdminAdapter;
import com.example.newEcom.model.BannerModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.List;

public class BannerListAdminActivity extends AppCompatActivity {
    private RecyclerView bannersRecyclerView;
    private BannerAdminAdapter adapter;
    private MaterialSearchBar searchBar;

    private List<BannerModel> bannerList = new ArrayList<>();
    private List<String> documentIds = new ArrayList<>();
    private List<BannerModel> fullBannerList = new ArrayList<>();
    private List<String> fullDocumentIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banner_list_admin);

        bannersRecyclerView = findViewById(R.id.bannersRecyclerView);
        ImageView backBtn = findViewById(R.id.backBtn);
        searchBar = findViewById(R.id.searchBar);

        backBtn.setOnClickListener(v -> onBackPressed());

        setupRecyclerView();
        setupSearchBar();
        loadBanners();
    }

    private void setupRecyclerView() {
        adapter = new BannerAdminAdapter(this, bannerList, documentIds);
        bannersRecyclerView.setAdapter(adapter);
    }

    private void setupSearchBar() {
        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBanners(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadBanners() {
        FirebaseUtil.getBanner().orderBy("bannerId").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fullBannerList.clear();
                        fullDocumentIds.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            fullBannerList.add(document.toObject(BannerModel.class));
                            fullDocumentIds.add(document.getId());
                        }
                        filterBanners("");
                    } else {
                        Toast.makeText(BannerListAdminActivity.this, "Failed to load banners.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterBanners(String query) {
        bannerList.clear();
        documentIds.clear();
        if (query.isEmpty()) {
            bannerList.addAll(fullBannerList);
            documentIds.addAll(fullDocumentIds);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (int i = 0; i < fullBannerList.size(); i++) {
                BannerModel banner = fullBannerList.get(i);
                // Search by banner ID
                if (String.valueOf(banner.getBannerId()).contains(lowerCaseQuery)) {
                    bannerList.add(banner);
                    documentIds.add(fullDocumentIds.get(i));
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}