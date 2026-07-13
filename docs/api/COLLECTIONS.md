# Firestore Collections

Ostatnia aktualizacja: 2026-07-13

## Cel

Dokument opisuje odpowiedzialność głównych kolekcji Firestore kidZone, relacje, ownership i zasady dostępu.

## Przegląd

| Kolekcja | Odpowiedzialność | Dostęp |
| --- | --- | --- |
| `users` | publiczny profil użytkownika | publiczny odczyt zgodny z produktem, zapis właściciela w ograniczonym zakresie |
| `users/{uid}/private/*` | dane prywatne i tokeny | właściciel lub administrator |
| `places` | miejsca | publiczny odczyt dozwolonych statusów, zapis zalogowanych |
| `reviews` | opinie i oceny | publiczny odczyt dozwolonych statusów, zapis autora |
| `reports` | zgłoszenia naruszeń | autor tworzy, administrator obsługuje |
| `changeRequests` | propozycje zmian miejsc | użytkownik tworzy, administrator rozpatruje |
| `badges` | odznaki użytkowników | odczyt zgodny z produktem, zapis po stronie zaufanej |
| `notifications` | powiadomienia aplikacyjne | wyłącznie odbiorca i backend |
| `appConfig` | konfiguracja backendowa | odczyt kontrolowany, zapis administracyjny |

## Relacje

```text
User
 ├─ Places
 ├─ Reviews
 ├─ Badges
 ├─ Notifications
 └─ Private data

Place
 ├─ Reviews
 ├─ Reports
 ├─ Change Requests
 └─ Storage photos
```

Firestore nie zapewnia joinów. Relacje są realizowane przez identyfikatory, zapytania z limitami oraz kontrolowaną denormalizację.

## users

Publiczny profil nie zawiera:

- e-maila,
- tokenów FCM,
- danych administracyjnych,
- dokładnej lokalizacji,
- danych wymaganych wyłącznie do obsługi konta.

Właściciel może edytować tylko dozwolone pola profilu. Pola rankingu, blokady, roli i liczników są chronione.

## Dane prywatne

Preferowana struktura:

```text
users/{uid}/private/profile
users/{uid}/private/messaging
users/{uid}/private/preferences
```

Dokumenty są czytelne wyłącznie dla właściciela lub upoważnionego administratora. Cleanup jest częścią account deletion.

## places

- `createdBy` wskazuje autora,
- publiczny odczyt zależy od statusu,
- autor nie może samodzielnie ustawiać statusu moderacji i agregatów,
- edycja przez autora jest ograniczona polityką produktu,
- usunięcie musi uwzględniać opinie, zdjęcia, zgłoszenia i cache.

## reviews

- `userId` i `placeId` definiują relację,
- autor zarządza własną opinią,
- ocena ma zakres 1–5,
- pola moderacyjne są chronione,
- agregaty miejsca są aktualizowane po stronie zaufanej,
- model powinien zapobiegać niekontrolowanym duplikatom opinii.

## reports i changeRequests

- użytkownik tworzy rekord,
- klient nie ustawia statusu rozpatrzenia,
- cudze rekordy nie są publicznie czytelne,
- panel administratora używa limitów, filtrów i indeksów,
- rekordy nie powinny kopiować zbędnych danych osobowych celu lub autora.

## notifications

- dokument należy do jednego odbiorcy,
- klient nie może utworzyć powiadomienia dla dowolnego użytkownika,
- payload zawiera minimalne dane i identyfikator celu,
- retencja i cleanup są jawnie określone,
- FCM tokeny nie są przechowywane w tej publicznej kolekcji.

## appConfig i Remote Config

Konfiguracja publiczna i bezpieczna dla klienta może znajdować się w Remote Config lub kontrolowanym dokumencie. Sekrety, role i dane administracyjne nie mogą być publikowane jako konfiguracja aplikacji.

## Denormalizacja

Denormalizujemy dane wyłącznie, gdy poprawia to koszt lub wydajność, na przykład:

- `ratingAverage`,
- `reviewsCount`,
- `photosCount`,
- liczniki rankingu.

Każde pole denormalizowane musi mieć właściciela aktualizacji, testy i strategię naprawy niespójności.

## Account deletion

Dla każdej kolekcji należy określić:

- usunięcie,
- anonimizację,
- zachowanie z uzasadnionego powodu,
- cleanup zależnych plików,
- wpływ na agregaty i ranking,
- zachowanie po częściowym błędzie.

## Checklista

- [ ] każda kolekcja ma jedną odpowiedzialność,
- [ ] publiczne i prywatne dane są rozdzielone,
- [ ] ownership odpowiada Rules,
- [ ] pola systemowe są chronione,
- [ ] relacje nie wymagają niekontrolowanych joinów,
- [ ] zapytania mają limity i indeksy,
- [ ] denormalizacja ma właściciela aktualizacji,
- [ ] account deletion jest opisane dla każdej kolekcji,
- [ ] dane wrażliwe nie trafiają do publicznych dokumentów.
