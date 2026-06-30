# Navigation

## Cel

Dokument opisuje architekturę nawigacji Android w KidZone opartą o Navigation Compose.

## Założenia

- Jeden główny NavHost.
- Ekrany identyfikowane przez typowane trasy.
- Argumenty przekazywane wyłącznie przez route lub SavedStateHandle.
- ViewModel nie zna NavController.

## Deep Links

Obsługiwane typy:

- kidzone://place/{placeId}
- https://.../place/{placeId}

Każdy deep link powinien:

- walidować argumenty,
- obsłużyć brak danych,
- przekierować do ekranu błędu, jeśli zasób nie istnieje.

## Back Stack

Zasady:

- Back zawsze wraca do logicznie poprzedniego ekranu.
- Po logowaniu użytkownik nie wraca do ekranu logowania.
- Po wylogowaniu stos nawigacji jest czyszczony.

## Argumenty

Preferowane typy:

- String ID
- Boolean
- Enum

Nie przekazujemy dużych obiektów między ekranami.

## Checklist

- [ ] Wszystkie trasy są opisane.
- [ ] Deep linki są przetestowane.
- [ ] Back stack działa poprawnie.
- [ ] Brak zależności ViewModel -> NavController.
- [ ] Obsłużone są błędne argumenty.
