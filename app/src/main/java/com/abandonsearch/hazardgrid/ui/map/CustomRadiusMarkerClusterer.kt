package com.abandonsearch.hazardgrid.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.animation.ValueAnimator
import android.graphics.drawable.BitmapDrawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.clusters.RadiusMarkerClusterer
import org.osmdroid.views.overlay.clusters.StaticCluster

class CustomRadiusMarkerClusterer(context: Context) : RadiusMarkerClusterer(context) {

    private val clusterPaint = Paint().apply {
        color = Color.parseColor("#FF5722")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    override fun buildClusterMarker(cluster: StaticCluster, mapView: MapView): Marker {
        val marker = super.buildClusterMarker(cluster, mapView)
        marker.setOnMarkerClickListener { _, _ ->
            animateCluster(mapView, cluster)
            true
        }
        return marker
    }

    private fun animateCluster(mapView: MapView, cluster: StaticCluster) {
        val markers = cluster.items
        val clusterCenter = cluster.position

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 500
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            for (marker in markers) {
                val startPoint = clusterCenter
                val endPoint = marker.position
                val newLat = startPoint.latitude + fraction * (endPoint.latitude - startPoint.latitude)
                val newLon = startPoint.longitude + fraction * (endPoint.longitude - startPoint.longitude)
                marker.position = GeoPoint(newLat, newLon)
            }
            mapView.invalidate()
        }
        animator.start()
        mapView.controller.animateTo(clusterCenter, mapView.zoomLevelDouble + 2, 500L)
    }

    override fun setClusterIcon(cluster: StaticCluster, marker: Marker) {
        val size = cluster.size.toString()
        val radius = 40f
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawCircle(radius, radius, radius, clusterPaint)
        canvas.drawText(size, radius, radius - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
        marker.icon = BitmapDrawable(mContext.resources, bitmap)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    }
}
