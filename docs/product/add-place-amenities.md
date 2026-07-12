# AddPlace: mapa kategorii i udogodnień

Dokument opisuje mapę udogodnień używaną w formularzu `AddPlace`.

Źródło prawdy w kodzie:

```text
app/src/main/java/com/kidzone/domain/model/Amenity.kt
```

UI formularza korzysta z:

```kotlin
Amenity.forCategory(category)
```

Lista udogodnień jest zależna od wybranej kategorii miejsca. Po zmianie kategorii formularz odświeża kafelki udogodnień w sekcji `Szczegóły`.

## Zasady UX

- Udogodnienia są wyświetlane jako kafelki/chipy w `FlowRow`.
- Chipy układają się automatycznie obok siebie i przechodzą do kolejnej linii, gdy brakuje miejsca.
- Nagłówek sekcji pokazuje liczbę wybranych elementów, np. `Udogodnienia (0)` albo `Udogodnienia (3)`.
- Niewybrane udogodnienie ma neutralny wygląd: jasne tło i spokojny stan wizualny.
- Wybrane udogodnienie ma kolorowe tło z palety aplikacji.
- Każde udogodnienie ma ikonę po lewej stronie tekstu.
- Kolejność jest ustawiona pod szybkie skanowanie przez rodzica: bezpieczeństwo, opieka nad dzieckiem, wygoda, dodatki.
- Po zmianie kategorii stan udogodnień jest czyszczony z elementów, które nie pasują do nowej kategorii.

## Mapa kategorii i udogodnień

### Plac zabaw (`PLAYGROUND`)

| Priorytet | Udogodnienie | Enum |
|---:|---|---|
| 1 | Ogrodzenie | `FENCING` |
| 2 | Miękka nawierzchnia | `SOFT_SURFACE` |
| 3 | Strefa malucha | `TODDLER_ZONE` |
| 4 | Strefa bez aut | `CAR_FREE_AREA` |
| 5 | Toaleta | `TOILET` |
| 6 | Przewijak | `CHANGING_TABLE` |
| 7 | Dostęp dla wózków | `STROLLER_ACCESS` |
| 8 | Ławki w cieniu | `SHADED_BENCHES` |
| 9 | Dobre oświetlenie | `GOOD_LIGHTING` |
| 10 | Miękkie zabezpieczenia | `SOFT_PROTECTION` |

### Restauracja (`RESTAURANT`)

| Priorytet | Udogodnienie | Enum |
|---:|---|---|
| 1 | Przewijak | `CHANGING_TABLE` |
| 2 | Krzesełko do karmienia | `HIGH_CHAIR` |
| 3 | Menu dla dzieci | `KIDS_MENU` |
| 4 | Naczynia dla dzieci | `KIDS_TABLEWARE` |
| 5 | Widoczny kącik dla dzieci | `KIDS_CORNER_VISIBLE` |
| 6 | Toaleta | `TOILET` |
| 7 | Dostęp dla wózków | `STROLLER_ACCESS` |
| 8 | Szybka obsługa | `FAST_SERVICE` |
| 9 | Ciche karmienie | `QUIET_FEEDING` |
| 10 | Mikrofalówka | `MICROWAVE` |
| 11 | Brak głośnej muzyki | `NO_LOUD_MUSIC` |
| 12 | Rozrywka dla dzieci | `KIDS_ENTERTAINMENT` |
| 13 | Parking | `PARKING` |
| 14 | Parking rodzinny | `FAMILY_PARKING` |
| 15 | Szerokie drzwi | `WIDE_DOORS` |
| 16 | Wi-Fi | `WIFI` |
| 17 | Oznaczenia przyjazne dzieciom | `KID_FRIENDLY_SIGNS` |

### Kawiarnia (`CAFE`)

| Priorytet | Udogodnienie | Enum |
|---:|---|---|
| 1 | Przewijak | `CHANGING_TABLE` |
| 2 | Krzesełko do karmienia | `HIGH_CHAIR` |
| 3 | Ciche karmienie | `QUIET_FEEDING` |
| 4 | Toaleta | `TOILET` |
| 5 | Dostęp dla wózków | `STROLLER_ACCESS` |
| 6 | Menu dla dzieci | `KIDS_MENU` |
| 7 | Naczynia dla dzieci | `KIDS_TABLEWARE` |
| 8 | Mikrofalówka | `MICROWAVE` |
| 9 | Brak głośnej muzyki | `NO_LOUD_MUSIC` |
| 10 | Rozrywka dla dzieci | `KIDS_ENTERTAINMENT` |
| 11 | Widoczny kącik dla dzieci | `KIDS_CORNER_VISIBLE` |
| 12 | Zabawki sensoryczne | `SENSORY_TOYS` |
| 13 | Miejsce do karmienia | `BREASTFEEDING_AREA` |
| 14 | Parking | `PARKING` |
| 15 | Parking rodzinny | `FAMILY_PARKING` |
| 16 | Szerokie drzwi | `WIDE_DOORS` |
| 17 | Wi-Fi | `WIFI` |
| 18 | Ciche strefy | `QUIET_AREAS` |
| 19 | Oznaczenia przyjazne dzieciom | `KID_FRIENDLY_SIGNS` |

