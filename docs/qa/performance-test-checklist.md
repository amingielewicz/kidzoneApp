# Performance and load test checklist

Powiazane issue: #174

## Cel

Ta checklista sluzy do recznej walidacji wydajnosci aplikacji kidZone przed release albo po zmianach dotykajacych mapy, list miejsc, rankingu, dodawania miejsca, uploadu zdjec lub Firebase.

Nie zastepuje testow automatycznych. Ma dac powtarzalny sposob sprawdzenia, czy aplikacja jest plynna dla uzytkownika koncowego i czy Firebase Performance pokazuje sensowne dane diagnostyczne.

## Dane testu

Przed rozpoczeciem uzupelnij:

```text
Data testu:
Tester:
Wersja aplikacji:
Commit:
Build: debug / release / internal test
Urzadzenie:
Android version:
Siec: Wi-Fi / LTE / throttled / offline
Firebase project:
Plan Firebase: Spark / Blaze
Wynik: PASS / FAIL / BLOCKED
Uwagi:
```

## Przygotowanie

- [ ] aplikacja jest zbudowana z aktualnego `main` albo brancha release,
- [ ] test jest wykonywany na realnym urzadzeniu, jesli sprawdzamy plynnosc mapy i upload zdjec,
- [ ] tester ma konto testowe z co najmniej jednym dodanym miejscem,
- [ ] w bazie sa miejsca z roznych kategorii i kilka miejsc z opiniami,
- [ ] tester ma dostep do Firebase Console, jesli sprawdza Performance Monitoring i Crashlytics,
- [ ] znany jest stan limitow i billing Firebase/Google Cloud,
- [ ] przed testem aplikacja zostala uruchomiona na czysto po instalacji albo po wyczyszczeniu danych, jesli sprawdzamy cold start.

## Komendy lokalne

Przed testem manualnym wykonaj:

```powershell
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat testDebugUnitTest
$env:MAPS_API_KEY="AIzaSyPlaceholder"; .\gradlew.bat detekt
```

Opcjonalnie, jesli konfiguracja podpisu release jest dostepna:

```powershell
.\gradlew.bat assembleRelease
```

## Scenariusze wydajnosciowe

### Cold start

- [ ] aplikacja startuje bez bialego/pustego ekranu utrzymujacego sie dluzej niz kilka sekund,
- [ ] ekran startowy pokazuje loading state albo docelowa tresc,
- [ ] po pierwszym uruchomieniu nie ma widocznego przyciecia przy inicjalizacji Firebase, Remote Config, FCM i cache,
- [ ] kolejne uruchomienie jest szybsze albo porownywalne z pierwszym.

### Zakladka Start

- [ ] sekcja `Blisko Ciebie` laduje sie bez blokowania UI,
- [ ] `TOP blisko Ciebie` nie powoduje dlugiego pustego stanu,
- [ ] `Ostatnio dodane w okolicy` pokazuje maksymalnie oczekiwana liczbe kart i nie rozpycha ekranu,
- [ ] brak lokalizacji albo odmowa uprawnienia pokazuje fallback bez petli loadingu,
- [ ] po powrocie internetu dane moga sie odswiezyc bez restartu aplikacji.

### Mapa

- [ ] pierwsze otwarcie mapy jest plynne,
- [ ] przesuwanie mapy nie powoduje widocznych przyciec,
- [ ] zoom nie wykonuje niekontrolowanej liczby zapytan,
- [ ] markery pojawiaja sie w rozsadnym czasie,
- [ ] lista fallback dla mapy dziala, jesli mapa nie jest dostepna,
- [ ] przejscie z mapy do szczegolow miejsca i powrot nie resetuje niepotrzebnie calego stanu.

### Lista i wyszukiwarka miejsc

- [ ] lista miejsc renderuje sie bez skokow ukladu,
- [ ] paginacja nie blokuje przewijania,
- [ ] wyszukiwanie po wpisaniu tekstu nie odpala zapytan po kazdym pojedynczym znaku bez debounce,
- [ ] pusty wynik wyszukiwania pokazuje czytelny stan,
- [ ] czyszczenie wyszukiwania szybko przywraca liste.

### Ranking

- [ ] ranking miejsc i uzytkownikow laduje sie bez dlugiego pustego ekranu,
- [ ] odswiezenie rankingu nie blokuje przewijania,
- [ ] brak danych pokazuje pusty stan zamiast spinnera bez konca,
- [ ] wejscie w miejsce z rankingu jest responsywne.

### Dodawanie miejsca

- [ ] formularz reaguje plynnie przy wpisywaniu dlugiej nazwy i opisu,
- [ ] wybor kategorii, udogodnien i lokalizacji nie powoduje przyciec,
- [ ] sprawdzanie miejsc w poblizu nie blokuje formularza,
- [ ] zapis bez zdjec pokazuje jasny loading state i konczy sie sukcesem albo czytelnym bledem,
- [ ] walidacja blednych danych jest natychmiastowa i zrozumiala.

### Upload zdjec

- [ ] wybor jednego zdjecia nie blokuje UI,
- [ ] wybor kilku zdjec pokazuje postep albo stan uploadu,
- [ ] kompresja zdjec nie powoduje dlugiego zamrozenia ekranu,
- [ ] zbyt duze albo bledne zdjecie daje kontrolowany komunikat,
- [ ] brak internetu podczas uploadu daje kontrolowany komunikat,
- [ ] po bledzie uploadu uzytkownik moze sprobowac ponownie.

