package com.example.newEcom.utils;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MoMoPayment {
    private static final String TAG = "MoMoPayment";
    private static final String MOMO_ENDPOINT = "https://test-payment.momo.vn/v2/gateway/api/create";
    private static final String MOMO_PARTNER_CODE = "MOMO";
    private static final String MOMO_ACCESS_KEY = "F8BBA842ECF85";
    private static final String MOMO_SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";

    public interface PaymentCallback {
        void onSuccess(String transactionId, String orderId);
        void onError(String error);
        void onPaymentUrlReady(String payUrl);
    }

    public static void createPayment(Activity activity, int orderId, int amount,
                                     String orderInfo, PaymentCallback callback) {
        new Thread(() -> {
            try {
                // 1. Tạo request parameters
                String momoOrderId = "ORDER_" + orderId + "_" + System.currentTimeMillis();
                String requestId = momoOrderId;
                String redirectUrl = "ecommerce://payment-result"; // Deep link về app
                String ipnUrl = "https://webhook.site/your-webhook-id"; // IPN endpoint (optional)
                String requestType = "payWithMethod"; // hoặc "captureWallet"
                
                // ✅ GỬI ORDER ID QUA EXTRA DATA (Base64 encoded)
                String extraDataRaw = String.valueOf(orderId); // Chỉ gửi orderId
                String extraData = android.util.Base64.encodeToString(
                    extraDataRaw.getBytes("UTF-8"), 
                    android.util.Base64.NO_WRAP
                );

                // 2. Tạo chữ ký (signature)
                String rawSignature = "accessKey=" + MOMO_ACCESS_KEY +
                        "&amount=" + amount +
                        "&extraData=" + extraData +
                        "&ipnUrl=" + ipnUrl +
                        "&orderId=" + momoOrderId +
                        "&orderInfo=" + orderInfo +
                        "&partnerCode=" + MOMO_PARTNER_CODE +
                        "&redirectUrl=" + redirectUrl +
                        "&requestId=" + requestId +
                        "&requestType=" + requestType;

                String signature = hmacSHA256(rawSignature, MOMO_SECRET_KEY);

                // 3. Tạo JSON request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("partnerCode", MOMO_PARTNER_CODE);
                requestBody.put("partnerName", "ShopEase");
                requestBody.put("storeId", "ShopEaseStore");
                requestBody.put("requestId", requestId);
                requestBody.put("amount", amount);
                requestBody.put("orderId", momoOrderId);
                requestBody.put("orderInfo", orderInfo);
                requestBody.put("redirectUrl", redirectUrl);
                requestBody.put("ipnUrl", ipnUrl);
                requestBody.put("lang", "vi");
                requestBody.put("extraData", extraData);
                requestBody.put("requestType", requestType);
                requestBody.put("signature", signature);

                Log.d(TAG, "Request: " + requestBody.toString());

                // 4. Gửi request đến MoMo
                URL url = new URL(MOMO_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                // 5. Đọc response
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String responseBody = scanner.hasNext() ? scanner.next() : "";
                    JSONObject response = new JSONObject(responseBody);

                    Log.d(TAG, "Response: " + responseBody);

                    int resultCode = response.getInt("resultCode");
                    String message = response.getString("message");

                    if (resultCode == 0) {
                        // Thành công - Lấy payment URL
                        String payUrl = response.getString("payUrl");
                        String deeplink = response.optString("deeplink", ""); // Link mở app MoMo
                        String qrCodeUrl = response.optString("qrCodeUrl", ""); // QR code

                        activity.runOnUiThread(() -> {
                            callback.onPaymentUrlReady(payUrl);
                            // ✅ MỞ BROWSER
                            openPaymentInBrowser(activity, payUrl);
                        });
                    } else {
                        activity.runOnUiThread(() ->
                                callback.onError("MoMo error: " + message + " (code: " + resultCode + ")")
                        );
                    }
                } else {
                    activity.runOnUiThread(() ->
                            callback.onError("HTTP error: " + responseCode)
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Error creating MoMo payment", e);
                activity.runOnUiThread(() ->
                        callback.onError("Error: " + e.getMessage())
                );
            }
        }).start();
    }


    private static void openPaymentInBrowser(Activity activity, String payUrl) {
        try {
            // Option 1: Chrome Custom Tabs (recommended)
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setShowTitle(true);
            builder.setUrlBarHidingEnabled(false);

            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(activity, Uri.parse(payUrl));

            Log.d(TAG, "Opened payment URL in Chrome Custom Tab: " + payUrl);

        } catch (Exception e) {
            // Option 2: Fallback - Mở browser mặc định
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(payUrl));
                activity.startActivity(browserIntent);
                Log.d(TAG, "Opened payment URL in default browser: " + payUrl);
            } catch (Exception ex) {
                Log.e(TAG, "Cannot open browser: " + ex.getMessage());
            }
        }
    }

    /**
     * Tạo chữ ký HMAC SHA256
     */
    private static String hmacSHA256(String data, String secretKey) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        byte[] hash = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
