# Large Font Guide

## Cel

Dokument opisuje wymagania dotyczące obsługi dużych rozmiarów czcionek i skalowania UI w aplikacji KidZone.

## Wymagania

- Tekst powinien skalować się zgodnie z ustawieniami systemowymi Android.
- Layout nie może ucinać treści przy dużej czcionce.
- Przyciski i pola formularzy muszą zachować czytelność.
- Ekrany powinny działać przy zwiększonym font scale.

## Scenariusze testowe

- Ekran logowania.
- Ekran rejestracji.
- Home.
- Lista miejsc.
- Szczegóły miejsca.
- Formularz dodawania miejsca.
- Formularz opinii.
- Profil.
- Ranking.
- Dialogi i snackbary.

## Zasady UI

- Unikać sztywnych wysokości tam, gdzie tekst może się powiększyć.
- Preferować elastyczne kontenery.
- Zapewnić możliwość przewijania, gdy treść nie mieści się na ekranie.
- Nie ukrywać krytycznych akcji poza ekranem bez scrolla.

## Checklist

- [ ] Tekst nie jest ucięty.
- [ ] Przyciski są widoczne.
- [ ] Formularze są używalne.
- [ ] Dialogi mieszczą treść albo są przewijalne.
- [ ] Ekrany działają przy dużym font scale.