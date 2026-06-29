# Release Process

Dokument opisuje proces przygotowania i publikacji wersji kidZone.

## Cele procesu

- minimalizować ryzyko regresji,
- zapewnić powtarzalny release,
- wymusić kontrolę security i privacy,
- mieć jasne kryterium GO / NO-GO,
- utrzymać historię decyzji release.

## Standardowy przepływ

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
release candidate
  ↓
internal testing
  ↓
closed testing
  ↓
production rollout
```

## 1. Przygotowanie release candidate

Przed RC:

- [ ] wszystkie P0 zamknięte,
- [ ] wszystkie P1 zamknięte albo świadomie przeniesione,
- [ ] release notes przygotowane,
- [ ] GO / NO-GO checklist rozpoczęta,
- [ ] signed build możliwy do wygenerowania,
- [ ] Firebase project i Google Play config zweryfikowane.

## 2. Build

Wymagane:

- [ ] `assembleDebug` przechodzi,
- [ ] testy jednostkowe przechodzą,
- [ ] release AAB/APK podpisany,
- [ ] brak sekretów w repo,
- [ ] release nie zawiera debug-only tools.

## 3. Internal Testing

Internal testing służy do sprawdzenia technicznej gotowości.

Sprawdzić:

- [ ] czysta instalacja,
- [ ] aktualizacja z poprzedniej wersji,
- [ ] logowanie/rejestracja,
- [ ] mapa,
- [ ] lista,
- [ ] dodawanie miejsca,
- [ ] opinie,
- [ ] ranking,
- [ ] profil,
- [ ] brak internetu,
- [ ] brak lokalizacji.

## 4. Closed Testing

Closed testing służy do zebrania feedbacku UX i stabilności.

Wymagane:

- [ ] minimum kilka realnych urządzeń,
- [ ] test z użytkownikiem nietechnicznym,
- [ ] sprawdzenie Crashlytics,
- [ ] sprawdzenie wydajności na słabszym Androidzie,
- [ ] sprawdzenie danych 1000 miejsc, jeśli dotyczy.

## 5. Production rollout

Rekomendowany rollout:

```text
5% → obserwacja → 20% → obserwacja → 50% → obserwacja → 100%
```

Przed zwiększeniem rollout:

- [ ] crash rate akceptowalny,
- [ ] brak nowych P0/P1,
- [ ] brak problemów z logowaniem,
- [ ] brak problemów z mapą,
- [ ] brak alertów kosztowych Firebase / Maps.

## Rollback

Rollback rozważyć, gdy:

- crash rate rośnie gwałtownie,
- login lub podstawowe flow nie działa,
- pojawia się krytyczna luka bezpieczeństwa,
- koszt Firebase / Maps rośnie nienaturalnie,
- występuje utrata danych.

## Hotfix

Hotfix idzie poza standardowym cyklem, ale nadal wymaga:

- [ ] minimalnego code review,
- [ ] testu fixowanego flow,
- [ ] signed release build,
- [ ] aktualizacji release notes,
- [ ] obserwacji Crashlytics po wydaniu.

## Po release

Po zakończeniu rollout:

- [ ] sprawdzić Crashlytics,
- [ ] sprawdzić Performance,
- [ ] sprawdzić Analytics,
- [ ] zanotować problemy,
- [ ] utworzyć follow-up issue,
- [ ] zamknąć release milestone.
