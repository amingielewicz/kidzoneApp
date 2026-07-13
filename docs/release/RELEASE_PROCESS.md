# Release Process

Ostatnia aktualizacja: 2026-07-13

## Cel

Proces przygotowania i publikacji wersji kidZone. Celem jest powtarzalny release, ograniczenie regresji oraz jednoznaczna decyzja GO / NO-GO.

## Przepływ

```text
feature branch
  ↓
Pull Request
  ↓
Code Review
  ↓
CI green
  ↓
merge to main
  ↓
release branch / candidate
  ↓
internal testing
  ↓
closed testing
  ↓
production rollout
```

## 1. Release candidate

- [ ] brak otwartych P0,
- [ ] P1 zamknięte albo jawnie zaakceptowane,
- [ ] `VERSION_NAME` i `VERSION_CODE` ustawione,
- [ ] release notes przygotowane,
- [ ] signed AAB możliwy do wygenerowania,
- [ ] właściwy projekt Firebase i konfiguracja Google Play,
- [ ] GO / NO-GO rozpoczęte.

## 2. Build i CI

- [ ] `assembleDebug` przechodzi,
- [ ] `testDebugUnitTest` przechodzi,
- [ ] `detekt` i `lint` przechodzą,
- [ ] `bundleRelease` przechodzi,
- [ ] release AAB jest podpisany,
- [ ] Gitleaks nie wykrywa sekretów,
- [ ] release nie zawiera narzędzi debugowych ani debug providerów.

## 3. Internal Testing

Sprawdź:

- [ ] czystą instalację,
- [ ] aktualizację z poprzedniej wersji,
- [ ] logowanie i rejestrację,
- [ ] Start, Mapę, Listę i szczegóły miejsca,
- [ ] dodawanie miejsca, opinii i zdjęć,
- [ ] ranking i profil,
- [ ] brak internetu,
- [ ] działanie bez lokalizacji,
- [ ] działanie bez kamery i powiadomień,
- [ ] trwałe odmowy i powrót z ustawień aplikacji,
- [ ] account deletion na koncie testowym.

## 4. Closed Testing

- [ ] kilka fizycznych urządzeń,
- [ ] różni producenci i wersje Androida,
- [ ] test na słabszym urządzeniu,
- [ ] test użytkownika nietechnicznego,
- [ ] Crashlytics i Performance zweryfikowane,
- [ ] koszty Firebase i Maps sprawdzone,
- [ ] większy zbiór danych nie blokuje UI.

## 5. Bramy compliance

Wymagany PASS:

- `docs/legal/account-deletion-test-checklist.md`,
- `docs/qa/android-permissions-device-matrix.md`,
- `docs/legal/google-play-data-safety-draft.md`,
- `docs/qa/google-play-security-checklist.md`,
- publiczne URL-e dokumentów,
- finalny legal review.

## 6. Production rollout

```text
5% → 20% → 50% → 100%
```

Przed zwiększeniem rollout:

- [ ] crash rate i ANR są akceptowalne,
- [ ] brak nowych P0/P1,
- [ ] logowanie i account deletion działają,
- [ ] mapa, lista i zdjęcia działają,
- [ ] brak nowych problemów z uprawnieniami,
- [ ] brak alertów kosztowych.

## 7. Halt rollout

Zatrzymaj rollout, gdy:

- rośnie crash rate lub ANR,
- login lub podstawowe flow nie działa,
- account deletion nie działa,
- występuje utrata danych,
- pojawia się luka privacy/security,
- uprawnienia powodują martwe akcje lub pętle,
- koszty rosną nietypowo.

## 8. Hotfix

Hotfix nadal wymaga:

- minimalnego review,
- testu poprawianego flow,
- testu regresji obszaru ryzyka,
- signed AAB,
- zwiększenia `VERSION_CODE`,
- aktualizacji release notes,
- obserwacji po wydaniu.

## 9. Po release

- [ ] Crashlytics i Android vitals sprawdzone,
- [ ] Performance i Analytics sprawdzone,
- [ ] Firebase i Maps usage sprawdzone,
- [ ] opinie użytkowników przejrzane,
- [ ] problemy zapisane jako follow-up issues,
- [ ] milestone release zamknięty dopiero po stabilizacji.

## Wynik

```text
Release:
Commit / tag:
AAB:
Track:
Internal testing: PASS / FAIL / BLOCKED
Closed testing: PASS / FAIL / BLOCKED
Compliance gates: PASS / FAIL / BLOCKED
GO / NO-GO:
Dowody:
```
