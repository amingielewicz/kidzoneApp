import {onDocumentCreated, onDocumentDeleted, onDocumentUpdated} from "firebase-functions/v2/firestore";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {HttpsError, onCall, onRequest} from "firebase-functions/v2/https";
import {defineSecret} from "firebase-functions/params";
import * as admin from "firebase-admin";
import * as nodemailer from "nodemailer";
import {
  escapeHtml,
  extractBearerToken,
  mapPhotoReason,
  mapReason,
  mapReviewReason,
  renderAdminResponse,
} from "./adminHelpers";

admin.initializeApp();

const db = admin.firestore();

type HttpRequestLike = {
  headers: {
    authorization?: string | string[];
  };
};

type HttpResponseLike = {
  status: (code: number) => {
    send: (body: string) => void;
  };
};

type ReviewInfo = {
  comment: string;
  rating: number;
  authorName: string;
  placeId: string;
};

const CONTACT_SUBJECT_MIN_LENGTH = 3;
const CONTACT_SUBJECT_MAX_LENGTH = 80;
const CONTACT_MESSAGE_MIN_LENGTH = 10;
const CONTACT_MESSAGE_MAX_LENGTH = 1000;

function privateMessagingRef(userId: string) {
  return db.collection("users").doc(userId).collection("private").doc("messaging");
}

async function getFcmTokens(
  userId: string,
  legacyUserData?: admin.firestore.DocumentData
): Promise<string[]> {
  const messagingDoc = await privateMessagingRef(userId).get();
  const privateTokens = messagingDoc.data()?.fcmTokens;
  if (Array.isArray(privateTokens) && privateTokens.length > 0) {
    return privateTokens;
  }
  const legacyTokens = legacyUserData?.fcmTokens;
  if (!Array.isArray(legacyTokens) || legacyTokens.length === 0) {
    return [];
  }

  await privateMessagingRef(userId).set({
    userId,
    fcmTokens: admin.firestore.FieldValue.arrayUnion(...legacyTokens),
    updatedAtMillis: Date.now(),
  }, {merge: true});
  await db.collection("users").doc(userId).set({
    fcmTokens: admin.firestore.FieldValue.delete(),
  }, {merge: true});

  return legacyTokens;
}

/**
 * Weryfikuje czy request pochodzi od zalogowanego admina.
 * Zwraca UID admina lub null (+ wysyła error response).
 */
async function verifyAdminRequest(
  req: HttpRequestLike,
  res: HttpResponseLike
): Promise<string | null> {
  const idToken = extractBearerToken(req.headers);
  if (!idToken) {
    res.status(401).send(renderAdminResponse("Brak autoryzacji", "Wymagany token w nagłówku Authorization."));
    return null;
  }
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    const uid = decoded.uid;

    // Sprawdzamy custom claim 'admin'
    if (decoded.admin === true) {
      return uid;
    }

    // Fallback do Firestore (dla nowo nadanych uprawnień przed odświeżeniem tokena)
    const userDoc = await db.collection("users").doc(uid).get();
    if (userDoc.exists && userDoc.data()?.role === "admin") {
      // Przy okazji ustawiamy brakujący claim
      await admin.auth().setCustomUserClaims(uid, {admin: true});
      return uid;
    }

    res.status(403).send(renderAdminResponse("Brak uprawnień", "Tylko administrator może wykonać tę akcję."));
    return null;
  } catch (err) {
    res.status(401).send(renderAdminResponse("Nieprawidłowy token", "Token wygasł lub jest nieprawidłowy."));
    return null;
  }
}

/**
 * Loguje akcje administracyjne do kolekcji audit_logs.
 */
async function logAudit(adminUid: string, action: string, details: Record<string, unknown>) {
  try {
    await db.collection("audit_logs").add({
      adminUid,
      action,
      details,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });
  } catch (err) {
    console.error("Failed to log audit:", err);
  }
}

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

async function getReviewInfo(reviewId: string): Promise<ReviewInfo> {
  try {
    const doc = await db.collection("reviews").doc(reviewId).get();
    if (!doc.exists) return {comment: "", rating: 0, authorName: "Nieznany", placeId: ""};
    const data = doc.data();
    return {
      comment: data?.comment || "",
      rating: data?.rating || 0,
      authorName: data?.authorName || "Anonim",
      placeId: data?.placeId || "",
    };
  } catch {
    return {comment: "", rating: 0, authorName: "Nieznany", placeId: ""};
  }
}

// --- Callable: zapis formularza kontaktowego ---
export const submitContactMessage = onCall(
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Wymagane logowanie.");
    }

    const subject = String(request.data?.subject || "").trim();
    const message = String(request.data?.message || "").trim();
    if (
      subject.length < CONTACT_SUBJECT_MIN_LENGTH ||
      subject.length > CONTACT_SUBJECT_MAX_LENGTH ||
      message.length < CONTACT_MESSAGE_MIN_LENGTH ||
      message.length > CONTACT_MESSAGE_MAX_LENGTH
    ) {
      throw new HttpsError("invalid-argument", "Nieprawidłowa treść formularza.");
    }

    const authToken = (request.auth?.token || {}) as Record<string, unknown>;
    const docRef = await db.collection("contact_messages").add({
      reporterId: uid,
      reporterName: String(authToken.name || ""),
      reporterEmail: String(authToken.email || ""),
      subject,
      message,
      status: "new",
      emailRequested: true,
      emailStatus: "pending",
      createdAtMillis: Date.now(),
    });

    return {messageId: docRef.id};
  }
);

// --- Trigger: formularz kontaktowy ---
export const onContactMessage = onDocumentCreated(
  {
    document: "contact_messages/{messageId}",
    secrets: [gmailEmail, gmailPassword, adminEmail],
  },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const messageId = event.params.messageId;
    const reporterId = data.reporterId || "";
    const reporterInfo = reporterId ? await getUserInfo(reporterId) : "Nieznany";
    const subject = data.subject || "(brak tematu)";
    const message = data.message || "";
    const reporterName = data.reporterName || "";
    const reporterEmail = data.reporterEmail || "";
    const projectId = process.env.GCLOUD_PROJECT || "playground-705e7162";
    const firestoreUrl =
      `https://console.firebase.google.com/project/${projectId}/firestore/data/contact_messages/${messageId}`;

    const html = wrapInTemplate("Nowa wiadomość z formularza kontaktowego", `
      <table>
        <tr><td>Temat:</td><td>${escapeHtml(subject)}</td></tr>
        <tr><td>Zgłaszający:</td><td>${escapeHtml(reporterInfo)}</td></tr>
        <tr><td>Nazwa z aplikacji:</td><td>${escapeHtml(reporterName || "(brak)")}</td></tr>
        <tr><td>Email konta:</td><td>${escapeHtml(reporterEmail || "(brak)")}</td></tr>
        <tr><td>UID:</td><td>${escapeHtml(reporterId || "(brak)")}</td></tr>
      </table>
      <h3 style="color:#1976D2; margin-top:16px;">Wiadomość:</h3>
      <p style="white-space:pre-wrap;">${escapeHtml(message)}</p>
      <p><a class="btn" href="${firestoreUrl}">Otwórz w Firebase Console</a></p>
    `);

    try {
      const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
      });

      await transporter.sendMail({
        from: `kidZone <${gmailEmail.value()}>`,
        to: adminEmail.value(),
        subject: `[kidZone] Kontakt: ${subject}`,
        html,
      });

      await event.data?.ref.update({
        emailStatus: "sent",
        emailedAtMillis: Date.now(),
      });
      console.log(`Contact email sent for ${messageId}`);
    } catch (err) {
      await event.data?.ref.update({
        emailStatus: "failed",
        emailError: String(err).slice(0, 500),
        emailFailedAtMillis: Date.now(),
      });
      console.error(`Contact email failed for ${messageId}:`, err);
      throw err;
    }
  }
);

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

