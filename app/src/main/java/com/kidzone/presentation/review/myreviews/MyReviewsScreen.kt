package com.kidzone.presentation.review.myreviews

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidzone.R
import com.kidzone.presentation.common.CategoryIcon
import com.kidzone.presentation.common.EmptyState as KidZoneEmptyState
import com.kidzone.presentation.common.CategoryBadge
import com.kidzone.presentation.common.KidZoneCard
import com.kidzone.presentation.common.KidZoneSpacing
import com.kidzone.presentation.common.RatingIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lista opinii wystawionych przez aktualnie zalogowanego usera.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MyReviewsScreen(
    onBack: () -> Unit,
    onOpenPlaceDetails: (placeId: String, source: String?) -> Unit,
    viewModel: MyReviewsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_reviews)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = uiState) {
                MyReviewsViewModel.UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is MyReviewsViewModel.UiState.Error -> {
                    Text(
                        text = s.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                is MyReviewsViewModel.UiState.Ready -> {
                    if (s.items.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = KidZoneSpacing.Screen,
                                vertical = KidZoneSpacing.CardCompact,
                            ),
                            verticalArrangement = Arrangement.spacedBy(KidZoneSpacing.Gap),
                        ) {
                            items(items = s.items, key = { it.review.id }) { item ->
                                MyReviewCard(
                                    item = item,
                                    onClick = { onOpenPlaceDetails(item.review.placeId, "my_reviews") },
                                    onDelete = { viewModel.openDeleteDialog(item.review.id) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogState.pendingDeleteReviewId != null) {
        ConfirmDeleteReviewDialog(
            isDeleting = dialogState.isDeleting,
            errorMessage = dialogState.deleteError,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDeleteDialog
        )
    }
}

@Composable
private fun EmptyState() {
    KidZoneEmptyState(
        icon = Icons.Filled.RateReview,
        title = stringResource(R.string.empty_my_reviews_title),
        message = stringResource(R.string.empty_my_reviews_subtitle)
    )
}

@Suppress("LongMethod", "FunctionNaming")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MyReviewCard(
    item: MyReviewItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedVisibilityScope? = null,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    val category = item.placeCategory
        ?: com.kidzone.domain.model.PlaceCategory.OTHER

    KidZoneCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(
                    R.string.map_open_place_details_label,
                ),
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier.padding(KidZoneSpacing.Card),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
            ) {
                CategoryIcon(
                    category = category,
                    animationKey = "my_reviews_place_icon_${item.review.placeId}",
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    size = 28.dp,
                    iconSize = 18.dp,
                )

                Spacer(Modifier.width(KidZoneSpacing.Gap))

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = item.placeName
                            ?: stringResource(R.string.place_unavailable),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = if (item.placeName == null) {
                            FontStyle.Italic
                        } else {
                            FontStyle.Normal
                        },
                        color = if (item.placeName == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                    )

                    Spacer(Modifier.height(KidZoneSpacing.GapTiny))

                    CategoryBadge(category = category)
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(
                                R.string.more_options,
                            ),
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.delete))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(KidZoneSpacing.GapSmall))

            StarRow(rating = item.review.rating)

            if (item.review.comment.isNotBlank()) {
                Spacer(Modifier.height(KidZoneSpacing.GapSmall))

                Text(
                    text = item.review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                )
            }

            Spacer(Modifier.height(KidZoneSpacing.GapSmall))

            Text(
                text = formatReviewDate(item.review),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StarRow(rating: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(rating.coerceIn(0, 5)) {
            RatingIcon(
                size = 18.dp,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun formatReviewDate(review: com.kidzone.domain.model.Review): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return if (review.updatedAtMillis > review.createdAtMillis) {
        stringResource(R.string.edited_date, formatter.format(Date(review.updatedAtMillis)))
    } else {
        stringResource(R.string.added_date, formatter.format(Date(review.createdAtMillis)))
    }
}

@Composable
private fun ConfirmDeleteReviewDialog(
    isDeleting: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(stringResource(R.string.delete_review_title)) },
        text = {
            Column {
                Text(stringResource(R.string.delete_review_confirmation))
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(stringResource(R.string.delete))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
