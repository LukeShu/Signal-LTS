/*
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.R;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/*
 * Java has a bunch of legacy datetime APIs:
 *
 *  - java.lang.System.currentTimeMillis()
 *  - java.text.DateFormat
 *  - java.text.DateFormatSymbols
 *  - java.text.SimpleDateFormat
 *  - java.util.Calendar
 *  - java.util.Date
 *  - java.util.GregorianCalendar
 *  - java.util.TimeZone
 *  - java.util.concurrent.TimeUnit
 *  - android.text.format.DateFormat
 *  - android.text.format.DateUtils
 *  - android.text.format.Time
 *
 * New code should use the "new" (Java 8, 2014) `java.time.**` APIs.
 *
 * Because I (lukeshu) don't (yet) care to go through the trouble of
 * re-writing the code in this class, it may use legacy APIs
 * internally, but all public signatures should only use java.time
 * types.
 *
 * When changing the signatures here, as an incremental step I just
 * converted things right before their call to DateUtils; most of the
 * surrounding code still uses legacy types.  When seeking to
 * modernize that code, key things to grep for are:
 *
 *  - `System\.currentTimeMillis` (should always be `Instant.now()`)
 *  - `Instant\.ofEpochMilli` (should only be used at deserialization)
 *  - `toEpochMilli` (should only be used at serialization)
 *
 * Other tips:
 *
 *  - `TimeUnit.XXX.toMillis(y)` should be `Duration.ofXxx(y)`
 */

/**
 * Utility methods to help display dates in a nice, easily readable way.
 *
 * TODO(lukeshu): Internally, many of these utilities are implemented
 * using legacy millisecond APIs.
 *
 * TODO(lukeshu): Many of these utilities duplicate stdlib utilities.
 * Figure out which ones, and either wrap or migrate to the stdlib
 * solutions.
 */
public class DateUtils {

  @SuppressWarnings("unused")
  private static final String                        TAG                    = Log.tag(DateUtils.class);
  private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT            = new ThreadLocal<>();
  private static final ThreadLocal<SimpleDateFormat> BRIEF_EXACT_FORMAT     = new ThreadLocal<>();
  private static final long                          MAX_RELATIVE_TIMESTAMP = TimeUnit.MINUTES.toMillis(3);
  private static final int                           HALF_A_YEAR_IN_DAYS    = 182;

  /**
   * For testing, it is important that we be able to mock the system
   * clock.  MockK is unable to mock System.currentTimeMillis(), but
   * is able to mock Instant.now().  Because we shouldn't overhaul the
   * code to use Instants instead of millis until *after* there are
   * tests, this is a shim to make mocking possible.
   */
  private static long System_currentTimeMillis() {
    return Instant.now().toEpochMilli();
  }

  private static boolean isWithin(final long millis, final long span, final TimeUnit unit) {
    return System_currentTimeMillis() - millis <= unit.toMillis(span);
  }

  private static boolean isWithinAbs(final long millis, final long span, final TimeUnit unit) {
    return Math.abs(System_currentTimeMillis() - millis) <= unit.toMillis(span);
  }

  private static boolean isToday(final long millis) {
    return isSameDay(Instant.ofEpochMilli(millis), Instant.now());
  }

  private static boolean isYesterday(final long when) {
    return isToday(when + TimeUnit.DAYS.toMillis(1));
  }

  private static int convertDelta(final long millis, TimeUnit to) {
    return (int) to.convert(System_currentTimeMillis() - millis, TimeUnit.MILLISECONDS);
  }

