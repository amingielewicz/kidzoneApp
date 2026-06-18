@file:Suppress("FunctionNaming", "TooManyFunctions")

package com.kidzone.presentation.ranking

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.User
import com.kidzone.presentation.common.BadgesIconRow
import com.kidzone.presentation.common.CategoryBadge
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.EmptyState
import com.kidzone.presentation.common.KidZoneCard
import com.kidzone.presentation.common.KidZoneRadii
import com.kidzone.presentation.common.KidZoneSpacing
import com.kidzone.presentation.common.UserBadge
import com.kidzone.presentation.common.chronologicalOrder
import com.kidzone.presentation.common.shimmerEffect

private const val PODIUM_FIRST_CONTAINER = 0xFFFFF8E1
private const val PODIUM_FIRST_BORDER = 0xFFFFD54F
private const val PODIUM_THIRD_CONTAINER = 0xFFFFF3E0
private const val PODIUM_THIRD_BORDER = 0xFFD7A86E
private const val PODIUM_SECOND_BORDER = 0xFFB0BEC5

/**
 * Ranking miejsc i użytkowników.
 *
 * Dwie zakładki w [PrimaryTabRow]:
 *  - **Miejsca** – top miejsc wg [Place.averageRating] (do 100 pozycji),
 *  - **Użytkownicy** – top najbardziej aktywnych użytkowników (do 100 pozycji).
 *
 * Odświeżanie:
 *  - Automatycznie przy każdym wejściu na zakładkę (ON_RESUME).
 *  - Pull-to-refresh (swipe w dół).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun RankingScreen(
    onOpenPlaceDetails: (placeId: String, source: String?) -> Unit,
    viewModel: RankingViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val state by viewModel.uiState.collectAsState()

    // Zachowujemy stan przewijania osobno dla każdej zakładki
    val placesListState = rememberLazyListState()
    val usersListState = rememberLazyListState()

    // Auto-refresh po przywróceniu internetu
    val networkStatus by com.kidzone.presentation.common.rememberNetworkStatus()
    var previousNetworkStatus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(networkStatus) }
    androidx.compose.runtime.LaunchedEffect(networkStatus) {
        if (previousNetworkStatus == com.kidzone.presentation.common.NetworkStatus.UNAVAILABLE
            && networkStatus == com.kidzone.presentation.common.NetworkStatus.AVAILABLE
        ) {
            viewModel.refresh()
        }
        previousNetworkStatus = networkStatus
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
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

        when {
            state.isLoading && state.topPlaces.isEmpty() && state.topUsers.isEmpty() -> {
                RankingSkeleton()
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

            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.refresh(forceShowLoading = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (selectedTab) {
                        0 -> TopPlacesList(
                            places = state.topPlaces,
                            onOpenPlaceDetails = { onOpenPlaceDetails(it, "ranking") },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            lazyListState = placesListState
                        )
                        else -> TopUsersList(
                            users = state.topUsers,
                            badgesByUserId = state.userBadges,
                            lazyListState = usersListState
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TopPlacesList(
    places: List<Place>,
    onOpenPlaceDetails: (placeId: String) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    lazyListState: LazyListState = rememberLazyListState()
) {
    if (places.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.EmojiEvents,
            title = "Ranking miejsc jest jeszcze pusty",
            message = "Dodaj opinię do miejsca, a najwyżej oceniane lokalizacje pojawią się tutaj."
        )
        return
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = places, key = { "ranking_${it.id}" }) { place ->
            val position = places.indexOf(place) + 1
            TopPlaceCard(
                position = position,
                place = place,
                onClick = { onOpenPlaceDetails(place.id) },
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope
            )
        }
    }
}

@Composable
private fun TopUsersList(
    users: List<User>,
    badgesByUserId: Map<String, List<UserBadge>>,
    lazyListState: LazyListState = rememberLazyListState()
) {
    if (users.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Person,
            title = "Ranking użytkowników jest jeszcze pusty",
            message = "Aktywność pojawi się po dodaniu pierwszych miejsc, opinii lub zdjęć."
        )
        return
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Przytrzymaj ikon\u0119 odznaki, aby zobaczy\u0107 jej nazw\u0119. " +
                    "Wszystkie odznaki do zdobycia znajdziesz w Profilu \u2192 Odznaki \u2192 \u201E?\u201D",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(items = users, key = { it.id }) { user ->
            val position = users.indexOf(user) + 1
            val badges = badgesByUserId[user.id].orEmpty()
            TopUserCard(position = position, user = user, badges = badges)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TopPlaceCard(
    position: Int,
    place: Place,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val categoryLabel = stringResource(place.category.labelRes)
    val addressLabel = place.address.takeIf { it.isNotBlank() }?.let { ", adres: $it" }.orEmpty()
    val highlight = when (position) {
        1 -> PodiumHighlight(
            containerColor = Color(PODIUM_FIRST_CONTAINER),
            borderColor = Color(PODIUM_FIRST_BORDER)
        )
        2 -> PodiumHighlight(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            borderColor = Color(PODIUM_SECOND_BORDER)
        )
        3 -> PodiumHighlight(
            containerColor = Color(PODIUM_THIRD_CONTAINER),
            borderColor = Color(PODIUM_THIRD_BORDER)
        )
        else -> null
    }

    val cardModifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Pozycja $position, ${place.name}, $categoryLabel$addressLabel, " +
                    "ocena %.1f, liczba opinii %d".format(place.averageRating, place.reviewsCount)
            }
            .clickable(
                onClickLabel = "Otwórz szczegóły miejsca",
                role = Role.Button,
                onClick = onClick
            )

    if (highlight == null) {
        KidZoneCard(modifier = cardModifier) {
            TopPlaceCardContent(
                position = position,
                place = place,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope
            )
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(KidZoneRadii.Card),
            colors = CardDefaults.cardColors(containerColor = highlight.containerColor),
            border = BorderStroke(1.dp, highlight.borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            TopPlaceCardContent(
                position = position,
                place = place,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TopPlaceCardContent(
    position: Int,
    place: Place,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PositionMedal(position = position)
            Spacer(Modifier.width(12.dp))

            CategoryIcon(
                category = place.category,
                animationKey = "ranking_place_icon_${place.id}",
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
                size = 28.dp,
                iconSize = 18.dp
            )

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                if (place.address.isNotBlank()) {
                    Spacer(Modifier.height(KidZoneSpacing.GapTiny))
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Spacer(Modifier.height(KidZoneSpacing.GapSmall))
                CategoryBadge(category = place.category)
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

@Composable
private fun TopUserCard(
    position: Int,
    user: User,
    badges: List<UserBadge>
) {
    val userName = user.name.ifBlank { "Użytkownik" }
    val badgesLabel = if (badges.isEmpty()) {
        "brak odznak"
    } else {
        "liczba odznak ${badges.size}"
    }

    val highlight = when (position) {
        1 -> PodiumHighlight(
            containerColor = Color(PODIUM_FIRST_CONTAINER),
            borderColor = Color(PODIUM_FIRST_BORDER)
        )
        2 -> PodiumHighlight(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            borderColor = Color(PODIUM_SECOND_BORDER)
        )
        3 -> PodiumHighlight(
            containerColor = Color(PODIUM_THIRD_CONTAINER),
            borderColor = Color(PODIUM_THIRD_BORDER)
        )
        else -> null
    }

    val cardModifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Pozycja $position, $userName, " +
                    "${user.placesAddedCount} miejsc, ${user.reviewsCount} opinii, $badgesLabel"
            }

    if (highlight == null) {
        KidZoneCard(modifier = cardModifier) {
            TopUserCardContent(position = position, user = user, badges = badges)
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(KidZoneRadii.Card),
            colors = CardDefaults.cardColors(containerColor = highlight.containerColor),
            border = BorderStroke(1.dp, highlight.borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            TopUserCardContent(position = position, user = user, badges = badges)
        }
    }
}

@Composable
private fun TopUserCardContent(
    position: Int,
    user: User,
    badges: List<UserBadge>
) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PositionMedal(position = position)
                Spacer(Modifier.width(12.dp))
                UserAvatar(user = user)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name.ifBlank { "U\u017Cytkownik" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    Text(
                        text = "${user.placesAddedCount} miejsc \u00B7 ${user.reviewsCount} opinii",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                BadgesIconRow(
                    badges = chronologicalOrder(badges, user.badgeEarnedAt)
                )
            }
        }
}

private data class PodiumHighlight(
    val containerColor: Color,
    val borderColor: Color
)

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

@Composable
private fun PositionMedal(position: Int) {
    val (background, contentColor) = when (position) {
        1 -> Color(0xFFFFD54F) to Color(0xFF3E2723)
        2 -> Color(0xFFB0BEC5) to Color(0xFF263238)
        3 -> Color(0xFFD7A86E) to Color(0xFF3E2723)
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

@Composable
private fun RankingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(8) {
            KidZoneCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(20.dp)
                                .clip(MaterialTheme.shapes.small)
                                .shimmerEffect()
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(14.dp)
                                .clip(MaterialTheme.shapes.small)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}

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
