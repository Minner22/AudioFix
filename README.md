# AudioFix

[![Build](https://github.com/Minner22/AudioFix/actions/workflows/build.yml/badge.svg)](https://github.com/Minner22/AudioFix/actions/workflows/build.yml)

**English** | [Polski](README.pl.md)

A simple desktop app that fixes the audio tracks of videos so they play on a TV
that does not support every audio format (most often **DTS**).

Instead of typing the path to `ffmpeg`, the file name and the conversion options in a terminal,
you pick a file, choose the tracks and click **Start**.

The app speaks English and Polish: switch with **PL | EN** in the toolbar, at any time, without a restart.

## Features

- Pick one or more files in the native Windows dialog (`.mkv`, `.mp4`, `.m2ts`) or drop them on the window.
- See all tracks of a file (video, audio, subtitles): codec, channels, language and title.
- **DTS tracks are highlighted** (also DTS-HD MA / DTS:X) and set to be converted.
- Convert chosen audio tracks to:
  - **PCM 24-bit** (`pcm_s24le`), the default,
  - E-AC3 (Dolby Digital Plus),
  - AC3 (Dolby Digital),
  - AAC.
- Remove audio and subtitle tracks you do not need.
- Change the default track (audio and subtitles).
- Video, chapters and attachments (e.g. subtitle fonts) are copied as they are, so the conversion is fast.
- The result is a new `.mkv` file, by default next to the original with the `_fixed` suffix
  (e.g. `Film.mkv` → `Film_fixed.mkv`); you can change the place and the name.
- **Queue**: many files converted one after another, with a progress bar and a log.

## Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| UI | JavaFX 25 + FXML (Scene Builder) |
| Build | Maven |
| Reading files | `ffprobe` (JSON) + Jackson |
| Conversion | `ffmpeg` run with `ProcessBuilder` |
| Tests | JUnit 5 |
| Distribution | `jpackage` (`.exe` installer with its own JRE) |

## Requirements

- JDK 25
- Maven 3.9+
- [ffmpeg](https://ffmpeg.org/download.html) (`ffmpeg.exe` and `ffprobe.exe`) to run from the sources
  (the installer has its own). If it is not on the `PATH`, the app asks for `ffmpeg.exe` on the first start
  and remembers it.

## Running

```bash
mvn javafx:run   # start the app
mvn test         # tests
```

## Installation

The installer `AudioFix-<version>.exe` contains Java, so there is nothing else to install. It installs for
the current user into `%LOCALAPPDATA%\AudioFix` (no administrator rights needed) and adds AudioFix to the Start menu
and a desktop shortcut. A newer installer replaces an older version; uninstall in *Settings → Apps*.
The installer also contains `ffmpeg.exe` and `ffprobe.exe`, so AudioFix converts files right after installation.
A path chosen in [ffmpeg settings] still comes first, e.g. for a newer ffmpeg.

AudioFix contains [FFmpeg](https://ffmpeg.org) under the GNU LGPL 3.0: the *shared* build from
[BtbN/FFmpeg-Builds](https://github.com/BtbN/FFmpeg-Builds), its version is shown in the About window,
the license is in `app\ffmpeg\LICENSE.txt` in the installation folder, the source code at https://ffmpeg.org/download.html.
AudioFix runs ffmpeg as a separate program and does not modify it.

### Building the installer

```bash
mvn -Pinstaller package                             # installer target/installer/AudioFix-<version>.exe
mvn -Pinstaller package -Dinstaller.type=app-image  # just the folder target/installer/AudioFix with AudioFix.exe
```

- The `.exe` installer needs the [WiX Toolset](https://wixtoolset.org), e.g. `winget install --id WiXToolset.WiXToolset -e`
  in PowerShell run as administrator (it also turns on the needed .NET Framework 3.5).
  The `app-image` variant works without WiX: zip the folder and run `AudioFix.exe` without installing.
- Add `-DskipTests` to skip the tests.
- The bundled Java contains only the needed modules and the locale data for Polish and English.
- ffmpeg is downloaded while building (about 80 MB, once, then from the copy in `~/.m2`) and checked with SHA-256.
  Its version is set in `pom.xml` (`ffmpeg.release`, `ffmpeg.name`, `ffmpeg.sha256`).
- The installer itself is in English.

### Logs

The app writes its log to `%LOCALAPPDATA%\AudioFix\logs\audiofix.log`: one file a day, kept for 14 days,
at most 50 MB together. The **Log folder** button in the About window opens the folder. When reporting a bug,
attach the file from the day it happened.

## How it works

For each file the app reads the list of streams with `ffprobe` and builds an `ffmpeg` command, e.g.:

```
ffmpeg -i "Film.mkv" -map 0:v -map 0:1 -map 0:3 -map 0:t? -map_chapters 0 \
       -c copy -c:a:0 pcm_s24le -disposition:a:0 default \
       "Film_fixed.mkv"
```

The chosen streams are mapped one by one, so no track is lost silently. Only the audio tracks that need it
are converted; the rest is copied.

## Translations

The texts are in `src/main/resources/pl/audiofix/i18n/`: `messages.properties` (Polish, the base file)
and `messages_en.properties`. The files are UTF-8; in a text with `{0}` an apostrophe is written twice (`''`).
FXML and code take every text from these files; `TranslationFilesTest` and `NoHardcodedTextsTest` check it.

To add a language, e.g. German (`de`):

1. Copy `messages_en.properties` to `messages_de.properties` and translate the texts.
   Counted texts (`*.count.one`, `*.count.many`, …) need exactly the forms the language's plural rule gives.
2. Add the plural rule to `TextFormats.pluralForm` and `TextFormats.pluralForms` (English-like by default: one / many).
3. Add `GERMAN(Locale.of("de"))` to `Language` and, if it should be chosen on a German Windows, to `Language.forSystem`.
4. Add a toggle button to the `language-switch` box in `main-view.fxml` (`toolbar.language.german` and its tooltip
   in every file) and to the button map and the choice in `MainController.setLanguageSettings`.
5. Add the language to `--include-locales` in `pom.xml`, so the installer formats its numbers.
6. Add the file to the lists in `TranslationFilesTest`.

## Roadmap

Tasks are tracked in the [AudioFix project on GitHub](https://github.com/users/Minner22/projects/4),
finished versions are on the [Releases](https://github.com/Minner22/AudioFix/releases) page.

- ✅ **0.1.0, MVP:** loading a video, track table with DTS/TrueHD highlighted, choosing actions and default
  tracks, output file, conversion with progress, log and cancelling.
- ✅ **0.2.0, quick conversion:** one-click conversion (like the old manual ffmpeg command),
  titles of converted tracks matching the new format, app version in the window title,
  About window with versions and licenses, automatic tests (CI) for every change.
- ✅ **0.3.0, queue:** many files converted in turn, progress of each file, dropping files on the window.
- ✅ **0.4.0, look:** new flat design (light and dark theme following Windows),
  estimated time left, colored log console.
- ✅ **1.0.0, installer:** `.exe` installer with its own Java and ffmpeg, automatic releases on GitHub.
- ✅ **1.1.0, languages:** the interface in Polish and English with a language switch.

How new versions are released: [RELEASING.md](RELEASING.md) (in Polish).

## License

[MIT](LICENSE). AudioFix runs [FFmpeg](https://ffmpeg.org) as a separate program; FFmpeg has its own license (LGPL/GPL).
