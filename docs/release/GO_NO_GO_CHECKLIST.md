# GO / NO-GO Checklist

Ten dokument jest bramką jakościową przed publikacją kidZone w Google Play.

## Statusy

Każdy punkt powinien mieć jeden status:

- `OK` — sprawdzone i działa,
- `Do poprawy` — wymaga naprawy przed release,
- `Nie dotyczy` — świadomie pominięte z uzasadnieniem.

## Poziomy gotowości

### MVP / Release Candidate (80–85%)

Minimalny poziom pozwalający rozpocząć Internal Testing lub Closed Testing.

Wymagane:

- [ ] Wszystkie główne funkcje działają.
- [ ] Brak krytycznych crashy.
- [ ] Logowanie i rejestracja działają.
- [ ] Mapa, lista, ranking, profil i dodawanie miejsca działają.
- [ ] UI jest spójny.
- [ ] UX zawiera loading, empty i error states.
- [ ] Onboarding / first-use guidance działa.
- [ ] Podstawowa accessibility jest spełniona.
- [ ] Cache, paginacja i limity zapytań są wdrożone.
- [ ] Crashlytics, Analytics i Performance działają.
- [ ] Smoke test i manual regression są wykonane.

### Optimum / Produkt Premium (90–95%)

Poziom rekomendowany dla pierwszego publicznego release.

Dodatkowo wymagane:

- [ ] Kompletny design system.
- [ ] Skeleton loading tam, gdzie ładowanie jest zauważalne.
- [ ] Dopracowane mikrocopy.
- [ ] App Check enforcement zweryfikowany.
- [ ] Release nie loguje danych wrażliwych.
- [ ] Mapa używa limitów, bounds/promienia i marker clusteringu.
- [ ] Remote Config kontroluje kluczowe limity.
- [ ] Testy wydajnościowe i obciążeniowe są wykonane.
- [ ] Smoke test na danych 1000 miejsc przechodzi.

### Enterprise / Maximum (98–100%)

Poziom długoterminowy, nieblokujący pierwszego publicznego release.

Przykładowe wymagania:

- [ ] AI/ML rekomendacje.
- [ ] Automatyczna moderacja opinii i zdjęć.
- [ ] OpenTelemetry / pełna observability.
- [ ] OWASP MASVS Level 2.
- [ ] Baseline Profiles i Macrobenchmark.
- [ ] Load, soak i chaos testing.
- [ ] Predictive UI.
- [ ] Zaawansowana telemetria produktu.

## Minimum do publicznego release

- [ ] Signed Release Build przechodzi.
- [ ] Aplikacja odpala się po czystej instalacji.
- [ ] Login działa.
- [ ] Rejestracja działa.
- [ ] Mapa działa z lokalizacją.
- [ ] Mapa działa bez lokalizacji.
- [ ] Lista miejsc działa.
- [ ] Dodawanie miejsca działa.
- [ ] Opinie i oceny działają.
- [ ] Ranking działa.
- [ ] Profil działa.
- [ ] Brak internetu jest obsłużony.
- [ ] Brak lokalizacji jest obsłużony.
- [ ] Crashlytics zbiera testowy crash.
- [ ] App Check jest zweryfikowany.
- [ ] Firestore Rules są sprawdzone.
- [ ] Storage Rules są sprawdzone.
- [ ] Release nie loguje danych wrażliwych.
- [ ] Smoke test na danych 1000 miejsc przechodzi.

## Release blockers

Release jest zablokowany, jeżeli występuje dowolny punkt:

- [ ] Crash przy starcie aplikacji.
- [ ] Brak możliwości logowania.
- [ ] Brak możliwości użycia mapy lub listy miejsc.
- [ ] Brak możliwości dodania miejsca.
- [ ] Krytyczny błąd Firestore / Storage Rules.
- [ ] Release build loguje dane wrażliwe.
- [ ] Crashlytics nie działa w release.
- [ ] App Check nie jest zweryfikowany.
- [ ] Signed release build nie przechodzi.

## Go / No-Go decision

### GO

Release może iść dalej, gdy:

- [ ] Wszystkie punkty MVP mają status `OK`.
- [ ] Wszystkie punkty minimum public release mają status `OK`.
- [ ] Nie ma aktywnych blockerów P0/P1.
- [ ] Smoke test release builda przeszedł.

### NO-GO

Release zatrzymujemy, gdy:

- [ ] Występuje dowolny release blocker.
- [ ] Security lub Firebase Rules nie zostały zweryfikowane.
- [ ] Crashlytics albo App Check nie zostały potwierdzone.
- [ ] Podstawowy flow użytkownika nie działa.

## Podpis release

- Data:
- Wersja:
- Build:
- Osoba sprawdzająca:
- Decyzja: `GO` / `NO-GO`
- Uwagi:
