# Google Play Data Safety — robocze odpowiedzi

Powiązane issue: #213

Parent: #182

Milestone: v0.1.0-alpha — pierwszy release techniczny

## Cel

Dokument zbiera robocze odpowiedzi do formularza **Google Play Data Safety** dla aplikacji kidZone.

To jest materiał pomocniczy do przepisania w Google Play Console. Ostateczne odpowiedzi trzeba potwierdzić z faktyczną konfiguracją Firebase, Google Play Console i aktualnym buildem aplikacji.

## Ważne założenia

Na podstawie aktualnych dokumentów i funkcji aplikacji zakładamy, że kidZone:

- pozwala zakładać konto użytkownika,
- używa Firebase Authentication,
- przechowuje dane w Cloud Firestore,
- przechowuje zdjęcia w Firebase Storage,
- używa Google Maps Platform,
- może używać Firebase Crashlytics / Performance Monitoring / Google Play Services,
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
| Diagnostyka | Do potwierdzenia | Zależnie od aktywnej konfiguracji Crashlytics/Performance. |
| Identyfikatory | Do potwierdzenia | Możliwe przez Firebase/Google Play Services. |
| Udostępnianie danych | Do potwierdzenia | Dane przetwarzane przez Google jako dostawcę usług. |
| Usuwanie danych | Do potwierdzenia | Powiązane z #212. |

## 1. Czy aplikacja zbiera dane użytkownika?

Rekomendowana odpowiedź:

```text
Tak.
```

Uzasadnienie:

Aplikacja obsługuje konto użytkownika, treści użytkownika, zdjęcia, lokalizację i dane techniczne.

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

Uwagi:

Nazwa użytkownika jest publicznym identyfikatorem autora treści, opinii, miejsc lub rankingu.

### 4.2 Dane osobowe — adres e-mail

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak |
| Czy udostępniane? | Przetwarzane przez Firebase/Google jako dostawcę usług; niepubliczne dla innych użytkowników |
| Czy wymagane? | Tak, jeśli użytkownik loguje się e-mailem; możliwe także przy logowaniu Google |
| Cel | Uwierzytelnianie, obsługa konta, bezpieczeństwo, kontakt administracyjny |
| Widoczność | Prywatne |

Uwagi:

Adres e-mail powinien znajdować się w prywatnym profilu użytkownika. Migracja prywatnych pól jest śledzona w #202.

### 4.3 Dane osobowe — imię i nazwisko

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak, jeśli pola są aktywne w profilu |
| Czy udostępniane? | Nie powinny być publiczne, jeśli są prywatną częścią profilu |
| Czy wymagane? | Opcjonalne |
| Cel | Profil użytkownika, obsługa konta |
| Widoczność | Prywatne albo zależne od UI — wymaga potwierdzenia |

Uwagi:

Jeżeli imię i nazwisko nie są wymagane do działania aplikacji, traktować jako opcjonalne dane profilu. Docelowo powinny trafić do `users/{uid}/private/profile`.

### 4.4 Zdjęcia i pliki użytkownika

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak |
| Czy udostępniane? | Zdjęcia miejsc/opinii mogą być publicznie widoczne w aplikacji; technicznie przechowywane w Firebase Storage |
| Czy wymagane? | Opcjonalne |
| Cel | Funkcjonalność aplikacji, treści użytkownika, profil, miejsca, opinie |
| Widoczność | Publiczna lub prywatna zależnie od typu zdjęcia |

Uwagi:

Zdjęcia miejsc i opinii mogą być widoczne publicznie. Avatar użytkownika może być widoczny przy profilu lub treściach, jeśli UI go pokazuje.

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

### 4.7 Dane diagnostyczne

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Tak, jeśli Crashlytics / Performance Monitoring są aktywne |
| Czy udostępniane? | Przetwarzane przez Firebase/Google jako dostawcę usług |
| Czy wymagane? | Zwykle automatyczne dla diagnostyki, zależnie od konfiguracji |
| Cel | Diagnostyka, stabilność, bezpieczeństwo, analiza awarii |
| Widoczność | Niepubliczne |

