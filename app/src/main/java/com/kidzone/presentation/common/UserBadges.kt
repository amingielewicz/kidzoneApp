@file:Suppress("MagicNumber")

package com.kidzone.presentation.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.domain.model.User

/**
 * Odznaki użytkownika, wyliczane lokalnie z liczników na [User].
 */
enum class UserBadge(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val color: Color,
    @StringRes val descriptionRes: Int
) {
    // ----- Pierwsze kroki -----
    FIRST_PLACE(
        labelRes = R.string.badge_first_place_label,
        icon = Icons.Filled.AddLocationAlt,
        color = Color(0xFFBF360C),
        descriptionRes = R.string.badge_first_place_desc
    ),
    FIRST_REVIEW(
        labelRes = R.string.badge_first_review_label,
        icon = Icons.Filled.ChatBubble,
        color = Color(0xFFBF360C),
        descriptionRes = R.string.badge_first_review_desc
    ),

    // ----- Drabinka miejsc -----
    EXPLORER(
        labelRes = R.string.badge_explorer_label,
        icon = Icons.Filled.Explore,
        color = Color(0xFF2E7D32),
        descriptionRes = R.string.badge_explorer_desc
    ),
    CARTOGRAPHER(
        labelRes = R.string.badge_cartographer_label,
        icon = Icons.Filled.Map,
        color = Color(0xFF388E3C),
        descriptionRes = R.string.badge_cartographer_desc
    ),
    PATHFINDER(
        labelRes = R.string.badge_pathfinder_label,
        icon = Icons.Filled.Terrain,
        color = Color(0xFF2E7D32),
        descriptionRes = R.string.badge_pathfinder_desc
    ),

    // ----- Drabinka opinii -----
    REVIEWER(
        labelRes = R.string.badge_reviewer_label,
        icon = Icons.Filled.RateReview,
        color = Color(0xFF1565C0),
        descriptionRes = R.string.badge_reviewer_desc
    ),
    CRITIC(
        labelRes = R.string.badge_critic_label,
        icon = Icons.Filled.Reviews,
        color = Color(0xFF1565C0),
        descriptionRes = R.string.badge_critic_desc
    ),
    SENIOR_REVIEWER(
        labelRes = R.string.badge_senior_reviewer_label,
        icon = Icons.Filled.Stars,
        color = Color(0xFF0D47A1),
        descriptionRes = R.string.badge_senior_reviewer_desc
    ),

    // ----- Wszechstronność -----
    COMMUNITY_PILLAR(
        labelRes = R.string.badge_community_pillar_label,
        icon = Icons.Filled.Groups,
        color = Color(0xFF6D4C41),
        descriptionRes = R.string.badge_community_pillar_desc
    ),
    FAMILY_EXPERT(
        labelRes = R.string.badge_family_expert_label,
        icon = Icons.Filled.Verified,
        color = Color(0xFF8E24AA),
        descriptionRes = R.string.badge_family_expert_desc
    ),

    // ----- Ranking użytkowników -----
    LEADER_BRONZE(
        labelRes = R.string.badge_leader_bronze_label,
        icon = Icons.Filled.MilitaryTech,
        color = Color(0xFF8D5524),
        descriptionRes = R.string.badge_leader_bronze_desc
    ),
    LEADER_SILVER(
        labelRes = R.string.badge_leader_silver_label,
        icon = Icons.Filled.MilitaryTech,
        color = Color(0xFF616161),
        descriptionRes = R.string.badge_leader_silver_desc
    ),
    LEADER_GOLD(
        labelRes = R.string.badge_leader_gold_label,
        icon = Icons.Filled.EmojiEvents,
        color = Color(0xFF8A5A00),
        descriptionRes = R.string.badge_leader_gold_desc
    ),

    // ----- Ranking miejsc -----
    PLACE_TOP3(
        labelRes = R.string.badge_place_top3_label,
        icon = Icons.Filled.Whatshot,
        color = Color(0xFF8D5524),
        descriptionRes = R.string.badge_place_top3_desc
    ),
    PLACE_TOP1(
        labelRes = R.string.badge_place_top1_label,
        icon = Icons.Filled.WorkspacePremium,
        color = Color(0xFF8A5A00),
        descriptionRes = R.string.badge_place_top1_desc
    );

    val label: String
        @Composable
        get() = stringResource(labelRes)

    val description: String
        @Composable
        get() = stringResource(descriptionRes)
}

