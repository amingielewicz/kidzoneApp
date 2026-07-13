# UI Checklist

Ostatnia aktualizacja: 2026-07-13

## Cel

Lista kontrolna UI dla kidZone. Stosować przy większych zmianach ekranów oraz przed release.

## Global UI

- [ ] ekran używa wspólnego design systemu,
- [ ] typografia, spacing, radius i ikony są spójne,
- [ ] jasny i ciemny motyw działają, jeśli są wspierane,
- [ ] długie teksty i nazwy mają bezpieczny fallback,
- [ ] layout działa na małym ekranie i w landscape,
- [ ] duża czcionka nie zasłania kluczowych akcji.

## Stany ekranu

- [ ] loading jest widoczny i nie blokuje bez potrzeby całego ekranu,
- [ ] empty state ma wyjaśnienie i CTA,
- [ ] error state mówi, co zrobić dalej,
- [ ] offline state jest odróżniony od zwykłego błędu,
- [ ] permission state ma działający fallback,
- [ ] saving/submitting blokuje double submit,
- [ ] sukces nie jest pokazany przed zakończeniem całej operacji.

## Karty i kategorie

- [ ] karta ma czytelny tytuł, kategorię, ocenę i metadane,
- [ ] `CategoryBadge` lub odpowiednik jest spójny między ekranami,
- [ ] kolor nie jest jedynym nośnikiem kategorii lub statusu,
- [ ] adres i metadane nie dominują nad nazwą,
- [ ] długie nazwy, opisy i adresy nie psują layoutu,
- [ ] kliknięcie całej karty i CTA nie wykonują dwóch akcji.

## Przyciski i FAB

- [ ] primary action jest jednoznaczna,
- [ ] secondary action nie konkuruje z primary,
- [ ] disabled i loading state są czytelne,
- [ ] touch target ma minimum 48 dp,
- [ ] FAB nie zasłania treści i jasno oznacza „Dodaj miejsce”,
- [ ] destrukcyjne akcje mają potwierdzenie.

## Formularze

- [ ] pola mają etykiety i logiczną kolejność,
- [ ] błędy są pokazane przy właściwym polu,
- [ ] klawiatura nie zasłania CTA,
- [ ] dane nie znikają po błędzie sieci,
- [ ] limit tekstu i zdjęć jest komunikowany,
- [ ] submit jest blokowany podczas operacji,
- [ ] retry nie tworzy duplikatu,
- [ ] pola obowiązkowe są oznaczone tekstowo, nie tylko kolorem.

## Uprawnienia i systemowe flow

- [ ] zgoda jest proszona w kontekście funkcji,
- [ ] zwykła odmowa ma czytelne zachowanie,
- [ ] trwała odmowa prowadzi do ustawień,
- [ ] powrót z ustawień odświeża stan,
- [ ] Photo Picker działa bez szerokiej zgody do galerii,
- [ ] brak lokalizacji, kamery lub powiadomień nie blokuje aplikacji.

## Dostępność

- [ ] ikony akcji mają etykiety,
- [ ] elementy dekoracyjne są ukryte dla TalkBack,
- [ ] kolejność fokusu jest logiczna,
- [ ] loading, error i snackbar są ogłaszane,
- [ ] dialog i bottom sheet poprawnie przejmują fokus,
- [ ] mapa ma alternatywę listową,
- [ ] kontrast jest wystarczający.

## Release UI check

- [ ] Login i Rejestracja,
- [ ] Start,
- [ ] Mapa z lokalizacją i bez niej,
- [ ] Lista, filtry i sortowanie,
- [ ] Szczegóły miejsca,
- [ ] Ranking,
- [ ] Profil i konto,
- [ ] Dodawanie miejsca, opinii i zdjęć,
- [ ] Account deletion,
- [ ] widget i powiadomienia.
