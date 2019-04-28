package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.mitteloupe.photostyle.glide.extension.getBitmapWithSize
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.glide.transformation.layered.BitmapLayerPool
import java.security.MessageDigest

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class PixelationTransformation(
    private val horizontalPixels: Int = KEEP_ASPECT_RATIO,
    private val verticalPixels: Int = KEEP_ASPECT_RATIO,
    private val blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC,
    layerIdentifier: Int = 0,
    bitmapLayerPool: BitmapLayerPool?
) : LayeredBitmapTransformation(layerIdentifier, bitmapLayerPool) {
    override val id =
        "com.mitteloupe.photostyle.glide.transformation.PixelationTransformation:$horizontalPixels:$verticalPixels"

    private val scaleMatrix = Matrix()

    private val smoothPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG + Paint.FILTER_BITMAP_FLAG) }

    init {
        if (horizontalPixels == KEEP_ASPECT_RATIO && verticalPixels == KEEP_ASPECT_RATIO) {
            throw IllegalArgumentException(
                "Cannot apply transformation when both target width and target height equal KEEP_ASPECT_RATIO"
            )
        }
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val scaleDownWidth = if (horizontalPixels != KEEP_ASPECT_RATIO) {
            horizontalPixels
        } else {
            (verticalPixels * toTransform.width) / toTransform.height
        }
        val scaleDownHeight = if (verticalPixels != KEEP_ASPECT_RATIO) {
            verticalPixels
        } else {
            (horizontalPixels * toTransform.height) / toTransform.width
        }
        val scaledDownBitmap = pool.getBitmapWithSize(scaleDownWidth, scaleDownHeight, toTransform.config)
        drawScaledBitmap(toTransform, scaledDownBitmap, smoothPaint)

        val outputBitmap = pool.getEqualBitmap(toTransform)
        drawScaledBitmap(scaledDownBitmap, outputBitmap, null)

        return storeOrBlendOutputBitmap(toTransform, outputBitmap, blendMode)
    }

    private fun drawScaledBitmap(sourceBitmap: Bitmap, targetBitmap: Bitmap, paint: Paint?) {
        val canvas = Canvas(targetBitmap)
        canvas.matrix = scaleMatrix.apply {
            setScale(
                targetBitmap.width.toFloat() / sourceBitmap.width.toFloat(),
                targetBitmap.height.toFloat() / sourceBitmap.height.toFloat()
            )
        }
        canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)
    }

    override fun equals(other: Any?) = other is PixelationTransformation &&
            other.horizontalPixels == horizontalPixels &&
            other.verticalPixels == verticalPixels

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }

    companion object {
        const val KEEP_ASPECT_RATIO = -1
    }
}