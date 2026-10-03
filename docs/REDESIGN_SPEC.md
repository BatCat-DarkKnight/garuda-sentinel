# Redesign Spec: Garuda Sentinel 2.1.0

Status: phase 1 (Report, Who can watch, Controls, bottom navigation). Phase 2 (First run, App detail, Photos) comes in a later release.

The idea in one line: **findings first.** Today the app is a set of data screens grouped by where the data came from. After the redesign it opens on a ranked list of what is worth a look, with a plain sentence and one action for each, and the raw data moves under "Your data".

## Ground rules

These apply to every change in this release.

1. **No change to what the app can access.** `AndroidManifest.xml` permissions, `queries`, exported components and `data_extraction_rules.xml` stay exactly as they are. The app must never request `INTERNET`.
2. **No new dependencies.** Use Compose, Material 3 and Navigation already in `libs.versions.toml`.
3. **No change to privacy defaults.** Photo GPS in exports stays off by default, location readings stay off by default, the encrypted ZIP stays the recommended option, the recents preview stays hidden, FLAG_SECURE stays opt-in, and "Delete all" removes exactly what it removes today.
4. **The app never changes other apps or settings.** Every action button opens an Android settings screen with a standard intent. Garuda Sentinel only reads.
5. **No web links that open automatically.** Where we point to an outside resource, show the address as text with a "Copy address" button. Opening a browser can leave history on a phone that someone else may be watching.
6. Copy is plain, short and specific. No em dashes anywhere in the UI, code comments or docs.

## Visual system (already built)

Tokens live in `ui/theme/Color.kt` (`Palette`) and components in `ui/components/Blocks.kt`. Use them; do not add loose hex values.

