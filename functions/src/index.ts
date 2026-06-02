import {onDocumentCreated, onDocumentDeleted} from "firebase-functions/v2/firestore";
import {defineSecret} from "firebase-functions/params";
import * as admin from "firebase-admin";
import * as nodemailer from "nodemailer";

admin.initializeApp();

const db = admin.firestore();

const gmailEmail = defineSecret("GMAIL_EMAIL");
const gmailPassword = defineSecret("GMAIL_PASSWORD");
const adminEmail = defineSecret("ADMIN_EMAIL");

// --- HTML Email Template ---
function wrapInTemplate(title: string, body: string): string {
  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    body { margin: 0; padding: 0; background: #f5f8fb; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
    .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; margin-top: 24px; margin-bottom: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    .header { background: linear-gradient(135deg, #1976D2, #42A5F5); padding: 24px; text-align: center; }
    .header h1 { color: #ffffff; margin: 0; font-size: 24px; }
    .header p { color: rgba(255,255,255,0.85); margin: 4px 0 0; font-size: 14px; }
    .body { padding: 24px; }
    .body h2 { color: #1976D2; margin-top: 0; font-size: 20px; }
    .body table { width: 100%; border-collapse: collapse; }
    .body table td { padding: 8px 12px; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
    .body table td:first-child { font-weight: 600; color: #555; white-space: nowrap; width: 140px; }
    .body ul { padding-left: 20px; }
    .body li { margin-bottom: 6px; color: #333; }
    .footer { padding: 16px 24px; background: #f5f8fb; text-align: center; font-size: 12px; color: #999; }
    .btn { display: inline-block; padding: 10px 20px; background: #1976D2; color: #fff !important; text-decoration: none; border-radius: 6px; margin-top: 12px; font-weight: 600; }
    pre { background: #f5f8fb; padding: 12px; border-radius: 6px; font-size: 13px; overflow-x: auto; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>kidZone</h1>
      <p>Mapa miejsc przyjaznych dzieciom</p>
    </div>
    <div class="body">
      <h2>${title}</h2>
      ${body}
    </div>
    <div class="footer">
      Ten email został wysłany automatycznie przez system kidZone.
    </div>
  </div>
</body>
</html>`;
}

// --- Helpers ---

async function getPlaceName(placeId: string): Promise<string> {
  try {
    const doc = await db.collection("places").doc(placeId).get();
    const name = doc.exists ? (doc.data()?.name || "Bez nazwy") : "Nieznane miejsce";
    return `${name} [${placeId}]`;
  } catch {
    return `Nieznane miejsce [${placeId}]`;
  }
}

async function getUserInfo(userId: string): Promise<string> {
  try {
    const doc = await db.collection("users").doc(userId).get();
    if (!doc.exists) return `Nieznany [${userId}]`;
    const data = doc.data();
    const name = data?.name || "";
    const email = data?.email || "";
    if (name && email) return `${name} (${email}) [${userId}]`;
    if (name) return `${name} [${userId}]`;
    if (email) return `${email} [${userId}]`;
    return `Nieznany [${userId}]`;
  } catch {
    return `Nieznany [${userId}]`;
  }
}

function mapReason(reason: string): string {
  const reasons: Record<string, string> = {
    "NOT_EXISTS": "Miejsce nie istnieje / zamknięte",
    "INAPPROPRIATE": "Nieodpowiednia treść (wulgaryzmy, reklama)",
    "DUPLICATE": "Duplikat innego miejsca",
    "FALSE_DATA": "Fałszywe dane (adres, udogodnienia)",
    "OTHER": "Inne",
  };
  const label = reasons[reason] || reason;
  return `${label} [${reason}]`;
}

function mapCategory(category: string): string {
  const categories: Record<string, string> = {
    "PLAYGROUND": "Plac zabaw",
    "PLAY_ROOM": "Sala zabaw",
    "CAFE": "Kawiarnia rodzinna",
    "RESTAURANT": "Restauracja",
    "PARK": "Park",
    "ATTRACTION": "Atrakcja",
    "OTHER": "Inne",
  };
  const label = categories[category] || category;
  return `${label} [${category}]`;
}

function mapAmenities(amenities: string[]): string {
  const amenityNames: Record<string, string> = {
    "CHANGING_TABLE": "Przewijak",
    "TOILET": "Czysta toaleta",
    "STROLLER_ACCESS": "Dostęp dla wózka",
    "PARKING": "Parking",
    "FENCING": "Ogrodzenie",
    "SOFT_SURFACE": "Miękka nawierzchnia",
    "SHADED_BENCHES": "Ławki w cieniu",
    "TODDLER_ZONE": "Strefa 0\u20133",
    "CAR_FREE_AREA": "Brak ruchu samochodowego",
    "KIDS_MENU": "Menu dziecięce",
    "HIGH_CHAIR": "Krzesełka do karmienia",
    "KIDS_TABLEWARE": "Naczynia dziecięce",
    "FAST_SERVICE": "Szybka obsługa",
    "KIDS_ENTERTAINMENT": "Kredki, zabawki",
    "KIDS_CORNER_VISIBLE": "Kącik widoczny od stolika",
    "AGE_ZONES": "Podział na strefy wiekowe",
    "ANIMATOR": "Animator",
    "MONITORING": "Monitoring",
    "TOY_SANITIZATION": "Dezynfekcja zabawek",
    "PARENT_ZONE": "Strefa dla rodziców",
    "LOCKERS": "Szafki na rzeczy",
    "SOFT_PROTECTION": "Miękkie zabezpieczenia",
    "QUIET_FEEDING": "Ciche miejsce do karmienia",
    "MICROWAVE": "Mikrofal\u00F3wka",
    "NO_LOUD_MUSIC": "Bez głośnej muzyki",
    "SENSORY_TOYS": "Zabawki sensoryczne",
    "PICNIC_AREA": "Strefa piknikowa",
    "SAFE_PATHS": "Bezpieczne alejki",
    "DRINKING_WATER": "Woda pitna",
    "BREASTFEEDING_AREA": "Miejsce do karmienia piersią",
    "GOOD_LIGHTING": "Dobre oświetlenie",
    "STROLLER_RENTAL": "Wypożyczalnia wózków",
    "REST_AREAS": "Strefy odpoczynku",
    "FAMILY_FAST_TRACK": "Priorytet dla rodzin",
    "PARENT_CHILD_ROOM": "Pokój rodzic + dziecko",
    "LOST_CHILD_POINT": "Punkt zgubionych dzieci",
    "WIDE_DOORS": "Szerokie drzwi",
    "FAMILY_PARKING": "Miejsca parkingowe family",
    "KID_FRIENDLY_SIGNS": "Oznaczenia dla dzieci",
    "WIFI": "WiFi",
    "QUIET_AREAS": "Strefy ciszy",
  };
  return amenities.map((a) => `${amenityNames[a] || a} [${a}]`).join(", ");
}

// --- Trigger: zgłoszenie naruszenia ---
export const onPlaceReport = onDocumentCreated(
  {
    document: "place_reports/{reportId}",
    secrets: [gmailEmail, gmailPassword, adminEmail],
  },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const placeId = data.placeId || "";
    const reporterId = data.reporterId || "";

    const [placeInfo, reporterInfo] = await Promise.all([
      getPlaceName(placeId),
      getUserInfo(reporterId),
    ]);

    const reason = mapReason(data.reason || "");
    const comment = data.comment || "";
    const projectId = process.env.GCLOUD_PROJECT || "playground-705e7162";
    const firestoreUrl =
      `https://console.firebase.google.com/project/${projectId}/firestore/data/place_reports/${event.params.reportId}`;

    const html = wrapInTemplate("Nowe zgłoszenie naruszenia", `
      <table>
        <tr><td>Miejsce:</td><td>${placeInfo}</td></tr>
        <tr><td>Powód:</td><td>${reason}</td></tr>
        <tr><td>Komentarz:</td><td>${comment || "(brak)"}</td></tr>
        <tr><td>Zgłaszający:</td><td>${reporterInfo}</td></tr>
      </table>
      <p><a class="btn" href="${firestoreUrl}">Otwórz w Firebase Console</a></p>
    `);

    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
    });

    await transporter.sendMail({
      from: `kidZone <${gmailEmail.value()}>`,
      to: adminEmail.value(),
      subject: `[kidZone] Zgłoszenie: ${placeInfo}`,
      html,
    });

    console.log(`Email sent for report ${event.params.reportId}`);
  }
);

// --- Trigger: propozycja zmiany ---
export const onPlaceChangeRequest = onDocumentCreated(
  {
    document: "place_change_requests/{requestId}",
    secrets: [gmailEmail, gmailPassword, adminEmail],
  },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const placeId = data.placeId || "";
    const requesterId = data.requesterId || "";
    const type = data.type || "EDIT";
    const changes = data.changes || {};

    const [placeInfo, requesterInfo] = await Promise.all([
      getPlaceName(placeId),
      getUserInfo(requesterId),
    ]);

    const projectId = process.env.GCLOUD_PROJECT || "playground-705e7162";
    const firestoreUrl =
      `https://console.firebase.google.com/project/${projectId}/firestore/data/place_change_requests/${event.params.requestId}`;

    const typeLabel = type === "LOCATION" ? "Korekta lokalizacji" : "Zmiana danych";

    let changesHtml = "";
    for (const [key, value] of Object.entries(changes)) {
      let displayValue: string;
      if (key === "category") {
        displayValue = mapCategory(value as string);
      } else if (key === "amenities" && Array.isArray(value)) {
        displayValue = mapAmenities(value as string[]);
      } else {
        displayValue = String(value);
      }
      const keyLabel: Record<string, string> = {
        "name": "Nazwa",
        "description": "Opis",
        "category": "Kategoria",
        "amenities": "Udogodnienia",
        "latitude": "Szerokość geo.",
        "longitude": "Długość geo.",
        "address": "Adres",
      };
      changesHtml += `<tr><td>${keyLabel[key] || key}:</td><td>${displayValue}</td></tr>`;
    }

    const html = wrapInTemplate(`${typeLabel}`, `
      <table>
        <tr><td>Miejsce:</td><td>${placeInfo}</td></tr>
        <tr><td>Zgłaszający:</td><td>${requesterInfo}</td></tr>
      </table>
      <h3 style="color:#1976D2; margin-top:16px;">Proponowane zmiany:</h3>
      <table>
        ${changesHtml}
      </table>
      <p><a class="btn" href="${firestoreUrl}">Otwórz w Firebase Console</a></p>
    `);

    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
    });

    await transporter.sendMail({
      from: `kidZone <${gmailEmail.value()}>`,
      to: adminEmail.value(),
      subject: `[kidZone] ${typeLabel}: ${placeInfo}`,
      html,
    });

    console.log(`Email sent for change request ${event.params.requestId}`);
  }
);

// --- Trigger: nowy użytkownik (email powitalny) ---
export const onUserCreated = onDocumentCreated(
  {
    document: "users/{userId}",
    secrets: [gmailEmail, gmailPassword, adminEmail],
  },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const userName = data.name || "Użytkowniku";
    const userEmail = data.email;
    if (!userEmail) return;

    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
    });

    // Email powitalny do użytkownika
    const welcomeHtml = wrapInTemplate(`Cześć, ${userName}!`, `
      <p>Dziękujemy za dołączenie do społeczności kidZone.</p>
      <p>Teraz możesz:</p>
      <ul>
        <li>Odkrywać miejsca przyjazne dzieciom w okolicy</li>
        <li>Dodawać własne miejsca i dzielić się z innymi rodzicami</li>
        <li>Wystawiać opinie i zdobywać odznaki</li>
      </ul>
      <p>Miłego odkrywania! 🚀</p>
    `);

    await transporter.sendMail({
      from: `kidZone <${gmailEmail.value()}>`,
      to: userEmail,
      subject: "Witaj w kidZone! 🐻",
      html: welcomeHtml,
    });

    // Powiadomienie do admina
    const adminHtml = wrapInTemplate("Nowa rejestracja", `
      <table>
        <tr><td>Nazwa:</td><td>${userName}</td></tr>
        <tr><td>Email:</td><td>${userEmail}</td></tr>
        <tr><td>UID:</td><td>${event.params.userId}</td></tr>
      </table>
    `);

    await transporter.sendMail({
      from: `kidZone <${gmailEmail.value()}>`,
      to: adminEmail.value(),
      subject: `[kidZone] Nowy użytkownik: ${userName}`,
      html: adminHtml,
    });

    console.log(`Welcome email sent to ${userEmail}`);
  }
);

// --- Trigger: użytkownik usunął konto ---
export const onUserDeleted = onDocumentDeleted(
  {
    document: "users/{userId}",
    secrets: [gmailEmail, gmailPassword, adminEmail],
  },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const userName = data.name || "Użytkownik";
    const userEmail = data.email;

    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
    });

    // Email do użytkownika
    if (userEmail) {
      const farewellHtml = wrapInTemplate(`Żegnaj, ${userName}!`, `
        <p>Twoje konto w kidZone zostało pomyślnie usunięte.</p>
        <p>Usunięto również:</p>
        <ul>
          <li>Wszystkie Twoje miejsca</li>
          <li>Wszystkie Twoje opinie</li>
          <li>Twoje zdjęcie profilowe</li>
        </ul>
        <p>Jeśli zmienisz zdanie, zawsze możesz założyć nowe konto.</p>
      `);

      await transporter.sendMail({
        from: `kidZone <${gmailEmail.value()}>`,
        to: userEmail,
        subject: "Twoje konto kidZone zostało usunięte",
        html: farewellHtml,
      });
    }

    // Powiadomienie do admina
    const adminHtml = wrapInTemplate("Użytkownik usunął konto", `
      <table>
        <tr><td>Nazwa:</td><td>${userName}</td></tr>
        <tr><td>Email:</td><td>${userEmail || "(brak)"}</td></tr>
        <tr><td>UID:</td><td>${event.params.userId}</td></tr>
      </table>
    `);

    await transporter.sendMail({
      from: `kidZone <${gmailEmail.value()}>`,
      to: adminEmail.value(),
      subject: `[kidZone] Konto usunięte: ${userName}`,
      html: adminHtml,
    });

    console.log(`Account deletion email sent for ${event.params.userId}`);
  }
);
