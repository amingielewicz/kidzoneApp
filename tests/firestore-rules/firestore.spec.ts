/**
 * Firestore Security Rules — unit tests.
 *
 * Requirements:
 *   - Firebase Emulator Suite running locally (`firebase emulators:start --only firestore`)
 *   - Or run via CI with emulator started as background service.
 *
 * These tests verify:
 *   1. Users collection: read/write/delete permissions
 *   2. Places collection: create/update/delete ownership rules
 *   3. Reviews collection: create restrictions (no self-review)
 *   4. Reports: create by authenticated, read by admin/owner
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
import {
  doc,
  setDoc,
  getDoc,
  updateDoc,
  deleteDoc,
  collection,
  addDoc,
  writeBatch,
  arrayUnion,
  deleteField,
  setLogLevel,
} from 'firebase/firestore';

const PROJECT_ID = 'kidzone-rules-test';

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
      host: '127.0.0.1',
      port: 8080,
    },
  });
});

afterAll(async () => {
  await testEnv.cleanup();
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

async function seedPlace(placeId: string, ownerUserId: string) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'places', placeId), {
      name: 'Test Place',
      ownerUserId,
      averageRating: 0,
      reviewsCount: 0,
      createdAtMillis: Date.now(),
    });
  });
}

async function seedUser(uid: string, data?: Record<string, unknown>) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users', uid), {
      name: 'Test User',
      placesAddedCount: 0,
      reviewsCount: 0,
      role: 'user',
      createdAtMillis: Date.now(),
      ...data,
    });
  });
}

// ─── Tests ───────────────────────────────────────────────────────────────────

describe('Users collection', () => {
  it('allows signed-in user to read any user doc', async () => {
    await seedUser('user1');
    const db = authedDb('user2');
    await assertSucceeds(getDoc(doc(db, 'users', 'user1')));
  });

  it('denies unauthenticated read', async () => {
    await seedUser('user1');
    const db = unauthDb();
    await assertFails(getDoc(doc(db, 'users', 'user1')));
  });

  it('allows owner to update own profile (non-protected fields)', async () => {
    await seedUser('user1');
    const db = authedDb('user1');
    await assertSucceeds(
      updateDoc(doc(db, 'users', 'user1'), { name: 'New Name' }),
    );
  });

  it('denies private fields on public user document create', async () => {
    const db = authedDb('user1');
    await assertFails(
      setDoc(doc(db, 'users', 'user1'), {
        name: 'Test User',
        role: 'user',
        email: 'private@example.com',
        fcmTokens: ['token-1'],
        placesAddedCount: 0,
      }),
    );
  });

  it('denies additional PII fields on public user document create', async () => {
    const db = authedDb('user1');
    await assertFails(
      setDoc(doc(db, 'users', 'user1'), {
        name: 'Test User',
        role: 'user',
        phone: '+48123123123',
        address: 'Private street 1',
        privateSettings: { marketing: false },
        placesAddedCount: 0,
        reviewsCount: 0,
      }),
    );
  });

  it('allows owner to read and write own private profile', async () => {
    const db = authedDb('user1');
    await assertSucceeds(
      setDoc(doc(db, 'users', 'user1', 'private', 'profile'), {
        userId: 'user1',
        email: 'private@example.com',
        firstName: 'Jan',
        lastName: 'Kowalski',
        emailNotificationsEnabled: true,
      }),
    );
    await assertSucceeds(getDoc(doc(db, 'users', 'user1', 'private', 'profile')));
  });

  it('allows owner to read and write own private messaging document', async () => {
    const db = authedDb('user1');
    await assertSucceeds(
      setDoc(doc(db, 'users', 'user1', 'private', 'messaging'), {
        userId: 'user1',
        fcmTokens: ['token-1'],
        updatedAtMillis: Date.now(),
      }),
    );
    await assertSucceeds(getDoc(doc(db, 'users', 'user1', 'private', 'messaging')));
  });

  it('denies another user from reading private messaging document', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'users', 'user1', 'private', 'messaging'), {
        userId: 'user1',
        fcmTokens: ['token-1'],
      });
    });
    const db = authedDb('user2');
    await assertFails(getDoc(doc(db, 'users', 'user1', 'private', 'messaging')));
  });

  it('denies owner from changing private messaging userId', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'users', 'user1', 'private', 'messaging'), {
        userId: 'user1',
        fcmTokens: ['token-1'],
      });
    });
    const db = authedDb('user1');
    await assertFails(
      updateDoc(doc(db, 'users', 'user1', 'private', 'messaging'), { userId: 'user2' }),
    );
  });

  it('allows registration batch to create public user and private messaging docs', async () => {
    const db = authedDb('user1');
    const batch = writeBatch(db);
    batch.set(doc(db, 'users', 'user1'), {
      id: 'user1',
      name: 'Test User',
      role: 'user',
      placesAddedCount: 0,
      reviewsCount: 0,
      createdAtMillis: Date.now(),
    });
    batch.set(doc(db, 'users', 'user1', 'private', 'messaging'), {
      userId: 'user1',
      fcmTokens: ['token-1'],
      updatedAtMillis: Date.now(),
    });

    await assertSucceeds(batch.commit());
  });

  it('allows owner to delete legacy public FCM tokens only', async () => {
    const createdAtMillis = Date.now();
    await seedUser('user1', { createdAtMillis, fcmTokens: ['token-1'] });
    const db = authedDb('user1');

    await assertSucceeds(
      setDoc(doc(db, 'users', 'user1'), {
        name: 'Test User',
        placesAddedCount: 0,
        reviewsCount: 0,
        role: 'user',
        createdAtMillis,
      }),
    );
  });

  it('allows login migration batch to write private token and delete public legacy field', async () => {
    const createdAtMillis = Date.now();
    await seedUser('user1', { createdAtMillis, fcmTokens: ['legacy-token'] });
    const db = authedDb('user1');
    const batch = writeBatch(db);
    batch.set(
      doc(db, 'users', 'user1', 'private', 'messaging'),
      {
        userId: 'user1',
        fcmTokens: arrayUnion('legacy-token'),
        updatedAtMillis: Date.now(),
      },
      { merge: true },
    );
    batch.update(doc(db, 'users', 'user1'), {
      fcmTokens: deleteField(),
    });

    await assertSucceeds(batch.commit());
  });

  it('denies another user from reading private profile', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'users', 'user1', 'private', 'profile'), {
        userId: 'user1',
        email: 'private@example.com',
      });
    });
    const db = authedDb('user2');
    await assertFails(getDoc(doc(db, 'users', 'user1', 'private', 'profile')));
  });

  it('allows admin to read private profile', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'users', 'user1', 'private', 'profile'), {
        userId: 'user1',
        email: 'private@example.com',
      });
    });
    const db = authedDb('admin1', { admin: true });
    await assertSucceeds(getDoc(doc(db, 'users', 'user1', 'private', 'profile')));
  });

  it('denies owner from changing own role', async () => {
    await seedUser('user1');
    const db = authedDb('user1');
    await assertFails(
      updateDoc(doc(db, 'users', 'user1'), { role: 'admin' }),
    );

    await assertFails(
      updateDoc(doc(db, 'users', 'user1'), { fcmTokens: ['token-1'] }),
    );
  });

  it('allows admin to update any user', async () => {
    await seedUser('user1');
    const db = authedDb('admin1', { admin: true });
    await assertSucceeds(
      updateDoc(doc(db, 'users', 'user1'), { role: 'admin' }),
    );
  });

  it('allows admin to delete user', async () => {
    await seedUser('user1');
    const db = authedDb('admin1', { admin: true });
    await assertSucceeds(deleteDoc(doc(db, 'users', 'user1')));
  });

  it('denies non-admin from deleting user', async () => {
    await seedUser('user1');
    const db = authedDb('user2');
    await assertFails(deleteDoc(doc(db, 'users', 'user1')));
  });
});

describe('Places collection', () => {
  it('allows unauthenticated read', async () => {
    await seedPlace('place1', 'owner1');
    const db = unauthDb();
    await assertSucceeds(getDoc(doc(db, 'places', 'place1')));
  });

  it('allows signed-in user to create place (with correct ownerUserId)', async () => {
    const db = authedDb('user1');
    await assertSucceeds(
      setDoc(doc(db, 'places', 'newPlace'), {
        name: 'My Place',
        ownerUserId: 'user1',
        averageRating: 0,
        reviewsCount: 0,
        createdAtMillis: Date.now(),
      }),
    );
  });

  it('denies creating place with different ownerUserId', async () => {
    const db = authedDb('user1');
    await assertFails(
      setDoc(doc(db, 'places', 'newPlace'), {
        name: 'My Place',
        ownerUserId: 'someoneElse',
        averageRating: 0,
        reviewsCount: 0,
        createdAtMillis: Date.now(),
      }),
    );
  });

  it('allows owner to update own place (non-protected fields)', async () => {
    await seedPlace('place1', 'owner1');
    const db = authedDb('owner1');
    await assertSucceeds(
      updateDoc(doc(db, 'places', 'place1'), { name: 'Updated Name' }),
    );
  });

  it('allows owner and admin to update place photos', async () => {
    await seedPlace('place1', 'owner1');
    const ownerDb = authedDb('owner1');
    await assertSucceeds(
      updateDoc(doc(ownerDb, 'places', 'place1'), {
        photoUrls: ['https://example.com/photo.webp'],
        photoUploadedBy: ['owner1'],
      }),
    );

    const adminDb = authedDb('admin1', { admin: true });
    await assertSucceeds(
      updateDoc(doc(adminDb, 'places', 'place1'), {
        photoUrls: ['https://example.com/moderated.webp'],
      }),
    );
  });

  it('denies non-owner from changing place photos', async () => {
    await seedPlace('place1', 'owner1');
    const db = authedDb('user2');
    await assertFails(
      updateDoc(doc(db, 'places', 'place1'), {
        photoUrls: ['https://attacker.example/photo.webp'],
        photoUploadedBy: ['user2'],
      }),
    );
  });

  it('denies owner from changing averageRating', async () => {
    await seedPlace('place1', 'owner1');
    const db = authedDb('owner1');
    await assertFails(
      updateDoc(doc(db, 'places', 'place1'), { averageRating: 5.0 }),
    );
  });

  it('allows owner to delete own place', async () => {
    await seedPlace('place1', 'owner1');
    const db = authedDb('owner1');
    await assertSucceeds(deleteDoc(doc(db, 'places', 'place1')));
  });

  it('denies non-owner from deleting place', async () => {
    await seedPlace('place1', 'owner1');
    const db = authedDb('user2');
    await assertFails(deleteDoc(doc(db, 'places', 'place1')));
  });

  it('allows admin to delete any place', async () => {
    await seedPlace('place1', 'owner1');
    const db = authedDb('admin1', { admin: true });
    await assertSucceeds(deleteDoc(doc(db, 'places', 'place1')));
  });
});

describe('Reviews collection', () => {
  it('allows unauthenticated read', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'reviews', 'review1'), {
        userId: 'user1',
        placeId: 'place1',
        rating: 4,
        comment: 'Nice',
        createdAtMillis: Date.now(),
      });
    });
    const db = unauthDb();
    await assertSucceeds(getDoc(doc(db, 'reviews', 'review1')));
  });

  it('denies creating review on own place', async () => {
    await seedPlace('place1', 'owner1');
    const db = authedDb('owner1');
    await assertFails(
      addDoc(collection(db, 'reviews'), {
        userId: 'owner1',
        placeId: 'place1',
        rating: 5,
        comment: 'Self review',
        createdAtMillis: Date.now(),
      }),
    );
  });

  it('allows creating review on other user\'s place', async () => {
    await seedPlace('place1', 'owner1');
    const db = authedDb('reviewer1');
    await assertSucceeds(
      addDoc(collection(db, 'reviews'), {
        userId: 'reviewer1',
        placeId: 'place1',
        rating: 4,
        comment: 'Good place',
        createdAtMillis: Date.now(),
      }),
    );
  });

  it('denies creating review with wrong userId', async () => {
    await seedPlace('place1', 'owner1');
    const db = authedDb('reviewer1');
    await assertFails(
      addDoc(collection(db, 'reviews'), {
        userId: 'someoneElse',
        placeId: 'place1',
        rating: 4,
        comment: 'Spoofed',
        createdAtMillis: Date.now(),
      }),
    );
  });

  it('allows review owner to update mutable fields with valid rating', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'reviews', 'review1'), {
        userId: 'reviewer1',
        placeId: 'place1',
        rating: 4,
        comment: 'Good',
        photoUrls: [],
        createdAtMillis: Date.now(),
        updatedAtMillis: Date.now(),
      });
    });
    const db = authedDb('reviewer1');
    await assertSucceeds(
      updateDoc(doc(db, 'reviews', 'review1'), {
        rating: 5,
        comment: 'Great',
        updatedAtMillis: Date.now(),
      }),
    );
  });

  it('denies review owner from updating rating outside allowed range', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'reviews', 'review1'), {
        userId: 'reviewer1',
        placeId: 'place1',
        rating: 4,
        comment: 'Good',
        photoUrls: [],
        createdAtMillis: Date.now(),
        updatedAtMillis: Date.now(),
      });
    });
    const db = authedDb('reviewer1');

    await assertFails(updateDoc(doc(db, 'reviews', 'review1'), { rating: 0 }));
    await assertFails(updateDoc(doc(db, 'reviews', 'review1'), { rating: 6 }));
    await assertFails(updateDoc(doc(db, 'reviews', 'review1'), { rating: '5' }));
  });

  it('denies review owner from updating immutable review fields', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'reviews', 'review1'), {
        userId: 'reviewer1',
        placeId: 'place1',
        rating: 4,
        comment: 'Good',
        photoUrls: [],
        createdAtMillis: Date.now(),
        updatedAtMillis: Date.now(),
      });
    });
    const db = authedDb('reviewer1');

    await assertFails(updateDoc(doc(db, 'reviews', 'review1'), { userId: 'user2' }));
    await assertFails(updateDoc(doc(db, 'reviews', 'review1'), { placeId: 'place2' }));
    await assertFails(updateDoc(doc(db, 'reviews', 'review1'), { createdAtMillis: Date.now() }));
  });
});

describe('Reports collections', () => {
  it('allows signed-in user to create place report', async () => {
    const db = authedDb('reporter1');
    await assertSucceeds(
      addDoc(collection(db, 'place_reports'), {
        placeId: 'place1',
        reporterId: 'reporter1',
        reason: 'INAPPROPRIATE',
        comment: 'Bad content',
        status: 'pending',
        createdAtMillis: Date.now(),
      }),
    );
  });

  it('denies creating report with spoofed reporterId', async () => {
    const db = authedDb('reporter1');
    await assertFails(
      addDoc(collection(db, 'place_reports'), {
        placeId: 'place1',
        reporterId: 'someoneElse',
        reason: 'INAPPROPRIATE',
        comment: 'Spoofed',
        status: 'pending',
        createdAtMillis: Date.now(),
      }),
    );
  });

  it('allows admin to read any report', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'place_reports', 'report1'), {
        placeId: 'place1',
        reporterId: 'reporter1',
        reason: 'SPAM',
        status: 'pending',
        createdAtMillis: Date.now(),
      });
    });
    const db = authedDb('admin1', { admin: true });
    await assertSucceeds(getDoc(doc(db, 'place_reports', 'report1')));
  });

  it('allows reporter to read own report', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'place_reports', 'report1'), {
        placeId: 'place1',
        reporterId: 'reporter1',
        reason: 'SPAM',
        status: 'pending',
        createdAtMillis: Date.now(),
      });
    });
    const db = authedDb('reporter1');
    await assertSucceeds(getDoc(doc(db, 'place_reports', 'report1')));
  });

  it('denies non-admin/non-reporter from reading report', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, 'place_reports', 'report1'), {
        placeId: 'place1',
        reporterId: 'reporter1',
        reason: 'SPAM',
        status: 'pending',
        createdAtMillis: Date.now(),
      });
    });
    const db = authedDb('randomUser');
    await assertFails(getDoc(doc(db, 'place_reports', 'report1')));
  });
});
