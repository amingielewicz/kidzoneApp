@file:Suppress("FunctionNaming", "MagicNumber")

package com.kidzone.widget

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.room.Room
import com.kidzone.MainActivity
import com.kidzone.R
import com.kidzone.data.local.KidZoneDatabase
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.presentation.common.isNewWithoutReviews
import com.kidzone.utils.DistanceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val WidgetGlassBackground = ColorProvider(Color(0xE6FFFFFF))
private val WidgetPrimaryBlue = ColorProvider(Color(0xFF1976D2))
private val WidgetStarGold = ColorProvider(Color(0xFFFFC107))
private val WidgetTextPrimary = ColorProvider(Color(0xFF1F2933))
private val WidgetTextSecondary = ColorProvider(Color(0xFF68717D))
private val WidgetRowBackground = ColorProvider(Color(0x33FFFFFF))
private val WidgetNewBackground = ColorProvider(Color(0xFFE3F2FD))
private val WidgetNewText = ColorProvider(Color(0xFF0D47A1))
private const val MAX_WIDGET_PLACES = 50
private const val ONE_MINUTE_MILLIS = 60_000L

/**
 * 🎯 Odpowiedzialności:
 * - Wyświetlanie listy najbliższych miejsc przyjaznych dzieciom bezpośrednio na ekranie głównym systemu.
 * - Udostępnianie statusu świeżości lokalizacji (stale location indicator).
 * - Szybka nawigacja (Deep Linking) do szczegółów wybranych miejsc.
 *
 * 🛡️ Bezpieczeństwo i Prywatność:
 * - Wykorzystuje przybliżoną lokalizację zapisaną w SharedPreferences.
 * - Nie pobiera lokalizacji w tle (tylko odczytuje stan z apki głównej).
 *
 * ⚡ Wydajność i Zasoby:
 * - Odczyt danych z cache Room (brak zapytań sieciowych z poziomu widgetu).
 * - Ograniczenie liczby wyświetlanych elementów do [MAX_WIDGET_PLACES] dla oszczędności RAM.
 */
class NearbyPlacesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetState = withContext(Dispatchers.IO) {
            loadWidgetState(context)
        }

        provideContent {
            GlanceTheme {
                NearbyPlacesContent(
                    context = context,
                    places = widgetState.places,
                    staleLocationAgeMinutes = widgetState.staleLocationAgeMinutes
                )
            }
        }
    }

    private fun loadWidgetState(context: Context): WidgetState {
        val allPlaces = loadPlacesFromCache(context)

        val prefs = context.getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE)
        val userLat = prefs.getFloat(KEY_LAST_LAT, 0f).toDouble()
        val userLng = prefs.getFloat(KEY_LAST_LNG, 0f).toDouble()

        if (userLat == 0.0 && userLng == 0.0) {
            return WidgetState(
                places = allPlaces
                    .sortedByDescending { it.averageRating }
                    .take(MAX_WIDGET_PLACES)
                    .map { it.copy(distanceMeters = null) },
                staleLocationAgeMinutes = null
            )
        }

        val locationTimestampMillis = prefs.getLong(KEY_LAST_LOCATION_TIME, 0L)
        val staleLocationAgeMinutes = locationTimestampMillis
            .takeIf { it > 0L }
            ?.let { ((System.currentTimeMillis() - it) / ONE_MINUTE_MILLIS).toInt().coerceAtLeast(0) }

        return WidgetState(
            places = allPlaces
                .map { place ->
                    val results = FloatArray(1)
                    Location.distanceBetween(userLat, userLng, place.latitude, place.longitude, results)
                    place.copy(distanceMeters = results[0].toInt())
                }
                .sortedBy { it.distanceMeters }
                .take(MAX_WIDGET_PLACES),
            staleLocationAgeMinutes = staleLocationAgeMinutes
        )
    }

    private fun loadPlacesFromCache(context: Context): List<WidgetPlace> {
        val db = Room.databaseBuilder(
            context,
            KidZoneDatabase::class.java,
            "kidzone_cache.db"
        ).fallbackToDestructiveMigration().build()

        val allPlaces = db.placeDao().runCatching {
            val cursor = db.openHelper.readableDatabase.query(
                """
                SELECT id, name, category, averageRating, reviewsCount,
                       latitude, longitude, createdAtMillis
                FROM places
                """.trimIndent()
            )
            val result = mutableListOf<WidgetPlace>()
            while (cursor.moveToNext()) {
                result.add(
                    WidgetPlace(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        category = PlaceCategory.fromKey(cursor.getString(2)),
                        averageRating = cursor.getDouble(3),
                        reviewsCount = cursor.getInt(4),
                        latitude = cursor.getDouble(5),
                        longitude = cursor.getDouble(6),
                        createdAtMillis = cursor.getLong(7)
                    )
                )
            }
            cursor.close()
            result
        }.getOrElse { emptyList() }

        db.close()
        return allPlaces
    }

    companion object {
        const val LOCATION_PREFS = "kidzone_widget_location"
        const val KEY_LAST_LAT = "last_latitude"
        const val KEY_LAST_LNG = "last_longitude"
        const val KEY_LAST_LOCATION_TIME = "last_location_time"
    }
}

