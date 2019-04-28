package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.graphics.PorterDuff
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.glide.transformation.layered.BitmapLayerPool
import com.mitteloupe.photostyle.glide.transformation.layered.BitmapWithBlend
import java.security.MessageDigest

private const val NO_IDENTIFIER = -1

/**
 * Created by Eran Boudjnah on 2019-04-28.
 */
abstract class LayeredBitmapTransformation(
    private val layerIdentifier: Int = NO_IDENTIFIER,
    private val bitmapLayerPool: BitmapLayerPool?
) : BitmapTransformation() {
    protected abstract val id: String

    fun storeOrBlendOutputBitmap(toTransform: Bitmap, output: Bitmap, blendMode: PorterDuff.Mode): Bitmap =
        if (bitmapLayerPool != null) {
            assert(layerIdentifier != NO_IDENTIFIER)
            bitmapLayerPool.put(layerIdentifier, BitmapWithBlend(output, blendMode))
            toTransform
        } else {
            output
        }

    abstract override fun equals(other: Any?): Boolean

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }
}