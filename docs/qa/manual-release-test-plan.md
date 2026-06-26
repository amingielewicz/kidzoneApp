# Manual release test plan

Powiązane issue: #210

## Cel

Ten dokument opisuje manualną checklistę testów przed release aplikacji kidZone.

Issue #210 pozostaje otwarte po dodaniu tej dokumentacji, ponieważ realne testy muszą zostać wykonane ręcznie na aplikacji, urządzeniu/emulatorze i docelowej konfiguracji Firebase/Google Cloud.

Strategia testow automatycznych i regresji oraz testy wydajnosci, obciazenia,
Firebase Performance i Crashlytics sa rozpisane osobno w:

- [Testing strategy and regression coverage](testing-strategy.md)
- [Performance and load test checklist](performance-test-checklist.md)

## Zakres

Plan obejmuje:

- wiadomości e-mail,
- zakładkę Start,
- zakładkę Mapy,
- listę miejsc,
- ranking,
- profil,
- rejestrację i edycję konta,
- logowanie i zmianę hasła,
- regulamin, RODO i dokumenty prawne,
- bezpieczeństwo,
- Firebase,
- Google Cloud,
- podstawowe testy po release.

## Zasady wykonania testów

Przed rozpoczęciem testów:

- [ ] aplikacja jest zbudowana z aktualnego `main` albo brancha release,
- [ ] tester zna wersję aplikacji i commit,
- [ ] wiadomo, czy testy są na emulatorze czy realnym urządzeniu,
- [ ] wiadomo, czy aplikacja korzysta z Firebase testowego czy produkcyjnego,
- [ ] tester ma konto testowe,
- [ ] tester ma dostęp do Firebase Console / Google Cloud Console, jeśli sprawdza logi i usage,
- [ ] znany jest stan planu Firebase: Spark albo Blaze,
- [ ] wykonano `./gradlew assembleDebug` albo odpowiedni build release.

Minimalny zapis wyniku testów:

```text
Data testu:
Tester:
Wersja aplikacji:
Commit:
Urządzenie / emulator:
Android version:
Firebase project:
Wynik: PASS / FAIL / BLOCKED
Uwagi:
```

## 1. Wiadomości e-mail

### Rejestracja

- [ ] użytkownik może założyć konto z poprawnym adresem e-mail,
- [ ] aplikacja poprawnie obsługuje błędny format e-mail,
- [ ] aplikacja poprawnie obsługuje zajęty e-mail,
- [ ] aplikacja pokazuje zrozumiały komunikat błędu,
- [ ] konto pojawia się w Firebase Authentication.

### Reset hasła

- [ ] użytkownik może wywołać reset hasła,
- [ ] Firebase wysyła wiadomość resetującą hasło,
- [ ] link resetu działa,
- [ ] po zmianie hasła można zalogować się nowym hasłem,
- [ ] stare hasło nie działa.

### Zmiana e-maila, jeśli funkcja istnieje

- [ ] użytkownik widzi aktualny e-mail,
- [ ] zmiana e-maila wymaga poprawnej walidacji,
- [ ] aplikacja nie pozwala ustawić pustego albo błędnego e-maila,
- [ ] zmiana jest widoczna po ponownym zalogowaniu.

## 2. Zakładka Start

- [ ] ekran Start ładuje się bez błędu,
- [ ] ekran Start nie pokazuje pustego ekranu bez komunikatu,
- [ ] widoczne są główne akcje aplikacji,
- [ ] użytkownik rozumie, co może zrobić dalej,
- [ ] elementy są czytelne na małym ekranie,
- [ ] kliknięcie w miejsce prowadzi do szczegółów,
- [ ] brak internetu pokazuje sensowny komunikat,
- [ ] loading state nie blokuje aplikacji na stałe,
- [ ] ekran działa po świeżej instalacji,
- [ ] ekran działa po ponownym uruchomieniu aplikacji.

## 3. Zakładka Mapy

### Uprawnienia

