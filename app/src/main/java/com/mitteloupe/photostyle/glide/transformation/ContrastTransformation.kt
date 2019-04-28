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
import androidx.annotation.FloatRange as AndroidFloatRange

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
@Suppress("EqualsOrHashCode")
class ContrastTransformation(
    @AndroidFloatRange(from = -1.0)
    private val contrast: Float = 0f,
    private val blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC,
    layerIdentifier: Int = 0,
    bitmapLayerPool: BitmapLayerPool?
) : LayeredBitmapTransformation(layerIdentifier, bitmapLayerPool) {
    override val id = "com.mitteloupe.photostyle.glide.transformation.ContrastTransformation:$contrast"

    private val contrastMatrix by lazy {
        val scaleFactor = Math.max(0f, contrast + 1f)
        val offset = -128f * (scaleFactor - 1f)
        ColorMatrix().apply {
            set(
                floatArrayOf(
                    scaleFactor, 0.0f, 0.0f, 0.0f, offset,
                    0.0f, scaleFactor, 0.0f, 0.0f, offset,
                    0.0f, 0.0f, scaleFactor, 0.0f, offset,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                )
            )
        }
    }

    private val contrastColorMatrixColorFilter by lazy { ColorMatrixColorFilter(contrastMatrix) }

    private val contrastPaint by lazy {
        Paint().apply {
            colorFilter = contrastColorMatrixColorFilter
        }
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val outputBitmap = pool.getEqualBitmap(toTransform)

        return storeOrBlendOutputBitmap(toTransform, drawFilteredBitmap(toTransform, outputBitmap), blendMode)
    }

    private fun drawFilteredBitmap(sourceBitmap: Bitmap, targetBitmap: Bitmap): Bitmap {
        val canvas = Canvas(targetBitmap)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, contrastPaint)
        return targetBitmap
    }

    override fun equals(other: Any?) = other is ContrastTransformation &&
            contrast == other.contrast
}