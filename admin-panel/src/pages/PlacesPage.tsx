import { useEffect, useState, useRef } from 'react';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Chip,
  TextField,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Tooltip,
  Rating,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  TableSortLabel,
  Tabs,
  Tab,
  FormControlLabel,
  Checkbox,
  Grid,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import CancelIcon from '@mui/icons-material/Cancel';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import {
  collection,
  query,
  orderBy,
  getDocs,
  getDoc,
  doc,
  deleteDoc,
  updateDoc,
  limit,
  where,
  arrayUnion,
} from 'firebase/firestore';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { db, storage } from '../services/firebase';
import { Place, PLACE_CATEGORY_LABELS, PlaceCategory } from '../types';

const ALL_AMENITIES: Record<string, string> = {
  CHANGING_TABLE: 'Przewijak',
  TOILET: 'Czysta toaleta',
  STROLLER_ACCESS: 'Dostęp dla wózka',
  PARKING: 'Parking',
  FENCING: 'Ogrodzenie',
  SOFT_SURFACE: 'Miękka nawierzchnia',
  SHADED_BENCHES: 'Ławki w cieniu',
  TODDLER_ZONE: 'Strefa 0–3',
  CAR_FREE_AREA: 'Brak ruchu samochodowego',
  KIDS_MENU: 'Menu dziecięce',
  HIGH_CHAIR: 'Krzesełka do karmienia',
  KIDS_TABLEWARE: 'Naczynia dziecięce',
  FAST_SERVICE: 'Szybka obsługa',
  KIDS_ENTERTAINMENT: 'Kredki, zabawki',
  KIDS_CORNER_VISIBLE: 'Kącik widoczny od stolika',
  AGE_ZONES: 'Podział na strefy wiekowe',
  ANIMATOR: 'Animator',
  MONITORING: 'Monitoring',
  TOY_SANITIZATION: 'Dezynfekcja zabawek',
  PARENT_ZONE: 'Strefa dla rodziców',
  LOCKERS: 'Szafki na rzeczy',
  WIFI: 'WiFi',
};

