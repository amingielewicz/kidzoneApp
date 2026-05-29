package com.kidzone.presentation.common

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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kidzone.domain.model.User

/**
 * Odznaki użytkownika, wyliczane lokalnie z liczników na [User].
 *
 * Trzymane w warstwie presentation/common (a nie w domain), bo same w sobie
 * nie są częścią modelu domenowego – to tylko prezentacja agregatów,
 * z progami zaszytymi w UI. Progi można później wyciągnąć do configa
 * (np. RemoteConfig), bez zmiany kontraktu domenowego.
 *
 * Lista jest świadomie short, żeby na liście rankingowej i ekranie profilu
 * nie wyglądała jak ściana chipów. Jeśli będziemy dodawać kolejne odznaki
 * (np. "5 lat z aplikacją"), warto wprowadzić sekcjonowanie / kolapsowanie.
 *
 * Jak zdobyć:
 *  - **Odkrywca**: dodaj 5 miejsc.
 *  - **Recenzent**: wystaw 10 opinii.
 *  - **Ekspert rodzinny**: 10+ miejsc oraz 20+ opinii.
 */
enum class UserBadge(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    /** Krótki opis zasady przyznawania – pokazujemy go w info-dialogu. */
    val description: String
) {
    EXPLORER(
        label = "Odkrywca",
        icon = Icons.Filled.EmojiEvents,
        color = Color(0xFF43A047),
        description = "Dodaj co najmniej 5 miejsc"
    ),
    REVIEWER(
        label = "Recenzent",
        icon = Icons.Filled.RateReview,
        color = Color(0xFF1E88E5),
        description = "Wystaw co najmniej 10 opinii"
    ),
    FAMILY_EXPERT(
        label = "Ekspert rodzinny",
        icon = Icons.Filled.Verified,
        color = Color(0xFF8E24AA),
        description = "Dodaj 10+ miejsc oraz 20+ opinii"
    )
}

/**
 * Lista odznak przyznanych [User] na podstawie aktualnych liczników.
 *
 * Wynik jest deterministyczny dla tego samego stanu liczników – kolejność
 * w wyniku odpowiada kolejności w enumie [UserBadge].
 */
fun User.computeBadges(): List<UserBadge> {
    val list = mutableListOf<UserBadge>()
    if (placesAddedCount >= 5) list += UserBadge.EXPLORER
    if (reviewsCount >= 10) list += UserBadge.REVIEWER
    if (placesAddedCount >= 10 && reviewsCount >= 20) list += UserBadge.FAMILY_EXPERT
    return list
}

/**
 * Pozioma rozjeżdżająca się lista chipów z odznakami. Używana w karcie
 * użytkownika w rankingu (gdzie pokazujemy tylko "co user osiągnął" bez
 * tooltipa - zachęta do wbicia tych samych odznak).
 *
 * Chipy są disabled (nieklikalne), bo to czysto informacyjny element.
 * Kolory ikon i tła pochodzą z [UserBadge.color] (półprzezroczyste tło,
 * żeby nie krzyczały na karcie).
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
 * Pełen wiersz odznaki - duża okrągła ikona w kolorowym tle + nazwa + opis.
 * Używana w info-dialogu na ekranie profilu, żeby user widział wszystkie
 * dostępne odznaki i sposób ich zdobycia.
 *
 * @param highlighted true = już zdobyte (pełny kolor); false = jeszcze
 *   nie (lekko wyszarzona ikona, bez "kolorowego" tła).
 */
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
