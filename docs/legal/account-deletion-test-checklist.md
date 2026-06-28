# Account deletion test checklist

Powiązane issue: #210
Powiązane issue release: #269, #270, #275
Parent: #182

Ostatnia aktualizacja: 2026-06-28

## Cel

Ten dokument opisuje sposób ręcznej weryfikacji usuwania konta użytkownika w aplikacji kidZone przed publikacją w Google Play.

Issue #210 pozostaje otwarte po dodaniu tej dokumentacji, ponieważ realny test musi zostać wykonany ręcznie na aplikacji i w Firebase Console.

## Dlaczego to ważne

Aplikacja pozwala zakładać konto użytkownika. Przed publikacją trzeba potwierdzić, że użytkownik ma realną i zrozumiałą możliwość usunięcia konta albo wysłania żądania usunięcia danych.

Flow usuwania konta musi być zgodny z:

- polityką prywatności,
- regulaminem,
- publiczną stroną `public/account-deletion.html`,
- Google Play Data Safety,
- realnym zachowaniem aplikacji,
- faktyczną konfiguracją Firebase.

## Zakres testu

Test obejmuje:

- istnienie akcji usunięcia konta,
- komunikaty ostrzegawcze,
- ponowne uwierzytelnienie użytkownika,
- Firebase Authentication,
- Firestore,
- Storage,
- tokeny FCM,
- publiczne treści użytkownika,
- prywatne dane użytkownika,
- dokumenty prawne,
- Google Play Console.

## Minimalne dane do zapisania przed testem

Przed wykonaniem testu zapisać:

```text
Data testu:
Tester:
Wersja aplikacji:
Commit:
Urządzenie / emulator:
Android version:
Firebase project:
Konto testowe e-mail:
UID użytkownika:
Wynik: PASS / FAIL / BLOCKED
```

## Konto testowe

Utworzyć konto testowe, które można bezpiecznie usunąć.

Na koncie testowym przygotować dane:

- [ ] publiczna nazwa użytkownika,
- [ ] opcjonalne imię i nazwisko, jeśli UI pozwala je ustawić,
- [ ] avatar, jeśli funkcja istnieje,
- [ ] co najmniej jedno dodane miejsce,
- [ ] co najmniej jedna opinia,
- [ ] co najmniej jedna ocena,
- [ ] co najmniej jedno zdjęcie miejsca lub opinii,
- [ ] token FCM zapisany po zalogowaniu, jeśli FCM jest aktywne,
- [ ] dane prywatne w `users/{uid}/private/*`, jeśli istnieją.

## 1. Czy użytkownik widzi opcję usunięcia konta?

Sprawdzić w aplikacji:

- [ ] profil,
- [ ] ustawienia konta,
- [ ] ekran edycji profilu,
- [ ] regulamin / prywatność,
- [ ] inne miejsca, gdzie może być akcja usunięcia.

Wynik:

```text
Opcja usunięcia konta istnieje: TAK / NIE
Ścieżka w aplikacji:
Uwagi:
```

Jeżeli opcja nie istnieje, sprawdzić, czy istnieje jasna procedura kontaktu e-mail.

## 2. Ostrzeżenie przed usunięciem

Przed usunięciem użytkownik powinien dostać jasny komunikat.

Sprawdzić, czy komunikat informuje o:

- [ ] usunięciu konta,
- [ ] skutkach dla logowania,
- [ ] skutkach dla danych prywatnych,
- [ ] skutkach dla treści publicznych,
- [ ] ewentualnej anonimizacji opinii/miejsc,
- [ ] tym, czy zdjęcia zostaną usunięte,
- [ ] nieodwracalności operacji,
- [ ] możliwej konieczności ponownego logowania.

Wynik:

```text
Ostrzeżenie jest jasne: TAK / NIE
Braki:
```

## 3. Ponowne uwierzytelnienie

Firebase może wymagać świeżego logowania przed usunięciem konta.

Sprawdzić:

- [ ] czy aplikacja obsługuje `requires-recent-login`,
- [ ] czy użytkownik może ponownie podać hasło,
- [ ] czy użytkownik logowany Google ma obsłużony reauth,
- [ ] czy błąd reauth jest pokazany w zrozumiały sposób,
- [ ] czy anulowanie reauth nie usuwa konta częściowo.

Wynik:

```text
Reauth działa: TAK / NIE / NIE DOTYCZY
Uwagi:
```

## 4. Firebase Authentication

Po usunięciu konta sprawdzić w Firebase Console:

- [ ] użytkownik znika z Firebase Authentication,
- [ ] użytkownik nie może zalogować się starym hasłem,
- [ ] sesja w aplikacji jest zakończona,
- [ ] aplikacja przechodzi do ekranu logowania,
- [ ] po restarcie aplikacji użytkownik nadal jest wylogowany.

