# Manual release test plan

Powiązane issue: #210, #274, #303

Ostatnia aktualizacja: 2026-07-13

## Cel

Manualna checklista regresji przed wydaniem kidZone. Testy należy wykonać na buildzie wskazanym przez commit lub tag oraz zapisać wynik PASS, FAIL albo BLOCKED.

## Dane testu

```text
Data:
Tester:
Wersja aplikacji:
Commit / tag:
Build type:
Urządzenie:
Android:
Firebase project:
Wynik: PASS / FAIL / BLOCKED
Uwagi:
```

## Przygotowanie

- [ ] build pochodzi z aktualnego `main` albo brancha release,
- [ ] znany jest `versionName` i `versionCode`,
- [ ] używany projekt Firebase jest zapisany,
- [ ] tester ma konto testowe,
- [ ] dostępne jest co najmniej jedno fizyczne urządzenie,
- [ ] wykonano signed build albo build odpowiadający testowanemu trackowi,
- [ ] znany jest stan usług Firebase i Google Maps.

## 1. Instalacja i aktualizacja

- [ ] czysta instalacja działa,
- [ ] pierwsze uruchomienie nie crashuje,
- [ ] aktualizacja z poprzedniej wersji działa,
- [ ] dane lokalne i sesja zachowują się zgodnie z projektem,
- [ ] po restarcie aplikacja wraca do poprawnego ekranu.

## 2. Rejestracja i logowanie

- [ ] rejestracja poprawnym e-mailem działa,
- [ ] walidacja błędnego i zajętego e-maila działa,
- [ ] logowanie poprawnymi danymi działa,
- [ ] błędne hasło i brak internetu są obsłużone,
- [ ] reset hasła działa,
- [ ] sesja utrzymuje się po restarcie,
- [ ] wylogowanie usuwa dostęp do danych prywatnych.

## 3. Start

- [ ] ekran ładuje się bez pustego stanu bez wyjaśnienia,
- [ ] główne akcje są widoczne,
- [ ] karty miejsc mają spójny `CategoryBadge`,
- [ ] kliknięcie w miejsce otwiera szczegóły,
- [ ] brak internetu jest obsłużony,
- [ ] akcja lokalizacji działa dla allow, deny i trwałej odmowy.

## 4. Mapa

- [ ] mapa i markery ładują się,
- [ ] klastry działają,
- [ ] kliknięcie markera otwiera miejsce,
- [ ] zoom i przesuwanie nie powodują crasha,
- [ ] „Moja lokalizacja” działa po zgodzie,
- [ ] odmowa nie blokuje mapy,
- [ ] trwała odmowa prowadzi do ustawień aplikacji,
- [ ] wyłączony GPS prowadzi do ustawień lokalizacji,
- [ ] brak internetu jest obsłużony.

## 5. Lista miejsc

- [ ] lista ładuje się i obsługuje pusty stan,
- [ ] wyszukiwanie działa,
- [ ] sortowanie działa,
- [ ] filtry kategorii i udogodnień działają,
- [ ] sekcje filtrów mają oczekiwany stan początkowy,
- [ ] sortowanie „Od najbliższych” działa po zgodzie,
- [ ] pierwsza i kolejna odmowa są obsłużone,
- [ ] trwała odmowa prowadzi do ustawień aplikacji,
- [ ] przycisk nie staje się martwy,
- [ ] długie nazwy i adresy nie psują layoutu.

## 6. Szczegóły miejsca

- [ ] dane miejsca i zdjęcia ładują się,
- [ ] brak zdjęcia ma fallback,
- [ ] opinie są widoczne,
- [ ] zgłoszenie naruszenia działa,
- [ ] propozycja zmiany działa,
- [ ] deep link otwiera poprawne miejsce,
- [ ] brak internetu ma czytelny stan.

## 7. Dodawanie i edycja miejsca

- [ ] wymagane pola są walidowane,
- [ ] kategoria i udogodnienia zapisują się,
- [ ] pobranie lokalizacji działa,
- [ ] odmowa lokalizacji nie blokuje całego formularza,
- [ ] trwała odmowa prowadzi do ustawień,
- [ ] Photo Picker wybiera zdjęcia bez szerokiej zgody do galerii,
- [ ] kamera działa po zgodzie,
- [ ] odmowa kamery nie blokuje Photo Pickera,
- [ ] trwała odmowa kamery prowadzi do ustawień,
- [ ] duplikaty zdjęć i limity są obsłużone,
- [ ] zapis tworzy poprawne dane w Firestore i Storage.

## 8. Opinie i zdjęcia

- [ ] dodanie opinii i oceny działa,
- [ ] walidacja pustej i zbyt długiej opinii działa,
- [ ] Photo Picker działa,
- [ ] kamera działa zgodnie z uprawnieniem,
- [ ] cudzej opinii i zdjęcia nie można edytować ani usuwać,
- [ ] zgłoszenie opinii i zdjęcia działa,
- [ ] ostrzeżenie dotyczące zdjęć dzieci i osób trzecich jest widoczne, jeśli przewidziane.

