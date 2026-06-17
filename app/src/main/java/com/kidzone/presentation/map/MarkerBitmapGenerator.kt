package com.kidzone.presentation.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.presentation.common.style
import java.util.concurrent.ConcurrentHashMap

private val globalCategoryCache = ConcurrentHashMap<PlaceCategory, BitmapDescriptor>()
private val globalClusterCache = ConcurrentHashMap<String, BitmapDescriptor>()

@Composable
fun rememberMarkerIcons(): MarkerIconCache {
    val density = LocalDensity.current
    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary.toArgb()
    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.toArgb()

    val playgroundPainter = rememberVectorPainter(PlaceCategory.PLAYGROUND.style.icon)
    val restaurantPainter = rememberVectorPainter(PlaceCategory.RESTAURANT.style.icon)
    val playroomPainter = rememberVectorPainter(PlaceCategory.PLAY_ROOM.style.icon)
    val cafePainter = rememberVectorPainter(PlaceCategory.CAFE.style.icon)
    val parkPainter = rememberVectorPainter(PlaceCategory.PARK.style.icon)
    val attractionPainter = rememberVectorPainter(PlaceCategory.ATTRACTION.style.icon)
    val otherPainter = rememberVectorPainter(PlaceCategory.OTHER.style.icon)

    val painters = mapOf(
        PlaceCategory.PLAYGROUND to playgroundPainter,
        PlaceCategory.RESTAURANT to restaurantPainter,
        PlaceCategory.PLAY_ROOM to playroomPainter,
        PlaceCategory.CAFE to cafePainter,
        PlaceCategory.PARK to parkPainter,
        PlaceCategory.ATTRACTION to attractionPainter,
        PlaceCategory.OTHER to otherPainter
    )

    val cache = remember(density, primaryColor, onPrimaryColor, surfaceColor) {
        globalCategoryCache.clear()
        globalClusterCache.clear()
        MarkerIconCache(density, primaryColor, onPrimaryColor, surfaceColor)
    }

    LaunchedEffect(cache, painters) {
        PlaceCategory.entries.forEach { category ->
            val painter = painters[category] ?: return@forEach
            val sizePx = with(density) { 36.dp.toPx() }.toInt()
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val center = sizePx / 2f
            val radius = with(density) { 14.dp.toPx() }
            val borderPx = with(density) { 2.dp.toPx() }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = 0x22000000
            canvas.drawCircle(center, center + with(density) { 1.dp.toPx() }, radius, paint)

            paint.color = category.style.color.toArgb()
            canvas.drawCircle(center, center, radius, paint)

            paint.color = surfaceColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = borderPx
            canvas.drawCircle(center, center, radius, paint)

            val iconSizePx = with(density) { 16.dp.toPx() }
            val offset = (sizePx - iconSizePx) / 2f
            val composeCanvas = androidx.compose.ui.graphics.Canvas(canvas)
            canvas.save()
            canvas.translate(offset, offset)
            CanvasDrawScope().draw(
                density = density,
                layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
                canvas = composeCanvas,
                size = Size(iconSizePx, iconSizePx)
            ) {
                with(painter) {
                    draw(
                        size = Size(iconSizePx, iconSizePx),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                    )
                }
            }
            canvas.restore()
            
            globalCategoryCache[category] = BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    return cache
}

class MarkerIconCache(
    private val density: Density,
    private val primaryColor: Int,
    private val onPrimaryColor: Int,
    private val surfaceColor: Int
) {
    fun getCategoryIcon(category: PlaceCategory): BitmapDescriptor {
        return globalCategoryCache[category] ?: BitmapDescriptorFactory.defaultMarker()
    }

    fun getClusterIcon(count: Int): BitmapDescriptor {
        val label = clusterCountLabel(count)
        return globalClusterCache.getOrPut(label) {
            createClusterBitmap(label)
        }
    }

    private fun createClusterBitmap(label: String): BitmapDescriptor {
        val sizePx = with(density) { 40.dp.toPx() }.toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = sizePx / 2f
        val radius = with(density) { 16.dp.toPx() }
        val borderPx = with(density) { 2.dp.toPx() }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0x22000000
        canvas.drawCircle(center, center + with(density) { 1.dp.toPx() }, radius, paint)

        paint.color = primaryColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(center, center, radius, paint)

        paint.color = surfaceColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderPx
        canvas.drawCircle(center, center, radius, paint)

        paint.style = Paint.Style.FILL
        paint.color = onPrimaryColor
        paint.textSize = with(density) { 12.dp.toPx() }
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER

        val textBounds = Rect()
        paint.getTextBounds(label, 0, label.length, textBounds)
        canvas.drawText(label, center, center + textBounds.height() / 2f, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

internal fun clusterCountLabel(count: Int): String = when {
    count < 10 -> count.toString()
    count >= 100 -> "100+"
    else -> "${(count / 10) * 10}+"
}