Wynik:

```text
Firebase Auth account removed: TAK / NIE
Uwagi:
```

## 5. Firestore — dokument publiczny użytkownika

Sprawdzić dokument:

```text
users/{uid}
```

Możliwe poprawne strategie:

### Opcja A — usunięcie dokumentu

```text
users/{uid} nie istnieje po usunięciu konta.
```

### Opcja B — anonimizacja dokumentu

```text
users/{uid} zostaje, ale nie zawiera danych identyfikujących użytkownika.
```

Jeśli dokument zostaje, sprawdzić:

- [ ] brak e-maila,
- [ ] brak imienia i nazwiska,
- [ ] brak prywatnych danych,
- [ ] nazwa użytkownika jest zanonimizowana,
- [ ] avatar jest usunięty albo zastąpiony fallbackiem,
- [ ] role i pola administracyjne nie ujawniają prywatnych informacji.

Wynik:

```text
users/{uid}: USUNIĘTY / ZANONIMIZOWANY / BEZ ZMIAN / BLOCKED
Uwagi:
```

## 6. Firestore — prywatne dane użytkownika

Sprawdzić ścieżki:

```text
users/{uid}/private/profile
users/{uid}/private/messaging
users/{uid}/private/*
```

Po usunięciu konta prywatne dane powinny być usunięte albo zanonimizowane.

Sprawdzić:

- [ ] e-mail usunięty,
- [ ] imię i nazwisko usunięte,
- [ ] ustawienia prywatne usunięte,
- [ ] tokeny FCM usunięte,
- [ ] prywatne dokumenty nie są publicznie czytelne,
- [ ] brak danych pozwalających łatwo zidentyfikować użytkownika.

Wynik:

```text
Private user data removed/anonymized: TAK / NIE / BLOCKED
Uwagi:
```

## 7. Publiczne treści użytkownika

Sprawdzić treści utworzone przed usunięciem konta:

- [ ] miejsca,
- [ ] opinie,
- [ ] oceny,
- [ ] zdjęcia,
- [ ] zgłoszenia,
- [ ] ranking.

Możliwe strategie:

### Opcja A — usunięcie treści

Wszystkie treści użytkownika są usuwane.

### Opcja B — anonimizacja autora

Treści zostają, ale autor jest zanonimizowany.

Przykład:

```text
Użytkownik usunięty
```

Sprawdzić:

- [ ] publiczne treści nie pokazują e-maila,
- [ ] publiczne treści nie pokazują imienia i nazwiska,
- [ ] publiczne treści nie linkują do prywatnego profilu,
- [ ] ranking nie pokazuje usuniętego użytkownika jako aktywnego profilu,
- [ ] szczegóły miejsca/opinii nie crashują po usunięciu autora.

Wynik:

```text
Public content: USUNIĘTE / ZANONIMIZOWANE / BEZ ZMIAN / BLOCKED
Uwagi:
```

## 8. Firebase Storage

Sprawdzić zdjęcia użytkownika w Storage.

Możliwe ścieżki:

```text
users/{uid}/avatar/*
places/{uid}/{placeId}/photos/*
reviews/{uid}/{reviewId}/photos/*
```

Sprawdzić:

- [ ] avatar został usunięty albo zanonimizowany,
- [ ] zdjęcia prywatne zostały usunięte,
- [ ] zdjęcia publiczne mają jasną strategię: usunięcie albo pozostawienie jako treść publiczna,
- [ ] aplikacja nie pokazuje uszkodzonych obrazków,
- [ ] brak dostępu do ścieżek po usuniętym koncie, jeśli powinny być prywatne.

Wynik:

```text
Storage cleanup: TAK / NIE / CZĘŚCIOWO / BLOCKED
Uwagi:
```

## 9. Firebase Cloud Messaging

Jeśli FCM jest aktywne, sprawdzić:

- [ ] token FCM jest usuwany z `users/{uid}/private/messaging`,
- [ ] wylogowany/usunięty użytkownik nie dostaje powiadomień powiązanych z kontem,
- [ ] token nie jest publicznie czytelny,
- [ ] token nie zostaje w logach.

Wynik:

```text
FCM token cleanup: TAK / NIE / NIE DOTYCZY / BLOCKED
Uwagi:
```

## 10. Zachowanie aplikacji po usunięciu konta

Po usunięciu konta:

- [ ] aplikacja kończy sesję użytkownika,
- [ ] użytkownik trafia do logowania/startu,
- [ ] aplikacja nie crashuje,
- [ ] przycisk wstecz nie wraca do zalogowanej części aplikacji,
- [ ] restart aplikacji nie przywraca sesji,
- [ ] cache lokalny nie pokazuje prywatnych danych,
- [ ] ponowna rejestracja tym samym e-mailem działa albo jest jasno obsłużona.

