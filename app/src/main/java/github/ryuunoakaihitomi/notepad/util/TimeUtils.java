package github.ryuunoakaihitomi.notepad.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    private TimeUtils() {
    }

    public static String getNowId() {
        @SuppressWarnings("SpellCheckingInspection") final String pattern = "yyyyMMddHHmmss";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    public static String getLocalFormatText(long time) {
        return DateFormat.getDateTimeInstance().format(new Date(time));
    }
}
