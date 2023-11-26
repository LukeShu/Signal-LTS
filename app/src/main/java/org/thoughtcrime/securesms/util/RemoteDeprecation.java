package org.thoughtcrime.securesms.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.annimon.stream.Stream;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.BuildConfig;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class RemoteDeprecation {

  private static final String TAG = Log.tag(RemoteDeprecation.class);

  private RemoteDeprecation() { }

  /**
   * @return The amount of time until this client version expires, or nul1 if
   *         there's no pending expiration.  A zero or negative Duration means
   *         that the client version has already expired.
   */
  public static @Nullable Duration getTimeUntilDeprecation() {
    return getTimeUntilDeprecation(FeatureFlags.clientExpiration(), Instant.now(), BuildConfig.VERSION_NAME);
  }

  /**
   * @return The amount of time until this client version expires, or null if
   *         there's no pending expiration.  A zero or negative Duration means
   *         that the client version has already expired.
   */
  @VisibleForTesting
  static @Nullable Duration getTimeUntilDeprecation(String json, @NonNull Instant currentTime, @NonNull String currentVersion) {
    if (Util.isEmpty(json)) {
      return null;
    }

    SemanticVersion ourVersion = Objects.requireNonNull(SemanticVersion.parse(currentVersion));

    ClientExpiration[] expirations = null;
    try {
      expirations = JsonUtils.fromJson(json, ClientExpiration[].class);
    } catch (IOException e) { // JsonUtils throws IOException on error
      Log.w(TAG, e);
      return null;
    }

    ClientExpiration expiration = Stream.of(expirations)
                                        .filter(c -> c.getVersion() != null && c.getExpiration() != null)
                                        .filter(c -> c.getVersion().compareTo(ourVersion) > 0)
                                        .sortBy(ClientExpiration::getExpiration)
                                        .findFirst()
                                        .orElse(null);
    if (expiration == null) {
      return null;
    }

    return Duration.between(currentTime, expiration.getExpiration());
  }

  private static final class ClientExpiration {
    @JsonProperty
    private final String minVersion;

    @JsonProperty
    private final String iso8601;

    ClientExpiration(@Nullable @JsonProperty("minVersion") String minVersion,
                     @Nullable @JsonProperty("iso8601") String iso8601)
    {
      this.minVersion = minVersion;
      this.iso8601    = iso8601;
    }

    public @Nullable SemanticVersion getVersion() {
      return SemanticVersion.parse(minVersion);
    }

    public @Nullable Instant getExpiration() {
      return DateUtils.parseIso8601(iso8601);
    }
  }

}
