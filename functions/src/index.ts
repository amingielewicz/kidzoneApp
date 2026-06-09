import {onDocumentCreated, onDocumentDeleted} from "firebase-functions/v2/firestore";
import {onRequest} from "firebase-functions/v2/https";
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

function mapReviewReason(reason: string): string {
  const reasons: Record<string, string> = {
    "SPAM": "Spam / reklama",
    "OFFENSIVE": "Obraźliwa treść",
    "FALSE_INFO": "Fałszywe informacje",
    "NOT_RELEVANT": "Nie dotyczy tego miejsca",
    "OTHER": "Inne",
  };
  const label = reasons[reason] || reason;
  return `${label} [${reason}]`;
}

function mapPhotoReason(reason: string): string {
  const reasons: Record<string, string> = {
    "INAPPROPRIATE": "Nieodpowiednia treść",
    "NOT_RELEVANT": "Niezwiązane z miejscem",
    "COPYRIGHT": "Narusza prawa autorskie",
    "OFFENSIVE": "Obraźliwe / wulgarne",
    "OTHER": "Inne",
  };
  const label = reasons[reason] || reason;
  return `${label} [${reason}]`;
}

async function getReviewInfo(reviewId: string): Promise<{comment: string; rating: number; authorName: string; placeId: string}> {
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



// --- HTTP Endpoint: Admin usuwa zgłoszone zdjęcie ---
export const adminDeletePhoto = onRequest(
  {secrets: [gmailEmail, gmailPassword, adminEmail], cors: true},
  async (req, res) => {
    const reportId = req.query.reportId as string;
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
          // Kontynuuj – może zdjęcie zostało już usunięte ręcznie
        }
      }

      // 2. Usuń URL z dokumentów reviews/places które go zawierają
      if (photoUrl) {
        // Szukaj w reviews
        const reviewsSnap = await db.collection("reviews")
          .where("photoUrls", "array-contains", photoUrl)
          .get();
        const batch = db.batch();
        reviewsSnap.docs.forEach((doc) => {
          const urls: string[] = doc.data().photoUrls || [];
          batch.update(doc.ref, {
            photoUrls: urls.filter((u: string) => u !== photoUrl),
          });
        });

        // Szukaj w places
        const placesSnap = await db.collection("places")
          .where("photoUrls", "array-contains", photoUrl)
          .get();
        placesSnap.docs.forEach((doc) => {
          const urls: string[] = doc.data().photoUrls || [];
          batch.update(doc.ref, {
            photoUrls: urls.filter((u: string) => u !== photoUrl),
          });
        });

        await batch.commit();
      }

      // 3. Oznacz zgłoszenie jako resolved
      await db.collection("photo_reports").doc(reportId).update({
        status: "resolved",
        resolvedAtMillis: Date.now(),
        action: "deleted",
      });

      res.status(200).send(renderAdminResponse(
        "Zdjęcie usunięte",
        "Zdjęcie zostało usunięte z Storage oraz ze wszystkich opinii i miejsc, które je zawierały."
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

/**
 * Renderuje prostą stronę HTML z wynikiem akcji admina.
 */
function renderAdminResponse(title: string, message: string): string {
  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>kidZone Admin - ${title}</title>
  <style>
    body { margin: 0; padding: 40px 20px; background: #f5f8fb; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; text-align: center; }
    .card { max-width: 500px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 32px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    h1 { color: #1976D2; margin-bottom: 12px; font-size: 24px; }
    p { color: #555; font-size: 16px; line-height: 1.5; }
    .logo { color: #1976D2; font-size: 14px; margin-top: 24px; font-weight: 600; }
  </style>
</head>
<body>
  <div class="card">
    <h1>${title}</h1>
    <p>${message}</p>
    <p class="logo">kidZone Admin Panel</p>
  </div>
</body>
</html>`;
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
    const fcmTokens: string[] = ownerData?.fcmTokens || [];

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

      // Wyczyść nieaktualne tokeny
      const tokensToRemove: string[] = [];
      response.responses.forEach((resp, idx) => {
        if (resp.error) {
          const errorCode = resp.error.code;
          if (
            errorCode === "messaging/invalid-registration-token" ||
            errorCode === "messaging/registration-token-not-registered"
          ) {
            tokensToRemove.push(fcmTokens[idx]);
          }
        }
      });

      if (tokensToRemove.length > 0) {
        await db.collection("users").doc(ownerUserId).update({
          fcmTokens: admin.firestore.FieldValue.arrayRemove(...tokensToRemove),
        });
        console.log(`Removed ${tokensToRemove.length} stale tokens for user ${ownerUserId}`);
      }
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
import {onDocumentUpdated} from "firebase-functions/v2/firestore";

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
      "REVIEWER", "CRITIC", "SENIOR_REVIEWER", "COMMUNITY_PILLAR", "FAMILY_EXPERT"
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

    const updates: Record<string, any> = {};
    const now = Date.now();
    for (const badge of toGrant) updates[`badgeEarnedAt.${badge}`] = now;
    for (const badge of toRevoke) updates[`badgeEarnedAt.${badge}`] = admin.firestore.FieldValue.delete();

    try {
      await db.collection("users").doc(userId).update(updates);
    } catch (err) {
      console.error(`Badge update failed for ${userId}:`, err);
      return;
    }

    const fcmTokens: string[] = afterData.fcmTokens || [];
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
    const fcmTokens: string[] = ownerData?.fcmTokens || [];
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
    await db.collection("users").doc(userId).update({
      fcmTokens: admin.firestore.FieldValue.arrayRemove(...tokensToRemove),
    });
  }
}



// --- Scheduled: daily ranking check → push for TOP 10/3/2/1 ---
import {onSchedule} from "firebase-functions/v2/scheduler";

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
        const fcmTokens: string[] = userData.fcmTokens || [];
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
          const fcmTokens: string[] = ownerData?.fcmTokens || [];
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
    const userEmail = afterData.email || "";
    const banReason = afterData.banReason || "Naruszenie regulaminu";
    const fcmTokens: string[] = afterData.fcmTokens || [];

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