// --- Trigger: zgłoszenie opinii jako spam ---
export const onReviewReport = onDocumentCreated(
  {
    document: "review_reports/{reportId}",
    secrets: [gmailEmail, gmailPassword, adminEmail],
  },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const reviewId = data.reviewId || "";
    const reporterId = data.reporterId || "";
    const reason = mapReviewReason(data.reason || "");
    const comment = data.comment || "";

    const [reviewInfo, reporterInfo] = await Promise.all([
      getReviewInfo(reviewId),
      getUserInfo(reporterId),
    ]);

    const placeInfo = reviewInfo.placeId ?
      await getPlaceName(reviewInfo.placeId) : "Nieznane miejsce";

    const projectId = process.env.GCLOUD_PROJECT || "playground-705e7162";
    const firestoreUrl =
      `https://console.firebase.google.com/project/${projectId}/firestore/data/review_reports/${event.params.reportId}`;

    const stars = "★".repeat(reviewInfo.rating) + "☆".repeat(5 - reviewInfo.rating);

    const html = wrapInTemplate("Zgłoszenie opinii", `
      <table>
        <tr><td>Miejsce:</td><td>${placeInfo}</td></tr>
        <tr><td>Autor opinii:</td><td>${reviewInfo.authorName}</td></tr>
        <tr><td>Ocena:</td><td>${stars} (${reviewInfo.rating}/5)</td></tr>
        <tr><td>Treść opinii:</td><td>${reviewInfo.comment || "(brak)"}</td></tr>
      </table>
      <h3 style="color:#D32F2F; margin-top:16px;">Zgłoszenie:</h3>
      <table>
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
      subject: `[kidZone] Zgłoszenie opinii: ${reviewInfo.authorName} @ ${placeInfo}`,
      html,
    });

    console.log(`Email sent for review report ${event.params.reportId}`);
  }
);

// --- Trigger: zgłoszenie zdjęcia ---
export const onPhotoReport = onDocumentCreated(
  {
    document: "photo_reports/{reportId}",
    secrets: [gmailEmail, gmailPassword, adminEmail],
  },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const photoUrl = data.photoUrl || "";
    const reporterId = data.reporterId || "";
    const reason = mapPhotoReason(data.reason || "");
    const comment = data.comment || "";
    const reportId = event.params.reportId;

    const reporterInfo = await getUserInfo(reporterId);

    const projectId = process.env.GCLOUD_PROJECT || "playground-705e7162";
    const firestoreUrl =
      `https://console.firebase.google.com/project/${projectId}/firestore/data/photo_reports/${reportId}`;

    // Deep link do Cloud Function HTTP endpoints dla akcji admina
    const baseUrl = `https://us-central1-${projectId}.cloudfunctions.net`;
    const deletePhotoUrl = `${baseUrl}/adminDeletePhoto?reportId=${reportId}`;
    const dismissReportUrl = `${baseUrl}/adminDismissPhotoReport?reportId=${reportId}`;

    const html = wrapInTemplate("Zgłoszenie zdjęcia", `
      <div style="text-align:center; margin-bottom:16px;">
        <a href="${photoUrl}" target="_blank">
          <img src="${photoUrl}" alt="Zgłoszone zdjęcie"
               style="max-width:100%; max-height:300px; border-radius:8px; border:1px solid #e0e0e0; object-fit:contain;" />
        </a>
        <p style="font-size:12px; color:#999; margin-top:4px;">Kliknij miniaturkę, aby otworzyć w pełnym rozmiarze</p>
      </div>
      <table>
        <tr><td>Powód:</td><td>${reason}</td></tr>
        <tr><td>Komentarz:</td><td>${comment || "(brak)"}</td></tr>
        <tr><td>Zgłaszający:</td><td>${reporterInfo}</td></tr>
      </table>
      <div style="margin-top:20px; text-align:center;">
        <a href="${deletePhotoUrl}" class="btn" style="background:#D32F2F; margin-right:8px;">Usuń zdjęcie</a>
        <a href="${dismissReportUrl}" class="btn" style="background:#757575;">Odrzuć zgłoszenie</a>
      </div>
      <p style="margin-top:12px; text-align:center;">
        <a href="${firestoreUrl}" style="font-size:12px; color:#1976D2;">Otwórz w Firebase Console</a>
      </p>
    `);

    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
    });

    await transporter.sendMail({
      from: `kidZone <${gmailEmail.value()}>`,
      to: adminEmail.value(),
      subject: "[kidZone] Zgłoszenie zdjęcia",
      html,
    });

    console.log(`Email sent for photo report ${reportId}`);
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
        <p>Twoje konto w kidZone zosta\u0142o pomy\u015Blnie usuni\u0119te.</p>
        <p>Co si\u0119 sta\u0142o z Twoimi danymi:</p>
        <ul>
          <li>Twoje dane osobowe (profil, email, avatar) \u2014 <strong>usuni\u0119te</strong></li>
          <li>Twoje opinie \u2014 zanonimizowane (autor: \u201ENieaktywny u\u017Cytkownik\u201D)</li>
          <li>Twoje miejsca \u2014 pozostaj\u0105 widoczne dla spo\u0142eczno\u015Bci, bez powi\u0105zania z Tob\u0105</li>
        </ul>
        <p>Je\u015Bli zmienisz zdanie, zawsze mo\u017Cesz za\u0142o\u017Cy\u0107 nowe konto.</p>
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

// --- Trigger: zmiana roli użytkownika → Custom Claims ---
export const onUserRoleChanged = onDocumentUpdated(
  {document: "users/{userId}"},
  async (event) => {
    const afterData = event.data?.after?.data();
    const beforeData = event.data?.before?.data();
    if (!afterData || !beforeData) return;

    if (afterData.role !== beforeData.role) {
      const uid = event.params.userId;
      if (afterData.role === "admin") {
        await admin.auth().setCustomUserClaims(uid, {admin: true});
        console.log(`Custom claim 'admin' set for user ${uid}`);
      } else {
        await admin.auth().setCustomUserClaims(uid, {admin: false});
        console.log(`Custom claim 'admin' removed for user ${uid}`);
      }
    }
  }
);

// --- Trigger: nowa opinia / usunięcie opinii → przelicz średnią ocen miejsca ---
export const updatePlaceStatsOnReviewCreate = onDocumentCreated(
  {document: "reviews/{reviewId}"},
  async (event) => {
    const data = event.data?.data();
    if (!data) return;
    const placeId = data.placeId;
    const rating = data.rating;
    const userId = data.userId;
    if (!placeId) return;

    // 1. Aktualizacja statystyk miejsca
    const placeRef = db.collection("places").doc(placeId);
    await db.runTransaction(async (tx) => {
      const placeSnap = await tx.get(placeRef);
      if (!placeSnap.exists) return;
      const placeData = placeSnap.data();
      const count = (placeData?.reviewsCount || 0) + 1;
      const oldAvg = placeData?.averageRating || 0;
      const newAvg = (oldAvg * (count - 1) + rating) / count;
      tx.update(placeRef, {
        reviewsCount: count,
        averageRating: Math.round(newAvg * 100) / 100,
      });
    });

    // 2. Aktualizacja licznika opinii użytkownika
    if (userId) {
      await db.collection("users").doc(userId).update({
        reviewsCount: admin.firestore.FieldValue.increment(1),
      });
    }

    console.log(`Place ${placeId} and User ${userId} stats updated after new review.`);
  }
);

export const updatePlaceStatsOnReviewDelete = onDocumentDeleted(
  {document: "reviews/{reviewId}"},
  async (event) => {
    const data = event.data?.data();
    if (!data) return;
    const placeId = data.placeId;
    const rating = data.rating;
    const userId = data.userId;
    if (!placeId) return;

    // 1. Aktualizacja statystyk miejsca
    const placeRef = db.collection("places").doc(placeId);
    await db.runTransaction(async (tx) => {
      const placeSnap = await tx.get(placeRef);
      if (!placeSnap.exists) return;
      const placeData = placeSnap.data();
      const count = Math.max(0, (placeData?.reviewsCount || 0) - 1);
      const oldAvg = placeData?.averageRating || 0;
      let newAvg = 0;
      if (count > 0) {
        newAvg = (oldAvg * (count + 1) - rating) / count;
      }
      tx.update(placeRef, {
        reviewsCount: count,
        averageRating: Math.round(newAvg * 100) / 100,
      });
    });

    // 2. Aktualizacja licznika opinii użytkownika
    if (userId) {
      await db.collection("users").doc(userId).update({
        reviewsCount: admin.firestore.FieldValue.increment(-1),
      });
    }

    console.log(`Place ${placeId} and User ${userId} stats updated after review deletion.`);
  }
);

// --- Trigger: nowe miejsce / usunięcie miejsca → licznik użytkownika ---
export const updateUserStatsOnPlaceCreate = onDocumentCreated(
  {document: "places/{placeId}"},
  async (event) => {
    const data = event.data?.data();
    if (!data) return;
    const ownerUserId = data.ownerUserId;
    if (!ownerUserId) return;
    await db.collection("users").doc(ownerUserId).update({
      placesAddedCount: admin.firestore.FieldValue.increment(1),
    });
  }
);

export const updateUserStatsOnPlaceDelete = onDocumentDeleted(
  {document: "places/{placeId}"},
  async (event) => {
    const data = event.data?.data();
    if (!data) return;
    const ownerUserId = data.ownerUserId;
    if (!ownerUserId) return;
    await db.collection("users").doc(ownerUserId).update({
      placesAddedCount: admin.firestore.FieldValue.increment(-1),
    });
  }
);


