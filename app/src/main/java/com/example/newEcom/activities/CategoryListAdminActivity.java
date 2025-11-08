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
import com.example.newEcom.adapters.CategoryAdminAdapter;
import com.example.newEcom.model.CategoryModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.List;

public class CategoryListAdminActivity extends AppCompatActivity {
    private RecyclerView categoriesRecyclerView;
    private CategoryAdminAdapter adapter;
    private MaterialSearchBar searchBar;

    private List<CategoryModel> categoryList = new ArrayList<>();
    private List<String> documentIds = new ArrayList<>();
    private List<CategoryModel> fullCategoryList = new ArrayList<>();
    private List<String> fullDocumentIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_list_admin);

        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        ImageView backBtn = findViewById(R.id.backBtn);
        searchBar = findViewById(R.id.searchBar);

        backBtn.setOnClickListener(v -> onBackPressed());

        setupRecyclerView();
        setupSearchBar();
        loadCategories();
    }

    private void setupRecyclerView() {
        adapter = new CategoryAdminAdapter(this, categoryList, documentIds);
        categoriesRecyclerView.setAdapter(adapter);
    }

    private void setupSearchBar() {
        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadCategories() {
        FirebaseUtil.getCategories().orderBy("categoryId").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fullCategoryList.clear();
                        fullDocumentIds.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            fullCategoryList.add(document.toObject(CategoryModel.class));
                            fullDocumentIds.add(document.getId());
                        }
                        filterCategories("");
                    } else {
                        Toast.makeText(CategoryListAdminActivity.this, "Failed to load categories.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterCategories(String query) {
        categoryList.clear();
        documentIds.clear();
        if (query.isEmpty()) {
            categoryList.addAll(fullCategoryList);
            documentIds.addAll(fullDocumentIds);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (int i = 0; i < fullCategoryList.size(); i++) {
                CategoryModel category = fullCategoryList.get(i);
                if (category.getName().toLowerCase().contains(lowerCaseQuery)) {
                    categoryList.add(category);
                    documentIds.add(fullDocumentIds.get(i));
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}