package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.glide.transformation.layered.BitmapLayerPool

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
@Suppress("EqualsOrHashCode")
class GrayscaleTransformation(
    private val saturation: Float = 0f,
    private val blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC,
    layerIdentifier: Int = 0,
    bitmapLayerPool: BitmapLayerPool?
) : LayeredBitmapTransformation(layerIdentifier, bitmapLayerPool) {
    override val id = "com.mitteloupe.photostyle.glide.transformation.GreyScaleTransformation:$saturation"

    private val saturationMatrix by lazy {
        ColorMatrix().apply {
            setSaturation(saturation)
        }
    }

    private val deSaturateColorMatrixColorFilter by lazy { ColorMatrixColorFilter(saturationMatrix) }

    private val deSaturatePaint by lazy {
        Paint().apply {
            colorFilter = deSaturateColorMatrixColorFilter
        }
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val outputBitmap = pool.getEqualBitmap(toTransform)

        return storeOrBlendOutputBitmap(toTransform, drawDesaturatedBitmap(toTransform, outputBitmap), blendMode)
    }

    private fun drawDesaturatedBitmap(sourceBitmap: Bitmap, targetBitmap: Bitmap): Bitmap {
        val canvas = Canvas(targetBitmap)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, deSaturatePaint)
        return targetBitmap
    }

    override fun equals(other: Any?) = other is GrayscaleTransformation &&
            saturation == other.saturation
}