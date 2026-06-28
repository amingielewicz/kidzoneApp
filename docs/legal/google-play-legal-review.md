# Google Play legal review przed publikacją

Powiązane issue: #182

Milestone: v1.0.0 — publiczny release produkcyjny

Ostatnia aktualizacja: 2026-06-28

## Cel

Celem dokumentu jest zebranie w jednym miejscu kontroli prawno-produktowej przed publikacją aplikacji kidZone w Google Play.

Dokument nie zastępuje porady prawnej. Służy jako techniczno-produktowa checklista zgodności aplikacji, dokumentów publicznych i deklaracji Google Play Console.

## Status ogólny

| Obszar | Status | Uwagi |
| --- | --- | --- |
| Regulamin | Gotowy w repo | Wymaga finalnego przeglądu publicznego URL po deployu hostingu. |
| Polityka prywatności | Gotowa w repo | Zsynchronizowana z aktualnymi Firebase/Google SDK w Gradle. |
| Usuwanie konta | Gotowe w repo, QA ręczne wymagane | Istnieje publiczna strona `/account-deletion` i akcja w Profilu; wynik flow trzeba potwierdzić na urządzeniu. |
| Zgody lokalizacji | Gotowe w repo, QA ręczne wymagane | Manifest nie ma background location; aplikacja pokazuje rationale przed dialogiem systemowym. |
| Zgody zdjęć | Gotowe w repo, QA ręczne wymagane | Aplikacja używa Photo Picker w głównych flow; `READ_MEDIA_IMAGES` wymaga finalnej decyzji przed publikacją. |
| Google Play Data Safety | Draft gotowy do przepisania | Ostatecznie przepisać i potwierdzić w Google Play Console. |
| Finalny przegląd prawny | Do zrobienia | Przed publikacją produkcyjną. |

## Status techniczny na 2026-06-27

Ta sekcja zbiera stan, który można potwierdzić w repozytorium bez ręcznego testu na urządzeniu ani dostępu do Google Play Console.

| Obszar | Stan | Evidence |
| --- | --- | --- |
| Regulamin | Dokument istnieje | `public/terms-of-service.html` |
| Polityka prywatności | Dokument istnieje | `public/privacy-policy.html` |
| Usuwanie konta | Publiczna strona istnieje | `public/account-deletion.html`, rewrite `/account-deletion` |
| Data Safety draft | Robocze odpowiedzi istnieją | `docs/legal/google-play-data-safety-draft.md` |
| Usuwanie konta QA | Checklistę testu przygotowano, wynik manualny nadal wymagany | `docs/legal/account-deletion-test-checklist.md` |
| Android permissions | Audyt przygotowano, wynik manualny nadal wymagany | `docs/legal/android-permissions-play-compliance.md` |
| Lokalizacja | Manifest deklaruje tylko foreground location | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`; brak `ACCESS_BACKGROUND_LOCATION` |
| Zdjęcia | Aplikacja używa Android Photo Picker w kluczowych flow | `PickVisualMedia`, `PickMultipleVisualMedia` w profilach, miejscach i opiniach |
| Kamera | Kamera jest deklarowana jako funkcja opcjonalna | `android.hardware.camera` z `android:required="false"` |
| Powiadomienia | Android 13+ permission jest deklarowane i obsługiwane | `POST_NOTIFICATIONS`, `RequestNotificationPermission` / `MainScreen` |

Elementy, których ten przegląd nie może zamknąć bez ręcznego testu:

- finalny wynik usuwania konta w aplikacji i Firebase Console,
- publiczny URL `/account-deletion` po deployu Firebase Hosting,
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

Dokumenty i aplikacja zapewniają dwie ścieżki:

- aplikacja: `Profil → Konto i bezpieczeństwo → Usuń konto`,
- publiczna strona: `/account-deletion` oraz `/account-deletion.html`.

Strona publiczna opisuje kontakt e-mail, zakres usuwanych/anonimizowanych danych, treści publiczne, zdjęcia oraz termin 30 dni.

### Do weryfikacji w aplikacji

- czy istnieje ekran lub akcja usunięcia konta,
- czy użytkownik dostaje jasne ostrzeżenie przed usunięciem,
- czy flow wymaga ponownego uwierzytelnienia, jeżeli Firebase tego wymaga,
- czy konto jest usuwane lub anonimizowane zgodnie z opisem w polityce prywatności,
- czy treści użytkownika po usunięciu konta są anonimizowane,
- czy zdjęcia i dane prywatne są sprzątane albo oznaczone do sprzątania,
- czy dokumentacja Google Play zawiera link lub opis procesu usuwania konta.

### Linki do Google Play

Po deployu hostingu do Google Play Console wpisać:

```text
Privacy Policy URL: https://playground-705e7162.web.app/privacy-policy
Account deletion URL: https://playground-705e7162.web.app/account-deletion
Terms URL: https://playground-705e7162.web.app/terms-of-service
```

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

Szczegółowy audyt Android permissions znajduje się w:

```text
docs/legal/android-permissions-play-compliance.md
```

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

| Ryzyko | Status | Decyzja |
| --- | --- | --- |
| Play Console nieuzupełnione | Otwarte | Przepisać `docs/legal/google-play-data-safety-draft.md` do Google Play Console. |
| Account deletion nieprzetestowane ręcznie | Otwarte | Wykonać `docs/legal/account-deletion-test-checklist.md` i dopisać wynik w issue #210. |
| Publiczne URL-e po deployu | Otwarte | Po deployu sprawdzić `/privacy-policy`, `/terms-of-service`, `/account-deletion`. |
| Finalny przegląd prawny | Otwarte | Właściciel musi potwierdzić treść przed produkcyjną publikacją. |

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
