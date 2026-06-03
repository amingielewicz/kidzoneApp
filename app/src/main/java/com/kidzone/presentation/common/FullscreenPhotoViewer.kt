package com.kidzone.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Fullscreen photo viewer z nawigacją swipe + pinch-to-zoom.
 *
 * @param photoUrls lista URL-i zdjęć do pokazania
 * @param initialIndex indeks zdjęcia od którego zaczynamy
 * @param onDismiss callback zamknięcia viewera
 * @param onReportPhoto opcjonalny callback zgłoszenia zdjęcia (index -> URL)
 */
@Composable
fun FullscreenPhotoViewer(
    photoUrls: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onReportPhoto: ((photoUrl: String) -> Unit)? = null
) {
    if (photoUrls.isEmpty()) {
        onDismiss()
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val pagerState = rememberPagerState(
                initialPage = initialIndex.coerceIn(0, photoUrls.lastIndex),
                pageCount = { photoUrls.size }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(
                    imageUrl = photoUrls[page],
                    onTap = { /* single tap - do nothing, could toggle UI */ }
                )
            }

            // Close button (top-left)
            FilledTonalIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Zamknij"
                )
            }

            // Report button (top-right) - only if callback provided
            if (onReportPhoto != null) {
                FilledTonalIconButton(
                    onClick = {
                        val currentUrl = photoUrls[pagerState.currentPage]
                        onReportPhoto(currentUrl)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = "Zgłoś zdjęcie"
                    )
                }
            }

            // Page indicator (bottom-center)
            if (photoUrls.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${photoUrls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Zdjęcie z obsługą pinch-to-zoom i pan (przesuwanie po powiększeniu).
 */
@Composable
private fun ZoomableImage(
    imageUrl: String,
    onTap: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale > 1f) {
                        val maxX = (size.width * (newScale - 1)) / 2
                        val maxY = (size.height * (newScale - 1)) / 2
                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Double tap to toggle zoom
                        if (scale > 1.5f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 3f
                        }
                    },
                    onTap = { onTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Zdjęcie pełnoekranowe",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )
    }
}
