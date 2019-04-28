package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuffXfermode
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.glide.transformation.layered.BitmapLayerPool
import com.mitteloupe.photostyle.glide.transformation.layered.BitmapWithBlend
import java.security.MessageDigest

/**
 * Created by Eran Boudjnah on 2019-04-28.
 */
class LayeredTransformation(
    private val bitmapLayerPool: BitmapLayerPool,
    private val layerIdentifier: Int
) : BitmapTransformation() {
    private val id = "com.mitteloupe.photostyle.glide.transformation.LayeredTransformation:$layerIdentifier"

    private val paint by lazy { Paint() }
    private val matrix = Matrix()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val outputBitmapWithBlend = bitmapLayerPool.getAndRemove(layerIdentifier)

        blendOutputBitmapToSource(outputBitmapWithBlend, toTransform)

        return toTransform
    }

    private fun blendOutputBitmapToSource(
        outputBitmapWithBlend: BitmapWithBlend,
        toTransform: Bitmap
    ) {
        paint.xfermode = PorterDuffXfermode(outputBitmapWithBlend.blendMode)

        val canvas = Canvas(toTransform)

        canvas.drawBitmap(outputBitmapWithBlend.bitmap, matrix, paint)

        outputBitmapWithBlend.bitmap.recycle()
    }

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?) = other is LayeredTransformation &&
            layerIdentifier == other.layerIdentifier
}