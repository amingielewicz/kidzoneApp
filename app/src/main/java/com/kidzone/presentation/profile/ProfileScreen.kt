package com.kidzone.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.SignInProvider
import com.kidzone.presentation.common.BadgeRowItem
import com.kidzone.presentation.common.BadgesRow
import com.kidzone.presentation.common.RankBadge
import com.kidzone.presentation.common.UserBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Profil zalogowanego użytkownika.
 *
 * Sekcje (z góry):
 *  1. **Header** – duży avatar + login (nick) + e-mail; przycisk "Edytuj profil"
 *     otwiera [EditProfileSheet].
 *  2. **Dane osobowe** – imię i nazwisko (jeśli wypełnione). Sekcja
 *     ukrywa się, gdy oba pola są puste.
 *  3. **Statystyki** – liczba dodanych miejsc i opinii, oraz data dołączenia.
 *  4. **Moje treści** – linki do "Moje miejsca" i "Moje opinie".
 *  5. **Odznaki** – wszystkie progi (zdobyte + niezdobyte) z opisami.
 *  6. **Konto i bezpieczeństwo** – zmiana hasła, zmiana e-maila, usunięcie
 *     konta. Pokazujemy tylko dla [SignInProvider.EMAIL_PASSWORD].
 *  7. **Ustawienia** – polityka prywatności i wylogowanie.
 *
 * @param onOpenMyPlaces nawigacja do ekranu z listą własnych miejsc
 * @param onOpenMyReviews nawigacja do ekranu z listą własnych opinii
 * @param onSignOut wylogowanie, ale też **usunięcie konta** zachowuje się
 *   tak samo (ostatecznie i tak wraca na ekran logowania – współdzielimy
 *   callback, żeby nie wprowadzać drugiego)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onOpenMyPlaces: () -> Unit,
    onOpenMyReviews: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val ui by viewModel.uiState.collectAsState()

    // Auto-refresh po przywróceniu internetu
    val networkStatus by com.kidzone.presentation.common.rememberNetworkStatus()
    var previousNetworkStatus by remember { mutableStateOf(networkStatus) }
    LaunchedEffect(networkStatus) {
        if (previousNetworkStatus == com.kidzone.presentation.common.NetworkStatus.UNAVAILABLE
            && networkStatus == com.kidzone.presentation.common.NetworkStatus.AVAILABLE
        ) {
            viewModel.refreshProfile()
        }
        previousNetworkStatus = networkStatus
    }

    // Snackbar pokazujemy dla informacji typu "Hasło zmienione" /
    // "Wysłaliśmy link na nowy adres". Po pokazaniu czyścimy stan.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.accountActionInfo) {
        val msg = ui.accountActionInfo ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeAccountActionInfo()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = ui.isRefreshing,
            onRefresh = { viewModel.refreshProfile() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (user == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                ProfileContent(
                    user = user!!,
                    signInProvider = ui.signInProvider,
                    obtainedBadges = ui.obtainedBadges,
                    userRank = ui.userRank,
                    onEdit = viewModel::openEditSheet,
                    onOpenMyPlaces = onOpenMyPlaces,
                    onOpenMyReviews = onOpenMyReviews,
                    onOpenBadgesInfo = viewModel::openBadgesInfo,
                    onChangePassword = viewModel::openChangePassword,
                    onChangeEmail = viewModel::openChangeEmail,
                    onDeleteAccount = viewModel::openDeleteAccount,
                    onPrivacyPolicy = viewModel::openPrivacyPolicy,
                    onSignOut = { viewModel.signOut(onSignOut) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // --- Dialogi i sheety ---

    if (ui.isEditOpen && user != null) {
        EditProfileSheet(
            initialDisplayName = user!!.name,
            initialFirstName = user!!.firstName,
            initialLastName = user!!.lastName,
            currentAvatarUrl = user!!.avatarUrl,
            isSaving = ui.isSaving,
            errorMessage = ui.saveError,
            onDismiss = viewModel::dismissEditSheet,
            onSave = { displayName, firstName, lastName, newAvatarUri ->
                viewModel.saveProfile(displayName, firstName, lastName, newAvatarUri)
            }
        )
    }

    if (ui.isPrivacyPolicyOpen) {
        PrivacyPolicyDialog(onDismiss = viewModel::dismissPrivacyPolicy)
    }

    if (ui.isChangePasswordOpen) {
        ChangePasswordDialog(
            isInProgress = ui.isAccountActionInProgress,
            errorMessage = ui.accountActionError,
            onDismiss = viewModel::dismissChangePassword,
            onConfirm = viewModel::changePassword
        )
    }

    if (ui.isChangeEmailOpen) {
        ChangeEmailDialog(
            currentEmail = user?.email.orEmpty(),
            isInProgress = ui.isAccountActionInProgress,
            errorMessage = ui.accountActionError,
            onDismiss = viewModel::dismissChangeEmail,
            onConfirm = viewModel::changeEmail
        )
    }

    if (ui.isDeleteAccountOpen && user != null) {
        DeleteAccountDialog(
            placesCount = user!!.placesAddedCount,
            reviewsCount = user!!.reviewsCount,
            isInProgress = ui.isAccountActionInProgress,
            errorMessage = ui.accountActionError,
            onDismiss = viewModel::dismissDeleteAccount,
            onConfirm = { password ->
                viewModel.deleteAccount(password, onDeleted = onSignOut)
            }
        )
    }

    // Info-dialog: lista wszystkich odznak + opisy progów. Wywoływany
    // z ikony "?" przy nagłówku sekcji "Odznaki". Bierzemy z VM-owego
    // `obtainedBadges`, żeby highlight w dialogu zgadzał się z chipami
    // na karcie (uwzględnia też ranking-based badges, które mogłyby się
    // nie zgodzić z naiwnym `user.computeBadges()` bez kontekstu).
    if (ui.isBadgesInfoOpen && user != null) {
        BadgesInfoDialog(
            obtained = ui.obtainedBadges.toSet(),
            onDismiss = viewModel::dismissBadgesInfo
        )
    }

    // Dialog gratulacyjny po zdobyciu nowych odznak – zbiorczy, scrollowalny.
    if (ui.newlyEarnedBadges.isNotEmpty()) {
        BadgeEarnedDialog(
            badges = ui.newlyEarnedBadges,
            onDismiss = viewModel::consumeNewlyEarnedBadge
        )
    }
}

@Composable
private fun ProfileContent(
    user: User,
    signInProvider: SignInProvider,
    obtainedBadges: List<UserBadge>,
    userRank: Int?,
    onEdit: () -> Unit,
    onOpenMyPlaces: () -> Unit,
    onOpenMyReviews: () -> Unit,
    onOpenBadgesInfo: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onDeleteAccount: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ProfileHeaderCard(user = user, userRank = userRank, onEdit = onEdit) }

        if (user.firstName.isNotBlank() || user.lastName.isNotBlank()) {
            item { PersonalInfoCard(user = user) }
        }

        item { StatsCard(user = user) }

        item {
            MyContentCard(
                placesCount = user.placesAddedCount,
                reviewsCount = user.reviewsCount,
                onOpenMyPlaces = onOpenMyPlaces,
                onOpenMyReviews = onOpenMyReviews
            )
        }

        item { BadgesCard(obtainedBadges = obtainedBadges, onOpenInfo = onOpenBadgesInfo) }

        // Sekcja "Konto i bezpieczeństwo" tylko dla email/password user.
        // Dla Google sign-in zmiana hasła jest po stronie Google,
        // a usunięcie konta wymaga reauth przez ponowne logowanie Google,
        // czego MVP nie obsługuje – zostawiamy info w polityce prywatności.
        if (signInProvider == SignInProvider.EMAIL_PASSWORD) {
            item {
                AccountSecurityCard(
                    onChangePassword = onChangePassword,
                    onChangeEmail = onChangeEmail,
                    onDeleteAccount = onDeleteAccount
                )
            }
        }

        item {
            SettingsCard(
                onPrivacyPolicy = onPrivacyPolicy,
                onSignOut = onSignOut
            )
        }
    }
}

// --- Header --------------------------------------------------------------

@Composable
private fun ProfileHeaderCard(
    user: User,
    userRank: Int?,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Box zamiast samej Column - potrzebujemy warstwy do nakładki
        // (plakietka TOP w prawym górnym rogu) niezależnej od centralnej
        // kolumny z avatarem / nazwą / akcją "Edytuj profil".
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileAvatar(avatarUrl = user.avatarUrl, size = 96.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = user.name.ifBlank { "Użytkownik" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (user.email.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Edytuj profil")
                }
            }

            // Plakietka rangi w TOP 100 - tylko jeśli user mieści się
            // w pierwszej setce. VM trzyma userRank ograniczony do tej
            // puli (BADGE_RANK_POOL=100 w computeBadgeContext), ale dla
            // bezpieczeństwa dorzucamy tu jeszcze guard.
            //
            // Kolor gwiazdki: gold dla 1..3, silver dla 4..10, zielony
            // (brand-secondary) dla 11..100 - logika w [RankBadge].
            if (userRank != null && userRank in 1..USER_RANK_BADGE_LIMIT) {
                RankBadge(
                    rank = userRank,
                    label = "TOP 100",
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

/** Górny próg rangi, dla której pokazujemy plakietkę "TOP" na profilu. */
private const val USER_RANK_BADGE_LIMIT = 100

@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    size: androidx.compose.ui.unit.Dp
) {
    if (avatarUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size / 2)
            )
        }
    } else {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
    }
}

// --- Personal info -------------------------------------------------------

@Composable
private fun PersonalInfoCard(user: User) {
    SectionCard(title = "Dane osobowe") {
        if (user.firstName.isNotBlank()) {
            InfoRow(label = "Imię", value = user.firstName)
        }
        if (user.lastName.isNotBlank()) {
            if (user.firstName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
            }
            InfoRow(label = "Nazwisko", value = user.lastName)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- Stats ---------------------------------------------------------------

@Composable
private fun StatsCard(user: User) {
    SectionCard(title = "Statystyki") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(
                icon = Icons.Filled.Place,
                value = user.placesAddedCount.toString(),
                label = "Miejsc"
            )
            StatItem(
                icon = Icons.Filled.RateReview,
                value = user.reviewsCount.toString(),
                label = "Opinii"
            )
        }
        if (user.createdAtMillis > 0L) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "W kidZone od ${formatDate(user.createdAtMillis)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- Moje treści ---------------------------------------------------------

@Composable
private fun MyContentCard(
    placesCount: Int,
    reviewsCount: Int,
    onOpenMyPlaces: () -> Unit,
    onOpenMyReviews: () -> Unit
) {
    SectionCard(title = "Moje treści") {
        NavRow(
            icon = Icons.Filled.Place,
            label = "Moje miejsca",
            trailingText = placesCount.toString(),
            onClick = onOpenMyPlaces
        )
        Spacer(Modifier.height(4.dp))
        NavRow(
            icon = Icons.Filled.RateReview,
            label = "Moje opinie",
            trailingText = reviewsCount.toString(),
            onClick = onOpenMyReviews
        )
    }
}

// --- Badges --------------------------------------------------------------

/**
 * Sekcja "Odznaki" na profilu.
 *
 * Pokazujemy tylko **zdobyte** odznaki (jako [BadgesRow] z półprzezroczystym
 * tłem chipów w kolorze odznaki). Po prawej stronie nagłówka ikona "?"
 * otwiera [BadgesInfoDialog] z pełną listą dostępnych odznak i opisem,
 * jak je zdobyć - dzięki temu user widzi swoje progresy bez ściany
 * "wyszarzonych" niezdobytych odznak na ekranie.
 *
 * Empty state: tekst "Nie masz jeszcze żadnych odznak. Sprawdź jak je zdobyć!"
 * - sam tooltip `?` służy jako CTA, więc nie potrzebujemy osobnego buttona.
 *
 * @param obtainedBadges już zdobyte odznaki, wyliczone w VM z uwzględnieniem
 *   kontekstu rankingowego (zob. ProfileViewModel.computeBadgeContext).
 */
@Composable
private fun BadgesCard(
    obtainedBadges: List<UserBadge>,
    onOpenInfo: () -> Unit
) {
    SectionCard(
        title = "Odznaki",
        leadingIcon = Icons.Filled.EmojiEvents,
        trailing = {
            IconButton(
                onClick = onOpenInfo,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = "Jak zdobyć odznaki?",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) {
        if (obtainedBadges.isEmpty()) {
            Text(
                text = "Nie masz jeszcze żadnych odznak. Kliknij \"?\" obok, " +
                    "żeby sprawdzić, jak je zdobyć.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            BadgesRow(badges = obtainedBadges)
        }
    }
}

/**
 * Dialog z listą wszystkich odznak (zdobyte + niezdobyte) + opisem
 * progów. Używamy ikony +/- w opisach progów - kolorowo dla zdobytych
 * (pełen kolor odznaki), wyszarzone dla pozostałych.
 *
 * Lista jest scrollowalna pionowo - przy 15+ odznakach nie zmieści się
 * cała na ekranie telefonu, a Material 3 AlertDialog domyślnie tnie
 * overflow zamiast scrollować. `verticalScroll` na wewnętrznej Column
 * to najprostszy wzorzec, który nie wymaga LazyColumn (ten ostatni
 * konfliktuje z mierzeniem wysokości w AlertDialog).
 */
@Composable
private fun BadgesInfoDialog(
    obtained: Set<UserBadge>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Jak zdobyć odznaki?") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Aktywność w aplikacji nagradzamy odznakami. " +
                        "Im więcej miejsc i opinii dodasz, tym więcej odznak zdobędziesz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                UserBadge.entries.forEach { badge ->
                    BadgeRowItem(
                        badge = badge,
                        highlighted = badge in obtained
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Rozumiem")
            }
        }
    )
}

/**
 * Dialog gratulacyjny pokazywany, gdy user właśnie zdobył nową odznakę.
 *
 * Wizualnie: duża okrągła ikona w kolorze odznaki + nazwa + krótki opis
 * jakie warunki spełnił. Pokazywany jeden naraz - jeśli user wbił kilka
 * odznak (np. backfill licznika reviews), kolejne czekają w VM-owym
 * buforze i pojawią się po zamknięciu poprzedniego.
 *
 * Wszystkie teksty (title + content) są wyśrodkowane horyzontalnie -
 * standardowy AlertDialog M3 trzyma title po lewej, ale dla okna typu
 * "achievement unlock" symetria czyta się znacznie lepiej. Tylko
 * confirmButton zostaje w naturalnej pozycji (prawy dolny róg dialogu).
 *
 * Świadomie blokujący - user musi kliknąć "Super!" żeby zamknąć, bo to
 * pozytywne wydarzenie powinno się wyróżnić względem zwykłej nawigacji.
 */
@Composable
private fun BadgeEarnedDialog(
    badges: List<UserBadge>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = if (badges.size == 1) "Gratulacje!" else "Gratulacje! (${badges.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (badges.size == 1) "Zdoby\u0142a\u015B/e\u015B now\u0105 odznak\u0119:"
                    else "Zdoby\u0142a\u015B/e\u015B nowe odznaki:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                badges.forEach { badge ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(badge.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = badge.icon,
                                contentDescription = null,
                                tint = badge.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = badge.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = badge.color
                            )
                            Text(
                                text = badge.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Super!")
            }
        }
    )
}

// --- Konto i bezpieczeństwo ---------------------------------------------

@Composable
private fun AccountSecurityCard(
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    SectionCard(title = "Konto i bezpieczeństwo") {
        NavRow(
            icon = Icons.Filled.Lock,
            label = "Zmień hasło",
            onClick = onChangePassword
        )
        Spacer(Modifier.height(4.dp))
        NavRow(
            icon = Icons.Filled.AlternateEmail,
            label = "Zmień adres e-mail",
            onClick = onChangeEmail
        )
        Spacer(Modifier.height(4.dp))
        // "Usuń konto" jest celowo wyróżnione kolorem error – działanie
        // nieodwracalne, użytkownik powinien świadomie się zatrzymać przed
        // kliknięciem. Konsekwencje pokażemy w dialogu potwierdzenia.
        NavRow(
            icon = Icons.Filled.DeleteForever,
            label = "Usuń konto",
            iconTint = MaterialTheme.colorScheme.error,
            labelColor = MaterialTheme.colorScheme.error,
            onClick = onDeleteAccount
        )
    }
}

// --- Settings ------------------------------------------------------------

@Composable
private fun SettingsCard(
    onPrivacyPolicy: () -> Unit,
    onSignOut: () -> Unit
) {
    SectionCard(title = "Ustawienia") {
        NavRow(
            icon = Icons.Filled.PrivacyTip,
            label = "Polityka prywatności",
            onClick = onPrivacyPolicy
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Wyloguj")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Wersja: dev",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Klikalny wiersz "ikona + tekst + chevron" lub "ikona + tekst + cyfra".
 *
 * Używany w sekcjach Moje treści, Konto, Ustawienia, żeby spójnie
 * sygnalizować przejście do innego ekranu / dialogu.
 *
 * @param trailingText opcjonalny tekst po prawej (np. liczba elementów).
 *   Gdy null – pokazujemy chevron `>` jako sygnał "kliknij i zobacz".
 */
@Composable
private fun NavRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailingText: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// --- Helpers -------------------------------------------------------------

@Composable
private fun SectionCard(
    title: String,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                trailing?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale("pl", "PL"))
    return formatter.format(Date(millis))
}
