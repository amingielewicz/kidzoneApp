#!/usr/bin/env node

import { readFile } from 'node:fs/promises';
import process from 'node:process';
import admin from 'firebase-admin';

const args = Object.fromEntries(
  process.argv.slice(2).map((arg) => {
    const [key, value] = arg.replace(/^--/, '').split('=');
    return [key, value ?? true];
  })
);

const input = args.input;
const projectId = args.projectId ?? process.env.GOOGLE_CLOUD_PROJECT ?? process.env.FIREBASE_PROJECT_ID;
const dryRun = args.dryRun === true || args.dryRun === 'true';

if (!input) {
  throw new Error('Missing --input=path/to/kidzone-scalability-100.json');
}

if (!projectId && !dryRun) {
  throw new Error('Missing --projectId or FIREBASE_PROJECT_ID / GOOGLE_CLOUD_PROJECT');
}

const raw = await readFile(input, 'utf8');
const dataset = JSON.parse(raw);
const { users = [], places = [], reviews = [] } = dataset.collections ?? {};

console.log('Dataset summary');
console.log(`users=${users.length}`);
console.log(`places=${places.length}`);
console.log(`reviews=${reviews.length}`);
console.log(`dryRun=${dryRun}`);

if (dryRun) {
  console.log('Dry run only. No Firestore writes were executed.');
  process.exit(0);
}

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId
});

const db = admin.firestore();

async function writeCollection(collectionName, documents) {
  const chunkSize = 450;
  let written = 0;

  for (let index = 0; index < documents.length; index += chunkSize) {
    const chunk = documents.slice(index, index + chunkSize);
    const batch = db.batch();

    for (const document of chunk) {
      if (!document.id) {
        throw new Error(`Document in ${collectionName} is missing id`);
      }
      const ref = db.collection(collectionName).doc(document.id);
      batch.set(ref, document, { merge: false });
    }

    await batch.commit();
    written += chunk.length;
    console.log(`${collectionName}: written ${written}/${documents.length}`);
  }
}

await writeCollection('users', users);
await writeCollection('places', places);
await writeCollection('reviews', reviews);

console.log('Seed completed.');