- [ ] aplikacja prosi o lokalizację w odpowiednim momencie,
- [ ] odmowa lokalizacji nie psuje aplikacji,
- [ ] po odmowie użytkownik nadal widzi mapę albo komunikat fallback,
- [ ] po przyznaniu lokalizacji mapa może pokazać okolice użytkownika,
- [ ] aplikacja nie wymaga lokalizacji w tle,
- [ ] w manifestach nie ma `ACCESS_BACKGROUND_LOCATION`, jeśli nie jest potrzebne.

### Działanie mapy

- [ ] mapa ładuje się poprawnie,
- [ ] markery miejsc są widoczne,
- [ ] kliknięcie w marker otwiera szczegóły albo kartę miejsca,
- [ ] przesuwanie mapy nie powoduje crasha,
- [ ] zoom działa poprawnie,
- [ ] większa liczba miejsc nie blokuje UI,
- [ ] ekran nie wykonuje niekontrolowanej liczby zapytań,
- [ ] klucz Google Maps działa na buildzie testowym,
- [ ] klucz Google Maps jest ograniczony do Android apps.

### Brak internetu

- [ ] aplikacja pokazuje komunikat braku internetu,
- [ ] aplikacja nie crashuje,
- [ ] po powrocie internetu mapa może się odświeżyć.

## 4. Zakładka Listy

- [ ] lista miejsc ładuje się poprawnie,
- [ ] pusty stan jest opisany,
- [ ] loading state jest widoczny,
- [ ] błąd ładowania jest obsłużony,
- [ ] kliknięcie w miejsce otwiera szczegóły,
- [ ] długie nazwy miejsc nie psują układu,
- [ ] długie adresy nie psują układu,
- [ ] zdjęcia mają fallback, jeśli brakuje obrazka,
- [ ] lista działa po przewinięciu,
- [ ] lista nie ładuje całej bazy bez potrzeby,
- [ ] brak internetu jest obsłużony.

## 5. Zakładka Ranking

- [ ] ranking ładuje się poprawnie,
- [ ] ranking miejsc jest czytelny,
- [ ] ranking użytkowników jest czytelny,
- [ ] TOP 3 jest wyróżnione, jeśli taka funkcja istnieje,
- [ ] puste rankingi mają komunikat,
- [ ] użytkownik bez wyników nie powoduje błędu,
- [ ] duża liczba rekordów nie blokuje UI,
- [ ] kliknięcie w element rankingu działa zgodnie z projektem,
- [ ] dane rankingowe nie pozwalają zwykłemu użytkownikowi manipulować licznikami.

## 6. Zakładka Profil

- [ ] profil ładuje dane użytkownika,
- [ ] profil działa po świeżym logowaniu,
- [ ] profil działa po restarcie aplikacji,
- [ ] avatar ładuje się poprawnie,
- [ ] brak avatara ma fallback,
- [ ] użytkownik może przejść do swoich miejsc,
- [ ] użytkownik może przejść do swoich opinii,
- [ ] użytkownik może się wylogować,
- [ ] po wylogowaniu aplikacja wraca do ekranu logowania,
- [ ] prywatne dane nie są widoczne publicznie.

## 7. Rejestracja i edycja konta

### Rejestracja

- [ ] formularz rejestracji waliduje puste pola,
- [ ] formularz waliduje format e-mail,
- [ ] formularz waliduje hasło,
- [ ] formularz obsługuje zajęty e-mail,
- [ ] użytkownik po rejestracji trafia do właściwego ekranu,
- [ ] dokument użytkownika powstaje w Firestore,
- [ ] prywatne pola użytkownika nie są publicznie odczytywalne.

### Edycja konta

- [ ] użytkownik może zmienić dostępne dane profilu,
- [ ] aplikacja waliduje dane profilu,
- [ ] zmiany są zapisane po restarcie aplikacji,
- [ ] użytkownik nie może zmienić pól administracyjnych,
- [ ] użytkownik nie może zmienić roli,
- [ ] użytkownik nie może edytować cudzego profilu.

