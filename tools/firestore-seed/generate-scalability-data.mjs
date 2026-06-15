#!/usr/bin/env node

import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const DEFAULT_PLACES_COUNT = 100;
const DEFAULT_USERS_COUNT = 25;
const DEFAULT_REVIEWS_PER_PLACE = 3;

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
      photoUrls: [],
      photoUploadedBy: {},
      photoHashes: [],
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
        photoUrls: [],
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

await mkdir(outputDir, { recursive: true });

const dataset = {
  metadata: {
    generatedAt: new Date(now).toISOString(),
    placesCount: places.length,
    usersCount: users.length,
    reviewsCount: reviews.length,
    reviewsPerPlace,
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
