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

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class FilterTransformation(
    private val filter: Filter
) : BitmapTransformation() {
    private val id = "com.mitteloupe.photostyle.glide.transformation.FilterTransformation:$filter"

    private val colorMatrixColorFilter by lazy {
        ColorMatrixColorFilter(filter.colorMatrix)
    }

    private val colorFilterPaint by lazy {
        Paint().apply {
            colorFilter = colorMatrixColorFilter
        }
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val outputBitmap = pool.getEqualBitmap(toTransform)

        return drawColorFilteredBitmap(toTransform, outputBitmap)
    }

    private fun drawColorFilteredBitmap(sourceBitmap: Bitmap, targetBitmap: Bitmap): Bitmap {
        val canvas = Canvas(targetBitmap)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, colorFilterPaint)
        return targetBitmap
    }

    override fun equals(other: Any?) = other is FilterTransformation

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) =
        messageDigest.update(id.toByteArray(CHARSET))
}

enum class Filter(val colorMatrix: ColorMatrix) {
    BROWNIE(
        ColorMatrix(
            floatArrayOf(
                0.6f, 0.346f, -0.271f, 0.0f, 47.432f,
                -0.038f, 0.861f, 0.151f, 0.0f, -36.968f,
                0.241f, -0.074f, 0.450f, 0.0f, -7.562f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )
    ),
    INVERT(
        ColorMatrix(
            arrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ).toFloatArray()
        )
    ),
    LSD(
        ColorMatrix(
            floatArrayOf(
                2.0f, -0.4f, 0.5f, 0.0f, 0.0f,
                -0.5f, 2.0f, -0.4f, 0.0f, 0.0f,
                -0.4f, -0.5f, 3.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )
    ),
    POLAROID(
        ColorMatrix(
            floatArrayOf(
                1.438f, -0.062f, -0.062f, 0.0f, 0.0f,
                -0.122f, 1.378f, -0.122f, 0.0f, 0.0f,
                -0.016f, -0.016f, 1.483f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )
    ),
    SEPIA_V1(
        ColorMatrix(
            floatArrayOf(
                0.189f, 0.769f, 0.393f, 0f, 0f,
                0.168f, 0.686f, 0.349f, 0f, 0f,
                0.131f, 0.534f, 0.272f, 0f, 0f,
                0.000f, 0.000f, 0.000f, 1f, 0f
            )
        )
    ),
    SEPIA_V2(
        ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0.0f, 0.0f,
                0.349f, 0.686f, 0.168f, 0.0f, 0.0f,
                0.272f, 0.534f, 0.131f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )
    ),
    VINTAGE_PINHOLE(
        ColorMatrix(
            floatArrayOf(
                0.628f, 0.320f, -0.04f, 0.0f, 9.651f,
                0.026f, 0.644f, 0.033f, 0.0f, 7.463f,
                0.047f, -0.085f, 0.524f, 0.0f, 5.159f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )
    )
}