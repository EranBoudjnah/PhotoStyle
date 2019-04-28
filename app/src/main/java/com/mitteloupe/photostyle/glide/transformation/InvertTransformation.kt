package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.renderscript.Allocation
import android.renderscript.RenderScript
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.glide.transformation.layered.BitmapLayerPool
import com.mitteloupe.photostyle.renderscript.ScriptC_invert

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
@Suppress("EqualsOrHashCode")
class InvertTransformation(
    private val renderScript: RenderScript,
    private val blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC,
    layerIdentifier: Int = 0,
    bitmapLayerPool: BitmapLayerPool?
) : LayeredBitmapTransformation(layerIdentifier, bitmapLayerPool) {
    override val id = "com.mitteloupe.photostyle.glide.transformation.InvertTransformation"

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val inPixelsAllocation = Allocation.createFromBitmap(
            renderScript,
            toTransform,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )

        val outputBitmap = pool.getEqualBitmap(toTransform)
        val outPixelsAllocation = Allocation.createFromBitmap(
            renderScript,
            outputBitmap,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )

        val script = ScriptC_invert(renderScript)

        script.forEach_invert(inPixelsAllocation, outPixelsAllocation)
        outPixelsAllocation.copyTo(outputBitmap)

        return storeOrBlendOutputBitmap(toTransform, outputBitmap, blendMode)
    }

    override fun equals(other: Any?) = other is InvertTransformation
}