# Security and Privacy

Garuda Sentinel is a personal Android privacy tool by Corbray Technologies. It shows you the metadata your own phone holds and explains what it could reveal. This page describes what the app accesses, what it promises, how you can check those promises yourself, and how to report a vulnerability.

## What the app can access

Every permission is optional. Without one, that part of the scan is skipped and the app says so.

| Data | Permission | Notes |
|---|---|---|
| Installed apps, their versions, installers and granted permissions | `QUERY_ALL_PACKAGES` (granted at install) | Needed to list your apps. |
| Photo, video and audio details, including EXIF camera data | `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED` (Android 13+), `READ_EXTERNAL_STORAGE` (Android 10 to 12) | Android 14+ "limited access" is supported. File contents are not copied. |
| GPS coordinates saved inside photos | `ACCESS_MEDIA_LOCATION` | Android hides photo GPS without it. |
| App usage history (last 7 days) and screen-on time | Usage access (`PACKAGE_USAGE_STATS`), turned on in system Settings | Read from Android's own usage records. |
| Your location | `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` | Only when you tap "Read my location". One reading, stored on the phone. |
| Connection type, Wi-Fi name and BSSID, local addresses, DNS | `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Read from the phone. No network request is made. Wi-Fi name needs location access. |
| Names, sizes and dates of files in one folder you pick | None (a folder grant from the system picker) | Only the folder you choose. Released when you tap "Delete all scan data". |
| Device model, Android version, security patch, battery, keyboards | None | Values any app can read. |

The app never reads messages, contacts, call logs, browsing history or what you type. It has no accessibility service, notification listener, scheduled job or boot receiver, and its own code declares no services or receivers. The only two in the APK come from AndroidX libraries: Room's invalidation service (not exported, and unused because the app has one database instance) and the profile installer receiver (only the system can call it; it optimizes app startup).

## Privacy guarantees

- **No network access.** The app does not request the `INTERNET` permission, so Android blocks every network connection it could make. This includes code from its libraries.
- **Stays on the phone.** Scan results are stored in the app's private storage (a Room database and DataStore settings).
- **No backups.** `allowBackup` is off, and the data extraction rules exclude every data domain from cloud backup and device-to-device transfer.
- **Exports only when you ask.** Data leaves the app only through an export you start. You choose the file location in the system Save dialog and which categories to include. The export is a plain JSON file and **is not encrypted**, and it can include photo GPS coordinates, so keep it somewhere safe.
- **Delete everything.** "Delete all scan data" clears the database and releases folder access. Uninstalling the app also removes everything it stored.
- **No logging.** The app's own code makes no logging calls, so scan data is not written to the system log.

## How to check it yourself

- **No internet permission:** open Settings > Apps > Garuda Sentinel > Permissions (on some phones, "App info > Permissions > See all"). "Have full network access" (Internet) should not appear. With adb: `adb shell dumpsys package com.corbraytechnologies.garudasentinel | grep INTERNET` prints nothing.
- **Permissions in the APK:** `aapt2 dump permissions GarudaSentinel.apk` lists every permission the APK requests.
- **Genuine APK:** check the signing certificate before installing:
  ```
  apksigner verify --print-certs GarudaSentinel.apk
  ```
  The certificate SHA-256 digest must be:
  ```
  55:2b:23:e9:45:36:c7:7b:06:29:cc:d2:29:53:3e:e8:52:8f:92:bb:4f:c9:06:89:ea:8f:a3:6c:0c:65:0d:7e
  ```
  (`apksigner` prints it without colons: `552b23e94536c77b0629ccd229533ee8528f92bb4fc90689ea8fa36c0c650d7e`.)
  Each release also lists the APK file's own SHA-256 next to the download.
- **Build it yourself:** the source in this repository builds the app; see the README. Your build is signed with your own key, so it has a different certificate and cannot update an installed release.

## Reporting a vulnerability

Please report security or privacy issues privately by email to **info@corbraytechnologies.com**, with "Garuda Sentinel security" in the subject. If private vulnerability reporting is enabled on this repository, you can use that instead. Please include the app version, your Android version and phone model, and the steps to reproduce.

Please do not open a public issue for a vulnerability until it has been fixed.

## Supported versions

Only the latest release is supported. Package name: `com.corbraytechnologies.garudasentinel`.
