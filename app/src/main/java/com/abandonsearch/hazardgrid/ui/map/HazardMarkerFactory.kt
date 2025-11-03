package com.abandonsearch.hazardgrid.ui.map

import android.content.Context
import android.graphics.drawable.Drawable
import com.abandonsearch.hazardgrid.R

class HazardMarkerFactory(private val context: Context) {

    private val markerSize = context.resources.getDimensionPixelSize(R.dimen.marker_size)

    private val defaultDrawable by lazy { createDrawable(isActive = false) }
    private val activeDrawable by lazy { createDrawable(isActive = true) }

    fun getDrawable(isActive: Boolean): Drawable = if (isActive) activeDrawable else defaultDrawable

    fun getClusterDrawable(clusterSize: Int): Drawable {
        return HazardMarkerDrawable(isCluster = true, clusterSize = clusterSize).apply {
            setBounds(0, 0, markerSize, markerSize)
        }
    }

    private fun createDrawable(isActive: Boolean): Drawable {
        return HazardMarkerDrawable(isActive = isActive).apply {
            setBounds(0, 0, markerSize, markerSize)
        }
    }
}
