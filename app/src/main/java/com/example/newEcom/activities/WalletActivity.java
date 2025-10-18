package com.example.newEcom.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.newEcom.R;

public class WalletActivity extends AppCompatActivity {

    private TextView walletAmountTextView;
    private Button depositBtn, withdrawBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        walletAmountTextView = findViewById(R.id.walletAmountTextView);
        depositBtn = findViewById(R.id.depositBtn);
        withdrawBtn = findViewById(R.id.withdrawBtn);

        // Nút Deposit
        depositBtn.setOnClickListener(v -> {
            // Code thêm tiền vào ví
        });

        // Nút Withdraw
        withdrawBtn.setOnClickListener(v -> {
            // Code rút tiền từ ví
        });

        // Hiển thị số tiền tạm thời
        walletAmountTextView.setText("$100.00");
    }
}
