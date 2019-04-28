package com.mitteloupe.photostyle.glide.transformation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.renderscript.Allocation
import android.renderscript.RenderScript
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.glide.transformation.layered.BitmapLayerPool
import com.mitteloupe.photostyle.renderscript.ScriptC_outline

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
@Suppress("EqualsOrHashCode")
class OutlineTransformation(
    private val context: Context,
    private val blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC,
    layerIdentifier: Int = 0,
    bitmapLayerPool: BitmapLayerPool?
) : LayeredBitmapTransformation(layerIdentifier, bitmapLayerPool) {
    override val id = "com.mitteloupe.photostyle.glide.transformation.OutlineTransformation"

    private val renderScript by lazy { RenderScript.create(context) }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap =
        storeOrBlendOutputBitmap(toTransform, getOutlineBitmap(toTransform, pool), blendMode)

    private fun getOutlineBitmap(
        toTransform: Bitmap,
        pool: BitmapPool
    ): Bitmap {
        val inPixelsAllocation = Allocation.createFromBitmap(
            renderScript,
            toTransform,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )

        val outPixelsAllocation = Allocation.createTyped(
            renderScript,
            inPixelsAllocation.type
        )

        val outlineScript = ScriptC_outline(renderScript)

        outlineScript._width = toTransform.width
        outlineScript._height = toTransform.height
        outlineScript._image = inPixelsAllocation
        outlineScript.forEach_outline(inPixelsAllocation, outPixelsAllocation)

        val outputBitmap = pool.getEqualBitmap(toTransform)
        outPixelsAllocation.copyTo(outputBitmap)
        return outputBitmap
    }

    override fun equals(other: Any?) = other is OutlineTransformation
}