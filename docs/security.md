# Security documentation

Powiązane issue: #166  
Parent: #159

## Cel

Ten dokument opisuje podstawowe zasady bezpieczeństwa projektu kidZone.

Dokument nie zastępuje audytu bezpieczeństwa. Jest checklistą operacyjną dla developmentu, testów i release.

## Zakres

Dokument obejmuje:

- ochronę sekretów,
- Firebase Security Rules,
- Storage Security Rules,
- App Check,
- abuse protection i rate limiting,
- audyt publicznych profili użytkowników,
- logowanie i diagnostykę,
- dane użytkownika,
- keystore,
- zależności,
- procedurę reakcji na incydent.

## Powiązane dokumenty

- `docs/app-check.md` - konfiguracja i smoke test App Check.
- `docs/abuse-rate-limiting.md` - strategia ograniczania spamu, nadużyć i kosztów.
- `docs/qa/public-user-profile-audit.md` - ręczna weryfikacja publicznych profili użytkowników.

## Zasady ogólne

- Nie commitujemy sekretów.
- Nie commitujemy keystore.
- Nie logujemy danych osobowych.
- Nie trzymamy prywatnych danych użytkownika w publicznych dokumentach Firestore.
- Nie otwieramy reguł Firebase na `allow read, write: if true`.
- Nie zakładamy, że debugowa konfiguracja jest bezpieczna dla release.
- Każda zmiana reguł Firebase powinna mieć osobny PR albo wyraźny opis w PR.

## Dane użytkownika

Dane użytkownika dzielimy na publiczne i prywatne.

### Dane publiczne

Przykłady:

- nazwa użytkownika,
- avatar, jeśli aplikacja pokazuje go publicznie,
- publiczne opinie,
- publiczne miejsca,
- publiczne oceny,
- publiczne zdjęcia miejsc.

### Dane prywatne

Przykłady:

- e-mail,
- imię i nazwisko,
- ustawienia powiadomień,
- dane administracyjne,
- tokeny FCM,
- dane związane z blokadą konta.

Docelowo prywatne dane użytkownika powinny trafiać do struktury podobnej do:

```text
users/{uid}/private/profile
users/{uid}/private/messaging
```

Dostęp do prywatnych dokumentów powinien mieć tylko właściciel konta albo administrator.

## Firebase Authentication

Zasady:

- operacje zależne od użytkownika wymagają zalogowania,
- operacje właścicielskie muszą sprawdzać `request.auth.uid`,
- operacje administracyjne muszą sprawdzać rolę admina,
- usuwanie konta wymaga osobnej procedury weryfikacji,
- nie zakładamy, że sam UID przesłany z klienta jest zaufany.

## Firestore Security Rules

Każda kolekcja powinna mieć jawne reguły.

Minimalne zasady:

- publiczne odczyty tylko tam, gdzie są potrzebne,
- zapis tylko dla zalogowanych użytkowników,
- aktualizacja tylko przez właściciela albo admina,
- pola systemowe nie mogą być dowolnie zmieniane przez klienta,
- role, ban, liczniki i statusy administracyjne nie powinny być edytowalne przez zwykłego użytkownika.

Przy zmianie reguł trzeba sprawdzić:

- [ ] kto może czytać dane,
- [ ] kto może tworzyć dane,
- [ ] kto może edytować dane,
- [ ] kto może usuwać dane,
- [ ] czy użytkownik może zmienić cudze dane,
- [ ] czy użytkownik może podnieść sobie uprawnienia,
- [ ] czy użytkownik może zmienić pola administracyjne,
- [ ] czy dane prywatne są oddzielone od publicznych.

## Firebase Storage Rules

Zasady:

- zdjęcia powinny być zapisywane w ścieżkach właścicielskich,
- właściciel może dodawać swoje pliki,
- właściciel nie powinien móc nadpisywać cudzych plików,
- legacy ścieżki powinny być tylko do odczytu albo wyłączone z zapisu,
- usuwanie zdjęć powinno być zgodne z flow usuwania konta.

Przykładowa logika ścieżek:

