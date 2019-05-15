package nl.rutilo.labeldb.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static java.time.temporal.ChronoField.*;

/** Human readable int dates (yyyymmdd) or long datetimes (yyyymmddhhmmssmmm). */
public class HumanReadableTime {
    public static final long MIN_VALUE = -999991231235959999L;
    public static final long MAX_VALUE =  999991231235959999L;
    public static final long MIN_YEAR = yearsOf(MIN_VALUE);
    public static final long MAX_YEAR = yearsOf(MAX_VALUE);

    public static final long MILLISECONDS = 1L;
    public static final long SECONDS      = 1000 * MILLISECONDS;
    public static final long MINUTES      = 100 * SECONDS;
    public static final long HOURS        = 100 * MINUTES;
    public static final long DAYS         = 100 * HOURS;
    public static final long MONTHS       = 100 * DAYS;
    public static final long YEARS        = 100 * MONTHS;

    public static final int INDEX_YEARS   = 0;
    public static final int INDEX_MONTHS  = 1;
    public static final int INDEX_DAYS    = 2;
    public static final int INDEX_HOURS   = 3;
    public static final int INDEX_MINUTES = 4;
    public static final int INDEX_SECONDS = 5;
    public static final int INDEX_MS      = 6;


    private HumanReadableTime() {}

    public static int getDate() {
        return getDateOf(LocalDateTime.now());
    }
    public static int getDateOf(LocalDateTime dt) {
        return dt.get(YEAR) * 10000
             + dt.get(MONTH_OF_YEAR) * 100
             + dt.get(DAY_OF_MONTH);
    }
    public static int getDateOf(long hrDt) {
        return getDateOf(toLocalDateTime(hrDt));
    }

    public static long getDateTime() {
        return getDateTimeOf(LocalDateTime.now());
    }
    public static long getDateTimeOf(LocalDateTime dt) {
        long hrDt = 0;
        hrDt = (hrDt      ) + dt.get(YEAR);
        hrDt = (hrDt * 100) + dt.get(MONTH_OF_YEAR);
        hrDt = (hrDt * 100) + dt.get(DAY_OF_MONTH);
        hrDt = (hrDt * 100) + dt.get(HOUR_OF_DAY);
        hrDt = (hrDt * 100) + dt.get(MINUTE_OF_HOUR);
        hrDt = (hrDt * 100) + dt.get(SECOND_OF_MINUTE);
        hrDt = (hrDt * 1000)+ dt.get(MILLI_OF_SECOND);
        return hrDt;
    }
    public static long getDateTimeOf(File file) {
        try {
            final FileTime fileTime = Files.getLastModifiedTime(file.toPath());
            final LocalDateTime dt = LocalDateTime.from(fileTime.toInstant().atZone(ZoneId.systemDefault()));
            return getDateTimeOf(dt);
        } catch (IOException e) {
            return -1;
        }
    }

    public static long toDateTime(long year, long month, long day, long hour, long minute, long second, long ms) {
        final long dt =
              YEARS   * (year   < MIN_YEAR ? MIN_YEAR : year > MAX_YEAR ? MAX_YEAR : year)
            + MONTHS  * (month  < 1 ? 1 : month  >  12 ?  12 : month)
            + DAYS    * (day    < 1 ? 1 : day    >  31 ?  31 : day)
            + HOURS   * (hour   < 0 ? 0 : hour   >  23 ?  23 : hour)
            + MINUTES * (minute < 0 ? 0 : minute >  59 ?  59 : minute)
            + SECONDS * (second < 0 ? 0 : second >  59 ?  59 : second)
            +           (ms     < 0 ? 0 : ms     > 999 ? 999 : ms);
        final int maxDays = daysInMonth(yearsOf(dt), monthsOf(dt));
        return daysOf(dt) > maxDays ? setDays(dt, maxDays) : dt;
    }

    public static int daysInMonth(long year, long month) {
        final boolean isLeapYear = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
        return (month + (month > 7 ? -1 : 0)) %2 == 1 ? 31 : (month == 2 ? 28 + (isLeapYear ? 1 : 0) : 30);
    }
    public static int daysInMonth(long time) {
        return daysInMonth(daysOf(time), monthsOf(time));
    }

