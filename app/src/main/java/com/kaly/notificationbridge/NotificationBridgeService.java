package com.kaly.notificationbridge;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationBridgeService extends NotificationListenerService {
    private static final String TAG = "NotificationBridge";
    private static final Map<String, Long> RECENT_SENT = new ConcurrentHashMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn == null || sbn.getNotification() == null) return;
            if (!BridgeConfig.getBoolean(this, BridgeConfig.KEY_ENABLED, false)) return;

            String packageName = sbn.getPackageName();
            String allowedPackages = BridgeConfig.getString(this, BridgeConfig.KEY_ALLOWED_PACKAGES, "");
            if (!FilterUtils.packageAllowed(packageName, allowedPackages)) return;

            Notification notification = sbn.getNotification();
            Bundle extras = notification.extras;
            String title = charSeqToString(extras.getCharSequence(Notification.EXTRA_TITLE));
            String text = charSeqToString(extras.getCharSequence(Notification.EXTRA_TEXT));
            String bigText = charSeqToString(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
            String subText = charSeqToString(extras.getCharSequence(Notification.EXTRA_SUB_TEXT));

            String content = firstNonBlank(bigText, text, subText);
            String fullText = (title + "\n" + content).trim();
            if (fullText.isEmpty()) return;

            String includeKeywords = BridgeConfig.getString(this, BridgeConfig.KEY_INCLUDE_KEYWORDS, "审批,待办,通知,报警,申请,任务");
            String blockKeywords = BridgeConfig.getString(this, BridgeConfig.KEY_BLOCK_KEYWORDS, "验证码,密码,登录,银行卡,支付,转账,动态码,code,otp");

            if (!FilterUtils.containsAnyKeyword(fullText, includeKeywords)) return;
            if (FilterUtils.containsBlockKeyword(fullText, blockKeywords)) return;

            NotificationPayload payload = new NotificationPayload();
            payload.packageName = packageName;
            payload.appName = getAppName(packageName);
            payload.title = FilterUtils.maskSensitive(title);
            payload.content = FilterUtils.maskSensitive(content);
            payload.postTime = sbn.getPostTime();
            payload.notificationKey = sbn.getKey();

            if (isDuplicate(payload)) return;

            WebhookSender.sendAsync(this, payload.toReadableText(), (success, message) ->
                    Log.i(TAG, "send result=" + success + ", " + message));
        } catch (Exception e) {
            Log.e(TAG, "onNotificationPosted error", e);
        }
    }

    private boolean isDuplicate(NotificationPayload payload) {
        int dedupSeconds = BridgeConfig.getInt(this, BridgeConfig.KEY_DEDUP_SECONDS, 60);
        if (dedupSeconds <= 0) return false;
        String hash = FilterUtils.sha256(payload.dedupRaw());
        long now = System.currentTimeMillis();
        Long last = RECENT_SENT.get(hash);
        if (last != null && now - last < dedupSeconds * 1000L) {
            return true;
        }
        RECENT_SENT.put(hash, now);
        cleanupRecent(now, dedupSeconds * 1000L * 3);
        return false;
    }

    private void cleanupRecent(long now, long ttlMs) {
        for (Map.Entry<String, Long> entry : RECENT_SENT.entrySet()) {
            if (now - entry.getValue() > ttlMs) {
                RECENT_SENT.remove(entry.getKey());
            }
        }
    }

    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private static String charSeqToString(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return "";
    }
}
