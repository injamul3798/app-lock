# App Lock

Privacy-first Android app locker. It locks selected apps with a pattern, PIN, or fingerprint, and it never talks to a server.

## Privacy

Confirmed from the app source, manifest, and tests:

- **Offline.** The app requests only biometric and boot-completed permissions. It does not request `INTERNET` or any other network permission. Network libraries (OkHttp, Retrofit, Firebase) are not included, and the app code makes no HTTP calls.
- **Nothing leaves the device.** There is no analytics, crash reporting, ads, accounts, or cloud sync. Cloud backup and device transfer are disabled (`android:allowBackup="false"`, with file, preferences, and database data excluded).
- **No remote logging or tracing.** The app does not call Android `Log`, and it does not ship a telemetry or tracing SDK.
- **Screen content stays private.** The accessibility service listens only for foreground window changes so it can tell which app opened. `canRetrieveWindowContent` is false, so it does not read screen text, messages, or keystrokes.

## What is stored on the device

The app does keep one local file so the lock still works after you close it. That file is `applock_vault.enc` in the app's private storage. It is encrypted with AES-256-GCM. The key lives in the Android Keystore and is not written into the file.

The vault holds only what the lock needs:

- Lock settings (method, biometric on/off, auto-lock, screen-capture protection)
- The list of locked app package names
- A salted hash of your pattern or PIN, never the pattern or PIN itself (PBKDF2-HMAC-SHA256, 120,000 iterations, 16-byte random salt)
- A local intrusion log of failed unlock attempts (time, app name, package, attempt count, and method), capped at 50 entries

That intrusion log stays inside the encrypted vault. You can clear it in the app. It is not uploaded anywhere.

## Run locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio.
2. Select **Open** and choose this project directory.
3. Let Android Studio finish the Gradle import.
4. Run the app on an emulator or a physical device.
5. Complete setup, choose the apps to lock, then enable **App Lock Protection Service** in Android Accessibility settings. Protection does not start until that service is on.
