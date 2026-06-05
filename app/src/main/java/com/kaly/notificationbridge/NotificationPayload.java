package com.kaly.notificationbridge;

public class NotificationPayload {
    public String appName;
    public String packageName;
    public String title;
    public String content;
    public long postTime;
    public String notificationKey;

    public String toReadableText() {
        StringBuilder sb = new StringBuilder();
        sb.append("【手机通知转发】\n");
        sb.append("应用：").append(nullToEmpty(appName)).append("\n");
        sb.append("包名：").append(nullToEmpty(packageName)).append("\n");
        if (!isBlank(title)) {
            sb.append("标题：").append(title).append("\n");
        }
        if (!isBlank(content)) {
            sb.append("内容：").append(content).append("\n");
        }
        sb.append("时间：").append(TimeUtils.format(postTime));
        return sb.toString();
    }

    public String dedupRaw() {
        return nullToEmpty(packageName) + "|" + nullToEmpty(title) + "|" + nullToEmpty(content);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
