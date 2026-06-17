# Firebase and Google Cloud cost alerts

Powiązane issue: #165  
Parent: #159

## Cel

Ten dokument opisuje sposób ograniczania ryzyka niekontrolowanych kosztów Firebase i Google Cloud w projekcie kidZone.

Najważniejsze założenie:

```text
Nie przechodzimy na płatną konfigurację bez budżetu, alertów i świadomej decyzji właściciela projektu.
```

## Zakres

Dokument obejmuje:

- sprawdzenie planu Firebase,
- konfigurację budżetu w Google Cloud Billing,
- progi alertów kosztowych,
- monitoring zużycia Firebase,
- monitoring usług Google Maps Platform,
- checklistę przed release,
- procedurę reakcji na nagły wzrost kosztów.

## Usługi objęte kontrolą kosztów

W projekcie szczególnie kontrolujemy:

| Usługa | Ryzyko kosztowe | Uwagi |
| --- | --- | --- |
| Cloud Firestore | Średnie / wysokie po wzroście ruchu | Odczyty, zapisy, indeksy, zapytania list/mapy/rankingu. |
| Firebase Storage | Średnie | Zdjęcia miejsc, opinii i avatarów, transfer danych. |
| Firebase Authentication | Niskie | Zwykle bezpieczne kosztowo przy standardowym użyciu, ale sprawdzić limity. |
| Firebase Crashlytics | Niskie | Ważne dla stabilności, zwykle bezpieczne kosztowo. |
| Firebase Analytics | Niskie | Zostaje w aplikacji, wymaga Data Safety. |
| Firebase Performance Monitoring | Do potwierdzenia | Chcemy używać, jeśli jest kosztowo bezpieczne. |
| Firebase Cloud Messaging | Niskie | Push notifications, tokeny urządzeń. |
| Firebase App Check | Niskie / zależne od konfiguracji | Chroni przed abuse; sprawdzić tryb enforcement. |
| Google Maps Platform | Średnie / wysokie | Mapy, geokodowanie, Places API, Directions API — zależnie od włączonych API. |
| Cloud Functions | Średnie / wysokie, jeśli używane | Koszt wywołań, czas wykonania, egress, błędne pętle. |

## Spark vs Blaze

### Spark

Spark to bezpłatny plan Firebase.

Ryzyka:

- ograniczone limity,
- po przekroczeniu limitów funkcje mogą przestać działać,
- część usług może wymagać Blaze,
- brak realnego rachunku nie oznacza braku ryzyka operacyjnego.

### Blaze

Blaze to plan płatny.

Ryzyka:

- możliwość naliczania kosztów po przekroczeniu darmowych limitów,
- ryzyko abuse, jeśli Firebase Rules są błędne,
- ryzyko kosztów przy Google Maps Platform,
- ryzyko kosztów przy Cloud Functions.

Zasada:

```text
Przed przejściem na Blaze muszą istnieć Budgets & Alerts.
```

## Minimalna konfiguracja budżetu

W Google Cloud Billing należy utworzyć budżet dla projektu używanego przez Firebase.

Rekomendowany budżet początkowy:

```text
Budżet miesięczny: 10–25 PLN albo najniższa świadomie zaakceptowana kwota.
```

Progi alertów:

| Próg | Znaczenie | Reakcja |
| --- | --- | --- |
| 50% | Wczesne ostrzeżenie | Sprawdzić, co generuje koszt. |
| 90% | Poważne ostrzeżenie | Wstrzymać eksperymenty i sprawdzić usage. |
| 100% | Limit budżetu osiągnięty | Ocenić wyłączenie kosztownych funkcji. |
| 120% | Przekroczenie budżetu | Natychmiastowy przegląd billing i logów. |

## Kto powinien dostawać alerty

Alerty powinny trafiać do:

- właściciela projektu,
- osoby odpowiedzialnej za release,
- osoby technicznej odpowiedzialnej za Firebase/GCP.

Minimalnie:

```text
1 aktywny adres e-mail właściciela projektu.
```

## Checklist konfiguracji Budgets & Alerts

W Google Cloud Console:

