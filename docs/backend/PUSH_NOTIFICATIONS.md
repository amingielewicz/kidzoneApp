# Push Notifications

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje architekturę powiadomień push w kidZone oraz zasady bezpieczeństwa, preferencji użytkownika, deep linków i sprzątania tokenów.

## Technologia

- Firebase Cloud Messaging,
- Cloud Functions jako warstwa wysyłająca,
- Firebase Authentication do identyfikacji odbiorcy,
- deep linki do otwierania właściwego ekranu.

## Typy powiadomień

- nowa opinia o miejscu,
- nowe zdjęcie miejsca,
- zdobycie odznaki,
- zmiana pozycji w rankingu,
- informacja administracyjna,
- komunikat maintenance.

Każdy typ powinien mieć właściciela biznesowego, jasny cel i możliwy do wyłączenia zakres.

## Preferencje użytkownika

- szanujemy globalną zgodę systemową,
- szanujemy preferencje kategorii powiadomień,
- odmowa `POST_NOTIFICATIONS` nie blokuje aplikacji,
- nie ponawiamy systemowego dialogu w pętli,
- użytkownik może zmienić preferencje bez ponownej rejestracji konta.

## Tokeny FCM

Token:

- jest przechowywany w prywatnej części danych użytkownika,
- nie jest publicznie czytelny,
- nie jest logowany w pełnej postaci,
- jest aktualizowany po zmianie,
- jest usuwany lub unieważniany po logout i delete account,
- jest sprzątany po błędzie `unregistered` lub innym trwałym błędzie dostarczenia.

Urządzenie może mieć więcej niż jeden token. Model danych powinien wspierać bezpieczny cleanup starych wpisów.

## Payload

Przykładowy minimalny payload:

```text
notification
  title
  body

data
  type
  targetId
  targetType
  notificationId
```

Payload nie powinien zawierać:

- e-maila,
- pełnej nazwy użytkownika, jeśli nie jest to konieczne,
- treści prywatnej,
- dokładnej lokalizacji,
- tokenów,
- pełnego opisu miejsca lub opinii.

Aplikacja pobiera aktualne dane po `targetId`, zamiast ufać pełnemu obiektowi z powiadomienia.

## Deep link

Każdy deep link:

- waliduje `type`, `targetType` i `targetId`,
- respektuje auth i stan konta,
- obsługuje brak zasobu,
- nie omija blokad i reguł dostępu,
- nie tworzy wielu kopii tego samego ekranu,
- ma kontrolowany fallback.

Link z powiadomienia używa tych samych reguł co link zewnętrzny.

## Deduplikacja

- event ma `notificationId` lub klucz deduplikacji,
- retry wysyłki nie tworzy wielu identycznych pushy,
- scheduled function ma limit liczby powiadomień,
- ta sama akcja użytkownika nie wyzwala kilku kanałów bez potrzeby.

## Bezpieczeństwo

- wysyłka odbywa się po stronie zaufanej,
- klient nie wybiera dowolnego odbiorcy,
- funkcja sprawdza auth, ownership lub rolę,
- payload nie zawiera danych wrażliwych,
- tokeny są prywatne,
- logi nie zawierają pełnych tokenów ani treści powiadomień zawierających PII.

## Zachowanie aplikacji

- tapnięcie otwiera właściwy ekran,
- brak logowania prowadzi przez kontrolowany auth flow,
- usunięty zasób pokazuje czytelny komunikat,
- brak internetu nie powoduje crasha,
- powiadomienie w foreground nie dubluje istniejącego UI,
- badge i stan przeczytania są aktualizowane idempotentnie.

## Monitoring

Monitorujemy:

- liczbę wysłanych pushy,
- sukcesy i błędy dostarczenia,
- nieważne tokeny,
- czas od eventu do wysyłki,
- liczbę deduplikowanych zdarzeń,
- otwarcia deep linków bez danych osobowych,
- nietypowy wzrost wolumenu i kosztów.

## Testy

- allow i deny `POST_NOTIFICATIONS`,
- token refresh,
- logout i delete account,
- nieważny token,
- duplikat eventu,
- brak zasobu,
- użytkownik zablokowany lub wylogowany,
- foreground i background,
- brak internetu,
- nieprawidłowy payload lub deep link.

## Checklista

- [ ] typ powiadomienia ma właściciela i cel,
- [ ] preferencje użytkownika są respektowane,
- [ ] token jest prywatny i ma cleanup,
- [ ] payload nie zawiera PII,
- [ ] deep link jest walidowany,
- [ ] wysyłka jest deduplikowana,
- [ ] nieważne tokeny są usuwane,
- [ ] logout i delete account są obsłużone,
- [ ] monitoring działa,
- [ ] scenariusze negatywne mają testy.