| Element | Spec |
|---|---|
| Background / surface | `Palette.Background` #0B1017, `Palette.Surface` #10161F |
| Dividers | 1dp `Palette.Hairline` #1D2735. Hairline dividers replace cards. No nested cards anywhere on these screens. |
| Text | `Text` #E9EEF3, `TextDim` #A9B4C2 for supporting text, `TextMuted` #8D99A8 for labels and captions |
| Accent | Gold `Accent` #E3AE3C, text on it `OnAccent` #14100A |
| Severity | High `SeverityHigh` #F08C7A, Medium `SeverityMedium` #E0A44E, Low `SeverityLow` #8D99A8, passed `Ok` #58C0A0 |
| Corners | 3dp for buttons and inputs, 4dp maximum anywhere. No pills except the Material `Switch` track. |
| Headlines | Newsreader Medium. Screen title 34sp (Report headline 42sp), line height about 1.05 to 1.1. Finding titles 23sp. |
| Body | IBM Plex Sans 15 to 16sp, line height about 1.55 |
| Labels and numbers | IBM Plex Mono. Section labels 11sp, uppercase, letter spacing 0.16em, `TextMuted` (or the severity colour for severity groups). Counts and values in Mono. |
| Gutter | 24dp side padding |
| Buttons | Primary: gold fill, 46 to 52dp tall, 3dp corners. Secondary: transparent with a 1dp outline in a new `Palette.OutlineStrong` (#2A3648). Destructive: transparent, `SeverityHigh` text, 1dp outline in a new `Palette.DangerOutline` (#5E3038). Add both tokens to `Palette` and `PaletteTest`. |
| High finding marker | 3dp `SeverityHigh` bar on the left edge of the row (as `FindingRow` already does) |
| Touch targets | At least 44dp |

## Navigation

The 11-item drawer becomes a bottom bar with four tabs:

| Tab | Icon (outline, 1.6dp stroke) | Contains |
|---|---|---|
| REPORT | shield | The Report screen (start destination) |
| EXPLORE | 2x2 grid | "Your data" list: Apps, Photos and media, Screen time, Files, Device and network. Each opens the existing screen. |
| CONTROLS | sliders | The new Controls screen |
| HELP | question mark in a circle | Permissions, FAQ, About (existing screens, listed as rows) |

Tab labels are IBM Plex Mono 11sp uppercase. Selected tab is gold, others `TextMuted`. A hairline sits above the bar. Who can watch, History and the detail screens open from these tabs with a back arrow (44dp target) in a top bar with a hairline under it. Remove the drawer and `Dest` entries that no longer have a drawer slot. Back from any tab root goes to Report, and back from Report exits.

## Screen 1: Report (replaces Home and Your Data)

Top to bottom:

1. **Header row.** "GARUDA SENTINEL" in Mono 12sp, letter spacing 0.18em, `TextMuted`. Hairline below.
2. **Headline.** "N things worth a look" in Newsreader 42sp. Zero findings: "Nothing needs a look". Before the first scan: "Run your first check" with the same layout and only the button.
3. **Tally line**, Mono 13sp: "1 high · 2 medium · 1 low · 7 passed", each count in its severity colour, omitting zero groups except "passed".
4. **Primary button** "Check again" (or "Run a check" before the first scan). Shows progress inline while scanning.
5. **Since your last check.** Label "SINCE <MMM d>" from the previous snapshot. Up to 5 lines from the existing scan comparison: gold Mono "+ " for added, `TextMuted` "− " for removed (use the minus sign U+2212, not a dash). Hidden when there is no earlier snapshot or memory-only mode is on.
6. **Findings**, ranked high, then medium, then low; ties by most recent. Each row: Mono severity label, Newsreader 23sp title, one sentence in `TextDim`, and a gold text action (44dp target). High rows get the left bar. Hairline between rows.
7. **Passed row.** "N checks passed" in `Ok`, chevron, opens Who can watch.
8. **"YOUR DATA"** list as in the Explore tab, with Mono counts on the right.

### Finding rules

Build these in a pure Kotlin `findings/` package from data the scan already collects, with unit tests for each rule. Do not add new collection.

| Severity | Rule | Title pattern | Action |
|---|---|---|---|
| HIGH | A user-installed app that came from outside a store (sideloaded) holds notification access, an enabled accessibility service or device admin | "An app from outside a store can read your notifications" (or "control your screen" / "manage this phone") | "Review this app" opens the app's row in Apps |
| HIGH | Any watcher at `WatcherLevel.ATTENTION` not covered above, such as a user-added certificate | Use the watcher's existing title | "Open Android setting" |
| MEDIUM | Photos with GPS coordinates | "N photos show exactly where they were taken" | "See the places" opens Media filtered to located photos |
| MEDIUM | User-installed apps with microphone, camera or precise location granted | "N apps you installed can use your microphone" (one finding per permission) | "See which apps" opens Apps sorted by Most access |
| MEDIUM | Background location granted to a user-installed app | "N apps can see where you are while closed" | "See which apps" |
| LOW | USB debugging on | "USB debugging is on" | "Open developer options" |
| LOW | Any other `WatcherLevel.CHECK` item | Watcher title | Its existing settings intent |

Name apps in the sentence when there are three or fewer. Everything at `WatcherLevel.FINE` counts toward "passed". If a check could not run because a permission was skipped, add a neutral row at the end ("Photos were not checked") with a "Set up" action, instead of hiding it.

## Screen 2: Who can watch (restyle of WatchersScreen)

1. Back arrow top bar.
2. Title "Who can watch this phone" (Newsreader 34sp). Intro: "These are the settings that let software see what you do. Garuda Sentinel only reads them. It never changes anything."
3. **Safety note, before any finding**, as a 3dp `Ok` left bar block: bold "Worried someone else set up your phone?", then "Removing monitoring software can alert the person who installed it. Talk to a tech safety advocate before you change anything." Then the address `stopstalkerware.org` as selectable Mono text with a "Copy address" text button (see ground rule 5).
4. Groups with coloured Mono labels: **NEEDS ATTENTION** (`SeverityHigh`, `ATTENTION` items as full rows with left bar, title, a Mono line like "1 APP · <APP NAME>", a sentence, and two buttons: primary "Review app", secondary "Open Android setting"), **WORTH KNOWING** (`SeverityMedium`, `CHECK` items as two-line rows with a chevron), **ALL CLEAR** (`Ok`, `FINE` items as compact rows: name on the left, short status on the right in `TextMuted`, or `Ok` for "On").
5. Footer in `TextMuted` 13sp: "Some things cannot be checked from inside an app: other apps' usage access, screen overlays, and which app is running a VPN." Keep it accurate to what the code actually cannot check.

## Screen 3: Controls (new home for settings and export)

Moves everything settings-like out of Scan History & Export. History itself stays reachable as a row here ("Past checks", Mono count, chevron).

1. Title "Controls".
2. **EXPORT A COPY.** Intro: "One file holding everything you tick. Together it says more about you than any single part, so protect it." Checkbox rows (50dp, hairlines) for each export section the exporter supports today, with today's defaults. "Photo locations" has an amber second line: "Off by default. These can show where you live and work." Then a radio group: "Password-protected ZIP" (selected by default, second line "Recommended. Opens with 7-Zip on Windows, Keka on Mac.") and "Plain JSON file". Password field (3dp corners, `OutlineStrong` border, Mono label "PASSWORD") with the existing strength hint underneath, shown only for the ZIP option. Full-width primary button "Choose where to save" opens the existing Save dialog.
3. **KEEPING RESULTS.** Switch row "Forget results when I close the app", second line "Nothing is written to storage. Turns off \"since your last check\"." This is the existing memory-only mode. Also move the screen privacy (FLAG_SECURE) switch here with its current wording.
4. **DELETE** (label in `SeverityHigh`). A sentence built from real counts: "Removes N saved checks, N location readings and access to your <folder> folder. Files you exported stay where you saved them." Destructive button "Delete all scan data" with the existing confirmation dialog.

## Out of scope for 2.1.0

First run (EULA stays as is), App detail, Photos map and "Clean before you share", new collectors, the Chrome extension.

## Done means

- Build, unit tests (existing plus the new findings tests), lint with 0 errors, and `PaletteTest` all pass.
- `aapt2 dump permissions` on the release APK matches 2.0.0 exactly.
- Checked on a real phone: first launch, scan, every finding action, every Controls row, export (ZIP and JSON), Delete all, rotation, back behaviour across tabs, and TalkBack labels on the bottom bar and buttons.
- Version 2.1.0, versionCode 4.
