package com.mitteloupe.photostyle.glide.transformation.layered

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.util.SparseArray

/**
 * Created by Eran Boudjnah on 2019-04-28.
 */
class BitmapLayerPool {
    private val bitmapsWithBlends = SparseArray<BitmapWithBlend>()

    fun put(layerIdentifier: Int, bitmapWithBlend: BitmapWithBlend) {
        bitmapsWithBlends.put(layerIdentifier, bitmapWithBlend)
    }

    fun getAndRemove(layerIdentifier: Int): BitmapWithBlend {
        val bitmapWithBlend = bitmapsWithBlends.get(layerIdentifier)!!
        bitmapsWithBlends.remove(layerIdentifier)
        return bitmapWithBlend
    }
}

class BitmapWithBlend(
    val bitmap: Bitmap,
    val blendMode: PorterDuff.Mode
)
