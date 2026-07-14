#!/usr/bin/env node

import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const DEFAULT_PLACES_COUNT = 100;
const DEFAULT_USERS_COUNT = 25;
const DEFAULT_REVIEWS_PER_PLACE = 3;
const DEFAULT_PLACE_PHOTO_EVERY = 1;
const DEFAULT_REVIEW_PHOTO_EVERY = 1;
const BROKEN_PLACE_PHOTO_URL = 'https://example.invalid/kidzone/broken-place-photo.jpg';

const categories = [
  'PLAYGROUND',
  'INDOOR_PLAYGROUND',
  'CAFE',
  'RESTAURANT',
  'PARK',
  'ATTRACTION',
  'OTHER'
];

const amenities = [
  'CHANGING_TABLE',
  'TOILET',
  'STROLLER_ACCESS',
  'PARKING',
  'HIGH_CHAIR',
  'KIDS_MENU',
  'PLAY_CORNER',
  'BREASTFEEDING_FRIENDLY'
];

const cityCenter = {
  latitude: 51.7592,
  longitude: 19.4560
};

const args = Object.fromEntries(
  process.argv.slice(2).map((arg) => {
    const [key, value] = arg.replace(/^--/, '').split('=');
    return [key, value ?? true];
  })
);

const placesCount = Number(args.places ?? DEFAULT_PLACES_COUNT);
const usersCount = Number(args.users ?? DEFAULT_USERS_COUNT);
const reviewsPerPlace = Number(args.reviewsPerPlace ?? DEFAULT_REVIEWS_PER_PLACE);
const includePhotos = args.photos !== 'false';
const placePhotoEvery = Number(args.placePhotoEvery ?? DEFAULT_PLACE_PHOTO_EVERY);
const reviewPhotoEvery = Number(args.reviewPhotoEvery ?? DEFAULT_REVIEW_PHOTO_EVERY);
const includeBrokenPhoto = args.brokenPhoto !== 'false';
const outputDir = path.resolve(args.output ?? 'output');

if (!Number.isInteger(placesCount) || placesCount <= 0) {
  throw new Error('--places must be a positive integer');
}

if (!Number.isInteger(usersCount) || usersCount <= 0) {
  throw new Error('--users must be a positive integer');
}

if (!Number.isInteger(reviewsPerPlace) || reviewsPerPlace < 0) {
  throw new Error('--reviewsPerPlace must be a non-negative integer');
}

if (!Number.isInteger(placePhotoEvery) || placePhotoEvery <= 0) {
  throw new Error('--placePhotoEvery must be a positive integer');
}

if (!Number.isInteger(reviewPhotoEvery) || reviewPhotoEvery <= 0) {
  throw new Error('--reviewPhotoEvery must be a positive integer');
}

const now = Date.now();

function pad(value, size = 4) {
  return String(value).padStart(size, '0');
}

function deterministicOffset(index, spread = 0.18) {
  const angle = index * 137.508;
  const radius = ((index % 50) / 50) * spread;
  const radians = angle * Math.PI / 180;
  return {
    latitude: Math.cos(radians) * radius,
    longitude: Math.sin(radians) * radius
  };
}

function placeholderPhotoUrl(kind, index, photoIndex = 0) {
  const seed = `kidzone-${kind}-${pad(index + 1)}-${photoIndex + 1}`;
  return `https://picsum.photos/seed/${seed}/960/720`;
}

function placeholderPhotoHash(kind, index, photoIndex = 0) {
  return `placeholder-${kind}-${pad(index + 1)}-${photoIndex + 1}`;
}

function createPlacePhotos(index, ownerUserId) {
  if (!includePhotos || index % placePhotoEvery !== 0) {
    return {
      photoUrls: [],
      photoUploadedBy: {},
      photoHashes: {}
    };
  }

  const photoUrls = [placeholderPhotoUrl('place', index)];

  if (includeBrokenPhoto && index === 0) {
    photoUrls.push(BROKEN_PLACE_PHOTO_URL);
  }

  return {
    photoUrls,
    photoUploadedBy: Object.fromEntries(photoUrls.map((url) => [url, ownerUserId])),
    photoHashes: Object.fromEntries(
      photoUrls.map((url, photoIndex) => [url, placeholderPhotoHash('place', index, photoIndex)])
    )
  };
}

function createReviewPhotos(globalIndex) {
  if (!includePhotos || globalIndex % reviewPhotoEvery !== 0) {
    return [];
  }

  return [placeholderPhotoUrl('review', globalIndex)];
}