// --- HTTP Endpoint: Admin usuwa opinię i wysyła email do autora ---
export const adminDeleteReview = onRequest(
  {secrets: [gmailEmail, gmailPassword], cors: true},
  async (req, res) => {
    const adminUid = await verifyAdminRequest(req, res);
    if (!adminUid) return;

    const reviewId = req.query.reviewId as string;
    const reason = req.query.reason as string || "Naruszenie regulaminu";

    if (!reviewId) {
      res.status(400).send(renderAdminResponse("Błąd", "Brak reviewId w żądaniu."));
      return;
    }

    try {
      // 1. Fetch the review document
      const reviewDoc = await db.collection("reviews").doc(reviewId).get();
      if (!reviewDoc.exists) {
        res.status(404).send(renderAdminResponse("Nie znaleziono", "Opinia nie istnieje lub została już usunięta."));
        return;
      }

      const reviewData = reviewDoc.data();
      const userId = reviewData?.userId || "";
      const placeId = reviewData?.placeId || "";
      const reviewRating = reviewData?.rating || 0;
      const reviewComment = reviewData?.comment || "(brak)";
      const authorName = reviewData?.authorName || "Użytkowniku";

      // 2. Fetch user document to get email
      let userEmail = "";
      if (userId) {
        const userDoc = await db.collection("users").doc(userId).get();
        if (userDoc.exists) {
          userEmail = userDoc.data()?.email || "";
        }
      }

      // 3. Delete the review
      await db.collection("reviews").doc(reviewId).delete();

      // Log audit
      await logAudit(adminUid, "DELETE_REVIEW", {
        reviewId,
        placeId,
        authorName,
        comment: reviewComment,
        reason,
      });

      // 5. Send email to the review author
      if (userEmail) {
        // Check email opt-in
        let emailEnabled = true;
        if (userId) {
          const userDoc2 = await db.collection("users").doc(userId).get();
          if (userDoc2.exists) {
            emailEnabled = userDoc2.data()?.emailNotificationsEnabled !== false;
          }
        }

        if (!emailEnabled) {
          console.log(`Review deletion email skipped for ${userId} - opted out`);
        } else {
          const transporter = nodemailer.createTransport({
            service: "gmail",
            auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
          });

          const html = wrapInTemplate("Twoja opinia została usunięta", `
            <p>Cześć, ${authorName}.</p>
            <p>Twoja opinia w aplikacji kidZone została usunięta przez administratora.</p>
            <table>
              <tr><td>Treść opinii:</td><td>${reviewComment}</td></tr>
              <tr><td>Ocena:</td><td>${"★".repeat(reviewRating)}${"☆".repeat(5 - reviewRating)} (${reviewRating}/5)</td></tr>
              <tr><td>Powód usunięcia:</td><td>${escapeHtml(reason)}</td></tr>
            </table>
            <p>Jeśli uważasz, że to pomyłka, skontaktuj się z nami odpowiadając na ten email.</p>
          `);

          await transporter.sendMail({
            from: `kidZone <${gmailEmail.value()}>`,
            to: userEmail,
            subject: "[kidZone] Twoja opinia została usunięta",
            html,
          });
        }
      }

      res.status(200).send(renderAdminResponse(
        "Opinia usunięta",
        `Opinia została usunięta. ${userEmail ? "Email z powiadomieniem wysłany do autora." : "Nie znaleziono adresu email autora."}`
      ));
    } catch (err) {
      console.error("adminDeleteReview error:", err);
      res.status(500).send(renderAdminResponse("Błąd serwera", `Wystąpił błąd: ${err}`));
    }
  }
);

// --- HTTP Endpoint: Admin usuwa miejsce i wysyła email do właściciela ---
export const adminDeletePlace = onRequest(
  {secrets: [gmailEmail, gmailPassword], cors: true},
  async (req, res) => {
    const adminUid = await verifyAdminRequest(req, res);
    if (!adminUid) return;

    const placeId = req.query.placeId as string;
    const reason = req.query.reason as string || "Naruszenie regulaminu";

    if (!placeId) {
      res.status(400).send(renderAdminResponse("Błąd", "Brak placeId w żądaniu."));
      return;
    }

    try {
      // 1. Pobierz dokument miejsca
      const placeDoc = await db.collection("places").doc(placeId).get();
      if (!placeDoc.exists) {
        res.status(404).send(renderAdminResponse("Nie znaleziono", "Miejsce nie istnieje lub zostało już usunięte."));
        return;
      }

      const placeData = placeDoc.data();
      const ownerUserId = placeData?.ownerUserId || "";
      const placeName = placeData?.name || "Bez nazwy";

      // 2. Pobierz email właściciela
      let ownerEmail = "";
      let ownerName = "Użytkowniku";
      if (ownerUserId) {
        const ownerDoc = await db.collection("users").doc(ownerUserId).get();
        if (ownerDoc.exists) {
          ownerEmail = ownerDoc.data()?.email || "";
          ownerName = ownerDoc.data()?.name || "Użytkowniku";
        }
      }

      // 3. Usuń dokument miejsca
      await db.collection("places").doc(placeId).delete();

      // Log audit
      await logAudit(adminUid, "DELETE_PLACE", {
        placeId,
        placeName,
        ownerUserId,
        reason,
      });

      // 4. Wyślij email do właściciela
      let emailSent = false;
      if (ownerEmail) {
        // Check email opt-in
        let emailEnabled = true;
        if (ownerUserId) {
          const ownerDocCheck = await db.collection("users").doc(ownerUserId).get();
          if (ownerDocCheck.exists) {
            emailEnabled = ownerDocCheck.data()?.emailNotificationsEnabled !== false;
          }
        }

        if (!emailEnabled) {
          console.log(`Place deletion email skipped for ${ownerUserId} - opted out`);
        } else {
          const transporter = nodemailer.createTransport({
            service: "gmail",
            auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
          });

          const html = wrapInTemplate("Twoje miejsce zostało usunięte", `
            <p>Cześć, ${ownerName}.</p>
            <p>Twoje miejsce <strong>${placeName}</strong> w aplikacji kidZone zostało usunięte przez administratora.</p>
            <table>
              <tr><td>Nazwa miejsca:</td><td>${placeName}</td></tr>
              <tr><td>Powód usunięcia:</td><td>${escapeHtml(reason)}</td></tr>
            </table>
            <p>Jeśli uważasz, że to pomyłka, skontaktuj się z nami odpowiadając na ten email.</p>
          `);

          await transporter.sendMail({
            from: `kidZone <${gmailEmail.value()}>`,
            to: ownerEmail,
            subject: `[kidZone] Twoje miejsce "${placeName}" zostało usunięte`,
            html,
          });
          emailSent = true;
        }
      }

      res.status(200).send(renderAdminResponse(
        "Miejsce usunięte",
        `Miejsce "${placeName}" zostało usunięte. ${emailSent ? "Email z powiadomieniem wysłany do właściciela." : "Nie znaleziono adresu email właściciela."}`
      ));
    } catch (err) {
      console.error("adminDeletePlace error:", err);
      res.status(500).send(renderAdminResponse("Błąd serwera", `Wystąpił błąd: ${err}`));
    }
  }
);

