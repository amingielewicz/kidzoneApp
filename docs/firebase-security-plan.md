# Firebase Security Hardening Plan

Ostatnia aktualizacja: 2026-07-13

## Cel

Plan utwardzenia Firebase w kidZone bez wprowadzania ryzykownych zmian produkcyjnych bez migracji, testów i możliwości rollbacku.

## Zakres

Projekt korzysta z:

- Firebase Authentication,
- Cloud Firestore,
- Firebase Storage,
- Cloud Functions,
- App Check,
- Cloud Messaging,
- Crashlytics, Analytics i Performance,
- Firebase Hosting.

## Docelowe zasady

- dane publiczne i prywatne są rozdzielone,
- ownership jest sprawdzany po UID,
- rola administratora pochodzi z custom claims lub innego zaufanego źródła,
- klient nie zmienia pól systemowych i moderacyjnych,
- ścieżki Storage zawierają właściciela tam, gdzie jest to potrzebne,
- App Check release używa Play Integrity,
- sekrety, tokeny i PII nie trafiają do logów,
- każda zmiana Rules ma testy emulatorowe.

## Publiczny i prywatny profil

Docelowa struktura:

```text
users/{uid}
users/{uid}/private/profile
users/{uid}/private/messaging
users/{uid}/private/preferences
```

`users/{uid}` zawiera tylko dane publiczne, na przykład:

- `displayName`,
- `avatarUrl`,
- publiczne liczniki i odznaki,
- publiczny status profilu zgodny z produktem.

Prywatne subdokumenty zawierają między innymi:

- e-mail,
- dane profilu niewidoczne publicznie,
- tokeny FCM,
- preferencje powiadomień.

Firestore nie ukrywa pojedynczych pól w dokumencie, dlatego dane prywatne nie mogą pozostawać w publicznie czytelnym profilu.

## Storage

Docelowe ścieżki:

```text
places/{ownerUserId}/{placeId}/photos/{fileId}
reviews/{ownerUserId}/{reviewId}/photos/{fileId}
users/{userId}/avatar/{fileId}
```

Rules powinny sprawdzać UID w ścieżce, MIME, rozmiar i dozwoloną operację.

Legacy paths pozostają read-only lub są migrowane kontrolowanie. Nie zaostrzamy Rules przed zmianą aktywnych ścieżek aplikacji.

## Role administratora

Źródłem prawdy nie jest edytowalne pole `role` w publicznym dokumencie.

Preferowane sprawdzenie:

```text
request.auth.token.admin == true
```

Zmiana custom claims odbywa się wyłącznie po stronie zaufanej. Po zmianie roli należy uwzględnić odświeżenie tokenu użytkownika.

## App Check

- debug provider tylko dla debug buildów,
- Play Integrity dla release,
- enforcement wdrażany etapami,
- signed build przechodzi smoke przed blokowaniem ruchu,
- monitoring i rollback są przygotowane,
- App Check nie zastępuje Rules, auth ani rate limitingu.

Szczegóły: `docs/app-check.md`.

## Cloud Functions

Funkcje powinny:

- sprawdzać auth i role,
- walidować payload,
- być idempotentne,
- nie ufać UID i polom administracyjnym z klienta,
- obsługiwać retry i błędy częściowe,
- nie logować PII,
- mieć limity kosztów i liczby operacji.

## Account deletion

Utwardzenie musi uwzględniać:

- cleanup prywatnych subdokumentów,
- usunięcie lub anonimizację profilu publicznego,
- cleanup Storage,
- cleanup tokenów FCM,
- aktualizację agregatów i rankingu,
- retry po błędzie częściowym,
- retencję backupów.

## Etapy wdrożenia

### Etap 1 — inwentaryzacja

- zidentyfikuj aktywne ścieżki Firestore i Storage,
- sprawdź wszystkie miejsca odczytu e-maila i tokenów FCM,
- potwierdź źródło roli administratora,
- spisz aktywne buildy i ich zależności,
- przygotuj testy Rules.

### Etap 2 — migracja danych prywatnych

- dodaj obsługę prywatnych subdokumentów,
- rozpocznij dual write lub kontrolowaną migrację,
- zmień odczyty aplikacji i backendu,
- zweryfikuj starsze buildy,
- dopiero potem usuń prywatne pola z publicznego dokumentu.

### Etap 3 — migracja Storage

- wdroż nowe ścieżki właścicielskie,
- zmień uploady aplikacji,
- dodaj cleanup i migrację istniejących plików,
- uruchom testy negatywne,
- zaostrz Rules po potwierdzeniu aktywnych klientów.

### Etap 4 — role i admin

- przenieś autoryzację na custom claims,
- sprawdź panel administracyjny i Cloud Functions,
- zablokuj możliwość samodzielnej zmiany roli,
- dodaj audyt operacji administracyjnych.

### Etap 5 — App Check i rate limiting

- zweryfikuj signed release build,
- uruchom monitoring,
- włącz enforcement jednej usługi,
- wykonaj smoke,
- rozszerz enforcement,
- wdroż limity backendowe i alerty kosztowe.

## Kolejność deploy

Dla zmiany zależnej od schematu:

1. kompatybilny backend i Rules,
2. migracja danych,
3. indeksy,
4. aplikacja obsługująca nowy model,
5. obserwacja aktywnych wersji,
6. usunięcie compatibility layer,
7. finalne zaostrzenie Rules.

Nie wdrażamy Rules, które natychmiast blokują aktualną wersję aplikacji.

## Testy

Wymagane scenariusze:

- publiczny profil bez PII,
- brak dostępu do cudzych danych prywatnych,
- brak eskalacji admina,
- upload tylko do własnej ścieżki,
- błędny MIME i rozmiar,
- działanie App Check dla debug i release,
- częściowy błąd account deletion,
- starszy build podczas migracji,
- rollback Rules i enforcement.

## Czego nie robić bez migracji

- nie usuwać ręcznie pól produkcyjnych bez sprawdzenia klientów,
- nie blokować odczytu profilu publicznego używanego przez aplikację,
- nie zmieniać Storage Rules przed wdrożeniem nowych ścieżek,
- nie włączać enforcement wszystkich usług jednocześnie,
- nie traktować pola `role` z klienta jako źródła dostępu,
- nie migrować danych bez backupu i planu rollbacku.

## Release gate

- [ ] aktualne ścieżki są zinwentaryzowane,
- [ ] migracja jest kompatybilna ze starszym buildem,
- [ ] testy Firestore i Storage Rules przechodzą,
- [ ] App Check smoke ma PASS,
- [ ] panel admina i Functions używają zaufanej roli,
- [ ] account deletion działa po migracji,
- [ ] monitoring i rollback są gotowe,
- [ ] Data Safety i polityka prywatności odpowiadają nowemu modelowi.

## Status

Dokument jest planem technicznym. Każdy etap wymaga osobnego PR, testów i świadomego wdrożenia do właściwego projektu Firebase.
