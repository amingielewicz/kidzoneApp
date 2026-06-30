# Product Decisions

## Cel

Dokument zbiera decyzje produktowe KidZone i ich uzasadnienie. Ma ograniczać powtarzanie tych samych dyskusji po kilku miesiącach.

## Format decyzji

Każda decyzja powinna mieć:

```text
Data:
Decyzja:
Kontekst:
Alternatywy:
Uzasadnienie:
Konsekwencje:
Powiązane issue / PR:
```

## Decyzja: mapa i lista jako równorzędne wejścia

Data: 2026-06

Decyzja:
Mapa nie jest jedynym sposobem odkrywania miejsc. Lista pozostaje równorzędnym sposobem dostępu do danych.

Kontekst:
Mapa jest wygodna, ale bywa problematyczna bez lokalizacji, przy słabym internecie i dla dostępności.

Uzasadnienie:
Lista poprawia dostępność, stabilność UX i daje fallback przy problemach z mapą.

Konsekwencje:
Każde miejsce widoczne na mapie powinno być możliwe do znalezienia także przez listę.

## Decyzja: kategorie jako badge / ikony

Data: 2026-06

Decyzja:
Kategorie miejsc pokazujemy jako badge albo ikonę, nie jako ciężkie belki wizualne.

Kontekst:
Ciężkie belki dominują kartę i pogarszają czytelność.

Uzasadnienie:
Badge jest lżejszy, bardziej skalowalny i lepiej pasuje do kart miejsc.

Konsekwencje:
Design system powinien zawierać spójny komponent badge kategorii.

## Decyzja: release przez GO / NO-GO checklist

Data: 2026-06

Decyzja:
Publiczny release wymaga przejścia checklisty GO / NO-GO.

Kontekst:
KidZone używa Firebase, Google Maps, lokalizacji, profili i danych społecznościowych.

Uzasadnienie:
Checklist zmniejsza ryzyko regresji, problemów privacy i błędów release.

Konsekwencje:
Release bez checklisty jest blokowany.

## Decyzja: ranking oparty o gotowe pola

Data: 2026-06

Decyzja:
Ranking miejsc powinien korzystać z gotowych pól, takich jak `ratingAverage` i `reviewsCount`.

Kontekst:
Liczenie rankingu przez pobieranie wszystkich opinii jest kosztowne i słabo skaluje się przy większej bazie.

Uzasadnienie:
Gotowe pola zmniejszają liczbę odczytów i upraszczają ekran rankingu.

Konsekwencje:
Dodanie lub zmiana opinii musi aktualizować pola rankingowe.

## Decyzja: onboarding krótki i pomijalny

Data: 2026-06

Decyzja:
Onboarding ma wyjaśniać główne funkcje, ale nie może blokować wejścia do aplikacji.

Kontekst:
Użytkownicy często chcą szybko sprawdzić mapę lub listę miejsc.

Uzasadnienie:
Krótki onboarding pomaga nowym użytkownikom, ale nie przeszkadza wracającym.

Konsekwencje:
Onboarding powinien być możliwy do pominięcia.

## Checklist dodawania decyzji

- [ ] Decyzja ma kontekst.
- [ ] Decyzja ma uzasadnienie.
- [ ] Wskazano konsekwencje.
- [ ] Podlinkowano issue albo PR, jeśli istnieje.
- [ ] Decyzja jest zrozumiała dla nowej osoby w projekcie.