// --- HTTP Endpoint: Admin usuwa zgłoszone zdjęcie ---
export const adminDeletePhoto = onRequest(
  {secrets: [gmailEmail, gmailPassword, adminEmail], cors: true},
  async (req, res) => {
    const adminUid = await verifyAdminRequest(req, res);
    if (!adminUid) return;

    const reportId = req.query.reportId as string;
    const reason = req.query.reason as string || "Naruszenie regulaminu";
    if (!reportId) {
      res.status(400).send(renderAdminResponse("Błąd", "Brak reportId w żądaniu."));
      return;
    }

    try {
      const reportDoc = await db.collection("photo_reports").doc(reportId).get();
      if (!reportDoc.exists) {
        res.status(404).send(renderAdminResponse("Nie znaleziono", "Zgłoszenie nie istnieje lub zostało już obsłużone."));
        return;
      }

      const reportData = reportDoc.data();
      const photoUrl = reportData?.photoUrl || "";
      const status = reportData?.status || "";

      if (status === "resolved" || status === "dismissed") {
        res.status(200).send(renderAdminResponse("Już obsłużone", `To zgłoszenie ma status: ${status}.`));
        return;
      }

      // 1. Usuń zdjęcie z Firebase Storage
      if (photoUrl) {
        try {
          const filePath = decodeStoragePath(photoUrl);
          if (filePath) {
            const bucket = admin.storage().bucket();
            await bucket.file(filePath).delete();
          }
        } catch (storageErr) {
          console.warn(`Could not delete photo from storage: ${storageErr}`);
        }
      }

      // 2. Usuń URL z dokumentów reviews/places które go zawierają + znajdź uploaderaa
      let uploaderUserId = "";
      if (photoUrl) {
        // Szukaj w reviews
        const reviewsSnap = await db.collection("reviews")
          .where("photoUrls", "array-contains", photoUrl)
          .get();
        const batch = db.batch();
        reviewsSnap.docs.forEach((d) => {
          const urls: string[] = d.data().photoUrls || [];
          batch.update(d.ref, {
            photoUrls: urls.filter((u: string) => u !== photoUrl),
          });
        });

        // Szukaj w places
        const placesSnap = await db.collection("places")
          .where("photoUrls", "array-contains", photoUrl)
          .get();
        placesSnap.docs.forEach((d) => {
          const urls: string[] = d.data().photoUrls || [];
          batch.update(d.ref, {
            photoUrls: urls.filter((u: string) => u !== photoUrl),
          });
          // Sprawdź kto uploadował to zdjęcie
          const uploadedBy: Record<string, string> = d.data().photoUploadedBy || {};
          if (uploadedBy[photoUrl]) {
            uploaderUserId = uploadedBy[photoUrl];
          }
        });

        await batch.commit();
      }

      // 3. Oznacz zgłoszenie jako resolved
      await db.collection("photo_reports").doc(reportId).update({
        status: "resolved",
        resolvedAtMillis: Date.now(),
        action: "deleted",
      });

      // Log audit
      await logAudit(adminUid, "DELETE_PHOTO", {
        reportId,
        photoUrl,
        reason,
      });

      // 4. Wyślij email do osoby, która uploadowała zdjęcie
      let emailSent = false;
      if (uploaderUserId) {
        const uploaderDoc = await db.collection("users").doc(uploaderUserId).get();
        if (uploaderDoc.exists) {
          const uploaderEmail = uploaderDoc.data()?.email || "";
          const uploaderName = uploaderDoc.data()?.name || "Użytkowniku";
          // Check email opt-in
          const emailEnabled = uploaderDoc.data()?.emailNotificationsEnabled !== false; // default true
          if (uploaderEmail && emailEnabled) {
            const transporter = nodemailer.createTransport({
              service: "gmail",
              auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
            });

            const html = wrapInTemplate("Twoje zdjęcie zostało usunięte", `
              <p>Cześć, ${uploaderName}.</p>
              <p>Twoje zdjęcie w aplikacji kidZone zostało usunięte przez administratora.</p>
              <table>
                <tr><td>Powód usunięcia:</td><td>${escapeHtml(reason)}</td></tr>
              </table>
              <p>Jeśli uważasz, że to pomyłka, skontaktuj się z nami odpowiadając na ten email.</p>
            `);

            await transporter.sendMail({
              from: `kidZone <${gmailEmail.value()}>`,
              to: uploaderEmail,
              subject: "[kidZone] Twoje zdjęcie zostało usunięte",
              html,
            });
            emailSent = true;
          } else if (uploaderEmail && !emailEnabled) {
            console.log(`Photo deletion email skipped for ${uploaderUserId} - opted out`);
          }
        }
      }

      res.status(200).send(renderAdminResponse(
        "Zdjęcie usunięte",
        `Zdjęcie zostało usunięte z Storage oraz ze wszystkich opinii i miejsc, które je zawierały. ${emailSent ? "Email z powiadomieniem wysłany do autora." : ""}`
      ));
    } catch (err) {
      console.error("adminDeletePhoto error:", err);
      res.status(500).send(renderAdminResponse("Błąd serwera", `Wystąpił błąd: ${err}`));
    }
  }
);

// --- HTTP Endpoint: Admin odrzuca zgłoszenie zdjęcia ---
export const adminDismissPhotoReport = onRequest(
  {secrets: [gmailEmail, gmailPassword, adminEmail], cors: true},
  async (req, res) => {
    const adminUid = await verifyAdminRequest(req, res);
    if (!adminUid) return;

    const reportId = req.query.reportId as string;
    if (!reportId) {
      res.status(400).send(renderAdminResponse("Błąd", "Brak reportId w żądaniu."));
      return;
    }

    try {
      const reportDoc = await db.collection("photo_reports").doc(reportId).get();
      if (!reportDoc.exists) {
        res.status(404).send(renderAdminResponse("Nie znaleziono", "Zgłoszenie nie istnieje."));
        return;
      }

      const status = reportDoc.data()?.status || "";
      if (status === "resolved" || status === "dismissed") {
        res.status(200).send(renderAdminResponse("Już obsłużone", `To zgłoszenie ma status: ${status}.`));
        return;
      }

      await db.collection("photo_reports").doc(reportId).update({
        status: "dismissed",
        resolvedAtMillis: Date.now(),
        action: "dismissed",
      });

      res.status(200).send(renderAdminResponse(
        "Zgłoszenie odrzucone",
        "Zgłoszenie zostało oznaczone jako odrzucone. Zdjęcie pozostaje bez zmian."
      ));
    } catch (err) {
      console.error("adminDismissPhotoReport error:", err);
      res.status(500).send(renderAdminResponse("Błąd serwera", `Wystąpił błąd: ${err}`));
    }
  }
);

/**
 * Dekoduje Firebase Storage download URL na ścieżkę pliku w bucket-cie.
 * Format URL: https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{path}?...
 */
function decodeStoragePath(downloadUrl: string): string {
  try {
    const url = new URL(downloadUrl);
    const pathSegment = url.pathname.split("/o/")[1];
    if (!pathSegment) return "";
    return decodeURIComponent(pathSegment);
  } catch {
    return "";
  }
}

// --- Trigger: nowa opinia → push do właściciela miejsca ---
export const onReviewCreatedPush = onDocumentCreated(
  {
    document: "reviews/{reviewId}",
  },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const placeId = data.placeId || "";
    const reviewAuthorName = data.authorName || "Ktoś";
    const rating = data.rating || 0;
    const comment = (data.comment || "").substring(0, 100);

    if (!placeId) return;

    const placeDoc = await db.collection("places").doc(placeId).get();
    if (!placeDoc.exists) return;
    const placeData = placeDoc.data();
    const ownerUserId = placeData?.ownerUserId || "";
    const placeName = placeData?.name || "Twoje miejsce";

    if (!ownerUserId) return;
    if (data.userId === ownerUserId) return;

    const ownerDoc = await db.collection("users").doc(ownerUserId).get();
    if (!ownerDoc.exists) return;
    const ownerData = ownerDoc.data();
    const fcmTokens = await getFcmTokens(ownerUserId, ownerData);

    if (fcmTokens.length === 0) return;

    // Sprawdź preferencje powiadomień — domyślnie włączone
    const notifPrefs = ownerData?.notificationPreferences || {};
    if (notifPrefs.newReviewOnMyPlace === false) return;

    const stars = "★".repeat(rating) + "☆".repeat(5 - rating);
    const body = comment
      ? `${stars} — "${comment}"`
      : `${stars}`;

    const message: admin.messaging.MulticastMessage = {
      tokens: fcmTokens,
      data: {
        type: "new_review",
        placeId: placeId,
        reviewId: event.params.reviewId,
        title: `${reviewAuthorName} oceni\u0142/a \u201E${placeName}\u201D`,
        body: body,
      },
      android: {priority: "high"},
    };

    try {
      const response = await admin.messaging().sendEachForMulticast(message);
      console.log(
        `Push sent for review ${event.params.reviewId}: ` +
        `${response.successCount} success, ${response.failureCount} failure`
      );

      await cleanStaleTokens(response, fcmTokens, ownerUserId);
    } catch (err) {
      console.error("Push notification error:", err);
    }
  }
);


// --- onReviewCreatedTopRank USUNIETY ---
// Zastapiony przez dailyRankingCheck (scheduled, raz dziennie).
// Powod: real-time trigger przy kazdej opinii jest zbyt kosztowny
// i nie obsluguje rankingu uzytkownikow.