    public static long setYears(long hrDt, long year) {
        final long[] parts = partsOf(hrDt);
        parts[INDEX_YEARS] = Math.max(MIN_YEAR, Math.min(MAX_YEAR, year));
        return fromParts(parts);
    }
    public static long setMonths(long hrDt, long months) {
        final long[] parts = partsOf(hrDt);
        parts[INDEX_MONTHS] = Math.max(1, Math.min(12, months));
        return fromParts(parts);
    }
    public static long setDays(long hrDt, long days) {
        final long[] parts = partsOf(hrDt);
        parts[INDEX_DAYS] = Math.max(1, Math.min(daysInMonth(yearsOf(hrDt), monthsOf(hrDt)), days));
        return fromParts(parts);
    }
    public static long setHours(long hrDt, long hours) {
        final long[] parts = partsOf(hrDt);
        parts[INDEX_HOURS] = Math.max(0, Math.min(23, hours));
        return fromParts(parts);
    }
    public static long setMinutes(long hrDt, long minutes) {
        final long[] parts = partsOf(hrDt);
        parts[INDEX_MINUTES] = Math.max(0, Math.min(59, minutes));
        return fromParts(parts);
    }
    public static long setSeconds(long hrDt, long seconds) {
        final long[] parts = partsOf(hrDt);
        parts[INDEX_SECONDS] = Math.max(0, Math.min(59, seconds));
        return fromParts(parts);
    }
    public static long setMs(long hrDt, long ms) {
        final long[] parts = partsOf(hrDt);
        parts[INDEX_MS] = Math.max(0, Math.min(999, ms));
        return fromParts(parts);
    }

    public static int msOf(long hrDt)      { return (int)((hrDt % SECONDS) / MILLISECONDS); }
    public static int secondsOf(long hrDt) { return (int)((hrDt % MINUTES) / SECONDS); }
    public static int minutesOf(long hrDt) { return (int)((hrDt % HOURS) / MINUTES); }
    public static int hoursOf(long hrDt)   { return (int)((hrDt % DAYS) / HOURS); }
    public static int daysOf(long hrDt)    { return (int)((hrDt % MONTHS) / DAYS); }
    public static int monthsOf(long hrDt)  { return (int)((hrDt % YEARS) / MONTHS); }
    public static int yearsOf(long hrDt)   { return (int)((hrDt / YEARS)); }

    public static long[] partsOf(long hrDt) {
        return new long[] { yearsOf(hrDt), monthsOf(hrDt), daysOf(hrDt), hoursOf(hrDt), minutesOf(hrDt), secondsOf(hrDt), msOf(hrDt) };
    }

    public static long fromParts(int[] parts) {
        return toDateTime(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6] );
    }
    public static long fromParts(long[] parts) {
        return toDateTime(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6] );
    }

    public static LocalDateTime toLocalDateTime(int hrD) {
        final int day   = (hrD % 100);
        final int month = (hrD / 100) % 100;
        final int year  = (hrD / 100) / 100;
        return LocalDateTime.of(year, month, day, 0, 0);
    }
    public static LocalDateTime toLocalDateTime(long hrDt) {
        long hr = hrDt;
        final int ms    = (int)(hr % 1000); hr /= 1000;
        final int sec   = (int)(hr % 100);  hr /= 100;
        final int min   = (int)(hr % 100);  hr /= 100;
        final int hour  = (int)(hr % 100);  hr /= 100;
        final int day   = (int)(hr % 100);  hr /= 100;
        final int month = (int)(hr % 100);  hr /= 100;
        final int year  = (int)hr;
        return LocalDateTime.of(year, month, day, hour, min, sec, (1000 * 1000 * ms));
    }
    public static long toSecondsSinceEpoch(long hrDt) {
        return toSecondsSinceEpoch(hrDt, ZoneOffset.UTC);
    }
    public static long toSecondsSinceEpoch(long hrDt, ZoneOffset zoneOffset) {
        return toLocalDateTime(hrDt).toEpochSecond(zoneOffset);
    }
    public static long toMsSinceEpoch(long hrDt) {
        return toMsSinceEpoch(hrDt, ZoneOffset.UTC);
    }
    public static long toMsSinceEpoch(long hrDt, ZoneOffset zoneOffset) {
        return toLocalDateTime(hrDt).toEpochSecond(zoneOffset) * 1000 + hrDt % 1000;
    }
}
