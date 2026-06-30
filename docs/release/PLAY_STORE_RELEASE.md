# Play Store Release

## Cel

Dokument opisuje minimalny proces publikacji KidZone w Google Play.

## Przed publikacją

- [ ] Version code zwiększony.
- [ ] Version name ustawiony.
- [ ] Signed release build wygenerowany.
- [ ] GO / NO-GO checklist wykonana.
- [ ] Release notes przygotowane.
- [ ] Crashlytics zweryfikowany.
- [ ] App Check zweryfikowany.
- [ ] Firestore Rules sprawdzone.
- [ ] Storage Rules sprawdzone.
- [ ] Brak danych wrażliwych w logach release.

## Internal testing

Internal testing jest pierwszą bramką techniczną.

Sprawdzić:

- [ ] czysta instalacja,
- [ ] aktualizacja z poprzedniej wersji,
- [ ] logowanie,
- [ ] rejestracja,
- [ ] mapa,
- [ ] lista,
- [ ] ranking,
- [ ] profil,
- [ ] dodawanie miejsca,
- [ ] opinie,
- [ ] brak internetu,
- [ ] brak lokalizacji.

## Closed testing

Closed testing służy do walidacji stabilności i UX.

Wymagane:

- [ ] test na kilku urządzeniach,
- [ ] test na słabszym urządzeniu,
- [ ] test użytkownika nietechnicznego,
- [ ] sprawdzenie Crashlytics,
- [ ] sprawdzenie Performance,
- [ ] sprawdzenie kosztów Firebase / Maps.

## Production rollout

Rekomendowany rollout:

```text
5% -> 20% -> 50% -> 100%
```

Przed zwiększeniem rollout:

- [ ] crash rate akceptowalny,
- [ ] brak blockerów P0/P1,
- [ ] brak problemów z logowaniem,
- [ ] brak problemów z mapą i listą,
- [ ] brak nietypowego wzrostu kosztów.

## Store listing

Przed publicznym wydaniem sprawdzić:

- [ ] nazwa aplikacji,
- [ ] krótki opis,
- [ ] pełny opis,
- [ ] ikona,
- [ ] screenshoty,
- [ ] grafika promocyjna,
- [ ] kategoria,
- [ ] polityka prywatności,
- [ ] deklaracje danych,
- [ ] content rating.

## Privacy

- [ ] Polityka prywatności jest aktualna.
- [ ] Deklaracje danych w Google Play są zgodne z aplikacją.
- [ ] Uprawnienia są uzasadnione.
- [ ] Lokalizacja jest opisana w komunikatach aplikacji.
- [ ] Zdjęcia i dane użytkownika mają jasny cel.

## Po publikacji

Po publikacji sprawdzić:

- [ ] Crashlytics po 1 godzinie,
- [ ] Crashlytics po 24 godzinach,
- [ ] Google Play Console vitals,
- [ ] Firebase usage,
- [ ] zgłoszenia użytkowników,
- [ ] opinie w sklepie.

## Rollback / halt rollout

Zatrzymać rollout, gdy:

- crash rate rośnie,
- login nie działa,
- mapa albo lista nie działa,
- występuje problem privacy/security,
- koszty Firebase / Maps rosną nietypowo,
- pojawiają się powtarzalne zgłoszenia P0/P1.
