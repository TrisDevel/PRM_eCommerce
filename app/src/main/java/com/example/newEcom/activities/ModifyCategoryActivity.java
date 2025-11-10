package com.example.newEcom.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.model.CategoryModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ModifyCategoryActivity extends AppCompatActivity {
    LinearLayout detailsLinearLayout;
    TextInputEditText nameEditText, descEditText, colorEditText;
    Button imageBtn, modifyCategoryBtn;
    ImageView backBtn, categoryImageView;
    TextView removeImageBtn;

    AutoCompleteTextView idDropDown;
    ArrayAdapter<String> idAdapter;

    CategoryModel currCategory;
    String docId;
    String categoryImage; // URL ảnh đang dùng/được chọn
    int categoryId;

    Context context = this;

    SweetAlertDialog dialog; // progress nhỏ để load/preview ảnh

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_category);

        detailsLinearLayout = findViewById(R.id.detailsLinearLayout);
        idDropDown = findViewById(R.id.idDropDown);
        idDropDown.setVisibility(View.GONE); // Hide the dropdown
        nameEditText = findViewById(R.id.nameEditText);
        descEditText = findViewById(R.id.descriptionEditText);
        colorEditText = findViewById(R.id.colorEditText);

        categoryImageView = findViewById(R.id.categoryImageView);
        imageBtn = findViewById(R.id.imageBtn);
        modifyCategoryBtn = findViewById(R.id.modifyCategoryBtn);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);

        // Nút chọn ảnh: mở dialog dán URL thay vì ACTION_GET_CONTENT
        imageBtn.setOnClickListener(v -> showImageUrlDialog());

        modifyCategoryBtn.setOnClickListener(v -> updateToFirebase());
        backBtn.setOnClickListener(v -> onBackPressed());
        removeImageBtn.setOnClickListener(v -> removeImage());

        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Loading...");
        dialog.setCancelable(false);

        docId = getIntent().getStringExtra("documentId");
        if (docId == null) {
            Toast.makeText(this, "Category ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadCategoryDetails();
    }

    private void loadCategoryDetails() {
        FirebaseUtil.getCategories().document(docId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                CategoryModel category = task.getResult().toObject(CategoryModel.class);
                if (category != null) {
                    initCategory(category);
                }
            } else {
                Toast.makeText(context, "Failed to load category details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initCategory(CategoryModel model) {
        currCategory = model;
        categoryId = currCategory.getCategoryId();

        // Hiện progress chỉ khi cần, ở đây load ảnh nhanh -> không show cũng được
        // dialog.show();

        // Lưu URL hiện tại vào biến để nếu không đổi thì không update icon
        categoryImage = currCategory.getIcon();

        // Preview ảnh hiện tại (có placeholder)
        categoryImageView.setVisibility(View.VISIBLE);
        removeImageBtn.setVisibility(View.VISIBLE);
        Picasso.get()
                .load(categoryImage)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(categoryImageView, new Callback() {
                    @Override public void onSuccess() { /* dialog.dismiss(); */ }
                    @Override public void onError(Exception e) { /* dialog.dismiss(); */ }
                });

        detailsLinearLayout.setVisibility(View.VISIBLE);

        nameEditText.setText(currCategory.getName());
        descEditText.setText(currCategory.getBrief());
        colorEditText.setText(currCategory.getColor());
    }

    /** Mở hộp thoại để dán URL ảnh; preview ngay nếu hợp lệ */
    private void showImageUrlDialog() {
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("https://example.com/image.jpg");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

        new AlertDialog.Builder(this)
                .setTitle("Paste Image URL")
                .setView(input)
                .setPositiveButton("Set", (d, w) -> {
                    String url = (input.getText() == null) ? "" : input.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(this, "URL is empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    categoryImage = url;

                    // Preview ảnh mới
                    // dialog.show(); // thường không cần
                    categoryImageView.setVisibility(View.VISIBLE);
                    removeImageBtn.setVisibility(View.VISIBLE);
                    Picasso.get()
                            .load(categoryImage)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(categoryImageView, new Callback() {
                                @Override public void onSuccess() { /* dialog.dismiss(); */ }
                                @Override public void onError(Exception e) {
                                    Toast.makeText(ModifyCategoryActivity.this, "Cannot load image from URL", Toast.LENGTH_SHORT).show();
                                    /* dialog.dismiss(); */
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateToFirebase() {
        if (!validate()) return;

        SweetAlertDialog progress = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        progress.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        progress.setTitleText("Saving...");
        progress.setCancelable(false);
        progress.show();

        // Cập nhật icon nếu khác URL cũ
        if (categoryImage != null && !categoryImage.equals(currCategory.getIcon())) {
            FirebaseUtil.getCategories().document(docId).update("icon", categoryImage);
        }
        // Cập nhật các field text
        updateDataToFirebase();

        progress.dismissWithAnimation();
        Toast.makeText(context, "Category has been modified successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    void updateDataToFirebase() {
        String newName  = safeText(nameEditText);
        String newBrief = safeText(descEditText);
        String newColor = normalizeHex(safeText(colorEditText)); // chuẩn hóa # nếu thiếu

        if (!newName.equals(currCategory.getName()))
            FirebaseUtil.getCategories().document(docId).update("name", newName);

        if (!newBrief.equals(currCategory.getBrief()))
            FirebaseUtil.getCategories().document(docId).update("brief", newBrief);

        if (!newColor.equals(currCategory.getColor()))
            FirebaseUtil.getCategories().document(docId).update("color", newColor);
    }

    private void removeImage() {
        // Chỉ xóa ảnh trên UI và bỏ URL (không dính Storage)
        categoryImage = null;
        categoryImageView.setImageDrawable(null);
        categoryImageView.setVisibility(View.GONE);
        removeImageBtn.setVisibility(View.GONE);
    }

    boolean validate() {
        boolean isValid = true;

        if (safeText(nameEditText).isEmpty()) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        if (safeText(descEditText).isEmpty()) {
            descEditText.setError("Description is required");
            isValid = false;
        }

        String color = safeText(colorEditText);
        if (color.isEmpty()) {
            colorEditText.setError("Color is required");
            isValid = false;
        } else {
            // cho phép thiếu '#'
            String hex = normalizeHex(color);
            try {
                Color.parseColor(hex);
            } catch (Exception e) {
                colorEditText.setError("Color should be HEX value");
                isValid = false;
            }
        }

        // KHÔNG bắt buộc đổi ảnh khi sửa (có thể để nguyên icon cũ)
        // Nếu bạn muốn bắt buộc có ảnh (cũ hoặc mới), bật check sau:
        // if (categoryImage == null || categoryImage.trim().isEmpty()) { ... }

        return isValid;
    }

    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String normalizeHex(String value) {
        if (value == null) return "#FF9800";
        String v = value.trim();
        if (v.isEmpty()) return "#FF9800";
        if (!v.startsWith("#")) v = "#" + v;
        return v;
    }


}