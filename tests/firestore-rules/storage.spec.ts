/**
 * Firebase Storage Security Rules — unit tests.
 *
 * Requirements:
 *   - Firebase Emulator Suite running locally:
 *     `firebase emulators:start --only storage`
 *   - Or run through `firebase emulators:exec --only storage "npx vitest --run"`.
 */
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
  RulesTestEnvironment,
} from '@firebase/rules-unit-testing';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { describe, it, beforeAll, afterAll, beforeEach } from 'vitest';
import {
  deleteObject,
  getBytes,
  ref,
  uploadBytes,
} from 'firebase/storage';

const PROJECT_ID = 'kidzone-storage-rules-test';
const BUCKET = `${PROJECT_ID}.appspot.com`;
const OWNER_UID = 'owner-user';
const OTHER_UID = 'other-user';
const ADMIN_UID = 'admin-user';
const IMAGE_BYTES = new Uint8Array([1, 2, 3, 4]);
const LARGE_IMAGE_BYTES = new Uint8Array(10 * 1024 * 1024);

let testEnv: RulesTestEnvironment;

beforeAll(async () => {
  const rulesPath = resolve(__dirname, '../../storage.rules');
  const rules = readFileSync(rulesPath, 'utf-8');

  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    storage: {
      rules,
      host: '127.0.0.1',
      port: 9199,
    },
  });
});

afterAll(async () => {
  await testEnv?.cleanup();
});

beforeEach(async () => {
  await testEnv.clearStorage();
});

function authedStorage(uid: string, claims?: Record<string, unknown>) {
  return testEnv.authenticatedContext(uid, claims).storage(BUCKET);
}

function unauthStorage() {
  return testEnv.unauthenticatedContext().storage(BUCKET);
}

async function seedImage(path: string, contentType = 'image/webp') {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const storage = ctx.storage(BUCKET);
    await uploadBytes(ref(storage, path), IMAGE_BYTES, { contentType });
  });
}

async function uploadImage(
  uid: string,
  path: string,
  bytes: Uint8Array = IMAGE_BYTES,
  contentType = 'image/webp',
) {
  const storage = authedStorage(uid);
  return uploadBytes(ref(storage, path), bytes, { contentType });
}

describe('Place photos storage rules', () => {
  const path = `places/${OWNER_UID}/place-1/photos/photo.webp`;

  it('allows public reads for place photos', async () => {
    await seedImage(path);

    await assertSucceeds(getBytes(ref(unauthStorage(), path)));
  });

  it('allows owner to create and overwrite own place photo', async () => {
    await assertSucceeds(uploadImage(OWNER_UID, path));
    await assertSucceeds(uploadImage(OWNER_UID, path, new Uint8Array([5, 6, 7])));
  });

  it('denies another user creating, overwriting, or deleting owner place photo', async () => {
    await assertFails(uploadImage(OTHER_UID, path));
    await seedImage(path);

    await assertFails(uploadImage(OTHER_UID, path));
    await assertFails(deleteObject(ref(authedStorage(OTHER_UID), path)));
  });

  it('allows owner and admin to delete place photo', async () => {
    await seedImage(path);
    await assertSucceeds(deleteObject(ref(authedStorage(OWNER_UID), path)));

    await seedImage(path);
    await assertSucceeds(deleteObject(ref(authedStorage(ADMIN_UID, { admin: true }), path)));
  });

  it('denies non-images and files at or above the 10 MB place photo limit', async () => {
    await assertFails(uploadImage(OWNER_UID, path, IMAGE_BYTES, 'text/plain'));
    await assertFails(uploadImage(OWNER_UID, path, LARGE_IMAGE_BYTES, 'image/webp'));
  });

  it('keeps legacy place paths read-only for non-admin users', async () => {
    const legacyPath = 'places/place-legacy/photos/photo.webp';
    await seedImage(legacyPath);

    await assertSucceeds(getBytes(ref(unauthStorage(), legacyPath)));
    await assertFails(uploadImage(OWNER_UID, legacyPath));
    await assertFails(deleteObject(ref(authedStorage(OWNER_UID), legacyPath)));
    await assertSucceeds(deleteObject(ref(authedStorage(ADMIN_UID, { admin: true }), legacyPath)));
  });
});

