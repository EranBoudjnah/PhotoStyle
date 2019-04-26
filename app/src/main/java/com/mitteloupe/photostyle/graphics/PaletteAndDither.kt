package com.mitteloupe.photostyle.graphics

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.IntRange
import com.mitteloupe.photostyle.clustering.KMeans
import com.mitteloupe.photostyle.clustering.KMeans.TerminationCriteria
import com.mitteloupe.photostyle.graphics.dithering.RgbToPaletteConverter
import com.mitteloupe.photostyle.math.Vector3
import com.mitteloupe.photostyle.math.toVector
import kotlin.system.measureNanoTime

/**
 * Created by Eran Boudjnah on 14/04/2019.
 */

class PaletteAndDither(
    private val bitmapVector3Converter: BitmapVector3Converter,
    private val rgbToPaletteConverter: RgbToPaletteConverter
) {
    fun processImage(sourceBitmap: Bitmap, targetBitmap: Bitmap, paletteRgb: Array<Vector3<Int>>) {
        val totalPixels = sourceBitmap.width * sourceBitmap.height
        val labels = IntArray(totalPixels)
        rgbToPaletteConverter.applyPalette(sourceBitmap, targetBitmap, paletteRgb, labels)
    }

    fun processImage(
        sourceBitmap: Bitmap,
        targetBitmap: Bitmap,
        @IntRange(from = 2L, to = 255L) colorsCount: Int,
        kMeans: KMeans<Vector3<Double>>,
        rgbLabConverter: RgbLabConverter
    ) {
        val (paletteLab, labels) = calculatePalette(sourceBitmap, rgbLabConverter, colorsCount, kMeans)

        val benchmark = measureNanoTime {
            val paletteRgb = rgbLabConverter.convertLabArrayToRgb(paletteLab)
            rgbToPaletteConverter.applyPalette(sourceBitmap, targetBitmap, paletteRgb, labels)
        }
        Log.d("Benchmark", "Applying palette took ${benchmark / 1_000_000_000.0} seconds")
    }

    private fun calculatePalette(
        sourceBitmap: Bitmap,
        rgbLabConverter: RgbLabConverter,
        colorsCount: Int,
        kMeans: KMeans<Vector3<Double>>
    ): Pair<Array<Vector3<Double>>, IntArray> {
        bitmapVector3Converter.initialize(sourceBitmap.width, sourceBitmap.height)
        val sourceRgbMatrix = bitmapVector3Converter.bitmapToVector3Matrix(sourceBitmap)

        val sourceLabMatrix = rgbLabConverter.convertRgbMatrixToLab(sourceRgbMatrix)

        val colorsVector: Array<Vector3<Double>> = sourceLabMatrix.toVector()

        val totalPixels = sourceBitmap.width * sourceBitmap.height
        val labels = IntArray(totalPixels)
        val paletteLab = Array(colorsCount) { Vector3(0.0, 0.0, 0.0) }

        val benchmark = measureNanoTime {
            kMeans.execute(
                colorsVector,
                colorsCount,
                labels,
                TerminationCriteria.Iterations(100),
                paletteLab
            )
        }
        Log.d("Benchmark", "K-Means took ${benchmark / 1_000_000_000.0} seconds")
        return Pair(paletteLab, labels)
    }
}
