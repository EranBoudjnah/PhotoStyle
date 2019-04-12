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
import com.mitteloupe.photostyle.math.clamp
import java.security.MessageDigest

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class BrightnessTransformation(
    @AndroidFloatRange(from = -1.0, to = 1.0)
    private val brightness: Float = 0f
) : BitmapTransformation() {
    private val id = "com.mitteloupe.photostyle.glide.transformation.BrightnessTransformation:$brightness"

    private val brightnessMatrix by lazy {
        val scaleFactor = brightness + 1f
        val offset = (255f * brightness).clamp(0f, 255f)
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

    private val brightnessColorMatrixColorFilter by lazy { ColorMatrixColorFilter(brightnessMatrix) }

    private val brightnessPaint by lazy {
        Paint().apply {
            colorFilter = brightnessColorMatrixColorFilter
        }
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val outputBitmap = pool.getEqualBitmap(toTransform)

        return drawFilteredBitmap(toTransform, outputBitmap)
    }

    private fun drawFilteredBitmap(sourceBitmap: Bitmap, targetBitmap: Bitmap): Bitmap {
        val canvas = Canvas(targetBitmap)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, brightnessPaint)
        return targetBitmap
    }

    override fun equals(other: Any?) = other is BrightnessTransformation &&
            brightness == other.brightness

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }
}