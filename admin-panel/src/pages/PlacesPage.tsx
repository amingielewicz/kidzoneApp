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
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import VisibilityIcon from '@mui/icons-material/Visibility';
import {
  collection,
  query,
  orderBy,
  getDocs,
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
  const [saving, setSaving] = useState(false);

  // Reviews for place
  const [placeReviews, setPlaceReviews] = useState<any[]>([]);
  const [loadingReviews, setLoadingReviews] = useState(false);
  const [editReview, setEditReview] = useState<any | null>(null);
  const [editReviewComment, setEditReviewComment] = useState('');
  const [editReviewRating, setEditReviewRating] = useState<number>(0);

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

  async function handleDelete(place: Place) {
    await deleteDoc(doc(db, 'places', place.id));
    setConfirmDialog((p) => ({ ...p, open: false }));
    setDetailPlace(null);
    await fetchPlaces();
  }

  function openDetail(place: Place) {
    setDetailPlace(place);
    setDetailTab(0);
    setEditName(place.name);
    setEditAddress(place.address);
    setEditDescription(place.description || '');
    setPlaceReviews([]);
  }

  async function saveBasicInfo() {
    if (!detailPlace) return;
    setSaving(true);
    try {
      await updateDoc(doc(db, 'places', detailPlace.id), {
        name: editName,
        address: editAddress,
        description: editDescription,
      });
      await fetchPlaces();
      setDetailPlace((prev) =>
        prev ? { ...prev, name: editName, address: editAddress, description: editDescription } : null
      );
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
        query(collection(db, 'reviews'), where('placeId', '==', placeId), orderBy('createdAtMillis', 'desc'))
      );
      setPlaceReviews(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
    } catch (err) {
      console.error('Failed to fetch reviews:', err);
    } finally {
      setLoadingReviews(false);
    }
  }

  async function deleteReview(reviewId: string) {
    await deleteDoc(doc(db, 'reviews', reviewId));
    if (detailPlace) {
      await fetchReviews(detailPlace.id);
      // Update review count
      const newCount = placeReviews.length - 1;
      await updateDoc(doc(db, 'places', detailPlace.id), { reviewsCount: newCount >= 0 ? newCount : 0 });
      await fetchPlaces();
    }
  }

  async function saveReviewEdit() {
    if (!editReview) return;
    setSaving(true);
    try {
      await updateDoc(doc(db, 'reviews', editReview.id), {
        comment: editReviewComment,
        rating: editReviewRating,
      });
      setEditReview(null);
      if (detailPlace) {
        await fetchReviews(detailPlace.id);
        // Recalculate average
        const snap = await getDocs(
          query(collection(db, 'reviews'), where('placeId', '==', detailPlace.id))
        );
        const reviews = snap.docs.map((d) => d.data());
        if (reviews.length > 0) {
          const avg = reviews.reduce((sum, r) => sum + (r.rating || 0), 0) / reviews.length;
          await updateDoc(doc(db, 'places', detailPlace.id), { averageRating: Math.round(avg * 100) / 100 });
          await fetchPlaces();
        }
      }
    } catch (err) {
      console.error('Failed to save review:', err);
    } finally {
      setSaving(false);
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
      await updateDoc(doc(db, 'places', detailPlace.id), {
        photoUrls: arrayUnion(downloadUrl),
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

  async function deletePhoto(photoUrl: string) {
    if (!detailPlace) return;
    const updatedUrls = (detailPlace.photoUrls || []).filter((u) => u !== photoUrl);
    await updateDoc(doc(db, 'places', detailPlace.id), { photoUrls: updatedUrls });
    setDetailPlace((prev) => (prev ? { ...prev, photoUrls: updatedUrls } : null));
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
              <TableCell>Kategoria</TableCell>
              <TableCell>Adres</TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'averageRating'}
                  direction={sortField === 'averageRating' ? sortDir : 'asc'}
                  onClick={() => handleSort('averageRating')}
                >
                  Ocena
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'reviewsCount'}
                  direction={sortField === 'reviewsCount' ? sortDir : 'asc'}
                  onClick={() => handleSort('reviewsCount')}
                >
                  Opinie
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'createdAtMillis'}
                  direction={sortField === 'createdAtMillis' ? sortDir : 'asc'}
                  onClick={() => handleSort('createdAtMillis')}
                >
                  Data
                </TableSortLabel>
              </TableCell>
              <TableCell>Akcje</TableCell>
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
                      onClick={() => confirm(`Usunąć "${place.name}"?`, () => handleDelete(place))}
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
            <DialogTitle>
              {detailPlace.name}
              <Typography variant="caption" display="block" color="text.secondary">
                ID: {detailPlace.id}
              </Typography>
            </DialogTitle>
            <DialogContent dividers>
              <Tabs value={detailTab} onChange={(_, v) => { setDetailTab(v); if (v === 2) fetchReviews(detailPlace.id); }} sx={{ mb: 2 }}>
                <Tab label="Dane" />
                <Tab label={`Zdjęcia (${detailPlace.photoUrls?.length || 0})`} />
                <Tab label="Opinie" />
              </Tabs>

              {/* Tab 0: Basic info */}
              {detailTab === 0 && (
                <Box display="flex" flexDirection="column" gap={2}>
                  <TextField
                    label="Nazwa"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    fullWidth
                    size="small"
                  />
                  <TextField
                    label="Adres"
                    value={editAddress}
                    onChange={(e) => setEditAddress(e.target.value)}
                    fullWidth
                    size="small"
                  />
                  <TextField
                    label="Opis"
                    value={editDescription}
                    onChange={(e) => setEditDescription(e.target.value)}
                    fullWidth
                    size="small"
                    multiline
                    rows={3}
                  />
                  <Box display="flex" gap={2}>
                    <Typography variant="body2">
                      <strong>Kategoria:</strong> {PLACE_CATEGORY_LABELS[detailPlace.category as PlaceCategory] || detailPlace.category}
                    </Typography>
                  </Box>
                  <Typography variant="body2">
                    <strong>Współrzędne:</strong> {detailPlace.latitude}, {detailPlace.longitude}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Właściciel (UID):</strong> {detailPlace.ownerUserId}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Ocena:</strong> {detailPlace.averageRating?.toFixed(2)} ({detailPlace.reviewsCount} opinii)
                  </Typography>
                  <Typography variant="body2">
                    <strong>Udogodnienia:</strong> {detailPlace.amenities?.join(', ') || '(brak)'}
                  </Typography>
                  <Button
                    variant="contained"
                    onClick={saveBasicInfo}
                    disabled={saving}
                    sx={{ alignSelf: 'flex-start' }}
                  >
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
                            onClick={() =>
                              confirm('Usunąć to zdjęcie?', () => deletePhoto(url))
                            }
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
                              <Tooltip title="Edytuj opinię">
                                <IconButton
                                  size="small"
                                  onClick={() => {
                                    setEditReview(review);
                                    setEditReviewComment(review.comment || '');
                                    setEditReviewRating(review.rating || 0);
                                  }}
                                >
                                  <EditIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                              <Tooltip title="Usuń opinię">
                                <IconButton
                                  size="small"
                                  color="error"
                                  onClick={() =>
                                    confirm('Usunąć tę opinię?', () => deleteReview(review.id))
                                  }
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
                onClick={() => confirm(`Usunąć "${detailPlace.name}"?`, () => handleDelete(detailPlace))}
              >
                Usuń miejsce
              </Button>
              <Button onClick={() => setDetailPlace(null)}>Zamknij</Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Edit Review Dialog */}
      <Dialog
        open={!!editReview}
        onClose={() => setEditReview(null)}
        maxWidth="sm"
        fullWidth
      >
        {editReview && (
          <>
            <DialogTitle>Edytuj opinię</DialogTitle>
            <DialogContent>
              <Box display="flex" flexDirection="column" gap={2} mt={1}>
                <Typography variant="body2">
                  <strong>Autor:</strong> {editReview.authorName}
                </Typography>
                <Box>
                  <Typography variant="body2" mb={0.5}>Ocena:</Typography>
                  <Rating
                    value={editReviewRating}
                    onChange={(_, v) => setEditReviewRating(v || 0)}
                  />
                </Box>
                <TextField
                  label="Komentarz"
                  value={editReviewComment}
                  onChange={(e) => setEditReviewComment(e.target.value)}
                  fullWidth
                  multiline
                  rows={3}
                />
              </Box>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setEditReview(null)}>Anuluj</Button>
              <Button variant="contained" onClick={saveReviewEdit} disabled={saving}>
                {saving ? <CircularProgress size={20} /> : 'Zapisz'}
              </Button>
            </DialogActions>
          </>
        )}
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
