# Wydawanie nowej wersji AudioFix

## Schemat wersji

[SemVer](https://semver.org/lang/pl/): `MAJOR.MINOR.PATCH`

- **MINOR** (`0.2.0` → `0.3.0`): nowe funkcje widoczne dla użytkownika,
- **PATCH** (`0.3.0` → `0.3.1`): tylko poprawki błędów,
- **MAJOR**: `1.0.0` = pierwsza wersja z instalatorem, gotowa dla zwykłego użytkownika. Wersje `0.x` to wersje rozwojowe.

Plan wydań (milestone'y na GitHubie):

| Wersja | Zawartość |
|---|---|
| `0.1.0` | MVP (wydane) |
| `0.2.0` | szybka konwersja (#21), tytuły ścieżek (#48), okno „O programie” (#51) |
| `0.3.0` | kolejka (#12, #13, #47, #49) |
| `0.4.0` | wygląd wg projektu Stitch, ciemny motyw (#43-#46, #35) |
| `1.0.0` | instalator z ffmpeg (#14, #24) |
| `1.1.0` | wielojęzyczność (#28-#33) |

## Wersja w budowie: `-SNAPSHOT`

- `master` ma **zawsze** wersję z dopiskiem `-SNAPSHOT`, np. `0.3.0-SNAPSHOT` = „przyszła wersja 0.3.0, jeszcze niewydana”.
- Wersja bez `-SNAPSHOT` istnieje tylko w commicie wydania, oznaczonym tagiem.
- Kolejne PR-y nie zmieniają wersji. Zmienia ją tylko proces wydania.

## Przed wydaniem

- [ ] zadania z milestone'u są zamknięte (milestone można zamknąć na GitHubie),
- [ ] ostatni build na `master` jest zielony (plakietka w README / zakładka *Actions*),
- [ ] lokalnie: `git checkout master`, `git pull`, `git status` bez zmian,
- [ ] ręczny test na prawdziwym filmie: wczytanie, zmiana ścieżek, konwersja, anulowanie,
- [ ] wynik odtworzony na telewizorze,
- [ ] ✅ przy wydawanej wersji w sekcji „Roadmapa” w `README.md`.

## Wydanie krok po kroku (PowerShell)

> **Uwaga:** w PowerShellu argumenty `-D…` **muszą być w cudzysłowach**.
> Bez nich PowerShell rozcina `-DnewVersion=0.2.0` na kropce i Maven ustawia wersję `0`.

Przykład dla wersji `0.2.0` (następna w budowie: `0.3.0-SNAPSHOT`):

```powershell
# 1. Wersja wydania
mvn versions:set "-DnewVersion=0.2.0" "-DgenerateBackupPoms=false"
mvn test
git add pom.xml
git commit -m "Release 0.2.0"

# 2. Tag z opisem
git tag -a v0.2.0 -m "AudioFix 0.2.0"

# 3. Następna wersja w budowie
mvn versions:set "-DnewVersion=0.3.0-SNAPSHOT" "-DgenerateBackupPoms=false"
git add pom.xml
git commit -m "Prepare 0.3.0-SNAPSHOT"

# 4. Wypchnięcie commitów i tagu
git push origin master --follow-tags

# 5. Wydanie na GitHubie z automatycznymi notatkami
gh release create v0.2.0 --title "AudioFix 0.2.0" --generate-notes
```

Sprawdzenie:

```powershell
gh release list
git show v0.2.0:pom.xml | Select-String "<version>" | Select-Object -First 1   # 0.2.0
Select-String "<version>" pom.xml | Select-Object -First 1                      # 0.3.0-SNAPSHOT
```

Krok 5 zniknie, gdy będzie działać automatyczne wydanie po tagu (#40).

### Dlaczego commity wydania idą bezpośrednio na `master`

Reguła ochrony `master` wymaga PR i zielonego CI, ale **rola administratora repo ma wyjątek**.
Commity wydania zmieniają tylko wersję w `pom.xml`, więc PR nic tu nie wnosi.
Jeśli wolisz przez PR: gałąź `release/0.2.0` z commitem `Release 0.2.0`, PR i squash merge, a tag ustawiasz po merge'u na commicie z `master`.

## Notatki wydania

`--generate-notes` zbiera tytuły PR-ów od poprzedniego tagu. Dlatego:

- **tytuł PR** = czytelny opis zmiany po angielsku (staje się tytułem commita po squashu i pozycją w notatkach),
- **label PR** decyduje o sekcji w notatkach (`.github/release.yml`):
    - `enhancement`: nowe funkcje,
    - `bug`: poprawki,
    - `documentation`: dokumentacja,
    - `infrastructure`: build, CI i proces wydań,
    - `skip-changelog`: nie pokazuj w notatkach (np. poprawka literówki w komentarzu).
- Najprościej: PR dostaje ten sam label co zamykane przez niego issue.

## Poprawka wydanej wersji (PATCH)

Gdy w wydanej wersji (np. `0.2.0`) jest błąd:

- **na `master` nie ma jeszcze niedokończonych funkcji:** poprawka normalnym PR-em do `master`, potem wydanie `0.2.1` według kroków wyżej (z `"-DnewVersion=0.2.1"`, a następna wersja w budowie zostaje bez zmian, np.
  `0.3.0-SNAPSHOT`),
- **na `master` są już niedokończone funkcje:** gałąź z tagu i wydanie z niej:
  ```powershell
  git checkout -b release/0.2 v0.2.0
  # poprawka (commit albo cherry-pick z master)
  mvn versions:set "-DnewVersion=0.2.1" "-DgenerateBackupPoms=false"
  git add pom.xml; git commit -m "Release 0.2.1"
  git tag -a v0.2.1 -m "AudioFix 0.2.1"
  git push origin release/0.2 --follow-tags
  gh release create v0.2.1 --title "AudioFix 0.2.1" --generate-notes --target release/0.2
  ```
  Poprawkę trzeba potem przenieść też na `master` (PR z cherry-pickiem).