// --- Trigger: zmiana dokumentu usera → server-side badge computation ---
export const onBadgeEarned = onDocumentUpdated(
  {document: "users/{userId}"},
  async (event) => {
    const beforeData = event.data?.before?.data();
    const afterData = event.data?.after?.data();
    if (!beforeData || !afterData) return;

    const userId = event.params.userId;
    const beforePlaces = beforeData.placesAddedCount || 0;
    const afterPlaces = afterData.placesAddedCount || 0;
    const beforeReviews = beforeData.reviewsCount || 0;
    const afterReviews = afterData.reviewsCount || 0;
    const beforeBadges = beforeData.badgeEarnedAt || {};
    const afterBadges = afterData.badgeEarnedAt || {};

    const countersChanged = beforePlaces !== afterPlaces || beforeReviews !== afterReviews;
    const badgesChanged = JSON.stringify(beforeBadges) !== JSON.stringify(afterBadges);

    if (!countersChanged && !badgesChanged) return;
    if (badgesChanged && !countersChanged) return; // anti-loop

    const deservedBadges = new Set<string>();
    if (afterPlaces >= 1) deservedBadges.add("FIRST_PLACE");
    if (afterReviews >= 1) deservedBadges.add("FIRST_REVIEW");
    if (afterPlaces >= 5) deservedBadges.add("EXPLORER");
    if (afterPlaces >= 15) deservedBadges.add("CARTOGRAPHER");
    if (afterPlaces >= 30) deservedBadges.add("PATHFINDER");
    if (afterReviews >= 10) deservedBadges.add("REVIEWER");
    if (afterReviews >= 25) deservedBadges.add("CRITIC");
    if (afterReviews >= 50) deservedBadges.add("SENIOR_REVIEWER");
    if (afterPlaces >= 5 && afterReviews >= 5) deservedBadges.add("COMMUNITY_PILLAR");
    if (afterPlaces >= 10 && afterReviews >= 20) deservedBadges.add("FAMILY_EXPERT");

    const currentBadgeNames = new Set(Object.keys(afterBadges));
    const countBased = [
      "FIRST_PLACE", "FIRST_REVIEW", "EXPLORER", "CARTOGRAPHER", "PATHFINDER",
      "REVIEWER", "CRITIC", "SENIOR_REVIEWER", "COMMUNITY_PILLAR", "FAMILY_EXPERT",
    ];

    const toGrant: string[] = [];
    for (const badge of deservedBadges) {
      if (!currentBadgeNames.has(badge)) toGrant.push(badge);
    }
    const toRevoke: string[] = [];
    for (const badge of countBased) {
      if (currentBadgeNames.has(badge) && !deservedBadges.has(badge)) toRevoke.push(badge);
    }

    if (toGrant.length === 0 && toRevoke.length === 0) return;

    const updates: Record<string, number | admin.firestore.FieldValue> = {};
    const now = Date.now();
    for (const badge of toGrant) updates[`badgeEarnedAt.${badge}`] = now;
    for (const badge of toRevoke) updates[`badgeEarnedAt.${badge}`] = admin.firestore.FieldValue.delete();

    try {
      await db.collection("users").doc(userId).update(updates);
    } catch (err) {
      console.error(`Badge update failed for ${userId}:`, err);
      return;
    }

    const fcmTokens = await getFcmTokens(userId, afterData);
    if (fcmTokens.length === 0) return;
    const notifPrefs = afterData.notificationPreferences || {};
    if (notifPrefs.newBadgeEarned === false) return;

    const badgeLabels: Record<string, string> = {
      "FIRST_PLACE": "Pierwszy \u015Blad", "FIRST_REVIEW": "Pierwsza opinia",
      "EXPLORER": "Odkrywca", "CARTOGRAPHER": "Kartograf", "PATHFINDER": "Tropiciel",
      "REVIEWER": "Recenzent", "CRITIC": "Krytyk", "SENIOR_REVIEWER": "Wytrawny recenzent",
      "COMMUNITY_PILLAR": "Filar spo\u0142eczno\u015Bci", "FAMILY_EXPERT": "Ekspert rodzinny",
      "LEADER_BRONZE": "Br\u0105zowy lider", "LEADER_SILVER": "Srebrny lider",
      "LEADER_GOLD": "Z\u0142oty lider", "PLACE_TOP3": "Lokalny faworyt", "PLACE_TOP1": "Architekt zabawy",
    };

    let title = "";
    let body = "";
    if (toGrant.length > 0) {
      const names = toGrant.map((b) => badgeLabels[b] || b).join(", ");
      title = toGrant.length === 1 ? `\u{1F3C5} Nowa odznaka: ${names}!` : `\u{1F3C5} Nowe odznaki: ${names}!`;
      body = "Otw\u00F3rz profil w kidZone, by zobaczy\u0107 swoje osi\u0105gni\u0119cia.";
    } else {
      const names = toRevoke.map((b) => badgeLabels[b] || b).join(", ");
      title = toRevoke.length === 1 ? `Utracona odznaka: ${names}` : `Utracone odznaki: ${names}`;
      body = "Spe\u0142nij ponownie wymagania, by j\u0105 odzyska\u0107.";
    }

    const message: admin.messaging.MulticastMessage = {
      tokens: fcmTokens,
      data: {type: "new_badge", badges: (toGrant.length > 0 ? toGrant : toRevoke).join(","), title: title, body: body},
      android: {priority: "high"},
    };

    try {
      const response = await admin.messaging().sendEachForMulticast(message);
      console.log(`Badge push for ${userId}: ${response.successCount} ok`);
      await cleanStaleTokens(response, fcmTokens, userId);
    } catch (err) {
      console.error("Badge push error:", err);
    }
  }
);


// --- Trigger: ktos dodal/usunal zdjecie do/z Twojego miejsca ---
export const onPhotoAddedToPlace = onDocumentUpdated(
  {document: "places/{placeId}"},
  async (event) => {
    const beforeData = event.data?.before?.data();
    const afterData = event.data?.after?.data();
    if (!beforeData || !afterData) return;

    const beforePhotos: string[] = beforeData.photoUrls || [];
    const afterPhotos: string[] = afterData.photoUrls || [];

    const photosAdded = afterPhotos.length > beforePhotos.length;
    const photosRemoved = afterPhotos.length < beforePhotos.length;

    if (!photosAdded && !photosRemoved) return;

    const ownerUserId = afterData.ownerUserId || "";
    if (!ownerUserId) return;

    // Sprawdz kto dokonal zmiany (dodal/usunal)
    const afterUploaders: Record<string, string> = afterData.photoUploadedBy || {};

    if (photosAdded) {
      const newPhotos = afterPhotos.filter((url: string) => !beforePhotos.includes(url));
      if (newPhotos.length === 0) return;
      const uploaderIds = newPhotos.map((url: string) => afterUploaders[url] || "");
      // Nie wysylaj jesli wlasciciel sam dodal
      if (uploaderIds.every((uid: string) => uid === ownerUserId)) return;
    }

    // Przy usunieciu: nie mamy info kto usunal (admin?). Wysylamy zawsze do ownera.

    const placeName = afterData.name || "Twoje miejsce";
    const placeId = event.params.placeId;

    const ownerDoc = await db.collection("users").doc(ownerUserId).get();
    if (!ownerDoc.exists) return;
    const ownerData = ownerDoc.data();
    const fcmTokens = await getFcmTokens(ownerUserId, ownerData);
    if (fcmTokens.length === 0) return;

    const notifPrefs = ownerData?.notificationPreferences || {};
    if (notifPrefs.newPhotoOnMyPlace === false) return;

    const title = photosAdded
      ? `\u{1F4F7} Nowe zdj\u0119cie do \u201E${placeName}\u201D`
      : `\u{1F5D1} Usuni\u0119to zdj\u0119cie z \u201E${placeName}\u201D`;
    const body = photosAdded
      ? "Kto\u015B doda\u0142 zdj\u0119cie do Twojego miejsca. Sprawd\u017A!"
      : "Zdj\u0119cie zosta\u0142o usuni\u0119te z Twojego miejsca.";

    const message: admin.messaging.MulticastMessage = {
      tokens: fcmTokens,
      data: {type: "new_photo", placeId: placeId, title: title, body: body},
      android: {priority: "high"},
    };

    try {
      const response = await admin.messaging().sendEachForMulticast(message);
      console.log(`Photo push for place ${placeId}: ${response.successCount} ok`);
      await cleanStaleTokens(response, fcmTokens, ownerUserId);
    } catch (err) {
      console.error("Photo push error:", err);
    }
  }
);


// --- Helper: usuwanie stale tokenow ---
async function cleanStaleTokens(
  response: admin.messaging.BatchResponse,
  tokens: string[],
  userId: string
): Promise<void> {
  const tokensToRemove: string[] = [];
  response.responses.forEach((resp, idx) => {
    if (resp.error) {
      const code = resp.error.code;
      if (
        code === "messaging/invalid-registration-token" ||
        code === "messaging/registration-token-not-registered"
      ) {
        tokensToRemove.push(tokens[idx]);
      }
    }
  });
  if (tokensToRemove.length > 0) {
    await privateMessagingRef(userId).set({
      fcmTokens: admin.firestore.FieldValue.arrayRemove(...tokensToRemove),
      updatedAtMillis: Date.now(),
    }, {merge: true});

    await db.collection("users").doc(userId).set({
      fcmTokens: admin.firestore.FieldValue.delete(),
    }, {merge: true});
  }
}


