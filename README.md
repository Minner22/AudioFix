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

Zadania prowadzone są w projekcie [AudioFix na GitHubie](https://github.com/users/Minner22/projects/4).

- **Etap 0, Fundament:** szkielet projektu Maven + JavaFX/FXML.
- **Etap 1, Analiza pliku:** model domenowy, wykrywanie ffmpeg, odczyt ścieżek przez ffprobe.
- **Etap 2, Silnik konwersji:** budowanie komendy ffmpeg, uruchamianie z postępem i anulowaniem.
- **Etap 3, UI pojedynczego pliku:** tabela ścieżek, podświetlanie DTS, ścieżki domyślne, plik wyjściowy, pierwsza działająca konwersja.
- **Etap 4, Kolejka:** przetwarzanie wielu plików po kolei.
- **Etap 5, Dystrybucja:** instalator `.exe` przez `jpackage`.
