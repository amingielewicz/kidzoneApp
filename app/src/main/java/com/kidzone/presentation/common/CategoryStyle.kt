@file:Suppress("MagicNumber")

package com.kidzone.presentation.common

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kidzone.domain.model.PlaceCategory

/**
 * 🎯 Odpowiedzialności:
 * - Definiowanie spójnej identyfikacji wizualnej dla kategorii miejsc (ikona + kolor).
 * - Udostępnianie ujednoliconych komponentów [CategoryIcon] oraz [CategoryBadge].
 *
 * ✅ Gwarancje:
 * - Spójność wizualna między wszystkimi ekranami aplikacji.
 * - Wsparcie dla animacji Shared Transitions dzięki ujednoliconej strukturze komponentów.
 */
data class CategoryStyle(
    val icon: ImageVector,
    val color: Color
)

/** Mapa kategoria → styl. Używaj jako [PlaceCategory.style]. */
val PlaceCategory.style: CategoryStyle
    get() = when (this) {
        PlaceCategory.PLAYGROUND -> CategoryStyle(
            icon = Icons.Filled.Toys,
            color = Color(0xFF2E7D32)
        )
        PlaceCategory.RESTAURANT -> CategoryStyle(
            icon = Icons.Filled.Restaurant,
            color = Color(0xFFBF360C)
        )
        PlaceCategory.PLAY_ROOM -> CategoryStyle(
            icon = Icons.Filled.SmartToy,
            color = Color(0xFF7B1FA2)
        )
        PlaceCategory.CAFE -> CategoryStyle(
            icon = Icons.Filled.LocalCafe,
            color = Color(0xFF5D4037)
        )
        PlaceCategory.PARK -> CategoryStyle(
            icon = Icons.Filled.Park,
            color = Color(0xFF1B5E20)
        )
        PlaceCategory.ATTRACTION -> CategoryStyle(
            icon = Icons.Filled.Attractions,
            color = Color(0xFFC2185B)
        )
        PlaceCategory.OTHER -> CategoryStyle(
            icon = Icons.Filled.Place,
            color = Color(0xFF616161)
        )
    }

/**
 * Standardowy komponent ikony kategorii używany w całej aplikacji.
 * Identyczna struktura w każdym miejscu gwarantuje 100% płynności animacji Shared Elements.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CategoryIcon(
    category: PlaceCategory,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    animationKey: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val style = category.style
    
    val baseModifier = modifier
        .size(size)
        .clip(RoundedCornerShape(8.dp))
        .background(style.color)

    val finalModifier = if (sharedTransitionScope != null && animatedContentScope != null && animationKey != null) {
        with(sharedTransitionScope) {
            baseModifier.sharedElement(
                rememberSharedContentState(key = animationKey),
                animatedVisibilityScope = animatedContentScope
            )
        }
    } else baseModifier

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun CategoryBadge(
    category: PlaceCategory,
    modifier: Modifier = Modifier
) {
    val style = category.style

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(KidZoneRadii.Badge),
        color = style.color.copy(alpha = 0.14f),
        contentColor = style.color
    ) {
        Text(
            text = stringResource(category.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