## 9. Profil i konto

- [ ] profil ładuje dane,
- [ ] edycja profilu zapisuje zmiany,
- [ ] avatar używa Photo Pickera,
- [ ] prywatne pola nie są publiczne,
- [ ] moje miejsca i moje opinie działają,
- [ ] wylogowanie czyści dane sesji,
- [ ] ponowne logowanie innym kontem nie pokazuje starego cache.

## 10. Usuwanie konta

Szczegóły: `docs/legal/account-deletion-test-checklist.md`.

- [ ] konto można usunąć z aplikacji,
- [ ] wymagane ponowne uwierzytelnienie jest obsłużone,
- [ ] Auth nie pozwala na ponowne logowanie,
- [ ] dane prywatne są usunięte albo zanonimizowane,
- [ ] publiczne treści zachowują się zgodnie z polityką,
- [ ] Storage i tokeny FCM są sprawdzone,
- [ ] cache i widget nie pokazują danych po restarcie.

## 11. Ranking i odznaki

- [ ] ranking użytkowników działa,
- [ ] ranking miejsc działa,
- [ ] puste stany są obsłużone,
- [ ] kliknięcia prowadzą do poprawnych ekranów,
- [ ] użytkownik nie może modyfikować liczników administracyjnych,
- [ ] odznaki wyświetlają się poprawnie.

## 12. Powiadomienia

- [ ] allow na Androidzie 13+ działa,
- [ ] deny nie blokuje aplikacji,
- [ ] brak pętli ponownych dialogów,
- [ ] typy powiadomień otwierają poprawne ekrany,
- [ ] token FCM jest prywatny,
- [ ] logout i delete account usuwają albo unieważniają token zgodnie z projektem.

## 13. Offline i synchronizacja

- [ ] aplikacja uruchamia się bez internetu,
- [ ] cache pokazuje oczekiwane dane,
- [ ] operacje offline mają czytelny status,
- [ ] synchronizacja po powrocie internetu działa,
- [ ] brak duplikatów po retry,
- [ ] inne konto nie widzi cache poprzedniego użytkownika.

## 14. Widget

- [ ] widget pokazuje poprawne miejsca,
- [ ] działa bez lokalizacji,
- [ ] nie pokazuje prywatnych danych po logout,
- [ ] nie pokazuje danych po usunięciu konta,
- [ ] kliknięcie otwiera poprawny ekran,
- [ ] odświeżanie nie jest nadmierne.

## 15. Dokumenty i Google Play

- [ ] Privacy Policy URL działa,
- [ ] Terms URL działa,
- [ ] Account deletion URL działa,
- [ ] dokumenty są dostępne bez logowania,
- [ ] Data Safety odpowiada aplikacji,
- [ ] manifest nie zawiera `ACCESS_BACKGROUND_LOCATION`,
- [ ] manifest nie zawiera `READ_MEDIA_IMAGES` ani `READ_EXTERNAL_STORAGE`,
- [ ] grupa docelowa to rodzice i opiekunowie.

## 16. Security i obserwowalność

- [ ] użytkownik nie odczytuje ani nie edytuje cudzych danych,
- [ ] role i pola administracyjne są chronione,
- [ ] Storage Rules chronią cudze pliki,
- [ ] App Check działa w release,
- [ ] logi nie zawierają sekretów ani danych prywatnych,
- [ ] Crashlytics nie dostaje surowych danych wyjątków użytkownika,
- [ ] Analytics i Performance odpowiadają Data Safety.

## 17. Accessibility i UX

- [ ] podstawowe flow działa z TalkBack,
- [ ] duża czcionka nie ucina kluczowych akcji,
- [ ] komunikaty błędów są czytelne,
- [ ] loading, empty i offline states są zrozumiałe,
- [ ] przyciski pozostają aktywne logicznie po odmowach uprawnień.

## Wynik końcowy

```markdown
## Manual release result

- Data:
- Tester:
- Build / commit:
- Urządzenia:
- Android:
- Wynik: PASS / FAIL / BLOCKED

### Obszary
- Auth: PASS / FAIL / BLOCKED
- Start / Mapa / Lista: PASS / FAIL / BLOCKED
- Miejsca / Opinie / Zdjęcia: PASS / FAIL / BLOCKED
- Runtime permissions: PASS / FAIL / BLOCKED
- Account deletion: PASS / FAIL / BLOCKED
- Offline / Widget: PASS / FAIL / BLOCKED
- Security / Privacy: PASS / FAIL / BLOCKED

### Dowody
- linki / screenshoty / nagrania / logi:

### Follow-up issues
- brak / #...
```

Wynik `FAIL` albo `BLOCKED` wymaga follow-up issue przed GO.
