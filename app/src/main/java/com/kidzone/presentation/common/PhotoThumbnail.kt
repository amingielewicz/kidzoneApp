package com.kidzone.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.kidzone.R

private val REMOVE_BUTTON_SIZE = 14.dp
private val REMOVE_ICON_SIZE = 8.dp

/**
 * Wspólny komponent miniatury zdjęcia z przyciskiem usuwania.
 * Używany w formularzach dodawania miejsca oraz dodawania opinii.
 */
@Suppress("FunctionNaming")
@Composable
fun KidZonePhotoThumbnail(
    model: Any,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    enabled: Boolean = true
) {
    // Kontener o stałym rozmiarze (thumbnail + wystający guzik)
    // Zapas na guzik to połowa jego rozmiaru (7dp), ale dla uproszczenia
    // używamy Boxa o rozmiarze thumbnaila z zIndexem dla guzika.
    Box(modifier = modifier.size(size + 6.dp)) {
        AsyncImage(
            model = model,
            contentDescription = stringResource(R.string.photo_thumbnail_description),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(size)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(1f)
                    .size(REMOVE_BUTTON_SIZE)
                    .clip(CircleShape)
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.95f)
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = onRemove
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.delete),
                    tint = Color.White,
                    modifier = Modifier.size(REMOVE_ICON_SIZE)
                )
            }
        }
    }
}
