package com.kidzone.widget

import android.content.Context
import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.room.Room
import com.kidzone.data.local.KidZoneDatabase
import com.kidzone.data.local.PlaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Glance AppWidget showing the 3 nearest places from the local Room cache.
 *
 * Layout per row: Place name | Rating (stars) | Distance (km/m)
 *
 * Refresh strategy: periodic every 30 minutes via system AppWidget update
 * mechanism (configured in widget_info.xml). Widget reads last known location
 * from SharedPreferences (written by the app when location is fetched).
 */
class NearbyPlacesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nearbyPlaces = withContext(Dispatchers.IO) {
            loadNearbyPlaces(context)
        }

        provideContent {
            GlanceTheme {
                NearbyPlacesContent(places = nearbyPlaces)
            }
        }
    }

    private fun loadNearbyPlaces(context: Context): List<WidgetPlace> {
        val db = Room.databaseBuilder(
            context,
            KidZoneDatabase::class.java,
            "kidzone_cache.db"
        ).fallbackToDestructiveMigration().build()

        val allPlaces = db.placeDao().runCatching {
            // Room suspend functions require coroutine scope, but we're
            // already in IO context from provideGlance. Use blocking query
            // as a workaround for widget simplicity.
            val cursor = db.openHelper.readableDatabase.query(
                "SELECT id, name, averageRating, reviewsCount, latitude, longitude FROM places"
            )
            val result = mutableListOf<WidgetPlace>()
            while (cursor.moveToNext()) {
                result.add(
                    WidgetPlace(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        averageRating = cursor.getDouble(2),
                        reviewsCount = cursor.getInt(3),
                        latitude = cursor.getDouble(4),
                        longitude = cursor.getDouble(5)
                    )
                )
            }
            cursor.close()
            result
        }.getOrElse { emptyList() }

        db.close()

        // Get last known location from shared preferences
        val prefs = context.getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE)
        val userLat = prefs.getFloat(KEY_LAST_LAT, 0f).toDouble()
        val userLng = prefs.getFloat(KEY_LAST_LNG, 0f).toDouble()

        if (userLat == 0.0 && userLng == 0.0) {
            // No location available – return top-rated places as fallback
            return allPlaces
                .sortedByDescending { it.averageRating }
                .take(3)
                .map { it.copy(distanceMeters = null) }
        }

        // Calculate distance and sort by nearest
        return allPlaces
            .map { place ->
                val results = FloatArray(1)
                Location.distanceBetween(userLat, userLng, place.latitude, place.longitude, results)
                place.copy(distanceMeters = results[0].toInt())
            }
            .sortedBy { it.distanceMeters }
            .take(3)
    }

    companion object {
        const val LOCATION_PREFS = "kidzone_widget_location"
        const val KEY_LAST_LAT = "last_latitude"
        const val KEY_LAST_LNG = "last_longitude"
    }
}

/**
 * Lightweight data class for widget display – only the fields we need.
 */
data class WidgetPlace(
    val id: String,
    val name: String,
    val averageRating: Double,
    val reviewsCount: Int,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int? = null
)

@Composable
private fun NearbyPlacesContent(places: List<WidgetPlace>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .background(GlanceTheme.colors.surface),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "Miejsca w pobliżu",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurface
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        if (places.isEmpty()) {
            Text(
                text = "Brak miejsc w pamięci podręcznej",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        } else {
            places.forEach { place ->
                PlaceRow(place = place)
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PlaceRow(place: WidgetPlace) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name (takes remaining space)
        Text(
            text = place.name,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Rating
        val ratingText = if (place.reviewsCount > 0) {
            "\u2605 %.1f".format(place.averageRating)
        } else {
            "\u2605 —"
        }
        Text(
            text = ratingText,
            style = TextStyle(
                fontSize = 12.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Distance
        val distanceText = place.distanceMeters?.let { meters ->
            if (meters < 1000) "${meters}m" else "%.1fkm".format(meters / 1000.0)
        } ?: ""
        if (distanceText.isNotEmpty()) {
            Text(
                text = distanceText,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.primary
                )
            )
        }
    }
}

/**
 * BroadcastReceiver that triggers widget updates.
 * Declared in AndroidManifest with the widget metadata.
 */
class NearbyPlacesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NearbyPlacesWidget()
}
