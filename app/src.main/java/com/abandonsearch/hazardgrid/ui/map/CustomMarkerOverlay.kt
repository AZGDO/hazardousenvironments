package com.abandonsearch.hazardgrid.ui.map

import android.graphics.Point
import android.graphics.Rect
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.shapes.MaterialShapes
import androidx.compose.material3.shapes.animateRoundedPolygonAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.abandonsearch.hazardgrid.data.Place
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import org.osmdroid.views.Projection

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomMarkerOverlay(
    mapView: MapView,
    places: List<Place>,
    activePlaceId: Int?,
    onMarkerSelected: (Place?) -> Unit,
) {
    val markers = remember { mutableStateListOf<Marker>() }
    val projection = mapView.projection

    LaunchedEffect(places) {
        markers.clear()
        places.forEach { place ->
            val lat = place.lat ?: return@forEach
            val lon = place.lon ?: return@forEach
            markers.add(
                Marker(
                    id = place.id,
                    place = place,
                    point = GeoPoint(lat, lon),
                    polygon = MaterialShapes.shapes.random(),
                    targetPolygon = MaterialShapes.shapes.random(),
                    color = accentColors.random().toArgb()
                )
            )
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val tappedMarker = getTappedMarker(offset, markers, projection)

                    if (tappedMarker != null) {
                        tappedMarker.targetPolygon = MaterialShapes.shapes.random()
                        onMarkerSelected(tappedMarker.place)
                    } else {
                        markers
                            .find { it.id == activePlaceId }
                            ?.targetPolygon = MaterialShapes.shapes.random()
                        onMarkerSelected(null)
                    }
                    mapView.invalidate()
                }
            }
    ) {
        val viewBounds = mapView.boundingBox.increaseByScale(1.5f)
        markers.forEach { marker ->
            if (viewBounds.contains(marker.point)) {
                val screenPoint = projection.toPixels(marker.point, null)
                val animatedPolygon by animateRoundedPolygonAsState(
                    targetValue = marker.targetPolygon,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                val path = Path()
                animatedPolygon.toPath(path.asAndroidPath())
                drawPath(path, color = Color(marker.color))
            }
        }
    }
}

private fun getTappedMarker(
    offset: androidx.compose.ui.geometry.Offset,
    markers: List<Marker>,
    projection: Projection
): Marker? {
    val touchRect = Rect(
        (offset.x - 20).toInt(),
        (offset.y - 20).toInt(),
        (offset.x + 20).toInt(),
        (offset.y + 20).toInt()
    )

    for (i in markers.indices.reversed()) {
        val marker = markers[i]
        val screenPoint = projection.toPixels(marker.point, null)
        if (touchRect.contains(screenPoint.x, screenPoint.y)) {
            return marker
        }
    }
    return null
}

private data class Marker(
    val id: Int,
    val place: Place,
    val point: GeoPoint,
    var polygon: RoundedPolygon,
    var targetPolygon: RoundedPolygon,
    var color: Int
)

private val accentColors = listOf(
    Color(0xFFF5C400),
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFF9C27B0),
    Color(0xFFFF5722)
)