/**
 * Kontekst rankingowy potrzebny do wyliczenia odznak.
 */
data class BadgeContext(
    val userRank: Int? = null,
    val bestPlaceRank: Int? = null
)

/**
 * Lista odznak przyznanych [User].
 */
fun User.computeBadges(context: BadgeContext = BadgeContext()): List<UserBadge> {
    val list = mutableListOf<UserBadge>()

    if (placesAddedCount >= 1) list += UserBadge.FIRST_PLACE
    if (reviewsCount >= 1) list += UserBadge.FIRST_REVIEW

    if (placesAddedCount >= 5) list += UserBadge.EXPLORER
    if (placesAddedCount >= 15) list += UserBadge.CARTOGRAPHER
    if (placesAddedCount >= 30) list += UserBadge.PATHFINDER

    if (reviewsCount >= 10) list += UserBadge.REVIEWER
    if (reviewsCount >= 25) list += UserBadge.CRITIC
    if (reviewsCount >= 50) list += UserBadge.SENIOR_REVIEWER

    if (placesAddedCount >= 5 && reviewsCount >= 5) list += UserBadge.COMMUNITY_PILLAR
    if (placesAddedCount >= 10 && reviewsCount >= 20) list += UserBadge.FAMILY_EXPERT

    when (context.userRank) {
        1 -> list += UserBadge.LEADER_GOLD
        2 -> list += UserBadge.LEADER_SILVER
        3 -> list += UserBadge.LEADER_BRONZE
        else -> { }
    }

    val placeRank = context.bestPlaceRank
    if (placeRank != null) {
        if (placeRank in 1..3) list += UserBadge.PLACE_TOP3
        if (placeRank == 1) list += UserBadge.PLACE_TOP1
    }

    return list
}

/**
 * Pozioma rozjeżdżająca się lista chipów z odznakami.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgesRow(
    badges: List<UserBadge>,
    modifier: Modifier = Modifier
) {
    if (badges.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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

/**
 * Kompaktowy rząd ikon-only odznak.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgesIconRow(
    badges: List<UserBadge>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    if (badges.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        badges.forEach { badge ->
            BadgeIconTile(badge = badge, iconSize = iconSize)
        }
    }
}

/**
 * Pojedyncza okrągła ikona odznaki + Material 3 [TooltipBox].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeIconTile(badge: UserBadge, iconSize: Dp) {
    val tooltipState = rememberTooltipState()
    val badgeLabel = badge.label
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(text = badgeLabel)
            }
        },
        state = tooltipState
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(badge.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badge.icon,
                contentDescription = badgeLabel,
                tint = badge.color,
                modifier = Modifier.size(iconSize * 0.6f)
            )
        }
    }
}

/**
 * Sortuje listę odznak chronologicznie.
 */
fun chronologicalOrder(
    badges: List<UserBadge>,
    badgeEarnedAt: Map<String, Long>
): List<UserBadge> = badges.sortedWith(
    compareBy<UserBadge> { badgeEarnedAt[it.name] ?: Long.MAX_VALUE }
        .thenBy { it.ordinal }
)

/**
 * Pełen wiersz odznaki.
 */
@Suppress("FunctionNaming")
@Composable
fun BadgeRowItem(
    badge: UserBadge,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (highlighted) badge.color.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badge.icon,
                contentDescription = null,
                tint = if (highlighted) {
                    badge.color
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = badge.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
