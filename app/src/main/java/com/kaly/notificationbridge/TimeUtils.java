package com.kaly.notificationbridge;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {
    public static String format(long timeMillis) {
        long t = timeMillis <= 0 ? System.currentTimeMillis() : timeMillis;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date(t));
    }
}
