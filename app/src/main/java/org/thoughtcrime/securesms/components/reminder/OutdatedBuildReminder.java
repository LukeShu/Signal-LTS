package org.thoughtcrime.securesms.components.reminder;

import android.content.Context;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.PlayStoreUtil;
import org.thoughtcrime.securesms.util.Util;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Reminder that is shown when a build is getting close to expiry (either because of the
 * compile-time constant, or remote deprecation).
 */
public class OutdatedBuildReminder extends Reminder {

  public OutdatedBuildReminder(final Context context) {
    setOkListener(v -> PlayStoreUtil.openPlayStoreOrOurApkDownloadPage(context));
    addAction(new Action(R.string.OutdatedBuildReminder_update_now, R.id.reminder_action_update_now));
  }

  @Override
  public @NonNull CharSequence getText(@NonNull Context context) {
    Util.ClientExpiration expiration = Util.getClientExpiration(context);

    return context.getResources().getString(R.string.OutdatedBuildReminder_your_version_of_signal_will_expire,
                                            DateUtils.ltsFormatInstant(context, Locale.getDefault(), expiration.deadline),
                                            expiration.reason);
  }

  @Override
  public boolean isDismissable() {
    return false;
  }

  public static boolean isEligible(@NonNull Context context) {
    Util.ClientExpiration expiration = Util.getClientExpiration(context);
    Instant now = Instant.now();
    return !expiration.isExpired(now) && Duration.between(now, expiration.deadline).toDays() <= 10;
  }
}
