import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

let testEnv;

const PROJECT_ID = 'kidzone-rules-test-js';
const OWNER_UID = 'owner-user';
const OTHER_UID = 'other-user';
const ADMIN_UID = 'admin-user';

function authedDb(uid, claims = {}) {
  return testEnv.authenticatedContext(uid, claims).firestore();
}

function publicDb() {
  return testEnv.unauthenticatedContext().firestore();
}

async function seed(path, data) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().doc(path).set(data);
  });
}

beforeAll(async () => {
  const rulesPath = resolve(__dirname, '../../firestore.rules');

  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(rulesPath, 'utf8'),
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

afterAll(async () => {
  await testEnv?.cleanup();
});

describe('users rules', () => {
  it('allows owner to create own user document with default role and zero counters', async () => {
    const db = authedDb(OWNER_UID);

    await assertSucceeds(
      db.doc(`users/${OWNER_UID}`).set({
        name: 'Owner',
        role: 'user',
        placesAddedCount: 0,
        reviewsCount: 0,
      })
    );
  });

  it('rejects private fields on public user document create', async () => {
    const db = authedDb(OWNER_UID);

    await assertFails(
      db.doc(`users/${OWNER_UID}`).set({
        name: 'Owner',
        role: 'user',
        placesAddedCount: 0,
        reviewsCount: 0,
        email: 'owner@example.com',
        fcmTokens: ['token-1'],
      })
    );
  });

  it('rejects user creation for another uid', async () => {
    const db = authedDb(OTHER_UID);

    await assertFails(
      db.doc(`users/${OWNER_UID}`).set({
        name: 'Owner',
        role: 'user',
        placesAddedCount: 0,
      })
    );
  });

  it('rejects self promotion to admin on create', async () => {
    const db = authedDb(OWNER_UID);

    await assertFails(
      db.doc(`users/${OWNER_UID}`).set({
        name: 'Owner',
        role: 'admin',
        placesAddedCount: 0,
      })
    );
  });

  it('rejects owner updating protected fields', async () => {
    await seed(`users/${OWNER_UID}`, {
      name: 'Owner',
      role: 'user',
      email: 'owner@example.com',
      placesAddedCount: 0,
      reviewsCount: 0,
    });

    const db = authedDb(OWNER_UID);

    await assertFails(
      db.doc(`users/${OWNER_UID}`).update({ role: 'admin' })
    );

    await assertFails(
      db.doc(`users/${OWNER_UID}`).update({ fcmTokens: ['token-1'] })
    );
  });

  it('allows admin claim to update protected user fields', async () => {
    await seed(`users/${OWNER_UID}`, {
      name: 'Owner',
      role: 'user',
      placesAddedCount: 0,
    });

    const db = authedDb(ADMIN_UID, { admin: true });

    await assertSucceeds(
      db.doc(`users/${OWNER_UID}`).update({ role: 'moderator' })
    );
  });

  it('allows signed in users to read user documents under current rules', async () => {
    await seed(`users/${OWNER_UID}`, {
      name: 'Owner',
    });

    const db = authedDb(OTHER_UID);

    await assertSucceeds(db.doc(`users/${OWNER_UID}`).get());
  });

  it('rejects anonymous user document reads', async () => {
    await seed(`users/${OWNER_UID}`, { name: 'Owner' });

    await assertFails(publicDb().doc(`users/${OWNER_UID}`).get());
  });

  it('allows owner to read and write own private profile', async () => {
    const db = authedDb(OWNER_UID);

    await assertSucceeds(
      db.doc(`users/${OWNER_UID}/private/profile`).set({
        userId: OWNER_UID,
        email: 'owner@example.com',
        firstName: 'Jan',
        lastName: 'Kowalski',
        emailNotificationsEnabled: true,
      })
    );
    await assertSucceeds(db.doc(`users/${OWNER_UID}/private/profile`).get());
  });

  it('rejects another user reading private profile', async () => {
    await seed(`users/${OWNER_UID}/private/profile`, {
      userId: OWNER_UID,
      email: 'owner@example.com',
    });

    const db = authedDb(OTHER_UID);

    await assertFails(db.doc(`users/${OWNER_UID}/private/profile`).get());
  });

  it('allows admin reading private profile', async () => {
    await seed(`users/${OWNER_UID}/private/profile`, {
      userId: OWNER_UID,
      email: 'owner@example.com',
    });

    const db = authedDb(ADMIN_UID, { admin: true });

    await assertSucceeds(db.doc(`users/${OWNER_UID}/private/profile`).get());
  });

  it('allows owner to register FCM token in private messaging document', async () => {
    const db = authedDb(OWNER_UID);

    await assertSucceeds(
      db.doc(`users/${OWNER_UID}/private/messaging`).set({
        userId: OWNER_UID,
        fcmTokens: ['token-1'],
        updatedAtMillis: Date.now(),
      })
    );
    await assertSucceeds(db.doc(`users/${OWNER_UID}/private/messaging`).get());
  });

  it('rejects another user reading private messaging document', async () => {
    await seed(`users/${OWNER_UID}/private/messaging`, {
      userId: OWNER_UID,
      fcmTokens: ['token-1'],
    });

    const db = authedDb(OTHER_UID);

    await assertFails(db.doc(`users/${OWNER_UID}/private/messaging`).get());
  });

  it('rejects owner changing private messaging userId', async () => {
    await seed(`users/${OWNER_UID}/private/messaging`, {
      userId: OWNER_UID,
      fcmTokens: ['token-1'],
    });

    const db = authedDb(OWNER_UID);

    await assertFails(
      db.doc(`users/${OWNER_UID}/private/messaging`).update({ userId: OTHER_UID })
    );
  });

  it('allows registration batch to create public user and private messaging docs', async () => {
    const db = authedDb(OWNER_UID);
    const batch = db.batch();
    batch.set(db.doc(`users/${OWNER_UID}`), {
      id: OWNER_UID,
      name: 'Owner',
      role: 'user',
      placesAddedCount: 0,
      reviewsCount: 0,
      createdAtMillis: Date.now(),
    });
    batch.set(db.doc(`users/${OWNER_UID}/private/messaging`), {
      userId: OWNER_UID,
      fcmTokens: ['token-1'],
      updatedAtMillis: Date.now(),
    });

    await assertSucceeds(batch.commit());
  });

  it('allows owner to delete legacy public FCM tokens only', async () => {
    await seed(`users/${OWNER_UID}`, {
      name: 'Owner',
      role: 'user',
      placesAddedCount: 0,
      reviewsCount: 0,
      fcmTokens: ['token-1'],
    });

    const db = authedDb(OWNER_UID);

    await assertSucceeds(
      db.doc(`users/${OWNER_UID}`).set({
        name: 'Owner',
        role: 'user',
        placesAddedCount: 0,
        reviewsCount: 0,
      })
    );
  });

  it('allows login migration batch to write private token and delete public legacy field', async () => {
    await seed(`users/${OWNER_UID}`, {
      name: 'Owner',
      role: 'user',
      placesAddedCount: 0,
      reviewsCount: 0,
      fcmTokens: ['legacy-token'],
    });

    const db = authedDb(OWNER_UID);
    const batch = db.batch();
    batch.set(
      db.doc(`users/${OWNER_UID}/private/messaging`),
      {
        userId: OWNER_UID,
        fcmTokens: ['legacy-token'],
        updatedAtMillis: Date.now(),
      },
      { merge: true }
    );
    batch.set(
      db.doc(`users/${OWNER_UID}`),
      {
        name: 'Owner',
        role: 'user',
        placesAddedCount: 0,
        reviewsCount: 0,
      }
    );

    await assertSucceeds(batch.commit());
  });
});

describe('places rules', () => {
  it('allows public read of places', async () => {
    await seed('places/place-1', { name: 'Playground', ownerUserId: OWNER_UID });

    await assertSucceeds(publicDb().doc('places/place-1').get());
  });

  it('allows signed in user to create owned place with zero counters', async () => {
    const db = authedDb(OWNER_UID);

    await assertSucceeds(
      db.doc('places/place-1').set({
        name: 'Playground',
        ownerUserId: OWNER_UID,
        averageRating: 0,
        reviewsCount: 0,
      })
    );
  });

  it('rejects creating place for another owner', async () => {
    const db = authedDb(OTHER_UID);

    await assertFails(
      db.doc('places/place-1').set({
        name: 'Playground',
        ownerUserId: OWNER_UID,
        averageRating: 0,
        reviewsCount: 0,
      })
    );
  });

  it('rejects owner changing protected place counters', async () => {
    await seed('places/place-1', {
      name: 'Playground',
      ownerUserId: OWNER_UID,
      averageRating: 0,
      reviewsCount: 0,
    });

    const db = authedDb(OWNER_UID);

    await assertFails(
      db.doc('places/place-1').update({ averageRating: 5 })
    );
  });

  it('allows owner to update non-protected place fields', async () => {
    await seed('places/place-1', {
      name: 'Playground',
      ownerUserId: OWNER_UID,
      averageRating: 0,
      reviewsCount: 0,
    });

    const db = authedDb(OWNER_UID);

    await assertSucceeds(
      db.doc('places/place-1').update({ name: 'Updated Playground' })
    );
  });
});

describe('reviews rules', () => {
  it('allows signed in user to review another user place with valid rating', async () => {
    await seed('places/place-1', { ownerUserId: OWNER_UID });

    const db = authedDb(OTHER_UID);

    await assertSucceeds(
      db.doc('reviews/review-1').set({
        userId: OTHER_UID,
        placeId: 'place-1',
        rating: 5,
      })
    );
  });

  it('rejects reviewing own place', async () => {
    await seed('places/place-1', { ownerUserId: OWNER_UID });

    const db = authedDb(OWNER_UID);

    await assertFails(
      db.doc('reviews/review-1').set({
        userId: OWNER_UID,
        placeId: 'place-1',
        rating: 5,
      })
    );
  });

  it('rejects invalid rating', async () => {
    await seed('places/place-1', { ownerUserId: OWNER_UID });

    const db = authedDb(OTHER_UID);

    await assertFails(
      db.doc('reviews/review-1').set({
        userId: OTHER_UID,
        placeId: 'place-1',
        rating: 6,
      })
    );
  });

  it('rejects creating review for another user', async () => {
    await seed('places/place-1', { ownerUserId: OWNER_UID });

    const db = authedDb(OTHER_UID);

    await assertFails(
      db.doc('reviews/review-1').set({
        userId: OWNER_UID,
        placeId: 'place-1',
        rating: 5,
      })
    );
  });

  it('rejects review owner changing protected fields', async () => {
    await seed('reviews/review-1', {
      userId: OWNER_UID,
      placeId: 'place-1',
      rating: 4,
    });

    const db = authedDb(OWNER_UID);

    await assertFails(
      db.doc('reviews/review-1').update({ placeId: 'place-2' })
    );
  });
});

describe('reports rules', () => {
  it('allows reporter to create and read own report', async () => {
    const db = authedDb(OWNER_UID);

    await assertSucceeds(
      db.doc('place_reports/report-1').set({
        reporterId: OWNER_UID,
        placeId: 'place-1',
      })
    );

    await assertSucceeds(db.doc('place_reports/report-1').get());
  });

  it('rejects report creation for another reporter', async () => {
    const db = authedDb(OTHER_UID);

    await assertFails(
      db.doc('place_reports/report-1').set({
        reporterId: OWNER_UID,
        placeId: 'place-1',
      })
    );
  });

  it('rejects reading another user report', async () => {
    await seed('place_reports/report-1', {
      reporterId: OWNER_UID,
      placeId: 'place-1',
    });

    const db = authedDb(OTHER_UID);

    await assertFails(db.doc('place_reports/report-1').get());
  });

  it('allows admin claim to read reports', async () => {
    await seed('place_reports/report-1', {
      reporterId: OWNER_UID,
      placeId: 'place-1',
    });

    const db = authedDb(ADMIN_UID, { admin: true });

    await assertSucceeds(db.doc('place_reports/report-1').get());
  });
});