## Firebase Performance Monitoring

Po wykonaniu scenariuszy sprawdz w Firebase Console, czy pojawiaja sie albo sa oczekiwane trace:

- [ ] `cold_start`,
- [ ] `location_fetch`,
- [ ] `image_compress`,
- [ ] `photo_upload`,
- [ ] `remote_config_fetch`,
- [ ] `place_load`,
- [ ] `nearby_places_load`,
- [ ] `map_places_load`,
- [ ] `top_places_load`,
- [ ] `places_page_load`,
- [ ] `place_search_load`,
- [ ] `add_place`.

## Remote Config parametry wydajnosciowe

W Firebase Console -> Remote Config sprawdz albo ustaw:

| Klucz | Domyslnie | Bezpieczny zakres w aplikacji | Wplyw |
| --- | ---: | ---: | --- |
| `perf_home_nearby_limit` | 20 | 5-40 | liczba kart w sekcji `Blisko Ciebie` |
| `perf_home_top_places_limit` | 20 | 5-40 | liczba kart w sekcji `TOP blisko Ciebie` |
| `perf_home_recently_added_limit` | 10 | 3-30 | liczba kart w sekcji `Ostatnio dodane w okolicy` |
| `perf_home_top_places_radius_km` | 10 | 1-50 | promien lokalnego rankingu top miejsc |
| `perf_home_fetch_radius_km` | 50 | 5-100 | promien jednego fetcha danych dla Start |
| `perf_map_markers_limit` | 1000 | 100-2000 | maksymalna liczba miejsc pobieranych dla viewportu mapy |
| `perf_ranking_top_limit` | 100 | 10-200 | liczba pozycji pokazywana w rankingu |
| `perf_ranking_fetch_pool` | 200 | 20-500 | pula pobierana przed filtrowaniem rankingu |

- [ ] zmiana `perf_home_nearby_limit` po fetchu Remote Config ogranicza liczbe kart na Start,
- [ ] zmiana `perf_map_markers_limit` nie powoduje pustej mapy ani widocznego przyciecia,
- [ ] `perf_ranking_fetch_pool` jest nie mniejszy niz `perf_ranking_top_limit`,
- [ ] nieprawidlowa wartosc w Remote Config wraca do domyslnego fallbacku.

Dla kazdego dostepnego trace sprawdz:

- [ ] trace ma rozsadna liczbe probek po testach,
- [ ] trace nie raportuje stalego statusu `error`,
- [ ] p95/p99 nie wskazuje oczywistej regresji,
- [ ] najwolniejsze trace maja jasny kontekst funkcjonalny,
- [ ] nie ma custom attributes zawierajacych e-mail, token, pelne URL-e zdjec albo inne dane prywatne.

## Crashlytics i logowanie release

- [ ] release nie uzywa `Timber.DebugTree`,
- [ ] `WARN+` trafia do Crashlytics jako breadcrumb albo non-fatal,
- [ ] breadcrumb nie zawiera e-maili ani dlugich tokenopodobnych wartosci,
- [ ] wymuszony testowy non-fatal pojawia sie w Firebase Console,
- [ ] Crashlytics nie pokazuje krytycznych crashy po smoke tescie.

## Testy obciazeniowe manualne

Te scenariusze wykonuj tylko na projekcie testowym albo z pelna swiadomoscia kosztow Firebase/Google Cloud.

- [ ] 20 szybkich wejsc/wyjsc w ekran mapy nie powoduje crasha,
- [ ] 20 kolejnych wyszukiwan miejsc nie powoduje limitow ani wyraznego spowolnienia,
- [ ] 10 wejsc w szczegoly roznych miejsc nie powoduje narastajacego opoznienia,
- [ ] 5 dodan miejsca pod rzad nie zostawia aplikacji w stanie loading,
- [ ] 5 uploadow zdjec pod rzad nie powoduje wycieku pamieci widocznego jako systemowe zamykanie aplikacji,
- [ ] po intensywnym tescie aplikacja nadal poprawnie otwiera Start, Mapy, Liste i Profil.

## Kryteria PASS

Test mozna uznac za PASS, jesli:

- [ ] wszystkie krytyczne scenariusze uzytkownika dzialaja bez crasha,
- [ ] loading state nie blokuje aplikacji na stale,
- [ ] Firebase Performance pokazuje trace dla najwazniejszych operacji albo brak danych jest wyjasniony typem buildu,
- [ ] Crashlytics nie pokazuje krytycznych nowych awarii,
- [ ] nie znaleziono logow z oczywistymi danymi wrazliwymi,
- [ ] znane problemy sa wpisane do issue z priorytetem.

## Kryteria FAIL / BLOCKED

Oznacz FAIL albo BLOCKED, jesli:

- aplikacja crashuje w podstawowym flow,
- mapa/lista/ranking regularnie zawieszaja UI,
- upload zdjec nie daje sie ukonczyc na stabilnej sieci,
- Performance Monitoring albo Crashlytics nie sa mozliwe do sprawdzenia, mimo ze release ma je wlaczone,
- Firebase/Google Cloud billing albo limity uniemozliwiaja wykonanie testu.
