import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {defineSecret} from "firebase-functions/params";
import * as admin from "firebase-admin";
import * as nodemailer from "nodemailer";

admin.initializeApp();

const db = admin.firestore();

const gmailEmail = defineSecret("GMAIL_EMAIL");
const gmailPassword = defineSecret("GMAIL_PASSWORD");
const adminEmail = defineSecret("ADMIN_EMAIL");

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
    "NOT_EXISTS": "Miejsce nie istnieje / zamkniete",
    "INAPPROPRIATE": "Nieodpowiednia tresc (wulgaryzmy, reklama)",
    "DUPLICATE": "Duplikat innego miejsca",
    "FALSE_DATA": "Falszywe dane (adres, udogodnienia)",
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
    "STROLLER_ACCESS": "Dostep dla wozka",
    "PARKING": "Parking",
    "FENCING": "Ogrodzenie",
    "SOFT_SURFACE": "Miekka nawierzchnia",
    "SHADED_BENCHES": "Lawki w cieniu",
    "TODDLER_ZONE": "Strefa 0-3",
    "CAR_FREE_AREA": "Brak ruchu samochodowego",
    "KIDS_MENU": "Menu dzieciece",
    "HIGH_CHAIR": "Krzesełka do karmienia",
    "KIDS_TABLEWARE": "Naczynia dzieciece",
    "FAST_SERVICE": "Szybka obsluga",
    "KIDS_ENTERTAINMENT": "Kredki, zabawki",
    "KIDS_CORNER_VISIBLE": "Kacik widoczny od stolika",
    "AGE_ZONES": "Podzial na strefy wiekowe",
    "ANIMATOR": "Animator",
    "MONITORING": "Monitoring",
    "TOY_SANITIZATION": "Dezynfekcja zabawek",
    "PARENT_ZONE": "Strefa dla rodzicow",
    "LOCKERS": "Szafki na rzeczy",
    "SOFT_PROTECTION": "Miekkie zabezpieczenia",
    "QUIET_FEEDING": "Ciche miejsce do karmienia",
    "MICROWAVE": "Mikrofalowka",
    "NO_LOUD_MUSIC": "Bez glosnej muzyki",
    "SENSORY_TOYS": "Zabawki sensoryczne",
    "PICNIC_AREA": "Strefa piknikowa",
    "SAFE_PATHS": "Bezpieczne alejki",
    "DRINKING_WATER": "Woda pitna",
    "BREASTFEEDING_AREA": "Miejsce do karmienia piersia",
    "GOOD_LIGHTING": "Dobre oswietlenie",
    "STROLLER_RENTAL": "Wypozyczalnia wozkow",
    "REST_AREAS": "Strefy odpoczynku",
    "FAMILY_FAST_TRACK": "Priorytet dla rodzin",
    "PARENT_CHILD_ROOM": "Pokoj rodzic + dziecko",
    "LOST_CHILD_POINT": "Punkt zgubionych dzieci",
    "WIDE_DOORS": "Szerokie drzwi",
    "FAMILY_PARKING": "Miejsca parkingowe family",
    "KID_FRIENDLY_SIGNS": "Oznaczenia dla dzieci",
    "WIFI": "WiFi",
    "QUIET_AREAS": "Strefy ciszy",
  };
  return amenities.map((a) => `${amenityNames[a] || a} [${a}]`).join(", ");
}

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

    const subject = `[kidZone] Zgłoszenie: ${placeInfo}`;
    const html = `
      <h2>Nowe zgłoszenie naruszenia</h2>
      <table style="border-collapse:collapse; font-family:sans-serif;">
        <tr><td style="padding:4px 12px;"><strong>Miejsce:</strong></td><td>${placeInfo}</td></tr>
        <tr><td style="padding:4px 12px;"><strong>Powód:</strong></td><td>${reason}</td></tr>
        <tr><td style="padding:4px 12px;"><strong>Komentarz:</strong></td><td>${comment || "(brak)"}</td></tr>
        <tr><td style="padding:4px 12px;"><strong>Zgłaszający:</strong></td><td>${reporterInfo}</td></tr>
      </table>
      <br>
      <p><a href="${firestoreUrl}">Otwórz w Firebase Console</a></p>
    `;

    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
    });

    await transporter.sendMail({
      from: `kidZone <${gmailEmail.value()}>`,
      to: adminEmail.value(),
      subject,
      html,
    });

    console.log(`Email sent for report ${event.params.reportId}`);
  }
);

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
      changesHtml += `<tr><td style="padding:4px 12px;"><strong>${keyLabel[key] || key}:</strong></td><td>${displayValue}</td></tr>`;
    }

    const subject = `[kidZone] ${typeLabel}: ${placeInfo}`;
    const html = `
      <h2>${typeLabel}</h2>
      <table style="border-collapse:collapse; font-family:sans-serif;">
        <tr><td style="padding:4px 12px;"><strong>Miejsce:</strong></td><td>${placeInfo}</td></tr>
        <tr><td style="padding:4px 12px;"><strong>Zgłaszający:</strong></td><td>${requesterInfo}</td></tr>
      </table>
      <h3>Proponowane zmiany:</h3>
      <table style="border-collapse:collapse; font-family:sans-serif;">
        ${changesHtml}
      </table>
      <br>
      <p><a href="${firestoreUrl}">Otwórz w Firebase Console</a></p>
    `;

    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
    });

    await transporter.sendMail({
      from: `kidZone <${gmailEmail.value()}>`,
      to: adminEmail.value(),
      subject,
      html,
    });

    console.log(`Email sent for change request ${event.params.requestId}`);
  }
);
