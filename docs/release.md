# Release process

Powiązane issue: #166  
Parent: #159

## Cel

Ten dokument opisuje bezpieczny proces przygotowania release aplikacji kidZone przed publikacją w Google Play.

Release nie powinien być wykonywany bez przejścia checklisty technicznej, bezpieczeństwa, prawnej i testowej.

## Zakres dokumentu

Dokument obejmuje:

- przygotowanie brancha release,
- lokalną walidację builda,
- podpisywanie aplikacji,
- obsługę keystore,
- GitHub Secrets,
- Firebase i Google Play Console,
- checklistę przed publikacją,
- procedurę awaryjnego rollbacku.

## Typy release

| Typ | Przykład | Opis |
| --- | --- | --- |
| Alpha | `v0.1.0-alpha` | Pierwszy release techniczny, testy wewnętrzne. |
| Beta | `v0.2.0-beta` | Szersze testy, stabilizacja danych i wydajności. |
| Release candidate | `v1.0.0-rc1` | Kandydat do publikacji produkcyjnej. |
| Production | `v1.0.0` | Wersja publiczna w Google Play. |

## Branching

Rekomendowany schemat:

```text
main
└── release/v0.1.0-alpha
```

Zasady:

- `main` powinien być stabilny,
- release przygotowujemy na osobnym branchu,
- poprawki do release trafiają przez PR,
- nie robimy przypadkowych commitów bezpośrednio na `main`,
- przed publikacją sprawdzamy, czy branch release zawiera ostatnie zmiany z `main`.

## Minimalna checklista przed release

Przed wygenerowaniem paczki release trzeba potwierdzić:

- [ ] build debug przechodzi lokalnie,
- [ ] build release przechodzi lokalnie albo w CI,
- [ ] testy smoke zostały wykonane,
- [ ] nie ma krytycznych błędów w logach,
- [ ] Firebase Rules są aktualne,
- [ ] Storage Rules są aktualne,
- [ ] App Check jest poprawnie skonfigurowany,
- [ ] Crashlytics działa w release,
- [ ] Data Safety jest zgodne z aktualnym kodem,
- [ ] polityka prywatności i regulamin są aktualne,
- [ ] flow usuwania konta zostało sprawdzone,
- [ ] wersja aplikacji została podbita,
- [ ] changelog został przygotowany.

## Lokalna walidacja

Podstawowa komenda:

```powershell
.\gradlew assembleDebug
```

Dla release:

```powershell
.\gradlew assembleRelease
```

Jeżeli projekt używa bundle do Google Play:

```powershell
.\gradlew bundleRelease
```

Przed PR release warto też wykonać:

```powershell
.\gradlew test
```

Jeżeli aktywne są narzędzia jakościowe:

```powershell
.\gradlew lint
.\gradlew detekt
```

## Wersjonowanie aplikacji

Przed publikacją trzeba sprawdzić w `app/build.gradle.kts`:

```kotlin
versionCode = ...
versionName = "..."
```

Zasady:

- `versionCode` musi rosnąć przy każdej paczce wysłanej do Google Play,
- `versionName` powinien odpowiadać tagowi release,
- nie wolno publikować dwóch różnych buildów z tym samym `versionCode`.

Przykład:

```kotlin
versionCode = 1
versionName = "0.1.0-alpha"
```

## Podpisywanie aplikacji

Release Android wymaga podpisania aplikacji.

Rekomendacja:

- keystore produkcyjny nie powinien być commitowany do repo,
- hasła do keystore nie powinny być wpisane w kodzie,
- dane podpisu powinny być trzymane w GitHub Secrets albo lokalnym `keystore.properties`,
- `keystore.properties` musi być w `.gitignore`,
- dostęp do keystore powinien mieć tylko właściciel projektu lub wyznaczone osoby.

Przykładowe lokalne pliki:

```text
keystore/release-key.jks
keystore.properties
```

Przykładowe pola w `keystore.properties`:

```properties
storeFile=keystore/release-key.jks
storePassword=***
keyAlias=***
keyPassword=***
```

Tego pliku nie wolno commitować.

## GitHub Secrets

