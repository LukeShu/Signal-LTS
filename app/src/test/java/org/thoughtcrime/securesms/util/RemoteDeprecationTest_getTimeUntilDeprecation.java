package org.thoughtcrime.securesms.util;

import android.content.Context;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.testutil.EmptyLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class RemoteDeprecationTest_getTimeUntilDeprecation {

  private final Context context = mock(Context.class);

  private final String   json;
  private final Instant  currentDate;
  private final String   currentVersion;
  private final Duration timeUntilExpiration;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // Null json, no expiration
        { null, Instant.parse("2020-01-01T00:00:00Z"), "1.1.0", null },

        // Truncated JSON (syntax error), no expiration
        { "[ {\"minVersion\": \"1.1.1\", \"iso8601\": \"2020-01-01T00:00:01Z\"}", Instant.parse("2020-01-01T00:00:00Z"), "1.1.1", null },

        // Unquoted JSON key (syntax error), no expiration
        { "[ {\"minVersion\": \"1.1.1\", \"iso8601\": \"2020-01-01T00:00:01Z\"}," +
          "  {minVersion: \"1.1.1\", \"iso8601\": \"2020-01-01T00:00:01Z\" } ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.0", null },

        // Empty json, no expiration
        { "[]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.0", null },

        // Badly formatted minVersion, no expiration
        { "[ {\"minVersion\": \"1.1\", \"iso8601\": \"2020-01-01T00:00:01Z\"} ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.1", null },

        // Badly formatted date, no expiration
        { "[ {\"minVersion\": \"1.1.1\", \"iso8601\": \"20-01T00:00:01Z\"} ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.1", null },

        // Missing minVersion, no expiration
        { "[ {\"iso8601\": \"20-01T00:00:01Z\"} ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.1", null },

        // Missing date, no expiration
        { "[ {\"minVersion\": \"1.1.1\"} ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.1", null },

        // Missing expiration and date, no expiration
        { "[ {} ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.1", null },

        // Invalid inner object, no expiration
        { "[ { \"a\": 1 } ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.1", null },

        // We meet the min version, no expiration
        { "[ {\"minVersion\": \"1.1.1\", \"iso8601\": \"2020-01-01T00:00:01Z\"} ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.1", null },

        // We exceed the min version, no expiration
        { "[ {\"minVersion\": \"1.1.1\", \"iso8601\": \"2020-01-01T00:00:01Z\"} ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.2", null },

        // We expire in 1 second
        { "[ {\"minVersion\": \"1.1.1\", \"iso8601\": \"2020-01-01T00:00:01Z\"} ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.0", Duration.ofSeconds(1) },

        // We have already expired
        { "[ {\"minVersion\": \"1.1.1\", \"iso8601\": \"2020-01-01T00:00:01Z\"} ]", Instant.parse("2020-01-01T00:00:02Z"), "1.1.0", Duration.ofSeconds(-1) },

        // Use the closest expiration when multiple ones are listed
        { "[ {\"minVersion\": \"1.1.2\", \"iso8601\": \"2020-02-01T00:00:00Z\"}," +
          "  {\"minVersion\": \"1.1.3\", \"iso8601\": \"2020-03-01T00:00:00Z\"}," +
          "  {\"minVersion\": \"1.1.1\", \"iso8601\": \"2020-01-01T00:00:01Z\"} ]", Instant.parse("2020-01-01T00:00:00Z"), "1.1.0", Duration.ofSeconds(1) },

        // Mixed valid and invalid inner objects
        { "[ {\"minVersion\": \"1.1\",   \"iso8601\": \"2020-01-01T00:00:01Z\"}," + // badly formatted minVersion
          "  {\"minVersion\": \"1.1.1\", \"iso8601\":      \"02-01T00:00:01Z\"}," + // badly formatted date
          "  {\"minVersion\": \"1.1\",   \"iso8601\":      \"03-01T00:00:01Z\"}," + // badly formatted both
          "  {                           \"iso8601\": \"2020-04-01T00:00:01Z\"}," + // missing minVersion
          "  {\"minVersion\": \"1.1.1\"                                       }," + // missing date
          "  {                                                                }," + // missing both
          "  { \"a\": 1                                                       }," + // invalid inner object
          "  {\"minVersion\": \"1.1.1\", \"iso8601\": \"2020-12-01T00:00:01Z\"} ]", // valid
          Instant.parse("2020-12-01T00:00:00Z"), "1.1.0", Duration.ofSeconds(1) },
    });
  }

  public RemoteDeprecationTest_getTimeUntilDeprecation(String json, Instant currentDate, String currentVersion, Duration timeUntilExpiration) {
    this.json                = json;
    this.currentDate         = currentDate;
    this.currentVersion      = currentVersion;
    this.timeUntilExpiration = timeUntilExpiration;
  }

  @BeforeClass
  public static void setup() {
    Log.initialize(new EmptyLogger());
  }

  @Test
  public void getTimeUntilExpiration() {
    Util.ClientExpiration expiration = RemoteDeprecation.getClientExpiration(context, json, currentVersion);
    if (timeUntilExpiration == null) {
      assertNull(expiration);
    } else {
      assertNotNull(expiration);
      assertEquals(timeUntilExpiration, Duration.between(currentDate, expiration.deadline));
    }
  }
}
