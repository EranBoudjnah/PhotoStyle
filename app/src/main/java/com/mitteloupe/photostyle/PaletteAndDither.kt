package com.mitteloupe.photostyle

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.IntRange
import com.mitteloupe.photostyle.clustering.KMeans
import com.mitteloupe.photostyle.clustering.KMeans.TerminationCriteria
import com.mitteloupe.photostyle.graphics.BitmapVector3Converter
import com.mitteloupe.photostyle.graphics.RgbLabConverter
import com.mitteloupe.photostyle.graphics.RgbToPaletteConverter
import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3
import kotlin.system.measureNanoTime

/**
 * Created by Eran Boudjnah on 14/04/2019.
 */

class PaletteAndDither(
    sourceBitmap: Bitmap,
    private val kMeans: KMeans<Vector3<Double>>,
    private val rgbLabConverter: RgbLabConverter,
    private val bitmapVector3Converter: BitmapVector3Converter,
    private val rgbToPaletteConverter: RgbToPaletteConverter
) {
    private val imageWidth = sourceBitmap.width
    private val imageHeight = sourceBitmap.height
    private val totalPixels = imageWidth * imageHeight
    private val sourceRgbMatrix by lazy {
        bitmapVector3Converter.initialize(imageWidth, imageHeight)
        bitmapVector3Converter.bitmapToVector3Matrix(sourceBitmap)
    }

    fun processImage(targetBitmap: Bitmap, @IntRange(from = 2L, to = 255L) colorsCount: Int) {
        val sourceLabMatrix = rgbLabConverter.convertRgbMatrixToLab(sourceRgbMatrix)

        val colorsVector: Array<Vector3<Double>> = sourceLabMatrix.toVector()

        val labels = IntArray(totalPixels)
        val paletteLab = Array(colorsCount) { Vector3(0.0, 0.0, 0.0) }

        val benchmark1 = measureNanoTime {
            kMeans.execute(
                colorsVector,
                colorsCount,
                labels,
                TerminationCriteria.Iterations(100),
                paletteLab
            )
        }
        Log.d("Benchmark", "K-Means took ${benchmark1 / 1_000_000_000.0} seconds")

        // replace pixels by their corresponding image centers
//        val posterizedLabMatrix = Matrix<Vector3<Double>>(imageWidth, imageHeight)
//            .initialize { _, _ -> Vector3(0.0, 0.0, 0.0) }
//        for (i in 0 until sourceLabMatrix.width) {
//            for (j in 0 until sourceLabMatrix.height) {
//                for (k in 0 until 3) {
//                    posterizedLabMatrix[i, j][k] = palette[labels[j + sourceLabMatrix.width * i]][k]
//                }
//            }
//        }

        val benchmark2 = measureNanoTime {
            val paletteRgb = rgbLabConverter.convertLabArrayToRgb(paletteLab)
            val rgbMatrix = rgbToPaletteConverter.applyPalette(sourceRgbMatrix, paletteRgb)
            bitmapVector3Converter.vector3MatrixToBitmap(rgbMatrix, targetBitmap)
        }
        Log.d("Benchmark", "Applying palette took ${benchmark2 / 1_000_000_000.0} seconds")
    }
}

private inline fun <reified T : Any> Matrix<T>.toVector(): Array<T> {
    val result: Array<T?> = arrayOfNulls(width * height)
    var index = 0
    forEachIndexed { value, _, _ ->
        result[index] = value
        index++
    }
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

private inline fun <T : Any> Matrix<T>.forEachIndexed(function: (value: T, x: Int, y: Int) -> Unit) {
    for (i in 0 until width) {
        for (j in 0 until height) {
            function(this[i, j], i, j)
        }
    }
}

private operator fun Vector3<Int>.minus(vector3: Vector3<Int>) =
    Vector3(x - vector3.x, y - vector3.y, z - vector3.z)
