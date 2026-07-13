# Firebase and Google Cloud cost alerts

Powiązane issue: #165  
Parent: #159

Ostatnia aktualizacja: 2026-07-14

## Cel

Ograniczenie ryzyka niekontrolowanych kosztów Firebase, Google Cloud i Google Maps w kidZone.

Najważniejsza zasada:

```text
Nie uruchamiamy płatnej konfiguracji ani kosztownej funkcji bez budżetu, alertów, właściciela i planu reakcji.
```

## Zakres

Kontrolujemy przede wszystkim:

- Firestore reads, writes, deletes i listenery,
- Storage, uploady i transfer,
- Cloud Functions, retry i czas wykonania,
- Google Maps Platform i aktywne API,
- App Check jako ochronę przed abuse,
- testy obciążeniowe i sztuczny ruch,
- koszt migracji, cleanup i scheduled jobs.

## Plan Firebase

Przed release zapisz aktualny plan:

```text
Firebase plan: Spark / Blaze
Billing account:
Project ID:
Owner:
Date verified:
```

### Spark

- brak rachunku nie oznacza braku ryzyka operacyjnego,
- przekroczenie limitu może zatrzymać funkcję,
- część usług może wymagać Blaze,
- limity nadal muszą być monitorowane.

### Blaze

- koszty naliczają się według użycia,
- błędne Rules, pętle Functions lub abuse mogą szybko zwiększyć rachunek,
- budżet nie jest twardym limitem wydatków,
- kosztowne funkcje muszą mieć niezależne mechanizmy ograniczania.

## Budżet i alerty

Budżet ustala właściciel projektu. Kwota musi być jawnie zaakceptowana i odpowiadać etapowi projektu.

Przykładowe progi:

| Próg | Znaczenie | Minimalna reakcja |
| --- | --- | --- |
| 50% | wczesne ostrzeżenie | przegląd kosztów według usługi |
| 80% lub 90% | poważne ostrzeżenie | zatrzymanie eksperymentów i analiza usage |
| 100% | budżet osiągnięty | decyzja o ograniczeniu funkcji |
| >100% | budżet przekroczony | incydent kosztowy i natychmiastowy przegląd |

Progi są przykładowe. W Google Cloud zapisujemy faktyczną konfigurację, a nie zakładamy, że alert automatycznie wyłączy billing.

## Odbiorcy alertów

Alerty powinny trafiać co najmniej do:

- właściciela projektu,
- osoby odpowiedzialnej za Firebase/GCP,
- osoby odpowiedzialnej za release lub incydenty.

Lista odbiorców jest przeglądana okresowo. Nie opieramy bezpieczeństwa kosztowego na jednym nieaktywnym adresie.

## Konfiguracja budżetu

- [ ] wybrano właściwe konto billing,
- [ ] budżet dotyczy właściwego projektu lub zakresu,
- [ ] ustawiono zaakceptowane progi,
- [ ] odbiorcy są aktualni,
- [ ] alert testowy lub kontrola konfiguracji została wykonana,
- [ ] wynik i data są udokumentowane,
- [ ] wiadomo, kto podejmuje decyzję o ograniczeniu kosztów.

## Firestore

Główne ryzyka:

- listy bez limitu,
- brak paginacji,
- wiele listenerów realtime,
- mapa pobierająca zbyt duży obszar,
- ranking liczony przez pełne kolekcje,
- retry tworzące dodatkowe zapisy,
- publiczne lub zbyt szerokie Rules.

Kontrole:

- [ ] każda lista ma limit i stabilną paginację,
- [ ] mapa używa bounds, promienia lub geohash,
- [ ] ranking korzysta z pól agregowanych,
- [ ] listener jest zamykany zgodnie z lifecycle,
- [ ] zapytania mają wymagane indeksy,
- [ ] cache ogranicza zbędne ponowne odczyty,
- [ ] Rules i App Check ograniczają abuse,
- [ ] testy nie używają produkcyjnego projektu bez świadomej decyzji.

## Storage

Główne ryzyka:

- duże pliki bez kompresji,
- brak limitów MIME i rozmiaru,
- duplikaty po retry,
- osierocone pliki,
- brak cleanup przy usuwaniu konta,
- masowe pobieranie lub publiczny transfer.

Kontrole:

- [ ] zdjęcia są kompresowane,
- [ ] Rules walidują ownership, MIME i rozmiar,
- [ ] Photo Picker ogranicza niepotrzebny dostęp,
- [ ] retry nie tworzy kilku kopii,
- [ ] cleanup miejsca, opinii i konta jest idempotentny,
- [ ] osierocone pliki są monitorowane,
- [ ] transfer i stored bytes są regularnie przeglądane.

## Cloud Functions

Główne ryzyka:

- pętla triggerów,
- nieograniczony batch,
- agresywny retry,
- zbyt długi timeout lub nadmierna pamięć,
- scheduled function skanująca całą bazę,
- publiczny endpoint bez auth, App Check lub rate limiting.

Kontrole:

