# Google Play legal review przed publikacją

Powiązane issue: #182

Milestone: v1.0.0 — publiczny release produkcyjny

Ostatnia aktualizacja: 2026-06-27

## Cel

Celem dokumentu jest zebranie w jednym miejscu kontroli prawno-produktowej przed publikacją aplikacji kidZone w Google Play.

Dokument nie zastępuje porady prawnej. Służy jako techniczno-produktowa checklista zgodności aplikacji, dokumentów publicznych i deklaracji Google Play Console.

## Status ogólny

| Obszar | Status | Uwagi |
| --- | --- | --- |
| Regulamin | Wstępnie gotowy | Wymaga finalnego przeglądu przed publikacją. |
| Polityka prywatności | Wstępnie gotowa | Wymaga sprawdzenia zgodności z faktycznym zakresem danych. |
| Usuwanie konta | Do weryfikacji w aplikacji | Dokumenty opisują możliwość usunięcia konta. Trzeba potwierdzić ekran i flow. |
| Zgody lokalizacji | Do weryfikacji w aplikacji | Trzeba sprawdzić komunikaty systemowe i uzasadnienie w UI. |
| Zgody zdjęć | Do weryfikacji w aplikacji | Trzeba sprawdzić Android Photo Picker / uprawnienia oraz komunikaty. |
| Google Play Data Safety | Roboczy draft gotowy | Ostatecznie przepisać i potwierdzić w Google Play Console. |
| Finalny przegląd prawny | Do zrobienia | Przed publikacją produkcyjną. |

## Status techniczny na 2026-06-27

Ta sekcja zbiera stan, który można potwierdzić w repozytorium bez ręcznego testu na urządzeniu ani dostępu do Google Play Console.

