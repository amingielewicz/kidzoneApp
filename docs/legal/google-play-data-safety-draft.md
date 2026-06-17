# Google Play Data Safety — robocze odpowiedzi

Powiązane issue: #213  
Powiązane issue pomocnicze: #216  
Parent: #182

Milestone: v0.1.0-alpha — pierwszy release techniczny

## Cel

Dokument zbiera robocze odpowiedzi do formularza **Google Play Data Safety** dla aplikacji kidZone.

To jest materiał pomocniczy do przepisania w Google Play Console. Ostateczne odpowiedzi trzeba potwierdzić z faktyczną konfiguracją Firebase, Google Play Console i aktualnym buildem aplikacji.

## Aktualne decyzje Firebase SDK

Stan audytu #216: zweryfikowano lokalny kod aplikacji, Cloud Functions,
`firebase.json`, `firestore.rules`, manifest Androida i zależności Gradle.
Data weryfikacji: 2026-06-17.

| SDK / usługa | Decyzja | Wpływ na Google Play Data Safety |
| --- | --- | --- |
| Firebase Authentication | Zostaje | Dane konta, e-mail, logowanie, account management. |
| Cloud Firestore | Zostaje | Profil, treści użytkownika, miejsca, opinie, zgłoszenia. |
| Firebase Storage | Zostaje | Zdjęcia miejsc, opinii i avatarów. |
| Firebase Crashlytics | Zostaje | Crash logs, diagnostics, device/app info. |
| Firebase Analytics | Zostaje | App activity, app interactions, search, screen views, login/sign-up events, user properties, user ID, device or other IDs. |
| Firebase Performance Monitoring | Zostaje | Performance data, diagnostics, device/app info. Custom traces są użyte w kodzie; Gradle plugin `com.google.firebase.firebase-perf` jest podpięty; debug/CI wyłącza kolekcję przez debug manifest, release nie ma tego wyłączenia. |
| Firebase Cloud Messaging | Zostaje | Push notifications, FCM registration tokeny, device or other IDs. Tokeny są zapisywane w `users/{uid}/private/messaging`, a Cloud Functions wysyłają push przez Admin SDK. |
| Firebase App Check | Zostaje | Security, fraud prevention, device/app integrity. Debug używa Debug provider fallback, release używa Play Integrity. |
| Firebase Remote Config | Zostaje | App functionality / configuration, eksperymenty i warianty UX; używa Analytics do exposure/user properties. |

## Ważne założenia

Na podstawie aktualnych dokumentów i funkcji aplikacji zakładamy, że kidZone:

- pozwala zakładać konto użytkownika,
- używa Firebase Authentication,
- przechowuje dane w Cloud Firestore,
- przechowuje zdjęcia w Firebase Storage,
- używa Google Maps Platform,
- używa Firebase Crashlytics,
- używa Firebase Analytics,
- używa Firebase Performance Monitoring przez SDK, Gradle plugin i custom traces w release,
- używa Firebase Cloud Messaging do push notifications,
- zapisuje FCM tokeny w prywatnym subdokumencie `users/{uid}/private/messaging`,
- pozwala dodawać miejsca, opinie, oceny, zdjęcia i zgłoszenia,
- korzysta z lokalizacji do pokazywania miejsc w pobliżu,
- nie jest aplikacją kierowaną bezpośrednio do dzieci.

## Status formularza

| Obszar | Status | Komentarz |
| --- | --- | --- |
| Dane konta | Do zadeklarowania | Email, nazwa użytkownika, opcjonalnie imię i nazwisko. |
| Lokalizacja | Do zadeklarowania | Używana do mapy i miejsc w pobliżu. |
| Zdjęcia | Do zadeklarowania | Użytkownik może przesyłać zdjęcia miejsc, opinii i avatar. |
| Treści użytkownika | Do zadeklarowania | Miejsca, opinie, oceny, zgłoszenia. |
| Crash logs | Do zadeklarowania | Crashlytics zostaje. |
| Diagnostics | Do zadeklarowania | Crashlytics i Performance Monitoring. |
| Analytics / app activity | Do zadeklarowania | Analytics zostaje. |
| Performance data | Do zadeklarowania | Performance Monitoring zostaje; custom traces są użyte w kodzie, release nie wyłącza SDK. |
| Device or other IDs | Do zadeklarowania | Firebase/Google Play Services/Analytics/Crashlytics/FCM mogą używać identyfikatorów. |
| Push notifications / FCM | Do zadeklarowania | FCM zostaje; tokeny są zapisywane prywatnie, Cloud Functions wysyłają powiadomienia, test FCM z Firebase Console przeszedł. |
| Usuwanie danych | Do potwierdzenia | Powiązane z #212. |

