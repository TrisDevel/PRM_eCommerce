package com.example.newEcom.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.newEcom.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DeliveryActivity extends AppCompatActivity {

    private TextView orderIdTextView, statusTextView, addressTextView, phoneTextView, nameTextView;
    private TextView estimatedDeliveryTextView, deliveryDateTextView;
    private CardView statusCardView;
    private ImageView backButton, statusIcon;
    private View statusIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery);

        // Get order data from intent
        int orderId = getIntent().getIntExtra("orderId", 0);
        String orderStatus = getIntent().getStringExtra("orderStatus");
        String orderAddress = getIntent().getStringExtra("orderAddress");
        String orderPhone = getIntent().getStringExtra("orderPhone");
        String orderName = getIntent().getStringExtra("orderName");
        int orderTotal = getIntent().getIntExtra("orderTotal", 0);

        // Initialize views
        initializeViews();

        // Set order data
        orderIdTextView.setText("Order #" + orderId);
        nameTextView.setText(orderName != null ? orderName : "N/A");
        phoneTextView.setText(orderPhone != null ? orderPhone : "N/A");
        addressTextView.setText(orderAddress != null ? orderAddress : "N/A");

        // Set status and update UI
        updateDeliveryStatus(orderStatus);

        // Calculate estimated delivery date (3-5 days from now)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 3);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault());
        String estimatedDate = dateFormat.format(calendar.getTime());
        estimatedDeliveryTextView.setText(estimatedDate);

        calendar.add(Calendar.DAY_OF_MONTH, 2);
        String deliveryDate = dateFormat.format(calendar.getTime());
        deliveryDateTextView.setText(deliveryDate);

        // Back button
        backButton.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        orderIdTextView = findViewById(R.id.orderIdTextView);
        statusTextView = findViewById(R.id.statusTextView);
        addressTextView = findViewById(R.id.addressTextView);
        phoneTextView = findViewById(R.id.phoneTextView);
        nameTextView = findViewById(R.id.nameTextView);
        estimatedDeliveryTextView = findViewById(R.id.estimatedDeliveryTextView);
        deliveryDateTextView = findViewById(R.id.deliveryDateTextView);
        statusCardView = findViewById(R.id.statusCardView);
        backButton = findViewById(R.id.backButton);
        statusIcon = findViewById(R.id.statusIcon);
        statusIndicator = findViewById(R.id.statusIndicator);
    }

    private void updateDeliveryStatus(String status) {
        if (status == null) status = "Pending";

        statusTextView.setText(status);

        // Update UI based on status
        switch (status.toUpperCase()) {
            case "PENDING":
            case "CONFIRMED":
                statusCardView.setCardBackgroundColor(getResources().getColor(R.color.orange_light));
                statusTextView.setTextColor(getResources().getColor(R.color.orange));
                statusIndicator.setBackgroundColor(getResources().getColor(R.color.orange));
                statusIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
                break;
            case "SHIPPING":
                statusCardView.setCardBackgroundColor(getResources().getColor(R.color.blue_light));
                statusTextView.setTextColor(getResources().getColor(R.color.blue));
                statusIndicator.setBackgroundColor(getResources().getColor(R.color.blue));
                statusIcon.setImageResource(android.R.drawable.ic_menu_directions);
                break;
            case "DELIVERED":
                statusCardView.setCardBackgroundColor(getResources().getColor(R.color.green_light));
                statusTextView.setTextColor(getResources().getColor(R.color.green));
                statusIndicator.setBackgroundColor(getResources().getColor(R.color.green));
                statusIcon.setImageResource(android.R.drawable.checkbox_on_background);
                break;
            case "CANCELLED":
                statusCardView.setCardBackgroundColor(getResources().getColor(R.color.red_light));
                statusTextView.setTextColor(getResources().getColor(R.color.red));
                statusIndicator.setBackgroundColor(getResources().getColor(R.color.red));
                statusIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                break;
            default:
                statusCardView.setCardBackgroundColor(getResources().getColor(R.color.grey_light));
                statusTextView.setTextColor(getResources().getColor(R.color.grey));
                statusIndicator.setBackgroundColor(getResources().getColor(R.color.grey));
                statusIcon.setImageResource(android.R.drawable.ic_menu_info_details);
                break;
        }
    }
}

