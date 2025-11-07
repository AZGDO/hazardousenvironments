package com.abandonsearch.hazardgrid.ui.map

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.view.MotionEvent
import android.content.Context
import com.abandonsearch.hazardgrid.data.Place
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class CustomMarkerOverlay(
    context: Context,
    private val onMarkerSelected: (Place?) -> Unit,
) : Overlay() {

    private val markers = mutableListOf<Marker>()
    private val visibleMarkers = mutableListOf<Marker>()
    private var activeMarker: Marker? = null
    private val markerFactory = HazardMarkerFactory(context)
    private val markerRect = Rect()
    private val viewRect = Rect()
    private val touchPoint = Point()

    fun setPlaces(places: List<Place>, activePlaceId: Int?) {
        activeMarker = null
        markers.clear()
        places.forEach { place ->
            val lat = place.lat ?: return@forEach
            val lon = place.lon ?: return@forEach
            val marker = Marker(
                id = place.id,
                place = place,
                point = GeoPoint(lat, lon)
            )
            markers.add(marker)
            if (place.id == activePlaceId) {
                activeMarker = marker
            }
        }
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val projection = mapView.projection
        val viewBounds = mapView.boundingBox
        visibleMarkers.clear()

        for (i in markers.indices.reversed()) {
            val marker = markers[i]
            if (viewBounds.contains(marker.point)) {
                visibleMarkers.add(marker)
                drawMarker(canvas, projection, marker)
            }
        }
    }

    private fun drawMarker(canvas: Canvas, projection: Projection, marker: Marker) {
        val screenPoint = projection.toPixels(marker.point, null)
        val drawable = markerFactory.getDrawable(isSelected = marker.id == activeMarker?.id)
        val halfWidth = drawable.intrinsicWidth / 2
        val halfHeight = drawable.intrinsicHeight / 2

        markerRect.set(
            screenPoint.x - halfWidth,
            screenPoint.y - halfHeight,
            screenPoint.x + halfWidth,
            screenPoint.y + halfHeight
        )
        drawable.bounds = markerRect
        drawable.draw(canvas)
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val tappedMarker = getTappedMarker(e, mapView)
        activeMarker = tappedMarker
        onMarkerSelected(tappedMarker?.place)
        mapView.invalidate()
        return tappedMarker != null
    }

    private fun getTappedMarker(e: MotionEvent, mapView: MapView): Marker? {
        val projection = mapView.projection
        touchPoint.set(e.x.toInt(), e.y.toInt())
        mapView.getDrawingRect(viewRect)

        for (i in visibleMarkers.indices.reversed()) {
            val marker = visibleMarkers[i]
            val screenPoint = projection.toPixels(marker.point, null)
            val drawable = markerFactory.getDrawable(isSelected = false)
            val halfWidth = drawable.intrinsicWidth / 2
            val halfHeight = drawable.intrinsicHeight / 2

            markerRect.set(
                screenPoint.x - halfWidth,
                screenPoint.y - halfHeight,
                screenPoint.x + halfWidth,
                screenPoint.y + halfHeight
            )

            if (markerRect.contains(touchPoint.x, touchPoint.y)) {
                return marker
            }
        }
        return null
    }

    private data class Marker(
        val id: Int,
        val place: Place,
        val point: GeoPoint,
    )
}