```text
places/{ownerUserId}/{placeId}/photos/{fileName}
reviews/{ownerUserId}/{reviewId}/photos/{fileName}
users/{ownerUserId}/avatar/{fileName}
```

## Firebase App Check

Cel App Check:

- ograniczyć nadużycia,
- utrudnić używanie Firebase spoza prawdziwej aplikacji,
- chronić Firestore, Storage i inne zasoby.

Zasady:

- debug provider tylko dla debug buildów,
- Play Integrity dla release,
- przed enforcement sprawdzić logi i kompatybilność,
- nie zostawiać debug tokenów w repo,
- dokumentować, gdzie App Check jest włączone.

Checklist:

- [ ] debug provider nie działa w release,
- [ ] release używa Play Integrity,
- [ ] debug tokeny nie są zapisane w repo,
- [ ] Firebase Console ma świadomie ustawiony monitoring albo enforcement,
- [ ] wymuszenie App Check nie blokuje prawdziwych użytkowników.

## Sekrety i konfiguracja

Sekrety nie mogą być commitowane.

Przykłady sekretów:

```text
keystore
hasła do keystore
API keys z ograniczeniami
service account JSON
GitHub tokens
Firebase admin credentials
Google Play service account
```

Dopuszczalne są tylko pliki i wartości, które są bezpieczne dla klienta mobilnego albo odpowiednio ograniczone.

## Klucze API

Klucze API używane w aplikacji mobilnej powinny być ograniczone.

Dla Google Maps API należy sprawdzić:

- [ ] ograniczenie do Android apps,
- [ ] poprawny package name,
- [ ] poprawny SHA-1 / SHA-256,
- [ ] brak niepotrzebnych API,
- [ ] limity i alerty kosztowe.

## GitHub Secrets

Sekrety do CI/CD powinny być trzymane w GitHub Secrets.