Do potwierdzenia:

- czy Crashlytics jest włączony w buildzie release,
- czy Performance Monitoring jest aktywny,
- czy użytkownik jest informowany o diagnostyce w polityce prywatności,
- czy zbieranie danych diagnostycznych można ograniczyć.

### 4.8 Identyfikatory urządzenia lub instalacji

| Pole | Propozycja odpowiedzi |
| --- | --- |
| Czy zbierane? | Możliwe |
| Czy udostępniane? | Przetwarzane przez Firebase/Google Play Services |
| Czy wymagane? | Zależne od Firebase/Google Play Services |
| Cel | Bezpieczeństwo, diagnostyka, działanie usług, powiadomienia, antynadużycia |
| Widoczność | Niepubliczne |

Do potwierdzenia:

- Firebase Installation ID,
- Crashlytics installation UUID,
- tokeny FCM, jeśli powiadomienia są aktywne,
- App Check tokeny,
- Google Play Services identifiers.

## 5. Udostępnianie danych

W Google Play termin „sharing” może obejmować przekazywanie danych podmiotom trzecim, ale nie zawsze obejmuje dostawców usług przetwarzających dane w imieniu operatora.

Robocza interpretacja:

```text
Dane nie są sprzedawane.
Dane są przetwarzane przez Google/Firebase jako dostawców usług technicznych.
Część treści użytkownika jest publicznie widoczna w aplikacji, bo taka jest funkcja aplikacji.
```

Do potwierdzenia w Google Play Console:

- czy Google/Firebase należy oznaczyć jako „shared” w konkretnych pytaniach formularza,
- czy publiczne treści użytkownika traktować jako udostępniane innym użytkownikom,
- czy Crashlytics/Performance wpływa na deklarację udostępniania.

## 6. Cele przetwarzania danych

Prawdopodobne cele do zaznaczenia:

| Cel | Dane |
| --- | --- |
| App functionality | konto, profil, miejsca, opinie, zdjęcia, lokalizacja |
| Analytics / diagnostics | crash logs, performance data, device/app info |
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
| Diagnostyka | Zależne od konfiguracji | Do potwierdzenia. |

## 8. Elementy do ręcznego potwierdzenia

Przed finalnym wypełnieniem Data Safety trzeba sprawdzić:

- [ ] aktywne usługi Firebase w konsoli,
- [ ] czy Crashlytics jest aktywny w release,
- [ ] czy Performance Monitoring jest aktywny w release,
- [ ] czy Analytics jest używany,
- [ ] czy FCM/powiadomienia są używane,
- [ ] czy App Check jest aktywny i w jakim trybie,
- [ ] manifest Androida pod kątem uprawnień lokalizacji i zdjęć,
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
- App interactions, jeśli zbierane przez Firebase/Analytics

App info and performance:
- Crash logs
- Diagnostics
- Performance data, jeśli aktywne

Device or other IDs:
- Device or other IDs, jeśli używane przez Firebase/Google Play Services
```

## 10. Rekomendacje przed publikacją

- Domknąć #212, zanim formularz zostanie oznaczony jako finalny.
- Zweryfikować manifest i uprawnienia Androida.
- Zweryfikować aktywne usługi Firebase.
- Unikać deklaracji „nie zbieramy”, jeśli Firebase lub Google Play Services mogą zbierać diagnostykę/identyfikatory.
- Nie oznaczać danych jako opcjonalnych, jeśli konto jest wymagane do korzystania z kluczowych funkcji.
- Dodać widoczne ostrzeżenie w UI przed publikacją zdjęć dzieci lub osób trzecich.

## 11. Wniosek

Aplikacja powinna być deklarowana jako aplikacja zbierająca dane użytkownika, w szczególności dane konta, treści użytkownika, zdjęcia i lokalizację.

Największe ryzyka przed publikacją:

1. brak potwierdzonego flow usuwania konta,
2. niepewna konfiguracja Crashlytics / Performance / Analytics,
3. brak finalnej migracji prywatnych pól użytkownika,
4. zdjęcia dzieci i osób trzecich,
5. lokalizacja dokładna versus przybliżona.
