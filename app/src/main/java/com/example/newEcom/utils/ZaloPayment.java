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

public class ZaloPayment {
    private static final String TAG = "ZaloPayment";
    private static final String ZALOPAY_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/create";
    private static final String ZALOPAY_APP_ID = "2553";
    private static final String ZALOPAY_APP_ID_V1 = "553";
    private static   final String ZALOPAY_KEY1 = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL";
    private static final String ZALOPAY_KEY_V1 = "9phuAOYhan4urywHTh0ndEXiV3pKHr5Q";
    private static final String ZALOPAY_KEY2 = "kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz";
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
                String callbackUrl = "ecommerce://payment-result";

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

                String data = ZALOPAY_APP_ID_V1 + "|" + appTransId + "|" + appUser + "|" +
                        amount + "|" + appTime + "|" + embedDataStr + "|" + itemStr;
                String mac = hmacSHA256(data, ZALOPAY_KEY_V1);

                JSONObject requestBody = new JSONObject();
                requestBody.put("app_id", Integer.parseInt(ZALOPAY_APP_ID_V1));
                requestBody.put("app_user", appUser);
                requestBody.put("app_time", appTime);
                requestBody.put("amount", amount);
                requestBody.put("app_trans_id", appTransId);
                requestBody.put("bank_code", "");
                requestBody.put("embed_data", embedDataStr);
                requestBody.put("item", itemStr);
                requestBody.put("callback_url", callbackUrl);
                requestBody.put("description", description);
                requestBody.put("mac", mac);

                URL url = new URL(ZALOPAY_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String responseBody = scanner.hasNext() ? scanner.next() : "";
                    JSONObject response = new JSONObject(responseBody);
                    int returnCode = response.getInt("return_code");
                    String returnMessage = response.getString("return_message");

                    if (returnCode == 1) {
                        String orderUrl = response.getString("order_url");
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
                        callback.onError("HTTP error: " + responseCode);
                    });
                }
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
}