| Obszar | Stan | Evidence |
| --- | --- | --- |
| Regulamin | Dokument istnieje | `public/terms-of-service.html` |
| Polityka prywatności | Dokument istnieje | `public/privacy-policy.html` |
| Data Safety draft | Robocze odpowiedzi istnieją | `docs/legal/google-play-data-safety-draft.md` |
| Usuwanie konta | Checklistę testu przygotowano, wynik manualny nadal wymagany | `docs/legal/account-deletion-test-checklist.md` |
| Lokalizacja | Manifest deklaruje tylko foreground location | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`; brak `ACCESS_BACKGROUND_LOCATION` |
| Zdjęcia | Aplikacja używa Android Photo Picker w kluczowych flow | `PickVisualMedia`, `PickMultipleVisualMedia` w profilach, miejscach i opiniach |
| Kamera | Kamera jest deklarowana jako funkcja opcjonalna | `android.hardware.camera` z `android:required="false"` |
| Powiadomienia | Android 13+ permission jest deklarowane i obsługiwane | `POST_NOTIFICATIONS`, `RequestNotificationPermission` / `MainScreen` |

Elementy, których ten przegląd nie może zamknąć bez ręcznego testu:

- finalny wynik usuwania konta w aplikacji i Firebase Console,
- realne komunikaty systemowe zgód lokalizacji, aparatu i zdjęć na urządzeniu,
- finalne odpowiedzi zapisane w Google Play Console,
- finalny przegląd prawny dokumentów przez właściciela/profesjonalnego doradcę.

## 1. Regulamin

Plik:

```text
public/terms-of-service.html
```

Zakres obecnie opisany:

- operator aplikacji,
- przeznaczenie aplikacji dla osób pełnoletnich,
- wymagania techniczne,
- konto użytkownika,
- zasady zachowania,
- treści użytkowników,
- licencja na treści użytkownika,
- zdjęcia, wizerunek i bezpieczeństwo dzieci,
- blokada konta,
- zgłaszanie naruszeń,
- odznaki i rankingi,
- prywatność,
- odpowiedzialność,
- funkcje płatne,
- zmiany regulaminu,
- kontakt.

### Do sprawdzenia przed publikacją

- czy operator i kontakt są finalne,
- czy dokument jest dostępny publicznie pod stabilnym adresem,
- czy link do regulaminu działa z poziomu aplikacji i strony,
- czy opis funkcji płatnych odpowiada aktualnemu stanowi aplikacji,
- czy flow blokady konta i odwołania jest zgodne z realną obsługą użytkowników,
- czy zasady publikowania zdjęć dzieci są wystarczająco widoczne w aplikacji, nie tylko w dokumencie.

## 2. Polityka prywatności

Plik:

```text
public/privacy-policy.html
```

Zakres obecnie opisany:

- administrator danych,
- dane konta,
- dane lokalizacyjne,
- treści użytkownika,
- dane techniczne,
- dane widoczne dla innych użytkowników,
- cele przetwarzania,
- podstawy prawne RODO,
- usługi zewnętrzne: Firebase, Google Maps Platform, Google Play Services,
- przechowywanie danych,
- prawa użytkownika,
- lokalizacja,
- bezpieczeństwo,
- dzieci,
- usunięcie konta i danych,
- zgłaszanie naruszeń,
- kontakt.

### Do sprawdzenia przed publikacją

- czy lista usług Google/Firebase odpowiada faktycznie włączonym usługom,
- czy Crashlytics, Performance Monitoring, Analytics albo App Check są opisane zgodnie z realną konfiguracją,
- czy opis danych technicznych nie jest zbyt ogólny względem Google Play Data Safety,
- czy okresy retencji są realnie możliwe do spełnienia,
- czy link do polityki prywatności działa publicznie bez logowania,
- czy polityka prywatności jest podana w Google Play Console.

## 3. Usuwanie konta

Dokumenty zakładają możliwość usunięcia konta z poziomu aplikacji albo kontakt z administratorem.

### Do weryfikacji w aplikacji

- czy istnieje ekran lub akcja usunięcia konta,
- czy użytkownik dostaje jasne ostrzeżenie przed usunięciem,
- czy flow wymaga ponownego uwierzytelnienia, jeżeli Firebase tego wymaga,
- czy konto jest usuwane lub anonimizowane zgodnie z opisem w polityce prywatności,
- czy treści użytkownika po usunięciu konta są anonimizowane,
- czy zdjęcia i dane prywatne są sprzątane albo oznaczone do sprzątania,
- czy dokumentacja Google Play zawiera link lub opis procesu usuwania konta.

### Rekomendacja

Przed publikacją dodać test manualny:

```text
1. Załóż konto testowe.
2. Dodaj miejsce, opinię i zdjęcie.
3. Usuń konto.
4. Sprawdź Auth, Firestore i Storage.
5. Sprawdź, co widzą inni użytkownicy.
6. Sprawdź, czy dane identyfikujące nie są publicznie dostępne.
```

## 4. Zgody lokalizacji

Aplikacja używa lokalizacji do pokazywania miejsc w pobliżu. Polityka prywatności wskazuje, że lokalizacja jest używana tylko za zgodą użytkownika i nie jest zapisywana jako historia lokalizacji.

Status repozytorium:

- manifest deklaruje `ACCESS_FINE_LOCATION` i `ACCESS_COARSE_LOCATION`,
- manifest nie deklaruje `ACCESS_BACKGROUND_LOCATION`,
- aplikacja prosi o lokalizację przez runtime permission dopiero w flow mapy/listy/dodawania miejsca,
- przed publikacją trzeba nadal sprawdzić realne systemowe dialogi i zachowanie po odmowie uprawnienia.

### Do weryfikacji w aplikacji

- czy aplikacja prosi tylko o potrzebny zakres lokalizacji,
- czy używana jest lokalizacja przybliżona, gdy wystarczy,
- czy aplikacja działa bez zgody lokalizacji,
- czy użytkownik rozumie, po co lokalizacja jest potrzebna,
- czy nie ma zbędnego dostępu do lokalizacji w tle,
- czy Google Play Console deklaruje lokalizację zgodnie z realnym użyciem.

### Rekomendacja

Na potrzeby MVP preferować:

```text
Lokalizacja tylko podczas używania aplikacji.
Brak lokalizacji w tle.
Brak historii lokalizacji użytkownika.
```

## 5. Zgody zdjęć i pliki użytkownika

Aplikacja pozwala dodawać zdjęcia miejsc, opinii i avatarów. Regulamin zawiera zasady dotyczące zdjęć, wizerunku i bezpieczeństwa dzieci.

Status repozytorium:

- avatar używa `ActivityResultContracts.PickVisualMedia`,
- dodawanie miejsca i opinii używa `PickMultipleVisualMedia`,
- dodawanie zdjęcia w szczegółach miejsca używa `PickVisualMedia`,
- kamera jest osobnym flow przez `TakePicture` i runtime permission `CAMERA`,
- manifest deklaruje `READ_MEDIA_IMAGES`, więc przed publikacją trzeba potwierdzić, czy Photo Picker w pełni wystarcza, czy ta deklaracja jest nadal potrzebna dla wspieranych wersji Androida.

### Do weryfikacji w aplikacji

- czy aplikacja używa Android Photo Picker tam, gdzie to możliwe,
- czy nie prosi o zbyt szeroki dostęp do galerii,
- czy użytkownik widzi informację, że nie powinien publikować zdjęć dzieci bez zgód,
- czy zdjęcia można zgłosić,
- czy właściciel może usuwać własne zdjęcia,
- czy reguły Firebase Storage blokują modyfikowanie cudzych plików,
- czy polityka prywatności i Data Safety obejmują zdjęcia użytkowników.

### Status techniczny

Hardening właścicielski Firebase Storage został wykonany w #161. Przed publikacją nadal trzeba zweryfikować zachowanie UI i deklaracje Google Play.

## 6. Google Play Data Safety Form

Formularz Data Safety powinien być wypełniony na podstawie realnego działania aplikacji, nie tylko dokumentów.

Roboczy draft odpowiedzi znajduje się w:

```text
docs/legal/google-play-data-safety-draft.md
```

Draft obejmuje aktualne użycie Firebase Authentication, Firestore, Storage, Crashlytics, Analytics, Performance Monitoring, Cloud Messaging, App Check i Remote Config. Ostateczny status nadal wymaga porównania z aktywnymi usługami w Firebase Console i przepisania odpowiedzi do Google Play Console.

### Dane potencjalnie deklarowane

| Kategoria | Czy dotyczy | Uwagi |
| --- | --- | --- |
| Email | Tak | Rejestracja/logowanie, konto użytkownika. |
| Nazwa użytkownika | Tak | Profil, autorzy treści, ranking. |
| Imię i nazwisko | Tak, jeśli używane | Opcjonalne dane profilu. |
| Zdjęcia użytkownika | Tak | Zdjęcia miejsc, opinii, avatar. |
| Lokalizacja | Tak | Miejsca w pobliżu, mapa, dodawanie miejsca. |
| Treści użytkownika | Tak | Miejsca, opinie, oceny, zgłoszenia. |
| Diagnostyka/crash logs | Tak, jeśli włączone | Firebase Crashlytics / Performance / logi awarii. |
| Identyfikatory urządzenia | Możliwe | Zależnie od Firebase/Google Play Services i konfiguracji. |

### Do sprawdzenia w Google Play Console

- czy dane są zbierane,
- czy dane są udostępniane podmiotom trzecim,
- czy dane są szyfrowane w transmisji,
- czy użytkownik może zażądać usunięcia danych,
- czy zbieranie danych jest opcjonalne czy wymagane,
- w jakim celu dane są używane: funkcjonalność aplikacji, bezpieczeństwo, diagnostyka, obsługa konta, personalizacja.

## 7. Braki i ryzyka przed publikacją

| Ryzyko | Poziom | Rekomendacja |
| --- | --- | --- |
| Brak pełnej migracji prywatnych pól użytkownika do private/profile | Średnie | Kontynuować #202. |
| Nieweryfikowany flow usuwania konta | Wysokie | Sprawdzić ręcznie i udokumentować. |
| Data Safety nieuzupełniony zgodnie z faktyczną konfiguracją Firebase | Wysokie | Przygotować osobną checklistę Google Play Console. |
| Zdjęcia dzieci/wizerunek | Wysokie | Dodać widoczne ostrzeżenie w UI podczas dodawania zdjęć. |
| Lokalizacja | Średnie | Potwierdzić brak lokalizacji w tle. |
| `READ_MEDIA_IMAGES` w manifeście | Średnie | Potwierdzić, czy jest nadal potrzebne mimo Photo Pickera. |
| Retencja danych | Średnie | Sprawdzić, czy deklarowane terminy są technicznie wykonalne. |

## 8. Checklista końcowa #182

- [x] Weryfikacja regulaminu — dokument istnieje i obejmuje główne obszary.
- [x] Weryfikacja polityki prywatności — dokument istnieje i obejmuje główne obszary.
- [x] Robocza weryfikacja Google Play Data Safety — draft istnieje i obejmuje aktywne SDK/usługi.
- [ ] Weryfikacja ekranu usuwania konta — wymaga testu w aplikacji.
- [ ] Weryfikacja zgód lokalizacji — wymaga testu na urządzeniu.
- [ ] Weryfikacja zgód zdjęć — wymaga testu na urządzeniu.
- [ ] Finalne przepisanie Google Play Data Safety Form — wymaga pracy w Google Play Console.
- [ ] Finalny przegląd prawny przed publikacją — do wykonania po domknięciu powyższych punktów.

## 9. Rekomendowane dalsze issue

- `legal: verify account deletion flow before Google Play release`
- `legal: fill and confirm Google Play Data Safety in Play Console`
- `privacy: add photo upload warning about children and third-party image rights`
- `privacy: verify location permission scope and no background location usage`
- `privacy: verify whether READ_MEDIA_IMAGES is still needed with Photo Picker`

## 10. Wniosek

Regulamin i polityka prywatności są wystarczające jako baza techniczna dla alphy, ale nie są jeszcze finalnym zamknięciem prawnym przed publikacją produkcyjną.

Najważniejsze blokery przed Google Play:

1. potwierdzenie usuwania konta,
2. przygotowanie Google Play Data Safety,
3. test zgód lokalizacji i zdjęć,
4. kontynuacja migracji prywatnych pól użytkownika w #202,
5. widoczne ostrzeżenie przy dodawaniu zdjęć dzieci lub osób trzecich.