## 1. Czy aplikacja zbiera dane użytkownika?

Rekomendowana odpowiedź:

```text
Tak.
```

Uzasadnienie:

Aplikacja obsługuje konto użytkownika, treści użytkownika, zdjęcia, lokalizację, diagnostykę i analitykę.

## 2. Czy wszystkie dane są szyfrowane podczas przesyłania?

Rekomendowana odpowiedź:

```text
Tak.
```

Uzasadnienie:

Firebase, Google APIs i Google Play Services komunikują się przez HTTPS/TLS. Trzeba jednak potwierdzić, że aplikacja nie wysyła żadnych danych do własnych endpointów poza HTTPS.

## 3. Czy użytkownik może zażądać usunięcia danych?

Rekomendowana odpowiedź robocza:

```text
Tak, ale wymaga potwierdzenia flow w aplikacji.
```

Powiązane zadanie:

```text
#212 legal: verify account deletion flow before Google Play release
```

Do potwierdzenia:

- czy usuwanie konta działa z poziomu aplikacji,
- czy istnieje publiczna procedura kontaktu mailowego,
- czy dane prywatne są usuwane lub anonimizowane,
- czy publiczne treści użytkownika są anonimizowane,
- czy zdjęcia użytkownika są usuwane albo pozostają jako treści zanonimizowane.

## 4. Kategorie danych

### 4.1 Dane osobowe — nazwa użytkownika

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak |
| Czy udostępniane? | Nie jako sprzedaż danych; widoczne publicznie w aplikacji jako element profilu/treści |
| Czy wymagane? | Tak dla konta/profilu publicznego |
| Cel | Funkcjonalność aplikacji, konto użytkownika, społeczność, ranking |
| Widoczność | Może być widoczne dla innych użytkowników |

Nazwa użytkownika jest publicznym identyfikatorem autora treści, opinii, miejsc lub rankingu.

### 4.2 Dane osobowe — adres e-mail

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak |
| Czy udostępniane? | Przetwarzane przez Firebase/Google jako dostawcę usług; niepubliczne dla innych użytkowników |
| Czy wymagane? | Tak, jeśli użytkownik loguje się e-mailem; możliwe także przy logowaniu Google |
| Cel | Uwierzytelnianie, obsługa konta, bezpieczeństwo, kontakt administracyjny |
| Widoczność | Prywatne |

Adres e-mail powinien znajdować się w prywatnym profilu użytkownika. Migracja prywatnych pól jest śledzona w #202.

### 4.3 Dane osobowe — imię i nazwisko

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak, jeśli pola są aktywne w profilu |
| Czy udostępniane? | Nie powinny być publiczne, jeśli są prywatną częścią profilu |
| Czy wymagane? | Opcjonalne |
| Cel | Profil użytkownika, obsługa konta |
| Widoczność | Prywatne albo zależne od UI — wymaga potwierdzenia |

Jeżeli imię i nazwisko nie są wymagane do działania aplikacji, traktować jako opcjonalne dane profilu. Docelowo powinny trafić do `users/{uid}/private/profile`.

### 4.4 Zdjęcia i pliki użytkownika

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak |
| Czy udostępniane? | Zdjęcia miejsc/opinii mogą być publicznie widoczne w aplikacji; technicznie przechowywane w Firebase Storage |
| Czy wymagane? | Opcjonalne |
| Cel | Funkcjonalność aplikacji, treści użytkownika, profil, miejsca, opinie |
| Widoczność | Publiczna lub prywatna zależnie od typu zdjęcia |

