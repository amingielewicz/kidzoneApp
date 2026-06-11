package com.kidzone.presentation.ranking

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.User
import com.kidzone.presentation.common.BadgesIconRow
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.UserBadge
import com.kidzone.presentation.common.chronologicalOrder
import com.kidzone.presentation.common.shimmerEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun RankingScreen(
    onOpenPlaceDetails: (placeId: String, source: String?) -> Unit,
    onOpenUserProfile: (userId: String) -> Unit,
    initialTab: String = "",
    viewModel: RankingViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val state by viewModel.uiState.collectAsState()

    val networkStatus by com.kidzone.presentation.common.rememberNetworkStatus()
    var previousNetworkStatus by remember { mutableStateOf(networkStatus) }
    LaunchedEffect(networkStatus) {
        if (previousNetworkStatus == com.kidzone.presentation.common.NetworkStatus.UNAVAILABLE
            && networkStatus == com.kidzone.presentation.common.NetworkStatus.AVAILABLE
        ) {
            viewModel.refresh()
        }
        previousNetworkStatus = networkStatus
    }

    var selectedTab by rememberSaveable { 
        mutableIntStateOf(if (initialTab == "users") 1 else 0) 
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

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
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (selectedTab) {
                        0 -> TopPlacesList(
                            places = state.topPlaces,
                            onOpenPlaceDetails = { onOpenPlaceDetails(it, "ranking") },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope
                        )
                        else -> TopUsersList(
                            users = state.topUsers,
                            badgesByUserId = state.userBadges,
                            onOpenUserProfile = onOpenUserProfile,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope
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
    animatedContentScope: AnimatedContentScope? = null
) {
    if (places.isEmpty()) {
        FullScreenCentered {
            Text(
                text = "Żadne miejsce nie ma jeszcze opinii. Wystaw pierwszą!",
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
                onClick = { onOpenPlaceDetails(place.id) },
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TopUsersList(
    users: List<User>,
    badgesByUserId: Map<String, List<UserBadge>>,
    onOpenUserProfile: (userId: String) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    if (users.isEmpty()) {
        FullScreenCentered {
            Text(
                text = "Brak aktywnych użytkowników. Bądź pierwszy - dodaj miejsce lub opinię!",
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
        item {
            Text(
                text = "Przytrzymaj ikonę odznaki, aby zobaczyć jej nazwę. " +
                    "Wszystkie odznaki do zdobycia znajdziesz w Profilu \u2192 Odznaki \u2192 „?”",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(items = users, key = { it.id }) { user ->
            val position = users.indexOf(user) + 1
            val badges = badgesByUserId[user.id].orEmpty()
            TopUserCard(
                position = position,
                user = user,
                badges = badges,
                onClick = { onOpenUserProfile(user.id) },
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope
            )
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TopUserCard(
    position: Int,
    user: User,
    badges: List<UserBadge>,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PositionMedal(position = position)
                Spacer(Modifier.width(12.dp))
                UserAvatar(
                    user = user,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name.ifBlank { "Użytkownik" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
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
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun UserAvatar(
    user: User,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val avatarSize = 44.dp

    val avatarModifier = Modifier
        .size(avatarSize)
        .clip(CircleShape)

    val finalModifier = if (sharedTransitionScope != null && animatedContentScope != null) {
        with(sharedTransitionScope) {
            avatarModifier.sharedElement(
                rememberSharedContentState(key = "user_avatar_${user.id}"),
                animatedVisibilityScope = animatedContentScope
            )
        }
    } else avatarModifier

    if (user.avatarUrl.isNullOrBlank()) {
        Box(
            modifier = finalModifier.background(MaterialTheme.colorScheme.primaryContainer),
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
            modifier = finalModifier,
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
