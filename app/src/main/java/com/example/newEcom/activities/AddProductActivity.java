package com.example.newEcom.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.model.ProductModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AddProductActivity extends AppCompatActivity {
    private static final String TAG = "AddProduct";

    // Views
    TextInputEditText idEditText, nameEditText, descEditText, specEditText, stockEditText, priceEditText, discountEditText;
    Button imageBtn, addProductBtn;
    ImageView backBtn, productImageView;
    TextView removeImageBtn;
    AutoCompleteTextView categoryDropDown;

    // Data
    ArrayAdapter<String> arrayAdapter;
    String category, productImage, shareLink, productName;
    int productId = 0;

    SweetAlertDialog dialog; // dùng hiển thị trạng thái chung (không upload)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // Bind
        idEditText        = findViewById(R.id.idEditText);
        nameEditText      = findViewById(R.id.nameEditText);
        categoryDropDown  = findViewById(R.id.categoryDropDown);
        descEditText      = findViewById(R.id.descriptionEditText);
        specEditText      = findViewById(R.id.specificationEditText);
        stockEditText     = findViewById(R.id.stockEditText);
        priceEditText     = findViewById(R.id.priceEditText);
        discountEditText  = findViewById(R.id.discountEditText);
        productImageView  = findViewById(R.id.productImageView);
        imageBtn          = findViewById(R.id.imageBtn);
        addProductBtn     = findViewById(R.id.addProductBtn);
        backBtn           = findViewById(R.id.backBtn);
        removeImageBtn    = findViewById(R.id.removeImageBtn);

        // Progress dialog chung
        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Please wait...");
        dialog.setCancelable(false);

        // Load next productId = (max productId in products) + 1
        loadNextProductId();

        // Load categories for dropdown
        loadCategories();

        // Handlers
        imageBtn.setOnClickListener(v -> showImageUrlDialog()); // đổi: nhập URL thay vì chọn file
        addProductBtn.setOnClickListener(v -> generateDynamicLink());
        backBtn.setOnClickListener(v -> onBackPressed());
        removeImageBtn.setOnClickListener(v -> removeImage());
    }

    // === Fetch next productId (max + 1) ===
    private void loadNextProductId() {
        FirebaseUtil.getProducts()
                .orderBy("productId", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "loadNextProductId failed", task.getException());
                        productId = (productId > 0) ? productId : 1;
                        idEditText.setText(String.valueOf(productId));
                        return;
                    }
                    QuerySnapshot qs = task.getResult();
                    if (qs == null || qs.isEmpty()) {
                        productId = 1;
                        idEditText.setText(String.valueOf(productId));
                        Log.d(TAG, "No products -> productId=1");
                        return;
                    }
                    DocumentSnapshot lastDoc = qs.getDocuments().get(0);
                    Long last = lastDoc.getLong("productId");
                    int base = (last == null) ? 0 : last.intValue();
                    productId = base + 1;
                    idEditText.setText(String.valueOf(productId));
                    Log.d(TAG, "Last productId=" + base + " -> next=" + productId);
                });
    }

    // === Categories ===
    private void loadCategories() {
        FirebaseUtil.getCategories().orderBy("name").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "loadCategories failed", task.getException());
                arrayAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, new ArrayList<>());
                categoryDropDown.setAdapter(arrayAdapter);
                return;
            }
            QuerySnapshot qs = task.getResult();
            List<String> list = new ArrayList<>();
            if (qs != null) {
                for (DocumentSnapshot d : qs.getDocuments()) {
                    String name = d.getString("name");
                    if (name != null) list.add(name);
                }
            }
            arrayAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, list);
            categoryDropDown.setAdapter(arrayAdapter);
            categoryDropDown.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
                category = (String) parent.getItemAtPosition(position);
            });
        });
    }

    // === Image via URL (không dùng Storage) ===
    private void showImageUrlDialog() {
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("https://example.com/image.jpg");

        new AlertDialog.Builder(this)
                .setTitle("Paste Image URL")
                .setView(input)
                .setPositiveButton("Set", (d, w) -> {
                    String url = (input.getText() == null) ? "" : input.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(this, "URL is empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    productImage = url;

                    // Preview
                    productImageView.setVisibility(View.VISIBLE);
                    removeImageBtn.setVisibility(View.VISIBLE);
                    Picasso.get()
                            .load(productImage)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(productImageView, new Callback() {
                                @Override public void onSuccess() { }
                                @Override public void onError(Exception e) {
                                    Toast.makeText(AddProductActivity.this, "Cannot load image from URL", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeImage() {
        productImage = null;
        productImageView.setImageDrawable(null);
        productImageView.setVisibility(View.GONE);
        removeImageBtn.setVisibility(View.GONE);
    }

    // === Validate & Submit ===
    private boolean validate() {
        boolean isValid = true;

        if (safeText(idEditText).isEmpty()) {
            idEditText.setError("Id is required");
            isValid = false;
        }
        if (safeText(nameEditText).isEmpty()) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        if (safeText(categoryDropDown).isEmpty()) {
            categoryDropDown.setError("Category is required");
            isValid = false;
        }
        if (safeText(descEditText).isEmpty()) {
            descEditText.setError("Description is required");
            isValid = false;
        }
        if (safeText(stockEditText).isEmpty()) {
            stockEditText.setError("Stock is required");
            isValid = false;
        }
        if (safeText(priceEditText).isEmpty()) {
            priceEditText.setError("Price is required");
            isValid = false;
        }
        if (safeText(discountEditText).isEmpty()) {
            discountEditText.setError("Discount is required");
            isValid = false;
        }
        if (productImage == null || productImage.trim().isEmpty()) {
            Toast.makeText(this, "Please set Image URL", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        return isValid;
    }

    private void generateDynamicLink() {
        if (!validate()) return;

        String titleForLink = safeText(nameEditText);
        if (titleForLink.isEmpty()) titleForLink = "Product " + (productId > 0 ? productId : 0);

        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://www.example.com/?product_id=" + productId))
                .setDomainUriPrefix("https://shopease.page.link")
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder("com.example.newEcom").build())
                .setSocialMetaTagParameters(new DynamicLink.SocialMetaTagParameters.Builder()
                        .setTitle(titleForLink)
                        .setImageUrl(Uri.parse(productImage)) // dùng URL ảnh đã dán
                        .build())
                .buildShortDynamicLink(ShortDynamicLink.Suffix.SHORT)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Uri shortLink = task.getResult().getShortLink();
                        shareLink = shortLink != null ? shortLink.toString() : null;
                        addToFirebase();
                    } else {
                        Log.e(TAG, "buildShortDynamicLink error", task.getException());
                        Toast.makeText(this, "Tạo link chia sẻ thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addToFirebase() {
        productName = safeText(nameEditText);
        String desc = safeText(descEditText);
        String spec = safeText(specEditText);

        if (category == null || category.trim().isEmpty()) {
            category = safeText(categoryDropDown);
        }

        int price    = safeInt(priceEditText, 0);
        int discount = safeInt(discountEditText, 0); // discount là số tiền giảm
        int stock    = safeInt(stockEditText, 0);
        int finalPrice = Math.max(0, price - discount);

        if (productImage == null || productImage.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập URL ảnh sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> sk = Arrays.asList(productName.trim().toLowerCase().split("\\s+"));

        ProductModel model = new ProductModel(
                productName, sk, productImage, category, desc, spec,
                price, discount, finalPrice, productId, stock, shareLink, 0f, 0
        );

        FirebaseUtil.getProducts().add(model)
                .addOnSuccessListener((DocumentReference ref) -> {
                    Toast.makeText(this, "Đã thêm sản phẩm!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "add product failed", e);
                    Toast.makeText(this, "Thêm sản phẩm thất bại", Toast.LENGTH_SHORT).show();
                });
    }

    // === Helpers ===
    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
    private String safeText(AutoCompleteTextView et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
    private int safeInt(TextInputEditText et, int def) {
        try {
            String s = safeText(et);
            return s.isEmpty() ? def : Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Không xóa gì ở Storage vì không upload
    }
}