Wynik:

```text
App state after deletion: PASS / FAIL / BLOCKED
Uwagi:
```

## 11. Local cache / Room

Jeśli aplikacja cacheuje dane lokalnie:

- [ ] prywatne dane użytkownika znikają po wylogowaniu/usunięciu konta,
- [ ] cache nie pokazuje danych usuniętego konta,
- [ ] publiczne dane mogą zostać, jeśli nie identyfikują użytkownika,
- [ ] aplikacja nie crashuje przy braku autora/UID.

Wynik:

```text
Local cache cleanup: TAK / NIE / NIE DOTYCZY / BLOCKED
Uwagi:
```

## 12. Dokumenty prawne

Sprawdzić zgodność z:

```text
public/privacy-policy.html
public/terms-of-service.html
public/account-deletion.html
```

Polityka prywatności powinna wyjaśniać:

- [ ] jak użytkownik usuwa konto,
- [ ] czy może zażądać usunięcia danych e-mailem,
- [ ] co dzieje się z treściami publicznymi,
- [ ] co dzieje się ze zdjęciami,
- [ ] jaki jest kontakt do administratora,
- [ ] czy część danych może zostać zanonimizowana zamiast usunięta.

Publiczna strona usuwania konta powinna:

- [ ] działać bez logowania,
- [ ] zawierać ścieżkę w aplikacji,
- [ ] zawierać kontakt e-mail,
- [ ] opisywać dane usuwane i anonimizowane,
- [ ] opisywać termin realizacji,
- [ ] linkować politykę prywatności i regulamin.

Wynik:

```text
Legal docs consistent: TAK / NIE / BLOCKED
Uwagi:
```

## 13. Google Play Console

Przed publikacją sprawdzić deklaracje:

- [ ] Data Safety mówi, że użytkownik może zażądać usunięcia danych,
- [ ] link do polityki prywatności działa,
- [ ] opis w Google Play nie obiecuje czegoś, czego aplikacja nie robi,
- [ ] procedura usuwania konta jest dostępna z aplikacji albo jasno opisana,
- [ ] procedura działa dla kont testowych.

Wynik:

```text
Google Play deletion declaration ready: TAK / NIE / BLOCKED
Uwagi:
```

## 14. Minimalny wynik PASS

Test można uznać za zaliczony, jeśli:

- [ ] konto można usunąć z aplikacji albo istnieje jasna procedura żądania usunięcia,
- [ ] użytkownik dostaje jasne ostrzeżenie,
- [ ] Firebase Auth nie pozwala dalej używać usuniętego konta,
- [ ] prywatne dane nie są publicznie widoczne,
- [ ] publiczne treści są usunięte albo zanonimizowane,
- [ ] zdjęcia mają jasną strategię usunięcia albo pozostawienia,
- [ ] dokumenty prawne są zgodne z aplikacją,
- [ ] wynik testu jest zapisany w issue #210.

## 15. Co zrobić przy wyniku FAIL

Jeżeli test nie przejdzie:

1. Nie zamykać #210.
2. Utworzyć osobne issue dla każdego realnego braku.
3. Oznaczyć braki jako blocker przed Google Play, jeśli dotyczą wymagań usuwania konta.
4. Nie oznaczać Data Safety jako finalnego.
5. Nie publikować aplikacji bez jasnej procedury usuwania konta/danych.

Przykładowe follow-up issue:

```text
legal: add in-app account deletion entry point
security: remove private profile data during account deletion
storage: delete or anonymize user photos after account deletion
privacy: anonymize public user content after account deletion
```

## 16. Format komentarza do issue #210

Po wykonaniu testu dopisać komentarz:

```markdown
## Account deletion test result

- Data:
- Tester:
- Wersja aplikacji:
- Commit:
- Urządzenie:
- Android:
- Firebase project:
- Konto testowe:
- UID:
- Wynik ogólny: PASS / FAIL / BLOCKED

### Firebase Auth
- Wynik:
- Uwagi:

### Firestore public user document
- Wynik:
- Uwagi:

### Firestore private user data
- Wynik:
- Uwagi:

### Public content
- Wynik:
- Uwagi:

### Storage
- Wynik:
- Uwagi:

### FCM tokens
- Wynik:
- Uwagi:

### Legal / Google Play
- Wynik:
- Uwagi:

### Follow-up issues
- ...
```

## Kryteria zamknięcia issue #210

Issue #210 można zamknąć dopiero, gdy:

- realny test usuwania konta został wykonany,
- wynik testu jest zapisany,
- braki mają osobne issue,
- zachowanie aplikacji jest zgodne z polityką prywatności,
- Google Play Data Safety może zostać wypełnione bez zgadywania.

Samo dodanie tej dokumentacji nie zamyka issue #210.
