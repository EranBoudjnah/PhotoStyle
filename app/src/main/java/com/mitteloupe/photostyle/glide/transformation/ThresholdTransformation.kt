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
import com.mitteloupe.photostyle.math.clamp
import androidx.annotation.FloatRange as AndroidFloatRange

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
@Suppress("EqualsOrHashCode")
class ThresholdTransformation(
    @AndroidFloatRange(from = -1.0, to = 1.0)
    private val threshold: Float = 0f,
    private val blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC,
    layerIdentifier: Int = 0,
    bitmapLayerPool: BitmapLayerPool?
) : LayeredBitmapTransformation(layerIdentifier, bitmapLayerPool) {
    override val id = "com.mitteloupe.photostyle.glide.transformation.ThresholdTransformation:$threshold"

    private val contrastMatrix by lazy {
        val m = 255f
        val t = -255f * (128f - 128f * threshold).clamp(0f, 255f)
        ColorMatrix().apply {
            setSaturation(0f)
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        m, 0.0f, 0.0f, 1.0f, t,
                        0.0f, m, 0.0f, 1.0f, t,
                        0.0f, 0.0f, m, 1.0f, t,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                    )
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

    override fun equals(other: Any?) = other is ThresholdTransformation &&
            threshold == other.threshold
}