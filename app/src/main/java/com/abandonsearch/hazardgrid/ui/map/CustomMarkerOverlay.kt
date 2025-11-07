package com.abandonsearch.hazardgrid.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.shapes.MaterialShapes
import androidx.compose.ui.graphics.toArgb
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.abandonsearch.hazardgrid.data.Place
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class CustomMarkerOverlay(
    private val mapView: MapView,
    private val onMarkerSelected: (Place?) -> Unit,
) : Overlay() {

    private val markers = mutableListOf<Marker>()
    private val handler = Handler(Looper.getMainLooper())
    private var activeMarker: Marker? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2f
    }
    private val path = Path()
    private val matrix = Matrix()

    fun setPlaces(places: List<Place>) {
        markers.clear()
        places.forEach { place ->
            val lat = place.lat ?: return@forEach
            val lon = place.lon ?: return@forEach
            markers.add(
                Marker(
                    id = place.id,
                    place = place,
                    point = GeoPoint(lat, lon),
                    startPolygon = MaterialShapes.shapes.random(),
                    endPolygon = MaterialShapes.shapes.random(),
                    color = accentColors.random().toArgb()
                )
            )
        }
        mapView.invalidate()
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        val viewBounds = mapView.boundingBox.increaseByScale(1.5f)
        markers.forEach { marker ->
            if (viewBounds.contains(marker.point)) {
                val screenPoint = projection.toPixels(marker.point, null)
                val polygon = if (marker.isAnimating) {
                    val progress = (System.currentTimeMillis() - marker.animationStartTime) / ANIMATION_DURATION.toFloat()
                    if (progress >= 1f) {
                        marker.isAnimating = false
                        marker.startPolygon = marker.endPolygon
                        marker.startPolygon
                    } else {
                        val morph = Morph(marker.startPolygon, marker.endPolygon)
                        // This is a simplified animation. A real implementation would use an animator.
                        // For now, we'll just interpolate the progress.
                        // val interpolatedProgress = marker.animationSpec.transform(progress)
                        // morph.toPath(interpolatedProgress, path)
                        // For now, just snap to the end.
                        marker.endPolygon
                    }
                } else {
                    marker.startPolygon
                }

                val polygonBounds = polygon.getBounds()
                matrix.setRectToRect(polygonBounds, RectF(-32f, -32f, 32f, 32f), Matrix.ScaleToFit.CENTER)
                polygon.toPath(path)
                path.transform(matrix)
                path.offset(screenPoint.x.toFloat(), screenPoint.y.toFloat())

                fillPaint.color = marker.color
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val tappedMarker = getTappedMarker(e, mapView)

        if (tappedMarker != null) {
            if (activeMarker != tappedMarker) {
                activeMarker?.let { animateShapeChange(it) }
                animateShapeChange(tappedMarker)
                activeMarker = tappedMarker
            }
            onMarkerSelected(tappedMarker.place)
        } else {
            activeMarker?.let { animateShapeChange(it) }
            activeMarker = null
            onMarkerSelected(null)
        }
        mapView.postInvalidate()
        return tappedMarker != null
    }

    private fun animateShapeChange(marker: Marker) {
        marker.isAnimating = true
        marker.animationStartTime = System.currentTimeMillis()
        marker.endPolygon = MaterialShapes.shapes.random()
        // In a real app, you'd use a ValueAnimator, but for now, we'll just invalidate.
        handler.post(object : Runnable {
            override fun run() {
                if (marker.isAnimating) {
                    mapView.postInvalidate()
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    private fun getTappedMarker(e: MotionEvent, mapView: MapView): Marker? {
        val projection = mapView.projection
        val touchRect = Rect(e.x.toInt() - 20, e.y.toInt() - 20, e.x.toInt() + 20, e.y.toInt() + 20)

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
        var startPolygon: RoundedPolygon,
        var endPolygon: RoundedPolygon,
        var color: Int,
        var isAnimating: Boolean = false,
        var animationStartTime: Long = 0L,
        val animationSpec: AnimationSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    companion object {
        private const val ANIMATION_DURATION = 500L
        private val accentColors = listOf(
            android.graphics.Color.parseColor("#F5C400"),
            android.graphics.Color.parseColor("#4CAF50"),
            android.graphics.Color.parseColor("#2196F3"),
            android.graphics.Color.parseColor("#9C27B0"),
            android.graphics.Color.parseColor("#FF5722")
        )
    }
}