Do potwierdzenia:

- czy aplikacja używa Android Photo Picker,
- czy wymaga szerokiego dostępu do galerii,
- czy zdjęcia avatarów są publiczne czy tylko dla zalogowanych,
- czy użytkownik może usuwać własne zdjęcia,
- czy istnieje ostrzeżenie dotyczące zdjęć dzieci i osób trzecich.

### 4.5 Lokalizacja

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak |
| Czy udostępniane? | Nie jako lokalizacja użytkownika; Google Maps/Google Services mogą przetwarzać dane technicznie |
| Czy wymagane? | Opcjonalne dla funkcji mapy/miejsc w pobliżu |
| Cel | Funkcjonalność aplikacji, mapa, miejsca w pobliżu, dodawanie miejsca |
| Widoczność | Lokalizacja użytkownika nie powinna być publiczna |

Rekomendowana deklaracja:

```text
Lokalizacja tylko podczas używania aplikacji.
Brak lokalizacji w tle.
Brak historii lokalizacji użytkownika.
```

Do potwierdzenia w kodzie i manifestach:

- `ACCESS_FINE_LOCATION`,
- `ACCESS_COARSE_LOCATION`,
- brak `ACCESS_BACKGROUND_LOCATION`,
- brak zapisywania historii lokalizacji użytkownika.

### 4.6 Treści użytkownika

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak |
| Czy udostępniane? | Tak, treści mogą być publiczne w aplikacji |
| Czy wymagane? | Opcjonalne |
| Cel | Funkcjonalność aplikacji, społeczność, moderacja |
| Widoczność | Publiczna lub administracyjna zależnie od typu treści |

Przykłady:

- dodane miejsca,
- opisy miejsc,
- opinie,
- oceny,
- zdjęcia,
- zgłoszenia naruszeń,
- zgłoszenia błędnych danych.

### 4.7 Dane diagnostyczne, crash logs i performance data

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak |
| Czy udostępniane? | Przetwarzane przez Firebase/Google jako dostawcę usług |
| Czy wymagane? | Zależne od konfiguracji; w release może działać automatycznie |
| Cel | Diagnostyka, stabilność, bezpieczeństwo, analiza awarii, poprawa wydajności |
| Widoczność | Niepubliczne |

Decyzje:

- Crashlytics zostaje, więc `Crash logs` i `Diagnostics` trzeba uwzględnić.
- Analytics zostaje, więc app activity / analytics trzeba uwzględnić.
- Performance Monitoring zostaje, więc `Performance data` trzeba uwzględnić.

Wynik audytu Performance Monitoring:

- dependency `firebase-perf` jest podpięte w `app/build.gradle.kts`,
- Gradle plugin `com.google.firebase.firebase-perf` jest podpięty w top-level i app-level Gradle,
- `PerformanceTraces` używa `FirebasePerformance.getInstance()` i custom traces,
- custom traces są używane m.in. dla lokalizacji, kompresji zdjęć, uploadu zdjęć, Remote Config i innych ścieżek,
- debug/CI wyłącza kolekcję przez debug manifest metadata `firebase_performance_collection_deactivated=true` i `firebase_performance_collection_enabled=false`,
- release build nie ustawia tego wyłączenia,
- plugin Gradle włącza automatyczną instrumentację buildów release obok ręcznych custom traces,
- według oficjalnej strony Firebase Pricing usługa Performance Monitoring jest no-cost.

Do potwierdzenia:

- czy Crashlytics jest włączony w buildzie release,
- czy w Firebase Console widać custom traces z release po publikacji,
- czy użytkownik jest informowany o diagnostyce i analityce w polityce prywatności.

### 4.8 Identyfikatory urządzenia lub instalacji

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak |
| Czy udostępniane? | Przetwarzane przez Firebase/Google Play Services |
| Czy wymagane? | Zależne od Firebase/Google Play Services |
| Cel | Bezpieczeństwo, diagnostyka, analityka, działanie usług, powiadomienia, antynadużycia |
| Widoczność | Niepubliczne |

