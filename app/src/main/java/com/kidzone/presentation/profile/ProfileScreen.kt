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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButtonDefaults
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.wrapContentWidth
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kidzone.R
import coil.decode.SvgDecoder
import com.kidzone.presentation.common.KidZoneRadii
import com.kidzone.presentation.common.KidZoneSpacing
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.SignInProvider
import com.kidzone.i18n.AppLanguage
import com.kidzone.presentation.common.BadgeRowItem
import com.kidzone.presentation.common.KidZoneCard
import com.kidzone.presentation.common.ModalDialogShape
import com.kidzone.presentation.common.NetworkStatus
import com.kidzone.presentation.common.OfflineAwareSubmitButton
import com.kidzone.presentation.common.ModalTextButton
import com.kidzone.presentation.common.NotificationPromptReason
import com.kidzone.presentation.common.NotificationSoftPromptDialog
import com.kidzone.presentation.common.RankBadge
import com.kidzone.presentation.common.UserBadge
import com.kidzone.presentation.common.rememberHapticFeedback
import com.kidzone.presentation.common.rememberReducedMotionEnabled
import com.kidzone.presentation.common.shouldShowNotificationPrompt
import com.kidzone.presentation.common.shimmerEffect
import com.kidzone.presentation.common.RequiredFieldLabel
import com.kidzone.presentation.common.rememberNetworkStatus
import com.kidzone.utils.UiText
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val CONTACT_SUBJECT_MAX_LENGTH = 80
private const val CONTACT_MESSAGE_MAX_LENGTH = 1000
private const val CONTACT_SUBJECT_WARNING_LENGTH = 70
private const val CONTACT_MESSAGE_WARNING_LENGTH = 950
private const val CONTACT_SUBJECT_MIN_LENGTH = 3
private const val CONTACT_MESSAGE_MIN_LENGTH = 10
private const val NOTIFICATION_PROMPT_TOP_LIMIT = 10
private const val SUPPI_URL = "https://suppi.pl/kidzone"
private const val SUPPI_WIDGET_URL =
    "https://suppi.pl/api/widget/button.svg?fill=6457FD&textColor=ffffff"

