package com.kaly.notificationbridge;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class FilterUtils {
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern LONG_NUMBER_PATTERN = Pattern.compile("(?<!\\d)\\d{6,20}(?!\\d)");
    private static final Pattern CODE_PATTERN = Pattern.compile("(?i)(验证码|校验码|动态码|code|otp)[：: ]*([A-Za-z0-9]{4,8})");

    public static Set<String> splitToSet(String csv) {
        Set<String> set = new HashSet<>();
        if (csv == null || csv.trim().isEmpty()) return set;
        String[] arr = csv.split("[,，\\n\\r;；]");
        for (String s : arr) {
            String v = s.trim();
            if (!v.isEmpty()) set.add(v);
        }
        return set;
    }

    public static boolean packageAllowed(String pkg, String allowedPackages) {
        Set<String> packages = splitToSet(allowedPackages);
        if (packages.isEmpty()) return true;
        return packages.contains(pkg);
    }

    public static boolean containsAnyKeyword(String text, String keywords) {
        Set<String> ks = splitToSet(keywords);
        if (ks.isEmpty()) return true;
        String lower = safe(text).toLowerCase(Locale.ROOT);
        for (String k : ks) {
            if (lower.contains(k.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    public static boolean containsBlockKeyword(String text, String blockKeywords) {
        Set<String> ks = splitToSet(blockKeywords);
        if (ks.isEmpty()) return false;
        String lower = safe(text).toLowerCase(Locale.ROOT);
        for (String k : ks) {
            if (lower.contains(k.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    public static String maskSensitive(String text) {
        if (text == null) return "";
        String masked = CODE_PATTERN.matcher(text).replaceAll("$1：****");
        masked = maskByPattern(masked, PHONE_PATTERN, 3, 4);
        masked = maskByPattern(masked, LONG_NUMBER_PATTERN, 3, 3);
        return masked;
    }

    private static String maskByPattern(String input, Pattern pattern, int prefix, int suffix) {
        java.util.regex.Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(maskMiddle(matcher.group(), prefix, suffix)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(safe(input).getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(safe(input).hashCode());
        }
    }

    private static String maskMiddle(String value, int prefix, int suffix) {
        if (value == null) return "";
        if (value.length() <= prefix + suffix) return "****";
        return value.substring(0, prefix) + "****" + value.substring(value.length() - suffix);
    }

    public static String safe(String s) {
        return s == null ? "" : s;
    }
}
