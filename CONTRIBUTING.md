# Contributing

Ostatnia aktualizacja: 2026-07-14

## Cel

Zasady pracy nad kidZone: issue, branchowanie, commity, Pull Requesty, review, testy, dokumentacja i Definition of Done.

## Zasady ogólne

- większa zmiana powinna mieć issue,
- każda zmiana trafia przez Pull Request,
- nie commitujemy bezpośrednio do `main`,
- PR ma jeden spójny cel,
- nie mieszamy refaktoru, funkcji i przypadkowych porządków,
- dokumentację aktualizujemy razem ze zmianą,
- nie mergujemy bez zielonego CI i zamkniętych blockerów,
- sekrety, PII i dane produkcyjne nie trafiają do repo.

## Branch naming

```text
feature/short-description
bugfix/short-description
hotfix/x.y.z-short-description
docs/short-description
refactor/short-description
test/short-description
ci/short-description
chore/short-description
```

Przykłady:

```text
feature/add-place-form
bugfix/map-empty-state
docs/engineering-handbook
hotfix/0.5.1-login-crash
```

Branch hotfix wychodzi z faktycznego stabilnego refa produkcyjnego, nie z gałęzi zawierającej niewydane zmiany.

## Commit messages

Preferowane prefiksy:

```text
feat:      nowa funkcja
fix:       poprawka błędu
docs:      dokumentacja
refactor:  refaktor bez zmiany zachowania
test:      testy
perf:      wydajność
chore:     utrzymanie
ci:        CI/CD
build:     build i zależności
```

Przykłady:

```text
feat: add place details screen
fix: handle permanent location denial
docs: update account deletion checklist
perf: limit map marker loading
```

## Pull Request

PR powinien zawierać:

- cel i zakres,
- powiązane issue,
- opis wpływu na użytkownika,
- sposób testowania,
- screenshoty lub nagrania dla UI,
- wpływ na architekturę, dane, release, security, privacy, performance i accessibility,
- plan migracji i rollbacku, jeśli dotyczy,
- informację o zmianach manifestu, Rules, Firebase SDK, Data Safety i account deletion.

## Draft PR

Użyj draftu, gdy:

- zakres jeszcze się zmienia,
- CI lub testy nie są kompletne,
- potrzebny jest wczesny feedback,
- migracja lub decyzja architektoniczna nie jest zamknięta.

Draft nie powinien być oznaczany jako ready tylko dlatego, że kod się kompiluje.

## Code review

Reviewer sprawdza:

- poprawność i czytelność,
- spójność zakresu,
- kierunek zależności,
- ownership danych i autoryzację,
- brak PII, tokenów i dokładnej lokalizacji w logach,
- brak nieograniczonych zapytań,
- idempotencję retry i eventów,
- loading, empty, error, offline i permission states,
- accessibility,
- testy i dokumentację,
- kompatybilność ze starszym buildem.

## Architecture review

Zmiana wymaga szczególnego review, gdy:

- Domain zaczyna zależeć od frameworka,
- ViewModel otrzymuje `Context` lub `NavController`,
- UI wywołuje Firebase albo DAO,
- DTO lub Entity trafia do Presentation,
- pojawia się nowy moduł, źródło danych lub cache,
- zmienia się model offline, migracja albo synchronizacja.

## Testy

Każda zmiana powinna mieć test na najniższym sensownym poziomie.

- bugfix otrzymuje test regresji, jeśli technicznie możliwe,
- Rules wymagają testów emulatora,
- migracje wymagają testu zgodności,
- UI krytyczne wymaga testu manualnego lub instrumentation,
- zmiany permissions wymagają macierzy urządzeń,
- account deletion wymaga pełnej checklisty,
- performance i koszt muszą być ocenione dla mapy, list, zdjęć i Firebase.

Flaky test wymaga issue, właściciela i planu naprawy. Sam rerun nie jest rozwiązaniem.

## Definition of Done

- [ ] PR ma jasny cel i zakres,
- [ ] kod buduje się lokalnie,
- [ ] CI jest zielone,
- [ ] testy pozytywne i negatywne przechodzą,
- [ ] bugfix ma test regresji albo uzasadnienie,
- [ ] UI ma wymagane stany,
- [ ] accessibility została sprawdzona,
- [ ] performance i koszty zostały ocenione,
- [ ] security i privacy zostały sprawdzone,
- [ ] migracja i rollback są opisane,
- [ ] dokumentacja jest aktualna,
- [ ] brak niezamierzonych zmian i sekretów,
- [ ] wszystkie rozmowy review są rozwiązane.

## CI przed merge

PR nie może zostać scalony, jeśli:

- build lub testy są czerwone,
- lint lub format check nie przechodzi,
- secret scan wykrywa problem,
- wymagany workflow nie został uruchomiony,
- nie wiadomo, dlaczego check został pominięty,
- branch jest nieaktualny w sposób zwiększający ryzyko konfliktu.

## Czego nie mergujemy

- prawdziwych kluczy API, tokenów i prywatnych kluczy,
- logów z danymi użytkowników,
- debug tokenów App Check,
- produkcyjnych plików konfiguracyjnych,
- szerokich Firestore lub Storage Rules bez testów,
- zapisu offline udającego sukces bez działającego replay,
- kodu z wyłączonym bezpieczeństwem tylko po to, aby przejść test,
- zmian, których wpływ na release jest nieznany.

## Release-impacting changes

Aktualizuj odpowiednie dokumenty:

- `docs/release/GO_NO_GO_CHECKLIST.md`,
- `docs/release/RELEASE_PROCESS.md`,
- `docs/release/RELEASE_NOTES_TEMPLATE.md`,
- `docs/release/PLAY_STORE_RELEASE.md`,
- `docs/release/VERSIONING.md`,
- `docs/release/HOTFIX_PROCESS.md`,
- `docs/legal/google-play-data-safety-draft.md`,
- `docs/legal/account-deletion-test-checklist.md`.

## Security reporting

Security issue oznacz jako `security`. Nie publikuj w issue realnych sekretów, danych użytkowników ani szczegółów ułatwiających aktywne nadużycie. Najpierw ogranicz wpływ, potem rotuj dane i dokumentuj incydent.

## Performance i koszty

Przy zmianach list, mapy, wyszukiwarki, Firebase, obrazów lub WorkManagera sprawdź:

- limity i paginację,
- debounce lub throttle,
- cache i lifecycle listenerów,
- recomposition,
- pamięć i główny wątek,
- retry, timeout i batch size,
- Firebase/Maps usage,
- możliwość wyłączenia funkcji.

## Dokumentacja

Dokumentacja powinna:

- odpowiadać aktualnemu kodowi,
- wskazywać źródło prawdy zamiast duplikować treść,
- mieć datę aktualizacji dla procesów zmiennych,
- zawierać realne ścieżki i komendy,
- odróżniać stan obecny od planu,
- nie obiecywać niewdrożonej funkcji.

## Narzędzia płatne

Przed dodaniem narzędzia lub usługi:

- sprawdź model płatności,
- określ właściciela kosztu,
- skonfiguruj budżet lub quota,
- opisz możliwość wyłączenia,
- oceń wpływ na prywatność i Data Safety.

## Merge method

Preferowany sposób:

```text
Squash and merge
```

Wyjątek powinien mieć uzasadnienie. Po hotfixie poprawka musi trafić również do aktywnej gałęzi rozwojowej.
