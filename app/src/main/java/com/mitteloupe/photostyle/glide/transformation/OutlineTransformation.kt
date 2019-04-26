package com.mitteloupe.photostyle.glide.transformation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.renderscript.Allocation
import android.renderscript.RenderScript
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.renderscript.ScriptC_invert
import com.mitteloupe.photostyle.renderscript.ScriptC_outline
import java.security.MessageDigest


/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class OutlineTransformation(
    private val context: Context,
    private val mode: Mode
) : BitmapTransformation() {
    private val id = "com.mitteloupe.photostyle.glide.transformation.OutlineTransformation"

    private val renderScript by lazy { RenderScript.create(context) }
    private val paint by lazy {
        Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        return when (mode) {
            Mode.OVERLAY -> {
                val canvas = Canvas(toTransform)

                canvas.drawBitmap(getOutlineBitmap(toTransform, true, pool), Matrix(), paint)

                toTransform
            }
            Mode.BLACK_OUTLINES -> getOutlineBitmap(toTransform, true, pool)
            Mode.WHITE_OUTLINES -> getOutlineBitmap(toTransform, false, pool)
        }
    }

    private fun getOutlineBitmap(
        toTransform: Bitmap,
        isInverted: Boolean,
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

        if (isInverted) {
            val invertScript = ScriptC_invert(renderScript)

            invertScript.forEach_invert(outPixelsAllocation, outPixelsAllocation)
        }

        val outputBitmap = pool.getEqualBitmap(toTransform)
        outPixelsAllocation.copyTo(outputBitmap)
        return outputBitmap
    }

    override fun equals(other: Any?) = other is OutlineTransformation

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }

    enum class Mode {
        WHITE_OUTLINES,
        BLACK_OUTLINES,
        OVERLAY
    }
}