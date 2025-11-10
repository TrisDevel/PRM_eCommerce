package com.example.newEcom.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.newEcom.R;
import com.example.newEcom.model.BannerModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class ModifyBannerActivity extends AppCompatActivity {

    LinearLayout detailsLinearLayout;
    TextInputEditText descEditText, imageBtn; // imageBtn giờ là input URL
    AutoCompleteTextView idDropDown, statusDropDown;
    ArrayAdapter<String> idAdapter, statusAdapter;
    ImageView backBtn, bannerImageView;
    TextView removeImageBtn;

    BannerModel currBanner;
    String status, docId, bannerImageUrl;
    int bannerId;
    Context context = this;
    SweetAlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_banner);

        // --- Init views ---
        detailsLinearLayout = findViewById(R.id.detailsLinearLayout);
        idDropDown = findViewById(R.id.idDropDown);
        descEditText = findViewById(R.id.descriptionEditText);
        imageBtn = findViewById(R.id.imageBtn); // input URL
        statusDropDown = findViewById(R.id.statusDropDown);
        bannerImageView = findViewById(R.id.bannerImageView);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);

        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Loading...");
        dialog.setCancelable(false);

        backBtn.setOnClickListener(v -> onBackPressed());
        removeImageBtn.setOnClickListener(v -> removeImage());

        initDropDown((bannerList, docIdList) -> {
            String[] ids = new String[bannerList.size()];
            for (int i = 0; i < bannerList.size(); i++)
                ids[i] = Integer.toString(bannerList.get(i).getBannerId());

            idAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, ids);
            idDropDown.setAdapter(idAdapter);

            idDropDown.setOnItemClickListener((parent, view, position, id) -> {
                docId = docIdList.get(position);
                initBanner(bannerList.get(position));
            });
        });

        // --- Khi người dùng nhập URL ảnh ---
        imageBtn.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                String url = s.toString().trim();
                if (!url.isEmpty()) {
                    loadImage(url);
                } else {
                    bannerImageView.setVisibility(View.GONE);
                    removeImageBtn.setVisibility(View.GONE);
                }
            }
        });

        findViewById(R.id.modifyBannerBtn).setOnClickListener(v -> updateToFirebase());
    }

    private void initDropDown(MyCallback myCallback) {
        FirebaseUtil.getBanner().orderBy("bannerId").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<BannerModel> banners = new ArrayList<>();
                        List<String> docIds = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            banners.add(doc.toObject(BannerModel.class));
                            docIds.add(doc.getId());
                        }
                        myCallback.onCallback(banners, docIds);
                    }
                });
    }

    private void initBanner(BannerModel model) {
        currBanner = model;
        bannerId = currBanner.getBannerId();

        detailsLinearLayout.setVisibility(View.VISIBLE);

        descEditText.setText(currBanner.getDescription());
        imageBtn.setText(currBanner.getBannerImage()); // hiển thị URL hiện tại
        loadImage(currBanner.getBannerImage());

        // --- Status dropdown ---
        String[] statusOptions = {"Active", "Inactive"};
        statusAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, statusOptions);
        statusDropDown.setAdapter(statusAdapter);

        statusDropDown.setText(currBanner.getStatus(), false);
        statusDropDown.setOnItemClickListener((parent, view, position, id) -> {
            status = parent.getItemAtPosition(position).toString();
            Toast.makeText(context, "Selected: " + status, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadImage(String url) {
        dialog.show();
        Picasso.get().load(url).into(bannerImageView, new Callback() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
                bannerImageView.setVisibility(View.VISIBLE);
                removeImageBtn.setVisibility(View.VISIBLE);
                bannerImageUrl = url;
            }
            @Override
            public void onError(Exception e) {
                dialog.dismiss();
                Toast.makeText(context, "Invalid image URL", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeImage() {
        SweetAlertDialog alertDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        alertDialog
                .setTitleText("Are you sure?")
                .setContentText("Do you want to remove this image?")
                .setConfirmText("Yes")
                .setCancelText("No")
                .setConfirmClickListener(dialog -> {
                    bannerImageUrl = null;
                    imageBtn.setText("");
                    bannerImageView.setImageDrawable(null);
                    bannerImageView.setVisibility(View.GONE);
                    removeImageBtn.setVisibility(View.GONE);
                    dialog.cancel();
                }).show();
    }

    private void updateToFirebase() {
        if (!validate()) return;

        dialog.show();

        if (!imageBtn.getText().toString().equals(currBanner.getBannerImage()))
            FirebaseUtil.getBanner().document(docId).update("bannerImage", imageBtn.getText().toString());
        if (!descEditText.getText().toString().equals(currBanner.getDescription()))
            FirebaseUtil.getBanner().document(docId).update("description", descEditText.getText().toString());
        if (!statusDropDown.getText().toString().equals(currBanner.getStatus()))
            FirebaseUtil.getBanner().document(docId).update("status", statusDropDown.getText().toString());

        dialog.dismiss();
        Toast.makeText(context, "Banner updated successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean validate() {
        boolean valid = true;
        if (descEditText.getText().toString().trim().isEmpty()) {
            descEditText.setError("Description required");
            valid = false;
        }
        if (statusDropDown.getText().toString().trim().isEmpty()) {
            statusDropDown.setError("Status required");
            valid = false;
        }
        if (imageBtn.getText().toString().trim().isEmpty()) {
            imageBtn.setError("Image URL required");
            valid = false;
        }
        return valid;
    }

    public interface MyCallback {
        void onCallback(List<BannerModel> banners, List<String> docIds);
    }
}
