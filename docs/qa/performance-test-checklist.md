# Performance and load test checklist

Powiązane issue: #174

Ostatnia aktualizacja: 2026-07-14

## Cel

Manualna walidacja wydajności kidZone przed release oraz po zmianach mapy, list, rankingu, formularzy, zdjęć, cache albo Firebase.

## Dane testu

```text
Data:
Tester:
Version name / code:
Commit:
Build: debug / signed release / Internal Testing
Urządzenie:
Android:
Sieć: Wi-Fi / LTE / throttled / offline
Firebase project:
Wynik: PASS / FAIL / BLOCKED
Dowody:
```

## Przygotowanie

- [ ] właściwy build i projekt Firebase,
- [ ] fizyczne urządzenie dla mapy, zdjęć i płynności,
- [ ] konto oraz dane testowe,
- [ ] dostęp do Performance, Crashlytics, vitals i billing,
- [ ] znane limity Remote Config,
- [ ] czysta instalacja dla cold start,
- [ ] aktualizacja z poprzedniej wersji dla warm start i migracji,
- [ ] brak produkcyjnych danych w testach obciążeniowych.

## Automatyczne bramki

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat testDebugUnitTest detekt lintDebug
```

Dla signed artefaktu użyj właściwego zadania release zgodnego z konfiguracją projektu.

## Start aplikacji

- [ ] cold start nie pozostawia długiego pustego ekranu,
- [ ] użytkownik widzi loading lub treść,
- [ ] inicjalizacja Firebase, Remote Config, FCM, Room i App Check nie zamraża UI,
- [ ] kolejne uruchomienie nie jest wyraźnie wolniejsze,
- [ ] migracja po aktualizacji nie powoduje ANR,
- [ ] start offline ma kontrolowany stan.

## Start, Lista i wyszukiwanie

- [ ] sekcje Startu ładują się niezależnie bez blokowania całego ekranu,
- [ ] brak lokalizacji ma szybki fallback,
- [ ] lista nie skacze podczas doładowania,
- [ ] paginacja nie blokuje scrolla i nie duplikuje rekordów,
- [ ] wyszukiwanie ma debounce,
- [ ] filtry i sortowanie reagują bez widocznego zamrożenia,
- [ ] czyszczenie wyszukiwania szybko przywraca dane,
- [ ] cache pozostaje użyteczny offline.

## Mapa

- [ ] pierwsze otwarcie jest płynne,
- [ ] ruch i zoom nie powodują lawiny zapytań,
- [ ] bounds, limit markerów i clustering działają,
- [ ] markery pojawiają się w akceptowalnym czasie,
- [ ] przejście do szczegółów i powrót nie resetuje całego stanu,
- [ ] fallback listowy działa,
- [ ] odmowa lokalizacji i wyłączony GPS nie tworzą nieskończonego loadingu,
- [ ] powrót z ustawień nie uruchamia wielu równoległych requestów.

## Ranking i Profil

- [ ] ranking nie pobiera nieograniczonego zbioru,
- [ ] refresh nie blokuje przewijania,
- [ ] puste dane kończą się empty state,
- [ ] pola agregowane pozwalają uniknąć N+1 reads,
- [ ] Profil, odznaki i statystyki nie tworzą kaskady requestów.

## Formularze i zapis

- [ ] wpisywanie długich danych jest płynne,
- [ ] walidacja nie blokuje głównego wątku,
- [ ] wyszukiwanie duplikatów miejsc ma limit i timeout,
- [ ] zapis ma jednoznaczny loading,
- [ ] double submit jest blokowany,
- [ ] retry nie tworzy duplikatu,
- [ ] zapis offline nie udaje sukcesu,
- [ ] częściowy błąd nie pozostawia trwałego spinnera.

## Zdjęcia

- [ ] Photo Picker otwiera się bez szerokiego dostępu do galerii,
- [ ] wybór jednego i kilku zdjęć nie zamraża UI,
- [ ] kompresja odbywa się poza głównym wątkiem,
- [ ] postęp uploadu jest widoczny,
- [ ] zły MIME lub rozmiar daje kontrolowany błąd,
- [ ] przerwanie sieci umożliwia bezpieczny retry,
- [ ] retry nie tworzy kilku plików,
- [ ] EXIF/GPS cleanup nie powoduje zauważalnego opóźnienia,
- [ ] po serii uploadów nie widać narastającego zużycia pamięci.

## Account deletion i cleanup

- [ ] operacja nie blokuje UI bez informacji,
- [ ] długie etapy mają kontrolowany status,
- [ ] błąd częściowy jest widoczny,
- [ ] retry cleanup jest idempotentne,
- [ ] logout/delete czyści lokalny cache i widget bez ANR,
- [ ] masowy cleanup nie powoduje niekontrolowanych kosztów.

## Firebase Performance

Sprawdź istniejące trace odpowiadające realnej implementacji, między innymi:

- cold start,
- location fetch,
- image compression,
- photo upload,
- Remote Config fetch,
- loading miejsc, mapy, listy i rankingu,
- add place,
- synchronizację offline.

Dla każdego trace:

- [ ] ma próbki dla testowanego builda,
- [ ] status błędu nie jest stale aktywny,
- [ ] p95/p99 nie pokazuje oczywistej regresji,
- [ ] atrybuty nie zawierają PII, dokładnej lokalizacji, URI zdjęć ani tokenów,
- [ ] nazwa i kontekst pozwalają rozpoznać operację.

Nie dopisuj fikcyjnego trace do checklisty tylko dlatego, że był kiedyś planowany. Nazwy muszą odpowiadać aktualnemu kodowi.

## Remote Config

Dla parametrów wpływających na wydajność sprawdź:

- wartości domyślne w aplikacji,
- bezpieczny zakres,
- fallback dla wartości błędnej,
- zależności między limitami,
- rollback,
- wpływ na koszty.

Szczególną uwagę zwróć na limity Startu, mapy, rankingu, paginacji, debounce i zdjęć.

## Crashlytics, vitals i logi

- [ ] brak nowego crasha i ANR,
- [ ] release nie używa debugowego drzewa logowania,
- [ ] breadcrumb i non-fatal nie zawierają PII,
- [ ] intensywny test nie generuje lawiny non-fatal,
- [ ] Android vitals nie pokazuje regresji po rollout,
- [ ] logi backendu nie zawierają tokenów ani pełnych payloadów.

## Kontrolowany test obciążenia

Wykonuj wyłącznie na projekcie testowym albo z zaakceptowanym ryzykiem kosztowym.

- [ ] wielokrotne wejście i wyjście z Mapy,
- [ ] seria wyszukiwań i zmian filtrów,
- [ ] seria wejść w szczegóły,
- [ ] kilka zapisów miejsca i opinii,
- [ ] kilka uploadów zdjęć,
- [ ] powtarzane retry tego samego eventu,
- [ ] szybkie przełączanie sieci online/offline,
- [ ] aplikacja po teście nadal działa bez restartu.

Nie używaj testu obciążenia do generowania spamu lub obchodzenia limitów produkcyjnych.

## Koszty

- [ ] liczba Firestore reads/writes jest zgodna z oczekiwaniem,
- [ ] mapa nie wykonuje nieograniczonych odczytów,
- [ ] upload i transfer Storage są kontrolowane,
- [ ] scheduled Functions mają checkpointy i limity,
- [ ] alerty billing są aktywne,
- [ ] nie ma wzrostu kosztów niewspółmiernego do liczby testów.

## Kryteria PASS

- brak crasha i trwałego loadingu,
- podstawowe flow pozostaje responsywne,
- nie ma oczywistej regresji p95/p99,
- requesty, markery i strony mają limity,
- upload i retry są stabilne,
- telemetryka nie zawiera PII,
- koszt jest zgodny z oczekiwaniem,
- znane problemy mają issue i priorytet.

## FAIL / BLOCKED

FAIL:

- crash lub ANR,
- regularne zamrożenie Mapy/Listy/Rankingu,
- niekończący się loading,
- duplikaty po retry,
- upload niemożliwy na stabilnej sieci,
- oczywisty wyciek PII do telemetryki,
- niekontrolowana liczba requestów lub kosztów.

BLOCKED:

- brak właściwego builda lub projektu,
- brak wymaganych danych albo dostępu do monitoringu,
- billing lub konfiguracja uniemożliwia bezpieczny test,
- środowisko testowe nie odpowiada testowanemu release.
