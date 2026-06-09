import { useEffect, useState } from 'react';
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
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import VisibilityIcon from '@mui/icons-material/Visibility';
import {
  collection,
  query,
  orderBy,
  getDocs,
  doc,
  deleteDoc,
  limit,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { Place, PLACE_CATEGORY_LABELS, PlaceCategory } from '../types';

function formatDate(millis: number): string {
  return new Date(millis).toLocaleDateString('pl-PL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

export function PlacesPage() {
  const [places, setPlaces] = useState<Place[]>([]);
  const [filteredPlaces, setFilteredPlaces] = useState<Place[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [detailPlace, setDetailPlace] = useState<Place | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Place | null>(null);

  useEffect(() => {
    fetchPlaces();
  }, []);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredPlaces(places);
    } else {
      const q = searchQuery.toLowerCase();
      setFilteredPlaces(
        places.filter(
          (p) =>
            p.name.toLowerCase().includes(q) ||
            p.address.toLowerCase().includes(q) ||
            p.id.toLowerCase().includes(q)
        )
      );
    }
  }, [searchQuery, places]);

  async function fetchPlaces() {
    setLoading(true);
    try {
      const snap = await getDocs(
        query(collection(db, 'places'), orderBy('createdAtMillis', 'desc'), limit(200))
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
    setConfirmDelete(null);
    await fetchPlaces();
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
      <Typography variant="h4" fontWeight={700} mb={3}>
        Miejsca ({places.length})
      </Typography>

      <TextField
        fullWidth
        placeholder="Szukaj po nazwie, adresie lub ID..."
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
        sx={{ mb: 3 }}
        size="small"
      />

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Nazwa</TableCell>
              <TableCell>Kategoria</TableCell>
              <TableCell>Adres</TableCell>
              <TableCell>Ocena</TableCell>
              <TableCell>Opinie</TableCell>
              <TableCell>Data</TableCell>
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
                  <Tooltip title="Szczegóły">
                    <IconButton size="small" onClick={() => setDetailPlace(place)}>
                      <VisibilityIcon />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Usuń miejsce">
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => setConfirmDelete(place)}
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
                  {searchQuery ? 'Brak wyników' : 'Brak miejsc'}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Detail Dialog */}
      <Dialog
        open={!!detailPlace}
        onClose={() => setDetailPlace(null)}
        maxWidth="sm"
        fullWidth
      >
        {detailPlace && (
          <>
            <DialogTitle>{detailPlace.name}</DialogTitle>
            <DialogContent dividers>
              <Box display="flex" flexDirection="column" gap={1.5}>
                <Typography variant="body2">
                  <strong>ID:</strong> {detailPlace.id}
                </Typography>
                <Typography variant="body2">
                  <strong>Kategoria:</strong>{' '}
                  {PLACE_CATEGORY_LABELS[detailPlace.category as PlaceCategory] || detailPlace.category}
                </Typography>
                <Typography variant="body2">
                  <strong>Adres:</strong> {detailPlace.address}
                </Typography>
                <Typography variant="body2">
                  <strong>Opis:</strong> {detailPlace.description || '(brak)'}
                </Typography>
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
                <Typography variant="body2">
                  <strong>Zdjęcia:</strong> {detailPlace.photoUrls?.length || 0}
                </Typography>
                {detailPlace.photoUrls?.length > 0 && (
                  <Box display="flex" gap={1} flexWrap="wrap">
                    {detailPlace.photoUrls.map((url, i) => (
                      <a key={i} href={url} target="_blank" rel="noopener noreferrer">
                        <img
                          src={url}
                          alt={`Zdjęcie ${i + 1}`}
                          style={{
                            width: 80,
                            height: 80,
                            objectFit: 'cover',
                            borderRadius: 4,
                          }}
                        />
                      </a>
                    ))}
                  </Box>
                )}
              </Box>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setDetailPlace(null)}>Zamknij</Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Confirm Delete Dialog */}
      <Dialog open={!!confirmDelete} onClose={() => setConfirmDelete(null)}>
        <DialogTitle>Usunąć miejsce?</DialogTitle>
        <DialogContent>
          <Typography>
            Czy na pewno chcesz usunąć <strong>{confirmDelete?.name}</strong>?
            <br />
            Ta operacja jest nieodwracalna.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDelete(null)}>Anuluj</Button>
          <Button
            variant="contained"
            color="error"
            onClick={() => confirmDelete && handleDelete(confirmDelete)}
          >
            Usuń
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
