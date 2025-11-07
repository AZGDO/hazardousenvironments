package com.abandonsearch.hazardgrid.ui.map

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.view.MotionEvent
import com.abandonsearch.hazardgrid.data.Place
import com.abandonsearch.hazardgrid.domain.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class CustomMarkerOverlay(
    private val onMarkerSelected: (Place?) -> Unit,
) : Overlay() {

    private val markers = mutableListOf<Marker>()
    private val visibleMarkers = mutableListOf<Marker>()
    private var activeMarker: Marker? = null
    private val markerFactory = HazardMarkerFactory()
    private val markerRect = Rect()
    private val viewRect = Rect()
    private val touchPoint = Point()

    fun setPlaces(places: List<Place>, activePlaceId: Int?) {
        markers.clear()
        places.forEach { place ->
            val marker = Marker(
                id = place.id,
                place = place,
                point = GeoPoint(place.latitude, place.longitude)
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
        markers.forEach { marker ->
            if (viewBounds.contains(marker.point.latitude, marker.point.longitude)) {
                visibleMarkers.add(marker)
                drawMarker(canvas, projection, marker)
            }
        }
    }

    private fun drawMarker(canvas: Canvas, projection: Projection, marker: Marker) {
        val screenPoint = projection.toPixels(marker.point.latitude, marker.point.longitude, null)
        val drawable = markerFactory.create(marker.place, isSelected = marker.id == activeMarker?.id)
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
        if (tappedMarker != null) {
            activeMarker = tappedMarker
            onMarkerSelected(tappedMarker.place)
        } else {
            activeMarker = null
            onMarkerSelected(null)
        }
        mapView.invalidate()
        return tappedMarker != null
    }

    private fun getTappedMarker(e: MotionEvent, mapView: MapView): Marker? {
        val projection = mapView.projection
        touchPoint.set(e.x.toInt(), e.y.toInt())
        mapView.getDrawingRect(viewRect)

        for (i in visibleMarkers.indices.reversed()) {
            val marker = visibleMarkers[i]
            val screenPoint = projection.toPixels(marker.point.latitude, marker.point.longitude, null)
            val drawable = markerFactory.create(marker.place, isSelected = false)
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
