@file:Suppress("FunctionNaming", "LongMethod", "LongParameterList")

package com.kidzone.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kidzone.R
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

private val VIEWER_ACTION_BUTTON_SIZE = 36.dp
private val VIEWER_ACTION_ICON_SIZE = 20.dp
private val VIEWER_ACTION_BUTTON_SIZE_LARGE = 44.dp
private val VIEWER_ACTION_ICON_SIZE_LARGE = 24.dp
private val VIEWER_ACTION_PADDING = 16.dp
private val VIEWER_BOTTOM_ACTION_PADDING = 48.dp
private const val VIEWER_ACTION_CONTAINER_ALPHA = 0.82f

enum class FullscreenPhotoAction {
    NONE,
    REPORT,
    DELETE
}

/**
 * Fullscreen photo viewer z nawigacją swipe + pinch-to-zoom.
 *
 * @param photoUrls lista URL-i zdjęć do pokazania
 * @param initialIndex indeks zdjęcia od którego zaczynamy
 * @param onDismiss callback zamknięcia viewera
 * @param onReportPhoto opcjonalny callback zgłoszenia zdjęcia (URL)
 * @param canReportPhoto per-URL check czy flaga zgłoszenia jest widoczna
 *   (np. ukryta dla zdjęć dodanych przez bieżącego usera)
 * @param onDeletePhoto opcjonalny callback usunięcia zdjęcia (URL)
 * @param canDeletePhoto per-URL check czy przycisk usunięcia jest widoczny
 *   (true gdy `photoUploadedBy[url] == currentUserId`)
 */
@Composable
fun FullscreenPhotoViewer(
    photoUrls: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onReportPhoto: ((photoUrl: String) -> Unit)? = null,
    canReportPhoto: (photoUrl: String) -> Boolean = { true },
    onDeletePhoto: ((photoUrl: String) -> Unit)? = null,
    canDeletePhoto: (photoUrl: String) -> Boolean = { false },
    photoAction: ((photoUrl: String) -> FullscreenPhotoAction)? = null
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
            // Track whether any page is zoomed – disable pager scroll when zoomed
            var isZoomed by remember { mutableStateOf(false) }

            val pagerState = rememberPagerState(
                initialPage = initialIndex.coerceIn(0, photoUrls.lastIndex),
                pageCount = { photoUrls.size }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isZoomed
            ) { page ->
                ZoomableImage(
                    imageUrl = photoUrls[page],
                    onTap = { /* single tap - do nothing */ },
                    onZoomChange = { zoomed -> isZoomed = zoomed }
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(VIEWER_ACTION_PADDING)
                    .size(VIEWER_ACTION_BUTTON_SIZE)
                    .background(
                        color = Color.Black.copy(alpha = VIEWER_ACTION_CONTAINER_ALPHA),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Zamknij",
                    tint = Color.White,
                    modifier = Modifier.size(VIEWER_ACTION_ICON_SIZE)
                )
            }

            // Safe current page index – guards against IndexOutOfBoundsException
            // when photoUrls list shrinks after deletion while pagerState hasn't
            // updated yet.
            val safeCurrentPage = pagerState.currentPage.coerceIn(0, photoUrls.lastIndex)

            val currentPhotoUrl = photoUrls[safeCurrentPage]
            val currentAction = photoAction?.invoke(currentPhotoUrl) ?: when {
                onDeletePhoto != null && canDeletePhoto(currentPhotoUrl) -> FullscreenPhotoAction.DELETE
                onReportPhoto != null && canReportPhoto(currentPhotoUrl) -> FullscreenPhotoAction.REPORT
                else -> FullscreenPhotoAction.NONE
            }
            if (currentAction != FullscreenPhotoAction.NONE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(
                            end = VIEWER_ACTION_PADDING,
                            bottom = VIEWER_BOTTOM_ACTION_PADDING
                        )
                        .zIndex(2f)
                ) {
                    if (currentAction == FullscreenPhotoAction.REPORT && onReportPhoto != null) {
                        IconButton(
                            onClick = { onReportPhoto(currentPhotoUrl) },
                            modifier = Modifier
                                .size(VIEWER_ACTION_BUTTON_SIZE_LARGE)
                                .background(
                                    color = Color.Black.copy(alpha = VIEWER_ACTION_CONTAINER_ALPHA),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = stringResource(R.string.report_photo),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(VIEWER_ACTION_ICON_SIZE_LARGE)
                            )
                        }
                    }

                    if (currentAction == FullscreenPhotoAction.DELETE && onDeletePhoto != null) {
                        IconButton(
                            onClick = {
                                onDeletePhoto(currentPhotoUrl)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(VIEWER_ACTION_BUTTON_SIZE_LARGE)
                                .background(
                                    color = Color.Black.copy(alpha = VIEWER_ACTION_CONTAINER_ALPHA),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete_photo),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(VIEWER_ACTION_ICON_SIZE_LARGE)
                            )
                        }
                    }
                }
            }

            // Page indicator (bottom-center)
            if (photoUrls.size > 1) {
                Text(
                    text = "${safeCurrentPage + 1} / ${photoUrls.size}",
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
 *
 * Kluczowe: przy `scale == 1f` (brak zoomu) ten composable NIE przechwytuje
 * gestów jednopalcowych (drag), dzięki czemu HorizontalPager normalnie
 * obsługuje swipe lewo/prawo. Pinch-to-zoom (dwa palce) działa zawsze.
 * Po powiększeniu (scale > 1f) pan jednopalcowy przesuwa zdjęcie, a pager
 * jest wyłączony przez `userScrollEnabled = false`.
 *
 * @param onZoomChange informuje rodzica czy zdjęcie jest powiększone
 */
@Composable
private fun ZoomableImage(
    imageUrl: String,
    onTap: () -> Unit = {},
    onZoomChange: (Boolean) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Czekaj na pierwszy palec
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.size

                        // Pinch-to-zoom: przechwytuj TYLKO gdy 2+ palce
                        // Pan: przechwytuj TYLKO gdy powiększony (scale > 1f)
                        if (pointerCount >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            onZoomChange(newScale > 1f)
                            if (newScale > 1f) {
                                val maxX = (size.width * (newScale - 1)) / 2
                                val maxY = (size.height * (newScale - 1)) / 2
                                offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                            // Konsumuj eventy żeby nie „uciekły" do pagera
                            event.changes.forEach { it.consume() }
                        } else if (pointerCount == 1 && scale > 1f) {
                            // Jeden palec + powiększone = pan po zdjęciu
                            val pan = event.calculatePan()
                            val maxX = (size.width * (scale - 1)) / 2
                            val maxY = (size.height * (scale - 1)) / 2
                            offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                            event.changes.forEach { it.consume() }
                        }
                        // Jeden palec + scale == 1f → NIE konsumujemy →
                        // HorizontalPager obsługuje swipe.
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.5f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            onZoomChange(false)
                        } else {
                            scale = 3f
                            onZoomChange(true)
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