Przykłady:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
MAPS_API_KEY
GOOGLE_SERVICES_JSON_BASE64
```

Zasady:

- nie wypisywać sekretów w logach,
- nie przekazywać sekretów do pull requestów z niezaufanych branchy,
- regularnie sprawdzać listę sekretów,
- po wycieku natychmiast obrócić sekret.

## Keystore

Keystore produkcyjny jest krytycznym zasobem.

Zasady:

- nie commitować keystore,
- trzymać kopię zapasową w bezpiecznym miejscu,
- znać procedurę odzyskania albo rotacji,
- ograniczyć dostęp,
- dokumentować, kto ma dostęp,
- nie wysyłać keystore przez komunikatory bez szyfrowania.

Minimalny plan przechowywania:

```text
1. Keystore lokalnie poza repo.
2. Kopia backup w bezpiecznym menedżerze haseł albo zaszyfrowanym archiwum.
3. Hasła w menedżerze haseł.
4. W CI tylko przez GitHub Secrets.
```

## Logowanie i Crashlytics

Zasady:

- nie logować haseł,
- nie logować tokenów FCM,
- nie logować pełnych e-maili, jeśli nie jest to potrzebne,
- nie logować danych prywatnych profilu,
- nie logować dokładnej lokalizacji użytkownika,
- w release ograniczać logi do ostrzeżeń i błędów,
- Crashlytics może zbierać dane diagnostyczne i musi być uwzględnione w Data Safety.

## Analytics i Performance Monitoring

Jeśli Analytics zostaje w aplikacji, Data Safety musi to uwzględniać.

Jeśli Performance Monitoring zostaje w aplikacji, Data Safety musi uwzględniać:

- diagnostics,
- performance data,
- device/app info.

Zasady:

- nie wysyłać danych osobowych jako event parameters,
- nie wysyłać e-maili w eventach,
- nie wysyłać pełnych adresów ani dokładnej lokalizacji jako eventów,
- custom eventy powinny mieć neutralne nazwy,
- koszt i limity Performance Monitoring trzeba potwierdzić przed release.

## Firebase Cloud Messaging

Jeżeli aplikacja używa FCM:

- tokeny FCM traktujemy jako dane techniczne powiązane z użytkownikiem,
- tokeny zapisujemy w prywatnej części profilu,
- tokeny nie powinny być publicznie odczytywalne,
- użytkownik powinien mieć kontrolę nad powiadomieniami,
- Android 13+ wymaga `POST_NOTIFICATIONS`.

Checklist:

- [ ] tokeny FCM są zapisane w prywatnym dokumencie,
- [ ] tokeny nie są logowane,
- [ ] tokeny są usuwane albo dezaktywowane przy wylogowaniu/usunięciu konta,
- [ ] aplikacja działa po odmowie powiadomień,
- [ ] Data Safety uwzględnia FCM, jeśli funkcja zostaje.

## Zależności

Przed release trzeba sprawdzać zależności.

Minimalna procedura:

```powershell
.\gradlew dependencyUpdates
.\gradlew detekt
.\gradlew lint
```

Jeżeli projekt ma OWASP Dependency Check:

```powershell
.\gradlew dependencyCheckAnalyze
```

Zasady:

- nie aktualizować wielu krytycznych bibliotek naraz bez testów,
- priorytetowo traktować podatności security,
- po aktualizacji Firebase sprawdzić Data Safety,
- po aktualizacji Compose/Android Gradle Plugin wykonać smoke test UI.

## Backup i odzyskiwanie

Elementy wymagające backupu:

- keystore,
- hasła keystore,
- konfiguracja Firebase,
- GitHub Secrets lista nazw,
- dokumentacja release,
- polityka prywatności i regulamin.

Nie backupujemy sekretów w plain text.

## Incydent bezpieczeństwa

Przykłady incydentów:

- wyciek keystore,
- wyciek service account,
- publiczne reguły Firebase,
- możliwość odczytu prywatnych danych użytkownika,
- nieautoryzowany zapis do Firestore/Storage,
- błędna publikacja danych osobowych,
- kosztowy abuse Firebase/GCP.

Procedura:

1. Zatrzymać dalsze wdrożenia.
2. Zidentyfikować zakres problemu.
3. Jeżeli wyciekł sekret — obrócić sekret.
4. Jeżeli problem dotyczy Firebase Rules — wdrożyć poprawione reguły.
5. Sprawdzić logi Firebase/GCP.
6. Ocenić, czy trzeba poinformować użytkowników.
7. Utworzyć issue z opisem incydentu.
8. Dodać test albo checklistę, która zapobiegnie powtórce.

## Czego nie robić

- Nie używać `allow read, write: if true` poza lokalnym emulatorem.
- Nie trzymać haseł w repo.
- Nie dodawać service account JSON do aplikacji mobilnej.
- Nie używać admin SDK w aplikacji Android.
- Nie ufać danym przesłanym przez klienta bez sprawdzenia reguł.
- Nie publikować release bez aktualnej polityki prywatności.
- Nie zostawiać debugowych obejść w release.

## Security checklist dla PR

Przy PR dotykającym danych, Firebase albo uprawnień sprawdzić:

- [ ] czy zmiana dotyka danych użytkownika,
- [ ] czy zmiana wymaga aktualizacji Firebase Rules,
- [ ] czy zmiana wymaga aktualizacji Storage Rules,
- [ ] czy zmiana wpływa na Data Safety,
- [ ] czy zmiana dodaje nowe uprawnienie Androida,
- [ ] czy zmiana dodaje nową usługę Firebase,
- [ ] czy zmiana loguje dane prywatne,
- [ ] czy zmiana wymaga migracji danych,
- [ ] czy zmiana wymaga aktualizacji polityki prywatności.

## Kryteria bezpieczeństwa przed release

Release można traktować jako gotowy bezpieczeństwowo, gdy:

- Firebase Rules są wdrożone,
- Storage Rules są wdrożone,
- App Check jest świadomie skonfigurowany,
- keystore jest bezpiecznie przechowywany,
- GitHub Secrets są aktualne,
- Data Safety jest zgodne z kodem,
- logi release nie ujawniają danych prywatnych,
- użytkownik może usunąć konto albo ma jasną procedurę żądania usunięcia danych,
- nie ma znanych krytycznych podatności w zależnościach,
- [Google Play security release checklist](qa/google-play-security-checklist.md) ma uzupełnione statusy i decyzję GO / NO-GO.