## 8. Logowanie i zmiana hasła

### Logowanie

- [ ] poprawne dane logowania działają,
- [ ] błędne hasło pokazuje komunikat,
- [ ] nieistniejący e-mail pokazuje komunikat,
- [ ] brak internetu jest obsłużony,
- [ ] sesja utrzymuje się po restarcie aplikacji,
- [ ] użytkownik może się wylogować.

### Zmiana hasła / reset

- [ ] reset hasła można uruchomić z ekranu logowania,
- [ ] użytkownik dostaje e-mail resetujący,
- [ ] po zmianie hasła można zalogować się nowym hasłem,
- [ ] aplikacja nie ujawnia zbyt szczegółowych informacji o istnieniu konta, jeśli nie jest to potrzebne.

## 9. Dodawanie miejsca

- [ ] użytkownik może otworzyć formularz dodawania miejsca,
- [ ] wymagane pola są walidowane,
- [ ] można dodać nazwę miejsca,
- [ ] można dodać adres albo lokalizację,
- [ ] można dodać kategorię,
- [ ] można dodać opis,
- [ ] można dodać zdjęcie, jeśli funkcja istnieje,
- [ ] zapis tworzy dokument w Firestore,
- [ ] zdjęcie trafia do właściwej ścieżki Storage,
- [ ] po dodaniu aplikacja pokazuje nowe miejsce,
- [ ] formularz nie pozwala wysłać pustych danych,
- [ ] brak internetu jest obsłużony.

## 10. Opinie, oceny i zdjęcia

- [ ] użytkownik może dodać opinię,
- [ ] użytkownik może dodać ocenę,
- [ ] użytkownik może dodać zdjęcie do opinii, jeśli funkcja istnieje,
- [ ] aplikacja waliduje puste opinie,
- [ ] aplikacja obsługuje długą opinię,
- [ ] opinia pojawia się na ekranie szczegółów,
- [ ] użytkownik nie może edytować cudzej opinii,
- [ ] użytkownik nie może usuwać cudzych zdjęć,
- [ ] zdjęcia nie psują UI,
- [ ] aplikacja informuje o ryzyku publikowania zdjęć dzieci/osób trzecich, jeśli takie ostrzeżenie istnieje.

## 11. Regulamin, RODO i dokumenty prawne

- [ ] link do polityki prywatności działa,
- [ ] link do regulaminu działa,
- [ ] dokumenty są aktualne,
- [ ] dokumenty mają poprawne dane kontaktowe,
- [ ] dokumenty opisują Firebase / Google,
- [ ] dokumenty opisują lokalizację,
- [ ] dokumenty opisują zdjęcia,
- [ ] dokumenty opisują usuwanie konta / danych,
- [ ] formularz Data Safety jest zgodny z dokumentami,
- [ ] aplikacja nie obiecuje mniej niż faktycznie zbiera.

## 12. Security

- [ ] zwykły użytkownik nie może odczytać prywatnych danych innego użytkownika,
- [ ] zwykły użytkownik nie może edytować cudzych danych,
- [ ] zwykły użytkownik nie może ustawić sobie roli admina,
- [ ] zwykły użytkownik nie może zmienić pól ban/role/counters,
- [ ] Storage nie pozwala zapisywać do cudzej ścieżki,
- [ ] tokeny FCM nie są publicznie czytelne,
- [ ] logi nie pokazują haseł, tokenów ani danych prywatnych,
- [ ] App Check jest świadomie skonfigurowany,
- [ ] debug provider nie działa w release.

## 13. Firebase

- [ ] Firebase Authentication działa,
- [ ] Firestore działa,
- [ ] Storage działa,
- [ ] Crashlytics działa w release albo jest świadomie odłożony,
- [ ] Analytics jest zgodne z Data Safety,
- [ ] Performance Monitoring jest zgodne z decyzją kosztową,
- [ ] FCM działa albo jest świadomie odłożone,
- [ ] Firestore Rules są wdrożone,
- [ ] Storage Rules są wdrożone,
- [ ] App Check jest w oczekiwanym trybie,
- [ ] usage Firestore/Storage nie pokazuje nietypowych pików.