Potwierdzone lub prawdopodobne identyfikatory techniczne:

- Firebase Installation ID,
- Crashlytics installation UUID,
- Analytics app instance ID,
- tokeny FCM,
- App Check tokeny,
- Google Play Services identifiers.

### 4.9 Push notifications / Firebase Cloud Messaging

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak |
| Czy udostępniane? | Przetwarzane przez Firebase/Google jako dostawcę usług |
| Czy wymagane? | Opcjonalne dla użytkownika; wymagane technicznie do push notifications |
| Cel | Powiadomienia, funkcjonalność aplikacji, komunikaty systemowe lub społecznościowe |
| Widoczność | Niepubliczne tokeny urządzenia; treść powiadomienia zależna od funkcji |

Wynik audytu FCM:

- dependency `firebase-messaging` jest podpięte,
- `KidZoneMessagingService` obsługuje `onNewToken()` i `onMessageReceived()`,
- tokeny są zapisywane w `users/{uid}/private/messaging.fcmTokens`,
- `FirebaseAuthRepository.signOut()` usuwa aktualny token z prywatnego subdokumentu,
- `ensurePrivateProfile()` migruje legacy `users/{uid}.fcmTokens` do `private/messaging`,
- Cloud Functions używają `admin.messaging().sendEachForMulticast(...)`,
- `getFcmTokens()` czyta `private/messaging` jako źródło podstawowe i ma legacy fallback,
- `cleanStaleTokens()` usuwa nieważne tokeny z `private/messaging`,
- manifest ma `POST_NOTIFICATIONS`,
- UI prosi o zgodę na powiadomienia na Androidzie 13+,
- `NotificationPrefsUseCase` pozwala użytkownikowi wyłączyć typy powiadomień,
- test ręczny z Firebase Console wysłał powiadomienie na token i zostało odebrane w APK.

Decyzja: FCM zostaje i trzeba uwzględnić push notifications oraz device/installation IDs w Data Safety.

### 4.10 Firebase Analytics, Remote Config i App Check

Firebase Analytics zostaje i jest realnie używany w kodzie:

- `AnalyticsHelper` loguje `screen_view`, `login`, `sign_up`, `search` oraz eventy własne: `add_place`, `view_place`, `delete_place`, `report_place`, `add_review`, `delete_review`, `add_photo`, `onboarding_started`, `onboarding_completed`, `onboarding_skipped`, `map_interaction`,
- `AnalyticsHelper` ustawia `setUserId(uid)` i user properties,
- `ExperimentManager` loguje `experiment_exposure` i ustawia user properties dla wariantów eksperymentów,
- Firebase Analytics jest dostarczany przez Hilt w `FirebaseModule`.

W Data Safety trzeba uwzględnić:

- app interactions / app activity,
- search history albo search interactions, jeśli formularz wymaga tej kategorii dla `search`,
- device or other IDs,
- analytics jako cel przetwarzania.

Remote Config zostaje:

- `RemoteConfigService` jest inicjalizowany przy starcie aplikacji,
- `ExperimentManager` używa Remote Config do wariantów eksperymentów,
- sama konfiguracja nie wygląda na osobną kategorię danych użytkownika, ale jest powiązana z app functionality i eksperymentami.

App Check zostaje:

- debug build próbuje użyć Debug provider, a release używa Play Integrity,
- w Data Safety traktować jako security / fraud prevention / app integrity,
- sprawdzić w Firebase Console enforcement przed finalnym release.

### 4.11 Ślad audytu #216

Sprawdzone pliki i wnioski:

