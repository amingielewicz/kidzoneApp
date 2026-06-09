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
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import PersonOffIcon from '@mui/icons-material/PersonOff';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import {
  collection,
  query,
  orderBy,
  getDocs,
  doc,
  updateDoc,
  deleteDoc,
  deleteField,
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
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);
  const [menuUser, setMenuUser] = useState<AppUser | null>(null);
  const [confirmDialog, setConfirmDialog] = useState<{
    open: boolean;
    title: string;
    description: string;
    action: () => Promise<void>;
    color: 'error' | 'primary' | 'success';
  }>({ open: false, title: '', description: '', action: async () => {}, color: 'primary' });

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
        query(collection(db, 'users'), orderBy('createdAtMillis', 'desc'), limit(500))
      );
      setUsers(snap.docs.map((d) => ({ id: d.id, ...d.data() } as AppUser)));
    } catch (err) {
      console.error('Failed to fetch users:', err);
    } finally {
      setLoading(false);
    }
  }

  function openMenu(event: React.MouseEvent<HTMLElement>, user: AppUser) {
    setMenuAnchor(event.currentTarget);
    setMenuUser(user);
  }

  function closeMenu() {
    setMenuAnchor(null);
    setMenuUser(null);
  }

  async function toggleAdmin(user: AppUser) {
    closeMenu();
    const isCurrentlyAdmin = user.role === 'admin';

    setConfirmDialog({
      open: true,
      title: isCurrentlyAdmin ? 'Odebrać uprawnienia admina?' : 'Nadać uprawnienia admina?',
      description: isCurrentlyAdmin
        ? `Użytkownik ${user.name || user.email} straci dostęp do panelu admina.`
        : `Użytkownik ${user.name || user.email} uzyska pełny dostęp do panelu admina.`,
      color: isCurrentlyAdmin ? 'error' : 'success',
      action: async () => {
        const userRef = doc(db, 'users', user.id);
        if (isCurrentlyAdmin) {
          await updateDoc(userRef, { role: deleteField() });
        } else {
          await updateDoc(userRef, { role: 'admin' });
        }
        await fetchUsers();
      },
    });
  }

  async function deleteUser(user: AppUser) {
    closeMenu();
    setConfirmDialog({
      open: true,
      title: 'Usunąć użytkownika?',
      description: `Czy na pewno chcesz usunąć dokument użytkownika "${user.name || user.email}"? To usunie dane z Firestore, ale NIE usunie konta z Firebase Auth (to trzeba zrobić ręcznie w konsoli).`,
      color: 'error',
      action: async () => {
        await deleteDoc(doc(db, 'users', user.id));
        await fetchUsers();
      },
    });
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
              <TableCell>Akcje</TableCell>
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
                    <Box>
                      <Typography variant="body2" fontWeight={500}>
                        {user.name || '(bez nazwy)'}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                        {user.id.slice(0, 10)}...
                      </Typography>
                    </Box>
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
                <TableCell>
                  <Tooltip title="Akcje">
                    <IconButton size="small" onClick={(e) => openMenu(e, user)}>
                      <MoreVertIcon />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
            {filteredUsers.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  {searchQuery ? 'Brak wyników' : 'Brak użytkowników'}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Context Menu */}
      <Menu anchorEl={menuAnchor} open={!!menuAnchor} onClose={closeMenu}>
        {menuUser && menuUser.role === 'admin' ? (
          <MenuItem onClick={() => menuUser && toggleAdmin(menuUser)}>
            <ListItemIcon>
              <PersonOffIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Odbierz rolę admin</ListItemText>
          </MenuItem>
        ) : (
          <MenuItem onClick={() => menuUser && toggleAdmin(menuUser)}>
            <ListItemIcon>
              <AdminPanelSettingsIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Nadaj rolę admin</ListItemText>
          </MenuItem>
        )}
        <MenuItem onClick={() => menuUser && deleteUser(menuUser)} sx={{ color: 'error.main' }}>
          <ListItemIcon>
            <DeleteForeverIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText>Usuń użytkownika</ListItemText>
        </MenuItem>
      </Menu>

      {/* Confirm Dialog */}
      <Dialog
        open={confirmDialog.open}
        onClose={() => setConfirmDialog((prev) => ({ ...prev, open: false }))}
      >
        <DialogTitle>{confirmDialog.title}</DialogTitle>
        <DialogContent>
          <Typography>{confirmDialog.description}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDialog((prev) => ({ ...prev, open: false }))}>
            Anuluj
          </Button>
          <Button
            variant="contained"
            color={confirmDialog.color}
            onClick={async () => {
              await confirmDialog.action();
              setConfirmDialog((prev) => ({ ...prev, open: false }));
            }}
          >
            Potwierdź
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