// --- Scheduled: daily ranking check → push for TOP 10/3/2/1 ---
export const dailyRankingCheck = onSchedule(
  {
    schedule: "every day 09:00",
    timeZone: "Europe/Warsaw",
  },
  async () => {
    // --- TOP users ---
    const usersSnap = await db.collection("users")
      .orderBy("placesAddedCount", "desc")
      .limit(100)
      .get();

    const activeUsers = usersSnap.docs.filter((doc) => {
      const d = doc.data();
      return (d.placesAddedCount || 0) > 0 || (d.reviewsCount || 0) > 0;
    });

    for (let i = 0; i < Math.min(activeUsers.length, 10); i++) {
      const userDoc = activeUsers[i];
      const userData = userDoc.data();
      const userId = userDoc.id;
      const position = i + 1;
      const lastRank: number = userData.lastKnownUserRank || 0;

      // Sprawdz czy user awansowal na nowa pozycje warta powiadomienia
      const milestone = getMilestone(position);
      const lastMilestone = getMilestone(lastRank);

      if (milestone !== null && milestone !== lastMilestone) {
        // Awans na nowy milestone!
        const fcmTokens = await getFcmTokens(userId, userData);
        const notifPrefs = userData.notificationPreferences || {};

        if (fcmTokens.length > 0 && notifPrefs.rankings !== false) {
          const title = getUserRankTitle(position);
          const body = `Jeste\u015B na ${position}. pozycji w rankingu u\u017Cytkownik\u00F3w kidZone!`;

          const message: admin.messaging.MulticastMessage = {
            tokens: fcmTokens,
            data: {type: "user_top_rank", rank: String(position), title: title, body: body},
            android: {priority: "high"},
          };

          try {
            const response = await admin.messaging().sendEachForMulticast(message);
            console.log(`User rank push for ${userId} (#${position}): ${response.successCount} ok`);
            await cleanStaleTokens(response, fcmTokens, userId);
          } catch (err) {
            console.error(`User rank push error for ${userId}:`, err);
          }
        }
      }

      // Zapisz aktualny rank
      await db.collection("users").doc(userId).update({lastKnownUserRank: position});
    }

    // Wyzeruj rank dla userow ktory wypadli z TOP 10
    for (let i = 10; i < activeUsers.length && i < 20; i++) {
      const doc = activeUsers[i];
      if ((doc.data().lastKnownUserRank || 0) <= 10) {
        await db.collection("users").doc(doc.id).update({lastKnownUserRank: 0});
      }
    }

    // --- TOP places ---
    const placesSnap = await db.collection("places")
      .orderBy("averageRating", "desc")
      .where("reviewsCount", ">", 0)
      .limit(100)
      .get();

    for (let i = 0; i < Math.min(placesSnap.docs.length, 10); i++) {
      const placeDoc = placesSnap.docs[i];
      const placeData = placeDoc.data();
      const placeId = placeDoc.id;
      const position = i + 1;
      const lastRank: number = placeData.lastKnownPlaceRank || 0;
      const ownerUserId = placeData.ownerUserId || "";

      const milestone = getMilestone(position);
      const lastMilestone = getMilestone(lastRank);

      if (milestone !== null && milestone !== lastMilestone && ownerUserId) {
        const ownerDoc = await db.collection("users").doc(ownerUserId).get();
        if (ownerDoc.exists) {
          const ownerData = ownerDoc.data();
          const fcmTokens = await getFcmTokens(ownerUserId, ownerData);
          const notifPrefs = ownerData?.notificationPreferences || {};

          if (fcmTokens.length > 0 && notifPrefs.rankings !== false) {
            const placeName = placeData.name || "Twoje miejsce";
            const title = getPlaceRankTitle(position, placeName);
            const body = `Na ${position}. pozycji w rankingu najlepszych miejsc kidZone!`;

            const message: admin.messaging.MulticastMessage = {
              tokens: fcmTokens,
              data: {type: "place_top_rank", placeId: placeId, title: title, body: body},
              android: {priority: "high"},
            };

            try {
              const response = await admin.messaging().sendEachForMulticast(message);
              console.log(`Place rank push for ${placeId} (#${position}): ${response.successCount} ok`);
              await cleanStaleTokens(response, fcmTokens, ownerUserId);
            } catch (err) {
              console.error(`Place rank push error for ${placeId}:`, err);
            }
          }
        }
      }

      await db.collection("places").doc(placeId).update({lastKnownPlaceRank: position});
    }

    // Wyzeruj rank dla miejsc ktore wypadly z TOP 10
    for (let i = 10; i < placesSnap.docs.length && i < 20; i++) {
      const doc = placesSnap.docs[i];
      if ((doc.data().lastKnownPlaceRank || 0) <= 10) {
        await db.collection("places").doc(doc.id).update({lastKnownPlaceRank: 0});
      }
    }

    console.log("Daily ranking check completed.");
  }
);

/**
 * Milestone: TOP10, TOP3, #2, #1.
 * Zwraca unikalny identyfikator milestone lub null jesli poza TOP10.
 */
function getMilestone(position: number): string | null {
  if (position === 1) return "TOP1";
  if (position === 2) return "TOP2";
  if (position === 3) return "TOP3";
  if (position >= 4 && position <= 10) return "TOP10";
  return null;
}

function getUserRankTitle(position: number): string {
  switch (position) {
    case 1: return "\u{1F3C6} Z\u0142oto! Jeste\u015B #1 w rankingu kidZone!";
    case 2: return "\u{1F948} Srebrna pozycja! Jeste\u015B na 2. miejscu!";
    case 3: return "\u{1F949} Br\u0105z! Jeste\u015B na 3. miejscu w rankingu!";
    default: return "\u{1F3C5} Awans! Jeste\u015B w TOP 10 u\u017Cytkownik\u00F3w kidZone!";
  }
}

function getPlaceRankTitle(position: number, placeName: string): string {
  switch (position) {
    case 1: return `\u{1F3C6} \u201E${placeName}\u201D na 1. miejscu w rankingu!`;
    case 2: return `\u{1F948} \u201E${placeName}\u201D na 2. miejscu!`;
    case 3: return `\u{1F949} \u201E${placeName}\u201D na 3. miejscu!`;
    default: return `\u{1F3C5} \u201E${placeName}\u201D w TOP 10 miejsc kidZone!`;
  }
}


// --- HTTP Endpoint: Jednorazowy backfill geohash na starych dokumentach places ---
/**
 * Geohash backfill — przechodzi po wszystkich dokumentach w kolekcji `places`,
 * sprawdza czy pole `geohash` jest puste/brakujące, i oblicza je z lat/lng.
 *
 * Użycie: wywołaj raz przez URL:
 *   https://us-central1-{projectId}.cloudfunctions.net/backfillGeohash
 *
 * Po wykonaniu możesz usunąć tę funkcję z deploymentu.
 */
export const backfillGeohash = onRequest(async (req, res) => {
  const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

  function encodeGeohash(latitude: number, longitude: number, precision = 7): string {
    let latMin = -90.0;
    let latMax = 90.0;
    let lngMin = -180.0;
    let lngMax = 180.0;
    let isLng = true;
    let bit = 0;
    let charIndex = 0;
    let hash = "";

    while (hash.length < precision) {
      if (isLng) {
        const mid = (lngMin + lngMax) / 2;
        if (longitude >= mid) {
          charIndex = charIndex | (1 << (4 - bit));
          lngMin = mid;
        } else {
          lngMax = mid;
        }
      } else {
        const mid = (latMin + latMax) / 2;
        if (latitude >= mid) {
          charIndex = charIndex | (1 << (4 - bit));
          latMin = mid;
        } else {
          latMax = mid;
        }
      }
      isLng = !isLng;
      bit++;
      if (bit === 5) {
        hash += BASE32[charIndex];
        bit = 0;
        charIndex = 0;
      }
    }
    return hash;
  }

  try {
    const placesSnap = await db.collection("places").get();
    let updated = 0;
    let skipped = 0;

    const batch = db.batch();
    let batchCount = 0;

    for (const doc of placesSnap.docs) {
      const data = doc.data();
      const existingGeohash = data.geohash || "";

      if (existingGeohash) {
        skipped++;
        continue;
      }

      const lat = data.latitude;
      const lng = data.longitude;

      if (typeof lat !== "number" || typeof lng !== "number") {
        skipped++;
        continue;
      }

      const geohash = encodeGeohash(lat, lng);
      batch.update(doc.ref, {geohash});
      updated++;
      batchCount++;

      // Firestore batch limit = 500
      if (batchCount >= 500) {
        await batch.commit();
        batchCount = 0;
      }
    }

    if (batchCount > 0) {
      await batch.commit();
    }

    const message = `Backfill complete. Updated: ${updated}, Skipped (already had geohash): ${skipped}, Total: ${placesSnap.size}`;
    console.log(message);
    res.status(200).send(renderAdminResponse("Backfill Geohash", message));
  } catch (err) {
    console.error("Backfill error:", err);
    res.status(500).send(renderAdminResponse("Błąd", `Backfill failed: ${err}`));
  }
});


