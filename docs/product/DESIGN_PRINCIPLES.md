# Design Principles

Ostatnia aktualizacja: 2026-07-13

## Cel

Zasady projektowe kidZone wspierają decyzje produktowe, UI i UX. Priorytetem jest szybkie znalezienie miejsca, zaufanie do danych i dostępność.

## 1. Rodzic szybko znajduje miejsce

Najważniejsze flow to znalezienie miejsca przyjaznego dzieciom.

- Start, Mapa i Lista są łatwo dostępne,
- filtry i sortowanie są zrozumiałe,
- najważniejsze informacje są widoczne bez wchodzenia w szczegóły,
- mapa ma pełny fallback w postaci listy,
- lokalizacja pomaga, ale nie jest warunkiem działania aplikacji.

## 2. Jeden ekran, jeden główny cel

- główna akcja jest widoczna,
- akcje drugorzędne nie konkurują z CTA,
- nie używamy ciężkich belek i nadmiaru kolorów,
- kategorie korzystają ze wspólnego badge lub ikony,
- komunikaty i formularze nie przeciążają użytkownika.

## 3. Każdy stan prowadzi dalej

Loading, empty, error, offline i permission state powinny mówić użytkownikowi, co się dzieje i co może zrobić.

- pusty stan ma sensowne CTA,
- błąd nie usuwa danych formularza,
- retry nie tworzy duplikatów,
- brak internetu nie udaje sukcesu zapisu,
- odmowa uprawnienia nie tworzy martwej akcji.

## 4. Dostępność jest częścią funkcji

- cele dotykowe mają odpowiedni rozmiar,
- elementy interaktywne mają etykiety,
- duża czcionka nie zasłania akcji,
- kolor nie jest jedynym nośnikiem informacji,
- mapa ma alternatywę listową,
- kolejność fokusu jest logiczna.

## 5. Uprawnienia są opcjonalne i zrozumiałe

- prosimy o zgodę w kontekście konkretnej funkcji,
- wyjaśniamy korzyść przed dialogiem systemowym,
- trwała odmowa prowadzi do ustawień aplikacji,
- po powrocie stan jest ponownie sprawdzany,
- Photo Picker działa bez szerokiego dostępu do galerii,
- odmowa powiadomień nie blokuje produktu.

## 6. Zaufanie ważniejsze niż tempo

- nie logujemy PII, tokenów ani dokładnej lokalizacji,
- jasno opisujemy sposób użycia danych,
- zgłoszenia i moderacja są dostępne,
- account deletion jest widoczne i działa,
- błędy częściowe nie są ukrywane,
- dane społecznościowe mają ownership i historię moderacji.

## 7. Dane i koszty są częścią UX

- listy i mapy mają limity,
- nie pobieramy całych kolekcji,
- cache poprawia użyteczność offline,
- kosztowne operacje mają rate limiting,
- wolne lub niestabilne funkcje nie blokują całej aplikacji.

## 8. Release bez dowodów nie istnieje

Przed wydaniem wymagane są:

- green CI,
- signed AAB,
- manual smoke,
- PASS dla runtime permissions,
- PASS dla account deletion,
- zgodna deklaracja Data Safety,
- zweryfikowane Rules i App Check,
- monitoring rollout.

## Checklista decyzji produktowej

- [ ] Czy funkcja pomaga znaleźć, ocenić lub bezpiecznie dodać miejsce?
- [ ] Czy główna wartość jest zrozumiała bez instrukcji?
- [ ] Czy działa bez zgód opcjonalnych?
- [ ] Czy ma loading, empty, error i offline state?
- [ ] Czy jest dostępna z TalkBack i dużą czcionką?
- [ ] Czy nie zwiększa niepotrzebnie kosztów lub liczby requestów?
- [ ] Czy ma strategię privacy, security i account deletion?
- [ ] Czy da się ją automatycznie lub manualnie zweryfikować?