- [ ] wejść w Billing,
- [ ] wybrać właściwe konto rozliczeniowe,
- [ ] przejść do Budgets & alerts,
- [ ] utworzyć budżet dla projektu kidZone,
- [ ] ustawić miesięczny próg budżetu,
- [ ] dodać alerty 50%, 90%, 100%, 120%,
- [ ] dodać odbiorców powiadomień,
- [ ] zapisać budżet,
- [ ] wykonać screenshot albo notatkę potwierdzającą konfigurację.

## Checklist Firebase Console

Przed release sprawdzić w Firebase Console:

- [ ] aktualny plan Firebase: Spark albo Blaze,
- [ ] Firestore usage,
- [ ] Storage usage,
- [ ] Authentication usage,
- [ ] Cloud Messaging usage,
- [ ] Crashlytics status,
- [ ] Analytics status,
- [ ] Performance Monitoring status,
- [ ] App Check status,
- [ ] czy Cloud Functions są włączone,
- [ ] czy są alerty lub warningi w konsoli.

## Firestore — kontrola kosztów

Największe ryzyka w Firestore:

- zbyt częste odczyty list,
- brak paginacji,
- słabe zapytania dla mapy,
- ranking liczony przez duże odczyty,
- zbyt częste listenery realtime,
- błędne reguły pozwalające na abuse.

Checklist:

- [ ] listy mają limit wyników,
- [ ] mapa nie pobiera całej bazy bez potrzeby,
- [ ] ranking nie wykonuje masowych odczytów przy każdym wejściu,
- [ ] ekrany nie tworzą wielu aktywnych listenerów naraz,
- [ ] query są indeksowane,
- [ ] Firestore Rules ograniczają zapisy,
- [ ] debug/test data nie generuje dużego ruchu produkcyjnego.

## Storage — kontrola kosztów

Ryzyka:

- duże zdjęcia bez kompresji,
- brak limitu rozmiaru pliku,
- publiczne lub zbyt szerokie odczyty,
- brak usuwania zdjęć po usunięciu konta,
- spam uploadów.

Checklist:

- [ ] zdjęcia są kompresowane przed uploadem,
- [ ] Storage Rules mają limit rozmiaru pliku,
- [ ] upload wymaga zalogowanego użytkownika,
- [ ] użytkownik zapisuje tylko do własnych ścieżek,
- [ ] legacy ścieżki nie pozwalają na nowy zapis,
- [ ] istnieje plan usuwania zdjęć po usunięciu konta,
- [ ] monitoring Storage usage jest sprawdzany przed release.

## Google Maps Platform — kontrola kosztów

Ryzyka:

- nieograniczony klucz API,
- włączone niepotrzebne API,
- brak limitów requestów,
- częste odświeżanie mapy,
- Places API albo geokodowanie bez kontroli.

Checklist:

- [ ] API key jest ograniczony do Android apps,
- [ ] ustawiony jest package name,
- [ ] ustawione są SHA-1 / SHA-256,
- [ ] włączone są tylko potrzebne API,
- [ ] istnieją limity albo quota tam, gdzie możliwe,
- [ ] usage Google Maps jest sprawdzany przed release,
- [ ] map screen nie odpala kosztownych zapytań przy każdym recomposition.

## Cloud Functions — kontrola kosztów

Jeśli Cloud Functions są używane:

- [ ] sprawdzić liczbę wywołań,
- [ ] sprawdzić timeouty,
- [ ] sprawdzić pamięć funkcji,
- [ ] sprawdzić retry policy,
- [ ] zabezpieczyć funkcje auth/App Check,
- [ ] upewnić się, że nie ma pętli triggerów Firestore,
- [ ] dodać alerty na nagły wzrost wywołań.

Ryzykowny przykład:

```text
Firestore trigger zapisuje dokument, który ponownie odpala ten sam trigger.
```

## Performance Monitoring

Decyzja projektowa:

```text
Performance Monitoring chcemy używać, jeśli jest kosztowo bezpieczne.
```

Przed release trzeba potwierdzić:

- [ ] czy Performance Monitoring jest aktywne w release,
- [ ] czy nie wymaga nieakceptowanego planu/kosztu,
- [ ] czy custom traces nie zawierają danych osobowych,
- [ ] czy Data Safety uwzględnia performance data,
- [ ] czy debug/CI nie generuje sztucznego ruchu produkcyjnego.

