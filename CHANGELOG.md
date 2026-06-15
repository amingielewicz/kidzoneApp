# Changelog

Wszystkie istotne zmiany w projekcie kidZone.

Format oparty na [Keep a Changelog](https://keepachangelog.com/pl/1.0.0/),
a wersjonowanie zgodne z [Semantic Versioning](https://semver.org/lang/pl/).

## [1.0.0] — 2026-06-15

### Dodane
- Mapa miejsc przyjaznych dzieciom (Google Maps SDK)
- Dodawanie nowych miejsc z 7 kategoriami i 40+ udogodnieniami
- Opinie i oceny (1-5 gwiazdek + komentarz + zdjecia)
- System odznak i rankingów (TOP 10 użytkowników i miejsc)
- Galeria zdjęć miejsc (upload, kompresja WebP)
- Powiadomienia push (FCM): nowa opinia, nowe zdjęcie, nowa odznaka, ranking
- Profil użytkownika z edycją danych i ustawieniami powiadomień
- Wyszukiwanie miejsc po nazwie
- Zgłaszanie naruszeń (miejsca, opinie, zdjęcia)
- Propozycje zmian w danych miejsc
- Widget "Miejsca w pobliżu" na ekranie głównym (Glance AppWidget)
- In-App Update (automatyczne powiadomienie o nowej wersji)
- In-App Review (zachęta do oceny w Google Play)
- Tryb offline (Room cache + WorkManager sync queue)
- Deep linking (https + custom scheme `kidzone://`)
- A/B Testing framework (Remote Config + Analytics)
- Maintenance mode (Remote Config gate)
- Onboarding (pierwszy uruchomienie)
- Panel administracyjny (React 19 + Material UI 6)
- Cloud Functions (18 triggerów: email, push, CRUD admin)
- Firestore Security Rules z walidacją typów i ownership
- Storage Rules z limitami rozmiaru i MIME
- Firebase App Check (Play Integrity + reCAPTCHA Enterprise)
- CI/CD: Android, Functions, Admin Panel, Firestore Rules Tests
- Staging environment z osobnym projektem Firebase
- Paginacja server-side (cursor-based) na liście miejsc
- Content Security Policy headers na hostingu
- Release signing config (keystore z local.properties / env vars)

### Bezpieczeństwo
- Firebase App Check (Play Integrity)
- Auth verification na wszystkich admin Cloud Functions
- Input sanitization (escapeHtml) w emailach
- ProGuard/R8 z minifikacją i shrink resources
- allowBackup=false
- Network Security Config (no cleartext HTTP)
- 1 zgłoszenie per user per target (duplicate prevention)
- CSP headers na Firebase Hosting

### Poprawki
- R8 missing classes fix dla play-services-location i play-core-ktx
- Zachowanie pozycji scrollu listy przy powrocie ze szczegółów miejsca
- Auto-scroll profilu do sekcji odznak po push notification

---

_Format wpisów w przyszłych wersjach:_

```
## [X.Y.Z] — YYYY-MM-DD

### Dodane
- Nowe funkcje

### Zmienione
- Zmiany w istniejących funkcjach

### Poprawki
- Naprawione bugi

### Usunięte
- Usunięte funkcje

### Bezpieczeństwo
- Poprawki bezpieczeństwa
```