| Plik / obszar | Wniosek |
| --- | --- |
| `gradle/libs.versions.toml` | Zdefiniowane są Firebase Auth, Firestore, Storage, Crashlytics, Messaging, Analytics, App Check, Performance i Remote Config. |
| `app/build.gradle.kts` | Wszystkie powyższe SDK są podpięte; app-level Gradle stosuje Google Services, Crashlytics i Firebase Performance plugin; debug wyłącza Firebase Performance collection przez debug manifest, release nie. |
| top-level `build.gradle.kts` | Zastosowane są Google Services, Crashlytics i Firebase Performance plugin. |
| `AndroidManifest.xml` | Są uprawnienia lokalizacji, zdjęć, kamery i `POST_NOTIFICATIONS`; jest zarejestrowany `KidZoneMessagingService`. |
| `KidZoneMessagingService.kt` | FCM token jest zapisywany, wiadomości są odbierane, tworzone są lokalne powiadomienia i deep linki. |
| `FirebaseAuthRepository.kt` | Sign-out usuwa token z `private/messaging`; login migruje legacy publiczne `fcmTokens`. |
| `NotificationPrefsUseCase.kt` | Użytkownik ma preferencje powiadomień push/email. |
| `functions/src/index.ts` | Cloud Functions czytają tokeny z `private/messaging`, migrują fallback legacy i wysyłają push przez `sendEachForMulticast`. |
| `firestore.rules` | Publiczne `fcmTokens` są blokowane; prywatny subdokument `private/messaging` jest dostępny dla właściciela/admina. |
| `AnalyticsHelper.kt` i `ExperimentManager.kt` | Analytics jest realnie używane do screenów, auth, search, interakcji, eksperymentów, user ID i user properties. |
| `PerformanceTraces.kt` i użycia w serwisach | Performance Monitoring jest realnie użyty przez custom traces, a Gradle plugin dodaje automatyczną instrumentację release. |
| `RemoteConfigService.kt` | Remote Config jest inicjalizowany i fetchowany przy starcie aplikacji. |
| Oficjalna Firebase Pricing page | Analytics, FCM, Crashlytics, Performance Monitoring i Remote Config są produktami no-cost; Firestore/Functions/Storage mają limity no-cost i potem billing zależny od użycia. |

## 5. Udostępnianie danych

Robocza interpretacja:

```text
Dane nie są sprzedawane.
Dane są przetwarzane przez Google/Firebase jako dostawców usług technicznych.
Część treści użytkownika jest publicznie widoczna w aplikacji, bo taka jest funkcja aplikacji.
```

Do potwierdzenia w Google Play Console:

- czy Google/Firebase należy oznaczyć jako „shared” w konkretnych pytaniach formularza,
- czy publiczne treści użytkownika traktować jako udostępniane innym użytkownikom,
- czy Crashlytics / Performance / Analytics wpływa na deklarację udostępniania,
- czy FCM wpływa na deklarację udostępniania.

## 6. Cele przetwarzania danych

Prawdopodobne cele do zaznaczenia:

| Cel | Dane |
| --- | --- |
| App functionality | konto, profil, miejsca, opinie, zdjęcia, lokalizacja, powiadomienia |
| Analytics | Analytics app events, app interactions, device/app info |
| Diagnostics | crash logs, performance data, diagnostics |
| Developer communications | e-mail, zgłoszenia, obsługa konta |
| Fraud prevention, security, compliance | e-mail, identyfikatory, logi, App Check, reguły Firebase |
| Personalization | potencjalnie lokalizacja i treści, jeśli aplikacja personalizuje listy/mapę |
| Account management | e-mail, profil, login, usuwanie konta |

## 7. Dane wymagane i opcjonalne

| Dane | Wymagane? | Uwagi |
| --- | --- | --- |
| E-mail | Tak dla konta | Wymagany przy rejestracji/logowaniu e-mail. |
| Nazwa użytkownika | Tak lub częściowo wymagane | Potrzebna do profilu publicznego. |
| Imię i nazwisko | Opcjonalne | Nie powinno być wymagane w MVP. |
| Zdjęcia | Opcjonalne | Funkcja dodatkowa. |
| Lokalizacja | Opcjonalna | Aplikacja powinna działać bez zgody, ale bez funkcji „w pobliżu”. |
| Opinie i miejsca | Opcjonalne | Użytkownik może korzystać tylko z przeglądania. |
| Crash logs / diagnostics | Zależne od konfiguracji | Crashlytics zostaje. |
| Analytics | Zależne od konfiguracji | Analytics zostaje. |
| Performance data | Zależne od konfiguracji | Performance Monitoring zostaje; usługa jest no-cost według Firebase Pricing. |
| FCM tokeny | Opcjonalne dla użytkownika, wymagane dla push notifications | FCM zostaje; tokeny są prywatne i służą do powiadomień. |

