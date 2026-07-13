# Android offline mode

Powiązane issue: #306

Ostatnia aktualizacja: 2026-07-14

## Cel

kidZone pozostaje użyteczne przy braku lub niestabilnej sieci. Room pełni rolę lokalnego cache odczytowego dla publicznych miejsc i opinii, Firebase pozostaje źródłem prawdy, a kolejka zapisów offline nie jest jeszcze produkcyjnie aktywna.

Najważniejsza reguła release:

```text
Aplikacja może pokazywać wcześniej zapisane dane offline, ale nie może informować o zapisaniu miejsca lub opinii, dopóki zapis nie został potwierdzony przez backend.
```

## Aktualny status

| Obszar | Status | Zachowanie |
| --- | --- | --- |
| cache miejsc | aktywny | Room zasila Start, Listę, Mapę i szczegóły |
| cache opinii | aktywny | Room przechowuje opinie pobrane z Firestore |
| widget | aktywny | korzysta z publicznego cache miejsc i prywatnego stanu lokalizacji |
| zapisy offline | wyłączone dla użytkownika | zapis kończy się kontrolowanym błędem |
| `pending_operations` | obecne technicznie | infrastruktura istnieje, replay jest zablokowany |
| conflict resolution | częściowa | udokumentowany model server-wins dla przyszłego replay |

## Architektura

```text
Compose UI / ViewModel
        ↓
Repository
   ↙          ↘
Room cache   Firebase
   ↓
Widget / offline reads

pending_operations
        ↓
WorkManager SyncWorker
        ↓
Firestore replay — dopiero po pełnym wdrożeniu processorów
```

## Dane lokalne

Baza `KidZoneDatabase` zawiera:

- `places`,
- `reviews`,
- `pending_operations`.

Odpowiedzialności:

- `PlaceDao` — listy, wyszukiwanie, bounds, ranking, właściciel i widget,
- `ReviewDao` — opinie miejsca i użytkownika,
- `PendingOperationDao` — statusy pending, in-progress, failed i dead-letter.

Room jest cache możliwym do odbudowania. Destructive fallback może być akceptowalny tylko wtedy, gdy nie usuwa jedynej kopii danych użytkownika ani nie gubi zaakceptowanych zapisów offline.

## Odczyt miejsc

- `observePlaces()` emituje z Room,
- `getPlace()` próbuje Firestore i korzysta z cache jako fallback,
- `getPlacesNear()` używa zapytania zdalnego, a przy błędzie może zwrócić cache,
- `getTopPlaces()` korzysta z pól agregowanych i cache,
- `getPlacesInBounds()` używa danych viewportu oraz limitów,
- `getPlacesPage()` zapisuje pobraną stronę do Room.

Cache staje się użyteczny dopiero po wcześniejszym pobraniu danych online.

## Odczyt opinii

- UI obserwuje dane lokalne,
- snapshot listener aktualizuje Room,
- cache może zasilić szczegóły miejsca i listę opinii użytkownika,
- treści odrzucone lub zgłoszone jako spam nie powinny trafiać do publicznego cache,
- brak cache kończy się kontrolowanym empty/error state.

## Zapisy

Aktualne zapisy są online-first:

- dodanie, zmiana i usunięcie miejsca zapisuje Firebase, a następnie Room,
- opinie zapisują Firebase, a po sukcesie aktualizują cache,
- timeout lub brak sieci zwraca kontrolowany błąd,
- nie jest emitowany fałszywy sukces,
- ponowienie nie może utworzyć duplikatu.

## Kolejka zapisów

`SyncManager.enqueue()` i `SyncWorker` stanowią przygotowanie do przyszłego replay, ale nie oznaczają gotowej funkcji offline-first.

Dopóki processory nie są kompletne:

- operacja nie jest oznaczana jako zsynchronizowana,
- błędny payload jest odrzucany,
- retryable failure zachowuje operację,
- trwały błąd trafia do dead-letter po ustalonym limicie,
- UI nie obiecuje późniejszej synchronizacji.

## Warunki włączenia replay

Przed udostępnieniem zapisów offline wymagane są:

- pełne processory add/update/delete dla miejsca i opinii,
- identyfikatory idempotencji,
- jawny status pending/failed w UI,
- bezpieczne retry i backoff,
- obsługa auth i wygasłej sesji,
- konflikt create/update/delete,
- testy emulatorowe,
- cleanup po logout i account deletion,
- monitoring dead-letter,
- brak utraty danych po aktualizacji aplikacji.

## Konflikty

Docelowy model dla aktualizacji może korzystać z `updatedAtMillis`:

1. Worker odczytuje stan serwera.
2. Nowszy rekord serwera wygrywa nad starszą lokalną zmianą.
3. Lokalna zmiana może zostać zastosowana tylko przy jawnie spełnionej regule.
4. Brak dokumentu na serwerze wymaga decyzji zależnej od typu operacji.
5. Brak połączenia pozostawia operację retryable.

Sam server-wins nie rozwiązuje wszystkich konfliktów. Usunięcia, zdjęcia, agregaty i częściowe zapisy wymagają osobnych reguł.

## Oczekiwane zachowanie błędów