## Analytics

Analytics zostaje w aplikacji.

Checklist:

- [ ] potwierdzić, jakie eventy są zbierane,
- [ ] nie wysyłać danych osobowych jako event parameters,
- [ ] nie wysyłać e-maili, imienia i nazwiska ani dokładnej lokalizacji w eventach,
- [ ] Data Safety uwzględnia app activity / analytics,
- [ ] polityka prywatności opisuje analitykę.

## App Check jako ochrona kosztów

App Check pomaga ograniczyć nadużycia.

Checklist:

- [ ] debug provider działa tylko w debug,
- [ ] release używa Play Integrity,
- [ ] enforcement jest świadomie ustawiony,
- [ ] przed enforcement sprawdzono, czy prawdziwi użytkownicy nie są blokowani,
- [ ] debug tokeny nie są w repo.

## Procedura cotygodniowego monitoringu

Raz w tygodniu, a przed release obowiązkowo:

- [ ] sprawdzić Google Cloud Billing,
- [ ] sprawdzić Firebase usage,
- [ ] sprawdzić Firestore reads/writes/deletes,
- [ ] sprawdzić Storage bandwidth i stored data,
- [ ] sprawdzić Google Maps usage,
- [ ] sprawdzić Crashlytics i Performance,
- [ ] sprawdzić, czy nie ma nietypowych pików.

## Procedura przed release

Przed publikacją builda:

- [ ] potwierdzić plan Firebase,
- [ ] potwierdzić aktywny budżet,
- [ ] potwierdzić alerty 50%, 90%, 100%, 120%,
- [ ] potwierdzić odbiorców alertów,
- [ ] sprawdzić usage z ostatnich 7 dni,
- [ ] sprawdzić, czy nie ma nieznanych kosztów,
- [ ] sprawdzić Firestore i Storage Rules,
- [ ] sprawdzić App Check,
- [ ] sprawdzić Google Maps quota,
- [ ] wpisać wynik w notatce release.

## Procedura przy nagłym wzroście kosztów

Jeśli pojawi się alert kosztowy albo nietypowy wzrost usage:

1. Wejść do Google Cloud Billing.
2. Sprawdzić, która usługa generuje koszt.
3. Sprawdzić usage w Firebase Console.
4. Jeżeli problem dotyczy Firestore — sprawdzić odczyty, zapisy, listenery i reguły.
5. Jeżeli problem dotyczy Storage — sprawdzić uploady, transfer i reguły.
6. Jeżeli problem dotyczy Maps — sprawdzić włączone API i quota.
7. Jeżeli problem dotyczy Functions — sprawdzić wywołania, logi i retry.
8. Tymczasowo ograniczyć kosztowną funkcję, jeśli to konieczne.
9. Utworzyć issue z opisem incydentu.
10. Po naprawie dopisać zabezpieczenie do dokumentacji albo testów.

## Minimalna notatka po konfiguracji alertów

Po skonfigurowaniu alertów warto dodać komentarz do issue #165:

```markdown
## Konfiguracja kosztów

- Firebase plan: Spark / Blaze
- Budżet miesięczny: ... PLN
- Alerty: 50%, 90%, 100%, 120%
- Odbiorcy alertów: ...
- Data konfiguracji: YYYY-MM-DD
- Uwagi: ...
```

## Kryteria zamknięcia issue #165

Issue można zamknąć, gdy:

- Budgets & Alerts są skonfigurowane w Google Cloud Billing,
- znany jest aktualny plan Firebase,
- wiadomo, gdzie sprawdzać Firestore usage,
- wiadomo, gdzie sprawdzać Storage usage,
- wiadomo, gdzie sprawdzać Google Maps usage,
- dokumentacja monitorowania kosztów znajduje się w repo,
- właściciel projektu wie, kto dostaje alerty.

## Czego nie robić

- Nie przechodzić na Blaze bez budżetu.
- Nie używać nieograniczonego Google Maps API key.
- Nie zostawiać publicznych reguł Firebase.
- Nie ignorować alertu 90% lub 100%.
- Nie zakładać, że brak ruchu testowego oznacza brak ryzyka po publikacji.
