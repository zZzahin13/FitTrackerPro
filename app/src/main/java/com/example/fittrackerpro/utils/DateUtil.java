package com.example.fittrackerpro.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil {
    public static String formatDateTime(long millis) {
        return new SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault())
                .format(new Date(millis));
    }

    private static final String[] DAY_NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private static Calendar midnightToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    public static long startOfToday() {
        return midnightToday().getTimeInMillis();
    }

    // Monday 00:00 of the current week. "Done this week" checks compare against this.
    public static long startOfWeek() {
        Calendar c = midnightToday();
        int daysSinceMonday = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        c.add(Calendar.DAY_OF_YEAR, -daysSinceMonday);
        return c.getTimeInMillis();
    }

    // 1 = Monday ... 7 = Sunday
    public static int todayDayOfWeek() {
        return ((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1;
    }

    public static String dayName(int dayOfWeek) {
        return DAY_NAMES[dayOfWeek - 1];
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }
}