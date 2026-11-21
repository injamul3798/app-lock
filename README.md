<h1 align="center">App Lock</h1>

<p align="center">
  Lock the apps you choose with a pattern, PIN, or fingerprint.<br>
  The locker stays on the phone. No account. No tracking.
</p>

<p align="center">
  <a href="#walkthrough">Walkthrough</a>
  &nbsp;·&nbsp;
  <a href="#privacy">Privacy</a>
  &nbsp;·&nbsp;
  <a href="#on-device-vault">On-device vault</a>
  &nbsp;·&nbsp;
  <a href="#run-locally">Run locally</a>
</p>

## Walkthrough

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <img src="docs/screenshots/01-welcome.jpg" width="250" alt="Welcome screen"><br><br>
      <b>1. Get started</b><br>
      <sub>Open the app. Nothing to sign up for.</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <img src="docs/screenshots/02-choose-method.jpg" width="250" alt="Choose pattern, PIN, or fingerprint"><br><br>
      <b>2. Choose a lock</b><br>
      <sub>Pattern, PIN, or fingerprint.</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <img src="docs/screenshots/03-select-apps.jpg" width="250" alt="Search the app list"><br><br>
      <b>3. Browse apps</b><br>
      <sub>Search, or filter locked and system apps.</sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="33%">
      <img src="docs/screenshots/04-apps-selected.jpg" width="250" alt="Apps selected to lock"><br><br>
      <b>4. Lock your picks</b><br>
      <sub>Turn on each app you want protected.</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <img src="docs/screenshots/05-home.jpg" width="250" alt="Home screen with protection active"><br><br>
      <b>5. Protection on</b><br>
      <sub>See locked apps and failed attempts.</sub>
    </td>
    <td align="center" valign="top" width="33%">
      <img src="docs/screenshots/06-manage-apps.jpg" width="250" alt="Manage locked apps"><br><br>
      <b>6. Manage later</b><br>
      <sub>Add or remove apps whenever you want.</sub>
    </td>
  </tr>
</table>

## Privacy

| | | |
| --- | --- | --- |
| **Offline** | **Stays on the phone** | **Screen stays private** |
| No `INTERNET` permission. OkHttp, Retrofit, and Firebase are not in the app, and the code makes no HTTP calls. | No analytics, crash reporting, ads, or accounts. Cloud backup and device transfer are off. | The accessibility service only sees which app opened. It does not read screen text, messages, or keystrokes. |

Checked against the manifest, the app source, and the unit test that rejects an `INTERNET` permission.

## On-device vault

One file, `applock_vault.enc`, sits in the app's private storage so the lock still works after you leave. It is encrypted with AES-256-GCM. The key is in the Android Keystore.

| Kept in the vault | How |
| --- | --- |
| Lock settings | Method, fingerprint on or off, auto-lock, screen-capture protection |
| Locked apps | Package names only |
| Pattern or PIN | Salted PBKDF2-HMAC-SHA256 hash, 120,000 iterations. The pattern and PIN themselves are not saved. |
| Failed attempts | Time, app name, package, attempt count, and method. Capped at 50. Clear them in the app. They are not uploaded. |

## Run locally

**Prerequisite:** [Android Studio](https://developer.android.com/studio)

1. Open this folder in Android Studio and let Gradle finish importing.
2. Run the app on an emulator or a phone.
3. Finish setup, choose the apps to lock, then turn on **App Lock Protection Service** in Android Accessibility settings. Protection starts only after that service is on.
