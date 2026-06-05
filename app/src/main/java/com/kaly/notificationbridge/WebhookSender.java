package com.kaly.notificationbridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class WebhookSender {
    private static final String TAG = "WebhookSender";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onResult(boolean success, String message);
    }

    public static void sendAsync(Context context, String text, Callback callback) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            boolean ok = false;
            StringBuilder detail = new StringBuilder();
            SharedPreferences prefs = BridgeConfig.prefs(appContext);

            boolean feishuEnabled = prefs.getBoolean(BridgeConfig.KEY_FEISHU_ENABLED, false);
            boolean dingtalkEnabled = prefs.getBoolean(BridgeConfig.KEY_DINGTALK_ENABLED, false);

            if (feishuEnabled) {
                String webhook = prefs.getString(BridgeConfig.KEY_FEISHU_WEBHOOK, "");
                String secret = prefs.getString(BridgeConfig.KEY_FEISHU_SECRET, "");
                Result r = sendFeishu(webhook, secret, text);
                ok = ok || r.success;
                detail.append("飞书：").append(r.message).append("\n");
            }

            if (dingtalkEnabled) {
                String webhook = prefs.getString(BridgeConfig.KEY_DINGTALK_WEBHOOK, "");
                String secret = prefs.getString(BridgeConfig.KEY_DINGTALK_SECRET, "");
                Result r = sendDingTalk(webhook, secret, text);
                ok = ok || r.success;
                detail.append("钉钉：").append(r.message).append("\n");
            }

            if (!feishuEnabled && !dingtalkEnabled) {
                detail.append("未启用任何推送通道");
            }

            if (callback != null) {
                callback.onResult(ok, detail.toString().trim());
            }
        });
    }

    private static Result sendFeishu(String webhook, String secret, String text) {
        if (isBlank(webhook)) return new Result(false, "Webhook 为空");
        try {
            JSONObject body = new JSONObject();
            if (!isBlank(secret)) {
                long timestamp = System.currentTimeMillis() / 1000;
                body.put("timestamp", String.valueOf(timestamp));
                body.put("sign", hmacSha256Base64WithKeyOnly(timestamp + "\n" + secret));
            }
            body.put("msg_type", "text");
            JSONObject content = new JSONObject();
            content.put("text", text);
            body.put("content", content);
            return postJson(webhook, body.toString());
        } catch (Exception e) {
            Log.e(TAG, "sendFeishu error", e);
            return new Result(false, e.getMessage());
        }
    }

    private static Result sendDingTalk(String webhook, String secret, String text) {
        if (isBlank(webhook)) return new Result(false, "Webhook 为空");
        try {
            String url = webhook;
            if (!isBlank(secret)) {
                long timestamp = System.currentTimeMillis();
                String sign = hmacSha256Base64(timestamp + "\n" + secret, secret);
                String encodedSign = URLEncoder.encode(sign, "UTF-8");
                String sep = webhook.contains("?") ? "&" : "?";
                url = webhook + sep + "timestamp=" + timestamp + "&sign=" + encodedSign;
            }
            JSONObject body = new JSONObject();
            body.put("msgtype", "text");
            JSONObject textObj = new JSONObject();
            textObj.put("content", text);
            body.put("text", textObj);
            return postJson(url, body.toString());
        } catch (Exception e) {
            Log.e(TAG, "sendDingTalk error", e);
            return new Result(false, e.getMessage());
        }
    }

    private static Result postJson(String urlString, String json) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            boolean success = code >= 200 && code < 300;
            return new Result(success, "HTTP " + code + " " + response);
        } catch (Exception e) {
            Log.e(TAG, "postJson error", e);
            return new Result(false, e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String hmacSha256Base64(String message, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(signData, Base64.NO_WRAP);
    }

    // 飞书自定义机器人签名常见实现：把 timestamp + "\n" + secret 作为 HMAC key，对空内容做 HMAC-SHA256。
    private static String hmacSha256Base64WithKeyOnly(String keyString) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyString.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(new byte[]{});
        return Base64.encodeToString(signData, Base64.NO_WRAP);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static class Result {
        boolean success;
        String message;
        Result(boolean success, String message) {
            this.success = success;
            this.message = message == null ? "" : message;
        }
    }
}