- [ ] funkcje mają auth, role i walidację payloadu,
- [ ] eventy są idempotentne,
- [ ] batch i paginacja mają limity,
- [ ] scheduled jobs używają checkpointów,
- [ ] timeout, pamięć i retry są świadomie ustawione,
- [ ] brak kaskady triggerów,
- [ ] kosztowne funkcje można wyłączyć przez config lub deploy,
- [ ] liczba wywołań i błędów jest monitorowana.

## Google Maps Platform

- [ ] klucz jest ograniczony do Android package name i certyfikatów,
- [ ] aktywne są tylko potrzebne API,
- [ ] quota jest ustawiona tam, gdzie możliwe,
- [ ] mapa nie odświeża requestów przy każdej recomposition,
- [ ] ruch kamery ma debounce,
- [ ] Places, Geocoding lub Directions nie są używane bez limitu,
- [ ] usage jest sprawdzany przed i po rollout.

## Analytics, Crashlytics i Performance

Te usługi również wymagają kontroli operacyjnej:

- eventy i trace nie zawierają PII,
- debug i CI nie generują niepotrzebnego ruchu produkcyjnego,
- nazwy trace odpowiadają aktualnemu kodowi,
- nadmierne non-fatal i breadcrumbs nie tworzą szumu,
- Data Safety odpowiada aktywnym SDK i konfiguracji.

## App Check i rate limiting

App Check pomaga ograniczyć ruch z nieautoryzowanych klientów, ale nie zastępuje:

- auth,
- Rules,
- rate limitingu,
- quota,
- monitoringu kosztów.

- [ ] debug provider działa tylko w debug,
- [ ] release używa Play Integrity,
- [ ] enforcement został poprzedzony smoke,
- [ ] prawidłowe buildy nie są blokowane,
- [ ] istnieje rollback,
- [ ] backendowe limity działają niezależnie od App Check.

## Testy wydajnościowe i obciążeniowe

- wykonuj je na projekcie testowym,
- określ maksymalną liczbę operacji przed startem,
- sprawdź billing przed i po teście,
- nie generuj spamu produkcyjnego,
- usuń dane testowe i pliki,
- zapisz wynik oraz koszt.

## Przegląd przed release

- [ ] aktualny plan Firebase jest znany,
- [ ] budżet i alerty są aktywne,
- [ ] odbiorcy są aktualni,
- [ ] usage z ostatniego okresu został sprawdzony,
- [ ] brak nieznanych kosztów,
- [ ] Firestore i Storage mają limity,
- [ ] Functions nie mają pętli ani nieograniczonych batchy,
- [ ] Maps key i quota są poprawne,
- [ ] App Check i rate limiting są zweryfikowane,
- [ ] wynik wpisano do release record.

## Monitoring okresowy

Regularnie sprawdzaj:

- koszt według usługi i projektu,
- Firestore reads/writes/deletes,
- Storage stored bytes i egress,
- Functions invocations, duration i errors,
- Google Maps usage,
- anomalie ruchu i abuse,
- wpływ rollout i nowych funkcji.

Częstotliwość zależy od ruchu. W czasie rollout lub incydentu przegląd jest częstszy niż w spokojnym okresie.

## Reakcja na nagły wzrost kosztów

1. Potwierdź alert, projekt, czas i usługę.
2. Sprawdź Billing Reports oraz usage usługi.
3. Porównaj wzrost z ostatnim deployem, rollout lub zmianą Remote Config.
4. Oceń, czy przyczyną jest legalny ruch, błąd, pętla lub abuse.
5. Zatrzymaj rollout albo eksperyment.
6. Ogranicz funkcję przez Remote Config, quota, Rules lub deploy.
7. Nie wyłączaj zabezpieczeń privacy/security tylko po to, aby ograniczyć koszt.
8. Zachowaj dowody i utwórz incident record.
9. Po naprawie dodaj alert, limit, test lub dashboard zapobiegawczy.
10. Zweryfikuj, że koszt wrócił do oczekiwanego poziomu.

## Minimalna notatka konfiguracyjna

```text
Firebase plan:
Billing account:
Budget:
Alert thresholds:
Recipients:
Maps quotas:
Owner:
Date verified:
Evidence:
```

## Kryteria zamknięcia issue #165

- budżet i alerty są skonfigurowane,
- aktualny plan Firebase jest znany,
- odbiorcy i właściciel reakcji są wskazani,
- wiadomo, gdzie sprawdzać Firestore, Storage, Functions i Maps usage,
- istnieje procedura incydentu kosztowego,
- konfiguracja została udokumentowana i zweryfikowana.

## Czego nie robić

- nie przechodzić na Blaze bez świadomej decyzji,
- nie traktować budżetu jako twardego limitu wydatków,
- nie używać nieograniczonego klucza Maps,
- nie pozostawiać szerokich Rules,
- nie ignorować alertu,
- nie wykonywać nieograniczonych testów na produkcji,
- nie zakładać, że mały ruch dziś oznacza małe ryzyko po publikacji.
