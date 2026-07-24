# Accessibility and TalkBack checklist

Ostatnia aktualizacja: 2026-07-14

Używaj tej checklisty dla release candidate oraz PR-ów zmieniających UI, nawigację, formularze, mapę, uprawnienia lub komunikaty dynamiczne.

## Setup

- [ ] TalkBack włączony,
- [ ] jasny i ciemny motyw,
- [ ] font scale: domyślny, około 1.3x, 1.5x–1.6x i maksimum urządzenia,
- [ ] display size: domyślny i duży,
- [ ] mały ekran i landscape,
- [ ] dynamic colors na Androidzie 12+,
- [ ] reduced motion,
- [ ] zwykłe konto użytkownika,
- [ ] fizyczne urządzenie dla co najmniej jednego pełnego smoke.

## Globalne zasady

- [ ] każda akcja ma czytelną nazwę, rolę i stan,
- [ ] elementy dekoracyjne nie są odczytywane,
- [ ] kolejność fokusu jest logiczna,
- [ ] fokus nie przeskakuje po recomposition,
- [ ] touch target ma minimum 48 dp,
- [ ] kolor nie jest jedynym nośnikiem informacji,
- [ ] dynamiczne komunikaty są ogłaszane raz,
- [ ] duży font nie ukrywa CTA.

## Logowanie i rejestracja

- [ ] pola e-mail i hasła odczytują etykiety, wartości i błędy,
- [ ] przycisk hasła mówi „Pokaż hasło” lub „Ukryj hasło”,
- [ ] login, rejestracja, Google sign-in i reset hasła są osiągalne gestami,
- [ ] loading i disabled state są zrozumiałe,
- [ ] błąd auth nie ujawnia technicznego wyjątku,
- [ ] po sukcesie fokus trafia do logicznego miejsca na następnym ekranie.

## Start i Lista

- [ ] nagłówek oraz główne akcje są odczytywane w logicznej kolejności,
- [ ] karta miejsca nie dubluje bez potrzeby semantyki dzieci,
- [ ] karta odczytuje nazwę, kategorię, ocenę i adres lub dystans,
- [ ] wyszukiwarka i filtry mają jasne etykiety,
- [ ] wybrany filtr i sortowanie mają dostępny stan,
- [ ] empty, offline i error state są ogłaszane,
- [ ] sortowanie „Od najbliższych” obsługuje brak lokalizacji.

## Mapa

- [ ] loading i błędy nie kradną fokusu wielokrotnie,
- [ ] „Moja lokalizacja” ma czytelną nazwę,
- [ ] brak zgody i wyłączony GPS mają dostępny fallback,
- [ ] trwała odmowa prowadzi do ustawień,
- [ ] lista fallback jest osiągalna gestami,
- [ ] bottom sheet przejmuje fokus,
- [ ] po zamknięciu bottom sheeta fokus wraca logicznie,
- [ ] miejsce można otworzyć bez polegania wyłącznie na markerze.

## Dodawanie miejsca i opinii

- [ ] wymagane pola oraz błędy są ogłaszane,
- [ ] kategoria i udogodnienia odczytują stan wyboru,
- [ ] lokalizacja, kamera i Photo Picker wyjaśniają cel,
- [ ] zmiana statusu GPS jest ogłaszana,
- [ ] miniatura zdjęcia i przycisk usunięcia mają znaczące nazwy,
- [ ] klawiatura nie blokuje zapisu,
- [ ] double submit jest niemożliwy,
- [ ] częściowy błąd uploadu nie jest ogłaszany jako pełny sukces.

## Szczegóły miejsca

- [ ] tytuł, kategoria, adres i ocena są odczytywane w użytecznej kolejności,
- [ ] opinie i zdjęcia są osiągalne gestami,
- [ ] akcje zgłoszenia jasno wskazują cel,
- [ ] galeria nie tworzy pustych elementów fokusu,
- [ ] usunięty lub niedostępny zasób ma kontrolowany komunikat,
- [ ] animacje odznak respektują reduced motion.

## Ranking i Profil

- [ ] użytkownik rozumie pozycję i podstawę rankingu,
- [ ] odznaki mają znaczące nazwy,
- [ ] statystyki profilu nie są odczytywane jako niepowiązane liczby,
- [ ] ustawienia, logout i account deletion są osiągalne,
- [ ] akcje destrukcyjne zawierają nazwę działania i celu,
- [ ] po logout/delete account fokus nie wraca do prywatnego ekranu.

## Dialogi, bottom sheety i snackbary

- [ ] warstwa przejmuje fokus,
- [ ] fokus nie wychodzi poza otwartą warstwę,
- [ ] przycisk zamknięcia ma jednoznaczną etykietę,
- [ ] treść jest przewijalna przy dużym foncie,
- [ ] snackbar jest ogłaszany bez wielokrotnego powtarzania,
- [ ] po zamknięciu fokus wraca do właściwego elementu.

## Uprawnienia i ustawienia systemowe

- [ ] rationale jest odczytane przed dialogiem systemowym,
- [ ] zwykła odmowa ma dalszy krok,
- [ ] trwała odmowa otwiera ustawienia aplikacji,
- [ ] powrót z ustawień odświeża i ogłasza stan,
- [ ] brak powiadomień nie blokuje aplikacji,
- [ ] Photo Picker działa bez szerokiej zgody galerii.

## Deep linki i powiadomienia

- [ ] deep link otwiera właściwy ekran,
- [ ] brak zasobu ma kontrolowany komunikat,
- [ ] wylogowany użytkownik przechodzi przez poprawny auth flow,
- [ ] powiadomienie nie tworzy zduplikowanego ekranu,
- [ ] tytuł i treść powiadomienia nie ujawniają PII.

## Automatyczne bramki

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat testDebugUnitTest detekt lintDebug assembleDebug assembleDebugAndroidTest
```

Dla panelu administracyjnego:

```powershell
cd admin-panel
npm.cmd ci
npm.cmd run lint
npm.cmd run format:check
npm.cmd run a11y:check
npm.cmd run build
```

Testy urządzeniowe, jeśli środowisko jest dostępne:

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat connectedDebugAndroidTest
```

## Wynik

```text
Urządzenie:
Android:
Build:
TalkBack: PASS / FAIL / BLOCKED
Large font: PASS / FAIL / BLOCKED
Display size: PASS / FAIL / BLOCKED
Landscape: PASS / FAIL / BLOCKED
Krytyczne problemy:
Dowody:
```