## 8. Elementy do ręcznego potwierdzenia

Przed finalnym wypełnieniem Data Safety trzeba sprawdzić:

- [ ] aktywne usługi Firebase w konsoli,
- [ ] czy Crashlytics jest aktywny w release,
- [x] Performance Monitoring jest użyty w kodzie przez custom traces, Gradle plugin jest podpięty i całość pozostaje w release,
- [x] Performance Monitoring jest no-cost według Firebase Pricing,
- [x] eventy Analytics są zidentyfikowane w `AnalyticsHelper` i `ExperimentManager`,
- [x] FCM/powiadomienia są używane,
- [x] aplikacja zapisuje tokeny FCM w `users/{uid}/private/messaging`,
- [ ] czy App Check jest aktywny i w jakim trybie,
- [ ] manifest Androida pod kątem uprawnień lokalizacji, zdjęć i powiadomień,
- [ ] realne flow usuwania konta,
- [ ] realne flow eksportu/usunięcia danych na żądanie,
- [ ] czy publiczne treści użytkownika są anonimizowane po usunięciu konta.

## 9. Robocza odpowiedź do Google Play Console

### Czy aplikacja zbiera dane?

```text
Tak.
```

### Czy dane są szyfrowane podczas przesyłania?

```text
Tak.
```

### Czy użytkownik może poprosić o usunięcie danych?

```text
Tak — do potwierdzenia po zakończeniu #212.
```

### Kategorie do zaznaczenia roboczo

```text
Personal info:
- Email address
- Name / username

Location:
- Approximate location
- Precise location, jeśli aplikacja używa dokładnej lokalizacji

Photos and videos:
- Photos

App activity:
- User-generated content
- App interactions
- Other user-generated content / app activity, jeśli formularz tego wymaga

App info and performance:
- Crash logs
- Diagnostics
- Performance data

Device or other IDs:
- Device or other IDs

Messages / notifications:
- Push notifications / notification tokens, ponieważ FCM zostaje i aplikacja faktycznie obsługuje powiadomienia
```

## 10. Rekomendacje przed publikacją

- Domknąć #212, zanim formularz zostanie oznaczony jako finalny.
- #216: decyzje SDK są zweryfikowane w tym dokumencie; przed finalnym wysłaniem formularza zostaje ręczne porównanie z aktywnymi usługami w Firebase Console.
- Zweryfikować manifest i uprawnienia Androida.
- Zweryfikować aktywne usługi Firebase.
- Uwzględnić Analytics w Data Safety, ponieważ zostaje w aplikacji.
- Uwzględnić Crashlytics w Data Safety, ponieważ zostaje w aplikacji.
- Uwzględnić Performance Monitoring, ponieważ SDK, Gradle plugin i custom traces zostają w aplikacji.
- Uwzględnić FCM/push notifications, ponieważ tokeny i wysyłka powiadomień są realnie używane.
- Nie oznaczać danych jako opcjonalnych, jeśli konto jest wymagane do korzystania z kluczowych funkcji.
- Dodać widoczne ostrzeżenie w UI przed publikacją zdjęć dzieci lub osób trzecich.

## 11. Wniosek

Aplikacja powinna być deklarowana jako aplikacja zbierająca dane użytkownika, w szczególności dane konta, treści użytkownika, zdjęcia, lokalizację, diagnostykę i analitykę.

Największe ryzyka przed publikacją:

1. brak potwierdzonego flow usuwania konta,
2. ręczne niedopasowanie finalnych odpowiedzi w Google Play Console do aktywnych usług Firebase,
3. zdjęcia dzieci i osób trzecich,
4. lokalizacja dokładna versus przybliżona,
5. brak jasnej komunikacji w polityce prywatności o Analytics, Crashlytics, Performance Monitoring i FCM.
