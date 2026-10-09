# AudioFix — Product Requirements Document (PRD) & Design Brief

**Wersja:** 1.0  
**Status:** Zatwierdzony do implementacji  
**Autorzy / Zespół:** AudioFix Core Team  
**Docelowa platforma:** Desktop (Windows 11 / cross-platform via JavaFX 21+ & JVM)  
**Silnik multimedialny:** FFmpeg (binarka zewnętrzna, LGPL)  

---

## 1. Wprowadzenie i cel projektu

### 1.1. Problem
Współczesne telewizory (zwłaszcza ze zintegrowanymi systemami Smart TV jak webOS, Tizen, Android TV/Google TV) oraz soundbary/amplitunery bez licencji na kodeki DTS lub Dolby TrueHD / Atmos nie potrafią odtworzyć dźwięku z wysokojakościowych plików wideo (np. 4K UHD Remux, kopie BD w kontenerach `.mkv`, `.m2ts`, `.mp4`). Skutkuje to błędem odtwarzania lub całkowitym brakiem dźwięku przy jednoczesnym prawidłowym wyświetlaniu obrazu wideo (HEVC/H.264).

### 1.2. Rozwiązanie: AudioFix
**AudioFix** to lekkie, natywne narzędzie desktopowe (GUI oparte na JavaFX z płaskim stylem CSS), stanowiące graficzną nakładkę (wrapper) na potężny silnik **FFmpeg**. Program umożliwia bezstratne przepisanie wideo (passthrough / stream copy) oraz szybką selekcję i transkodowanie nieobsługiwanych strumieni audio do bezstratnego lub powszechnie zgodnego formatu (domyślnie: **PCM 24-bit / 16-bit** lub opcjonalnie **E-AC3 / AC3 / AAC**), wraz z selekcją napisów i ustawieniem domyślnych flag.

### 1.3. Główne zasady projektowe
1. **Zero niepotrzebnego ponownego kodowania obrazu:** Wideo zawsze pozostaje nienaruszone (`-c:v copy`).
2. **Przejrzystość:** Użytkownik jednym rzutem oka widzi, które ścieżki zostaną przekonwertowane (wyróżnienie barwne), które pozostaną nienaruszone, a które zostaną pominięte.
3. **Natywna ergonomia desktopowa:** Pełna zgodność ze standardem JavaFX Flat CSS (brak webowych anty-wzorców, brak hamburger menu czy pływających przycisków FAB).

---

## 2. Persony i grupy docelowe

- **Kino domowe & Entuzjasta 4K Remux:** Posiada pliki o wadze 30–80 GB z wieloma ścieżkami audio (TrueHD Atmos, DTS-HD MA, komentarze reżyserskie) i napisami PGS. Chce szybko naprawić plik pod odtwarzacz w telewizorze bez utraty jakości obrazu i bez konieczności wpisywania skomplikowanych komend w terminalu.
- **Użytkownik okazjonalny:** Pobiera serial lub film, który „nie ma dźwięku na TV”. Potrzebuje przycisku **„Szybka konwersja”** (1-click fix: zachowaj wideo + najlepszy dźwięk jako PCM).

---

## 3. Kluczowe wymagania funkcjonalne (Functional Requirements)

### 3.1. Zarządzanie kolejką plików (Queue Panel, ~25% szerokości)
- **#FR-01: Dodawanie plików:** Obsługa formatów `.mkv`, `.mp4`, `.m2ts` za pomocą przycisku „Dodaj pliki…” oraz Drag & Drop na obszar okna.
- **#FR-02: Wielozadaniowa kolejka (#12/#13):** Lista wczytanych plików ze stanami:
  - *Oczekuje* (Pending)
  - *W trakcie [X%]* (In Progress z mini-paskiem postępu)
  - *Gotowe* (Finished / Success)
  - *Błąd* (Error)
  - *Anulowano* (Cancelled)
- **#FR-03: Wybór elementu z kolejki:** Kliknięcie w element kolejki przełącza i prezentuje strumienie wybranego pliku w panelu głównym.
- **#FR-04: Usuwanie z kolejki:** Możliwość usunięcia zaznaczonego lub zakończonego zadania przyciskiem „Usuń z kolejki”.

### 3.2. Inspekcja i edycja ścieżek (Tracks Table, ~75% szerokości)
- **#FR-05: Ekstrakcja metadanych:** Integracja z `ffprobe` do szybkiego wykrywania wszystkich strumieni (Wideo, Audio, Napisy, Komentarze).
- **#FR-06: Tabela ścieżek z kolumnami:**
  1. `Zostaw` (Checkbox) — zaznaczenie czy strumień ma trafić do pliku wynikowego (dla wideo zablokowane na stałe `true`).
  2. `#` (ID strumienia FFmpeg, np. 0, 1, 2…).
  3. `Typ` (Badge/Ikona: Wideo / Audio / Napisy).
  4. `Kodek` (np. `hevc (Main 10)`, `truehd (Dolby TrueHD + Atmos)`, `dts (DTS-HD MA 5.1)`, `ac3`, `hdmv_pgs_subtitle`).
  5. `Kanały` (np. 8 ch, 6 ch, 2 ch).
  6. `Język` (np. `pol`, `eng`, `und`).
  7. `Tytuł` (tekst z metadanych, np. *TrueHD Atmos 7.1*, *Polski (dialogi)*).
  8. `Akcja` (ComboBox):
     - Dostępne opcje dla Audio: `Kopiuj (passthru)`, `PCM 24-bit` (domyślna dla niekompatybilnych), `PCM 16-bit`, `E-AC3`, `AC3`, `AAC`.
     - Dla Wideo i Napisów: zablokowane na `Kopiuj (passthru)`.
  9. `Domyślna` (Radio button oddzielnie dla grupy Audio i grupy Napisy) — ustawia flagę `default` w nagłówku kontenera Matroska.
