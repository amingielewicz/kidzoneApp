# Design Principles

## Cel

Dokument opisuje zasady projektowe KidZone. Ma pomagać w podejmowaniu decyzji UI, UX i produktowych.

## 1. Rodzic ma szybko znaleźć miejsce

Najważniejszy scenariusz aplikacji to szybkie znalezienie miejsca przyjaznego dzieciom.

W praktyce:

- mapa i lista muszą być łatwo dostępne,
- filtry muszą być zrozumiałe,
- najważniejsze informacje muszą być widoczne bez wchodzenia w szczegóły,
- aplikacja nie może zmuszać użytkownika do myślenia technicznego.

## 2. Mniej hałasu, więcej informacji

KidZone powinien być lekki wizualnie. UI ma pomagać, nie dominować.

W praktyce:

- unikamy ciężkich belek i nadmiaru kolorów,
- kategorie pokazujemy jako badge lub ikonę,
- informacje drugorzędne nie konkurują z nazwą miejsca,
- każdy ekran ma jeden główny cel.

## 3. Każdy pusty stan prowadzi dalej

Pusty ekran nie może wyglądać jak błąd.

W praktyce każdy empty state powinien mieć:

- prosty nagłówek,
- krótkie wyjaśnienie,
- sensowne CTA.

## 4. Dostępność jest częścią UX

Dostępność nie jest dodatkiem na koniec. Jest warunkiem jakości.

W praktyce:

- dbamy o kontrast,
- dbamy o touch targety,
- dodajemy opisy ikon,
- wspieramy większe fonty,
- mapa ma alternatywę listową.

## 5. Offline i błędy są normalnym stanem

Aplikacja mobilna działa w realnym świecie: słaby internet, brak GPS, zmienne warunki.

W praktyce:

- brak internetu ma osobny komunikat,
- brak lokalizacji nie blokuje całej aplikacji,
- cache powinien pomagać w podstawowym korzystaniu,
- błędy mówią, co można zrobić dalej.

## 6. Zaufanie ważniejsze niż tempo

KidZone operuje na danych społecznościowych i lokalizacji. Zaufanie jest kluczowe.

W praktyce:

- nie logujemy danych wrażliwych,
- jasno komunikujemy uprawnienia,
- dbamy o zgłaszanie naruszeń,
- walidujemy zdjęcia i opinie,
- nie ukrywamy błędów.

## 7. Release bez checklisty nie istnieje

Każde wydanie przechodzi przez GO / NO-GO.

W praktyce:

- nie wypuszczamy bez smoke testu,
- nie wypuszczamy bez sprawdzenia Crashlytics,
- nie wypuszczamy bez sprawdzenia App Check i Rules,
- nie wypuszczamy, jeśli podstawowe flow nie działa.

## Checklist decyzji produktowej

Przed dodaniem nowej funkcji odpowiedz:

- [ ] Czy pomaga rodzicowi znaleźć lub ocenić miejsce?
- [ ] Czy upraszcza korzystanie z aplikacji?
- [ ] Czy nie zwiększa niepotrzebnie złożoności?
- [ ] Czy da się ją sensownie przetestować?
- [ ] Czy nie pogarsza dostępności?
- [ ] Czy nie zwiększa ryzyka privacy/security?
