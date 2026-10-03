# Bundled fonts

The app has no `INTERNET` permission, so every font ships inside the APK as a `res/font`
resource. There are no downloadable fonts and no Google Fonts URLs.

| Family | Use | Files in `app/src/main/res/font` | Licence |
|---|---|---|---|
| Newsreader | Display, headlines, large titles | `newsreader_regular.ttf`, `newsreader_medium.ttf`, `newsreader_semibold.ttf` | SIL Open Font License 1.1, no Reserved Font Name |
| IBM Plex Sans | Body text and UI | `ibm_plex_sans.ttf` (variable, weights 400, 500 and 600 used) | SIL Open Font License 1.1, Reserved Font Name "Plex" |
| IBM Plex Mono | Numbers, section labels, technical values | `ibm_plex_mono_regular.ttf`, `ibm_plex_mono_medium.ttf` | SIL Open Font License 1.1, Reserved Font Name "Plex" |

Copyright notices:
- Newsreader: Copyright 2020 The Newsreader Project Authors (https://github.com/productiontype/Newsreader)
- IBM Plex: Copyright 2017 IBM Corp. with Reserved Font Name "Plex"

## Licence files

The OFL requires every copy of the fonts to carry the copyright notice and the licence. They are
in two places:
- `app/src/main/assets/licenses/Newsreader-OFL.txt`, `IBMPlexSans-OFL.txt`, `IBMPlexMono-OFL.txt`,
  which ship inside the APK next to the fonts.
- The fonts' own `name` table (copyright in record 0, licence text and URL in records 13 and 14),
  kept in every file, including the processed Newsreader files.

## Where the files came from

Downloaded on 2026-09-22 from the Google Fonts repository (`github.com/google/fonts`, `main` branch):

| Source file | Google Fonts upstream commit | SHA-256 of the file in the app |
|---|---|---|
| `ofl/newsreader/Newsreader[opsz,wght].ttf` (Version 1.003) | `1ece6a8` of productiontype/NewsReader | processed, see below |
| `ofl/ibmplexsans/IBMPlexSans[wdth,wght].ttf` (Version 3.201) | `3e31289` of googlefonts/plex | `3b031aa4216174205bd8471f88a49b91f093169e9e87bd5262242bc5967fe2e3` |
| `ofl/ibmplexmono/IBMPlexMono-Regular.ttf` (Version 2.3) | `9ab3b5b` of googlefonts/plex | `6a3412f058c7d8dfd9170c41e85ade48e5156ecb89356110ca57a0a27734af46` |
| `ofl/ibmplexmono/IBMPlexMono-Medium.ttf` (Version 2.3) | `9ab3b5b` of googlefonts/plex | `a9b4c49bb299e05b5f6c481e7fb5e78943d2793249a0c8874ab574a2d1ea6755` |

## What was changed and why

- **Newsreader** was turned into three static instances (weights 400, 500 and 600, optical size
  pinned at 30) and subset to Latin (Basic Latin, Latin-1, Latin Extended-A and common
  punctuation), about 77 KB each. `build_newsreader.py` in this folder rebuilds them. Newsreader
  has no Reserved Font Name, so the processed files keep the name, and they stay under the OFL.
- **IBM Plex Sans and IBM Plex Mono are not modified.** The OFL counts subsetting or instancing as
  making a modified version, and a modified version may not use a Reserved Font Name. Plex has
  one ("Plex"), so the files ship byte for byte as Google Fonts publishes them, and their SHA-256
  above can be checked against the originals. Google Fonts only publishes Plex Sans as a
  variable font, so the app selects weights 400, 500 and 600 with font variation settings.
  Only Regular and Medium of Plex Mono are included.

Characters outside a bundled font (for example an app name in another script) fall back to the
system font, as before.