  private static String getFormattedDateTime(long time, String template, Locale locale) {
    final String localizedPattern = getLocalizedPattern(template, locale);

    final SimpleDateFormat formatter = new SimpleDateFormat(localizedPattern, locale);
    setLowercaseAmPmStrings(formatter, locale);
    // Since we're setting the timezone to the default, this is
    // normally a no-op, but is important for testing because mocking
    // ZoneId.systemDefault is easy, but mocking the timezone used by
    // `new Date()` is a pain.
    formatter.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));

    return formatter.format(new Date(time));
  }

  public static @NonNull String formatElapsedTime(@Nullable final Duration elapsed) {
    long elapsedSeconds = 0;
    if (elapsed != null) {
      elapsedSeconds = elapsed.getSeconds();
    }
    // NB: android.text.format.DateUtils.formatElapsedTime() uses Locale.getDefault()
    return android.text.format.DateUtils.formatElapsedTime(elapsedSeconds);
  }

  public static @NonNull String getBriefRelativeTimeSpanString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
      return context.getString(R.string.DateUtils_just_now);
    } else if (isWithin(timestamp, 1, TimeUnit.HOURS)) {
      int mins = convertDelta(timestamp, TimeUnit.MINUTES);
      return context.getResources().getString(R.string.DateUtils_minutes_ago, mins);
    } else if (isWithin(timestamp, 1, TimeUnit.DAYS)) {
      int hours = convertDelta(timestamp, TimeUnit.HOURS);
      return context.getResources().getQuantityString(R.plurals.hours_ago, hours, hours);
    } else if (isWithin(timestamp, 6, TimeUnit.DAYS)) {
      return getFormattedDateTime(timestamp, "EEE", locale);
    } else if (isWithin(timestamp, 365, TimeUnit.DAYS)) {
      return getFormattedDateTime(timestamp, "MMM d", locale);
    } else {
      return getFormattedDateTime(timestamp, "MMM d, yyyy", locale);
    }
  }

  public static @NonNull String getExtendedRelativeTimeSpanString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
      return context.getString(R.string.DateUtils_just_now);
    } else if (isWithin(timestamp, 1, TimeUnit.HOURS)) {
      int mins = (int)TimeUnit.MINUTES.convert(System_currentTimeMillis() - timestamp, TimeUnit.MILLISECONDS);
      return context.getResources().getString(R.string.DateUtils_minutes_ago, mins);
    } else {
      StringBuilder format = new StringBuilder();
      if      (isWithin(timestamp,   6, TimeUnit.DAYS)) format.append("EEE ");
      else if (isWithin(timestamp, 365, TimeUnit.DAYS)) format.append("MMM d, ");
      else                                              format.append("MMM d, yyyy, ");

      if (DateFormat.is24HourFormat(context)) format.append("HH:mm");
      else                                    format.append("hh:mm a");

      return getFormattedDateTime(timestamp, format.toString(), locale);
    }
  }

  public static @NonNull String getSimpleRelativeTimeSpanString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
      return context.getString(R.string.DateUtils_just_now);
    } else if (isWithin(timestamp, 1, TimeUnit.HOURS)) {
      int mins = (int) TimeUnit.MINUTES.convert(System_currentTimeMillis() - timestamp, TimeUnit.MILLISECONDS);
      return context.getResources().getString(R.string.DateUtils_minutes_ago, mins);
    } else {
      return getOnlyTimeString(context, locale, when);
    }
  }

  /**
   * Formats a given timestamp as just the time.
   *
   * For example:
   *  For 12 hour locale: 7:23 pm
   *  For 24 hour locale: 19:23
   */
  public static @NonNull String getOnlyTimeString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    String format = DateFormat.is24HourFormat(context) ? "HH:mm" : "hh:mm a";
    return getFormattedDateTime(timestamp, format, locale);
  }

  public static @NonNull String getTimeString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    StringBuilder format = new StringBuilder();

    if      (isSameDay(Instant.now(), when))                   format.append("");
    else if (isWithinAbs(timestamp,   6, TimeUnit.DAYS))       format.append("EEE ");
    else if (isWithinAbs(timestamp, 364, TimeUnit.DAYS))       format.append("MMM d, ");
    else                                                       format.append("MMM d, yyyy, ");

    if (DateFormat.is24HourFormat(context)) format.append("HH:mm");
    else                                    format.append("hh:mm a");

    return getFormattedDateTime(timestamp, format.toString(), locale);
  }

  /**
   * Formats the passed timestamp based on the current time at a day precision.
   *
   * For example:
   *  - Today
   *  - Wed
   *  - Mon
   *  - Jan 31
   *  - Feb 4
   *  - Jan 12, 2033
   */
  public static @NonNull String getDayPrecisionTimeString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    if (simpleDateFormat.format(System_currentTimeMillis()).equals(simpleDateFormat.format(timestamp))) {
      return context.getString(R.string.DeviceListItem_today);
    } else {
      String format;

      if (isWithinAbs(timestamp, 6, TimeUnit.DAYS)) {
        format = "EEE ";
      } else if (isWithinAbs(timestamp, 365, TimeUnit.DAYS)) {
        format = "MMM d";
      } else {
        format = "MMM d, yyy";
      }

      return getFormattedDateTime(timestamp, format, locale);
    }
  }

  public static @NonNull String getDayPrecisionTimeSpanString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    if (simpleDateFormat.format(System_currentTimeMillis()).equals(simpleDateFormat.format(timestamp))) {
      return context.getString(R.string.DeviceListItem_today);
    } else {
      String format;

      if      (isWithin(timestamp, 6, TimeUnit.DAYS))   format = "EEE ";
      else if (isWithin(timestamp, 365, TimeUnit.DAYS)) format = "MMM d";
      else                                              format = "MMM d, yyy";

      return getFormattedDateTime(timestamp, format, locale);
    }
  }

  public static @NonNull String getMessageDetailsTimeString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    String dateFormatPattern;

    if (DateFormat.is24HourFormat(context)) {
      dateFormatPattern = getLocalizedPattern("MMM d, yyyy HH:mm:ss zzz", locale);
    } else {
      dateFormatPattern = getLocalizedPattern("MMM d, yyyy hh:mm:ss a zzz", locale);
    }

    final SimpleDateFormat formatter = new SimpleDateFormat(dateFormatPattern, locale);
    // Since we're setting the timezone to the default, this is
    // normally a no-op, but is important for testing because mocking
    // ZoneId.systemDefault is easy, but mocking the timezone used by
    // `new Date()` is a pain.
    formatter.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));

    return formatter.format(when.toEpochMilli());
  }

  public static @NonNull String getConversationDateHeaderString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    if (isToday(timestamp)) {
      return context.getString(R.string.DateUtils_today);
    } else if (isYesterday(timestamp)) {
      return context.getString(R.string.DateUtils_yesterday);
    } else if (isWithin(timestamp, HALF_A_YEAR_IN_DAYS, TimeUnit.DAYS)) {
      return formatDateWithDayOfWeek(locale, when);
    } else {
      return formatDateWithYear(locale, when);
    }
  }

  public static @NonNull String getScheduledMessagesDateHeaderString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    if (isToday(timestamp)) {
      return context.getString(R.string.DateUtils_today);
    } else if (isWithinAbs(timestamp, HALF_A_YEAR_IN_DAYS, TimeUnit.DAYS)) {
      return formatDateWithDayOfWeek(locale, when);
    } else {
      return formatDateWithYear(locale, when);
    }
  }

  public static @NonNull String getScheduledMessageDateString(@NonNull final Context context, @NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    String dayModifier;
    if (isToday(timestamp)) {
      // Since we're passing in the default timezone, normally we'd be
      // able to just omit that argument, but passing it explicitly is
      // important for testing because mocking the default timezone
      // used by `Calendar.getInstance(locale)` is a pain.
      Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.systemDefault()), locale);
      // Since we're setting the time to the current time, this is
      // normally a no-op, but is important for testing because
      // mocking the clock used by `Calendar.getInstance()` is a pain.
      calendar.setTimeInMillis(System_currentTimeMillis());
      if (calendar.get(Calendar.HOUR_OF_DAY) >= 19) {
        dayModifier = context.getString(R.string.DateUtils_tonight);
      } else {
        dayModifier = context.getString(R.string.DateUtils_today);
      }
    } else {
      dayModifier = context.getString(R.string.DateUtils_tomorrow);
    }
    String format = DateFormat.is24HourFormat(context) ? "HH:mm" : "hh:mm a";
    String time   = getFormattedDateTime(timestamp, format, locale);

    return context.getString(R.string.DateUtils_schedule_at, dayModifier, time);
  }

  public static @NonNull String formatDateWithDayOfWeek(@NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    return getFormattedDateTime(timestamp, "EEE, MMM d", locale);
  }

  public static @NonNull String formatDateWithYear(@NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    return getFormattedDateTime(timestamp, "MMM d, yyyy", locale);
  }

  public static @NonNull String formatDate(@NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    return getFormattedDateTime(timestamp, "EEE, MMM d, yyyy", locale);
  }

  public static @NonNull String formatDateWithMonthAndDay(@NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    return getFormattedDateTime(timestamp, "MMMM dd", locale);
  }

  public static @NonNull String formatDateWithoutDayOfWeek(@NonNull final Locale locale, @NonNull final Instant when) {
    final long timestamp = when.toEpochMilli();
    return getFormattedDateTime(timestamp, "MMM d yyyy", locale);
  }

  public static boolean isSameDay(@NonNull final Instant t1, @NonNull final Instant t2) {
    ZoneId tz = ZoneId.systemDefault();
    LocalDateTime dt1 = LocalDateTime.ofInstant(t1, tz);
    LocalDateTime dt2 = LocalDateTime.ofInstant(t2, tz);

    return (dt1.getYear() == dt2.getYear() &&
            dt1.getMonthValue() == dt2.getMonthValue() &&
            dt1.getDayOfMonth() == dt2.getDayOfMonth());
  }

  public static boolean isSameExtendedRelativeTimestamp(@NonNull final Instant second, @NonNull final Instant first) {
    return second.toEpochMilli() - first.toEpochMilli() < MAX_RELATIVE_TIMESTAMP;
  }

  private static String getLocalizedPattern(String template, Locale locale) {
    return DateFormat.getBestDateTimePattern(locale, template);
  }

  private static @NonNull SimpleDateFormat setLowercaseAmPmStrings(@NonNull SimpleDateFormat format, @NonNull Locale locale) {
    DateFormatSymbols symbols = new DateFormatSymbols(locale);

    symbols.setAmPmStrings(new String[] { "am", "pm"});
    format.setDateFormatSymbols(symbols);

    return format;
  }

  /**
   * e.g. 2020-09-04T19:17:51Z
   * https://www.iso.org/iso-8601-date-and-time-format.html
   *
   * Note: SDK_INT == 0 check needed to pass unit tests due to JVM date parser differences.
   *
   * @return The timestamp if able to be parsed, otherwise null.
   */
  @SuppressLint({ "ObsoleteSdkInt", "NewApi" })
  public static @Nullable Instant parseIso8601(@Nullable final String date) {
    SimpleDateFormat format;
    if (Build.VERSION.SDK_INT == 0 || Build.VERSION.SDK_INT >= 24) {
      format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault());
    } else {
      format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
    }

    if (Util.isEmpty(date)) {
      return null;
    }

    try {
      return Instant.ofEpochMilli(format.parse(date).getTime());
    } catch (ParseException e) {
      Log.w(TAG, "Failed to parse date.", e);
      return null;
    }
  }

  @SuppressLint("SimpleDateFormat")
  private static SimpleDateFormat getDateFormat() {
    SimpleDateFormat format = DATE_FORMAT.get();

    if (format == null) {
      format = new SimpleDateFormat("yyyyMMdd");
      DATE_FORMAT.set(format);
    }

    return format;
  }

  @SuppressLint("SimpleDateFormat")
  private static SimpleDateFormat getBriefExactFormat() {
    SimpleDateFormat format = BRIEF_EXACT_FORMAT.get();

    if (format == null) {
      format = new SimpleDateFormat();
      BRIEF_EXACT_FORMAT.set(format);
    }

    return format;
  }
}
