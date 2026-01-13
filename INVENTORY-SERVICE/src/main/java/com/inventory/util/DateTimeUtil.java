package com.inventory.util;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    private static final ZoneId DUBAI_ZONE = ZoneId.of("Asia/Dubai");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateTimeUtil() {}

    public static LocalDateTime nowDubai() {
        return LocalDateTime.now(DUBAI_ZONE);
    }

    public static String nowDubaiFormatted() {
        return nowDubai().format(FORMATTER);
    }

    public static String formatDubai(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.atZone(DUBAI_ZONE).format(FORMATTER);
    }
}