Jeżeli release jest budowany w GitHub Actions, wymagane sekrety powinny być zapisane w ustawieniach repozytorium.

Rekomendowane sekrety:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
MAPS_API_KEY
GOOGLE_SERVICES_JSON_BASE64
```

Zasady bezpieczeństwa:

- sekretów nie wypisujemy w logach CI,
- sekretów nie kopiujemy do README,
- sekretów nie commitujemy,
- dostęp do ustawień repo ograniczamy do minimum,
- po podejrzeniu wycieku sekret należy obrócić.

## Firebase przed release

Przed publikacją sprawdzamy:

- [ ] projekt Firebase wskazuje właściwe środowisko,
- [ ] `google-services.json` pochodzi z właściwego projektu,
- [ ] Firestore Rules są wdrożone,
- [ ] Storage Rules są wdrożone,
- [ ] App Check jest w oczekiwanym trybie,
- [ ] Crashlytics działa dla release,
- [ ] Analytics jest zgodne z Data Safety,
- [ ] Performance Monitoring jest zgodne z decyzją kosztową,
- [ ] Cloud Messaging jest zgodne z decyzją dotyczącą powiadomień.

## Google Play Console przed publikacją

Przed wysłaniem paczki do Google Play trzeba sprawdzić:

- [ ] nazwa aplikacji,
- [ ] opis krótki i pełny,
- [ ] grafiki i ikony,
- [ ] polityka prywatności,
- [ ] regulamin, jeśli linkowany,
- [ ] Data Safety,
- [ ] App access,
- [ ] Ads declaration,
- [ ] Content rating,
- [ ] Target audience,
- [ ] test internal / closed / open,
- [ ] kraj dystrybucji.

## Smoke test przed publikacją

Minimalny test manualny:

- [ ] start aplikacji,
- [ ] rejestracja,
- [ ] logowanie,
- [ ] wylogowanie,
- [ ] reset hasła,
- [ ] mapa,
- [ ] lista miejsc,
- [ ] ranking,
- [ ] profil,
- [ ] dodanie miejsca,
- [ ] dodanie zdjęcia,
- [ ] dodanie opinii,
- [ ] zgłoszenie treści,
- [ ] odmowa lokalizacji,
- [ ] odmowa powiadomień,
- [ ] brak internetu,
- [ ] usunięcie konta albo ścieżka zgłoszenia usunięcia danych.

## Changelog

Przed release przygotowujemy notatkę:

```markdown
## v0.1.0-alpha

### Added
- ...

### Changed
- ...

### Fixed
- ...

### Security
- ...
```

## Tag release

Po merge release do `main` można utworzyć tag:

```powershell
git checkout main
git pull origin main
git tag v0.1.0-alpha
git push origin v0.1.0-alpha
```

Tag powinien wskazywać dokładny commit, z którego powstał build wysłany do Google Play.

## Rollback i procedura awaryjna

Jeżeli release ma krytyczny błąd:

1. Zatrzymać rollout w Google Play Console.
2. Sprawdzić Crashlytics i opinie testerów.
3. Utworzyć `hotfix/...` z `main` albo z tagu release.
4. Naprawić problem.
5. Wykonać smoke test.
6. Zwiększyć `versionCode`.
7. Wysłać nowy build.
8. Opisać problem w changelogu.

## Czego nie robić

- Nie commitować keystore.
- Nie commitować haseł.
- Nie publikować builda bez aktualnego Data Safety.
- Nie publikować builda bez sprawdzenia usuwania konta.
- Nie zostawiać debug providerów w release.
- Nie wysyłać builda z testowymi kluczami API.
- Nie zakładać, że Firebase Rules są wdrożone tylko dlatego, że plik istnieje w repo.

## Kryteria gotowości release

Release jest gotowy, gdy:

- build jest zielony,
- paczka jest podpisana poprawnym keystore,
- checklisty prawne i bezpieczeństwa są wykonane,
- Data Safety pasuje do faktycznego działania aplikacji,
- Firebase Rules są wdrożone,
- nie ma krytycznych błędów w Crashlytics,
- właściciel projektu świadomie akceptuje ryzyka przed publikacją.
