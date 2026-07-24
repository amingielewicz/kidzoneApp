# Observability

Ostatnia aktualizacja: 2026-07-13

## Cel

Standard obserwowalności kidZone: logi, Crashlytics, metryki, wydajność i diagnostyka bez ujawniania danych użytkowników.

## Zasady

- diagnostyka ma pomagać znaleźć przyczynę problemu,
- komunikat techniczny jest oddzielony od komunikatu użytkownika,
- dane osobowe, tokeny i dokładna lokalizacja nie trafiają do telemetrii,
- zdarzenia mają ograniczoną, stabilną strukturę,
- release nie może generować nadmiernej liczby logów.

## Poziomy logów

```text
DEBUG  informacje lokalne dla developmentu
INFO   istotny etap działania funkcji
WARN   sytuacja nietypowa, ale obsłużona
ERROR  błąd wymagający diagnozy
```

W release `DEBUG` powinien być wyłączony lub ograniczony.

## Bezpieczny kontekst

Można zbierać:

- nazwę funkcji i ekranu,
- nazwę operacji,
- wersję i build aplikacji,
- środowisko,
- typ błędu,
- status operacji,
- czas trwania,
- anonimowe liczniki retry.

Nie zbieramy:

- haseł i tokenów,
- e-maili, nazwisk i treści profilu,
- treści opinii i formularzy,
- pełnych payloadów,
- dokładnych współrzędnych,
- URI zdjęć,
- surowych odpowiedzi backendu zawierających dane użytkownika.

## Crashlytics

Do Crashlytics mogą trafiać:

- crashe,
- kontrolowane non-fatal errors,
- bezpieczny kontekst funkcji,
- wersja i stan operacji.

Zasady:

- nie przekazujemy surowych wyjątków zawierających dane użytkownika,
- custom keys nie zawierają PII,
- breadcrumbs nie zawierają treści formularzy ani identyfikatorów plików,
- user ID jest wyłączony albo pseudonimizowany zgodnie z decyzją prywatności,
- powtarzalne błędy są grupowane i ograniczane.

## Performance Monitoring

Trace może obejmować:

- cold start,
- ładowanie miejsc,
- mapę,
- wyszukiwanie,
- ranking,
- zapis miejsca,
- upload zdjęć.

Nazwy trace i atrybuty nie mogą zawierać danych użytkownika ani identyfikatorów zasobów.

## Analytics

Eventy produktowe:

- używają neutralnych nazw,
- nie zawierają e-maili, pełnych adresów ani dokładnej lokalizacji,
- nie zawierają tekstu opinii i wyszukiwanych fraz, jeśli mogą identyfikować użytkownika,
- odpowiadają deklaracji Data Safety.

## Alerty

Monitorujemy:

- crash rate i ANR,
- błędy logowania,
- błędy mapy i listy,
- nieudane uploady zdjęć,
- błędy account deletion,
- problemy z uprawnieniami,
- wzrost kosztów Firebase i Maps,
- błędy Cloud Functions.

## Checklista

- [ ] zdarzenie nie zawiera danych wrażliwych,
- [ ] krytyczny błąd jest obserwowalny,
- [ ] kontekst jest wystarczający do diagnozy,
- [ ] diagnostyka nie spamuje,
- [ ] komunikat użytkownika nie zawiera szczegółów technicznych,
- [ ] Crashlytics i Analytics odpowiadają Data Safety,
- [ ] trace i event parameters są bezpieczne.