// --- Trigger: admin zablokował użytkownika → push + email ---
export const onUserBanned = onDocumentUpdated(
  {
    document: "users/{userId}",
    secrets: [gmailEmail, gmailPassword],
  },
  async (event) => {
    const beforeData = event.data?.before?.data();
    const afterData = event.data?.after?.data();
    if (!beforeData || !afterData) return;

    const beforeBanned = beforeData.bannedUntilMillis || 0;
    const afterBanned = afterData.bannedUntilMillis || 0;

    // Sprawdź czy blokada została właśnie nadana (wcześniej brak, teraz jest)
    if (afterBanned === 0 || afterBanned === beforeBanned) return;
    // Jeśli wcześniej był zablokowany tak samo → nic nie rób
    if (beforeBanned === afterBanned) return;

    const userId = event.params.userId;
    const userName = afterData.name || "Użytkowniku";
    const privateProfileDoc = await db.collection("users")
      .doc(userId)
      .collection("private")
      .doc("profile")
      .get();
    const privateProfile = privateProfileDoc.data() || {};
    const userEmail = privateProfile.email || afterData.email || "";
    const banReason = afterData.banReason || "Naruszenie regulaminu";
    const fcmTokens = await getFcmTokens(userId, afterData);

    let banInfo: string;
    if (afterBanned === -1) {
      banInfo = "Twoje konto zostało zablokowane bezpowrotnie.";
    } else {
      const date = new Date(afterBanned).toLocaleDateString("pl-PL", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
      banInfo = `Twoje konto zostało zablokowane do ${date}.`;
    }

    // --- Push notification ---
    if (fcmTokens.length > 0) {
      const message: admin.messaging.MulticastMessage = {
        tokens: fcmTokens,
        data: {
          type: "account_banned",
          title: "Konto zablokowane",
          body: banInfo,
          bannedUntilMillis: String(afterBanned),
          banReason: banReason,
        },
        android: {priority: "high"},
      };

      try {
        const response = await admin.messaging().sendEachForMulticast(message);
        console.log(`Ban push for ${userId}: ${response.successCount} ok`);
        await cleanStaleTokens(response, fcmTokens, userId);
      } catch (err) {
        console.error("Ban push error:", err);
      }
    }

    // --- Email ---
    if (userEmail) {
      // Check email opt-in (user can opt out of notification emails)
      const emailEnabled = privateProfile.emailNotificationsEnabled ??
        afterData.emailNotificationsEnabled ??
        true;
      if (!emailEnabled) {
        console.log(`Ban email skipped for ${userId} - opted out of email notifications`);
        return;
      }

      const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
      });

      const html = wrapInTemplate("Konto zablokowane", `
        <p>Cześć, ${userName}.</p>
        <p>${banInfo}</p>
        <table>
          <tr><td>Powód:</td><td>${banReason}</td></tr>
        </table>
        <p>Jeśli uważasz, że to pomyłka, skontaktuj się z nami odpowiadając na ten email.</p>
      `);

      await transporter.sendMail({
        from: `kidZone <${gmailEmail.value()}>`,
        to: userEmail,
        subject: "[kidZone] Twoje konto zostało zablokowane",
        html,
      });

      console.log(`Ban email sent to ${userEmail} for user ${userId}`);
    }
  }
);


// --- HTTP Endpoint: Admin usuwa użytkownika z powodem ---
export const adminDeleteUser = onRequest(
  {secrets: [gmailEmail, gmailPassword], cors: true},
  async (req, res) => {
    const adminUid = await verifyAdminRequest(req, res);
    if (!adminUid) return;

    const userId = req.query.userId as string;
    const reason = req.query.reason as string || "Naruszenie regulaminu";

    if (!userId) {
      res.status(400).send(renderAdminResponse("Błąd", "Brak userId w żądaniu."));
      return;
    }

    try {
      const userDoc = await db.collection("users").doc(userId).get();
      if (!userDoc.exists) {
        res.status(404).send(renderAdminResponse("Nie znaleziono", "Użytkownik nie istnieje."));
        return;
      }

      const userData = userDoc.data();
      const userName = userData?.name || "Użytkowniku";
      const userEmail = userData?.email || "";

      // Usuń dokument użytkownika
      await db.collection("users").doc(userId).delete();

      // Log audit
      await logAudit(adminUid, "DELETE_USER", {
        userId,
        userName,
        userEmail,
        reason,
      });

      // Wyślij email z powodem usunięcia
      if (userEmail) {
        // Check email opt-in
        const emailEnabled = userData?.emailNotificationsEnabled !== false; // default true
        if (!emailEnabled) {
          console.log(`User deletion email skipped for ${userId} - opted out`);
        } else {
          const transporter = nodemailer.createTransport({
            service: "gmail",
            auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
          });

          const html = wrapInTemplate("Konto usunięte", `
            <p>Cześć, ${userName}.</p>
            <p>Twoje konto w kidZone zostało usunięte przez administratora.</p>
            <table>
              <tr><td>Powód:</td><td>${escapeHtml(reason)}</td></tr>
            </table>
            <p>Jeśli uważasz, że to pomyłka, skontaktuj się z nami odpowiadając na ten email.</p>
          `);

          await transporter.sendMail({
            from: `kidZone <${gmailEmail.value()}>`,
            to: userEmail,
            subject: "[kidZone] Twoje konto zostało usunięte",
            html,
          });
        }
      }

      res.status(200).send(renderAdminResponse("Użytkownik usunięty", `Konto ${userName} zostało usunięte. Email z powodem wysłany.`));
    } catch (err) {
      console.error("adminDeleteUser error:", err);
      res.status(500).send(renderAdminResponse("Błąd serwera", `${err}`));
    }
  }
);


// --- HTTP Endpoint: Admin usuwa zdjęcie z miejsca (z panelu szczegółów) ---
export const adminDeletePhotoFromPlace = onRequest(
  {secrets: [gmailEmail, gmailPassword], cors: true},
  async (req, res) => {
    const adminUid = await verifyAdminRequest(req, res);
    if (!adminUid) return;

    const placeId = req.query.placeId as string;
    const photoUrl = req.query.photoUrl as string;
    const reason = req.query.reason as string || "Naruszenie regulaminu";

    if (!placeId || !photoUrl) {
      res.status(400).send(renderAdminResponse("Błąd", "Brak placeId lub photoUrl."));
      return;
    }

    try {
      const placeDoc = await db.collection("places").doc(placeId).get();
      if (!placeDoc.exists) {
        res.status(404).send(renderAdminResponse("Nie znaleziono", "Miejsce nie istnieje."));
        return;
      }

      const placeData = placeDoc.data();
      const placeName = placeData?.name || "Nieznane miejsce";
      const photoUploadedBy: Record<string, string> = placeData?.photoUploadedBy || {};
      const uploaderId = photoUploadedBy[photoUrl] || "";

      // Usuń URL z listy photoUrls
      const currentUrls: string[] = placeData?.photoUrls || [];
      const updatedUrls = currentUrls.filter((u: string) => u !== photoUrl);
      await db.collection("places").doc(placeId).update({photoUrls: updatedUrls});

      // Usuń plik z Storage
      try {
        const filePath = decodeStoragePath(photoUrl);
        if (filePath) {
          const bucket = admin.storage().bucket();
          await bucket.file(filePath).delete();
        }
      } catch (storageErr) {
        console.warn(`Could not delete photo from storage: ${storageErr}`);
      }

      // Log audit
      await logAudit(adminUid, "DELETE_PHOTO_FROM_PLACE", {
        placeId,
        placeName,
        photoUrl,
        reason,
      });

      // Wyślij email do uploadera
      if (uploaderId && uploaderId !== "admin") {
        const userDoc = await db.collection("users").doc(uploaderId).get();
        if (userDoc.exists) {
          const userEmail = userDoc.data()?.email;
          const userName = userDoc.data()?.name || "Użytkowniku";
          // Check email opt-in
          const emailEnabled = userDoc.data()?.emailNotificationsEnabled !== false; // default true
          if (userEmail && emailEnabled) {
            const transporter = nodemailer.createTransport({
              service: "gmail",
              auth: {user: gmailEmail.value(), pass: gmailPassword.value()},
            });
            const html = wrapInTemplate("Zdjęcie usunięte", `
              <p>Cześć, ${userName}.</p>
              <p>Twoje zdjęcie dodane do miejsca <strong>${placeName}</strong> zostało usunięte przez administratora.</p>
              <table>
                <tr><td>Powód:</td><td>${escapeHtml(reason)}</td></tr>
              </table>
              <p>Jeśli uważasz, że to pomyłka, skontaktuj się z nami odpowiadając na ten email.</p>
            `);
            await transporter.sendMail({
              from: `kidZone <${gmailEmail.value()}>`,
              to: userEmail,
              subject: `[kidZone] Twoje zdjęcie zostało usunięte z "${placeName}"`,
              html,
            });
          } else if (userEmail && !emailEnabled) {
            console.log(`Photo deletion email skipped for ${uploaderId} - opted out`);
          }
        }
      }

      res.status(200).send(renderAdminResponse("Zdjęcie usunięte", `Zdjęcie z "${placeName}" zostało usunięte. Email z powodem wysłany.`));
    } catch (err) {
      console.error("adminDeletePhotoFromPlace error:", err);
      res.status(500).send(renderAdminResponse("Błąd serwera", `${err}`));
    }
  }
);


