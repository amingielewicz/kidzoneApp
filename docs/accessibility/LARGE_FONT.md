# Large Font Guide

Ostatnia aktualizacja: 2026-07-14

## Cel

Wymagania dotyczące dużych rozmiarów czcionek, display size i elastycznego layoutu w kidZone.

## Zakres testów

Sprawdź co najmniej:

- domyślny font scale,
- około 1.3x,
- około 1.5x–1.6x,
- największy praktyczny rozmiar dostępny na urządzeniu,
- domyślny i duży display size,
- mały ekran oraz landscape.

## Zasady layoutu

- tekst skaluje się zgodnie z ustawieniami systemu,
- unikamy sztywnych wysokości dla elementów zawierających tekst,
- preferujemy zawijanie i elastyczne kontenery,
- ekran jest przewijalny, gdy treść nie mieści się pionowo,
- CTA nie jest na stałe przycięte poza ekranem,
- wiersz może przejść do układu pionowego, jeśli brakuje miejsca,
- tekst nie nachodzi na ikony ani inne pola,
- nie zmniejszamy tekstu poniżej stylu tylko po to, aby zmieścić layout.

## Nawigacja i app bars

- tytuły nie zasłaniają akcji,
- dolna nawigacja pozostaje czytelna,
- etykiety mogą się skracać tylko z zachowaniem zrozumiałości,
- przycisk Back i akcje app bara mają odpowiedni touch target,
- duży tekst nie powoduje nakładania się elementów systemowych.

## Karty i listy

- wysokość karty wynika z treści,
- nazwa miejsca może zajmować kilka linii,
- kategoria, ocena i dystans zachowują hierarchię,
- długi adres i opis mają sensowne zawijanie lub kontrolowane skrócenie,
- element listy nie ucina głównej akcji,
- stabilne klucze i layout nie powodują skoków przy zmianie skali.

## Formularze

- etykieta i komunikat błędu są w pełni czytelne,
- pole nie ma stałej wysokości niezgodnej z tekstem,
- klawiatura nie zasłania CTA,
- formularz jest przewijalny,
- przycisk zapisu pozostaje osiągalny,
- licznik znaków, helper text i błąd nie nachodzą na siebie,
- dialog systemowy, Photo Picker i kamera mają poprawny kontekst przed uruchomieniem.

## Dialogi i bottom sheety

- treść jest przewijalna,
- przyciski nie wychodzą poza ekran,
- akcje destrukcyjne pozostają widoczne i jednoznaczne,
- bottom sheet może rozszerzyć wysokość,
- fokus i TalkBack odpowiadają wizualnemu układowi,
- duży font nie uniemożliwia zamknięcia warstwy.

## Mapa

- overlaye i kontrolki nie nachodzą na siebie,
- bottom sheet zachowuje czytelność,
- komunikaty zgody, GPS i offline mieszczą się lub przewijają,
- fallback listy jest w pełni używalny,
- mapa nie wymaga odczytania tekstu osadzonego wyłącznie wewnątrz markera.

## Scenariusze testowe

- onboarding,
- logowanie, rejestracja i reset hasła,
- Start,
- Lista, filtry i sortowanie,
- Mapa i bottom sheet,
- Szczegóły miejsca,
- Dodaj miejsce,
- Dodaj opinię i zdjęcie,
- Ranking,
- Profil i ustawienia konta,
- account deletion,
- dialogi, snackbary i błędy,
- offline i permission states.

## Typowe problemy

- tekst ucięty przez stałą wysokość,
- CTA poza ekranem bez scrolla,
- nachodzące badge i rating,
- przycisk z jedną linią wymuszającą zbyt mały tekst,
- dialog większy niż viewport,
- bottom navigation bez miejsca na etykietę,
- błąd pola niewidoczny po otwarciu klawiatury,
- poziomy układ, który powinien zmienić się na pionowy.

## Release gate

- [ ] krytyczne flow działa przy 1.5x–1.6x,
- [ ] największy praktyczny font nie blokuje podstawowych akcji,
- [ ] tekst nie nachodzi na inne elementy,
- [ ] formularze i dialogi są przewijalne,
- [ ] CTA pozostają dostępne,
- [ ] lista i mapa mają używalny layout,
- [ ] TalkBack działa również przy dużym foncie,
- [ ] mały ekran i landscape nie ujawniają krytycznych regresji.
