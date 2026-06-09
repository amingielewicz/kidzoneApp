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
  TextField,
  CircularProgress,
  Avatar,
  Chip,
} from '@mui/material';
import {
  collection,
  query,
  orderBy,
  getDocs,
  limit,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { AppUser } from '../types';

function formatDate(millis: number): string {
  return new Date(millis).toLocaleDateString('pl-PL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

export function UsersPage() {
  const [users, setUsers] = useState<AppUser[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<AppUser[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUsers();
  }, []);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredUsers(users);
    } else {
      const q = searchQuery.toLowerCase();
      setFilteredUsers(
        users.filter(
          (u) =>
            u.name.toLowerCase().includes(q) ||
            u.email.toLowerCase().includes(q) ||
            u.id.toLowerCase().includes(q)
        )
      );
    }
  }, [searchQuery, users]);

  async function fetchUsers() {
    setLoading(true);
    try {
      const snap = await getDocs(
        query(collection(db, 'users'), orderBy('createdAtMillis', 'desc'), limit(200))
      );
      setUsers(snap.docs.map((d) => ({ id: d.id, ...d.data() } as AppUser)));
    } catch (err) {
      console.error('Failed to fetch users:', err);
    } finally {
      setLoading(false);
    }
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
        Użytkownicy ({users.length})
      </Typography>

      <TextField
        fullWidth
        placeholder="Szukaj po nazwie, emailu lub ID..."
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
        sx={{ mb: 3 }}
        size="small"
      />

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Użytkownik</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Rola</TableCell>
              <TableCell>Miejsca</TableCell>
              <TableCell>Opinie</TableCell>
              <TableCell>Data rejestracji</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredUsers.map((user) => (
              <TableRow key={user.id} hover>
                <TableCell>
                  <Box display="flex" alignItems="center" gap={1}>
                    <Avatar
                      src={user.avatarUrl}
                      sx={{ width: 32, height: 32, fontSize: 14 }}
                    >
                      {user.name?.charAt(0) || '?'}
                    </Avatar>
                    <Typography variant="body2" fontWeight={500}>
                      {user.name || '(bez nazwy)'}
                    </Typography>
                  </Box>
                </TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>
                  {user.role === 'admin' ? (
                    <Chip label="Admin" color="primary" size="small" />
                  ) : (
                    <Chip label="User" variant="outlined" size="small" />
                  )}
                </TableCell>
                <TableCell>{user.placesAddedCount || 0}</TableCell>
                <TableCell>{user.reviewsCount || 0}</TableCell>
                <TableCell>{formatDate(user.createdAtMillis)}</TableCell>
              </TableRow>
            ))}
            {filteredUsers.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  {searchQuery ? 'Brak wyników' : 'Brak użytkowników'}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
