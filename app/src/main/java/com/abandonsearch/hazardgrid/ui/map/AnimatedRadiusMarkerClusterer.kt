package com.abandonsearch.hazardgrid.ui.map

import android.content.Context
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.clusters.RadiusMarkerClusterer
import org.osmdroid.views.overlay.clusters.StaticCluster

class AnimatedRadiusMarkerClusterer(context: Context) : RadiusMarkerClusterer(context) {

    override fun buildClusterMarker(cluster: StaticCluster, mapView: MapView): Marker {
        return super.buildClusterMarker(cluster, mapView)
    }
}
