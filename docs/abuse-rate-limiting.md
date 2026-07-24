# Abuse and rate limiting strategy

Powiązane issue: #281

Ostatnia aktualizacja: 2026-07-13

## Cel

Strategia ograniczania spamu, automatyzacji, nadużyć i niekontrolowanych kosztów w kidZone.

Firestore Rules i Storage Rules chronią integralność danych, ale nie zastępują rate limitingu, detekcji anomalii ani moderacji.

## Obszary ryzyka

- spam miejscami i fałszywe lokalizacje,
- spam opiniami i manipulacja ocenami,
- zalew zgłoszeń i propozycji zmian,
- masowe uploady zdjęć,
- nadmiarowe tokeny FCM,
- automatyzacja callable functions,
- nadużycia deep linków i powiadomień,
- kosztowne zapytania mapy, listy i rankingu.

## Limity startowe

Wartości są punktem wyjścia i wymagają walidacji po testach beta.

| Obszar | Limit startowy | Główna egzekucja |
| --- | ---: | --- |
| nowe miejsca | 10 dziennie | backend / Cloud Functions |
| opinie | 30 dziennie | backend / Cloud Functions |
| zgłoszenia | 50 dziennie | backend / Cloud Functions |
| propozycje zmian | 30 dziennie | backend / Cloud Functions |
| zdjęcia miejsc i opinii | 40 dziennie | backend + Storage Rules |
| zmiana avatara | 10 dziennie | backend + Storage Rules |
| zdjęcie miejsca/opinii | poniżej 10 MB | Storage Rules |
| avatar | poniżej 5 MB | Storage Rules |

Limity mogą być ostrzejsze dla nowych kont, niezweryfikowanych klientów lub wykrytych anomalii, ale nie powinny blokować normalnego użycia bez czytelnego komunikatu.

## Co egzekwują Rules

Firestore Rules:

- ownership przez UID,
- brak samodzielnej eskalacji roli,
- brak prywatnych pól w publicznym profilu,
- ochrona cudzych danych,
- zakres oceny 1–5,
- chronione pola moderacyjne i agregaty,
- walidacja typów, długości i dozwolonych pól.

Storage Rules:

- zapis wyłącznie do dozwolonej ścieżki właściciela,
- dozwolone MIME,
- maksymalny rozmiar,
- kontrola usuwania,
- brak zapisu do legacy paths,
- operacje administracyjne wyłącznie dla roli zaufanej.

## Czego Rules nie rozwiązują

- limitów dziennych i godzinowych,
- reputacji konta,
- wykrywania botów,
- deduplikacji rozproszonych eventów,
- progresywnych blokad,
- alertów kosztowych,
- analizy treści,
- korelacji nadużyć między urządzeniami i kontami.

Te mechanizmy należą do backendu, Cloud Functions i procesów operacyjnych.

## Idempotencja i deduplikacja

- każda kosztowna operacja ma klucz deduplikacji,
- retry nie tworzy drugiego miejsca, opinii, zgłoszenia ani powiadomienia,
- event backendowy zakłada możliwość ponownego dostarczenia,
- podwójne kliknięcie jest blokowane w UI i backendzie,
- częściowy błąd nie może zostać oznaczony jako sukces.

## App Check

App Check ogranicza ruch z niezaufanych klientów, ale nie zastępuje auth i limitów.

Rollout:

1. debug tokeny dla developmentu,
2. signed release build z Play Integrity,
3. monitoring poprawnych i niepoprawnych requestów,
4. enforcement jednej usługi,
5. smoke test i obserwacja,
6. stopniowe rozszerzanie.

Szczegóły: `docs/app-check.md`.

## Monitoring

Monitorujemy:

- liczbę operacji na użytkownika i urządzenie,
- wzrost kolekcji zgłoszeń,
- liczbę i rozmiar uploadów,
- błędy `permission-denied` i rate limit,
- nieważne tokeny FCM,
- koszty Firestore, Storage, Functions i Maps,
- nietypowe skoki ruchu,
- liczbę zablokowanych i odrzuconych operacji.

Logi nie zawierają treści opinii, pełnych e-maili, dokładnej lokalizacji ani tokenów.

## Reakcja progresywna

1. Odrzuć operację z czytelnym komunikatem.
2. Zastosuj krótki cooldown.
3. Ogranicz wybrane funkcje konta.
4. Oznacz konto do weryfikacji.
5. Zablokuj konto przy potwierdzonym nadużyciu.
6. Usuń lub ukryj szkodliwe treści.
7. Sprawdź powiązane pliki i eventy.
8. Dodaj test, limit lub kontrolę zapobiegawczą.

Każda blokada powinna mieć powód, właściciela decyzji i możliwość audytu.

## UX

- komunikat nie ujawnia szczegółów mechanizmu ochrony,
- użytkownik wie, kiedy może spróbować ponownie,
- błąd nie usuwa danych formularza,
- limit nie jest przedstawiany jako awaria sieci,
- retry po cooldownie nie tworzy duplikatu.

## Testy

- przekroczenie limitu,
- równoległe requesty,
- double submit,
- retry tego samego eventu,
- różne konta na jednym urządzeniu,
- nieważny App Check,
- cudza ścieżka Storage,
- za duży plik i zły MIME,
- użytkownik zablokowany,
- alert kosztowy i procedura reakcji.

Testy Rules:

```powershell
firebase emulators:exec --only firestore,storage "npm --prefix tests/firestore-rules test"
```

## Otwarte decyzje

- sposób przechowywania liczników rate limit,
- limity dla nowych i zaufanych kont,
- model reputacji,
- moderacja zdjęć,
- panel anomalii dla administratora,
- retencja danych pomocniczych używanych do ochrony przed nadużyciami.

## Checklista

- [ ] limity są egzekwowane po stronie zaufanej,
- [ ] Rules chronią ownership i pola,
- [ ] retry jest idempotentne,
- [ ] App Check ma kontrolowany rollout,
- [ ] monitoring kosztów i anomalii działa,
- [ ] logi nie zawierają PII,
- [ ] komunikaty są czytelne,
- [ ] blokady mają audyt i powód,
- [ ] testy negatywne przechodzą.
