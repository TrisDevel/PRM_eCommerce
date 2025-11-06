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
import com.example.newEcom.adapters.ProductAdminAdapter;
import com.example.newEcom.model.ProductModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.List;

public class ProductListAdminActivity extends AppCompatActivity {
    private RecyclerView productsRecyclerView;
    private ProductAdminAdapter adapter;
    private MaterialSearchBar searchBar;

    private List<ProductModel> productList = new ArrayList<>();
    private List<String> documentIds = new ArrayList<>();
    private List<ProductModel> fullProductList = new ArrayList<>();
    private List<String> fullDocumentIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list_admin);

        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        ImageView backBtn = findViewById(R.id.backBtn);
        searchBar = findViewById(R.id.searchBar);

        backBtn.setOnClickListener(v -> onBackPressed());

        setupRecyclerView();
        setupSearchBar();
        loadProducts();
    }

    private void setupRecyclerView() {
        adapter = new ProductAdminAdapter(this, productList, documentIds);
        productsRecyclerView.setAdapter(adapter);
    }

    private void setupSearchBar() {
        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadProducts() {
        FirebaseUtil.getProducts().orderBy("productId").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fullProductList.clear();
                        fullDocumentIds.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            fullProductList.add(document.toObject(ProductModel.class));
                            fullDocumentIds.add(document.getId());
                        }
                        // Initially, show all products
                        filterProducts("");
                    } else {
                        Toast.makeText(ProductListAdminActivity.this, "Failed to load products.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterProducts(String query) {
        productList.clear();
        documentIds.clear();
        if (query.isEmpty()) {
            productList.addAll(fullProductList);
            documentIds.addAll(fullDocumentIds);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (int i = 0; i < fullProductList.size(); i++) {
                ProductModel product = fullProductList.get(i);
                if (product.getName().toLowerCase().contains(lowerCaseQuery)) {
                    productList.add(product);
                    documentIds.add(fullDocumentIds.get(i));
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}