Szczegolowa walidacja Performance Monitoring i Crashlytics: [Performance and load test checklist](performance-test-checklist.md).

## 14. Google Cloud

- [ ] aktualny plan Firebase jest znany,
- [ ] jeśli projekt jest na Blaze, budżet i alerty są ustawione,
- [ ] jeśli projekt jest na Spark, jest to zapisane w issue/release notes,
- [ ] Maps API key jest ograniczony do Android apps,
- [ ] API key ma ograniczenia API,
- [ ] niepotrzebne API są wyłączone,
- [ ] usage Google Maps jest sprawdzony,
- [ ] billing nie pokazuje nieznanych kosztów.

## 15. Uprawnienia Androida

- [ ] aplikacja prosi o lokalizację tylko wtedy, gdy jest potrzebna,
- [ ] aplikacja działa po odmowie lokalizacji,
- [ ] aplikacja prosi o powiadomienia na Androidzie 13+,
- [ ] aplikacja działa po odmowie powiadomień,
- [ ] aplikacja prosi o dostęp do zdjęć tylko wtedy, gdy jest potrzebny,
- [ ] aplikacja działa po odmowie zdjęć,
- [ ] aplikacja nie wymaga lokalizacji w tle,
- [ ] użytkownik rozumie, po co aplikacja prosi o uprawnienia.

## 16. Brak internetu i błędy

- [ ] aplikacja pokazuje informację o braku internetu,
- [ ] logowanie bez internetu jest obsłużone,
- [ ] mapa bez internetu jest obsłużona,
- [ ] lista bez internetu jest obsłużona,
- [ ] dodawanie miejsca bez internetu jest obsłużone,
- [ ] upload zdjęcia bez internetu jest obsłużony,
- [ ] aplikacja nie crashuje przy timeoutach,
- [ ] użytkownik widzi sensowny komunikat błędu.

## 17. Build release

Przed finalną paczką sprawdzić:

```powershell
.\gradlew assembleDebug
```

Jeśli release signing jest skonfigurowany:

```powershell
.\gradlew assembleRelease
.\gradlew bundleRelease
```

Jeżeli release signing nie jest skonfigurowany lokalnie, dopuszczalny jest wynik:

```text
compileReleaseKotlin, transformReleaseClassesWithAsm i minifyReleaseWithR8 przechodzą,
packageRelease odpada dopiero na braku konfiguracji podpisu.
```

## 18. Smoke test po release

Po publikacji w kanale testowym:

- [ ] aplikacja instaluje się z Google Play,
- [ ] aplikacja startuje,
- [ ] logowanie działa,
- [ ] mapa działa,
- [ ] lista miejsc działa,
- [ ] ranking działa,
- [ ] profil działa,
- [ ] dodanie miejsca działa,
- [ ] Crashlytics nie pokazuje krytycznych crashy,
- [ ] Firebase usage nie rośnie nietypowo,
- [ ] Google Cloud Billing nie pokazuje nieoczekiwanych kosztów.

## 19. Format raportu z testów

Po wykonaniu testów dopisać komentarz do issue #210:

```markdown
## Wynik testów manualnych

- Data:
- Wersja aplikacji:
- Commit:
- Urządzenie:
- Android:
- Firebase project:
- Wynik ogólny: PASS / FAIL / BLOCKED

### PASS
- ...

### FAIL
- ...

### BLOCKED
- ...

### Uwagi
- ...
```

## Kryteria zamknięcia issue #210

Issue #210 można zamknąć dopiero, gdy:

- testy manualne zostały faktycznie wykonane,
- wyniki są zapisane w issue albo w osobnym raporcie,
- krytyczne błędy mają osobne issue,
- znane blokery release zostały usunięte albo świadomie zaakceptowane,
- build release albo testowy build Google Play został sprawdzony.

Samo dodanie tej dokumentacji nie zamyka issue #210.
