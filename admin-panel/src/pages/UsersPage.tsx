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
  Snackbar,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import PersonOffIcon from '@mui/icons-material/PersonOff';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import BlockIcon from '@mui/icons-material/Block';
import EditIcon from '@mui/icons-material/Edit';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import CancelIcon from '@mui/icons-material/Cancel';
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
import { db, auth } from '../services/firebase';
import { adminFetch } from '../services/api';
import { callFunction } from '../services/cloudFunctions';
import { sendPasswordResetEmail } from 'firebase/auth';
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
  const [editEmail, setEditEmail] = useState('');
  const [editRole, setEditRole] = useState('');
  const [saving, setSaving] = useState(false);

  // Ban dialog
  const [banDialogOpen, setBanDialogOpen] = useState(false);
  const [banTargetUser, setBanTargetUser] = useState<AppUser | null>(null);
  const [banDays, setBanDays] = useState<string>('7');
  const [banPermanent, setBanPermanent] = useState(false);
  const [banReason, setBanReason] = useState('');

  // Confirm dialog
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmTitle, setConfirmTitle] = useState('');
  const [confirmDesc, setConfirmDesc] = useState('');
  const [confirmColor, setConfirmColor] = useState<'error' | 'primary' | 'success'>('primary');
  const confirmActionRef = useRef<(() => Promise<void>) | null>(null);

  // Snackbar for password reset
  const [snackbarMsg, setSnackbarMsg] = useState('');

  useEffect(() => {
    fetchUsers();
  }, []);

  useEffect(() => {
    let result = [...users];
    if (roleFilter === 'admin') result = result.filter((u) => u.role === 'admin');
    else if (roleFilter === 'user') result = result.filter((u) => u.role !== 'admin');
    if (banFilter === 'banned') result = result.filter((u) => isUserBanned(u));
    else if (banFilter === 'active') result = result.filter((u) => !isUserBanned(u));
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter(
        (u) =>
          (u.name || '').toLowerCase().includes(q) ||
          (u.email || '').toLowerCase().includes(q) ||
          u.id.toLowerCase().includes(q),
      );
    }
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
    const b = user.bannedUntilMillis;
    if (!b) return false;
    if (b === -1) return true;
    return b > Date.now();
  }

  function getBanLabel(user: any): string {
    const b = user.bannedUntilMillis;
    if (!b) return '';
    if (b === -1) return 'Zablokowany bezpowrotnie';
    if (b > Date.now()) return `Zablokowany do ${formatBanDate(b)}`;
    return '';
  }

  async function fetchUsers() {
    setLoading(true);
    try {
      const snap = await getDocs(
        query(collection(db, 'users'), orderBy('createdAtMillis', 'desc'), limit(500)),
      );
      setUsers(snap.docs.map((d) => ({ id: d.id, ...d.data() }) as AppUser));
    } catch (err) {
      console.error(err);
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

  function openConfirm(
    title: string,
    desc: string,
    color: 'error' | 'primary' | 'success',
    action: () => Promise<void>,
  ) {
    confirmActionRef.current = action;
    setConfirmTitle(title);
    setConfirmDesc(desc);
    setConfirmColor(color);
    setConfirmOpen(true);
  }

  async function handleConfirm() {
    if (confirmActionRef.current) {
      await confirmActionRef.current();
    }
    setConfirmOpen(false);
    confirmActionRef.current = null;
  }

  function openEditDialog(user: AppUser) {
    closeMenu();
    setEditUser(user);
    setEditName(user.name || '');
    setEditEmail(user.email || '');
    setEditRole(user.role || 'user');
  }

  async function saveUserEdit() {
    if (!editUser) return;
    setSaving(true);
    try {
      // Aktualizacja nazwy i roli w Firestore
      const updates: any = { name: editName };
      if (!isCurrentUser(editUser)) {
        if (editRole === 'admin') updates.role = 'admin';
        else updates.role = deleteField();
      }
      await updateDoc(doc(db, 'users', editUser.id), updates);

      // Aktualizacja emaila przez Cloud Function (sync Auth + Firestore)
      if (editEmail !== editUser.email && editEmail.trim()) {
        await callFunction('adminUpdateUserEmail', {
          userId: editUser.id,
          email: editEmail.trim(),
        });
      }

      setEditUser(null);
      await fetchUsers();
    } catch (err) {
      console.error(err);
    } finally {
      setSaving(false);
    }
  }

  function openBanDialog(user: AppUser) {
    closeMenu();
    setBanTargetUser(user);
    setBanDays('7');
    setBanPermanent(false);
    setBanReason('');
    setBanDialogOpen(true);
  }

  async function applyBan() {
    if (!banTargetUser) return;
    setSaving(true);
    try {
      const bannedUntilMillis = banPermanent
        ? -1
        : Date.now() + parseInt(banDays) * 24 * 60 * 60 * 1000;
      await updateDoc(doc(db, 'users', banTargetUser.id), {
        bannedUntilMillis,
        banReason: banReason || 'Naruszenie regulaminu',
      });
      setBanDialogOpen(false);
      setBanTargetUser(null);
      await fetchUsers();
    } catch (err) {
      console.error(err);
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

  // Delete user with reason
  const [deleteUserDialogOpen, setDeleteUserDialogOpen] = useState(false);
  const [deleteUserReason, setDeleteUserReason] = useState('');
  const [deleteUserTarget, setDeleteUserTarget] = useState<AppUser | null>(null);

  function openDeleteUserDialog(user: AppUser) {
    setDeleteUserTarget(user);
    setDeleteUserReason('');
    setDeleteUserDialogOpen(true);
  }

  async function handleDeleteUser() {
    if (!deleteUserTarget || !deleteUserReason.trim()) return;
    try {
      await callFunction('adminDeleteUser', {
        userId: deleteUserTarget.id,
        reason: deleteUserReason,
      });
    } catch (e) {
      console.error('Failed to delete user:', e);
    }
    setDeleteUserDialogOpen(false);
    await fetchUsers();
  }

  async function resetPassword(user: AppUser) {
    closeMenu();
    if (!user.email) return;
    try {
      await sendPasswordResetEmail(auth, user.email);
      setSnackbarMsg(`Email z resetem hasła wysłany do ${user.email}`);
    } catch (err) {
      console.error('Failed to send password reset:', err);
      setSnackbarMsg('Błąd wysyłania emaila z resetem hasła');
    }
  }

  function handleSort(field: SortField) {
    if (sortField === field) setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    else {
      setSortField(field);
      setSortDir('asc');
    }
  }

  if (loading)
    return (
      <Box display="flex" justifyContent="center" py={6}>
        <CircularProgress />
      </Box>
    );

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} mb={1}>
        Użytkownicy ({users.length})
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Zarządzaj kontami użytkowników. Edytuj dane, nadawaj role, blokuj konta.
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
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Status</InputLabel>
          <Select value={banFilter} label="Status" onChange={(e) => setBanFilter(e.target.value)}>
            <MenuItem value="all">Wszystkie</MenuItem>
            <MenuItem value="active">Aktywni</MenuItem>
            <MenuItem value="banned">Zablokowani</MenuItem>
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
              <TableCell sx={{ width: 100 }}>Rola</TableCell>
              <TableCell sx={{ width: 150 }}>Status</TableCell>
              <TableCell sx={{ width: 80 }}>
                <TableSortLabel
                  active={sortField === 'placesAddedCount'}
                  direction={sortField === 'placesAddedCount' ? sortDir : 'asc'}
                  onClick={() => handleSort('placesAddedCount')}
                >
                  Miejsca
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
                  Rejestracja
                </TableSortLabel>
              </TableCell>
              <TableCell sx={{ width: 80 }}>Akcje</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredUsers.map((user) => (
              <TableRow
                key={user.id}
                hover
                sx={isUserBanned(user) ? { bgcolor: '#fff3e0' } : undefined}
              >
                <TableCell>
                  <Box display="flex" alignItems="center" gap={1}>
                    <Avatar
                      src={(user as any).avatarUrl}
                      sx={{ width: 32, height: 32, fontSize: 14 }}
                    >
                      {user.name?.charAt(0) || '?'}
                    </Avatar>
                    <Box>
                      <Typography variant="body2" component="span" fontWeight={500}>
                        {user.name || '(bez nazwy)'}
                      </Typography>
                      {isCurrentUser(user) && (
                        <Chip label="Ty" size="small" color="info" sx={{ ml: 1 }} />
                      )}
                      <Box display="flex" alignItems="center" gap={0.5}>
                        <Tooltip title={user.id}>
                          <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{ fontFamily: 'monospace', fontSize: 11 }}
                          >
                            {user.id.slice(0, 12)}...
                          </Typography>
                        </Tooltip>
                        <Tooltip title="Kopiuj UID">
                          <IconButton
                            size="small"
                            onClick={() => navigator.clipboard.writeText(user.id)}
                          >
                            <ContentCopyIcon sx={{ fontSize: 14 }} />
                          </IconButton>
                        </Tooltip>
                      </Box>
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
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Edytuj</ListItemText>
        </MenuItem>
        {menuUser && !isCurrentUser(menuUser) && menuUser.email && (
          <MenuItem onClick={() => menuUser && resetPassword(menuUser)}>
            <ListItemIcon>
              <VpnKeyIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Resetuj hasło</ListItemText>
          </MenuItem>
        )}
        {menuUser &&
          !isCurrentUser(menuUser) && [
            !isUserBanned(menuUser) ? (
              <MenuItem key="ban" onClick={() => menuUser && openBanDialog(menuUser)}>
                <ListItemIcon>
                  <BlockIcon fontSize="small" color="warning" />
                </ListItemIcon>
                <ListItemText>Zablokuj</ListItemText>
              </MenuItem>
            ) : (
              <MenuItem key="unban" onClick={() => menuUser && unbanUser(menuUser)}>
                <ListItemIcon>
                  <LockOpenIcon fontSize="small" color="success" />
                </ListItemIcon>
                <ListItemText>Odblokuj</ListItemText>
              </MenuItem>
            ),
            menuUser.role === 'admin' ? (
              <MenuItem
                key="demote"
                onClick={() => {
                  if (!menuUser) return;
                  const u = menuUser;
                  closeMenu();
                  openConfirm(
                    'Odebrać rolę admina?',
                    `${u.name || u.email} straci dostęp do panelu.`,
                    'error',
                    async () => {
                      await updateDoc(doc(db, 'users', u.id), { role: deleteField() });
                      await fetchUsers();
                    },
                  );
                }}
              >
                <ListItemIcon>
                  <PersonOffIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Odbierz admin</ListItemText>
              </MenuItem>
            ) : (
              <MenuItem
                key="promote"
                onClick={() => {
                  if (!menuUser) return;
                  const u = menuUser;
                  closeMenu();
                  openConfirm(
                    'Nadać rolę admina?',
                    `${u.name || u.email} uzyska pełny dostęp do panelu.`,
                    'success',
                    async () => {
                      await updateDoc(doc(db, 'users', u.id), { role: 'admin' });
                      await fetchUsers();
                    },
                  );
                }}
              >
                <ListItemIcon>
                  <AdminPanelSettingsIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Nadaj admin</ListItemText>
              </MenuItem>
            ),
            <MenuItem
              key="delete"
              onClick={() => {
                if (!menuUser) return;
                const u = menuUser;
                closeMenu();
                openDeleteUserDialog(u);
              }}
              sx={{ color: 'error.main' }}
            >
              <ListItemIcon>
                <DeleteForeverIcon fontSize="small" color="error" />
              </ListItemIcon>
              <ListItemText>Usuń użytkownika</ListItemText>
            </MenuItem>,
          ]}
        {menuUser && isCurrentUser(menuUser) && (
          <MenuItem disabled>
            <ListItemText sx={{ color: 'text.secondary' }}>
              Nie możesz zmieniać własnej roli/blokady
            </ListItemText>
          </MenuItem>
        )}
      </Menu>

      {/* Edit User Dialog */}
      <Dialog open={!!editUser} onClose={() => setEditUser(null)} maxWidth="xs" fullWidth>
        {editUser && (
          <>
            <DialogTitle
              sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
            >
              <span>Edytuj użytkownika</span>
              <IconButton size="small" onClick={() => setEditUser(null)}>
                <CancelIcon />
              </IconButton>
            </DialogTitle>
            <DialogContent>
              <Box display="flex" flexDirection="column" gap={2} mt={1}>
                <TextField
                  label="Imię / Nazwa"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  fullWidth
                  size="small"
                />
                <TextField
                  label="Email"
                  value={editEmail}
                  onChange={(e) => setEditEmail(e.target.value)}
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
                    <Select
                      value={editRole}
                      label="Rola"
                      onChange={(e) => setEditRole(e.target.value)}
                    >
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
      <Dialog open={banDialogOpen} onClose={() => setBanDialogOpen(false)} maxWidth="xs" fullWidth>
        {banTargetUser && (
          <>
            <DialogTitle
              sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
            >
              <span>Zablokuj: {banTargetUser.name || banTargetUser.email}</span>
              <IconButton size="small" onClick={() => setBanDialogOpen(false)}>
                <CancelIcon />
              </IconButton>
            </DialogTitle>
            <DialogContent>
              <Box display="flex" flexDirection="column" gap={2} mt={1}>
                <Alert severity="warning" variant="outlined">
                  Zablokowany użytkownik nie będzie mógł się zalogować do aplikacji.
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
                  label="Powód blokady"
                  value={banReason}
                  onChange={(e) => setBanReason(e.target.value)}
                  fullWidth
                  size="small"
                  placeholder="Naruszenie regulaminu"
                />
                {!banPermanent && (
                  <Typography variant="body2" color="text.secondary">
                    Blokada wygaśnie:{' '}
                    {formatBanDate(Date.now() + parseInt(banDays) * 24 * 60 * 60 * 1000)}
                  </Typography>
                )}
                {banPermanent && (
                  <Typography variant="body2" color="error">
                    Blokada bezpowrotna — użytkownik nigdy nie będzie mógł się zalogować.
                  </Typography>
                )}
              </Box>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setBanDialogOpen(false)}>Anuluj</Button>
              <Button variant="contained" color="error" onClick={applyBan} disabled={saving}>
                {saving ? <CircularProgress size={20} /> : 'Zablokuj'}
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Delete User Dialog */}
      <Dialog
        open={deleteUserDialogOpen}
        onClose={() => setDeleteUserDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          Usuń użytkownika: {deleteUserTarget?.name || deleteUserTarget?.email}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Podaj powód usunięcia konta. Zostanie wysłany do użytkownika emailem.
          </Typography>
          <TextField
            autoFocus
            fullWidth
            multiline
            rows={3}
            label="Powód usunięcia"
            value={deleteUserReason}
            onChange={(e) => setDeleteUserReason(e.target.value)}
            placeholder="Wpisz powód usunięcia konta..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteUserDialogOpen(false)}>Anuluj</Button>
          <Button
            variant="contained"
            color="error"
            disabled={!deleteUserReason.trim() || saving}
            onClick={async () => {
              setSaving(true);
              await handleDeleteUser();
              setSaving(false);
            }}
          >
            {saving ? <CircularProgress size={20} color="inherit" /> : 'Usuń'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Confirm Dialog */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle>{confirmTitle}</DialogTitle>
        <DialogContent>
          <Typography>{confirmDesc}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Anuluj</Button>
          <Button variant="contained" color={confirmColor} onClick={handleConfirm}>
            Potwierdź
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={!!snackbarMsg}
        autoHideDuration={4000}
        onClose={() => setSnackbarMsg('')}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={() => setSnackbarMsg('')} severity="success" variant="filled">
          {snackbarMsg}
        </Alert>
      </Snackbar>
    </Box>
  );
}
