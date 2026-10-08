# AudioFix

[![Build](https://github.com/Minner22/AudioFix/actions/workflows/build.yml/badge.svg)](https://github.com/Minner22/AudioFix/actions/workflows/build.yml)

Prosta aplikacja desktopowa do poprawiania ścieżek dźwiękowych w filmach tak, żeby dało się je odtworzyć na telewizorze,
który nie obsługuje wszystkich formatów audio (najczęściej **DTS**).

Zamiast ręcznie wpisywać w terminalu ścieżkę do `ffmpeg`, nazwę pliku i parametry konwersji, wystarczy wyklikać plik,
wybrać ścieżki i kliknąć **Start**.

## Funkcje

- Wybór jednego lub wielu plików w natywnym oknie Windowsa (`.mkv`, `.mp4`, `.m2ts`).
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
- [ffmpeg](https://ffmpeg.org/download.html) (`ffmpeg.exe` i `ffprobe.exe`). Jeśli nie ma go w `PATH`,
  aplikacja przy pierwszym uruchomieniu poprosi o wskazanie `ffmpeg.exe` i zapamięta tę lokalizację.

## Uruchamianie

```bash
mvn javafx:run   # uruchomienie aplikacji
mvn test         # testy
```

## Jak to działa

Dla każdego pliku aplikacja odczytuje listę strumieni przez `ffprobe`, a następnie buduje komendę `ffmpeg`, np.:

```
ffmpeg -i "Film.mkv" -map 0:v -map 0:1 -map 0:3 -map 0:t? -map_chapters 0 \
       -c copy -c:a:0 pcm_s24le -disposition:a:0 default \
       "Film_fixed.mkv"
```

Wybrane strumienie są mapowane jawnie, więc żadna ścieżka nie ginie po cichu. Konwertowane są tylko te ścieżki audio,
które tego wymagają, a reszta jest kopiowana.

## Roadmapa

Zadania prowadzone są w projekcie [AudioFix na GitHubie](https://github.com/users/Minner22/projects/4),
a gotowe wersje na stronie [Releases](https://github.com/Minner22/AudioFix/releases).

- ✅ **0.1.0, MVP:** wczytanie filmu, tabela ścieżek z wyróżnieniem DTS/TrueHD, wybór akcji i ścieżek
  domyślnych, plik wyjściowy, konwersja z postępem, logiem i anulowaniem.
- ✅ **0.2.0, szybka konwersja:** konwersja jednym kliknięciem (jak dawna ręczna komenda ffmpeg),
  tytuły przekonwertowanych ścieżek zgodne z nowym formatem, wersja aplikacji w tytule okna,
  okno „O programie” z wersjami i licencjami, automatyczne testy (CI) przy każdej zmianie.
- **0.3.0, kolejka:** wiele plików przetwarzanych po kolei, postęp każdego pliku, przeciąganie plików na okno.
- **0.4.0, wygląd:** nowy, płaski wygląd wg projektu (jasny i ciemny motyw zgodny z Windowsem),
  szacowany czas do końca konwersji, kolorowana konsola logów.
- **1.0.0, instalator:** instalator `.exe` z wbudowaną Javą i ffmpeg, automatyczne wydania na GitHubie.
- **1.1.0, wielojęzyczność:** interfejs po polsku i angielsku z wyborem języka.

Proces wydawania nowych wersji: [RELEASING.md](RELEASING.md).

## Licencja

[MIT](LICENSE). AudioFix uruchamia [FFmpeg](https://ffmpeg.org) jako osobny program, a FFmpeg ma własną licencję (LGPL/GPL).
