# Signal-LTS: Luke T. Shumaker's modified version of Signal private messenger

Hi, I'm Luke T. Shumaker.  This is my modified version of Signal
private messenger.

I use this as my daily driver; so even if I lag behind a bit on
merging new versions of Signal, you can be reasonably confident that I
won't lag behind so much that things stop working.

## User-visible differences from the standard version of Signal

Bug fixes:

 - Non-UTF-8 locales now work correctly.  Or at least the tests pass
   for non-UTF-8 locales.

## Switching from the standard version of Signal

1. Install the F-Droid app store from f-droid.org

2. In F-Droid, go to "Settings" (lower-right) -> "Repositories" (near
   the top) -> "+" in the upper right, and enter
   https://fdroid.lukeshu.com/repo

3. In Signal, go to hamburger -> "Settings" -> "Chats" -> "Backups"
   and make a backup, and ensure you know what the backup password is
   and what your PIN is.

4. Uninstall Signal.

5. In F-Droid, do a search for Signal, and Signal-LTS should show
   up.  Install it.

6. Open Signal, and restore from the backup.

7. In the Signal app info (from Android settings, or by long-holding
   on the Signal app icon), set it to be the default SMS app.  There
   is currently **not** an option to do that from within Signal itself
   (TODO).

8. (maybe?) If you had any connected devices, you may need to set
   those back up??

## Developer-visible differences from the standard version of Signal

 - Stricter `.editorconfig`, and compliance with it; so that you don't
   need to reconfigure your editor on a file-by-file basis.

 - `./gradlew qa` now works and passes on non-UTF-8 locales (a notable
   instance of which is the reproducible-builds Docker image), so that
   you can get better automated feedback.

 - `./gradlew test` now works, so you can get better automated
   feedback.

 - `./gradlew qa` no longer depends on `clean`, so it won't take
   forever anymore.

 - Can now be published to an F-Droid repo.  It now builds a universal
   APK rather than building separate APKs for each CPU architecture.
   This is arguably a downgrade for the user-experience, as the user
   will have larger files to download; but, F-Droid doesn't support
   split APKs, so this is necessary for being able to publish it as an
   F-Droid repo.

# Original Signal README

---

## Signal Android

Signal is a simple, powerful, and secure messenger.

Signal uses your phone's data connection (WiFi/3G/4G/5G) to communicate securely. Millions of people use Signal every day for free and instantaneous communication anywhere in the world. Send and receive high-fidelity messages, participate in HD voice/video calls, and explore a growing set of new features that help you stay connected. Signal’s advanced privacy-preserving technology is always enabled, so you can focus on sharing the moments that matter with the people who matter to you.

Currently available on the Play Store and [signal.org](https://signal.org/android/apk/).

<a href='https://play.google.com/store/apps/details?id=org.thoughtcrime.securesms&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height='80px'/></a>

### Contributing Bug reports
We use GitHub for bug tracking. Please search the existing issues for your bug and create a new one if the issue is not yet tracked!

https://github.com/signalapp/Signal-Android/issues

### Joining the Beta
Want to live life on the bleeding edge and help out with testing?

You can subscribe to Signal Android Beta releases here:
https://play.google.com/apps/testing/org.thoughtcrime.securesms

If you're interested in a life of peace and tranquility, stick with the standard releases.

### Contributing Code

If you're new to the Signal codebase, we recommend going through our issues and picking out a simple bug to fix (check the "easy" label in our issues) in order to get yourself familiar. Also please have a look at the [CONTRIBUTING.md](https://github.com/signalapp/Signal-Android/blob/main/CONTRIBUTING.md), that might answer some of your questions.

For larger changes and feature ideas, we ask that you propose it on the [unofficial Community Forum](https://community.signalusers.org) for a high-level discussion with the wider community before implementation.

### Contributing Ideas
Have something you want to say about Signal projects or want to be part of the conversation? Get involved in the [community forum](https://community.signalusers.org).

## Help

### Support
For troubleshooting and questions, please visit our support center!

https://support.signal.org/

### Documentation
Looking for documentation? Check out the wiki!

https://github.com/signalapp/Signal-Android/wiki

## Legal things
### Cryptography Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.
BEFORE using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing cryptographic functions with asymmetric algorithms.
The form and manner of this distribution makes it eligible for export under the License Exception ENC Technology Software Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section 740.13) for both object code and source code.

### License

Copyright 2013-2023 Signal

Licensed under the GNU AGPLv3: https://www.gnu.org/licenses/agpl-3.0.html

Google Play and the Google Play logo are trademarks of Google LLC.
