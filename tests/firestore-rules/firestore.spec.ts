import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
  RulesTestEnvironment,
} from '@firebase/rules-unit-testing';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { describe, it, beforeAll, afterAll, beforeEach, vi } from 'vitest';

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

function authedDb(uid: string, claims?: Record<string, unknown>) {
  return testEnv.authenticatedContext(uid, claims).firestore();
}

async function seed(path: string, data: Record<string, unknown>) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc(path).set(data);
  });
}

describe('Mandatory TOS Flow', () => {
  it('allows user doc creation without TOS', async () => {
    const db = authedDb('user1');
    await assertSucceeds(
      db.doc('users/user1').set({
        name: 'User 1',
        role: 'user',
        placesAddedCount: 0,
        reviewsCount: 0,
      })
    );
  });

  it('blocks creating places without TOS acceptance', async () => {
    await seed('users/user1', { name: 'User 1', role: 'user' });
    const db = authedDb('user1');
    await assertFails(
      db.doc('places/p1').set({
        name: 'Place 1',
        ownerUserId: 'user1',
        averageRating: 0,
        reviewsCount: 0,
        photoUrls: [],
        photoHashes: {},
      })
    );
  });

  it('allows creating places after TOS acceptance', async () => {
    await seed('users/user1', {
        name: 'User 1',
        role: 'user',
        tosAcceptedAtMillis: TEST_TS
    });
    const db = authedDb('user1');
    await assertSucceeds(
      db.doc('places/p1').set({
        name: 'Place 1',
        ownerUserId: 'user1',
        averageRating: 0,
        reviewsCount: 0,
        photoUrls: [],
        photoHashes: {},
        createdAtMillis: TEST_TS
      })
    );
  });
});
