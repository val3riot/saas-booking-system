package it.booking.common;

public final class TextUtils {

    private TextUtils() {
    }

    public static String trim(String value) {
        return value.trim();
    }

    public static String trimNullable(String value) {
        return value == null ? null : value.trim();
    }
}
