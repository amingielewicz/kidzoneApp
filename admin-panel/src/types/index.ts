// --- Place ---
export interface Place {
  id: string;
  name: string;
  description: string;
  category: PlaceCategory;
  latitude: number;
  longitude: number;
  address: string;
  amenities: string[];
  photoUrls: string[];
  ownerUserId: string;
  averageRating: number;
  reviewsCount: number;
  createdAtMillis: number;
  geohash?: string;
}

export type PlaceCategory =
  | 'PLAYGROUND'
  | 'PLAY_ROOM'
  | 'CAFE'
  | 'RESTAURANT'
  | 'PARK'
  | 'ATTRACTION'
  | 'OTHER';

export const PLACE_CATEGORY_LABELS: Record<PlaceCategory, string> = {
  PLAYGROUND: 'Plac zabaw',
  PLAY_ROOM: 'Sala zabaw',
  CAFE: 'Kawiarnia rodzinna',
  RESTAURANT: 'Restauracja',
  PARK: 'Park',
  ATTRACTION: 'Atrakcja',
  OTHER: 'Inne',
};

// --- Review ---
export interface Review {
  id: string;
  placeId: string;
  userId: string;
  authorName: string;
  rating: number;
  comment: string;
  photoUrls: string[];
  createdAtMillis: number;
}

// --- User ---
export interface AppUser {
  id: string;
  name: string;
  email: string;
  avatarUrl?: string;
  placesAddedCount: number;
  reviewsCount: number;
  createdAtMillis: number;
  role?: string;
}

// --- Reports ---
export interface PlaceReport {
  id: string;
  placeId: string;
  reporterId: string;
  reason: PlaceReportReason;
  comment: string;
  status: ReportStatus;
  createdAtMillis: number;
  resolvedAtMillis?: number;
}

export type PlaceReportReason =
  | 'NOT_EXISTS'
  | 'INAPPROPRIATE'
  | 'DUPLICATE'
  | 'FALSE_DATA'
  | 'OTHER';

export const PLACE_REPORT_REASON_LABELS: Record<PlaceReportReason, string> = {
  NOT_EXISTS: 'Miejsce nie istnieje / zamknięte',
  INAPPROPRIATE: 'Nieodpowiednia treść',
  DUPLICATE: 'Duplikat innego miejsca',
  FALSE_DATA: 'Fałszywe dane',
  OTHER: 'Inne',
};

export interface ReviewReport {
  id: string;
  reviewId: string;
  reporterId: string;
  reason: ReviewReportReason;
  comment: string;
  status: ReportStatus;
  createdAtMillis: number;
  resolvedAtMillis?: number;
}

export type ReviewReportReason =
  | 'SPAM'
  | 'OFFENSIVE'
  | 'FALSE_INFO'
  | 'NOT_RELEVANT'
  | 'OTHER';

export const REVIEW_REPORT_REASON_LABELS: Record<ReviewReportReason, string> = {
  SPAM: 'Spam / reklama',
  OFFENSIVE: 'Obraźliwa treść',
  FALSE_INFO: 'Fałszywe informacje',
  NOT_RELEVANT: 'Nie dotyczy tego miejsca',
  OTHER: 'Inne',
};

export interface PhotoReport {
  id: string;
  photoUrl: string;
  reporterId: string;
  reason: PhotoReportReason;
  comment: string;
  status: ReportStatus;
  createdAtMillis: number;
  resolvedAtMillis?: number;
}

export type PhotoReportReason =
  | 'INAPPROPRIATE'
  | 'NOT_RELEVANT'
  | 'COPYRIGHT'
  | 'OFFENSIVE'
  | 'OTHER';

export const PHOTO_REPORT_REASON_LABELS: Record<PhotoReportReason, string> = {
  INAPPROPRIATE: 'Nieodpowiednia treść',
  NOT_RELEVANT: 'Niezwiązane z miejscem',
  COPYRIGHT: 'Narusza prawa autorskie',
  OFFENSIVE: 'Obraźliwe / wulgarne',
  OTHER: 'Inne',
};

export type ReportStatus = 'pending' | 'resolved' | 'dismissed';

// --- Place Change Requests ---
export interface PlaceChangeRequest {
  id: string;
  placeId: string;
  requesterId: string;
  type: 'EDIT' | 'LOCATION';
  changes: Record<string, unknown>;
  status: ReportStatus;
  createdAtMillis: number;
  resolvedAtMillis?: number;
}
