# Coding Guidelines

Ostatnia aktualizacja: 2026-07-13

## Cel

Zasady kodowania kidZone mają utrzymać projekt czytelny, testowalny, bezpieczny i łatwy do rozwijania.

## Architektura

```text
Presentation → Domain ← Data → Framework
```

- ViewModel nie importuje `Context`, `Activity`, `View` ani `NavController`.
- Logika biznesowa nie trafia do Composable.
- Domain nie zależy od Androida ani Firebase.
- Repository ukrywa źródła danych, cache i synchronizację.
- Use case opisuje jedną operację biznesową.
- DTO, Entity i wyjątki techniczne nie przeciekają do UI.

## Kotlin

- Używamy pełnych, czytelnych nazw.
- Preferujemy `val` nad `var`.
- Ograniczamy nullable i użycie `!!`.
- Nie połykamy wyjątków pustym `catch`.
- Publiczne API dokumentujemy, gdy kontrakt nie jest oczywisty.
- Funkcje utrzymujemy małe i z jedną odpowiedzialnością.
- Używamy typów domenowych zamiast luźnych `String` i `Boolean`, gdy zmniejsza to ryzyko błędu.

## Coroutines i Flow

- Operacje ViewModelu uruchamiamy w `viewModelScope`.
- Nie używamy `GlobalScope`.
- Obsługujemy anulowanie, timeout i race conditions.
- `StateFlow` służy do trwałego stanu, a `SharedFlow` lub inny event stream do zdarzeń jednorazowych.
- Zapisy i retry projektujemy jako idempotentne.
- Nie uruchamiamy niekontrolowanych kolektorów bez lifecycle.

## Compose

- Composable renderuje stan i emituje akcje.
- Listy używają `LazyColumn` lub `LazyRow` ze stabilnymi `key`.
- Ciężkie filtrowanie, sortowanie i mapowanie wykonujemy poza UI.
- Efekty uboczne mają stabilne klucze.
- Systemowych dialogów nie uruchamiamy z recomposition.
- Wspólne elementy, takie jak `CategoryBadge`, stany ekranu i komponenty formularzy, są współdzielone.
- Kolor nie może być jedynym nośnikiem informacji.

## ViewModel

- wystawia niemutowalny `UiState`,
- przyjmuje intencje użytkownika,
- korzysta z use case'ów lub interfejsów repozytoriów,
- mapuje błędy techniczne,
- chroni przed double submit,
- nie uruchamia bezpośrednio systemowych ekranów,
- nie loguje surowych danych użytkownika.

## Repository i dane

- zapytania mają limity i paginację,
- mapy i listy nie pobierają całych kolekcji,
- cache ma jawne zasady odświeżania,
- operacje offline nie mogą udawać sukcesu,
- mappery oddzielają DTO, Entity i modele domenowe,
- zapis z retry nie tworzy duplikatów,
- błędy częściowe nie są raportowane jako sukces.

## Uprawnienia

- używamy wspólnych handlerów dla lokalizacji i kamery,
- rozróżniamy zwykłą i trwałą odmowę,
- po trwałej odmowie kierujemy do ustawień aplikacji,
- po powrocie odświeżamy rzeczywisty stan,
- nie deklarujemy uprawnień, których aplikacja nie potrzebuje,
- Photo Picker nie wymaga szerokiego dostępu do galerii.

## Logging i telemetryka

Nie logujemy ani nie przekazujemy do Crashlytics lub Analytics:

- haseł i tokenów,
- pełnych e-maili,
- danych prywatnych profilu,
- dokładnej lokalizacji,
- treści opinii i formularzy,
- URI zdjęć,
- pełnych payloadów.

Logi zawierają bezpieczny kontekst: funkcję, operację, wersję, typ błędu i status.

## Testy

- każda poprawka regresji powinna mieć test,
- logika limitów, sortowania, retry i uprawnień nie może być testowana wyłącznie manualnie,
- testy nie używają produkcyjnego Firebase ani prawdziwych sekretów,
- zmiana Rules wymaga testów emulatorowych,
- zmiana flow release, privacy lub security wymaga aktualizacji checklisty.

## PR checklist

- [ ] kod jest w odpowiedniej warstwie,
- [ ] brak ciężkiej logiki w Composable,
- [ ] błędy i retry są obsłużone,
- [ ] brak double submit i duplikatów,
- [ ] testy zostały dodane lub zaktualizowane,
- [ ] logi nie zawierają PII,
- [ ] uprawnienia i Data Safety są nadal zgodne,
- [ ] dokumentacja została zaktualizowana,
- [ ] wymagane buildy i testy przechodzą.