describe('Review photos storage rules', () => {
  const path = `reviews/${OWNER_UID}/review-1/photos/photo.webp`;

  it('allows public reads for review photos', async () => {
    await seedImage(path);

    await assertSucceeds(getBytes(ref(unauthStorage(), path)));
  });

  it('allows owner to create and overwrite own review photo', async () => {
    await assertSucceeds(uploadImage(OWNER_UID, path));
    await assertSucceeds(uploadImage(OWNER_UID, path, new Uint8Array([5, 6, 7])));
  });

  it('denies another user creating, overwriting, or deleting owner review photo', async () => {
    await assertFails(uploadImage(OTHER_UID, path));
    await seedImage(path);

    await assertFails(uploadImage(OTHER_UID, path));
    await assertFails(deleteObject(ref(authedStorage(OTHER_UID), path)));
  });

  it('allows owner and admin to delete review photo', async () => {
    await seedImage(path);
    await assertSucceeds(deleteObject(ref(authedStorage(OWNER_UID), path)));

    await seedImage(path);
    await assertSucceeds(deleteObject(ref(authedStorage(ADMIN_UID, { admin: true }), path)));
  });

  it('keeps legacy review paths read-only for non-admin users', async () => {
    const legacyPath = 'reviews/review-legacy/photos/photo.webp';
    await seedImage(legacyPath);

    await assertSucceeds(getBytes(ref(unauthStorage(), legacyPath)));
    await assertFails(uploadImage(OWNER_UID, legacyPath));
    await assertFails(deleteObject(ref(authedStorage(OWNER_UID), legacyPath)));
    await assertSucceeds(deleteObject(ref(authedStorage(ADMIN_UID, { admin: true }), legacyPath)));
  });
});

describe('Avatar storage rules', () => {
  const path = `avatars/${OWNER_UID}/avatar.jpg`;

  it('allows signed-in users to read avatars', async () => {
    await seedImage(path, 'image/jpeg');

    await assertSucceeds(getBytes(ref(authedStorage(OTHER_UID), path)));
  });

  it('denies anonymous avatar reads', async () => {
    await seedImage(path, 'image/jpeg');

    await assertFails(getBytes(ref(unauthStorage(), path)));
  });

  it('allows owner to create and overwrite own avatar', async () => {
    await assertSucceeds(uploadImage(OWNER_UID, path, IMAGE_BYTES, 'image/jpeg'));
    await assertSucceeds(uploadImage(OWNER_UID, path, new Uint8Array([8, 9]), 'image/jpeg'));
  });

  it('denies another user creating, overwriting, or deleting owner avatar', async () => {
    await assertFails(uploadImage(OTHER_UID, path, IMAGE_BYTES, 'image/jpeg'));
    await seedImage(path, 'image/jpeg');

    await assertFails(uploadImage(OTHER_UID, path, IMAGE_BYTES, 'image/jpeg'));
    await assertFails(deleteObject(ref(authedStorage(OTHER_UID), path)));
  });

  it('allows owner and admin to delete avatar', async () => {
    await seedImage(path, 'image/jpeg');
    await assertSucceeds(deleteObject(ref(authedStorage(OWNER_UID), path)));

    await seedImage(path, 'image/jpeg');
    await assertSucceeds(deleteObject(ref(authedStorage(ADMIN_UID, { admin: true }), path)));
  });

  it('denies non-images and files at or above the 5 MB avatar limit', async () => {
    const oversizedAvatar = new Uint8Array(5 * 1024 * 1024);

    await assertFails(uploadImage(OWNER_UID, path, IMAGE_BYTES, 'text/plain'));
    await assertFails(uploadImage(OWNER_UID, path, oversizedAvatar, 'image/jpeg'));
  });
});
