# Garuda Sentinel: Architecture

Garuda Sentinel is an Android app that shows you the metadata your own phone already holds, and explains what it could reveal about you. It reads; it never uploads. This document describes how the app is built and how each privacy promise is enforced in code, so you can check the claims yourself.

Version 2.1.0. Package `com.corbraytechnologies.garudasentinel`. Distribution is by sideloaded APK.

## What the app does

| Area | What it shows | Permission |
|---|---|---|
| Apps | Every installed app: version, install and update dates, installer, target SDK, requested against granted permissions, with plain-language labels for the sensitive ones. | `QUERY_ALL_PACKAGES` (install time) |
| Photos, videos, audio | MediaStore details plus EXIF: camera, ISO, exposure and GPS. The source folder (Camera, Gallery album, Screenshots, Quick Share and so on) is estimated from the path. Android 14 limited access is supported. | `READ_MEDIA_*`, or `READ_EXTERNAL_STORAGE` on Android 10 to 12, plus `ACCESS_MEDIA_LOCATION` for photo GPS |
| Files | Name, size and dates of files in **one folder the user picks** with the system picker. | None; a folder grant through the Storage Access Framework |
| App usage | Last 7 days from Android's usage records: foreground time, opens, last used, screen-on time today, browser time. | Usage access, a special access granted in system Settings |
| Device and network | Model, Android version, security patch, screen, locale, uptime, battery, connection, VPN, local addresses, DNS, Wi-Fi name and BSSID where Android allows, keyboards. | `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` |
| Location | One reading, only when the user taps "Read my location". | Coarse and fine location |
| Who can watch | Accessibility services, notification access, device administrators, background location, apps from outside a store, apps with no launcher icon, certificates added by hand, default SMS app, screen lock, developer options, USB debugging. Read only. | None beyond the above |
| Report | Ranked findings built from the scan and the watcher signals (see "Findings" below), a tally, and how many checks passed. | None |
| Since your last check | Up to five differences from the previous scan: apps installed or removed, sensitive permissions newly granted, new watcher signals. Hidden in memory-only mode. | None |
| Export | A password-protected ZIP (selected by default) or one plain JSON file, to a location chosen in the system Save dialog, with per-category include and exclude. | None |

The app never reads message contents, contacts, call logs, browsing history or keystrokes. It has no accessibility service and no notification listener.

## Privacy model, and how each guarantee is enforced

| Guarantee | How it is enforced | How to verify |
|---|---|---|
| No network access | `AndroidManifest.xml` does not request `INTERNET`, so the OS blocks every connection, including from libraries. The image loader is Coil 3 without its network artifact, so no HTTP client is on the classpath. | `aapt2 dump permissions <apk>`; `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` |
| Results stay on the phone | Room database and DataStore in app-private storage. Nothing is written to shared storage. | Read `ExportService`; the only writer is the Save dialog |
| No backups or transfer | `allowBackup="false"` plus `res/xml/data_extraction_rules.xml`, which excludes every domain from cloud backup and device transfer. | Read the manifest and that file |
| Exports are deliberate | Writes go only to a URI returned by `ActivityResultContracts.CreateDocument`. Photo GPS is excluded unless the user opts in; location readings are off by default; the password-protected ZIP is selected by default. | `ExportBuilder`, `ExportService`, `ExportDefaults`, `ExportBuilderTest`, `ExportDefaultsTest` |
| Optional encryption | zip4j writes a standard ZIP with AES-256 (WinZip AE-2). No cryptography is written by hand. The single entry is always `garuda-export.json` with a fixed timestamp, so the readable ZIP metadata says nothing about the user. | `EncryptedExport`, `EncryptedExportTest`; open a file with 7-Zip |
| Nothing is invented | Fields Android does not expose are null or "Not available". Estimates are labelled "estimated". Signals that cannot be read, and checks skipped for a missing permission, are reported as not checked. | `WatcherRules`, `FindingRules`, `MediaDates`, collectors |
| Read only, no links | Every action opens an Android settings screen with a standard intent; the app changes no setting and no other app. Outside resources are shown as text with a "Copy address" button, so no browser history is created. | `SettingsIntents`, `CopyableAddress` |
| No background work | No services, receivers, workers or alarms of the app's own. Scans run only while the app is open. | Merged manifest: only the launcher activity plus AndroidX components |
| Delete means delete | "Delete all scan data" clears every table (Room then checkpoints and vacuums) and releases every folder grant the app holds. | `DataWiper` |
| Optional no-disk mode | "Forget results when I close the app" opens the database with `inMemoryDatabaseBuilder` and deletes any file left on disk. | `StorageMode`, `AppDatabase.create` |
| Screen privacy | `setRecentsScreenshotEnabled(false)` on Android 13+ keeps results out of the recents preview. An opt-in setting adds `FLAG_SECURE`. | `MainActivity` |
| No logging of personal data | The app's own code contains no logging calls. | `grep -rn "Log\." app/src/main` |

## Module map

Single module, `app`. Kotlin, Jetpack Compose, minSdk 29, target and compile SDK 36.