/**
 * Profil zalogowanego użytkownika.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming", "LongMethod", "CyclomaticComplexMethod", "LongParameterList")
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onOpenMyPlaces: () -> Unit,
    onOpenMyReviews: () -> Unit,
    scrollToSection: String = "",
    onLocaleChanged: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsState()
    val ui by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val reducedMotionEnabled = rememberReducedMotionEnabled()
    var notificationPromptReason by remember {
        mutableStateOf<NotificationPromptReason?>(null)
    }
    LaunchedEffect(ui.newlyEarnedBadges) {
        if (ui.newlyEarnedBadges.isNotEmpty()) haptic.reward()
    }

    LaunchedEffect(ui.userRank, ui.newlyEarnedBadges) {
        val reason = notificationPromptReasonFor(
            userRank = ui.userRank,
            hasNewBadge = ui.newlyEarnedBadges.isNotEmpty()
        )
        if (reason != null && shouldShowNotificationPrompt(context, reason)) {
            notificationPromptReason = reason
        }
    }

    val networkStatus by rememberNetworkStatus()
    val isOffline = networkStatus == NetworkStatus.UNAVAILABLE
    var previousNetworkStatus by remember { mutableStateOf(networkStatus) }
    LaunchedEffect(networkStatus) {
        if ((previousNetworkStatus == NetworkStatus.UNAVAILABLE)
            && (networkStatus == NetworkStatus.AVAILABLE)
        ) {
            viewModel.refreshProfile()
        }
        previousNetworkStatus = networkStatus
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.accountActionInfo) {
        val msg = ui.accountActionInfo ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg.asString(context))
        viewModel.consumeAccountActionInfo()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = ui.isRefreshing,
            onRefresh = { viewModel.refreshProfile() },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (user == null) {
                ProfileSkeleton()
            } else {
                ProfileContent(
                    user = user!!,
                    signInProvider = ui.signInProvider,
                    obtainedBadges = ui.obtainedBadges,
                    userRank = ui.userRank,
                    scrollToSection = scrollToSection,
                    onEdit = viewModel::openEditSheet,
                    onOpenMyPlaces = onOpenMyPlaces,
                    onOpenMyReviews = onOpenMyReviews,
                    onOpenBadgesInfo = viewModel::openBadgesInfo,
                    onChangePassword = viewModel::openChangePassword,
                    onChangeEmail = viewModel::openChangeEmail,
                    onDeleteAccount = viewModel::openDeleteAccount,
                    onTermsOfService = viewModel::openTermsOfService,
                    onPrivacyPolicy = viewModel::openPrivacyPolicy,
                    onContact = viewModel::openContact,
                    onNotificationPrefs = viewModel::openNotificationPrefs,
                    selectedLanguage = ui.selectedLanguage,
                    onLanguageSettings = viewModel::openLanguageDialog,
                    onSignOut = { viewModel.signOut(onSignOut) },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                },
        )
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
            },
        )
    }

    if (ui.isTermsOfServiceOpen) {
        TermsOfServiceDialog(onDismiss = viewModel::dismissTermsOfService)
    }

    if (ui.isPrivacyPolicyOpen) {
        PrivacyPolicyDialog(onDismiss = viewModel::dismissPrivacyPolicy)
    }

    if (ui.isContactOpen) {
        ContactSupportDialog(
            isSubmitting = ui.isAccountActionInProgress,
            isOffline = isOffline,
            errorMessage = ui.accountActionError,
            onDismiss = viewModel::dismissContact,
            onSubmit = viewModel::submitContactMessage,
        )
    }

    if (ui.isNotificationPrefsOpen) {
        NotificationPreferencesDialog(
            currentPrefs = ui.notificationPrefs,
            isOffline = isOffline,
            onSave = viewModel::saveNotificationPrefs,
            onDismiss = viewModel::dismissNotificationPrefs,
        )
    }

    if (ui.isLanguageDialogOpen) {
        LanguageSettingsDialog(
            selectedLanguage = ui.selectedLanguage,
            onSelectLanguage = { language ->
                viewModel.saveLanguage(language)
                onLocaleChanged()
            },
            onDismiss = viewModel::dismissLanguageDialog,
        )
    }

    if (ui.isChangePasswordOpen) {
        ChangePasswordDialog(
            isInProgress = ui.isAccountActionInProgress,
            errorMessage = ui.accountActionError,
            onDismiss = viewModel::dismissChangePassword,
            onConfirm = viewModel::changePassword,
        )
    }

    if (ui.isChangeEmailOpen) {
        ChangeEmailDialog(
            currentEmail = user?.email.orEmpty(),
            isInProgress = ui.isAccountActionInProgress,
            errorMessage = ui.accountActionError,
            onDismiss = viewModel::dismissChangeEmail,
            onConfirm = viewModel::changeEmail,
        )
    }

    if (ui.isDeleteAccountOpen && user != null) {
        DeleteAccountDialog(
            placesCount = user!!.placesAddedCount,
            reviewsCount = user!!.reviewsCount,
            isInProgress = ui.isAccountActionInProgress,
            isOffline = isOffline,
            errorMessage = ui.accountActionError,
            isGoogleUser = ui.signInProvider == SignInProvider.GOOGLE,
            onDismiss = viewModel::dismissDeleteAccount,
            onConfirm = { password ->
                viewModel.deleteAccount(password, onDeleted = onSignOut)
            },
            onConfirmGoogle = { idToken ->
                viewModel.deleteAccountGoogle(idToken, onDeleted = onSignOut)
            },
        )
    }

    if (ui.isBadgesInfoOpen && user != null) {
        BadgesInfoDialog(
            obtained = ui.obtainedBadges.toSet(),
            onDismiss = viewModel::dismissBadgesInfo,
        )
    }

    if (ui.newlyEarnedBadges.isNotEmpty()) {
        BadgeEarnedDialog(
            badges = ui.newlyEarnedBadges,
            reducedMotionEnabled = reducedMotionEnabled,
            onDismiss = viewModel::consumeNewlyEarnedBadge,
        )
    }

    if (ui.newlyEarnedBadges.isEmpty()) {
        notificationPromptReason?.let { reason ->
            NotificationSoftPromptDialog(
                reason = reason,
                onDismiss = { notificationPromptReason = null }
            )
        }
    }
}


private fun notificationPromptReasonFor(
    userRank: Int?,
    hasNewBadge: Boolean
): NotificationPromptReason? = when {
    userRank in 1..3 -> NotificationPromptReason.Podium
    userRank in 4..NOTIFICATION_PROMPT_TOP_LIMIT -> NotificationPromptReason.Top10
    hasNewBadge -> NotificationPromptReason.FirstBadge
    else -> null
}

@Composable
@Suppress("FunctionNaming", "LongParameterList")
private fun ProfileContent(
    user: User,
    signInProvider: SignInProvider,
    obtainedBadges: List<UserBadge>,
    userRank: Int?,
    scrollToSection: String = "",
    onEdit: () -> Unit,
    onOpenMyPlaces: () -> Unit,
    onOpenMyReviews: () -> Unit,
    onOpenBadgesInfo: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onDeleteAccount: () -> Unit,
    onTermsOfService: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onContact: () -> Unit,
    onNotificationPrefs: () -> Unit,
    selectedLanguage: AppLanguage,
    onLanguageSettings: () -> Unit,
    onSignOut: () -> Unit,
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(scrollToSection) {
        if (scrollToSection == "badges") {
            val hasPersonalInfo = user.firstName.isNotBlank() || user.lastName.isNotBlank()
            val badgesIndex = if (hasPersonalInfo) 4 else 3
            lazyListState.animateScrollToItem(badgesIndex)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(KidZoneSpacing.GapSmall),
    ) {
        item { ProfileHeaderCard(user = user, userRank = userRank, onEdit = onEdit) }

        if (user.firstName.isNotBlank() || user.lastName.isNotBlank()) {
            item { PersonalInfoCard(user = user) }
        }

        item {
            StatsCard(
                user = user,
                onOpenMyPlaces = onOpenMyPlaces,
                onOpenMyReviews = onOpenMyReviews,
            )
        }

        item {
            MyContentCard(
                placesCount = user.placesAddedCount,
                reviewsCount = user.reviewsCount,
                onOpenMyPlaces = onOpenMyPlaces,
                onOpenMyReviews = onOpenMyReviews,
            )
        }

        item { BadgesCard(obtainedBadges = obtainedBadges, onOpenInfo = onOpenBadgesInfo) }

        item {
            AccountSecurityCard(
                showPasswordAndEmail = signInProvider == SignInProvider.EMAIL_PASSWORD,
                onChangePassword = onChangePassword,
                onChangeEmail = onChangeEmail,
                onDeleteAccount = onDeleteAccount,
            )
        }

        item {
            SettingsCard(
                onTermsOfService = onTermsOfService,
                onPrivacyPolicy = onPrivacyPolicy,
                onContact = onContact,
                onNotificationPrefs = onNotificationPrefs,
                selectedLanguage = selectedLanguage,
                onLanguageSettings = onLanguageSettings,
                onSignOut = onSignOut,
            )
        }
    }
}


@Suppress("LongMethod", "FunctionNaming")
@Composable
private fun ProfileHeaderCard(
    user: User,
    userRank: Int?,
    onEdit: () -> Unit,
) {
    KidZoneCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = KidZoneSpacing.Card,
                    vertical = KidZoneSpacing.CardCompact,
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProfileAvatar(avatarUrl = user.avatarUrl, size = 96.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = user.name.ifBlank { stringResource(R.string.default_user_name) },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (user.email.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.edit_profile))
                }
            }

            if (userRank != null && userRank in 1..USER_RANK_BADGE_LIMIT) {
                RankBadge(
                    rank = userRank,
                    label = stringResource(R.string.top_100_label),
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}


private const val USER_RANK_BADGE_LIMIT = 100

@Suppress("FunctionNaming")
@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    size: androidx.compose.ui.unit.Dp,
) {
    if (avatarUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size / 2),
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
            contentScale = ContentScale.Crop,
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun PersonalInfoCard(user: User) {
    SectionCard(title = stringResource(R.string.personal_info_title)) {
        if (user.firstName.isNotBlank()) {
            InfoRow(label = stringResource(R.string.first_name), value = user.firstName)
        }
        if (user.lastName.isNotBlank()) {
            if (user.firstName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
            }
            InfoRow(label = stringResource(R.string.last_name), value = user.lastName)
        }
    }
}


@Suppress("FunctionNaming")
@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun StatsCard(
    user: User,
    onOpenMyPlaces: () -> Unit,
    onOpenMyReviews: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.stats_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            StatItem(
                icon = Icons.Filled.Place,
                value = user.placesAddedCount.toString(),
                label = stringResource(R.string.stats_places),
                onClick = onOpenMyPlaces,
                modifier = Modifier.weight(1f),
            )
            StatItem(
                icon = Icons.Filled.RateReview,
                value = user.reviewsCount.toString(),
                label = stringResource(R.string.stats_reviews),
                onClick = onOpenMyReviews,
                modifier = Modifier.weight(1f),
            )
        }
        if (user.createdAtMillis > 0L) {
            Spacer(
                modifier = Modifier.height(16.dp),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = 0.6f,
                ),
            )

            Spacer(
                modifier = Modifier.height(10.dp),
            )

            Text(
                text = stringResource(
                    R.string.member_since,
                    formatDate(user.createdAtMillis),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}


@Suppress("FunctionNaming")
@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clickable(
                onClickLabel = label,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(
                horizontal = KidZoneSpacing.GapSmall,
                vertical = KidZoneSpacing.GapTiny,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun MyContentCard(
    placesCount: Int,
    reviewsCount: Int,
    onOpenMyPlaces: () -> Unit,
    onOpenMyReviews: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.my_content_title)) {

        ProfileNavRow(
            icon = Icons.Filled.Place,
            label = stringResource(R.string.my_places),
            trailingText = placesCount.toString(),
            onClick = onOpenMyPlaces,
        )

        ProfileNavRow(
            icon = Icons.Filled.RateReview,
            label = stringResource(R.string.my_reviews),
            trailingText = reviewsCount.toString(),
            onClick = onOpenMyReviews,
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun BadgesCard(
    obtainedBadges: List<UserBadge>,
    onOpenInfo: () -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.badges_title),
        leadingIcon = Icons.Filled.EmojiEvents,
        trailing = {
            IconButton(
                onClick = onOpenInfo,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = stringResource(R.string.how_to_earn_badges),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    ) {
        if (obtainedBadges.isEmpty()) {
            Text(
                text = stringResource(R.string.no_badges_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ProfileBadgesGrid(badges = obtainedBadges)
        }
    }
}


@Suppress("FunctionNaming")
@Composable
private fun ProfileBadgesGrid(
    badges: List<UserBadge>,
) {
    val rows = badges.chunked(2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KidZoneSpacing.GapSmall)
    ) {
        rows.forEach { rowBadges ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KidZoneSpacing.GapTiny)
            ) {
                rowBadges.forEach { badge ->
                    ProfileBadgeItem(
                        badge = badge,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (rowBadges.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


@Suppress("FunctionNaming")
@Composable
private fun ProfileBadgeItem(
    badge: UserBadge,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(badge.color.copy(alpha = 0.10f))
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = badge.icon,
            contentDescription = null,
            tint = badge.color,
            modifier = Modifier.size(16.dp),
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = badge.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun BadgesInfoDialog(
    obtained: Set<UserBadge>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ModalDialogShape,
        icon = {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.badges_info_title),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.badges_info_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                UserBadge.entries.forEach { badge ->
                    BadgeRowItem(
                        badge = badge,
                        highlighted = badge in obtained,
                    )
                }
            }
        },
        confirmButton = {
            ModalTextButton(
                text = stringResource(R.string.i_understand),
                onClick = onDismiss,
            )
        },
    )
}

@Composable
@Suppress("FunctionNaming", "LongMethod")
private fun BadgeEarnedDialog(
    badges: List<UserBadge>,
    reducedMotionEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(24.dp),
                shape = ModalDialogShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    val congratsText = if (badges.size == 1) {
                        stringResource(R.string.congratulations)
                    } else {
                        "${stringResource(R.string.congratulations)} (${badges.size})"
                    }
                    Text(
                        text = congratsText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (badges.size == 1) stringResource(R.string.earned_new_badge)
                            else stringResource(R.string.earned_new_badges),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(16.dp))
                        badges.forEach { badge ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(badge.color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = badge.icon,
                                        contentDescription = null,
                                        tint = badge.color,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = badge.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badge.color,
                                    )
                                    Text(
                                        text = badge.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    ModalTextButton(
                        text = stringResource(R.string.great),
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            if (!reducedMotionEnabled) {
                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = listOf(
                        Party(
                            speed = 10f,
                            maxSpeed = 35f,
                            damping = 0.9f,
                            angle = 330,
                            spread = 60,
                            colors = listOf(
                                0xFFFFC93C.toInt(),
                                0xFF1E88E5.toInt(),
                                0xFF43A047.toInt(),
                                0xFFFFFFFF.toInt(),
                            ),
                            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(40),
                            position = Position.Relative(0.0, 0.4),
                        ),
                        Party(
                            speed = 10f,
                            maxSpeed = 35f,
                            damping = 0.9f,
                            angle = 210,
                            spread = 60,
                            colors = listOf(
                                0xFFFFC93C.toInt(),
                                0xFF1E88E5.toInt(),
                                0xFF43A047.toInt(),
                                0xFFFFFFFF.toInt(),
                            ),
                            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(40),
                            position = Position.Relative(1.0, 0.4),
                        ),
                        Party(
                            speed = 0f,
                            maxSpeed = 20f,
                            damping = 0.9f,
                            angle = 90,
                            spread = 360,
                            colors = listOf(0xFFFFC93C.toInt(), 0xFFFFFFFF.toInt()),
                            emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(25),
                            position = Position.Relative(0.5, -0.1),
                        ),
                    ),
                )
            }
        }
    }
}


@Suppress("FunctionNaming")
@Composable
private fun AccountSecurityCard(
    showPasswordAndEmail: Boolean = true,
    onChangePassword: () -> Unit,
    onChangeEmail: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.account_security_title)) {
        if (showPasswordAndEmail) {
            ProfileNavRow(
                icon = Icons.Filled.Lock,
                label = stringResource(R.string.change_password_title),
                onClick = onChangePassword,
            )

            ProfileNavRow(
                icon = Icons.Filled.AlternateEmail,
                label = stringResource(R.string.change_email),
                onClick = onChangeEmail,
            )
        }
        ProfileNavRow(
            icon = Icons.Filled.DeleteForever,
            label = stringResource(R.string.delete_account),
            iconTint = MaterialTheme.colorScheme.primary,
            labelColor = MaterialTheme.colorScheme.onSurface,
            onClick = onDeleteAccount,
        )
    }
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun SettingsCard(
    onTermsOfService: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onContact: () -> Unit,
    onNotificationPrefs: () -> Unit,
    selectedLanguage: AppLanguage,
    onLanguageSettings: () -> Unit,
    onSignOut: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.settings)) {
        ProfileNavRow(
            icon = Icons.Filled.Language,
            label = stringResource(R.string.language_settings_title),
            trailingText = stringResource(selectedLanguage.labelRes),
            onClick = onLanguageSettings,
        )

        ProfileNavRow(
            icon = Icons.Filled.Notifications,
            label = stringResource(R.string.notification_settings),
            onClick = onNotificationPrefs,
        )

        ProfileNavRow(
            icon = Icons.Filled.Gavel,
            label = stringResource(R.string.terms_of_service),
            onClick = onTermsOfService,
        )

        ProfileNavRow(
            icon = Icons.Filled.PrivacyTip,
            label = stringResource(R.string.privacy_policy),
            onClick = onPrivacyPolicy,
        )

        ProfileNavRow(
            icon = Icons.Filled.Email,
            label = stringResource(R.string.contact_support),
            onClick = onContact,
        )

        Spacer(Modifier.height(KidZoneSpacing.GapSmall))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )

            Spacer(Modifier.width(KidZoneSpacing.GapSmall))

            Text(stringResource(R.string.sign_out))
        }

        Spacer(Modifier.height(KidZoneSpacing.GapSmall))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(16.dp))

        SuppiSupportButton()

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(
                R.string.app_version,
                com.kidzone.BuildConfig.VERSION_NAME,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun SuppiSupportButton() {
    val context = LocalContext.current

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(SUPPI_WIDGET_URL)
            .decoderFactory(SvgDecoder.Factory())
            .crossfade(true)
            .build(),
        contentDescription = stringResource(R.string.suppi_support_description),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .width(150.dp)
            .clip(RoundedCornerShape(KidZoneRadii.Control))
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.suppi_support_description),
            ) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(SUPPI_URL),
                ).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }

                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    // Brak aplikacji obsługującej link.
                }
            },
    )
}

@Composable
@Suppress("FunctionNaming", "LongMethod")
private fun ContactSupportDialog(
    isSubmitting: Boolean,
    isOffline: Boolean,
    errorMessage: UiText?,
    onDismiss: () -> Unit,
    onSubmit: (
        subject: String,
        message: String,
    ) -> Unit,
) {
    val context = LocalContext.current

    var subject by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    val subjectValid =
        subject.trim().length >= CONTACT_SUBJECT_MIN_LENGTH

    val messageValid =
        message.trim().length >= CONTACT_MESSAGE_MIN_LENGTH

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) {
                onDismiss()
            }
        },
        shape = ModalDialogShape,
        icon = {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(
                    R.string.contact_support_title,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        R.string.contact_support_body,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(
                    modifier = Modifier.height(12.dp),
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it.take(
                            CONTACT_SUBJECT_MAX_LENGTH,
                        )
                    },
                    enabled = !isSubmitting,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        RequiredFieldLabel(
                            label = stringResource(
                                R.string.contact_support_subject_label,
                            ),
                        )
                    },
                    isError =
                        subject.isNotBlank() &&
                                !subjectValid,
                    supportingText = {
                        Text(
                            text = stringResource(
                                R.string.contact_support_subject_counter,
                                subject.length,
                                CONTACT_SUBJECT_MAX_LENGTH,
                            ),
                            color = if (subject.length >= CONTACT_SUBJECT_WARNING_LENGTH) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                )

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = {
                        message = it.take(
                            CONTACT_MESSAGE_MAX_LENGTH,
                        )
                    },
                    enabled = !isSubmitting,
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        RequiredFieldLabel(
                            label = stringResource(
                                R.string.contact_support_message_label,
                            ),
                        )
                    },
                    isError =
                        message.isNotBlank() &&
                                !messageValid,
                    supportingText = {
                        Text(
                            text = stringResource(
                                R.string.contact_support_message_counter,
                                message.length,
                                CONTACT_MESSAGE_MAX_LENGTH,
                            ),
                            color = if (message.length >= CONTACT_MESSAGE_WARNING_LENGTH) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                )

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                Text(
                    text = stringResource(
                        R.string.contact_support_hint,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                errorMessage?.let { error ->
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )

                    Text(
                        text = error.asString(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            OfflineAwareSubmitButton(
                label = stringResource(
                    R.string.contact_support_send,
                ),
                onClick = {
                    onSubmit(
                        subject.trim(),
                        message.trim(),
                    )
                },
                isOffline = isOffline,
                enabled = subjectValid && messageValid,
                isLoading = isSubmitting,
                offlineLabel = stringResource(
                    R.string.contact_support_offline_send_action,
                ),
            )
        },
        dismissButton = {
            ModalTextButton(
                text = stringResource(
                    R.string.cancel,
                ),
                onClick = onDismiss,
                enabled = !isSubmitting,
            )
        },
    )
}

@Suppress(
    "FunctionNaming",
    "LongMethod",
    "MagicNumber",
)
@Composable
private fun LanguageSettingsDialog(
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    @Suppress("MagicNumber")
    val selectedColor = Color(0xFF2E7D32)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ModalDialogShape,
        title = {
            Text(
                text = stringResource(R.string.language_settings_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppLanguage.entries.forEach { language ->
                    val isSelected = language == selectedLanguage

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(
                                role = Role.RadioButton,
                                onClick = {
                                    onSelectLanguage(language)
                                },
                            )
                            .padding(
                                horizontal = 4.dp,
                                vertical = 4.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = languageFlag(language),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(12.dp),
                        )

                        Text(
                            text = stringResource(language.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )

                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = selectedColor,
                                unselectedColor =
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            ModalTextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
            )
        },
    )
}

private fun languageFlag(
    language: AppLanguage,
): String = when (language) {
    AppLanguage.SYSTEM -> "🌐"
    AppLanguage.POLISH -> "🇵🇱"
    AppLanguage.ENGLISH -> "🇬🇧"
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
internal fun ProfileNavRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailingText: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(
                onClickLabel = label,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {}
            .padding(
                horizontal = KidZoneSpacing.GapTiny,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )

        Spacer(
            modifier = Modifier.width(12.dp),
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )

        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(
                modifier = Modifier.width(4.dp),
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun SectionCard(
    title: String,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    KidZoneCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(
                horizontal = KidZoneSpacing.Card,
                vertical = KidZoneSpacing.CardCompact,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )

                    Spacer(Modifier.width(KidZoneSpacing.GapSmall))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )

                trailing?.invoke()
            }

            Spacer(Modifier.height(KidZoneSpacing.GapSmall))

            content()
        }
    }
}


private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

@Suppress("FunctionNaming", "LongMethod")
@Composable
private fun ProfileSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .shimmerEffect(),
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(24.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect(),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(16.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect(),
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .shimmerEffect(),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                repeat(2) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .shimmerEffect(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(20.dp)
                                .clip(MaterialTheme.shapes.small)
                                .shimmerEffect(),
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(20.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect(),
                )
                Spacer(Modifier.height(16.dp))
                repeat(2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .shimmerEffect(),
                        )
                        Spacer(Modifier.width(KidZoneSpacing.GapSmall))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(MaterialTheme.shapes.small)
                                .shimmerEffect(),
                        )
                    }
                }
            }
        }
    }
}
