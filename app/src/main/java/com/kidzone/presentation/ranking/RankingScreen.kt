package com.kidzone.presentation.ranking

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.User
import com.kidzone.presentation.common.style

/**
 * Ranking miejsc i użytkowników.
 *
 * Dwie zakładki w [PrimaryTabRow]:
 *  - **Miejsca** – TOP 10 miejsc wg [Place.averageRating],
 *  - **Użytkownicy** – TOP 10 najbardziej aktywnych użytkowników, z odznakami
 *    wyliczanymi klient-side z [User.placesAddedCount] i [User.reviewsCount].
 *
 * Stany ładowanie / błąd / pusta lista trzymamy spójnie z resztą aplikacji
 * (zob. [com.kidzone.presentation.place.list.PlaceListScreen]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit,
    viewModel: RankingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Pasek z zakładkami + przycisk refresh w prawym górnym rogu.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.weight(1f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Miejsca") },
                    icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Użytkownicy") },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) }
                )
            }
            IconButton(
                onClick = viewModel::refresh,
                enabled = !state.isLoading
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Odśwież ranking")
            }
        }

        when {
            state.isLoading && state.topPlaces.isEmpty() && state.topUsers.isEmpty() -> {
                FullScreenCentered { CircularProgressIndicator() }
            }

            state.errorMessage != null && state.topPlaces.isEmpty() && state.topUsers.isEmpty() -> {
                FullScreenCentered {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            else -> when (selectedTab) {
                0 -> TopPlacesList(
                    places = state.topPlaces,
                    onOpenPlaceDetails = onOpenPlaceDetails
                )
                else -> TopUsersList(users = state.topUsers)
            }
        }
    }
}

@Composable
private fun TopPlacesList(
    places: List<Place>,
    onOpenPlaceDetails: (placeId: String) -> Unit
) {
    if (places.isEmpty()) {
        FullScreenCentered {
            Text(
                text = "Brak ocenionych miejsc – dodaj pierwsze i wystaw opinię!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = places, key = { it.id }) { place ->
            val position = places.indexOf(place) + 1
            TopPlaceCard(
                position = position,
                place = place,
                onClick = { onOpenPlaceDetails(place.id) }
            )
        }
    }
}

@Composable
private fun TopUsersList(users: List<User>) {
    if (users.isEmpty()) {
        FullScreenCentered {
            Text(
                text = "Brak użytkowników z dodanymi miejscami.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = users, key = { it.id }) { user ->
            val position = users.indexOf(user) + 1
            TopUserCard(position = position, user = user)
        }
    }
}

@Composable
private fun TopPlaceCard(
    position: Int,
    place: Place,
    onClick: () -> Unit
) {
    val style = place.category.style
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PositionMedal(position = position)
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (place.address.isNotBlank()) {
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "%.1f".format(place.averageRating),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${place.reviewsCount} opinii",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TopUserCard(
    position: Int,
    user: User
) {
    val badges = user.computeBadges()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PositionMedal(position = position)
                Spacer(Modifier.width(12.dp))
                UserAvatar(user = user)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name.ifBlank { "Użytkownik" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "${user.placesAddedCount} miejsc · ${user.reviewsCount} opinii",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                BadgesRow(badges = badges)
            }
        }
    }
}

@Composable
private fun UserAvatar(user: User) {
    val avatarSize = 44.dp
    if (user.avatarUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    } else {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Mała "medal" – kółko z numerem pozycji. Pierwsze trzy miejsca dostają
 * specjalne kolory (złoto/srebro/brąz), reszta neutralny tonalny kolor.
 */
@Composable
private fun PositionMedal(position: Int) {
    val (background, contentColor) = when (position) {
        1 -> Color(0xFFFFD54F) to Color(0xFF3E2723)         // złoto
        2 -> Color(0xFFB0BEC5) to Color(0xFF263238)         // srebro
        3 -> Color(0xFFD7A86E) to Color(0xFF3E2723)         // brąz
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = position.toString(),
            color = contentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- Odznaki ---------------------------------------------------------

/**
 * Odznaki wyliczane lokalnie z liczników na [User].
 *
 * Trzymane w UI (a nie w domain), bo same w sobie nie są częścią modelu –
 * to tylko prezentacja agregatów. Progi można później wyciągnąć do configa.
 */
private enum class UserBadge(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    EXPLORER("Odkrywca", Icons.Filled.EmojiEvents, Color(0xFF43A047)),
    REVIEWER("Recenzent", Icons.Filled.RateReview, Color(0xFF1E88E5)),
    FAMILY_EXPERT("Ekspert rodzinny", Icons.Filled.Verified, Color(0xFF8E24AA))
}

private fun User.computeBadges(): List<UserBadge> {
    val list = mutableListOf<UserBadge>()
    if (placesAddedCount >= 5) list += UserBadge.EXPLORER
    if (reviewsCount >= 10) list += UserBadge.REVIEWER
    if (placesAddedCount >= 10 && reviewsCount >= 20) list += UserBadge.FAMILY_EXPERT
    return list
}

@Composable
private fun BadgesRow(badges: List<UserBadge>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        badges.forEach { badge ->
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        text = badge.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = badge.icon,
                        contentDescription = null,
                        tint = badge.color,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = badge.color.copy(alpha = 0.10f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                    disabledLeadingIconContentColor = badge.color
                )
            )
        }
    }
}

// --- helpers ---------------------------------------------------------

@Composable
private fun FullScreenCentered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
