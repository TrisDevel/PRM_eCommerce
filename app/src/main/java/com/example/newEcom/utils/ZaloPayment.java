package com.example.newEcom.utils;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ZaloPayment {
    private static final String TAG = "ZaloPayment";
    private static final String ZALOPAY_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/create";
    private static final String ZALOPAY_APP_ID = "2553";
    private static final String ZALOPAY_KEY1 = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL";
    private static final String ZALOPAY_KEY2 = "trMrHtvjo6myautxDUiAcYsVtaeQ8nhf"; // KEY2 for callback verification
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public interface PaymentCallback {
        void onSuccess(String transactionId, String orderId);
        void onError(String error);
        void onPaymentUrlReady(String payUrl);
    }

    public static void createPayment(Activity activity,
        int orderId, int amount,
        String orderInfo, PaymentCallback callback
    ) {
        new Thread(() -> {
            try {
                String appTransId = getVietnamDatePrefix() + "_" + (System.currentTimeMillis() % 1000000);
                String appUser = "ZaloPayDemo";
                long appTime = System.currentTimeMillis();
                String description = "Android Payment - Thanh toán đơn hàng #" + appTransId;
                String callbackUrl = "ecommerce://payment-result?orderId=" + orderId + "&paymentMethod=ZALOPAY";

                JSONObject embedDataObj = new JSONObject();
                embedDataObj.put("redirecturl", callbackUrl);
                String embedDataStr = embedDataObj.toString();

                JSONArray itemsArr = new JSONArray();
                JSONObject itemObj = new JSONObject();
//                itemObj.put("itemid", "knb");
//                itemObj.put("itemname", "kim nguyen bao");
                itemObj.put("orderId", orderId);
                itemObj.put("orderInfo", orderInfo);
//                itemObj.put("itemquantity", quantity);
                itemsArr.put(itemObj);
                String itemStr = itemsArr.toString();

                String data = ZALOPAY_APP_ID + "|" + appTransId + "|" + appUser + "|" +
                        amount + "|" + appTime + "|" + embedDataStr + "|" + itemStr;
                String mac = hmacSHA256(data, ZALOPAY_KEY1);

                JSONObject requestBody = new JSONObject();
                requestBody.put("app_id", Integer.parseInt(ZALOPAY_APP_ID));
                requestBody.put("app_user", appUser);
                requestBody.put("app_time", appTime);
                requestBody.put("amount", amount);
                requestBody.put("app_trans_id", appTransId);
                requestBody.put("bank_code", "");
                requestBody.put("embed_data", embedDataStr);
                requestBody.put("item", itemStr);
                requestBody.put("callback_url", "https://saved-saved-honeybee.ngrok-free.app/zalo/notify");
                requestBody.put("description", description);
                requestBody.put("mac", mac);

                RequestBody body = RequestBody.create(JSON, requestBody.toString());

                URL url = new URL(ZALOPAY_ENDPOINT);
                Request request = new Request.Builder()
                    .url(ZALOPAY_ENDPOINT)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build();

                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful()) {  
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    int returnCode = jsonResponse.getInt("return_code");
                    String returnMessage = jsonResponse.getString("return_message");

                    if (returnCode == 1) {
                        String orderUrl = jsonResponse.getString("order_url");
                        Log.d(TAG, "createZaloPayOrder: " + orderUrl);
                        activity.runOnUiThread(() -> {
                            callback.onPaymentUrlReady(orderUrl);
                            openPaymentInBrowser(activity, orderUrl);
                        });
                    } else {
                        activity.runOnUiThread(() -> {
                            callback.onError("ZaloPay error: " + returnMessage + " (code: " + returnCode + ")");
                        });
                    }
                } else {
                    activity.runOnUiThread(() -> {
                        callback.onError("HTTP error: " + response.code());
                    });
                }
                
                response.close(); // ⚠️ Quan trọng: Phải close response
                
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    callback.onError("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private static String getVietnamDatePrefix() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyMMdd");
        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("GMT+7");
        sdf.setTimeZone(tz);
        return sdf.format(new java.util.Date());
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

    private static String hmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        mac.init(secretKey);
        byte[] bytes = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * ✅ VERIFY ZALOPAY CALLBACK CHECKSUM
     * Format: HmacSHA256(appid|apptransid|amount|status|pmcid|bankcode|discountamount, KEY2)
     */
    public static boolean verifyCallbackChecksum(
            String appId, String appTransId, String amount, String status,
            String pmcId, String bankCode, String discountAmount, String checksum
    ) {
        try {
            // Build data string theo format ZaloPay
            String data = appId + "|" + appTransId + "|" + amount + "|" + status + "|" + 
                         pmcId + "|" + bankCode + "|" + discountAmount;
            
            Log.d(TAG, "Verifying checksum with data: " + data);
            
            // Calculate expected checksum
            String expectedChecksum = hmacSHA256(data, ZALOPAY_KEY2);
            
            Log.d(TAG, "Received checksum: " + checksum);
            Log.d(TAG, "Expected checksum: " + expectedChecksum);
            
            // Compare
            boolean isValid = expectedChecksum.equals(checksum);
            Log.d(TAG, "Checksum verification: " + (isValid ? "✅ VALID" : "❌ INVALID"));
            
            return isValid;
        } catch (Exception e) {
            Log.e(TAG, "Error verifying checksum: " + e.getMessage());
            return false;
        }
    }
}
