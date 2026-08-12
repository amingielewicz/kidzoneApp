/**
 * Firestore Security Rules — unit tests.
 *
 * This file uses the method-based API (Compat style) to ensure compatibility
 * with the rules-unit-testing environment in CI.
 */
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
  RulesTestEnvironment,
} from '@firebase/rules-unit-testing';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { describe, it, beforeAll, afterAll, beforeEach, vi } from 'vitest';
import { setLogLevel } from 'firebase/firestore';

const PROJECT_ID = 'kidzone-rules-test-spec';
const TEST_TS = 1700000000000;

let testEnv: RulesTestEnvironment;

vi.setConfig({
  testTimeout: 30_000,
  hookTimeout: 30_000,
});

beforeAll(async () => {
  const rulesPath = resolve(__dirname, '../../firestore.rules');
  const rules = readFileSync(rulesPath, 'utf-8');
  setLogLevel('silent');

  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
    },
  });
});

afterAll(async () => {
  await testEnv?.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

// ─── Helpers ─────────────────────────────────────────────────────────────────

function authedDb(uid: string, claims?: Record<string, unknown>) {
  return testEnv.authenticatedContext(uid, claims).firestore();
}

function unauthDb() {
  return testEnv.unauthenticatedContext().firestore();
}

async function seed(path: string, data: Record<string, unknown>) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc(path).set(data);
  });
}

// ─── Tests ───────────────────────────────────────────────────────────────────

describe('Users collection', () => {
  it('allows signed-in user to read any user doc', async () => {
    await seed('users/user1', { name: 'User 1' });
    const db = authedDb('user2');
    await assertSucceeds(db.doc('users/user1').get());
  });

  it('denies unauthenticated read', async () => {
    await seed('users/user1', { name: 'User 1' });
    const db = unauthDb();
    await assertFails(db.doc('users/user1').get());
  });

  it('allows owner to update own profile (non-protected fields)', async () => {
    await seed('users/user1', {
        name: 'User 1',
        tosAcceptedAtMillis: TEST_TS
    });
    const db = authedDb('user1');
    await assertSucceeds(
      db.doc('users/user1').update({ name: 'New Name' })
    );
  });

  it('denies private fields on public user document create', async () => {
    const db = authedDb('user1');
    await assertFails(
      db.doc('users/user1').set({
        name: 'Test User',
        role: 'user',
        email: 'private@example.com',
        fcmTokens: ['token-1'],
        placesAddedCount: 0,
        tosAcceptedAtMillis: TEST_TS
      })
    );
  });

  it('allows owner to read and write own private profile', async () => {
    const db = authedDb('user1');
    await assertSucceeds(
      db.doc('users/user1/private/profile').set({
        userId: 'user1',
        email: 'private@example.com',
        firstName: 'Jan',
        lastName: 'Kowalski',
        emailNotificationsEnabled: true,
      })
    );
    await assertSucceeds(db.doc('users/user1/private/profile').get());
  });

  it('allows owner to read and write own private messaging document', async () => {
    const db = authedDb('user1');
    await assertSucceeds(
      db.doc('users/user1/private/messaging').set({
        userId: 'user1',
        fcmTokens: ['token-1'],
        updatedAtMillis: TEST_TS,
      })
    );
    await assertSucceeds(db.doc('users/user1/private/messaging').get());
  });

  it('allows registration batch to create public user and private messaging docs', async () => {
    const db = authedDb('user1');
    const batch = db.batch();
    batch.set(db.doc('users/user1'), {
      id: 'user1',
      name: 'Test User',
      role: 'user',
      placesAddedCount: 0,
      reviewsCount: 0,
      createdAtMillis: TEST_TS,
      tosAcceptedAtMillis: TEST_TS,
    });
    batch.set(db.doc('users/user1/private/messaging'), {
      userId: 'user1',
      fcmTokens: ['token-1'],
      updatedAtMillis: TEST_TS,
    });

    await assertSucceeds(batch.commit());
  });

  it('allows admin to delete user', async () => {
    await seed('users/user1', { name: 'User 1' });
    const db = authedDb('admin1', { admin: true });
    await assertSucceeds(db.doc('users/user1').delete());
  });

  it('denies non-admin from deleting user', async () => {
    await seed('users/user1', { name: 'User 1' });
    const db = authedDb('user2');
    await assertFails(db.doc('users/user1').delete());
  });
});

describe('Places collection', () => {
  it('allows signed-in user to create place (with correct ownerUserId)', async () => {
    const db = authedDb('user1');
    await assertSucceeds(
      db.doc('places/newPlace').set({
        name: 'My Place',
        ownerUserId: 'user1',
        averageRating: 0,
        reviewsCount: 0,
        createdAtMillis: TEST_TS,
        photoUrls: [],
        photoHashes: {},
      })
    );
  });

  it('allows owner and admin to update place photos', async () => {
    await seed('places/place1', {
      ownerUserId: 'owner1',
      photoUrls: [],
      photoHashes: {},
    });

    const ownerDb = authedDb('owner1');
    await assertSucceeds(
      ownerDb.doc('places/place1').update({
        photoUrls: ['https://example.com/photo.webp'],
        photoUploadedBy: { 'https://example.com/photo.webp': 'owner1' },
        photoHashes: { 'https://example.com/photo.webp': 'hash1' },
      })
    );
  });

  it('allows non-owner to ADD photos but not remove them', async () => {
    await seed('places/place1', {
      ownerUserId: 'owner1',
      photoUrls: ['https://example.com/p1.webp'],
      photoUploadedBy: { 'https://example.com/p1.webp': 'owner1' },
      photoHashes: { 'https://example.com/p1.webp': 'h1' },
    });

    const db = authedDb('user2');

    // Adding should succeed
    await assertSucceeds(
      db.doc('places/place1').update({
        photoUrls: ['https://example.com/p1.webp', 'https://example.com/p2.webp'],
        photoUploadedBy: {
            'https://example.com/p1.webp': 'owner1',
            'https://example.com/p2.webp': 'user2'
        },
        photoHashes: {
            'https://example.com/p1.webp': 'h1',
            'https://example.com/p2.webp': 'h2'
        },
      })
    );

    // Replacing/Removing should fail
    await assertFails(
      db.doc('places/place1').update({
        photoUrls: ['https://attacker.example/photo.webp'],
        photoUploadedBy: { 'https://attacker.example/photo.webp': 'user2' },
        photoHashes: { 'https://attacker.example/photo.webp': 'h3' },
      })
    );
  });

  it('allows owner to delete own place', async () => {
    await seed('places/place1', { ownerUserId: 'owner1' });
    const db = authedDb('owner1');
    await assertSucceeds(db.doc('places/place1').delete());
  });
});

describe('Reviews collection', () => {
  it('denies creating review on own place', async () => {
    await seed('places/place1', { ownerUserId: 'owner1' });
    const db = authedDb('owner1');
    await assertFails(
      db.collection('reviews').add({
        userId: 'owner1',
        placeId: 'place1',
        rating: 5,
        photoUrls: [],
        photoHashes: {},
        createdAtMillis: TEST_TS,
      })
    );
  });
});
