# Contributing

## Cel

Dokument opisuje zasady pracy nad KidZone: branchowanie, Pull Requesty, review, testy i Definition of Done.

## Zasady ogólne

- Każda większa zmiana powinna mieć issue.
- Każda zmiana trafia przez Pull Request.
- PR powinien mieć jasny opis celu i zakresu.
- Nie mieszamy wielu niezwiązanych zmian w jednym PR.
- Dokumentację aktualizujemy razem ze zmianą, jeśli wpływa na proces, architekturę, release albo UX.
- Nie commitujemy bezpośrednio do `main`.
- Nie mergujemy PR bez zielonego CI.

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

## Commit messages

Preferowane prefiksy:

```text
feat:      nowa funkcja
fix:       poprawka błędu
docs:      dokumentacja
refactor:  refaktor bez zmiany zachowania
test:      testy
perf:      wydajność
chore:     zmiany techniczne / utrzymaniowe
ci:        CI/CD
build:     build / dependencies
```

Przykłady:

```text
feat: add place details screen
fix: handle missing location permission
docs: add release checklist
perf: limit map marker loading
```

## Pull Request

PR powinien zawierać:

- opis zmiany,
- powiązane issue,
- zakres zmian,
- sposób testowania,
- screenshoty lub nagrania dla UI,
- wpływ na release, security, performance i accessibility.

## Code review

Reviewer sprawdza:

- czy kod jest czytelny,
- czy zakres PR jest spójny,
- czy nie ma regresji UX,
- czy nie ma danych wrażliwych w logach,
- czy nie ma nieograniczonych zapytań,
- czy testy i dokumentacja są adekwatne.

## Definition of Done

Zmiana jest gotowa, gdy:

- [ ] PR ma jasny opis.
- [ ] Kod buduje się lokalnie.
- [ ] CI przechodzi.
- [ ] Testy są dodane lub świadomie pominięte.
- [ ] UI ma loading, empty i error states, jeśli dotyczy.
- [ ] Accessibility została sprawdzona, jeśli dotyczy UI.
- [ ] Performance został sprawdzony, jeśli dotyczy list, mapy, wyszukiwarki lub Firebase.
- [ ] Security/privacy zostały sprawdzone, jeśli dotyczy danych użytkownika, logowania, uploadu lub rules.
- [ ] Dokumentacja została zaktualizowana, jeśli zmiana wpływa na proces albo architekturę.

## CI przed merge

PR można mergować tylko wtedy, gdy workflow CI jest zielony.

Jeżeli CI jest czerwony, PR nie powinien być mergowany.

## Czego nie mergujemy

Nie mergujemy PR, jeśli:

- build nie przechodzi,
- testy są czerwone,
- lint zgłasza błędy,
- wykryto potencjalny sekret,
- zmiana dodaje prawdziwe klucze API, tokeny albo dane dostępowe,
- nie wiadomo, co zmiana właściwie robi,
- PR miesza kilka niezwiązanych tematów.

## Release changes

Zmiany wpływające na release muszą aktualizować odpowiednie dokumenty:

- `docs/release/GO_NO_GO_CHECKLIST.md`,
- `docs/release/RELEASE_PROCESS.md`,
- `docs/release/RELEASE_NOTES_TEMPLATE.md`,
- `docs/release/PLAY_STORE_RELEASE.md`,
- `docs/release/VERSIONING.md`,
- `docs/release/HOTFIX_PROCESS.md`.

## Security

Nie commitujemy:

- sekretów,
- tokenów,
- prywatnych kluczy,
- plików konfiguracyjnych z produkcyjnymi danymi dostępowymi,
- logów z danymi użytkowników.

Security issue oznaczamy jako `security` i nie wklejamy do niego realnych sekretów ani danych prywatnych.

## Performance

Przy zmianach list, mapy, wyszukiwarki, Firebase albo obrazów sprawdzić:

- limity zapytań,
- paginację,
- debounce/throttle,
- cache,
- rekompozycje Compose,
- koszty Firebase / Maps.

## Dokumentacja

Dokumentacja powinna być:

- praktyczna,
- aktualna,
- krótka tam, gdzie się da,
- konkretna tam, gdzie trzeba,
- powiązana z realnymi procesami projektu.

## Koszty i narzędzia płatne

Przed dodaniem narzędzia, które może generować koszty, najpierw sprawdzamy jego model płatności.

Przykłady narzędzi lub funkcji, które mogą wymagać dodatkowej weryfikacji kosztów:

- GitHub Advanced Security,
- CodeQL dla prywatnych repozytoriów,
- GitHub Team rulesets dla prywatnych repozytoriów,
- długie testy emulatorowe zużywające minuty GitHub Actions,
- dodatkowe usługi Firebase albo Google Maps.

## Zalecany merge method

Preferowany sposób merge:

```text
Squash and merge
```

Dzięki temu historia `main` zostaje czysta i czytelna.