```
com.corbraytechnologies.garudasentinel
  GarudaApp.kt          Application and AppContainer: hand-rolled dependency wiring, application context only
  MainActivity.kt       Edge-to-edge Compose host; recents and FLAG_SECURE handling
  collect/              One collector per source. They read the OS and never guess:
                          AppCollector, MediaCollector, FileCollector, UsageCollector (with pure UsageMath),
                          DeviceCollector, LocationCollector, WatcherCollector
  data/                 Room entities, DAOs, database and migrations; DataStore settings; StorageMode
  scan/ScanCoordinator  Runs a scan on an application scope, per-step status, writes results and a snapshot
  export/               Export schema and pure ExportBuilder, ExportDefaults, ExportService (Save dialog),
                          EncryptedExport (zip4j), DataWiper
  findings/             Pure FindingRules (what the Report ranks), ReportText and WatcherRows (screen wording)
  model/                Serializable value types shared by collectors, export and snapshots
  permissions/          Permission and app-op checks, plus the intents that open the right system screens
  ui/                   Navigation host and bottom bar, view models, screens, shared components, theme
  utils/                Pure logic: AppCategorizer, SensitivePermissions, WatcherRules, ScanDiff,
                          MediaDates, AppLists, DeleteSummary, formatters
```

Rule of thumb in this codebase: decisions and formatting live in pure classes under `findings`, `utils` or `model` so they can be unit tested on the JVM, while Compose code stays declarative and Android APIs stay inside `collect`.

## Navigation

A bottom bar with four tabs replaces the drawer used up to 2.0.0. Each tab root is a screen without a back arrow; everything opened from a tab has a top bar with a back arrow.

| Tab | Root screen | Opens |
|---|---|---|
| Report | Ranked findings, "Since your last check", passed checks, Your data | Who can watch, one app's row in Apps, Apps sorted by access, located photos, Permissions, Android settings screens |
| Explore | "Your data": Apps, Photos and media, Screen time, Files, Device and network | The existing data screens |
| Controls | Export a copy, Keeping results (memory-only mode, screenshot blocking), Delete | Past checks |
| Help | Permissions, FAQ, About | Those screens |

Report is always at the bottom of the back stack: back from any other tab root returns to Report, and back from Report leaves the app.

## Findings

`FindingRules` builds the Report from data the scan already collects; it adds no collection.

| Severity | Rule | Action |
|---|---|---|
| High | An app the user installed from outside a store holds notification access, an accessibility service or device admin | Opens the app's row in Apps |
| High | Any other "needs attention" watcher (accessibility, notification access or device admin from a store app, a certificate added by hand, no screen lock) | Opens the matching Android setting |
| Medium | Photos with GPS coordinates | Opens Photos and media filtered to located photos |
| Medium | Apps the user installed with microphone, camera or precise location granted (one finding each) | Opens Apps sorted by most access |
| Medium | Background location granted to an app the user installed | Opens Apps sorted by most access |
| Low | USB debugging on | Opens developer options |
| Low | Any other "worth knowing" watcher (apps with no icon, apps from outside a store, developer options) | Opens the matching Android setting, or App info when one app is involved |

Findings are ranked high, then medium, then low, and by the newest app update or photo date within a severity. Apps are named when there are three or fewer. Garuda Sentinel itself is left out of the app-based rules. Every "all clear" watcher counts as passed, except "Not checked on this phone". A check that could not run (no photo access, no photo location access, or settings Android would not let the app read) is shown as a neutral row instead of being hidden.

## Scan flow

```
Report: Run a check (or Check again)
  ScanCoordinator.start() on the application scope (a scan survives navigation)
    parallel steps, each reporting Waiting, Running, Done, Skipped or Failed:
      AppCollector    -> apps table
      MediaCollector  -> media table (EXIF read per item; GPS needs ACCESS_MEDIA_LOCATION)
      FileCollector   -> files table (only when a folder was chosen)
      UsageCollector  -> in-memory usage summary (needs Usage access)
    writes one ScanLogEntity: counts and notes about anything skipped
    writes one ScanSnapshotEntity: apps with installer and granted sensitive permissions,
      plus watcher signals, used by ScanDiff for "What changed since your last scan"
```

A missing permission never blocks a scan. The step is marked Skipped with the reason, and the reason is stored in the scan log and shown in Past checks. The Report also shows a neutral row for a photo check that could not run.

## Data stored

| What | Where | Cleared by |
|---|---|---|
| Apps, media, files, locations, scan logs, scan snapshots | Room database `garuda_sentinel.db` in app-private storage, or in memory when "Forget results" is on | Delete all scan data, uninstall, or closing the app in memory-only mode |
| EULA accepted, chosen folder URI, block-screenshots setting | DataStore `settings` in app-private storage | Uninstall |
| "Forget results" flag | Marker file in no-backup storage, so it can be read before the database opens | Uninstall |
| Exported files | Wherever the user saved them | The user |

Schema changes use real Room `Migration` classes, never destructive fallback. Exported schemas live in `app/schemas` and an instrumented test migrates a real version 1 database and checks nothing is lost. The snapshot table keeps the newest 20 entries.

## Build and verify

Requirements: Android Studio with its bundled JDK, Android SDK platform 36.

```
./gradlew assembleDebug           # app/build/outputs/apk/debug
./gradlew testDebugUnitTest       # JVM unit tests
./gradlew connectedDebugAndroidTest   # migration test, needs a device or emulator
./gradlew lint                    # expect 0 errors
./gradlew assembleRelease         # minified with R8; unsigned unless keystore.properties exists
```

Release builds are minified and resource-shrunk. Room, kotlinx.serialization and DataStore supply their own keep rules; no extra ones are needed.

To verify a release APK:

```
apksigner verify --print-certs app-release.apk    # signing certificate
aapt2 dump permissions app-release.apk            # no INTERNET
```

See `SECURITY.md` for the published certificate fingerprint and how to report a vulnerability.
