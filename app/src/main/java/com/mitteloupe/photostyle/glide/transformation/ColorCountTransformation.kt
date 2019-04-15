package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.clustering.KMeans
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.graphics.BitmapVector3Converter
import com.mitteloupe.photostyle.graphics.PaletteAndDither
import com.mitteloupe.photostyle.graphics.RgbLabConverter
import com.mitteloupe.photostyle.graphics.dithering.BayerConverter
import com.mitteloupe.photostyle.math.Vector3Arithmetic
import java.security.MessageDigest
import kotlin.system.measureNanoTime

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class ColorCountTransformation(
    private val colorCount: Int = 16
) : BitmapTransformation() {
    private val id = "com.mitteloupe.photostyle.glide.transformation.ColorCountTransformation:$colorCount"

    private val arithmetic = Vector3Arithmetic()

    private val kMeans by lazy { KMeans(arithmetic) }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val outputBitmap = pool.getEqualBitmap(toTransform)

        val benchmark = measureNanoTime {
            PaletteAndDither(
                toTransform,
                kMeans,
                RgbLabConverter(),
                BitmapVector3Converter(),
                BayerConverter()
            ).processImage(outputBitmap, colorCount)
        }
        Log.d("Benchmark", "Process image took ${benchmark / 1_000_000_000.0} seconds")

        return outputBitmap
    }

    override fun equals(other: Any?) = other is ColorCountTransformation &&
            colorCount == other.colorCount

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }
}