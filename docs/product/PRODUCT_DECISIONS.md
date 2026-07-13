# Product Decisions

Ostatnia aktualizacja: 2026-07-14

## Cel

Rejestr decyzji produktowych kidZone wraz z kontekstem, uzasadnieniem i konsekwencjami. Dokument ogranicza powracanie do już rozstrzygniętych dyskusji.

## Format decyzji

```text
Data:
Status: proposed / accepted / superseded
Decyzja:
Kontekst:
Alternatywy:
Uzasadnienie:
Konsekwencje:
Powiązane issue / PR:
```

## Mapa i lista są równorzędne

Data: 2026-06

Status: accepted

Decyzja: mapa nie jest jedynym sposobem odkrywania miejsc. Lista pozostaje pełnym fallbackiem i równorzędnym wejściem.

Uzasadnienie:

- działa bez zgody na lokalizację,
- poprawia dostępność,
- jest stabilniejsza przy słabym internecie,
- umożliwia wykonanie tych samych zadań z TalkBack.

Konsekwencje:

- miejsce dostępne na mapie powinno być możliwe do znalezienia na liście,
- odmowa lokalizacji nie blokuje aplikacji,
- zmiany mapy muszą uwzględniać fallback listowy.

## Kategorie używają wspólnego badge

Data: 2026-06

Status: accepted

Decyzja: kategorie są prezentowane przez wspólny badge lub ikonę zamiast ciężkich belek.

Konsekwencje:

- jeden komponent i jedna semantyka kategorii,
- spójność Startu, Listy, Mapy, Rankingu i szczegółów,
- kolor nie jest jedynym nośnikiem kategorii.

## Publiczny release wymaga GO / NO-GO

Data: 2026-06

Status: accepted

Decyzja: release produkcyjny wymaga zakończonej checklisty GO / NO-GO.

Konsekwencje:

- signed AAB, smoke i monitoring są obowiązkowe,
- runtime permissions, account deletion, Data Safety, Rules i App Check są bramami,
- brak dowodów oznacza NO-GO.

## Ranking korzysta z pól agregowanych

Data: 2026-06

Status: accepted

Decyzja: ranking używa pól takich jak `ratingAverage`, `reviewsCount` i kontrolowany `rankScore`.

Konsekwencje:

- klient nie pobiera wszystkich opinii,
- agregaty są aktualizowane po stronie zaufanej,
- wymagane są testy spójności i mechanizm naprawy,
- klient nie może samodzielnie zmieniać pól rankingowych.

## Onboarding jest krótki i pomijalny

Data: 2026-06

Status: accepted

Decyzja: onboarding wyjaśnia główne funkcje, ale nie blokuje wejścia do aplikacji.

Konsekwencje:

- użytkownik może go pominąć,
- zgody są proszone dopiero w kontekście funkcji,
- powrót do aplikacji nie wymusza ponownego onboardingu.

## Uprawnienia są opcjonalne

Data: 2026-07

Status: accepted

Decyzja: lokalizacja, kamera i powiadomienia nie są wymagane do podstawowego korzystania z kidZone.

Konsekwencje:

- wszystkie punkty wejścia mają wspólną obsługę odmowy,
- trwała odmowa prowadzi do ustawień aplikacji,
- brak zgody nie tworzy martwej akcji,
- Photo Picker działa bez `READ_MEDIA_IMAGES` i `READ_EXTERNAL_STORAGE`,
- aplikacja nie deklaruje `ACCESS_BACKGROUND_LOCATION`.

## Dane publiczne i prywatne są rozdzielone

Data: 2026-07

Status: accepted as target architecture

Decyzja: publiczny profil i prywatne dane użytkownika są przechowywane osobno.

Docelowo:

```text
users/{uid}
users/{uid}/private/profile
users/{uid}/private/messaging
users/{uid}/private/preferences
```

Konsekwencje:

- e-mail i tokeny FCM nie są publiczne,
- migracja wymaga kompatybilności ze starszym buildem,
- account deletion obejmuje wszystkie prywatne subdokumenty.

## Usunięcie konta jest bramą release

Data: 2026-07

Status: accepted

Decyzja: publiczny release nie może otrzymać GO bez pełnego testu account deletion.

Konsekwencje:

- test obejmuje Auth, Firestore, Storage, FCM, Room, cache i widget,
- częściowy cleanup nie jest pełnym sukcesem,
- publiczna strona usuwania konta i Play Console muszą być aktualne.

## Zapisy offline nie udają sukcesu

Data: 2026-07

Status: accepted

Decyzja: dopóki kolejka replay nie gwarantuje synchronizacji, zapis offline nie jest prezentowany jako zakończony sukces.

Konsekwencje:

- użytkownik widzi stan oczekujący albo kontrolowany błąd,
- retry jest idempotentne,
- brak internetu nie tworzy duplikatów danych.

## Checklista nowej decyzji

- [ ] ma datę i status,
- [ ] opisuje kontekst i alternatywy,
- [ ] zawiera mierzalne konsekwencje,
- [ ] uwzględnia UX, accessibility, privacy, security i koszty,
- [ ] wskazuje potrzebne testy i migracje,
- [ ] ma powiązane issue lub PR, jeśli istnieje,
- [ ] decyzja zastąpiona wskazuje następcę.
