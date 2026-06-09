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
  FormControl,
  InputLabel,
  Select,
  TableSortLabel,
  Alert,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import PersonOffIcon from '@mui/icons-material/PersonOff';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import BlockIcon from '@mui/icons-material/Block';
import EditIcon from '@mui/icons-material/Edit';
import LockOpenIcon from '@mui/icons-material/LockOpen';
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
import { useAuth } from '../hooks/useAuth';
import { AppUser } from '../types';

function formatDate(millis: number): string {
  return new Date(millis).toLocaleDateString('pl-PL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

function formatBanDate(millis: number): string {
  return new Date(millis).toLocaleDateString('pl-PL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

type SortField = 'name' | 'email' | 'placesAddedCount' | 'reviewsCount' | 'createdAtMillis';
type SortDir = 'asc' | 'desc';

interface BanInfo {
  bannedUntilMillis?: number; // timestamp or -1 for permanent
  banReason?: string;
}

export function UsersPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<AppUser[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<AppUser[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('all');
  const [banFilter, setBanFilter] = useState<string>('all');
  const [sortField, setSortField] = useState<SortField>('createdAtMillis');
  const [sortDir, setSortDir] = useState<SortDir>('desc');
  const [loading, setLoading] = useState(true);
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);
  const [menuUser, setMenuUser] = useState<AppUser | null>(null);

  // Edit dialog
  const [editUser, setEditUser] = useState<AppUser | null>(null);
  const [editName, setEditName] = useState('');
  const [editRole, setEditRole] = useState('');
  const [saving, setSaving] = useState(false);

  // Ban dialog
  const [banDialog, setBanDialog] = useState<{ open: boolean; user: AppUser | null }>({ open: false, user: null });
  const [banDays, setBanDays] = useState<string>('7');
  const [banPermanent, setBanPermanent] = useState(false);
  const [banReason, setBanReason] = useState('');

  // Confirm
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
    let result = [...users];

    // Role filter
    if (roleFilter === 'admin') {
      result = result.filter((u) => u.role === 'admin');
    } else if (roleFilter === 'user') {
      result = result.filter((u) => u.role !== 'admin');
    }

    // Ban filter
    if (banFilter === 'banned') {
      result = result.filter((u) => isUserBanned(u));
    } else if (banFilter === 'active') {
      result = result.filter((u) => !isUserBanned(u));
    }

    // Search
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter(
        (u) =>
          (u.name || '').toLowerCase().includes(q) ||
          (u.email || '').toLowerCase().includes(q) ||
          u.id.toLowerCase().includes(q)
      );
    }

    // Sort
    result.sort((a, b) => {
      let aVal: any = (a as any)[sortField] ?? '';
      let bVal: any = (b as any)[sortField] ?? '';
      if (typeof aVal === 'string') aVal = aVal.toLowerCase();
      if (typeof bVal === 'string') bVal = bVal.toLowerCase();
      if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });

    setFilteredUsers(result);
  }, [searchQuery, users, roleFilter, banFilter, sortField, sortDir]);

  function isUserBanned(user: any): boolean {
    const bannedUntil = user.bannedUntilMillis;
    if (!bannedUntil) return false;
    if (bannedUntil === -1) return true; // permanent
    return bannedUntil > Date.now();
  }

  function getBanLabel(user: any): string {
    const bannedUntil = user.bannedUntilMillis;
    if (!bannedUntil) return '';
    if (bannedUntil === -1) return 'Ban permanentny';
    if (bannedUntil > Date.now()) return `Ban do ${formatBanDate(bannedUntil)}`;
    return '';
  }

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

  function isCurrentUser(user: AppUser): boolean {
    return currentUser?.uid === user.id;
  }

  function openEditDialog(user: AppUser) {
    closeMenu();
    setEditUser(user);
    setEditName(user.name || '');
    setEditRole(user.role || 'user');
  }

  async function saveUserEdit() {
    if (!editUser) return;
    setSaving(true);
    try {
      const updates: any = { name: editName };
      if (!isCurrentUser(editUser)) {
        if (editRole === 'admin') {
          updates.role = 'admin';
        } else {
          updates.role = deleteField();
        }
      }
      await updateDoc(doc(db, 'users', editUser.id), updates);
      setEditUser(null);
      await fetchUsers();
    } catch (err) {
      console.error('Failed to save user:', err);
    } finally {
      setSaving(false);
    }
  }

  function openBanDialog(user: AppUser) {
    closeMenu();
    setBanDialog({ open: true, user });
    setBanDays('7');
    setBanPermanent(false);
    setBanReason('');
  }

  async function applyBan() {
    if (!banDialog.user) return;
    setSaving(true);
    try {
      const bannedUntilMillis = banPermanent ? -1 : Date.now() + parseInt(banDays) * 24 * 60 * 60 * 1000;
      await updateDoc(doc(db, 'users', banDialog.user.id), {
        bannedUntilMillis,
        banReason: banReason || 'Naruszenie regulaminu',
      });
      setBanDialog({ open: false, user: null });
      await fetchUsers();
    } catch (err) {
      console.error('Failed to ban user:', err);
    } finally {
      setSaving(false);
    }
  }

  async function unbanUser(user: AppUser) {
    closeMenu();
    await updateDoc(doc(db, 'users', user.id), {
      bannedUntilMillis: deleteField(),
      banReason: deleteField(),
    });
    await fetchUsers();
  }

  async function deleteUser(user: AppUser) {
    closeMenu();
    setConfirmDialog({
      open: true,
      title: 'Usunąć użytkownika?',
      description: `Usunięcie dokumentu "${user.name || user.email}" z Firestore. Konto Firebase Auth pozostanie (usuń ręcznie w konsoli).`,
      color: 'error',
      action: async () => {
        await deleteDoc(doc(db, 'users', user.id));
        await fetchUsers();
      },
    });
  }

  function handleSort(field: SortField) {
    if (sortField === field) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDir('asc');
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

      <Box display="flex" gap={2} mb={3} flexWrap="wrap">
        <TextField
          placeholder="Szukaj po nazwie, emailu lub ID..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          size="small"
          sx={{ flexGrow: 1, minWidth: 200 }}
        />
        <FormControl size="small" sx={{ minWidth: 120 }}>
          <InputLabel>Rola</InputLabel>
          <Select value={roleFilter} label="Rola" onChange={(e) => setRoleFilter(e.target.value)}>
            <MenuItem value="all">Wszystkie</MenuItem>
            <MenuItem value="admin">Admin</MenuItem>
            <MenuItem value="user">User</MenuItem>
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Status</InputLabel>
          <Select value={banFilter} label="Status" onChange={(e) => setBanFilter(e.target.value)}>
            <MenuItem value="all">Wszystkie</MenuItem>
            <MenuItem value="active">Aktywne</MenuItem>
            <MenuItem value="banned">Zbanowane</MenuItem>
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
                  Użytkownik
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'email'}
                  direction={sortField === 'email' ? sortDir : 'asc'}
                  onClick={() => handleSort('email')}
                >
                  Email
                </TableSortLabel>
              </TableCell>
              <TableCell>Rola</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortField === 'placesAddedCount'}
                  direction={sortField === 'placesAddedCount' ? sortDir : 'asc'}
                  onClick={() => handleSort('placesAddedCount')}
                >
                  Miejsca
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
                  Rejestracja
                </TableSortLabel>
              </TableCell>
              <TableCell>Akcje</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredUsers.map((user) => (
              <TableRow key={user.id} hover sx={isUserBanned(user) ? { bgcolor: '#fff3e0' } : undefined}>
                <TableCell>
                  <Box display="flex" alignItems="center" gap={1}>
                    <Avatar src={(user as any).avatarUrl} sx={{ width: 32, height: 32, fontSize: 14 }}>
                      {user.name?.charAt(0) || '?'}
                    </Avatar>
                    <Box>
                      <Typography variant="body2" fontWeight={500}>
                        {user.name || '(bez nazwy)'}
                        {isCurrentUser(user) && (
                          <Chip label="Ty" size="small" color="info" sx={{ ml: 1 }} />
                        )}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                        {user.id.slice(0, 12)}...
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
                <TableCell>
                  {isUserBanned(user) ? (
                    <Tooltip title={(user as any).banReason || ''}>
                      <Chip label={getBanLabel(user)} color="error" size="small" />
                    </Tooltip>
                  ) : (
                    <Chip label="Aktywny" color="success" size="small" variant="outlined" />
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
                <TableCell colSpan={8} align="center">
                  {searchQuery ? 'Brak wyników' : 'Brak użytkowników'}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Context Menu */}
      <Menu anchorEl={menuAnchor} open={!!menuAnchor} onClose={closeMenu}>
        <MenuItem onClick={() => menuUser && openEditDialog(menuUser)}>
          <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
          <ListItemText>Edytuj</ListItemText>
        </MenuItem>
        {menuUser && !isCurrentUser(menuUser) && (
          [
            !isUserBanned(menuUser) ? (
              <MenuItem key="ban" onClick={() => menuUser && openBanDialog(menuUser)}>
                <ListItemIcon><BlockIcon fontSize="small" color="warning" /></ListItemIcon>
                <ListItemText>Zbanuj</ListItemText>
              </MenuItem>
            ) : (
              <MenuItem key="unban" onClick={() => menuUser && unbanUser(menuUser)}>
                <ListItemIcon><LockOpenIcon fontSize="small" color="success" /></ListItemIcon>
                <ListItemText>Odbanuj</ListItemText>
              </MenuItem>
            ),
            menuUser.role === 'admin' ? (
              <MenuItem key="demote" onClick={() => {
                if (!menuUser) return;
                closeMenu();
                setConfirmDialog({
                  open: true, title: 'Odebrać rolę admina?',
                  description: `${menuUser.name || menuUser.email} straci dostęp do panelu.`,
                  color: 'error',
                  action: async () => {
                    await updateDoc(doc(db, 'users', menuUser.id), { role: deleteField() });
                    await fetchUsers();
                  },
                });
              }}>
                <ListItemIcon><PersonOffIcon fontSize="small" /></ListItemIcon>
                <ListItemText>Odbierz admin</ListItemText>
              </MenuItem>
            ) : (
              <MenuItem key="promote" onClick={() => {
                if (!menuUser) return;
                closeMenu();
                setConfirmDialog({
                  open: true, title: 'Nadać rolę admina?',
                  description: `${menuUser.name || menuUser.email} uzyska pełny dostęp do panelu.`,
                  color: 'success',
                  action: async () => {
                    await updateDoc(doc(db, 'users', menuUser.id), { role: 'admin' });
                    await fetchUsers();
                  },
                });
              }}>
                <ListItemIcon><AdminPanelSettingsIcon fontSize="small" /></ListItemIcon>
                <ListItemText>Nadaj admin</ListItemText>
              </MenuItem>
            ),
            <MenuItem key="delete" onClick={() => menuUser && deleteUser(menuUser)} sx={{ color: 'error.main' }}>
              <ListItemIcon><DeleteForeverIcon fontSize="small" color="error" /></ListItemIcon>
              <ListItemText>Usuń użytkownika</ListItemText>
            </MenuItem>,
          ]
        )}
        {menuUser && isCurrentUser(menuUser) && (
          <MenuItem disabled>
            <ListItemText sx={{ color: 'text.secondary' }}>Nie możesz zmieniać własnej roli/banu</ListItemText>
          </MenuItem>
        )}
      </Menu>

      {/* Edit User Dialog */}
      <Dialog open={!!editUser} onClose={() => setEditUser(null)} maxWidth="xs" fullWidth>
        {editUser && (
          <>
            <DialogTitle>Edytuj użytkownika</DialogTitle>
            <DialogContent>
              <Box display="flex" flexDirection="column" gap={2} mt={1}>
                <TextField
                  label="Imię / Nazwa"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  fullWidth
                  size="small"
                />
                {isCurrentUser(editUser) ? (
                  <Alert severity="info" variant="outlined">
                    Nie możesz zmienić własnej roli.
                  </Alert>
                ) : (
                  <FormControl size="small" fullWidth>
                    <InputLabel>Rola</InputLabel>
                    <Select value={editRole} label="Rola" onChange={(e) => setEditRole(e.target.value)}>
                      <MenuItem value="user">User</MenuItem>
                      <MenuItem value="admin">Admin</MenuItem>
                    </Select>
                  </FormControl>
                )}
              </Box>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setEditUser(null)}>Anuluj</Button>
              <Button variant="contained" onClick={saveUserEdit} disabled={saving}>
                {saving ? <CircularProgress size={20} /> : 'Zapisz'}
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Ban Dialog */}
      <Dialog open={banDialog.open} onClose={() => setBanDialog({ open: false, user: null })} maxWidth="xs" fullWidth>
        {banDialog.user && (
          <>
            <DialogTitle>
              Zbanuj: {banDialog.user.name || banDialog.user.email}
            </DialogTitle>
            <DialogContent>
              <Box display="flex" flexDirection="column" gap={2} mt={1}>
                <Alert severity="warning" variant="outlined">
                  Zbanowany użytkownik nie będzie mógł się zalogować do aplikacji.
                </Alert>
                <FormControl size="small" fullWidth>
                  <InputLabel>Czas trwania</InputLabel>
                  <Select
                    value={banPermanent ? 'permanent' : banDays}
                    label="Czas trwania"
                    onChange={(e) => {
                      if (e.target.value === 'permanent') {
                        setBanPermanent(true);
                      } else {
                        setBanPermanent(false);
                        setBanDays(e.target.value);
                      }
                    }}
                  >
                    <MenuItem value="1">1 dzień</MenuItem>
                    <MenuItem value="3">3 dni</MenuItem>
                    <MenuItem value="7">7 dni</MenuItem>
                    <MenuItem value="14">14 dni</MenuItem>
                    <MenuItem value="30">30 dni</MenuItem>
                    <MenuItem value="90">90 dni</MenuItem>
                    <MenuItem value="365">365 dni</MenuItem>
                    <MenuItem value="permanent">Bezpowrotnie (permanentny)</MenuItem>
                  </Select>
                </FormControl>
                <TextField
                  label="Powód bana"
                  value={banReason}
                  onChange={(e) => setBanReason(e.target.value)}
                  fullWidth
                  size="small"
                  placeholder="Naruszenie regulaminu"
                />
                {!banPermanent && (
                  <Typography variant="body2" color="text.secondary">
                    Ban wygaśnie: {formatBanDate(Date.now() + parseInt(banDays) * 24 * 60 * 60 * 1000)}
                  </Typography>
                )}
                {banPermanent && (
                  <Typography variant="body2" color="error">
                    Ban permanentny — użytkownik nigdy nie będzie mógł się zalogować.
                  </Typography>
                )}
              </Box>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setBanDialog({ open: false, user: null })}>Anuluj</Button>
              <Button variant="contained" color="error" onClick={applyBan} disabled={saving}>
                {saving ? <CircularProgress size={20} /> : 'Zbanuj'}
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Confirm Dialog */}
      <Dialog
        open={confirmDialog.open}
        onClose={() => setConfirmDialog((p) => ({ ...p, open: false }))}
      >
        <DialogTitle>{confirmDialog.title}</DialogTitle>
        <DialogContent>
          <Typography>{confirmDialog.description}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDialog((p) => ({ ...p, open: false }))}>Anuluj</Button>
          <Button
            variant="contained"
            color={confirmDialog.color}
            onClick={async () => {
              await confirmDialog.action();
              setConfirmDialog((p) => ({ ...p, open: false }));
            }}
          >
            Potwierdź
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