| Sytuacja | Oczekiwane zachowanie |
| --- | --- |
| offline z cache | pokaż cache i status offline |
| offline bez cache | empty/error state z retry |
| timeout zapisu | kontrolowany błąd, brak sukcesu |
| błąd walidacji lub Rules | brak enqueue jako zwykły offline retry |
| auth error | logowanie lub reauthentication |
| failure WorkManager | retry z backoffem |
| limit retry | dead-letter i monitoring |
| częściowy upload | brak pełnego sukcesu, cleanup lub retry |

## UX offline

- Start, Lista, Mapa i szczegóły mogą wyświetlać cache,
- dane powinny być oznaczone zachowaniem i statusem, bez technicznego żargonu,
- rejestracja i zapisy wymagające sieci pokazują czytelny komunikat,
- formularz zachowuje wpisane dane po błędzie,
- retry nie tworzy duplikatów,
- aplikacja nie pozostaje w nieskończonym loadingu,
- brak sieci nie blokuje nawigacji po dostępnych danych.

## Prywatność

Po logout, ban sign-out i account deletion:

- prywatny profil nie jest widoczny z cache,
- tokeny i prywatne preferencje są czyszczone,
- prywatna lokalizacja widgetu jest usuwana,
- widget jest odświeżany,
- pending operations użytkownika nie mogą zostać wykonane po zakończeniu jego sesji,
- publiczny cache może pozostać tylko wtedy, gdy nie identyfikuje poprzedniego użytkownika.

## Cache invalidation

### Miejsca

- cache jest czyszczony według TTL,
- aktualna implementacja używa TTL 7 dni,
- jawny flow może wykonać `clearAll()`,
- rekordy usunięte lub ukryte powinny zniknąć przy odświeżeniu.

### Opinie

- aktualizowane przez snapshot listeners,
- stream miejsca może zastąpić lokalny zestaw,
- brak globalnego TTL wymaga świadomego monitorowania stale data,
- usunięcie konta lub moderacja nie może pozostawić prywatnego powiązania autora.

### Widget

- publiczne miejsca pochodzą z Room,
- ostatnia lokalizacja jest prywatna,
- prywatny stan jest czyszczony po zakończeniu sesji.

## Główne pliki

```text
app/src/main/java/com/kidzone/data/local/KidZoneDatabase.kt
app/src/main/java/com/kidzone/data/local/PlaceDao.kt
app/src/main/java/com/kidzone/data/local/ReviewDao.kt
app/src/main/java/com/kidzone/data/local/sync/PendingOperationDao.kt
app/src/main/java/com/kidzone/data/local/sync/PendingOperationEntity.kt
app/src/main/java/com/kidzone/data/repository/FirestorePlaceRepository.kt
app/src/main/java/com/kidzone/data/repository/FirestoreReviewRepository.kt
app/src/main/java/com/kidzone/sync/SyncManager.kt
app/src/main/java/com/kidzone/sync/SyncWorker.kt
app/src/main/java/com/kidzone/widget/NearbyPlacesWidget.kt
```

## Manual QA

### Cache odczytowy

- [ ] otwórz Start, Listę, Mapę i szczegóły online,
- [ ] otwórz opinie miejsca,
- [ ] wyłącz sieć i uruchom aplikację ponownie,
- [ ] potwierdź dostępność wcześniej pobranych danych,
- [ ] potwierdź kontrolowany stan bez cache,
- [ ] sprawdź wyszukiwanie, filtry i fallback mapy,
- [ ] włącz sieć i sprawdź odświeżenie bez restartu.

### Zapisy

- [ ] offline spróbuj dodać miejsce,
- [ ] aplikacja nie pokazuje sukcesu,
- [ ] formularz zachowuje dane,
- [ ] offline spróbuj dodać opinię,
- [ ] po powrocie sieci nie pojawia się phantom record,
- [ ] ręczny retry tworzy tylko jeden rekord.

### Prywatność

- [ ] dodaj widget i użyj lokalizacji,
- [ ] wykonaj logout,
- [ ] sprawdź widget, restart i cache profilu,
- [ ] powtórz dla ban sign-out i account deletion,
- [ ] sprawdź, że queued operation nie wykonuje się po usunięciu konta.

### Worker

- [ ] testowy pending record nie jest fałszywie oznaczany jako synced,
- [ ] invalid payload jest odrzucany,
- [ ] retry zachowuje operację,
- [ ] dead-letter działa po limicie,
- [ ] logi nie zawierają payloadu ani PII.

## Testy automatyczne

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat testDebugUnitTest detekt
```

Dla zmian Rules:

```powershell
cd tests/firestore-rules
npm test
```

Przed produkcyjnym replay wymagane są również testy integracyjne `SyncWorker` z Firebase Emulator.

## Definition of Done

- [ ] cache odczytowy działa bez crasha,
- [ ] offline bez cache ma kontrolowany stan,
- [ ] zapisy nie udają sukcesu,
- [ ] prywatny stan jest czyszczony po zakończeniu sesji,
- [ ] retry i worker są idempotentne,
- [ ] replay ma testy emulatorowe przed włączeniem,
- [ ] dokumentacja odpowiada faktycznej implementacji.
