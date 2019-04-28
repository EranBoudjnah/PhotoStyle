package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.renderscript.RenderScript
import android.util.Log
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.mitteloupe.photostyle.clustering.KMeans
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.glide.transformation.layered.BitmapLayerPool
import com.mitteloupe.photostyle.graphics.BitmapVector3Converter
import com.mitteloupe.photostyle.graphics.PaletteAndDither
import com.mitteloupe.photostyle.graphics.RgbLabConverter
import com.mitteloupe.photostyle.graphics.dithering.OrderedDitheringConverter
import com.mitteloupe.photostyle.graphics.dithering.Pattern
import com.mitteloupe.photostyle.graphics.dithering.RgbToPaletteConverter
import com.mitteloupe.photostyle.math.Vector3Arithmetic
import kotlin.system.measureNanoTime

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
@Suppress("EqualsOrHashCode")
class ColorCountTransformation(
    private val renderScript: RenderScript,
    private val colorCount: Int = 16,
    private val bitmapVector3Converter: BitmapVector3Converter = BitmapVector3Converter(),
    private val rgbToPaletteConverter: RgbToPaletteConverter = OrderedDitheringConverter(
        renderScript,
        Pattern.PATTERN_BAYER
    ),
    private val blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC,
    layerIdentifier: Int = 0,
    bitmapLayerPool: BitmapLayerPool?
) : LayeredBitmapTransformation(layerIdentifier, bitmapLayerPool) {
    override val id = "com.mitteloupe.photostyle.glide.transformation.ColorCountTransformation:$colorCount"

    private val arithmetic = Vector3Arithmetic()

    private val kMeans by lazy { KMeans(arithmetic) }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val outputBitmap = pool.getEqualBitmap(toTransform)

        val benchmark = measureNanoTime {
            PaletteAndDither(
                bitmapVector3Converter,
                rgbToPaletteConverter
            ).processImage(toTransform, outputBitmap, colorCount, kMeans, RgbLabConverter())
        }
        Log.d("Benchmark", "Process image took ${benchmark / 1_000_000_000.0} seconds")

        return storeOrBlendOutputBitmap(toTransform, outputBitmap, blendMode)
    }

    override fun equals(other: Any?) = other is ColorCountTransformation &&
            colorCount == other.colorCount
}