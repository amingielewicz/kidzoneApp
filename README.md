# kidZone 🐻

Społecznościowa aplikacja mobilna dla rodziców — odkrywaj, dodawaj i oceniaj miejsca przyjazne dzieciom.

## 📱 Funkcje

### Dla użytkowników:
- 🗺️ Mapa miejsc przyjaznych dzieciom w okolicy (Google Maps)
- 📍 Dodawanie nowych miejsc z kategoriami i udogodnieniami
- ⭐ Opinie i oceny (1-5 gwiazdek + komentarz + zdjęcia)
- 🏆 System odznak i rankingów (użytkownicy + miejsca)
- 📷 Galeria zdjęć miejsc
- 🔔 Powiadomienia push (nowa opinia, nowe zdjęcie, ranking)
- 👤 Profil użytkownika z edycją danych
- 🔍 Wyszukiwanie miejsc
- 📊 Ranking TOP 10 użytkowników i miejsc
- 🚨 Zgłaszanie naruszeń (miejsca, opinie, zdjęcia)
- 💡 Propozycje zmian w danych miejsc

### Panel administracyjny (React):
- 📊 Dashboard ze statystykami
- 🚨 Zarządzanie zgłoszeniami (miejsca, opinie, zdjęcia)
- 📝 Zatwierdzanie/odrzucanie propozycji zmian
- 🏠 Zarządzanie miejscami (edycja, usuwanie z powodem)
- 👥 Zarządzanie użytkownikami (edycja, blokowanie, resetowanie hasła)
- 🔒 Pełne zabezpieczenie — auth check na Cloud Functions

## 🛠️ Tech Stack

### Android:
- Kotlin + Jetpack Compose
- Hilt (Dependency Injection)
- Firebase Auth + Firestore + Storage + Crashlytics + FCM + App Check
- Google Maps SDK
- Room (cache)
- Material Design 3

### Panel admina:
- React 19 + TypeScript
- Vite 6
- Material UI 6
- Firebase SDK 11
- React Router 7

### Backend:
- Firebase Cloud Functions (Node.js/TypeScript)
- Nodemailer (email notifications)
- Firebase Hosting (SPA + static pages)

## 🚀 Setup

### Android:
1. Skopiuj `google-services.json.template` do `google-services.json` i uzupełnij
2. W `local.properties` dodaj: `MAPS_API_KEY=AIza...`
3. Build: `./gradlew assembleDebug`

### Panel admina:
```bash
cd admin-panel
npm install
cp .env.example .env  # uzupełnij klucze Firebase
npm run dev
```

### Cloud Functions:
```bash
cd functions
npm install
npm run build
```

### Deploy Firebase:
```bash
firebase target:apply hosting app playground-705e7162
firebase deploy --only functions,firestore:rules,storage,hosting:app
```

## 🔒 Bezpieczeństwo

- Firebase App Check (Play Integrity + reCAPTCHA Enterprise)
- Auth verification na wszystkich admin Cloud Functions
- Input sanitization (escapeHtml) w emailach
- Firestore Security Rules z walidacją typów
- Storage Rules z limitami rozmiaru i MIME
- CSP headers na hostingu
- ProGuard/R8 w release
- allowBackup=false
- Network Security Config (no cleartext)
- 1 zgłoszenie per user per target (duplicate prevention)

## 📁 Struktura projektu

```
├── app/                    # Android app (Kotlin/Compose)
│   ├── src/main/java/com/kidzone/
│   │   ├── data/           # Repository implementations, DTOs
│   │   ├── domain/         # Models, Repository interfaces
│   │   ├── presentation/   # UI (Compose screens, ViewModels)
│   │   ├── messaging/      # FCM service
│   │   └── utils/          # Helpers, exceptions
│   └── build.gradle.kts
├── admin-panel/            # React admin panel
│   ├── src/
│   │   ├── pages/          # Dashboard, Reports, Places, Users
│   │   ├── components/     # Layout, shared components
│   │   ├── hooks/          # useAuth
│   │   ├── services/       # Firebase config, API helpers
│   │   └── types/          # TypeScript interfaces
│   └── package.json
├── functions/              # Cloud Functions
│   └── src/index.ts        # Triggers + HTTP endpoints
├── public/                 # Firebase Hosting (static)
│   ├── privacy-policy.html
│   └── terms-of-service.html
├── firestore.rules
├── firestore.indexes.json
├── storage.rules
└── firebase.json
```

## 📧 Cloud Functions

| Trigger | Opis |
|---------|------|
| onPlaceReport | Email do admina o nowym zgłoszeniu miejsca |
| onPlaceChangeRequest | Email o propozycji zmiany |
| onUserCreated | Email powitalny + powiadomienie admina |
| onReviewReport | Email o zgłoszeniu opinii |
| onPhotoReport | Email o zgłoszeniu zdjęcia + przyciski akcji |
| onUserDeleted | Email pożegnalny + powiadomienie admina |
| onReviewCreatedPush | Push do właściciela miejsca o nowej opinii |
| onBadgeEarned | Push o nowej odznace |
| onPhotoAddedToPlace | Push o nowym zdjęciu |
| onUserBanned | Push + email o blokadzie konta |
| dailyRankingCheck | Push o awansie w rankingu (scheduled) |
| adminDeletePlace | HTTP: usuń miejsce + email z powodem |
| adminDeleteReview | HTTP: usuń opinię + email z powodem |
| adminDeletePhoto | HTTP: usuń zgłoszone zdjęcie + email |
| adminDeletePhotoFromPlace | HTTP: usuń zdjęcie z miejsca + email |
| adminDeleteUser | HTTP: usuń użytkownika + email z powodem |
| adminDismissPhotoReport | HTTP: odrzuć zgłoszenie zdjęcia |

## 📋 Regulamin i Polityka Prywatności

- Regulamin: `/terms-of-service`
- Polityka prywatności: `/privacy-policy`

## 💰 Koszty

| Usługa | Plan | Limit free |
|--------|------|-----------|
| Firebase Auth | Spark | 50k MAU |
| Firestore | Spark | 50k reads/20k writes/day |
| Cloud Functions | Spark | 2M invocations/month |
| Firebase Storage | Spark | 5GB |
| Firebase Hosting | Spark | 10GB storage, 360MB/day |
| Google Maps SDK | — | $200/month credit (~28k loads) |
| Play Integrity | — | 10k requests/day |

## 📄 Licencja

Projekt prywatny.
