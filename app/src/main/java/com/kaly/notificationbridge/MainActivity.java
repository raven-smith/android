package com.kaly.notificationbridge;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private CheckBox enabledBox;
    private CheckBox feishuEnabledBox;
    private CheckBox dingtalkEnabledBox;
    private EditText feishuWebhookInput;
    private EditText feishuSecretInput;
    private EditText dingtalkWebhookInput;
    private EditText dingtalkSecretInput;
    private EditText allowedPackagesInput;
    private EditText includeKeywordsInput;
    private EditText blockKeywordsInput;
    private EditText dedupSecondsInput;
    private TextView permissionStatusView;
    private TextView resultView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadConfig();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionStatus();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Android 通知桥接器");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(32, 43, 61));
        title.setGravity(Gravity.START);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("监听本机通知栏中的指定 App 消息，并转发到飞书或钉钉群机器人。建议只填写目标 App 包名，不要转发全部通知。");
        desc.setTextSize(14);
        desc.setTextColor(Color.rgb(88, 96, 112));
        desc.setPadding(0, dp(8), 0, dp(14));
        root.addView(desc);

        permissionStatusView = label("通知读取权限：检测中");
        root.addView(permissionStatusView);

        Button openPermissionBtn = button("打开通知读取权限设置");
        openPermissionBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(openPermissionBtn);

        enabledBox = checkbox("启用通知转发");
        root.addView(enabledBox);

        root.addView(section("推送通道"));
        feishuEnabledBox = checkbox("启用飞书群机器人");
        root.addView(feishuEnabledBox);
        feishuWebhookInput = input("飞书 Webhook", false);
        root.addView(feishuWebhookInput);
        feishuSecretInput = input("飞书签名密钥，可留空", true);
        root.addView(feishuSecretInput);

        dingtalkEnabledBox = checkbox("启用钉钉群机器人");
        root.addView(dingtalkEnabledBox);
        dingtalkWebhookInput = input("钉钉 Webhook", false);
        root.addView(dingtalkWebhookInput);
        dingtalkSecretInput = input("钉钉加签密钥，可留空", true);
        root.addView(dingtalkSecretInput);

        root.addView(section("过滤规则"));
        allowedPackagesInput = multiInput("App 包名白名单，逗号分隔；留空表示不限制，但不建议。例：com.tencent.mm,com.alibaba.android.rimet");
        root.addView(allowedPackagesInput);
        includeKeywordsInput = multiInput("包含关键词，逗号分隔；为空表示不过滤关键词。例：审批,待办,通知,报警,申请,任务");
        root.addView(includeKeywordsInput);
        blockKeywordsInput = multiInput("屏蔽关键词，逗号分隔。例：验证码,密码,登录,银行卡,支付,转账");
        root.addView(blockKeywordsInput);
        dedupSecondsInput = input("去重时间，单位秒，默认 60", false);
        dedupSecondsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(dedupSecondsInput);

        Button saveBtn = button("保存配置");
        saveBtn.setOnClickListener(v -> saveConfig());
        root.addView(saveBtn);

        Button testBtn = button("发送测试消息");
        testBtn.setOnClickListener(v -> {
            saveConfigSilently();
            resultView.setText("正在发送测试消息...");
            String text = "【手机通知转发测试】\n应用：通知桥接器\n标题：测试消息\n内容：如果你看到这条消息，说明飞书/钉钉 Webhook 配置可用。\n时间：" + TimeUtils.format(System.currentTimeMillis());
            WebhookSender.sendAsync(this, text, (success, message) -> runOnUiThread(() -> {
                resultView.setText((success ? "发送完成\n" : "发送失败\n") + message);
                Toast.makeText(this, success ? "测试消息已发送" : "测试消息发送失败", Toast.LENGTH_LONG).show();
            }));
        });
        root.addView(testBtn);

        Button batteryBtn = button("打开本应用系统设置，建议允许后台运行");
        batteryBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        root.addView(batteryBtn);

        resultView = label("运行日志会显示在这里。真实通知转发日志请在 Android Studio Logcat 中搜索 NotificationBridge。 ");
        resultView.setPadding(0, dp(14), 0, 0);
        root.addView(resultView);

        setContentView(scrollView);
    }

    private void loadConfig() {
        SharedPreferences prefs = BridgeConfig.prefs(this);
        enabledBox.setChecked(prefs.getBoolean(BridgeConfig.KEY_ENABLED, false));
        feishuEnabledBox.setChecked(prefs.getBoolean(BridgeConfig.KEY_FEISHU_ENABLED, false));
        dingtalkEnabledBox.setChecked(prefs.getBoolean(BridgeConfig.KEY_DINGTALK_ENABLED, false));
        feishuWebhookInput.setText(prefs.getString(BridgeConfig.KEY_FEISHU_WEBHOOK, ""));
        feishuSecretInput.setText(prefs.getString(BridgeConfig.KEY_FEISHU_SECRET, ""));
        dingtalkWebhookInput.setText(prefs.getString(BridgeConfig.KEY_DINGTALK_WEBHOOK, ""));
        dingtalkSecretInput.setText(prefs.getString(BridgeConfig.KEY_DINGTALK_SECRET, ""));
        allowedPackagesInput.setText(prefs.getString(BridgeConfig.KEY_ALLOWED_PACKAGES, ""));
        includeKeywordsInput.setText(prefs.getString(BridgeConfig.KEY_INCLUDE_KEYWORDS, "审批,待办,通知,报警,申请,任务"));
        blockKeywordsInput.setText(prefs.getString(BridgeConfig.KEY_BLOCK_KEYWORDS, "验证码,密码,登录,银行卡,支付,转账,动态码,code,otp"));
        dedupSecondsInput.setText(String.valueOf(prefs.getInt(BridgeConfig.KEY_DEDUP_SECONDS, 60)));
    }

    private void saveConfig() {
        saveConfigSilently();
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
        refreshPermissionStatus();
    }

    private void saveConfigSilently() {
        int dedupSeconds = parseInt(dedupSecondsInput.getText().toString(), 60);
        BridgeConfig.save(
                this,
                enabledBox.isChecked(),
                feishuEnabledBox.isChecked(),
                dingtalkEnabledBox.isChecked(),
                feishuWebhookInput.getText().toString(),
                feishuSecretInput.getText().toString(),
                dingtalkWebhookInput.getText().toString(),
                dingtalkSecretInput.getText().toString(),
                allowedPackagesInput.getText().toString(),
                includeKeywordsInput.getText().toString(),
                blockKeywordsInput.getText().toString(),
                dedupSeconds
        );
    }

    private void refreshPermissionStatus() {
        boolean enabled = isNotificationListenerEnabled();
        permissionStatusView.setText(enabled ? "通知读取权限：已开启" : "通知读取权限：未开启，请先授权");
        permissionStatusView.setTextColor(enabled ? Color.rgb(25, 128, 56) : Color.rgb(190, 65, 65));
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (flat == null || flat.isEmpty()) return false;
        ComponentName cn = new ComponentName(this, NotificationBridgeService.class);
        return flat.contains(cn.flattenToString()) || flat.toLowerCase().contains(getPackageName().toLowerCase());
    }

    private TextView section(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(18);
        view.setTextColor(Color.rgb(32, 43, 61));
        view.setPadding(0, dp(20), 0, dp(8));
        return view;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(Color.rgb(88, 96, 112));
        view.setLineSpacing(0, 1.15f);
        return view;
    }

    private CheckBox checkbox(String text) {
        CheckBox cb = new CheckBox(this);
        cb.setText(text);
        cb.setTextSize(15);
        cb.setPadding(0, dp(8), 0, dp(8));
        return cb;
    }

    private EditText input(String hint, boolean password) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setTextSize(14);
        editText.setPadding(dp(10), dp(8), dp(10), dp(8));
        editText.setInputType(password ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        editText.setLayoutParams(lp);
        return editText;
    }

    private EditText multiInput(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setMinLines(2);
        editText.setMaxLines(4);
        editText.setGravity(Gravity.TOP | Gravity.START);
        editText.setTextSize(14);
        editText.setPadding(dp(10), dp(8), dp(10), dp(8));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        editText.setLayoutParams(lp);
        return editText;
    }

    private Button button(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, dp(4));
        btn.setLayoutParams(lp);
        return btn;
    }

    private int parseInt(String value, int def) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
