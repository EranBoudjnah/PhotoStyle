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
import android.util.Log
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.graphics.BitmapVector3Converter
import com.mitteloupe.photostyle.graphics.SobelEdgeDetection
import com.mitteloupe.photostyle.renderscript.ScriptC_invert
import java.security.MessageDigest
import kotlin.system.measureNanoTime


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
        val outputBitmap = pool.getEqualBitmap(toTransform)

        val benchmark = measureNanoTime {
            SobelEdgeDetection(
                BitmapVector3Converter()
            ).processImage(toTransform, outputBitmap)
        }
        Log.d("Benchmark", "Process image took ${benchmark / 1_000_000_000.0} seconds")

        return when (mode) {
            Mode.OVERLAY -> {
                val canvas = Canvas(toTransform)

                canvas.drawBitmap(getInvertedBitmap(outputBitmap, pool), Matrix(), paint)

                toTransform
            }
            Mode.BLACK_OUTLINES -> getInvertedBitmap(outputBitmap, pool)
            Mode.WHITE_OUTLINES -> outputBitmap
        }
    }

    private fun getInvertedBitmap(
        toTransform: Bitmap,
        pool: BitmapPool
    ): Bitmap {
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