- **#FR-07: Inteligentne reguły automatyczne:**
  - Kodeki DTS, DTS-HD MA, DTS:X oraz Dolby TrueHD są domyślnie mapowane na akcję `PCM 24-bit` z wizualnym wyróżnieniem wiersza (bursztynowo-pomarańczowy akcent).
  - Standardowe ścieżki AC3, E-AC3, AAC są domyślnie ustawiane na `Kopiuj`.
- **#FR-08: Ścieżka wyjściowa:** Pole tekstowe (read-only) z domyślnym sufiksem `.AudioFixed.mkv` oraz przycisk „Zmień…” (JavaFX `FileChooser`).

### 3.3. Wykonywanie konwersji i operacje w tle
- **#FR-09: Tryby konwersji:**
  - **Start:** Rozpoczyna przetwarzanie według niestandardowo skomfigurowanej tabeli ścieżek.
  - **Szybka konwersja (#21):** Automatyczny algorytm 1-klik: zachowuje wideo + konwertuje pierwszy/najwyższy jakościowo strumień przestrzenny do PCM 24-bit + zachowuje napisy pasujące do języka systemu.
- **#FR-10: Pasek postępu i estymacja czasu:** Wyliczanie postępu na podstawie czasu z logu FFmpeg (`time=00:15:32.10`), estymacja czasu pozostałego (ETA).
- **#FR-11: Blokada kontrolek i anulowanie:** W trakcie trwania zadania elementy edycyjne są zablokowane, a przycisk „Anuluj” staje się aktywny (wysyła sygnał `SIGINT` / `q` do procesu FFmpeg).
- **#FR-12: Konsola logów na żywo:** 5–6 linijkowy obszar `TextArea` (monospace), wyświetlający generowaną komendę CLI oraz kluczowe linie wyjścia FFmpeg (fps, q, bitrate, speed).

### 3.4. Walidacja i okna dialogowe
- **#FR-13: Walidacja audio:** Blokada uruchomienia z komunikatem ostrzegawczym, jeśli użytkownik odznaczy wszystkie ścieżki dźwiękowe (*„Zostaw co najmniej jedną ścieżkę audio.”*).
- **#FR-14: Okno „Ustawienia FFmpeg”:** Konfiguracja ścieżki do `ffmpeg.exe` i `ffprobe.exe` z automatycznym testem wersji.
- **#FR-15: Okno „O programie” (#24):** Informacja o wersji, link do repozytorium oraz nota licencyjna FFmpeg LGPL.
- **#FR-16: Przełącznik języka PL / EN (#31):** Płaski przełącznik segmentowy w prawym górnym rogu. Dynamiczne skalowanie szerokości przycisków (`USE_COMPUTED_SIZE`) zapobiegające ucinaniu dłuższych fraz w języku angielskim.

---

## 4. Wymagania niefunkcjonalne (Non-Functional Requirements)

| Kategoria | Wymaganie |
|---|---|
| **Rozmiar i responsywność okna** | Domyślny wymiar 1100×700 px, skalowalny (min. 960×600 px). SplitPane umożliwiający zmianę proporcji kolejki i tabeli. |
| **Wydajność** | Użycie strumieniowania dyskowego bez ładowania całych plików do RAM. Kopiowanie wideo ograniczone wyłącznie przepustowością I/O dysku (SSD/HDD). |
| **Stylistyka JavaFX CSS** | Czysty Flat Design bez gradientów i efektów skeuomorficznych. Proste selektory `.button`, `.table-view`, `.list-view`, `.text-area`, `.radio-button`, `.check-box`. |
| **Wsparcie dla Dark & Light Mode** | Natywna obsługa dwóch motywów: jasnego (Fluent Precision Light) oraz ciemnego (Flat Precision Dark). |
| **Współbieżność** | Operacje FFmpeg wykonywane w osobnym wątku roboczym `javafx.concurrent.Task` / `CompletableFuture`, zapobiegającym zamrażaniu wątku interfejsu (JavaFX Application Thread). |

---

## 5. Architektura interfejsu i wzorce wizualne (UI Architecture)

### 5.1. Układ okna (Layout Grid)
```
+-----------------------------------------------------------------------------------------------+
| [Ikona] AudioFix v1.0                     [Dodaj pliki...] [Ustawienia] [Szybka...]   [PL|EN] [O programie] [_][#][X] |
+------------------------------------+----------------------------------------------------------+
| KOLEJKA (25%)                      | ŚCIEŻKI: NazwaPliku.mkv (Rozmiar kontenera)              |
| +--------------------------------+ | +------------------------------------------------------+ |
| | Drive.2011.UHD... (W trakcie)  | | | [v] 0  Wideo  hevc       -    und  -     [Kopiuj      v] ( )| |
| | Flow.2024.UHD...   (Oczekuje)  | | | [v] 1  Audio  truehd  8 ch   eng  Atmos [PCM 24-bit  v] (*)| |
| | Married...         (Gotowe)    | | | [v] 2  Audio  ac3     6 ch   eng  5.1   [Kopiuj      v] ( )| |
| |                                | | | [v] 3  Audio  dts     6 ch   eng  MA    [PCM 24-bit  v] ( )| |
| | [Usuń z kolejki]               | | | [ ] 4  Audio  ac3     2 ch   eng  Koment[Kopiuj      v] ( )| |
| +--------------------------------+ | | [v] 6  Napisy pgs        -    pol  Polski[Kopiuj      v] (*)| |
|                                    | +------------------------------------------------------+ |
|                                    | Plik wyjściowy: [ D:\Wideo\Gotowe\...        ] [Zmień...] |
+------------------------------------+----------------------------------------------------------+
| [> Start] [⚡ Szybka konwersja] [Anuluj]  Konwertowanie: Ścieżka #1 i #3        42% (Pozostało 02:15) |
| [====================================Pasek postępu===========================================]|
| > ffmpeg -y -i "Drive..." -map 0:0 -c:v copy -map 0:1 -c:a:0 pcm_s24le ...                     |
| [out#0/matroska] Video: hevc (Main 10), Audio: pcm_s24le, 48000 Hz, 7.1, 9216 kb/s            |
+-----------------------------------------------------------------------------------------------+
```

### 5.2. Tokeny kolorystyczne (JavaFX CSS Palette)

#### Ciemny motyw (Dark Mode)
- **Tło bazowe (`.root`):** `#12131a`
- **Powierzchnie kontenerów / tabel:** `#1b1b23` / `#22222d`
- **Subtelne obramowania (borders):** `#2e303e`
- **Wyróżnienie konwersji (Warm Orange):** `#ea580c` (tło badge / obramowanie comboboxa), `#fb923c` (tekst kodeka)
- **Główny akcent akcji (Primary Blue):** `#0284c7` / `#38bdf8`
- **Konsola logów:** `#0d0e15` z tekstem `#a5b4fc` / `#38bdf8`

#### Jasny motyw (Light Mode)
- **Tło bazowe:** `#f8f9ff`
- **Powierzchnie kontenerów:** `#ffffff`
- **Obramowania kontrolek:** `#cbd5e1`
- **Wyróżnienie konwersji:** `#ea580c` (badge) z tłem ostrzegawczym `#fff7ed`
- **Akcent akcji:** `#0284c7`

---

## 6. Generowana komenda FFmpeg (Algorytm mapowania)

Przykład dla pliku referencyjnego `Drive.2011.UHD.BluRay.2160p...REMUX.mkv`:
```bash
ffmpeg -y -i "D:\Wideo\Drive.2011.UHD.BluRay.2160p..REMUX.mkv" \
  -map 0:0 -c:v copy \
  -map 0:1 -c:a:0 pcm_s24le -metadata:s:a:0 title="TrueHD Atmos 7.1 (PCM)" -disposition:a:0 default \
  -map 0:2 -c:a:1 copy -disposition:a:1 0 \
  -map 0:3 -c:a:2 pcm_s24le -metadata:s:a:2 title="DTS-HD MA 5.1 (PCM)" -disposition:a:2 0 \
  -map 0:6 -c:s:0 copy -disposition:s:0 default \
  -map 0:7 -c:s:1 copy -disposition:s:1 0 \
  "D:\Wideo\Gotowe\Drive.2011.UHD.BluRay.2160p.AudioFixed.mkv"
```

---

## 7. Plan wdrożenia i etapy (Roadmap)

1. **Faza 1 (MVP — UI & CLI Engine):**
   - Podstawowy layout JavaFX w oparciu o przygotowane ekrany Light & Dark mode.
   - Klasa `FFmpegProcessManager` obsługująca wywołania asynchroniczne i parsowanie strumienia stdout/stderr.
   - Obsługa pojedynczego pliku i podstawowe transkodowanie do PCM 24-bit.
2. **Faza 2 (Kolejka & Auto-reguły — #12/#13/#21):**
   - Obsługa kolejki wielu zadań z automatycznym przetwarzaniem wsadowym (batch processing).
   - Implementacja przycisku „Szybka konwersja” (analiza strumieni i dobór optymalnego profilu).
3. **Faza 3 (I18N & Polish/English UX — #31/#24):**
   - Wdrożenie pakietów zasobów `ResourceBundle` (PL/EN).
   - Dynamiczny układ kontrolek bez ucinania etykiet tekstowych.
   - Dialog „O programie” z kompletem licencji LGPL.
