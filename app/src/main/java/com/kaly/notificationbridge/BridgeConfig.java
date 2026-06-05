package com.kaly.notificationbridge;

import android.content.Context;
import android.content.SharedPreferences;

public class BridgeConfig {
    private static final String PREFS = "notification_bridge_config";

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_FEISHU_ENABLED = "feishu_enabled";
    public static final String KEY_DINGTALK_ENABLED = "dingtalk_enabled";
    public static final String KEY_FEISHU_WEBHOOK = "feishu_webhook";
    public static final String KEY_FEISHU_SECRET = "feishu_secret";
    public static final String KEY_DINGTALK_WEBHOOK = "dingtalk_webhook";
    public static final String KEY_DINGTALK_SECRET = "dingtalk_secret";
    public static final String KEY_ALLOWED_PACKAGES = "allowed_packages";
    public static final String KEY_INCLUDE_KEYWORDS = "include_keywords";
    public static final String KEY_BLOCK_KEYWORDS = "block_keywords";
    public static final String KEY_DEDUP_SECONDS = "dedup_seconds";

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getString(Context context, String key, String def) {
        return prefs(context).getString(key, def);
    }

    public static boolean getBoolean(Context context, String key, boolean def) {
        return prefs(context).getBoolean(key, def);
    }

    public static int getInt(Context context, String key, int def) {
        return prefs(context).getInt(key, def);
    }

    public static void save(Context context,
                            boolean enabled,
                            boolean feishuEnabled,
                            boolean dingtalkEnabled,
                            String feishuWebhook,
                            String feishuSecret,
                            String dingtalkWebhook,
                            String dingtalkSecret,
                            String allowedPackages,
                            String includeKeywords,
                            String blockKeywords,
                            int dedupSeconds) {
        prefs(context).edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putBoolean(KEY_FEISHU_ENABLED, feishuEnabled)
                .putBoolean(KEY_DINGTALK_ENABLED, dingtalkEnabled)
                .putString(KEY_FEISHU_WEBHOOK, safe(feishuWebhook))
                .putString(KEY_FEISHU_SECRET, safe(feishuSecret))
                .putString(KEY_DINGTALK_WEBHOOK, safe(dingtalkWebhook))
                .putString(KEY_DINGTALK_SECRET, safe(dingtalkSecret))
                .putString(KEY_ALLOWED_PACKAGES, safe(allowedPackages))
                .putString(KEY_INCLUDE_KEYWORDS, safe(includeKeywords))
                .putString(KEY_BLOCK_KEYWORDS, safe(blockKeywords))
                .putInt(KEY_DEDUP_SECONDS, dedupSeconds)
                .apply();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
