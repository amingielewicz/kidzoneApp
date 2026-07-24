# Account deletion test checklist

Powiązane issue: #182, #210, #269, #270, #275, #292

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje ręczną weryfikację usuwania konta użytkownika przed publikacją kidZone w Google Play.

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
Konto testowe:
UID:
Wynik: PASS / FAIL / BLOCKED
```

## Przygotowanie konta

Na koncie testowym utwórz:

- publiczną nazwę użytkownika,
- opcjonalne dane profilu,
- avatar,
- co najmniej jedno miejsce,
- opinię i ocenę,
- co najmniej jedno zdjęcie,
- aktywny token FCM,
- dane prywatne w `users/{uid}/private/*`, jeśli istnieją.

## 1. Dostępność funkcji

- [ ] akcja usunięcia konta jest dostępna w `Profil → Konto i bezpieczeństwo`,
- [ ] użytkownik nie musi szukać jej w dokumentach prawnych,
- [ ] publiczna strona `/account-deletion` działa bez logowania,
- [ ] ścieżka w aplikacji i na stronie publicznej jest zgodna.

## 2. Ostrzeżenie

Komunikat powinien jasno opisywać:

- nieodwracalność operacji,
- utratę możliwości logowania,
- sposób obsługi danych prywatnych,
- sposób obsługi miejsc, opinii, ocen i zdjęć,
- możliwą anonimizację treści publicznych,
- konieczność ponownego uwierzytelnienia.

- [ ] użytkownik może anulować operację,
- [ ] anulowanie nie zmienia danych,
- [ ] potwierdzenie nie jest możliwe przypadkowym pojedynczym kliknięciem.

## 3. Ponowne uwierzytelnienie

Sprawdź osobno:

- konto e-mail i hasło,
- konto Google, jeśli obsługiwane.

- [ ] `requires-recent-login` jest obsłużone,
- [ ] błędne hasło nie usuwa części danych,
- [ ] anulowany reauth nie usuwa części danych,
- [ ] błąd sieci nie pozostawia konta w stanie pośrednim,
- [ ] użytkownik dostaje czytelny komunikat i możliwość ponowienia.

## 4. Firebase Authentication

Po operacji:

- [ ] użytkownik znika z Firebase Authentication,
- [ ] stare dane logowania nie działają,
- [ ] sesja jest zakończona,
- [ ] restart aplikacji nie przywraca sesji,
- [ ] przycisk wstecz nie wraca do części zalogowanej.

## 5. Firestore — profil publiczny

Sprawdź `users/{uid}`.

Poprawny wynik:

- dokument usunięty, albo
- dokument zanonimizowany bez danych identyfikujących.

- [ ] brak e-maila,
- [ ] brak imienia i nazwiska,
- [ ] nazwa użytkownika zanonimizowana, jeśli dokument zostaje,
- [ ] avatar usunięty albo zastąpiony fallbackiem,
- [ ] brak prywatnych i administracyjnych informacji,
- [ ] reguły nie pozwalają byłemu użytkownikowi na dostęp.

## 6. Firestore — dane prywatne

Sprawdź:

```text
users/{uid}/private/profile
users/{uid}/private/messaging
users/{uid}/private/*
```

- [ ] e-mail i dane profilu usunięte,
- [ ] ustawienia prywatne usunięte,
- [ ] tokeny FCM usunięte lub unieważnione,
- [ ] brak dokumentów pozwalających zidentyfikować użytkownika,
- [ ] brak publicznego odczytu pozostałych dokumentów.

## 7. Treści publiczne

Sprawdź miejsca, opinie, oceny, zdjęcia, zgłoszenia i ranking.

- [ ] publiczne treści nie pokazują e-maila ani danych prywatnych,
- [ ] autor jest usunięty albo zanonimizowany zgodnie z polityką,
- [ ] ranking nie prowadzi do aktywnego profilu usuniętego konta,
- [ ] szczegóły miejsca i opinii nie crashują bez autora,
- [ ] zgłoszenia administracyjne zachowują dane tylko w uzasadnionym zakresie.

## 8. Firebase Storage

Sprawdź między innymi:

```text
users/{uid}/avatar/*
places/{uid}/{placeId}/photos/*
reviews/{uid}/{reviewId}/photos/*
```

- [ ] avatar został usunięty,
- [ ] pliki prywatne zostały usunięte,
- [ ] zdjęcia publiczne mają strategię zgodną z dokumentami,
- [ ] brak uszkodzonych odnośników w UI,
- [ ] usunięte konto nie może zapisywać ani usuwać plików,
- [ ] cleanup częściowy ma retry albo osobne issue.

## 9. FCM

- [ ] token został usunięty albo unieważniony,
- [ ] usunięty użytkownik nie dostaje powiadomień konta,
- [ ] token nie jest publiczny,
- [ ] token nie występuje w logach,
- [ ] scheduled functions nie próbują wysyłać do nieaktywnego konta bez obsługi błędu.

## 10. Lokalny stan aplikacji

- [ ] Room i preferences nie pokazują prywatnych danych,
- [ ] widget nie pokazuje danych konta,
- [ ] cache obrazów nie pokazuje avatara jako aktywnego profilu,
- [ ] inne konto nie widzi danych poprzedniego użytkownika,
- [ ] aplikacja działa po ponownej rejestracji lub logowaniu innym kontem.

## 11. Odporność na błędy

Przetestuj:

- brak internetu przed potwierdzeniem,
- utratę internetu podczas operacji,
- timeout Cloud Function,
- częściowe niepowodzenie Storage,
- restart aplikacji podczas procesu,
- dwukrotne kliknięcie przycisku.

Oczekiwane zachowanie:

- brak duplikacji operacji,
- brak fałszywego komunikatu sukcesu,
- czytelna informacja o stanie,
- możliwość bezpiecznego retry,
- błąd jest raportowany bez danych osobowych.

## 12. Dokumenty publiczne

Sprawdź zgodność:

- `public/privacy-policy.html`,
- `public/terms-of-service.html`,
- `public/account-deletion.html`,
- `docs/legal/google-play-data-safety-draft.md`.

- [ ] opis odpowiada realnej implementacji,
- [ ] kontakt jest aktualny,
- [ ] termin realizacji jest wykonalny,
- [ ] opisano usuwanie i anonimizację,
- [ ] linki działają po HTTPS bez logowania.

## 13. Google Play Console

- [ ] Account deletion URL jest zapisany,
- [ ] Privacy Policy URL jest zapisany,
- [ ] Data Safety deklaruje możliwość usunięcia danych,
- [ ] deklaracja odpowiada wynikowi testu,
- [ ] dowód finalnej konfiguracji jest dołączony do issue.

## Kryteria PASS

PASS wymaga jednocześnie:

- usunięcia konta Auth,
- usunięcia lub anonimizacji danych prywatnych,
- spójnej strategii treści publicznych,
- poprawnego cleanup Storage i FCM,
- wyczyszczenia lokalnego cache i widgetu,
- zgodności dokumentów i Play Console,
- braku krytycznego błędu w scenariuszach awaryjnych.

## Wynik

```markdown
## Account deletion result

- Data:
- Tester:
- Build / commit:
- Provider: email / Google
- Urządzenie:
- Wynik: PASS / FAIL / BLOCKED

### Wyniki
- Reauthentication: PASS / FAIL / BLOCKED
- Firebase Auth: PASS / FAIL / BLOCKED
- Firestore public profile: PASS / FAIL / BLOCKED
- Private data: PASS / FAIL / BLOCKED
- Storage: PASS / FAIL / BLOCKED
- FCM: PASS / FAIL / BLOCKED
- Cache and widget: PASS / FAIL / BLOCKED
- Legal and Play Console: PASS / FAIL / BLOCKED

### Dowody
- screenshoty / nagrania / logi / linki:

### Follow-up issues
- brak / #...
```

Issue #210 pozostaje otwarte do czasu wykonania realnego testu z wynikiem PASS.
