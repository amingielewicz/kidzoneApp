# Audyt pobierania danych miejsc

Powiązane issue: #169  
Milestone: v0.2.0-beta — data & scalability foundation

## Cel

Sprawdzić, czy obecny fundament pobierania miejsc jest gotowy na większą liczbę rekordów oraz wskazać, które elementy issue #169 są już zrobione, a które wymagają dalszych zmian.

## Stan obecny

### Repozytorium miejsc

Aplikacja korzysta z `FirestorePlaceRepository` jako centralnej implementacji `PlaceRepository`.

W repozytorium są już obecne ważne elementy ograniczające koszt i liczbę odczytów:

- `PAGE_SIZE_SNAPSHOT = 20` dla pierwszej strony obserwowanych miejsc,
- `OWNER_PLACES_LIMIT = 100`,
- `GEO_QUERY_LIMIT = 200`,
- współdzielone flow przez `shareIn(...)`,
- cache przez Room,
- paginowane pobieranie przez `getPlacesPage(...)`,
- pobieranie miejsc w bounds mapy przez `getPlacesInBounds(...)`,
- limity dla zapytań geohash.

To oznacza, że duża część fundamentu została już wdrożona i issue #169 nie powinno być traktowane jako praca od zera.

## Status względem #169

| Obszar | Status | Komentarz |
|---|---|---|
| Jedno źródło prawdy dla miejsc | częściowo zrobione | `FirestorePlaceRepository` pełni rolę głównej implementacji. |
| Cache miejsc | częściowo zrobione | Jest Room cache i synchronizacja z Firestore. |
| Limit pierwszej strony | zrobione | Snapshot listener używa limitu 20. |
| Lista 20 na stronę | częściowo zrobione | Interfejs ma `getPlacesPage(...)`; trzeba potwierdzić użycie w UI. |
| Debounce wyszukiwarki | do weryfikacji | Trzeba sprawdzić ViewModel/ecran listy. |
| Ranking bez pobierania opinii | do weryfikacji | Trzeba sprawdzić ekran rankingowy i repozytorium opinii. |
| Karty miejsc bez dodatkowych requestów opinii | do weryfikacji | Trzeba sprawdzić komponenty kart i list. |
| Stabilne klucze Compose | do weryfikacji | Trzeba sprawdzić `LazyColumn`, `LazyRow`, mapę i ranking. |
| Profil bez skanowania całej bazy | do weryfikacji | Trzeba sprawdzić ekran profilu i statystyki użytkownika. |
| Dataset 100/1000 miejsc | niezrobione | Zakres lepiej zostawić w #179. |

## Najważniejsze ryzyka

### 1. UI może nadal omijać paginację

Repozytorium udostępnia `getPlacesPage(...)`, ale samo istnienie metody nie potwierdza, że UI faktycznie jej używa.

Do sprawdzenia:

- `PlaceListViewModel`,
- ekran listy miejsc,
- ekran startowy,
- ranking,
- mapa.

### 2. Search może wymagać spójnej normalizacji

Firestore prefix search po `name` może być zależny od wielkości liter i znaków diakrytycznych. To nie blokuje milestone, ale warto zaplanować osobne issue dla pola znormalizowanego, np. `searchName`.

### 3. Mapa powinna zostać w #171

Mapa ma już metodę `getPlacesInBounds(...)`, więc część fundamentu istnieje. Optymalizacja ruchu kamery, throttle/debounce i markery powinny zostać w issue #171, nie w #169.

### 4. Dane testowe powinny zostać w #179

Walidacja 100/1000 miejsc wymaga osobnego generatora i powtarzalnego procesu. To nie powinno blokować mniejszych PR-ów porządkujących samo pobieranie.

## Rekomendowany podział pracy

### PR 1 — audyt i plan

Ten dokument.

Cel: potwierdzić aktualny stan i zawęzić #169 do realnych braków.

### PR 2 — użycie paginacji w UI

Zakres:

- sprawdzić i ewentualnie poprawić listę miejsc,
- upewnić się, że pierwsza strona i kolejne strony korzystają z `getPlacesPage(...)`,
- ograniczyć ryzyko dublowania pierwszej strony.

### PR 3 — debounce i query state

Zakres:

- sprawdzić wyszukiwarkę,
- dodać albo potwierdzić debounce 300–500 ms,
- upewnić się, że szybkie wpisywanie nie generuje lawiny zapytań.

### PR 4 — ranking i karty miejsc

Zakres:

- upewnić się, że ranking korzysta z `averageRating` i `reviewsCount`,
- upewnić się, że karta miejsca nie pobiera opinii osobnym requestem,
- dodać testy tam, gdzie to realne.

### PR 5 — profil i statystyki

Zakres:

- sprawdzić, czy profil użytkownika nie liczy statystyk przez skan całej bazy,
- zaplanować denormalizację/statystyki użytkownika, jeśli będzie potrzebna.

## Decyzja

Issue #169 powinno pozostać otwarte do czasu potwierdzenia użycia paginacji, debounce, rankingu i profilu po stronie UI.

Nie ma jednak potrzeby zaczynać od dużego refaktoru repozytorium, bo podstawowe limity, cache i metody paginacji są już obecne.

## Koszt

0 zł za zmiany w repozytorium.

Realne koszty mogą pojawić się wyłącznie przy zwiększonym użyciu Firestore, dlatego dalsze PR-y powinny ograniczać liczbę odczytów i unikać niekontrolowanych listenerów.
