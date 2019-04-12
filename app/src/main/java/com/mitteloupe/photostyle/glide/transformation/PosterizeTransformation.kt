package com.mitteloupe.photostyle.glide.transformation

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.RenderScript
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.renderscript.ScriptC_posterize
import java.security.MessageDigest
import androidx.annotation.IntRange as AndroidIntRange

private const val MINIMUM_POSTERIZE_VALUE = 2
private const val MAXIMUM_POSTERIZE_VALUE = 255

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class PosterizeTransformation(
    private val context: Context,
    @AndroidIntRange(
        from = MINIMUM_POSTERIZE_VALUE.toLong(),
        to = MAXIMUM_POSTERIZE_VALUE.toLong()
    ) private val posterizeValue: Int = MINIMUM_POSTERIZE_VALUE
) : BitmapTransformation() {
    private val id = "com.mitteloupe.photostyle.glide.transformation.PosterizeTransformation:$posterizeValue"

    init {
        if (posterizeValue !in MINIMUM_POSTERIZE_VALUE..MAXIMUM_POSTERIZE_VALUE) {
            throw IllegalArgumentException(
                "Cannot apply transformation when posterize value out of range [$MINIMUM_POSTERIZE_VALUE-$MAXIMUM_POSTERIZE_VALUE]: $posterizeValue"
            )
        }
    }

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

        val script = ScriptC_posterize(renderScript)
        script.invoke_setParams(posterizeValue)

        script.forEach_posterize(inPixelsAllocation, outPixelsAllocation)
        outPixelsAllocation.copyTo(outputBitmap)

        return outputBitmap
    }

    override fun equals(other: Any?) = other is PosterizeTransformation &&
            other.posterizeValue == posterizeValue

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }
}