### Sala zabaw (`PLAY_ROOM`)

| Priorytet | Udogodnienie | Enum |
|---:|---|---|
| 1 | Strefy wiekowe | `AGE_ZONES` |
| 2 | Miękkie zabezpieczenia | `SOFT_PROTECTION` |
| 3 | Monitoring | `MONITORING` |
| 4 | Dezynfekcja zabawek | `TOY_SANITIZATION` |
| 5 | Toaleta | `TOILET` |
| 6 | Przewijak | `CHANGING_TABLE` |
| 7 | Dostęp dla wózków | `STROLLER_ACCESS` |
| 8 | Strefa malucha | `TODDLER_ZONE` |
| 9 | Animator | `ANIMATOR` |
| 10 | Strefa rodzica | `PARENT_ZONE` |
| 11 | Szafki | `LOCKERS` |
| 12 | Zabawki sensoryczne | `SENSORY_TOYS` |
| 13 | Parking | `PARKING` |
| 14 | Parking rodzinny | `FAMILY_PARKING` |
| 15 | Szerokie drzwi | `WIDE_DOORS` |
| 16 | Wi-Fi | `WIFI` |

### Park (`PARK`)

| Priorytet | Udogodnienie | Enum |
|---:|---|---|
| 1 | Bezpieczne ścieżki | `SAFE_PATHS` |
| 2 | Strefa bez aut | `CAR_FREE_AREA` |
| 3 | Toaleta | `TOILET` |
| 4 | Dostęp dla wózków | `STROLLER_ACCESS` |
| 5 | Przewijak | `CHANGING_TABLE` |
| 6 | Dobre oświetlenie | `GOOD_LIGHTING` |
| 7 | Ławki w cieniu | `SHADED_BENCHES` |
| 8 | Strefa piknikowa | `PICNIC_AREA` |
| 9 | Woda pitna | `DRINKING_WATER` |
| 10 | Miejsce do karmienia | `BREASTFEEDING_AREA` |
| 11 | Parking | `PARKING` |
| 12 | Oznaczenia przyjazne dzieciom | `KID_FRIENDLY_SIGNS` |

### Atrakcja (`ATTRACTION`)

| Priorytet | Udogodnienie | Enum |
|---:|---|---|
| 1 | Toaleta | `TOILET` |
| 2 | Przewijak | `CHANGING_TABLE` |
| 3 | Dostęp dla wózków | `STROLLER_ACCESS` |
| 4 | Pokój rodzica z dzieckiem | `PARENT_CHILD_ROOM` |
| 5 | Punkt zgubionego dziecka | `LOST_CHILD_POINT` |
| 6 | Strefy odpoczynku | `REST_AREAS` |
| 7 | Woda pitna | `DRINKING_WATER` |
| 8 | Szybka ścieżka rodzinna | `FAMILY_FAST_TRACK` |
| 9 | Wypożyczalnia wózków | `STROLLER_RENTAL` |
| 10 | Menu dla dzieci | `KIDS_MENU` |
| 11 | Miejsce do karmienia | `BREASTFEEDING_AREA` |
| 12 | Strefa rodzica | `PARENT_ZONE` |
| 13 | Szafki | `LOCKERS` |
| 14 | Parking | `PARKING` |
| 15 | Parking rodzinny | `FAMILY_PARKING` |
| 16 | Szerokie drzwi | `WIDE_DOORS` |
| 17 | Wi-Fi | `WIFI` |
| 18 | Ciche strefy | `QUIET_AREAS` |
| 19 | Oznaczenia przyjazne dzieciom | `KID_FRIENDLY_SIGNS` |

### Inne (`OTHER`)

| Priorytet | Udogodnienie | Enum |
|---:|---|---|
| 1 | Toaleta | `TOILET` |
| 2 | Przewijak | `CHANGING_TABLE` |
| 3 | Dostęp dla wózków | `STROLLER_ACCESS` |
| 4 | Mikrofalówka | `MICROWAVE` |
| 5 | Parking | `PARKING` |
| 6 | Parking rodzinny | `FAMILY_PARKING` |
| 7 | Szerokie drzwi | `WIDE_DOORS` |
| 8 | Wi-Fi | `WIFI` |
| 9 | Ciche strefy | `QUIET_AREAS` |

## Zasada dodawania nowych udogodnień

Przy dodaniu nowego udogodnienia trzeba wykonać trzy kroki:

1. Dodać wartość do `enum class Amenity`.
2. Przypisać `applicableCategories`.
3. Dodać tę wartość do `categoryPriorityMap` w odpowiednim miejscu priorytetu.

Jeśli udogodnienie nie trafi do `categoryPriorityMap`, nie będzie pokazane w formularzu `AddPlace`, nawet jeśli ma przypisaną kategorię w `applicableCategories`.
