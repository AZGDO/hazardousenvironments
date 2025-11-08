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
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
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
    private val colorScheme: ColorScheme
) : Overlay() {

    private val markers = mutableListOf<Marker>()
    private val clusters = mutableListOf<MarkerCluster>()
    private val handler = Handler(Looper.getMainLooper())
    private var activeMarker: Marker? = null
    private val interpolator = OvershootInterpolator()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2f
    }
    private val path = Path()
    private val matrix = Matrix()

    private val shapes = listOf(
        MaterialShapes.Circle,
        MaterialShapes.Oval,
        MaterialShapes.Diamond,
        MaterialShapes.Sunny,
        MaterialShapes.Square,
        MaterialShapes.Pill,
        MaterialShapes.Cookie4Sided,
        MaterialShapes.Slanted,
        MaterialShapes.Triangle,
        MaterialShapes.Pentagon,
        MaterialShapes.Cookie6Sided,
        MaterialShapes.Arch,
        MaterialShapes.Gem,
        MaterialShapes.Cookie7Sided,
        MaterialShapes.Arrow,
        MaterialShapes.Ghostish,
        MaterialShapes.Flower,
        MaterialShapes.Bun
    )
    private val accentColors = listOf(
        colorScheme.primary,
        colorScheme.secondary,
        colorScheme.tertiary,
        colorScheme.primaryContainer,
        colorScheme.secondaryContainer,
        colorScheme.tertiaryContainer
    )

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
                    startPolygon = shapes.random(),
                    endPolygon = shapes.random(),
                    color = accentColors.random().toArgb()
                )
            )
        }
        mapView.invalidate()
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        updateClusters(projection)
        val viewBounds = mapView.boundingBox.increaseByScale(1.5f)

        clusters.forEach { cluster ->
            if (cluster.markers.size == 1) {
                val marker = cluster.markers.first()
                if (viewBounds.contains(marker.point)) {
                    drawMarker(canvas, projection, marker)
                }
            } else {
                if (cluster.isAnimating) {
                    var progress = (System.currentTimeMillis() - cluster.animationStartTime) / ANIMATION_DURATION.toFloat()
                    if (progress >= 1f) {
                        cluster.isAnimating = false
                        progress = 1f
                    }
                    val interpolatedProgress = interpolator.getInterpolation(progress)

                    // Create a Path for the end polygon at the center
                    val endPath = Path()
                    cluster.endPolygon!!.toPath(endPath)
                    val endBounds = RectF()
                    endPath.computeBounds(endBounds, true)
                    matrix.reset()
                    matrix.setRectToRect(
                        endBounds,
                        RectF(-32f, -32f, 32f, 32f),
                        Matrix.ScaleToFit.CENTER
                    )
                    endPath.transform(matrix)
                    val centerScreenPoint = projection.toPixels(cluster.centerPoint, null)
                    endPath.offset(centerScreenPoint.x.toFloat(), centerScreenPoint.y.toFloat())

                    // Morph from the cluster path to the end path
                    // This is a simplification; true morphing between arbitrary paths is complex.
                    // Here we'll just fade out the cluster and fade in the new shape.
                    val alpha = (255 * (1 - interpolatedProgress)).toInt()
                    fillPaint.alpha = alpha
                    strokePaint.alpha = alpha
                    val clusterPath = Path()
                    cluster.markers.forEach { marker ->
                        val markerPath = getMarkerPath(projection, marker)
                        clusterPath.op(markerPath, Path.Op.UNION)
                    }
                    canvas.drawPath(clusterPath, fillPaint)
                    canvas.drawPath(clusterPath, strokePaint)

                    fillPaint.alpha = (255 * interpolatedProgress).toInt()
                    strokePaint.alpha = fillPaint.alpha
                    canvas.drawPath(endPath, fillPaint)

                    fillPaint.alpha = 255
                    strokePaint.alpha = 255


                } else {
                    val clusterPath = Path()
                    cluster.markers.forEach { marker ->
                        val markerPath = getMarkerPath(projection, marker)
                        clusterPath.op(markerPath, Path.Op.UNION)
                    }

                    // For now, just use the color of the first marker in the cluster
                    fillPaint.color = cluster.markers.first().color
                    canvas.drawPath(clusterPath, fillPaint)
                    canvas.drawPath(clusterPath, strokePaint)
                }
            }
        }
    }

    private fun drawMarker(canvas: Canvas, projection: Projection, marker: Marker) {
        val screenPoint = projection.toPixels(marker.point, null)
        val markerPath = getMarkerPath(projection, marker)

        canvas.save()
        canvas.rotate(marker.rotation, screenPoint.x.toFloat(), screenPoint.y.toFloat())
        fillPaint.color = marker.color
        canvas.drawPath(markerPath, fillPaint)
        canvas.drawPath(markerPath, strokePaint)
        canvas.restore()
    }

    private fun getMarkerPath(projection: Projection, marker: Marker): Path {
        val interpolatedPoint = if (marker.isAnimating && marker.targetPoint != null) {
            val progress = interpolator.getInterpolation(((System.currentTimeMillis() - marker.animationStartTime) / ANIMATION_DURATION.toFloat()).coerceAtMost(1f))
            GeoPoint(
                marker.point.latitude + (marker.targetPoint!!.latitude - marker.point.latitude) * progress,
                marker.point.longitude + (marker.targetPoint!!.longitude - marker.point.longitude) * progress
            )
        } else {
            marker.point
        }
        val screenPoint = projection.toPixels(interpolatedPoint, null)
        val markerPath = Path()

        if (marker.isAnimating) {
            var progress = (System.currentTimeMillis() - marker.animationStartTime) / ANIMATION_DURATION.toFloat()
            if (progress >= 1f) {
                marker.isAnimating = false
                marker.startPolygon = marker.endPolygon
                progress = 1f
            }
            val interpolatedProgress = interpolator.getInterpolation(progress)
            val morph = Morph(marker.startPolygon, marker.endPolygon)
            morph.toPath(interpolatedProgress, markerPath)
        } else {
            marker.startPolygon.toPath(markerPath)
        }

        val polygonBounds = if (marker.isAnimating) {
            val tempBounds = RectF()
            markerPath.computeBounds(tempBounds, true)
            floatArrayOf(tempBounds.left, tempBounds.top, tempBounds.right, tempBounds.bottom)
        } else {
            marker.startPolygon.calculateBounds()
        }

        matrix.reset()
        matrix.setRectToRect(
            RectF(polygonBounds[0], polygonBounds[1], polygonBounds[2], polygonBounds[3]),
            RectF(-32f, -32f, 32f, 32f),
            Matrix.ScaleToFit.CENTER
        )
        markerPath.transform(matrix)
        markerPath.offset(screenPoint.x.toFloat(), screenPoint.y.toFloat())
        return markerPath
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val tappedCluster = getTappedCluster(e, mapView)

        if (tappedCluster != null) {
            if (tappedCluster.markers.size == 1) {
                val tappedMarker = tappedCluster.markers.first()
                animateShapeChange(tappedMarker)
                activeMarker = tappedMarker
                onMarkerSelected(tappedMarker.place)
            } else {
                animateClusterMorph(tappedCluster)
                onMarkerSelected(null) // Or decide what to do for cluster selection
            }
        } else {
            activeMarker?.let { animateShapeChange(it) }
            activeMarker = null
            onMarkerSelected(null)
        }
        mapView.postInvalidate()
        return tappedCluster != null
    }

    private fun getTappedCluster(e: MotionEvent, mapView: MapView): MarkerCluster? {
        for (cluster in clusters) {
            for (marker in cluster.markers) {
                val projection = mapView.projection
                val screenPoint = projection.toPixels(marker.point, null)
                val touchRect = android.graphics.Rect(e.x.toInt() - 40, e.y.toInt() - 40, e.x.toInt() + 40, e.y.toInt() + 40)
                if (touchRect.contains(screenPoint.x, screenPoint.y)) {
                    return cluster
                }
            }
        }
        return null
    }

    private fun animateClusterMorph(cluster: MarkerCluster) {
        val centerPoint = GeoPoint(
            cluster.markers.sumOf { it.point.latitude } / cluster.markers.size,
            cluster.markers.sumOf { it.point.longitude } / cluster.markers.size
        )
        val endPolygon = shapes.random()

        cluster.markers.forEach { marker ->
            marker.isAnimating = true
            marker.animationStartTime = System.currentTimeMillis()
            marker.targetPoint = centerPoint
            marker.endPolygon = endPolygon
        }

        handler.post(object : Runnable {
            override fun run() {
                if (cluster.markers.any { it.isAnimating }) {
                    mapView.postInvalidate()
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    private fun animateShapeChange(marker: Marker) {
        marker.isAnimating = true
        marker.animationStartTime = System.currentTimeMillis()

        var nextShape = shapes.random()
        while (nextShape == marker.startPolygon) {
            nextShape = shapes.random()
        }
        marker.endPolygon = nextShape

        var nextColor = accentColors.random().toArgb()
        while (nextColor == marker.color) {
            nextColor = accentColors.random().toArgb()
        }
        marker.color = nextColor

        marker.rotation = (0..360).random().toFloat()

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
        val touchRect = android.graphics.Rect(e.x.toInt() - 40, e.y.toInt() - 40, e.x.toInt() + 40, e.y.toInt() + 40)

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
        var rotation: Float = (0..360).random().toFloat(),
        var isAnimating: Boolean = false,
        var animationStartTime: Long = 0L,
        var targetPoint: GeoPoint? = null,
        val animationSpec: AnimationSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    companion object {
        private const val ANIMATION_DURATION = 500L
        private const val CLUSTER_RADIUS_DP = 150
    }

    private data class MarkerCluster(
        val markers: MutableList<Marker>,
        var isAnimating: Boolean = false,
        var animationStartTime: Long = 0L,
        var centerPoint: GeoPoint? = null,
        var endPolygon: RoundedPolygon? = null
    )

    private fun updateClusters(projection: Projection) {
        clusters.clear()
        val unclusteredMarkers = markers.toMutableList()
        val clusterRadiusPx = CLUSTER_RADIUS_DP * mapView.context.resources.displayMetrics.density

        while (unclusteredMarkers.isNotEmpty()) {
            val currentMarker = unclusteredMarkers.first()
            unclusteredMarkers.remove(currentMarker)

            val newCluster = MarkerCluster(mutableListOf(currentMarker))
            clusters.add(newCluster)

            val nearbyMarkers = unclusteredMarkers.filter {
                val p1 = projection.toPixels(currentMarker.point, null)
                val p2 = projection.toPixels(it.point, null)
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                dx * dx + dy * dy < clusterRadiusPx * clusterRadiusPx
            }

            newCluster.markers.addAll(nearbyMarkers)
            unclusteredMarkers.removeAll(nearbyMarkers)
        }
    }
}
