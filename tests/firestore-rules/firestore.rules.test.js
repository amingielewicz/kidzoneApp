import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { setLogLevel } from 'firebase/firestore';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

let testEnv;

const PROJECT_ID = 'kidzone-rules-test-js';
const OWNER_UID = 'owner-user';
const OTHER_UID = 'other-user';
const ADMIN_UID = 'admin-user';

// Constant timestamp for stable tests
const TEST_TS = 1700000000000;

vi.setConfig({
  testTimeout: 30_000,
  hookTimeout: 30_000,
});

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
  setLogLevel('silent');

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
  it('allows owner to create own user document without TOS initially', async () => {
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

  it('allows owner to update ONLY tosAcceptedAtMillis to accept TOS', async () => {
    await seed(`users/${OWNER_UID}`, {
      name: 'Owner',
      role: 'user',
      placesAddedCount: 0,
      reviewsCount: 0,
    });

    const db = authedDb(OWNER_UID);
    await assertSucceeds(
      db.doc(`users/${OWNER_UID}`).update({ tosAcceptedAtMillis: TEST_TS })
    );
  });

  it('rejects owner updating other protected fields during TOS acceptance', async () => {
    await seed(`users/${OWNER_UID}`, {
      name: 'Owner',
      role: 'user',
      placesAddedCount: 0,
      reviewsCount: 0,
    });

    const db = authedDb(OWNER_UID);
    await assertFails(
      db.doc(`users/${OWNER_UID}`).update({
        tosAcceptedAtMillis: TEST_TS,
        role: 'admin'
      })
    );
  });
});

describe('places rules', () => {
  it('allows creating owned place ONLY IF TOS is accepted', async () => {
    // 1. Without TOS -> Fail
    const db = authedDb(OWNER_UID);
    await assertFails(
      db.doc('places/place-1').set({
        name: 'Playground',
        ownerUserId: OWNER_UID,
        averageRating: 0,
        reviewsCount: 0,
        photoUrls: [],
        photoHashes: {},
      })
    );

    // 2. With TOS -> Succeed
    await seed(`users/${OWNER_UID}`, {
      tosAcceptedAtMillis: TEST_TS,
      role: 'user'
    });

    await assertSucceeds(
      db.doc('places/place-1').set({
        name: 'Playground',
        ownerUserId: OWNER_UID,
        averageRating: 0,
        reviewsCount: 0,
        photoUrls: [],
        photoHashes: {},
      })
    );
  });
});

describe('reviews rules', () => {
  it('allows creating review ONLY IF TOS is accepted', async () => {
    await seed('places/place-1', { ownerUserId: OTHER_UID });
    const db = authedDb(OWNER_UID);

    // 1. Without TOS -> Fail
    await assertFails(
      db.doc('reviews/review-1').set({
        userId: OWNER_UID,
        placeId: 'place-1',
        rating: 5,
        photoUrls: [],
        photoHashes: {},
      })
    );

    // 2. With TOS -> Succeed
    await seed(`users/${OWNER_UID}`, {
      tosAcceptedAtMillis: TEST_TS
    });

    await assertSucceeds(
      db.doc('reviews/review-1').set({
        userId: OWNER_UID,
        placeId: 'place-1',
        rating: 5,
        photoUrls: [],
        photoHashes: {},
      })
    );
  });
});
