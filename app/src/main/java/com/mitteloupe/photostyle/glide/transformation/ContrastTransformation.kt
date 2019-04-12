package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.annotation.FloatRange as AndroidFloatRange
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import java.security.MessageDigest

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class ContrastTransformation(
    @AndroidFloatRange(from = -1.0)
    private val contrast: Float = 0f
) : BitmapTransformation() {
    private val id = "com.mitteloupe.photostyle.glide.transformation.ContrastTransformation:$contrast"

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

        return drawFilteredBitmap(toTransform, outputBitmap)
    }

    private fun drawFilteredBitmap(sourceBitmap: Bitmap, targetBitmap: Bitmap): Bitmap {
        val canvas = Canvas(targetBitmap)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, contrastPaint)
        return targetBitmap
    }

    override fun equals(other: Any?) = other is ContrastTransformation &&
            contrast == other.contrast

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }
}