function formatDate(millis: number): string {
  return new Date(millis).toLocaleDateString('pl-PL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

type SortField = 'name' | 'averageRating' | 'reviewsCount' | 'createdAtMillis';
type SortDir = 'asc' | 'desc';

export function PlacesPage() {
  const [places, setPlaces] = useState<Place[]>([]);
  const [filteredPlaces, setFilteredPlaces] = useState<Place[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<string>('all');
  const [sortField, setSortField] = useState<SortField>('createdAtMillis');
  const [sortDir, setSortDir] = useState<SortDir>('desc');
  const [loading, setLoading] = useState(true);

  // Detail/Edit dialog
  const [detailPlace, setDetailPlace] = useState<Place | null>(null);
  const [detailTab, setDetailTab] = useState(0);
  const [editName, setEditName] = useState('');
  const [editAddress, setEditAddress] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editCategory, setEditCategory] = useState('');
  const [editLat, setEditLat] = useState('');
  const [editLng, setEditLng] = useState('');
  const [editAmenities, setEditAmenities] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  // Reviews for place
  const [placeReviews, setPlaceReviews] = useState<any[]>([]);
  const [loadingReviews, setLoadingReviews] = useState(false);

  // Delete review with reason
  const [deleteReviewDialog, setDeleteReviewDialog] = useState<{ open: boolean; review: any | null }>({ open: false, review: null });
  const [deleteReviewReason, setDeleteReviewReason] = useState('');

  // Confirm
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmTitle, setConfirmTitle] = useState('');
  const confirmActionRef = useRef<(() => Promise<void>) | null>(null);

  useEffect(() => {
    fetchPlaces();
  }, []);

  useEffect(() => {
    let result = [...places];

    // Filter by category
    if (categoryFilter !== 'all') {
      result = result.filter((p) => p.category === categoryFilter);
    }

    // Filter by search
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter(
        (p) =>
          p.name.toLowerCase().includes(q) ||
          p.address.toLowerCase().includes(q) ||
          p.id.toLowerCase().includes(q)
      );
    }

    // Sort
    result.sort((a, b) => {
      let aVal: any = a[sortField];
      let bVal: any = b[sortField];
      if (typeof aVal === 'string') aVal = aVal.toLowerCase();
      if (typeof bVal === 'string') bVal = bVal.toLowerCase();
      if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });

    setFilteredPlaces(result);
  }, [searchQuery, places, categoryFilter, sortField, sortDir]);

  async function fetchPlaces() {
    setLoading(true);
    try {
      const snap = await getDocs(
        query(collection(db, 'places'), orderBy('createdAtMillis', 'desc'), limit(500))
      );
      setPlaces(snap.docs.map((d) => ({ id: d.id, ...d.data() } as Place)));
    } catch (err) {
      console.error('Failed to fetch places:', err);
    } finally {
      setLoading(false);
    }
  }

  // Delete place with reason
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteReason, setDeleteReason] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<Place | null>(null);

  function openDeleteDialog(place: Place) {
    setDeleteTarget(place);
    setDeleteReason('');
    setDeleteDialogOpen(true);
  }

  async function handleDelete() {
    if (!deleteTarget || !deleteReason.trim()) return;
    const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID || 'playground-705e7162';
    const url = `https://us-central1-${projectId}.cloudfunctions.net/adminDeletePlace?placeId=${deleteTarget.id}&reason=${encodeURIComponent(deleteReason)}`;
    try { await fetch(url); } catch (e) { console.error('Failed to delete place:', e); }
    setDeleteDialogOpen(false);
    setDetailPlace(null);
    await fetchPlaces();
  }

  // Owner email
  const [ownerEmail, setOwnerEmail] = useState('');

  function openDetail(place: Place) {
    setDetailPlace(place);
    setDetailTab(0);
    setEditName(place.name);
    setEditAddress(place.address);
    setEditDescription(place.description || '');
    setEditCategory(place.category || '');
    setEditLat(String(place.latitude || ''));
    setEditLng(String(place.longitude || ''));
    setEditAmenities(place.amenities || []);
    setPlaceReviews([]);
    setOwnerEmail('');
    // Fetch owner email
    if (place.ownerUserId) {
      getDoc(doc(db, 'users', place.ownerUserId)).then((snap) => {
        if (snap.exists()) setOwnerEmail(snap.data()?.email || '');
      }).catch(() => {});
    }
  }

  async function saveBasicInfo() {
    if (!detailPlace) return;
    setSaving(true);
    try {
      const updates: any = {
        name: editName,
        address: editAddress,
        description: editDescription,
        category: editCategory,
        latitude: parseFloat(editLat) || detailPlace.latitude,
        longitude: parseFloat(editLng) || detailPlace.longitude,
        amenities: editAmenities,
      };
      await updateDoc(doc(db, 'places', detailPlace.id), updates);
      setDetailPlace((prev) => prev ? { ...prev, ...updates } : null);
      await fetchPlaces();
    } catch (err) {
      console.error('Failed to save:', err);
    } finally {
      setSaving(false);
    }
  }

  async function fetchReviews(placeId: string) {
    setLoadingReviews(true);
    try {
      const snap = await getDocs(
        query(collection(db, 'reviews'), where('placeId', '==', placeId))
      );
      const reviews = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      reviews.sort((a: any, b: any) => (b.createdAtMillis || 0) - (a.createdAtMillis || 0));
      setPlaceReviews(reviews);
    } catch (err) {
      console.error('Failed to fetch reviews:', err);
    } finally {
      setLoadingReviews(false);
    }
  }

  async function deleteReview(reviewId: string, reason: string) {
    const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID || 'playground-705e7162';
    const url = `https://us-central1-${projectId}.cloudfunctions.net/adminDeleteReview?reviewId=${encodeURIComponent(reviewId)}&reason=${encodeURIComponent(reason)}`;
    try {
      const resp = await fetch(url);
      if (!resp.ok) {
        console.error('Cloud Function error:', resp.status);
      }
    } catch (err) {
      console.error('Failed to delete review via Cloud Function:', err);
    }
    if (detailPlace) {
      await fetchReviews(detailPlace.id);
      await fetchPlaces();
    }
  }

  // Photos upload
  const [uploading, setUploading] = useState(false);

  async function uploadPhoto(file: File) {
    if (!detailPlace) return;
    setUploading(true);
    try {
      const fileName = `${Date.now()}_${file.name}`;
      const storageRef = ref(storage, `places/${detailPlace.id}/${fileName}`);
      await uploadBytes(storageRef, file);
      const downloadUrl = await getDownloadURL(storageRef);

      // Get current photoUploadedBy map
      const placeSnap = await getDoc(doc(db, 'places', detailPlace.id));
      const currentMap = placeSnap.data()?.photoUploadedBy || {};
      currentMap[downloadUrl] = 'admin';

      await updateDoc(doc(db, 'places', detailPlace.id), {
        photoUrls: arrayUnion(downloadUrl),
        photoUploadedBy: currentMap,
      });
      const newUrls = [...(detailPlace.photoUrls || []), downloadUrl];
      setDetailPlace((prev) => prev ? { ...prev, photoUrls: newUrls } : null);
      await fetchPlaces();
    } catch (err) {
      console.error('Upload failed:', err);
    } finally {
      setUploading(false);
    }
  }

  // Delete photo with reason
  const [deletePhotoDialogOpen, setDeletePhotoDialogOpen] = useState(false);
  const [deletePhotoReason, setDeletePhotoReason] = useState('');
  const [deletePhotoUrl, setDeletePhotoUrl] = useState('');

  function openDeletePhotoDialog(url: string) {
    setDeletePhotoUrl(url);
    setDeletePhotoReason('');
    setDeletePhotoDialogOpen(true);
  }

  async function handleDeletePhoto() {
    if (!detailPlace || !deletePhotoReason.trim()) return;
    const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID || 'playground-705e7162';
    const url = `https://us-central1-${projectId}.cloudfunctions.net/adminDeletePhotoFromPlace?placeId=${detailPlace.id}&photoUrl=${encodeURIComponent(deletePhotoUrl)}&reason=${encodeURIComponent(deletePhotoReason)}`;
    try { await fetch(url); } catch (e) { console.error('Failed to delete photo:', e); }
    const updatedUrls = (detailPlace.photoUrls || []).filter((u) => u !== deletePhotoUrl);
    setDetailPlace((prev) => (prev ? { ...prev, photoUrls: updatedUrls } : null));
    setDeletePhotoDialogOpen(false);
    await fetchPlaces();
  }

  function handleSort(field: SortField) {
    if (sortField === field) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDir('asc');
    }
  }

  function confirm(title: string, action: () => Promise<void>) {
    confirmActionRef.current = action;
    setConfirmTitle(title);
    setConfirmOpen(true);
  }

  async function handleConfirm() {
    if (confirmActionRef.current) await confirmActionRef.current();
    setConfirmOpen(false);
    confirmActionRef.current = null;
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={6}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} mb={1}>
        Miejsca ({places.length})
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Przeglądaj, edytuj i zarządzaj wszystkimi miejscami w aplikacji.
      </Typography>

      <Box display="flex" gap={2} mb={3} flexWrap="wrap">
        <TextField
          placeholder="Szukaj po nazwie, adresie lub ID..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          size="small"
          sx={{ flexGrow: 1, minWidth: 200 }}
        />
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Kategoria</InputLabel>
          <Select
            value={categoryFilter}
            label="Kategoria"
            onChange={(e) => setCategoryFilter(e.target.value)}
          >
            <MenuItem value="all">Wszystkie</MenuItem>
            {Object.entries(PLACE_CATEGORY_LABELS).map(([key, label]) => (
              <MenuItem key={key} value={key}>
                {label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'name'}
                  direction={sortField === 'name' ? sortDir : 'asc'}
                  onClick={() => handleSort('name')}
                >
                  Nazwa
                </TableSortLabel>
              </TableCell>
              <TableCell sx={{ width: 130 }}>Kategoria</TableCell>
              <TableCell>Adres</TableCell>
              <TableCell sx={{ width: 160 }}>
                <TableSortLabel
                  active={sortField === 'averageRating'}
                  direction={sortField === 'averageRating' ? sortDir : 'asc'}
                  onClick={() => handleSort('averageRating')}
                >
                  Ocena
                </TableSortLabel>
              </TableCell>
              <TableCell sx={{ width: 80 }}>
                <TableSortLabel
                  active={sortField === 'reviewsCount'}
                  direction={sortField === 'reviewsCount' ? sortDir : 'asc'}
                  onClick={() => handleSort('reviewsCount')}
                >
                  Opinie
                </TableSortLabel>
              </TableCell>
              <TableCell sx={{ width: 120 }}>
                <TableSortLabel
                  active={sortField === 'createdAtMillis'}
                  direction={sortField === 'createdAtMillis' ? sortDir : 'asc'}
                  onClick={() => handleSort('createdAtMillis')}
                >
                  Data
                </TableSortLabel>
              </TableCell>
              <TableCell sx={{ width: 100 }}>Akcje</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredPlaces.map((place) => (
              <TableRow key={place.id} hover>
                <TableCell>
                  <Typography variant="body2" fontWeight={600}>
                    {place.name}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Chip
                    label={PLACE_CATEGORY_LABELS[place.category as PlaceCategory] || place.category}
                    size="small"
                    variant="outlined"
                  />
                </TableCell>
                <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {place.address}
                </TableCell>
                <TableCell>
                  <Box display="flex" alignItems="center" gap={0.5}>
                    <Rating value={place.averageRating} precision={0.1} size="small" readOnly />
                    <Typography variant="caption">
                      ({place.averageRating?.toFixed(1) || '—'})
                    </Typography>
                  </Box>
                </TableCell>
                <TableCell>{place.reviewsCount}</TableCell>
                <TableCell>{formatDate(place.createdAtMillis)}</TableCell>
                <TableCell>
                  <Tooltip title="Szczegóły / Edycja">
                    <IconButton size="small" onClick={() => openDetail(place)}>
                      <EditIcon />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Usuń miejsce">
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => openDeleteDialog(place)}
                    >
                      <DeleteIcon />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
            {filteredPlaces.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  {searchQuery || categoryFilter !== 'all' ? 'Brak wyników' : 'Brak miejsc'}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Detail / Edit Dialog */}
      <Dialog
        open={!!detailPlace}
        onClose={() => setDetailPlace(null)}
        maxWidth="md"
        fullWidth
      >
        {detailPlace && (
          <>
            <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <Box>
                {detailPlace.name}
                <Typography variant="caption" display="block" color="text.secondary">
                  ID: {detailPlace.id}
                </Typography>
              </Box>
              <IconButton size="small" onClick={() => setDetailPlace(null)}><CancelIcon /></IconButton>
            </DialogTitle>
            <DialogContent dividers>
              <Tabs value={detailTab} onChange={(_, v) => { setDetailTab(v); if (v === 2) fetchReviews(detailPlace.id); }} sx={{ mb: 2 }}>
                <Tab label="Dane" />
                <Tab label={`Zdjęcia (${detailPlace.photoUrls?.length || 0})`} />
                <Tab label={`Opinie (${detailPlace.reviewsCount || 0})`} />
              </Tabs>

              {/* Tab 0: Basic info */}
              {detailTab === 0 && (
                <Box display="flex" flexDirection="column" gap={2}>
                  <TextField label="Nazwa" value={editName} onChange={(e) => setEditName(e.target.value)} fullWidth size="small" />
                  <TextField label="Adres" value={editAddress} onChange={(e) => setEditAddress(e.target.value)} fullWidth size="small" />
                  <TextField label="Opis" value={editDescription} onChange={(e) => setEditDescription(e.target.value)} fullWidth size="small" multiline rows={3} />

                  <FormControl size="small" fullWidth>
                    <InputLabel>Kategoria</InputLabel>
                    <Select value={editCategory} label="Kategoria" onChange={(e) => setEditCategory(e.target.value)}>
                      {Object.entries(PLACE_CATEGORY_LABELS).map(([key, label]) => (
                        <MenuItem key={key} value={key}>{label}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <Grid container spacing={1}>
                    <Grid item xs={6}>
                      <TextField label="Szerokość (lat)" value={editLat} onChange={(e) => setEditLat(e.target.value)} fullWidth size="small" />
                    </Grid>
                    <Grid item xs={6}>
                      <TextField label="Długość (lng)" value={editLng} onChange={(e) => setEditLng(e.target.value)} fullWidth size="small" />
                    </Grid>
                  </Grid>

                  <Box sx={{ p: 1.5, bgcolor: '#f5f5f5', borderRadius: 1 }}>
                    <Box display="flex" alignItems="center" gap={0.5}>
                      <Typography variant="body2" color="text.secondary"><strong>Właściciel UID:</strong> {detailPlace.ownerUserId || '(brak)'}</Typography>
                      {detailPlace.ownerUserId && <Tooltip title="Kopiuj UID"><IconButton size="small" onClick={() => navigator.clipboard.writeText(detailPlace.ownerUserId || '')}><ContentCopyIcon sx={{ fontSize: 14 }} /></IconButton></Tooltip>}
                    </Box>
                    <Typography variant="body2" color="text.secondary"><strong>Email:</strong> {ownerEmail || '(brak / nie pobrano)'}</Typography>
                  </Box>

                  <Typography variant="subtitle2" mt={1}>Udogodnienia:</Typography>
                  <Box display="flex" flexWrap="wrap" gap={0}>
                    {Object.entries(ALL_AMENITIES).map(([key, label]) => (
                      <FormControlLabel
                        key={key}
                        control={
                          <Checkbox
                            size="small"
                            checked={editAmenities.includes(key)}
                            onChange={(e) => {
                              if (e.target.checked) setEditAmenities((prev) => [...prev, key]);
                              else setEditAmenities((prev) => prev.filter((a) => a !== key));
                            }}
                          />
                        }
                        label={<Typography variant="body2">{label}</Typography>}
                        sx={{ width: '48%', m: 0 }}
                      />
                    ))}
                  </Box>

                  <Typography variant="body2" color="text.secondary">
                    <strong>Ocena:</strong> {detailPlace.averageRating?.toFixed(2)} ({detailPlace.reviewsCount} opinii)
                  </Typography>

                  <Button variant="contained" onClick={saveBasicInfo} disabled={saving} sx={{ alignSelf: 'flex-start' }}>
                    {saving ? <CircularProgress size={20} /> : 'Zapisz zmiany'}
                  </Button>
                </Box>
              )}

              {/* Tab 1: Photos */}
              {detailTab === 1 && (
                <Box>
                  <Box mb={2}>
                    <Button
                      variant="outlined"
                      component="label"
                      disabled={uploading}
                    >
                      {uploading ? <CircularProgress size={20} /> : 'Dodaj zdjęcie'}
                      <input
                        type="file"
                        hidden
                        accept="image/*"
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) uploadPhoto(file);
                          e.target.value = '';
                        }}
                      />
                    </Button>
                  </Box>
                  {(!detailPlace.photoUrls || detailPlace.photoUrls.length === 0) ? (
                    <Typography color="text.secondary">Brak zdjęć</Typography>
                  ) : (
                    <Box display="flex" gap={2} flexWrap="wrap">
                      {detailPlace.photoUrls.map((url, i) => (
                        <Box key={i} position="relative">
                          <a href={url} target="_blank" rel="noopener noreferrer">
                            <img
                              src={url}
                              alt={`Zdjęcie ${i + 1}`}
                              style={{
                                width: 120,
                                height: 120,
                                objectFit: 'cover',
                                borderRadius: 8,
                              }}
                            />
                          </a>
                          <IconButton
                            size="small"
                            color="error"
                            sx={{
                              position: 'absolute',
                              top: -8,
                              right: -8,
                              bgcolor: 'white',
                              boxShadow: 1,
                              '&:hover': { bgcolor: '#ffebee' },
                            }}
                            onClick={() => openDeletePhotoDialog(url)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Box>
                      ))}
                    </Box>
                  )}
                </Box>
              )}

              {/* Tab 2: Reviews */}
              {detailTab === 2 && (
                <Box>
                  {loadingReviews ? (
                    <Box display="flex" justifyContent="center" py={3}>
                      <CircularProgress size={24} />
                    </Box>
                  ) : placeReviews.length === 0 ? (
                    <Typography color="text.secondary">Brak opinii</Typography>
                  ) : (
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Autor</TableCell>
                          <TableCell>Ocena</TableCell>
                          <TableCell>Komentarz</TableCell>
                          <TableCell>Data</TableCell>
                          <TableCell>Akcje</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {placeReviews.map((review) => (
                          <TableRow key={review.id}>
                            <TableCell>{review.authorName || 'Anonim'}</TableCell>
                            <TableCell>
                              <Rating value={review.rating} size="small" readOnly />
                            </TableCell>
                            <TableCell sx={{ maxWidth: 250, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                              {review.comment || '—'}
                            </TableCell>
                            <TableCell>{formatDate(review.createdAtMillis)}</TableCell>
                            <TableCell>
                              <Tooltip title="Usuń opinię">
                                <IconButton
                                  size="small"
                                  color="error"
                                  onClick={() => {
                                    setDeleteReviewDialog({ open: true, review });
                                    setDeleteReviewReason('');
                                  }}
                                >
                                  <DeleteIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  )}
                </Box>
              )}
            </DialogContent>
            <DialogActions>
              <Button
                color="error"
                onClick={() => openDeleteDialog(detailPlace)}
              >
                Usuń miejsce
              </Button>
              <Button onClick={() => setDetailPlace(null)}>Zamknij</Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Delete Review Dialog */}
      <Dialog
        open={deleteReviewDialog.open}
        onClose={() => setDeleteReviewDialog({ open: false, review: null })}
        maxWidth="sm"
        fullWidth
      >
        {deleteReviewDialog.review && (
          <>
            <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>Usuń opinię</span>
              <IconButton size="small" onClick={() => setDeleteReviewDialog({ open: false, review: null })}><CancelIcon /></IconButton>
            </DialogTitle>
            <DialogContent>
              <Box display="flex" flexDirection="column" gap={2} mt={1}>
                <Typography variant="body2">
                  <strong>Autor:</strong> {deleteReviewDialog.review.authorName || 'Anonim'}
                </Typography>
                <Typography variant="body2">
                  <strong>Treść:</strong> {deleteReviewDialog.review.comment || '(brak)'}
                </Typography>
                <TextField
                  label="Powód usunięcia"
                  value={deleteReviewReason}
                  onChange={(e) => setDeleteReviewReason(e.target.value)}
                  fullWidth
                  multiline
                  rows={3}
                  placeholder="Podaj powód usunięcia opinii (zostanie wysłany autorowi emailem)"
                />
              </Box>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setDeleteReviewDialog({ open: false, review: null })}>Anuluj</Button>
              <Button
                variant="contained"
                color="error"
                disabled={!deleteReviewReason.trim() || saving}
                onClick={async () => {
                  setSaving(true);
                  await deleteReview(deleteReviewDialog.review.id, deleteReviewReason);
                  setDeleteReviewDialog({ open: false, review: null });
                  setSaving(false);
                }}
              >
                {saving ? <CircularProgress size={20} /> : 'Usuń i powiadom'}
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Delete Photo Dialog */}
      <Dialog open={deletePhotoDialogOpen} onClose={() => setDeletePhotoDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Usuń zdjęcie</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Podaj powód usunięcia zdjęcia. Zostanie wysłany do użytkownika, który je dodał.
          </Typography>
          <TextField
            autoFocus
            fullWidth
            multiline
            rows={3}
            label="Powód usunięcia"
            value={deletePhotoReason}
            onChange={(e) => setDeletePhotoReason(e.target.value)}
            placeholder="Wpisz powód usunięcia zdjęcia..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeletePhotoDialogOpen(false)}>Anuluj</Button>
          <Button variant="contained" color="error" disabled={!deletePhotoReason.trim()} onClick={handleDeletePhoto}>Usuń</Button>
        </DialogActions>
      </Dialog>

      {/* Delete Place Dialog */}
      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Usuń miejsce: {deleteTarget?.name}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Podaj powód usunięcia. Zostanie wysłany do właściciela miejsca.
          </Typography>
          <TextField
            autoFocus
            fullWidth
            multiline
            rows={3}
            label="Powód usunięcia"
            value={deleteReason}
            onChange={(e) => setDeleteReason(e.target.value)}
            placeholder="Wpisz powód usunięcia..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Anuluj</Button>
          <Button variant="contained" color="error" disabled={!deleteReason.trim()} onClick={handleDelete}>Usuń</Button>
        </DialogActions>
      </Dialog>

      {/* Confirm Dialog */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle>Potwierdzenie</DialogTitle>
        <DialogContent><Typography>{confirmTitle}</Typography></DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Anuluj</Button>
          <Button variant="contained" color="error" onClick={handleConfirm}>Potwierdź</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
