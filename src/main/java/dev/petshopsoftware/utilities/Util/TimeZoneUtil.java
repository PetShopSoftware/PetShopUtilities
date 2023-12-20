package dev.petshopsoftware.utilities.Util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class TimeZoneUtil {
    public static String offsetToUTC(int rawOffsetMillis) {
        int offsetHours = Math.abs(rawOffsetMillis) / (60 * 60 * 1000);
        int offsetMinutes = (Math.abs(rawOffsetMillis) / (60 * 1000)) % 60;
        String offsetSign = rawOffsetMillis == 0 ? "" : (rawOffsetMillis > 0 ? "+" : "-");
        return String.format("%s%02d:%02d", offsetSign, offsetHours, offsetMinutes);
    }

    public static String offsetToUTC(TimeZone timeZone) {
        return offsetToUTC(timeZone.getRawOffset());
    }

    public static TimeZone getTimeZoneFromDate(Date date) {
        String offset = offsetToUTC(date.getTimezoneOffset() * 60 * 1000);
        if (offset.equals("00:00")) return TimeZone.getTimeZone("UTC");
        else return TimeZone.getTimeZone("UTC" + offset);
    }

    public static String epochToInternetDateTime(long epoch) {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epoch));
    }

    public static long internetDateTimeToEpoch(String internetDateTime) {
        return Instant.parse(internetDateTime).toEpochMilli();
    }
}