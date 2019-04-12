package com.mitteloupe.photostyle.glide.transformation

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.RenderScript
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.renderscript.ScriptC_invert
import java.security.MessageDigest

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class InvertTransformation(
    private val context: Context
) : BitmapTransformation() {
    private val id = "com.mitteloupe.photostyle.glide.transformation.InvertTransformation"

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val renderScript = RenderScript.create(context)
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

        return outputBitmap
    }

    override fun equals(other: Any?) = other is InvertTransformation

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }
}