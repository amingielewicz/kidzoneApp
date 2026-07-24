# Navigation

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje zasady nawigacji kidZone opartej na Navigation Compose, deep linkach i przewidywalnym back stacku.

## Założenia

- jeden główny `NavHost`,
- typowane trasy,
- małe argumenty przekazywane przez route lub `SavedStateHandle`,
- ViewModel nie zna `NavController`,
- ekran zgłasza intencję nawigacji przez event lub callback,
- nie przekazujemy pełnych obiektów domenowych między ekranami.

## Główne obszary

Nawigacja obejmuje:

- splash i onboarding,
- logowanie i rejestrację,
- główną nawigację zakładkową,
- Start, Mapę, Listę, Ranking i Profil,
- szczegóły miejsca,
- dodawanie i edycję miejsca,
- opinie i zdjęcia,
- ustawienia konta i dokumenty prawne.

## Argumenty

Preferowane typy:

- identyfikator `String`,
- `Boolean`,
- enum lub typowana trasa,
- proste wartości możliwe do odtworzenia.

Dane ekranu są ponownie pobierane przez ViewModel na podstawie identyfikatora. Nie przekazujemy dużych obiektów ani danych prywatnych w URI.

## Deep linki

Przykładowe typy:

```text
kidzone://place/{placeId}
https://.../place/{placeId}
```

Każdy deep link:

- waliduje schemat i argumenty,
- nie ufa danym wejściowym,
- obsługuje brak zasobu,
- respektuje wymaganie logowania,
- nie omija blokad konta ani uprawnień,
- prowadzi do kontrolowanego błędu zamiast crasha.

Deep link z FCM powinien używać tych samych reguł walidacji.

## Back stack

- Back wraca do logicznie poprzedniego ekranu.
- Po logowaniu ekrany auth są usuwane ze stosu.
- Po wylogowaniu stos części zalogowanej jest czyszczony.
- Po usunięciu konta nie można wrócić do ekranu prywatnego.
- Po ban sign-out stos jest czyszczony.
- Nawigacja z powiadomienia nie powinna tworzyć wielu kopii tego samego ekranu.

## Główne zakładki

Przełączanie zakładek powinno:

- zachowywać oczekiwany stan i scroll,
- nie tworzyć kolejnych kopii root destination,
- nie pobierać ponownie całej bazy bez potrzeby,
- mieć przewidywalne zachowanie Back.

## Nawigacja po sukcesie

Nawigacja po zapisie jest eventem jednorazowym. Nie powinna być przechowywana jako trwały boolean w `UiState`, który może uruchomić się ponownie po rotacji lub powrocie do ekranu.

Podwójne kliknięcie zapisu nie może otworzyć dwóch ekranów ani utworzyć dwóch rekordów.

## Ustawienia systemowe

Otwieranie ustawień aplikacji, ustawień lokalizacji, Photo Pickera i kamery jest akcją platformową. Po powrocie ekran odświeża faktyczny stan zgody lub usługi.

## Checklista

- [ ] wszystkie trasy są opisane,
- [ ] argumenty są małe i walidowane,
- [ ] deep linki mają testy pozytywne i negatywne,
- [ ] ViewModel nie zna `NavController`,
- [ ] auth, logout, ban i delete account czyszczą właściwy back stack,
- [ ] event nawigacyjny nie odtwarza się po rotacji,
- [ ] deep link z FCM nie omija kontroli dostępu,
- [ ] błędny argument nie powoduje crasha.