function createUsers(count) {
  return Array.from({ length: count }, (_, index) => {
    const id = `test-user-${pad(index + 1)}`;
    return {
      id,
      name: `Testowy Rodzic ${index + 1}`,
      email: `test-user-${index + 1}@example.invalid`,
      firstName: `Rodzic`,
      lastName: `Testowy ${index + 1}`,
      avatarUrl: null,
      placesAddedCount: 0,
      reviewsCount: 0,
      createdAtMillis: now - index * 86_400_000,
      nameLowercase: `testowy rodzic ${index + 1}`,
      badgeEarnedAt: {},
      bannedUntilMillis: 0,
      banReason: '',
      emailNotificationsEnabled: false
    };
  });
}

function createPlaces(count, users) {
  return Array.from({ length: count }, (_, index) => {
    const owner = users[index % users.length];
    const category = categories[index % categories.length];
    const offset = deterministicOffset(index);
    const selectedAmenities = amenities.filter((_, amenityIndex) => (index + amenityIndex) % 3 === 0);
    const reviewsCount = Math.max(1, reviewsPerPlace);
    const averageRating = Number((3.5 + (index % 15) / 10).toFixed(1));
    const placePhotos = createPlacePhotos(index, owner.id);

    owner.placesAddedCount += 1;

    return {
      id: `test-place-${pad(index + 1)}`,
      ownerUserId: owner.id,
      name: `Testowe miejsce ${index + 1}`,
      description: `Wygenerowane miejsce testowe do walidacji skalowalności aplikacji kidZone. Rekord ${index + 1}.`,
      category,
      latitude: Number((cityCenter.latitude + offset.latitude).toFixed(6)),
      longitude: Number((cityCenter.longitude + offset.longitude).toFixed(6)),
      address: `Testowa ${index + 1}, Łódź`,
      averageRating,
      reviewsCount,
      amenities: selectedAmenities,
      photoUrls: placePhotos.photoUrls,
      photoUploadedBy: placePhotos.photoUploadedBy,
      photoHashes: placePhotos.photoHashes,
      createdAtMillis: now - index * 3_600_000,
      geohash: ''
    };
  });
}

function createReviews(places, users) {
  const reviews = [];

  for (const place of places) {
    for (let reviewIndex = 0; reviewIndex < reviewsPerPlace; reviewIndex++) {
      const globalIndex = reviews.length;
      const user = users[(globalIndex + reviewIndex) % users.length];
      const rating = 3 + ((globalIndex + reviewIndex) % 3);

      user.reviewsCount += 1;

      reviews.push({
        id: `test-review-${pad(globalIndex + 1, 6)}`,
        placeId: place.id,
        userId: user.id,
        authorName: user.name,
        rating,
        comment: `Testowa opinia ${reviewIndex + 1} dla ${place.name}.`,
        photoUrls: createReviewPhotos(globalIndex),
        createdAtMillis: place.createdAtMillis + reviewIndex * 60_000,
        updatedAtMillis: place.createdAtMillis + reviewIndex * 60_000,
        reportedAsSpam: false
      });
    }
  }

  return reviews;
}

const users = createUsers(usersCount);
const places = createPlaces(placesCount, users);
const reviews = createReviews(places, users);
const placesWithPhotos = places.filter((place) => place.photoUrls.length > 0).length;
const reviewsWithPhotos = reviews.filter((review) => review.photoUrls.length > 0).length;
const brokenPhotoUrls = includePhotos && includeBrokenPhoto ? [BROKEN_PLACE_PHOTO_URL] : [];

await mkdir(outputDir, { recursive: true });

const dataset = {
  metadata: {
    generatedAt: new Date(now).toISOString(),
    placesCount: places.length,
    usersCount: users.length,
    reviewsCount: reviews.length,
    reviewsPerPlace,
    photosEnabled: includePhotos,
    placesWithPhotos,
    reviewsWithPhotos,
    brokenPhotoUrls,
    note: 'Generated test data for kidZone scalability validation. Do not use in production.'
  },
  collections: {
    users,
    places,
    reviews
  }
};

const outputFile = path.join(outputDir, `kidzone-scalability-${placesCount}.json`);
await writeFile(outputFile, `${JSON.stringify(dataset, null, 2)}\n`, 'utf8');

console.log(`Generated ${outputFile}`);
console.log(`users=${users.length} places=${places.length} reviews=${reviews.length}`);
console.log(`placesWithPhotos=${placesWithPhotos} reviewsWithPhotos=${reviewsWithPhotos}`);
if (brokenPhotoUrls.length > 0) {
  console.log(`brokenPhotoUrls=${brokenPhotoUrls.join(',')}`);
}
