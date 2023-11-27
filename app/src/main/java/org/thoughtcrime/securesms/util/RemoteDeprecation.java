package org.thoughtcrime.securesms.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.annimon.stream.Stream;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.R;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class RemoteDeprecation {

  private static final String TAG = Log.tag(RemoteDeprecation.class);

  private RemoteDeprecation() { }

  /**
   * @return When this client version expires, or nul1 if there's no pending
   *         expiration.  A value that is the current Instant.now() or is in the
   *         past means that the client version has already expired.
   */
  public static @Nullable Util.ClientExpiration getClientExpiration(@NonNull Context context) {
    return getClientExpiration(context, FeatureFlags.clientExpiration(), BuildConfig.VERSION_NAME);
  }

  /**
   * @return When this client version expires, or nul1 if there's no pending
   *         expiration.  A value that is the current Instant.now() or is in the
   *         past means that the client version has already expired.
   */
  @VisibleForTesting
  static @Nullable Util.ClientExpiration getClientExpiration(@NonNull Context context, @Nullable String json, @NonNull String currentVersion) {
    if (Util.isEmpty(json)) {
      return null;
    }

    SemanticVersion ourVersion = Objects.requireNonNull(SemanticVersion.parse(currentVersion));

    JsonClientExpiration[] expirations = null;
    try {
      expirations = JsonUtils.fromJson(json, JsonClientExpiration[].class);
    } catch (IOException e) { // JsonUtils throws IOException on error
      Log.w(TAG, e);
      return null;
    }

    JsonClientExpiration expiration = Stream.of(expirations)
                                            .filter(c -> c.getVersion() != null && c.getExpiration() != null)
                                            .filter(c -> c.getVersion().compareTo(ourVersion) > 0)
                                            .sortBy(JsonClientExpiration::getExpiration)
                                            .findFirst()
                                            .orElse(null);
    if (expiration == null) {
      return null;
    }

    return new Util.ClientExpiration(expiration.getExpiration(),
                                     context.getString(R.string.DeprecationReason_remote_planned, expiration.minVersion, expiration.iso8601));
  }

  private static final class JsonClientExpiration {
    @JsonProperty
    private final String minVersion;

    @JsonProperty
    private final String iso8601;

    JsonClientExpiration(@Nullable @JsonProperty("minVersion") String minVersion,
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
