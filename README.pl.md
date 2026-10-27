# AudioFix

[![Build](https://github.com/Minner22/AudioFix/actions/workflows/build.yml/badge.svg)](https://github.com/Minner22/AudioFix/actions/workflows/build.yml)

[English](README.md) | **Polski**

Prosta aplikacja desktopowa do poprawiania ścieżek dźwiękowych w filmach tak, żeby dało się je odtworzyć na telewizorze,
który nie obsługuje wszystkich formatów audio (najczęściej **DTS**).

Zamiast ręcznie wpisywać w terminalu ścieżkę do `ffmpeg`, nazwę pliku i parametry konwersji, wystarczy wyklikać plik,
wybrać ścieżki i kliknąć **Start**.

Aplikacja działa po polsku i po angielsku: język zmienia przełącznik **PL | EN** na pasku narzędzi, w każdej chwili i bez restartu.

## Funkcje

- Wybór jednego lub wielu plików w natywnym oknie Windowsa (`.mkv`, `.mp4`, `.m2ts`) albo przeciągnięcie ich na okno.
- Podgląd wszystkich ścieżek pliku (wideo, audio, napisy): kodek, liczba kanałów, język i tytuł.
- **Podświetlanie ścieżek DTS** (także DTS-HD MA / DTS:X) z automatycznie ustawioną konwersją.
- Konwersja wybranych ścieżek audio do formatu:
  - **PCM 24-bit** (`pcm_s24le`), domyślnie,
  - E-AC3 (Dolby Digital Plus),
  - AC3 (Dolby Digital),
  - AAC.
- Usuwanie zbędnych ścieżek audio i napisów.
- Zmiana ścieżki domyślnej (audio i napisy).
- Wideo, rozdziały i załączniki (np. fonty do napisów) są kopiowane bez zmian, więc konwersja jest szybka.
- Wynik zapisywany jest jako nowy plik `.mkv`, domyślnie obok oryginału z suffixem `_fixed`
  (np. `Film.mkv` → `Film_fixed.mkv`), z możliwością zmiany miejsca i nazwy.
- **Kolejka**: wiele plików przetwarzanych jeden po drugim, z paskiem postępu i logiem.

## Stack

| Warstwa | Technologia |
|---|---|
| Język | Java 25 |
| UI | JavaFX 25 + FXML (Scene Builder) |
| Build | Maven |
| Analiza plików | `ffprobe` (JSON) + Jackson |
| Konwersja | `ffmpeg` uruchamiany przez `ProcessBuilder` |
| Testy | JUnit 5 |
| Dystrybucja | `jpackage` (instalator `.exe` z wbudowanym JRE) |

## Wymagania

- JDK 25
- Maven 3.9+
- [ffmpeg](https://ffmpeg.org/download.html) (`ffmpeg.exe` i `ffprobe.exe`) do uruchamiania ze źródeł
  (instalator ma własny). Jeśli nie ma go w `PATH`, aplikacja przy pierwszym uruchomieniu poprosi o wskazanie
  `ffmpeg.exe` i zapamięta tę lokalizację.

## Uruchamianie

```bash
mvn javafx:run   # uruchomienie aplikacji
mvn test         # testy
```

## Instalacja

Instalator `AudioFix-<wersja>.exe` ma w sobie Javę, więc nie trzeba jej instalować. Instaluje się na koncie
bieżącego użytkownika do `%LOCALAPPDATA%\AudioFix` (bez uprawnień administratora) i dodaje AudioFix do menu Start
oraz skrót na pulpicie. Nowsza wersja instalatora zastępuje starszą, a odinstalowanie jest w
*Ustawienia → Aplikacje*. Instalator zawiera też `ffmpeg.exe` i `ffprobe.exe`, więc po instalacji AudioFix od razu konwertuje pliki.
Ścieżka wskazana w [Ustawienia ffmpeg] nadal ma pierwszeństwo, np. dla nowszego ffmpeg.

AudioFix zawiera [FFmpeg](https://ffmpeg.org) na licencji GNU LGPL 3.0: build *shared* z
[BtbN/FFmpeg-Builds](https://github.com/BtbN/FFmpeg-Builds), wersja widoczna w oknie „O programie”,
licencja w `app\ffmpeg\LICENSE.txt` w folderze instalacji, kod źródłowy na https://ffmpeg.org/download.html.
AudioFix uruchamia ffmpeg jako osobny program i go nie modyfikuje.

### Budowanie instalatora

  ```bash
  mvn -Pinstaller package                             # instalator target/installer/AudioFix-<wersja>.exe
  mvn -Pinstaller package -Dinstaller.type=app-image  # sam folder target/installer/AudioFix z AudioFix.exe
  ```

- Instalator `.exe` wymaga [WiX Toolset](https://wixtoolset.org), np. `winget install --id WiXToolset.WiXToolset -e`
  w PowerShellu uruchomionym jako administrator (włącza też potrzebny .NET Framework 3.5).
  Wariant `app-image` działa bez WiX: folder można spakować do `.zip` i uruchamiać `AudioFix.exe` bez instalacji.
- Dodaj `-DskipTests`, żeby nie czekać na testy.
- Wbudowana Java zawiera tylko potrzebne moduły i dane lokalizacji dla polskiego i angielskiego.
- ffmpeg jest pobierany przy budowaniu (ok. 80 MB, raz, potem z kopii w `~/.m2`) i sprawdzany sumą SHA-256.
  Wersję zmienia się w `pom.xml` (`ffmpeg.release`, `ffmpeg.name`, `ffmpeg.sha256`).
- Sam instalator jest po angielsku.

### Logi

Aplikacja zapisuje log do `%LOCALAPPDATA%\AudioFix\logs\audiofix.log`: jeden plik na dzień, przez 14 dni,
razem najwyżej 50 MB. Folder otwiera przycisk **Folder logów** w oknie „O programie”. Przy zgłaszaniu błędu
dołącz plik z dnia, w którym wystąpił.

## Jak to działa

Dla każdego pliku aplikacja odczytuje listę strumieni przez `ffprobe`, a następnie buduje komendę `ffmpeg`, np.:

```
ffmpeg -i "Film.mkv" -map 0:v -map 0:1 -map 0:3 -map 0:t? -map_chapters 0 \
       -c copy -c:a:0 pcm_s24le -disposition:a:0 default \
       "Film_fixed.mkv"
```

Wybrane strumienie są mapowane jawnie, więc żadna ścieżka nie ginie po cichu. Konwertowane są tylko te ścieżki audio,
które tego wymagają, a reszta jest kopiowana.

## Tłumaczenia

Teksty są w `src/main/resources/pl/audiofix/i18n/`: `messages.properties` (polski, plik bazowy)
i `messages_en.properties`. Pliki są w UTF-8, a w tekście z `{0}` apostrof zapisuje się podwójnie (`''`).
FXML i kod biorą każdy tekst z tych plików, co sprawdzają `TranslationFilesTest` i `NoHardcodedTextsTest`.

Jak dodać język, np. niemiecki (`de`):

1. Skopiuj `messages_en.properties` do `messages_de.properties` i przetłumacz teksty.
   Teksty liczone (`*.count.one`, `*.count.many`, …) muszą mieć dokładnie te formy, które daje reguła odmiany języka.
2. Dodaj regułę odmiany do `TextFormats.pluralForm` i `TextFormats.pluralForms` (domyślnie jak angielski: one / many).
3. Dodaj `GERMAN(Locale.of("de"))` do `Language`, a jeśli ma być wybierany na niemieckim Windowsie, także do `Language.forSystem`.
4. Dodaj przycisk do pola `language-switch` w `main-view.fxml` (`toolbar.language.german` z podpowiedzią
   w każdym pliku) oraz do mapy przycisków i wyboru języka w `MainController.setLanguageSettings`.
5. Dodaj język do `--include-locales` w `pom.xml`, żeby instalator formatował liczby.
6. Dodaj plik do list w `TranslationFilesTest`.

## Roadmapa

Zadania prowadzone są w projekcie [AudioFix na GitHubie](https://github.com/users/Minner22/projects/4),
a gotowe wersje na stronie [Releases](https://github.com/Minner22/AudioFix/releases).

- ✅ **0.1.0, MVP:** wczytanie filmu, tabela ścieżek z wyróżnieniem DTS/TrueHD, wybór akcji i ścieżek
  domyślnych, plik wyjściowy, konwersja z postępem, logiem i anulowaniem.
- ✅ **0.2.0, szybka konwersja:** konwersja jednym kliknięciem (jak dawna ręczna komenda ffmpeg),
  tytuły przekonwertowanych ścieżek zgodne z nowym formatem, wersja aplikacji w tytule okna,
  okno „O programie” z wersjami i licencjami, automatyczne testy (CI) przy każdej zmianie.
- ✅ **0.3.0, kolejka:** wiele plików przetwarzanych po kolei, postęp każdego pliku, przeciąganie plików na okno.
- ✅ **0.4.0, wygląd:** nowy, płaski wygląd wg projektu (jasny i ciemny motyw zgodny z Windowsem),
  szacowany czas do końca konwersji, kolorowana konsola logów.
- ✅ **1.0.0, instalator:** instalator `.exe` z wbudowaną Javą i ffmpeg, automatyczne wydania na GitHubie.
- ✅ **1.1.0, wielojęzyczność:** interfejs po polsku i angielsku z wyborem języka.

Proces wydawania nowych wersji: [RELEASING.md](RELEASING.md).

## Licencja

[MIT](LICENSE). AudioFix uruchamia [FFmpeg](https://ffmpeg.org) jako osobny program, a FFmpeg ma własną licencję (LGPL/GPL).
