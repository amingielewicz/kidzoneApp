# Release process

Powiązane issue: #159, #166, #210, #269, #272, #274, #275, #303

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje bezpieczny proces przygotowania wydania kidZone do Google Play. Release nie może ominąć bramek technicznych, bezpieczeństwa, prywatności i manualnego QA.

## Dokumenty źródłowe

- [Szczegółowy proces release](release/RELEASE_PROCESS.md)
- [Publikacja w Google Play](release/PLAY_STORE_RELEASE.md)
- [Go/No-Go](release/GO_NO_GO_CHECKLIST.md)
- [Google Play security checklist](qa/google-play-security-checklist.md)
- [Manual release test plan](qa/manual-release-test-plan.md)
- [Manual release gates](legal/v1-release-manual-gates.md)

## Typy wydań

| Typ | Przykład | Cel |
| --- | --- | --- |
| Alpha | `v0.1.0-alpha` | test techniczny |
| Beta | `v0.2.0-beta` | stabilizacja i testy zamknięte |
| Release candidate | `v1.0.0-rc1` | finalna walidacja |
| Production | `v1.0.0` | publiczne wydanie |

## Branching

```text
main
└── release/v1.0.0
```

Zasady:

- `main` pozostaje stabilny,
- release przygotowujemy na osobnym branchu,
- poprawki trafiają przez PR,
- branch release musi zawierać aktualne zmiany z `main`,
- tag wskazuje dokładny commit wysłany do Google Play.

## Minimalna bramka przed buildem

- [ ] `assembleDebug` przechodzi,
- [ ] `bundleRelease` przechodzi,
- [ ] testy jednostkowe i statyczne przechodzą,
- [ ] Gitleaks nie wykrywa sekretów,
- [ ] Firestore Rules i Storage Rules są aktualne,
- [ ] App Check release używa Play Integrity,
- [ ] Crashlytics i Performance są zgodne z Data Safety,
- [ ] `versionName` i `versionCode` są poprawne,
- [ ] changelog i release notes są gotowe.

## Wersjonowanie

Wersja jest zarządzana w `version.properties`:

```properties
VERSION_NAME=1.0.0
VERSION_CODE=1
```

Zasady:

- `VERSION_CODE` musi rosnąć przy każdym uploadzie,
- `VERSION_NAME` odpowiada tagowi i release notes,
- nie publikujemy dwóch różnych buildów z tym samym `VERSION_CODE`.

## Build lokalny

```powershell
.\gradlew assembleDebug
.\gradlew testDebugUnitTest
.\gradlew detekt
.\gradlew lint
.\gradlew bundleRelease
```

Wynikowy bundle:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Podpisywanie

- keystore nie trafia do repo,
- hasła są przechowywane w menedżerze haseł lub GitHub Secrets,
- kopia keystore jest przechowywana poza repo,
- dostęp ma tylko właściciel lub upoważnione osoby,
- po podejrzeniu wycieku uruchamiamy procedurę rotacji.

Typowe sekrety CI:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
MAPS_API_KEY
GOOGLE_SERVICES_JSON
```

## Firebase przed release

- [ ] produkcyjny projekt Firebase,
- [ ] produkcyjny `google-services.json`,
- [ ] wdrożone Firestore Rules,
- [ ] wdrożone Storage Rules,
- [ ] App Check w oczekiwanym trybie,
- [ ] Crashlytics działa bez danych osobowych,
- [ ] Analytics, Performance, FCM i Remote Config odpowiadają Data Safety,
- [ ] budżety i alerty kosztowe są skonfigurowane.

## Uprawnienia Android

Finalny manifest powinien zawierać tylko wymagany zakres:

- `ACCESS_FINE_LOCATION`,
- `ACCESS_COARSE_LOCATION`,
- `CAMERA`,
- `POST_NOTIFICATIONS`.

Nie powinien zawierać:

- `ACCESS_BACKGROUND_LOCATION`,
- `READ_MEDIA_IMAGES`,
- `READ_EXTERNAL_STORAGE`,
- `QUERY_ALL_PACKAGES`.

Photo Picker musi działać bez szerokiego dostępu do galerii. Po trwałej odmowie lokalizacji lub kamery aplikacja ma prowadzić do ustawień aplikacji, a przyciski nie mogą stawać się martwe.

## Google Play Console

Przed uploadem sprawdź:

- [ ] nazwę i opisy,
- [ ] grafiki i screenshoty,
- [ ] Privacy Policy URL,
- [ ] Account deletion URL,
- [ ] Data Safety,
- [ ] App access,
- [ ] Ads declaration,
- [ ] Content rating,
- [ ] Target audience: rodzice i opiekunowie,
- [ ] kraje dystrybucji,
- [ ] track testowy.

## Manualne bramki

Wymagany wynik PASS:

- account deletion,
- runtime permissions,
- publiczne dokumenty,
- Data Safety,
- manualny smoke test,
- Google Play security checklist.

## Rollout

Rekomendowany staged rollout:

```text
5% → 20% → 50% → 100%
```

Przed zwiększeniem rollout sprawdź Android vitals, Crashlytics, problemy z logowaniem, mapą, listą, zdjęciami, uprawnieniami i kosztami.

## Rollback i hotfix

1. Zatrzymaj rollout.
2. Sprawdź Crashlytics, vitals i zgłoszenia.
3. Utwórz `hotfix/*` z właściwego tagu lub `main`.
4. Napraw problem i dodaj test regresji.
5. Zwiększ `VERSION_CODE`.
6. Wykonaj pełne bramki wymagane dla zakresu poprawki.
7. Wyślij nowy build.
8. Uzupełnij changelog i opis incydentu.

## Kryteria gotowości

Release jest gotowy, gdy:

- build i CI są zielone,
- AAB jest podpisany właściwym kluczem,
- Data Safety odpowiada finalnemu buildowi,
- account deletion i runtime permissions mają PASS,
- Rules i App Check są wdrożone,
- nie ma P0/P1,
- właściciel świadomie zatwierdził GO.
