package com.example.newEcom.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.model.BannerModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AddBannerActivity extends AppCompatActivity {

    TextInputEditText idEditText, descEditText;
    AutoCompleteTextView statusDropDown;
    Button imageBtn, addBannerBtn;
    ImageView backBtn, bannerImageView;
    TextView removeImageBtn;

    SweetAlertDialog dialog;

    String bannerImageUrl, status;
    int bannerId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_banner);

        // Bind views
        idEditText       = findViewById(R.id.idEditText);
        descEditText     = findViewById(R.id.descEditText);
        statusDropDown   = findViewById(R.id.statusDropDown);
        imageBtn         = findViewById(R.id.imageBtn);
        addBannerBtn     = findViewById(R.id.addBannerBtn);
        bannerImageView  = findViewById(R.id.bannerImageView);
        backBtn          = findViewById(R.id.backBtn);
        removeImageBtn   = findViewById(R.id.removeImageBtn);

        // Progress dialog
        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Please wait...");
        dialog.setCancelable(false);

        // Load next bannerId
        loadNextBannerId();

        // Setup status dropdown
        setupStatusDropdown();

        // Click handlers
        imageBtn.setOnClickListener(v -> showImageUrlDialog());
        removeImageBtn.setOnClickListener(v -> removeImage());
        addBannerBtn.setOnClickListener(v -> addBannerToFirebase());
        backBtn.setOnClickListener(v -> onBackPressed());
    }

    private void loadNextBannerId() {
        FirebaseUtil.getBanner()
                .orderBy("bannerId", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        bannerId = 1;
                        idEditText.setText(String.valueOf(bannerId));
                        return;
                    }
                    QuerySnapshot qs = task.getResult();
                    if (qs == null || qs.isEmpty()) {
                        bannerId = 1;
                    } else {
                        DocumentSnapshot lastDoc = qs.getDocuments().get(0);
                        Long last = lastDoc.getLong("bannerId");
                        bannerId = (last == null ? 0 : last.intValue()) + 1;
                    }
                    idEditText.setText(String.valueOf(bannerId));
                });
    }

    private void setupStatusDropdown() {
        String[] statusList = {"Live", "Not Live"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, statusList);
        statusDropDown.setAdapter(adapter);

        statusDropDown.setOnItemClickListener((parent, view, position, id) -> {
            status = (String) parent.getItemAtPosition(position);
        });
    }

    private void showImageUrlDialog() {
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("https://example.com/banner.jpg");

        new AlertDialog.Builder(this)
                .setTitle("Paste Image URL")
                .setView(input)
                .setPositiveButton("Set", (dialogInterface, i) -> {
                    String url = (input.getText() == null) ? "" : input.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(this, "URL is empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    bannerImageUrl = url;

                    bannerImageView.setVisibility(View.VISIBLE);
                    removeImageBtn.setVisibility(View.VISIBLE);
                    Picasso.get()
                            .load(bannerImageUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(bannerImageView, new Callback() {
                                @Override public void onSuccess() {}
                                @Override public void onError(Exception e) {
                                    Toast.makeText(AddBannerActivity.this, "Cannot load image from URL", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeImage() {
        bannerImageUrl = null;
        bannerImageView.setImageDrawable(null);
        bannerImageView.setVisibility(View.GONE);
        removeImageBtn.setVisibility(View.GONE);
    }

    private boolean validate() {
        boolean isValid = true;
        if (Objects.requireNonNull(idEditText.getText()).toString().trim().isEmpty()) {
            idEditText.setError("ID is required");
            isValid = false;
        }
        if (Objects.requireNonNull(descEditText.getText()).toString().trim().isEmpty()) {
            descEditText.setError("Description is required");
            isValid = false;
        }
        if (status == null || status.trim().isEmpty()) {
            Toast.makeText(this, "Please select status", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        if (bannerImageUrl == null || bannerImageUrl.trim().isEmpty()) {
            Toast.makeText(this, "Please set banner image URL", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        return isValid;
    }

    private void addBannerToFirebase() {
        if (!validate()) return;

        String desc = descEditText.getText().toString().trim();

        BannerModel banner = new BannerModel(bannerId, desc, bannerImageUrl, status);

        FirebaseUtil.getBanner()
                .add(banner)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Banner added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Add banner failed!", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
