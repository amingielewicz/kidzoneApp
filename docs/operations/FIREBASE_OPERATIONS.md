# Firebase Operations Handbook

Ostatnia aktualizacja: 2026-07-14

## Cel

Operacyjne zasady utrzymania Firebase dla kidZone przed release, podczas rollout i w reakcji na incydenty.

## Zakres

- Authentication,
- Firestore,
- Storage,
- Cloud Functions,
- Remote Config,
- App Check,
- Crashlytics,
- Performance,
- Analytics,
- Cloud Messaging,
- billing, quota i backup.

## Odpowiedzialność

Dla każdej usługi należy wskazać:

- właściciela technicznego,
- projekt Firebase i środowisko,
- osoby z dostępem,
- sposób deployu,
- monitoring,
- rollback,
- dokumentację incydentu.

## Firestore

### Przed release

- [ ] Rules i testy emulatora mają PASS,
- [ ] indeksy są wdrożone przed aplikacją,
- [ ] listy, mapa i ranking mają limity,
- [ ] publiczne dokumenty nie zawierają PII,
- [ ] dane prywatne są oddzielone,
- [ ] migracja jest kompatybilna z aktywnymi buildami,
- [ ] account deletion obejmuje wszystkie kolekcje.

### Po release

- [ ] monitoruj reads, writes i deletes,
- [ ] sprawdź `permission-denied` i błędy indeksów,
- [ ] wykryj nietypowy wzrost listenerów i zapytań,
- [ ] porównaj usage z rollout,
- [ ] zweryfikuj agregaty i błędy synchronizacji.

## Storage

### Przed release

- [ ] Storage Rules i testy mają PASS,
- [ ] ownership ścieżek jest poprawny,
- [ ] MIME i rozmiar są walidowane,
- [ ] kompresja oraz EXIF/GPS cleanup działają,
- [ ] Photo Picker nie wymaga szerokiej zgody galerii,
- [ ] cleanup miejsca, opinii i konta jest przetestowany.

### Po release

- [ ] monitoruj upload failures, storage i transfer,
- [ ] sprawdź osierocone pliki,
- [ ] wykryj spam i nietypowe rozmiary,
- [ ] sprawdź błędy cleanup po account deletion.

## Authentication

- [ ] prawidłowe providery są aktywne,
- [ ] domeny i SHA są poprawne,
- [ ] auth errors nie ujawniają informacji o kontach,
- [ ] logout czyści lokalny stan i tokeny,
- [ ] operacje wrażliwe obsługują reauthentication,
- [ ] ban i delete account kończą sesję.

## Cloud Functions

- [ ] auth, role i payload validation,
- [ ] idempotencja i deduplikacja,
- [ ] kontrolowany retry oraz timeout,
- [ ] brak pętli triggerów,
- [ ] limity batch i kosztów,
- [ ] bezpieczne logi bez PII,
- [ ] monitoring błędów częściowych,
- [ ] testy oraz smoke na właściwym projekcie.

## App Check

- [ ] debug provider tylko w debug,
- [ ] release korzysta z Play Integrity,
- [ ] signed build ma smoke PASS,
- [ ] enforcement wdrażany jest po jednej usłudze,
- [ ] błędy attestation są monitorowane,
- [ ] istnieje procedura szybkiego wyłączenia enforcement.

## Remote Config

Każdy parametr ma:

- wartość domyślną w aplikacji,
- wartość dla każdego środowiska,
- opis i właściciela,
- typ oraz bezpieczny zakres,
- fallback,
- plan rollbacku.

Parametry krytyczne obejmują między innymi maintenance mode, feature flags, limity mapy, listy, rankingu, uploadu i debounce.

Zmiana produkcyjna powinna być zapisana z datą, powodem i wynikiem obserwacji.

## Crashlytics, Analytics i Performance

- [ ] SDK odpowiadają deklaracji Data Safety,
- [ ] eventy, trace i custom keys nie zawierają PII,
- [ ] release nie emituje debugowego spamu,
- [ ] testowy non-fatal lub crash potwierdza działanie konfiguracji,
- [ ] alerty crash/ANR są aktywne,
- [ ] nazwy trace odpowiadają rzeczywistemu kodowi.

## Cloud Messaging

- [ ] tokeny są prywatne,
- [ ] token refresh działa,
- [ ] nieważne tokeny są sprzątane,
- [ ] logout/delete account wykonuje cleanup,
- [ ] payload i deep link są walidowane,
- [ ] odmowa powiadomień nie blokuje aplikacji.

## Billing i quota

- [ ] znany jest plan Firebase,
- [ ] budżet i alerty są aktywne,
- [ ] odbiorcy alertów są aktualni,
- [ ] Maps API key ma ograniczenia,
- [ ] aktywne są tylko potrzebne API,
- [ ] quota i usage są przeglądane przed release,
- [ ] kosztowne funkcje mają limity i możliwość wyłączenia.

Szczegóły: `docs/firebase-cost-alerts.md`.

## Backup i restore

- [ ] częstotliwość, retencja i właściciel są określone,
- [ ] backup jest szyfrowany i odseparowany,
- [ ] restore test ma aktualny wynik PASS,
- [ ] keystore i konfiguracja release mają bezpieczną kopię,
- [ ] restore uwzględnia konta usunięte po dacie kopii.

## Kolejność deploy

Dla zmian zależnych od backendu:

1. kompatybilne Functions i Rules,
2. migracja danych,
3. indeksy,
4. Remote Config,
5. aplikacja,
6. monitoring,
7. usunięcie warstwy zgodności,
8. finalne zaostrzenie Rules lub enforcement.

## Incident checklist

- [ ] ustal usługę, projekt, wersję i czas rozpoczęcia,
- [ ] sprawdź status Firebase/GCP,
- [ ] przejrzyj ostatnie deploye i config changes,
- [ ] sprawdź billing, quota, Crashlytics i logi Functions,
- [ ] oceń wpływ na dane i privacy,
- [ ] zastosuj mitigation: halt rollout, rollback, config change lub feature flag,
- [ ] zapisz oś czasu, właściciela i dowody,
- [ ] po naprawie dodaj kontrolę zapobiegawczą.

## Przegląd okresowy

- [ ] dostępy i role są aktualne,
- [ ] debug tokeny i stare sekrety usunięte,
- [ ] nieużywane API wyłączone,
- [ ] alerty kosztowe i techniczne przetestowane,
- [ ] backup i restore zweryfikowane,
- [ ] dokumentacja odpowiada produkcji,
- [ ] otwarte ryzyka mają issue i właściciela.