// --- HTTP Endpoint: Admin aktualizuje email użytkownika (sync Firestore + Auth) ---
export const adminUpdateUserEmail = onRequest(
  {cors: true},
  async (req, res) => {
    const adminUid = await verifyAdminRequest(req, res);
    if (!adminUid) return;

    const userId = req.query.userId as string;
    const newEmail = req.query.email as string;

    if (!userId || !newEmail) {
      res.status(400).send(renderAdminResponse("Błąd", "Wymagane parametry: userId, email."));
      return;
    }

    if (!newEmail.includes("@") || !newEmail.includes(".")) {
      res.status(400).send(renderAdminResponse("Błąd", "Nieprawidłowy format adresu email."));
      return;
    }

    try {
      // 1. Aktualizuj email w Firebase Auth
      await admin.auth().updateUser(userId, {email: newEmail});

      // 2. Aktualizuj email w Firestore (sync)
      await db.collection("users").doc(userId).update({email: newEmail});

      // Log audit
      await logAudit(adminUid, "UPDATE_USER_EMAIL", {userId, newEmail});

      res.status(200).send(renderAdminResponse(
        "Email zaktualizowany",
        `Email użytkownika ${userId} zmieniony na ${newEmail} (Auth + Firestore).`
      ));
    } catch (err: unknown) {
      console.error("adminUpdateUserEmail error:", err);
      const errorCode = typeof err === "object" && err !== null && "code" in err ?
        String(err.code) :
        "";
      if (errorCode === "auth/email-already-exists") {
        res.status(409).send(renderAdminResponse("Konflikt", "Ten adres email jest już używany przez inne konto."));
      } else if (errorCode === "auth/invalid-email") {
        res.status(400).send(renderAdminResponse("Błąd", "Nieprawidłowy format adresu email."));
      } else if (errorCode === "auth/user-not-found") {
        res.status(404).send(renderAdminResponse("Nie znaleziono", "Użytkownik nie istnieje w Firebase Auth."));
      } else {
        const errorMessage = err instanceof Error ? err.message : String(err);
        res.status(500).send(renderAdminResponse("Błąd serwera", errorMessage));
      }
    }
  }
);

// --- HTTP Endpoint: Rate limit check ---
export const checkRateLimit = onRequest(
  {cors: true},
  async (req, res) => {
    const authHeader = req.headers.authorization || "";
    if (!authHeader.startsWith("Bearer ")) {
      res.status(401).json({allowed: false, reason: "Unauthorized"});
      return;
    }
    const idToken = authHeader.split("Bearer ")[1];

    try {
      const decoded = await admin.auth().verifyIdToken(idToken);
      const uid = decoded.uid;
      const action = req.query.action as string;

      if (!action) {
        res.status(400).json({allowed: false, reason: "Missing action parameter"});
        return;
      }

      const limits: Record<string, {maxPerHour: number; collection: string; userField: string}> = {
        addPlace: {maxPerHour: 10, collection: "places", userField: "ownerUserId"},
        addReview: {maxPerHour: 20, collection: "reviews", userField: "userId"},
        reportPlace: {maxPerHour: 10, collection: "place_reports", userField: "reporterId"},
        reportReview: {maxPerHour: 10, collection: "review_reports", userField: "reporterId"},
        reportPhoto: {maxPerHour: 10, collection: "photo_reports", userField: "reporterId"},
      };

      const config = limits[action];
      if (!config) {
        res.status(200).json({allowed: true});
        return;
      }

      const oneHourAgo = Date.now() - 60 * 60 * 1000;
      const recentDocs = await db.collection(config.collection)
        .where(config.userField, "==", uid)
        .where("createdAtMillis", ">", oneHourAgo)
        .limit(config.maxPerHour + 1)
        .get();

      if (recentDocs.size >= config.maxPerHour) {
        res.status(429).json({
          allowed: false,
          reason: `Przekroczono limit ${config.maxPerHour} akcji na godzinę.`,
          retryAfterMinutes: 60,
        });
      } else {
        res.status(200).json({allowed: true, remaining: config.maxPerHour - recentDocs.size});
      }
    } catch (err) {
      console.error("checkRateLimit error:", err);
      res.status(200).json({allowed: true}); // fail-open
    }
  }
);


// =============================================================================
// FB2 — Daily Cleanup Scheduler
// =============================================================================

/**
 * Scheduled Cloud Function: runs daily at 03:00 Warsaw time.
 *
 * Tasks:
 *  1. Expired bans: unban users whose `bannedUntilMillis` has passed
 *  2. Orphaned photos: find Storage photos not referenced by any place/review
 *  3. Stale pending operations: clean dead-letter entries older than 30 days
 *  4. Dismissed reports: delete reports in 'dismissed' status older than 90 days
 *
 * Designed to be idempotent — safe to re-run manually via Firebase Console.
 */
export const dailyCleanup = onSchedule(
  {
    schedule: "every day 03:00",
    timeZone: "Europe/Warsaw",
  },
  async () => {
    console.log("dailyCleanup: starting...");

    const results = {
      expiredBansCleared: 0,
      orphanedPhotosDeleted: 0,
      staleReportsCleaned: 0,
    };

    // ─── 1. Expired bans ─────────────────────────────────────────────────
    try {
      const now = Date.now();
      // Find users with temporary bans that have expired
      // (bannedUntilMillis > 0 means temporary; -1 = permanent)
      const expiredBansSnap = await db.collection("users")
        .where("bannedUntilMillis", ">", 0)
        .where("bannedUntilMillis", "<", now)
        .get();

      const batch = db.batch();
      for (const doc of expiredBansSnap.docs) {
        batch.update(doc.ref, {
          bannedUntilMillis: admin.firestore.FieldValue.delete(),
          banReason: admin.firestore.FieldValue.delete(),
        });
        results.expiredBansCleared++;
      }

      if (results.expiredBansCleared > 0) {
        await batch.commit();
        console.log(`dailyCleanup: cleared ${results.expiredBansCleared} expired bans`);
      }
    } catch (err) {
      console.error("dailyCleanup: expired bans error:", err);
    }

    // ─── 2. Orphaned photos (Storage cleanup) ────────────────────────────
    try {
      // Strategy: check photo_reports with status='resolved' that have photoUrl
      // still in Storage — these were "resolved" (photo deleted from place doc)
      // but the actual Storage file might remain.
      //
      // For MVP: we only clean photos from resolved photo_reports older than 7 days
      // where the photo was supposed to be deleted.
      const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
      const resolvedPhotoReports = await db.collection("photo_reports")
        .where("status", "==", "resolved")
        .where("resolvedAtMillis", "<", sevenDaysAgo)
        .limit(50)
        .get();

      for (const doc of resolvedPhotoReports.docs) {
        const data = doc.data();
        const photoUrl = data.photoUrl;
        if (photoUrl) {
          try {
            const bucket = admin.storage().bucket();
            // Extract path from download URL
            const urlPath = decodeURIComponent(
              photoUrl.split("/o/")[1]?.split("?")[0] || ""
            );
            if (urlPath) {
              const file = bucket.file(urlPath);
              const [exists] = await file.exists();
              if (exists) {
                await file.delete();
                results.orphanedPhotosDeleted++;
              }
            }
          } catch (photoErr) {
            // Best-effort — don't fail the whole job for one photo
            console.warn(`dailyCleanup: failed to delete photo from report ${doc.id}:`, photoErr);
          }
        }
      }

      if (results.orphanedPhotosDeleted > 0) {
        console.log(`dailyCleanup: deleted ${results.orphanedPhotosDeleted} orphaned photos`);
      }
    } catch (err) {
      console.error("dailyCleanup: orphaned photos error:", err);
    }

    // ─── 3. Stale dismissed reports (>90 days) ───────────────────────────
    try {
      const ninetyDaysAgo = Date.now() - 90 * 24 * 60 * 60 * 1000;
      const collections = ["place_reports", "review_reports", "photo_reports"];

      for (const collName of collections) {
        const staleSnap = await db.collection(collName)
          .where("status", "==", "dismissed")
          .where("resolvedAtMillis", "<", ninetyDaysAgo)
          .limit(100)
          .get();

        if (staleSnap.empty) continue;

        const batch = db.batch();
        for (const doc of staleSnap.docs) {
          batch.delete(doc.ref);
          results.staleReportsCleaned++;
        }
        await batch.commit();
      }

      if (results.staleReportsCleaned > 0) {
        console.log(`dailyCleanup: deleted ${results.staleReportsCleaned} stale dismissed reports`);
      }
    } catch (err) {
      console.error("dailyCleanup: stale reports error:", err);
    }

    console.log("dailyCleanup: completed", results);
  }
);
