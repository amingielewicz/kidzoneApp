const {onRequest} = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();

async function countAuthUsers() {
  let total = 0;
  let pageToken;

  do {
    const page = await admin.auth().listUsers(1000, pageToken);
    total += page.users.length;
    pageToken = page.pageToken;
  } while (pageToken);

  return total;
}

/**
 * 🎯 Cel: Dostarczanie zagregowanych statystyk publicznych (użytkownicy, miejsca, opinie).
 * ⚡ Wyzwalacz (Trigger): Request HTTP (GET).
 * ✅ Efekty uboczne: Brak modyfikacji danych.
 * 🛡️ Bezpieczeństwo: Publicznie dostępny endpoint (bez Auth). Brak PII.
 * ⚙️ Techniczne: Region europe-central2, Cache 5 min (s-maxage=300).
 */
exports.publicStats = onRequest(
  {
    region: "europe-central2",
    cors: false,
    timeoutSeconds: 30,
    memory: "256MiB",
  },
  async (request, response) => {
    if (request.method !== "GET") {
      response.set("Allow", "GET");
      response.status(405).json({error: "Method not allowed"});
      return;
    }

    try {
      const [users, placesSnapshot, reviewsSnapshot] = await Promise.all([
        countAuthUsers(),
        db.collection("places").count().get(),
        db.collection("reviews").count().get(),
      ]);

      response.set("Cache-Control", "public, max-age=300, s-maxage=300");
      response.status(200).json({
        users,
        places: placesSnapshot.data().count,
        reviews: reviewsSnapshot.data().count,
        supporters: null,
        generatedAt: new Date().toISOString(),
      });
    } catch (error) {
      console.error("publicStats failed", error);
      response.status(500).json({error: "Could not load public statistics"});
    }
  }
);
