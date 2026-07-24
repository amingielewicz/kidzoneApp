# Accessibility Guide

Ostatnia aktualizacja: 2026-07-13

## Cel

Standard dostępności kidZone dla interfejsu Android. Docelowo aplikacja powinna spełniać założenia WCAG 2.2 AA tam, gdzie mają zastosowanie do aplikacji mobilnej.

## Wymagania podstawowe

- kontrast zgodny z wymaganiami,
- cele dotykowe minimum 48 dp,
- wszystkie elementy interaktywne mają czytelną nazwę lub etykietę,
- kolor nie jest jedynym nośnikiem informacji,
- duża czcionka nie zasłania treści i CTA,
- kolejność fokusu jest logiczna,
- UI działa z TalkBack,
- mapa ma pełną alternatywę listową.

## Semantyka Compose

- ikona dekoracyjna nie jest odczytywana,
- ikona akcji ma `contentDescription` lub czytelną semantykę,
- złożona karta może być scalona w jeden logiczny element,
- przycisk nie jest opisany wyłącznie nazwą ikony,
- stan zaznaczenia, rozwinięcia, błędu i loadingu jest dostępny dla czytnika,
- element klikalny nie ma kilku konkurujących opisów.

## Formularze

- każde pole ma trwałą etykietę,
- błąd jest powiązany z polem i odczytywany,
- pola obowiązkowe są oznaczone tekstowo,
- kolejność przechodzenia klawiaturą i TalkBack jest logiczna,
- klawiatura nie zasłania przycisku zapisu,
- dane nie znikają po błędzie,
- loading przy zapisie jest komunikowany.

## Komunikaty i dynamiczne zmiany

- snackbar, błąd i ważny status są ogłaszane,
- fokus nie przeskakuje bez powodu,
- po otwarciu dialogu lub bottom sheeta fokus trafia do niego,
- po zamknięciu fokus wraca do logicznego miejsca,
- sukces nie jest komunikowany wyłącznie kolorem lub ikoną.

## Mapa

- użytkownik może wykonać równoważne zadanie na liście,
- marker i bottom sheet mają czytelne opisy,
- akcja „Moja lokalizacja” jest opisana,
- brak zgody lub GPS ma tekstowy komunikat i fallback,
- gesty mapy nie są jedynym sposobem dotarcia do miejsca.

## Uprawnienia

- cel zgody jest wyjaśniony przed dialogiem systemowym,
- odmowa ma czytelny dalszy krok,
- trwała odmowa prowadzi do ustawień aplikacji,
- przycisk po odmowie pozostaje dostępny,
- po powrocie z ustawień stan jest ponownie ogłaszany,
- brak zgody nie blokuje całej aplikacji.

## Duża czcionka i layout

Sprawdź co najmniej:

- 100%,
- 130%,
- 160%,
- największy praktyczny rozmiar systemowy.

Tekst nie może nachodzić na inne elementy, być ucinany bez sensownego fallbacku ani ukrywać głównego CTA. Preferujemy zawijanie i elastyczny layout zamiast stałej wysokości.

## Multimedia

- obrazy dekoracyjne są ukryte dla czytnika,
- obrazy przekazujące znaczenie mają opis,
- zdjęcie miejsca nie wymaga szczegółowego automatycznego opisu, jeśli obok znajduje się pełna informacja tekstowa,
- akcje galerii i usuwania zdjęcia są jednoznaczne.

## Testy

- TalkBack na fizycznym urządzeniu lub emulatorze,
- duża czcionka i display size,
- landscape,
- mały ekran,
- ciemny i jasny motyw,
- klawiatura sprzętowa lub Switch Access, jeśli dotyczy,
- brak lokalizacji, internetu i zgód,
- dialogi, bottom sheety, snackbary i formularze.

## Release gate

- [ ] krytyczne flow można ukończyć z TalkBack,
- [ ] touch targety mają minimum 48 dp,
- [ ] duża czcionka nie blokuje CTA,
- [ ] kontrast jest poprawny,
- [ ] mapa ma alternatywę listową,
- [ ] błędy i loading są ogłaszane,
- [ ] dialogi poprawnie zarządzają fokusem,
- [ ] uprawnienia mają dostępny fallback,
- [ ] nie ma krytycznego problemu accessibility bez zaakceptowanego issue.
