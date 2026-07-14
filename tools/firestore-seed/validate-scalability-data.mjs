#!/usr/bin/env node

import { readFile } from 'node:fs/promises';

const args = Object.fromEntries(
  process.argv.slice(2).map((arg) => {
    const [key, value] = arg.replace(/^--/, '').split('=');
    return [key, value ?? true];
  })
);

const input = args.input;

if (!input) {
  throw new Error('Missing --input=path/to/kidzone-scalability-100.json');
}

const raw = await readFile(input, 'utf8');
const dataset = JSON.parse(raw);
const { places = [], reviews = [] } = dataset.collections ?? {};

const placesWithPhotos = places.filter((place) => place.photoUrls?.length > 0);
const reviewsWithPhotos = reviews.filter((review) => review.photoUrls?.length > 0);
const brokenPhotoUrls = [
  ...places.flatMap((place) => place.photoUrls ?? []),
  ...reviews.flatMap((review) => review.photoUrls ?? [])
].filter((url) => url.includes('example.invalid'));

const placesWithIncompleteUploaders = placesWithPhotos.filter((place) => {
  const uploadedBy = place.photoUploadedBy ?? {};
  return place.photoUrls.some((url) => !uploadedBy[url]);
});

const placesWithHashMismatch = placesWithPhotos.filter((place) => {
  const hashes = place.photoHashes ?? {};
  const urls = place.photoUrls ?? [];
  return Array.isArray(hashes)
    || Object.keys(hashes).length !== urls.length
    || urls.some((url) => typeof hashes[url] !== 'string');
});

console.log('Photo validation summary');
console.log(`places=${places.length}`);
console.log(`reviews=${reviews.length}`);
console.log(`placesWithPhotos=${placesWithPhotos.length}`);
console.log(`reviewsWithPhotos=${reviewsWithPhotos.length}`);
console.log(`brokenPhotoUrls=${brokenPhotoUrls.length}`);

if (placesWithPhotos.length !== places.length) {
  throw new Error(`Expected every place to have photoUrls, got ${placesWithPhotos.length}/${places.length}`);
}

if (reviewsWithPhotos.length !== reviews.length) {
  throw new Error(`Expected every review to have photoUrls, got ${reviewsWithPhotos.length}/${reviews.length}`);
}

if (brokenPhotoUrls.length === 0) {
  throw new Error('Expected at least one broken placeholder URL for image error-state testing');
}

if (placesWithIncompleteUploaders.length > 0) {
  throw new Error(`Missing photoUploadedBy entries for ${placesWithIncompleteUploaders.length} places`);
}

if (placesWithHashMismatch.length > 0) {
  throw new Error(`photoHashes count does not match photoUrls for ${placesWithHashMismatch.length} places`);
}

console.log('Photo validation passed.');
