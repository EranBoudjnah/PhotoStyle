package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import java.security.MessageDigest

private const val ID = "com.mitteloupe.photostyle.glide.transformation.GreyScaleTransformation"

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class GreyScaleTransformation(
    private val saturation: Float = 0f
) : BitmapTransformation() {
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

        return drawDesaturatedBitmap(toTransform, outputBitmap)
    }

    private fun drawDesaturatedBitmap(sourceBitmap: Bitmap, targetBitmap: Bitmap): Bitmap {
        val canvas = Canvas(targetBitmap)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, deSaturatePaint)
        return targetBitmap
    }

    override fun equals(other: Any?) = other is GreyScaleTransformation

    override fun hashCode() = ID.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID.toByteArray(CHARSET))
    }
}