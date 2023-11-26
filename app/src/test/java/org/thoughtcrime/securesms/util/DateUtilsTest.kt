package org.thoughtcrime.securesms.util

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.hamcrest.Matchers.equalTo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class DateUtilsTest {

  @Rule
  @JvmField
  val collector = ErrorCollector()

  private fun I(str: String): Instant = Instant.parse(str)
  private fun Z(str: String): ZoneId = ZoneId.of(str)

  @Test
  fun testFormatElapsedTime() {
    // Testdata.
    val testcases = mapOf<Duration?, String>(
      null to "00:00",
      Duration.ZERO to "00:00",

      Duration.ofSeconds(5) to "00:05",
      Duration.ofMinutes(5) to "05:00",
      Duration.ofHours(5) to "5:00:00",
      Duration.ofDays(5) to "120:00:00",

      Duration.ofSeconds(-5) to "00:-5",
      Duration.ofSeconds(-15) to "00:-15",
      Duration.ofMinutes(-5) to "00:-300",
      Duration.ofMinutes(-15) to "00:-900",
      Duration.ofHours(-5) to "00:-18000",
      Duration.ofHours(-15) to "00:-54000",
      Duration.ofDays(-5) to "00:-432000"
    )

    // Run.
    for ((input, exp) in testcases) {
      collector.checkThat("formatElapsedTime($input)", DateUtils.formatElapsedTime(input), equalTo(exp))
    }
  }

  @Test
  fun testGetTimeString() {
    // Types.
    class Input(
      val tz: ZoneId,
      val now: Instant,
      val x: Instant
    )
    class Output(
      // take Context+Locale
      val getBriefRelativeTimeSpanString: String,
      val getExtendedRelativeTimeSpanString: String,
      val getSimpleRelativeTimeSpanString: String,
      val getOnlyTimeString: String,
      val getTimeString: String,
      val getDayPrecisionTimeString: String,
      val getDayPrecisionTimeSpanString: String,
      val getMessageDetailsTimeString: String,
      val getConversationDateHeaderString: String,
      val getScheduledMessagesDateHeaderString: String,
      val getScheduledMessageDateString: String,
      // take just Locale
      val formatDateWithDayOfWeek: String,
      val formatDateWithYear: String,
      val formatDate: String,
      val formatDateWithMonthAndDay: String,
      val formatDateWithoutDayOfWeek: String
    )

    // Testdata.
    val testcases = mapOf<Input, Output>(
      Input(Z("-03:00"), I("2020-01-01T00:00:01Z"), I("2020-01-01T00:00:02Z")) to Output( // 1s in the future
        getBriefRelativeTimeSpanString = "Now",
        getExtendedRelativeTimeSpanString = "Now",
        getSimpleRelativeTimeSpanString = "Now",
        getOnlyTimeString = "09:00 pm",
        getTimeString = "09:00 pm",
        getDayPrecisionTimeString = "Today",
        getDayPrecisionTimeSpanString = "Today",
        getMessageDetailsTimeString = "Dec 31, 2019 09:00:02 PM GMT-03:00",
        getConversationDateHeaderString = "Today",
        getScheduledMessagesDateHeaderString = "Today",
        getScheduledMessageDateString = "Tonight at 09:00 pm",
        formatDateWithDayOfWeek = "Tue, Dec 31",
        formatDateWithYear = "Dec 31, 2019",
        formatDate = "Tue, Dec 31, 2019",
        formatDateWithMonthAndDay = "December 31",
        formatDateWithoutDayOfWeek = "Dec 31 2019"
      ),
      Input(Z("-03:00"), I("2020-01-01T00:00:01Z"), I("2020-01-05T00:00:01Z")) to Output( // 5d in the future
        getBriefRelativeTimeSpanString = "Now", // only designed to work on times in the past?
        getExtendedRelativeTimeSpanString = "Now", // only designed to work on times in the past?
        getSimpleRelativeTimeSpanString = "Now", // only designed to work on times in the past?
        getOnlyTimeString = "09:00 pm",
        getTimeString = "Sat 09:00 pm",
        getDayPrecisionTimeString = "Sat ", // trailing space?
        getDayPrecisionTimeSpanString = "Sat ", // trailing space?
        getMessageDetailsTimeString = "Jan 4, 2020 09:00:01 PM GMT-03:00",
        getConversationDateHeaderString = "Sat, Jan 4",
        getScheduledMessagesDateHeaderString = "Sat, Jan 4",
        getScheduledMessageDateString = "Tomorrow at 09:00 pm", // only designed to work on times <=1 day in the future?
        formatDateWithDayOfWeek = "Sat, Jan 4",
        formatDateWithYear = "Jan 4, 2020",
        formatDate = "Sat, Jan 4, 2020",
        formatDateWithMonthAndDay = "January 04",
        formatDateWithoutDayOfWeek = "Jan 4 2020"
      ),
      Input(Z("-03:00"), I("2020-01-01T00:00:01Z"), I("2019-12-26T00:00:01Z")) to Output( // 5d in the past
        getBriefRelativeTimeSpanString = "Wed",
        getExtendedRelativeTimeSpanString = "Wed 09:00 pm",
        getSimpleRelativeTimeSpanString = "09:00 pm", // no day indicator?
        getOnlyTimeString = "09:00 pm",
        getTimeString = "Wed 09:00 pm",
        getDayPrecisionTimeString = "Wed ", // trailing space?
        getDayPrecisionTimeSpanString = "Wed ", // trailing space?
        getMessageDetailsTimeString = "Dec 25, 2019 09:00:01 PM GMT-03:00",
        getConversationDateHeaderString = "Wed, Dec 25",
        getScheduledMessagesDateHeaderString = "Wed, Dec 25",
        getScheduledMessageDateString = "Tomorrow at 09:00 pm", // only designed to work on times in the future?
        formatDateWithDayOfWeek = "Wed, Dec 25",
        formatDateWithYear = "Dec 25, 2019",
        formatDate = "Wed, Dec 25, 2019",
        formatDateWithMonthAndDay = "December 25",
        formatDateWithoutDayOfWeek = "Dec 25 2019"
      ),
      Input(Z("-03:00"), I("2020-01-01T21:59:00Z"), I("2020-01-01T23:00:00Z")) to Output( // "now" is just before the "today"->"tonight" changeover (7pm), "val" is later tonight
        getBriefRelativeTimeSpanString = "Now", // only designed to work on times in the past?
        getExtendedRelativeTimeSpanString = "Now", // only designed to work on times in the past?
        getSimpleRelativeTimeSpanString = "Now", // only designed to work on times in the past?
        getOnlyTimeString = "08:00 pm",
        getTimeString = "08:00 pm",
        getDayPrecisionTimeString = "Today",
        getDayPrecisionTimeSpanString = "Today",
        getMessageDetailsTimeString = "Jan 1, 2020 08:00:00 PM GMT-03:00",
        getConversationDateHeaderString = "Today",
        getScheduledMessagesDateHeaderString = "Today",
        getScheduledMessageDateString = "Today at 08:00 pm",
        formatDateWithDayOfWeek = "Wed, Jan 1",
        formatDateWithYear = "Jan 1, 2020",
        formatDate = "Wed, Jan 1, 2020",
        formatDateWithMonthAndDay = "January 01",
        formatDateWithoutDayOfWeek = "Jan 1 2020"
      ),
      Input(Z("-03:00"), I("2020-01-01T22:00:00Z"), I("2020-01-01T23:00:00Z")) to Output( // "now" is just after the "today"->"tonight" changeover (7pm), "val" is later tonight
        getBriefRelativeTimeSpanString = "Now", // only designed to work on times in the past?
        getExtendedRelativeTimeSpanString = "Now", // only designed to work on times in the past?
        getSimpleRelativeTimeSpanString = "Now", // only designed to work on times in the past?
        getOnlyTimeString = "08:00 pm",
        getTimeString = "08:00 pm",
        getDayPrecisionTimeString = "Today",
        getDayPrecisionTimeSpanString = "Today",
        getMessageDetailsTimeString = "Jan 1, 2020 08:00:00 PM GMT-03:00",
        getConversationDateHeaderString = "Today",
        getScheduledMessagesDateHeaderString = "Today",
        getScheduledMessageDateString = "Tonight at 08:00 pm",
        formatDateWithDayOfWeek = "Wed, Jan 1",
        formatDateWithYear = "Jan 1, 2020",
        formatDate = "Wed, Jan 1, 2020",
        formatDateWithMonthAndDay = "January 01",
        formatDateWithoutDayOfWeek = "Jan 1 2020"
      )
    )

    for ((input, exp) in testcases) {
      try {
        mockkStatic(Instant::class)
        every { Instant.now() } returns input.now
        mockkStatic(ZoneId::class)
        every { ZoneId.systemDefault() } returns input.tz

        val context: Context = ApplicationProvider.getApplicationContext()
        val locale = Locale.US
        // take Context+Locale
        collector.checkThat("getBriefRelativeTimeSpanString(c, l, ${input.x})", DateUtils.getBriefRelativeTimeSpanString(context, locale, input.x), equalTo(exp.getBriefRelativeTimeSpanString))
        collector.checkThat("getExtendedRelativeTimeSpanString(c, l, ${input.x})", DateUtils.getExtendedRelativeTimeSpanString(context, locale, input.x), equalTo(exp.getExtendedRelativeTimeSpanString))
        collector.checkThat("getSimpleRelativeTimeSpanString(c, l, ${input.x})", DateUtils.getSimpleRelativeTimeSpanString(context, locale, input.x), equalTo(exp.getSimpleRelativeTimeSpanString))
        collector.checkThat("getOnlyTimeString(c, l, ${input.x})", DateUtils.getOnlyTimeString(context, locale, input.x), equalTo(exp.getOnlyTimeString))
        collector.checkThat("getTimeString(c, l, ${input.x})", DateUtils.getTimeString(context, locale, input.x), equalTo(exp.getTimeString))
        collector.checkThat("getDayPrecisionTimeString(c, l, ${input.x})", DateUtils.getDayPrecisionTimeString(context, locale, input.x), equalTo(exp.getDayPrecisionTimeString))
        collector.checkThat("getDayPrecisionTimeSpanString(c, l, ${input.x})", DateUtils.getDayPrecisionTimeSpanString(context, locale, input.x), equalTo(exp.getDayPrecisionTimeSpanString))
        collector.checkThat("getMessageDetailsTimeString(c, l, ${input.x})", DateUtils.getMessageDetailsTimeString(context, locale, input.x), equalTo(exp.getMessageDetailsTimeString))
        collector.checkThat("getConversationDateHeaderString(c, l, ${input.x})", DateUtils.getConversationDateHeaderString(context, locale, input.x), equalTo(exp.getConversationDateHeaderString))
        collector.checkThat("getScheduledMessagesDateHeaderString(c, l, ${input.x})", DateUtils.getScheduledMessagesDateHeaderString(context, locale, input.x), equalTo(exp.getScheduledMessagesDateHeaderString))
        collector.checkThat("getScheduledMessageDateString(c, l, ${input.x})", DateUtils.getScheduledMessageDateString(context, locale, input.x), equalTo(exp.getScheduledMessageDateString))
        // take just Locale
        collector.checkThat("formatDateWithDayOfWeek(l, ${input.x})", DateUtils.formatDateWithDayOfWeek(locale, input.x), equalTo(exp.formatDateWithDayOfWeek))
        collector.checkThat("formatDateWithYear(l, ${input.x})", DateUtils.formatDateWithYear(locale, input.x), equalTo(exp.formatDateWithYear))
        collector.checkThat("formatDate(l, ${input.x})", DateUtils.formatDate(locale, input.x), equalTo(exp.formatDate))
        collector.checkThat("formatDateWithMonthAndDay(l, ${input.x})", DateUtils.formatDateWithMonthAndDay(locale, input.x), equalTo(exp.formatDateWithMonthAndDay))
        collector.checkThat("formatDateWithoutDayOfWeek(l, ${input.x})", DateUtils.formatDateWithoutDayOfWeek(locale, input.x), equalTo(exp.formatDateWithoutDayOfWeek))
      } finally {
        unmockkStatic(ZoneId::class)
        unmockkStatic(Instant::class)
      }
    }
  }

  @Test
  fun testIsSame() {
    // Types.
    class Input(
      val tz: ZoneId,
      val a: Instant,
      val b: Instant
    )
    class Output(
      val isSameDay: Boolean,
      val isSameExtendedRelativeTimestamp: Boolean
    )

    // Testdata.
    val testcases = mapOf<Input, Output>(
      Input(Z("-03:00"), I("2020-01-02T02:59:59Z"), I("2020-01-01T03:00:01Z")) to Output(true, false)
    )

    // Run.
    for ((input, exp) in testcases) {
      mockkStatic(ZoneId::class) {
        every { ZoneId.systemDefault() } returns input.tz
        collector.checkThat("isSameDay(${input.a}, ${input.b})", DateUtils.isSameDay(input.a, input.b), equalTo(exp.isSameDay))
        collector.checkThat("isSameExtendedRelativeTimestamp(${input.a}, ${input.b})", DateUtils.isSameExtendedRelativeTimestamp(input.a, input.b), equalTo(exp.isSameExtendedRelativeTimestamp))
      }
    }
  }

  @Test
  fun testParseIso8601() {
    // Testdata.
    val testcases = mapOf<String?, Instant?>(
      null to null,
      "" to null,
      "bogus" to null,
      "2020-01-01T01:00:00Z" to I("2020-01-01T01:00:00Z")
    )

    // Run.
    for ((input, exp) in testcases) {
      collector.checkThat("parseIso8601($input)", DateUtils.parseIso8601(input), equalTo(exp))
    }
  }
}