private data class WidgetState(
    val places: List<WidgetPlace>,
    val staleLocationAgeMinutes: Int?
)

data class WidgetPlace(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val averageRating: Double,
    val reviewsCount: Int,
    val latitude: Double,
    val longitude: Double,
    val createdAtMillis: Long,
    val distanceMeters: Int? = null
)

@Composable
private fun NearbyPlacesContent(
    context: Context,
    places: List<WidgetPlace>,
    staleLocationAgeMinutes: Int?
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetGlassBackground)
            .cornerRadius(20.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        WidgetHeader(context = context)
        Spacer(modifier = GlanceModifier.height(12.dp))

        if (places.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_no_cached_places),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = WidgetTextSecondary
                )
            )
        } else {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            ) {
                items(
                    items = places,
                    itemId = { it.stableItemId }
                ) { place ->
                    PlaceRow(
                        context = context,
                        place = place,
                        staleLocationAgeMinutes = staleLocationAgeMinutes
                    )
                    Spacer(modifier = GlanceModifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(context: Context) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = context.getString(R.string.widget_nearby_places_title),
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetTextPrimary
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text = context.getString(R.string.widget_brand_label),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetPrimaryBlue
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun PlaceRow(
    context: Context,
    place: WidgetPlace,
    staleLocationAgeMinutes: Int?
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetRowBackground)
            .cornerRadius(14.dp)
            .clickable(actionStartActivity(place.detailsIntent(context)))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(place.category.widgetIconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(32.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = GlanceModifier.width(10.dp))

        Text(
            text = place.name,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = WidgetTextPrimary
            ),
            maxLines = 2,
            modifier = GlanceModifier.defaultWeight()
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaceRatingStatus(place = place)

            val distanceText = place.distanceText(context)
            if (distanceText.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = distanceText.withStaleAge(context, staleLocationAgeMinutes),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = WidgetPrimaryBlue
                    )
                )
            }
        }
    }
}

@Composable
private fun PlaceRatingStatus(place: WidgetPlace) {
    when {
        place.reviewsCount > 0 -> RatingRow(
            text = "%.1f (%d)".format(place.averageRating, place.reviewsCount),
            fontSize = 12,
            starColor = WidgetStarGold,
            textColor = WidgetTextPrimary
        )

        isNewWithoutReviews(
            reviewsCount = place.reviewsCount,
            createdAtMillis = place.createdAtMillis
        ) -> Text(
            text = "Nowe",
            modifier = GlanceModifier
                .background(WidgetNewBackground)
                .cornerRadius(12.dp)
                .padding(horizontal = 7.dp, vertical = 4.dp),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetNewText
            ),
            maxLines = 1
        )

        else -> RatingRow(
            text = "Brak ocen",
            fontSize = 11,
            starColor = WidgetTextSecondary,
            textColor = WidgetTextSecondary
        )
    }
}

@Composable
private fun RatingRow(
    text: String,
    fontSize: Int,
    starColor: ColorProvider,
    textColor: ColorProvider
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "★",
            style = TextStyle(
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Medium,
                color = starColor
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            ),
            maxLines = 1
        )
    }
}

private fun String.withStaleAge(context: Context, staleLocationAgeMinutes: Int?): String {
    if (isBlank()) return this
    val ageText = staleLocationAgeMinutes?.let {
        DistanceUtils.formatStaleAge(context, it)
    }
    return ageText?.let { "$this ($it)" } ?: this
}

private val WidgetPlace.stableItemId: Long
    get() = id.hashCode().toLong().let { if (it > 0) it else -it + 1L }

private fun WidgetPlace.distanceText(context: Context): String = distanceMeters?.let { meters ->
    DistanceUtils.formatDistanceMeters(context, meters)
}.orEmpty()

private fun WidgetPlace.detailsIntent(context: Context): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse("kidzone://place/$id"))
        .setClass(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private val PlaceCategory.widgetIconRes: Int
    get() = when (this) {
        PlaceCategory.PLAYGROUND -> R.drawable.ic_map_marker_playground
        PlaceCategory.PLAY_ROOM -> R.drawable.ic_map_marker_play_room
        PlaceCategory.CAFE -> R.drawable.ic_map_marker_cafe
        PlaceCategory.RESTAURANT -> R.drawable.ic_map_marker_restaurant
        PlaceCategory.PARK -> R.drawable.ic_map_marker_park
        PlaceCategory.ATTRACTION -> R.drawable.ic_map_marker_attraction
        PlaceCategory.OTHER -> R.drawable.ic_map_marker_other
    }

class NearbyPlacesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NearbyPlacesWidget()
}
