package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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
import com.example.newEcom.model.BannerModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class ModifyBannerActivity extends AppCompatActivity {

    LinearLayout detailsLinearLayout;
    TextInputEditText descEditText;
    Button imageBtn, modifyBannerBtn;
    ImageView backBtn, bannerImageView;
    TextView removeImageBtn;

    AutoCompleteTextView idDropDown, statusDropDown;
    ArrayAdapter<String> idAdapter, statusAdapter;
    BannerModel currBanner;
    String status, docId, bannerImage;
    Uri imageUri;
    int bannerId;
    Context context = this;
    boolean imageUploaded = true;

    SweetAlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_banner);

        // --- Init views ---
        detailsLinearLayout = findViewById(R.id.detailsLinearLayout);
        idDropDown = findViewById(R.id.idDropDown);
        idDropDown.setVisibility(View.GONE);
        descEditText = findViewById(R.id.descriptionEditText);
        statusDropDown = findViewById(R.id.statusDropDown);
        bannerImageView = findViewById(R.id.bannerImageView);
        imageBtn = findViewById(R.id.imageBtn);
        modifyBannerBtn = findViewById(R.id.modifyBannerBtn);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);

        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Uploading image...");
        dialog.setCancelable(false);

        // --- Click listeners ---
        backBtn.setOnClickListener(v -> onBackPressed());
        imageBtn.setOnClickListener(v -> pickImage());
        removeImageBtn.setOnClickListener(v -> removeImage());
        modifyBannerBtn.setOnClickListener(v -> updateToFirebase());

        docId = getIntent().getStringExtra("documentId");
        if (docId == null) {
            Toast.makeText(this, "Banner ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadBannerDetails();
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
    }

    private void loadBannerDetails() {
        FirebaseUtil.getBanner().document(docId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                BannerModel banner = task.getResult().toObject(BannerModel.class);
                if (banner != null) {
                    initBanner(banner);
                }
            } else {
                Toast.makeText(context, "Failed to load banner details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initBanner(BannerModel model) {
        currBanner = model;
        bannerId = currBanner.getBannerId();

        // Load image
        Picasso.get().load(currBanner.getBannerImage()).into(bannerImageView);

        // Show details layout
        detailsLinearLayout.setVisibility(View.VISIBLE);
        bannerImageView.setVisibility(View.VISIBLE);
        removeImageBtn.setVisibility(View.VISIBLE);

        descEditText.setText(currBanner.getDescription());

        // --- Status dropdown ---
        String[] statusOptions = {"Live", "Not Live"};
        statusAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, statusOptions);
        statusDropDown.setAdapter(statusAdapter);

        // Set default value
        if(currBanner.getStatus().equals("Active") || currBanner.getStatus().equals("Inactive")){
            statusDropDown.setText(currBanner.getStatus(), false); // false = không trigger filter
        }

        statusDropDown.setOnItemClickListener((parent, view, position, id) -> {
            status = parent.getItemAtPosition(position).toString();
            Toast.makeText(context, "Selected: " + status, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateToFirebase() {
        if (!validate()) return;

        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Loading...");
        dialog.setCancelable(false);
        dialog.show();

        if (imageUri != null) {
            FirebaseUtil.getBannerImageReference(bannerId + "").putFile(imageUri)
                    .addOnCompleteListener(t -> FirebaseUtil.getBannerImageReference(bannerId + "").getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                bannerImage = uri.toString();
                                FirebaseUtil.getBanner().document(docId).update("bannerImage", bannerImage);
                                updateDataToFirebase();
                                dialog.dismiss();
                                Toast.makeText(context, "Banner modified successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            }));
        } else {
            updateDataToFirebase();
            dialog.dismiss();
            Toast.makeText(context, "Banner modified successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void updateDataToFirebase() {
        if (!descEditText.getText().toString().equals(currBanner.getDescription()))
            FirebaseUtil.getBanner().document(docId).update("description", descEditText.getText().toString());
        if (!statusDropDown.getText().toString().equals(currBanner.getStatus()))
            FirebaseUtil.getBanner().document(docId).update("status", statusDropDown.getText().toString());
    }

    private boolean validate() {
        boolean isValid = true;
        if (statusDropDown.getText().toString().trim().isEmpty()) {
            statusDropDown.setError("Status is required");
            isValid = false;
        }
        if (descEditText.getText().toString().trim().isEmpty()) {
            descEditText.setError("Description is required");
            isValid = false;
        }
        if (!imageUploaded) {
            Toast.makeText(context, "Image is not selected", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        return isValid;
    }

    private void removeImage() {
        SweetAlertDialog alertDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        alertDialog
                .setTitleText("Are you sure?")
                .setContentText("Do you want to remove this image?")
                .setConfirmText("Yes")
                .setCancelText("No")
                .setConfirmClickListener(dialog -> {
                    imageUploaded = false;
                    bannerImageView.setImageDrawable(null);
                    bannerImageView.setVisibility(View.GONE);
                    removeImageBtn.setVisibility(View.GONE);
                    dialog.cancel();
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageUploaded = true;
            dialog.show();

            Picasso.get().load(imageUri).into(bannerImageView, new Callback() {
                @Override
                public void onSuccess() {
                    dialog.dismiss();
                }

                @Override
                public void onError(Exception e) { }
            });

            bannerImageView.setVisibility(View.VISIBLE);
            removeImageBtn.setVisibility(View.VISIBLE);
        }
    }


}
