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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kidzone.domain.model.User
import com.kidzone.presentation.common.BadgesList
import com.kidzone.presentation.common.computeBadges
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
 *     ukrywa się, gdy oba pola są puste, żeby nie pokazywać pustego placeholdera.
 *  3. **Statystyki** – liczba dodanych miejsc i opinii, oraz data dołączenia.
 *  4. **Odznaki** – wszystkie progi (zdobyte + niezdobyte) z opisami,
 *     żeby user widział, do czego dąży.
 *  5. **Ustawienia** – polityka prywatności i wylogowanie.
 *
 * Stan ładowania: dopóki [ProfileViewModel.user] = null pokazujemy
 * spinner. Brak zalogowanego usera nie powinien się tu zdarzyć (NavGraph
 * wcześniej przekierował na Login), ale gdyby – dostaniemy spinner i
 * MainScreen zareaguje przez sygnał z signOut → onSignOut.
 */
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val ui by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (user == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            ProfileContent(
                user = user!!,
                onEdit = viewModel::openEditSheet,
                onPrivacyPolicy = viewModel::openPrivacyPolicy,
                onSignOut = { viewModel.signOut(onSignOut) }
            )
        }
    }

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
}

@Composable
private fun ProfileContent(
    user: User,
    onEdit: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ProfileHeaderCard(user = user, onEdit = onEdit) }

        // Sekcja "Dane osobowe" pokazuje się tylko gdy user wypełnił imię
        // lub nazwisko. Pusta sekcja byłaby kosmetycznym szumem.
        if (user.firstName.isNotBlank() || user.lastName.isNotBlank()) {
            item { PersonalInfoCard(user = user) }
        }

        item { StatsCard(user = user) }

        item { BadgesCard(user = user) }

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
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
    }
}

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

// --- Badges --------------------------------------------------------------

@Composable
private fun BadgesCard(user: User) {
    val obtained = user.computeBadges().toSet()
    SectionCard(
        title = "Odznaki",
        leadingIcon = Icons.Filled.EmojiEvents
    ) {
        if (obtained.isEmpty()) {
            Text(
                text = "Dodaj miejsca i opinie, żeby zdobywać odznaki!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }
        BadgesList(obtained = obtained)
    }
}

// --- Settings ------------------------------------------------------------

@Composable
private fun SettingsCard(
    onPrivacyPolicy: () -> Unit,
    onSignOut: () -> Unit
) {
    SectionCard(title = "Ustawienia") {
        SettingsRow(
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
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// --- Helpers -------------------------------------------------------------

@Composable
private fun SectionCard(
    title: String,
    leadingIcon: ImageVector? = null,
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
                    fontWeight = FontWeight.SemiBold
                )
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
