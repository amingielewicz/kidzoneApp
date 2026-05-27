package com.playground.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Toys
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.playground.domain.model.PlaceCategory

/**
 * Wizualna reprezentacja kategorii miejsca – ikona + kolor.
 *
 * Używana przez:
 *  - [com.playground.presentation.place.add.AddPlaceScreen] (dropdown kategorii)
 *  - listę miejsc (po wdrożeniu) – jako mała ikonka obok nazwy karty
 *  - mapę (po wdrożeniu) – jako pinezka w odpowiednim kolorze
 *
 * Trzymana w warstwie presentation (a nie w enum w domain), żeby model
 * domenowy nie zależał od androidx.compose.
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
            color = Color(0xFF43A047) // zielony – plac zabaw kojarzy się z trawą / outdoor
        )
        PlaceCategory.RESTAURANT -> CategoryStyle(
            icon = Icons.Filled.Restaurant,
            color = Color(0xFFE65100) // pomarańcz – jedzenie / appetite color
        )
        PlaceCategory.PLAY_ROOM -> CategoryStyle(
            icon = Icons.Filled.SmartToy,
            color = Color(0xFF7B1FA2) // fiolet – sala zabaw / indoor zabawy
        )
        PlaceCategory.CAFE -> CategoryStyle(
            icon = Icons.Filled.LocalCafe,
            color = Color(0xFF5D4037) // brąz – kawa
        )
        PlaceCategory.PARK -> CategoryStyle(
            icon = Icons.Filled.Park,
            color = Color(0xFF2E7D32) // ciemnozielony – natura
        )
        PlaceCategory.ATTRACTION -> CategoryStyle(
            icon = Icons.Filled.Attractions,
            color = Color(0xFFC2185B) // róż / fuksja – fun, atrakcje
        )
        PlaceCategory.OTHER -> CategoryStyle(
            icon = Icons.Filled.Place,
            color = Color(0xFF616161) // szary – neutralny placeholder
        )
    }
