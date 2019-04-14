package com.mitteloupe.photostyle

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.annotation.IntRange
import com.mitteloupe.photostyle.clustering.KMeans
import com.mitteloupe.photostyle.clustering.KMeans.TerminationCriteria
import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.RgbLabConverter
import com.mitteloupe.photostyle.math.Vector3
import kotlin.system.measureNanoTime

/**
 * Created by Eran Boudjnah on 14/04/2019.
 */

class PaletteAndDither(
    sourceBitmap: Bitmap,
    private val kMeans: KMeans<Vector3<Double>>,
    private val rgbLabConverter: RgbLabConverter
) {
    private val imageWidth = sourceBitmap.width
    private val imageHeight = sourceBitmap.height
    private val totalPixels = imageWidth * imageHeight
    private val sourceRgbMatrix by lazy {
        val result = Matrix<Vector3<Int>>(imageWidth, imageHeight)
        result.apply {
            initialize { x, y ->
                val color = sourceBitmap.getPixel(x, y)
                Vector3(
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
                )
            }
        }
    }

    fun processImage(targetBitmap: Bitmap, @IntRange(from = 2L, to = 255L) colorsCount: Int) {
        val sourceLabMatrix = rgbLabConverter.convertRgbMatrixToLab(sourceRgbMatrix)

        val colorsVector: Array<Vector3<Double>> = sourceLabMatrix.toVector()

        val labels = IntArray(totalPixels)
        val palette = Array(colorsCount) { Vector3(0.0, 0.0, 0.0) }

        val benchmark = measureNanoTime {
            kMeans.execute(
                colorsVector,
                colorsCount,
                labels,
                TerminationCriteria.Iterations(100),
                palette
            )
        }
        Log.d("Benchmark", "K-Means took ${benchmark / 1_000_000_000.0} seconds")

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

//        val paletteInts = Vector<Vector3<Int>>(colorsCount)
//        palette.forEach { color ->
//            paletteInts.add(Vector3(color[0].toInt(), color[1].toInt(), color[2].toInt()))
//        }
        val floydSteinbergLabMatrix = floydSteinberg(sourceLabMatrix, palette)
        val floydSteinbergRGBMatrix = rgbLabConverter.convertLabMatrixToRgb(floydSteinbergLabMatrix)

        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val color = floydSteinbergRGBMatrix[x, y]
                val red = color[0]
                val green = color[1]
                val blue = color[2]
                targetBitmap.setPixel(x, y, Color.rgb(red, green, blue))
            }
        }
    }

    private fun floydSteinberg(
        imgOrig: Matrix<Vector3<Double>>,
        palette: Array<Vector3<Double>>
    ): Matrix<Vector3<Double>> {
        val img = Matrix<Vector3<Double>>(imgOrig.width, imgOrig.height)
            .initialize { x, y ->
                val pixelOriginal = imgOrig[x, y]
                Vector3(pixelOriginal[0], pixelOriginal[1], pixelOriginal[2])
            }
        val resImg = Matrix<Vector3<Double>>(imgOrig.width, imgOrig.height)
            .initialize { _, _ -> Vector3(0.0, 0.0, 0.0) }

        img.forEachIndexed { value, x, y ->
            val newPixel = findClosestPaletteColor(value, palette)
            resImg[x, y] = newPixel

            for (k in 0 until 3) {
                val quantError = value[k] - newPixel[k]
                if (x + 1 < img.width) {
                    img[x + 1, y][k] = (img[x + 1, y][k] + (7 * quantError) / 16) // .clamp(0, 255)
                }
                if (x - 1 > 0 && y + 1 < img.height) {
                    img[x - 1, y + 1][k] = (img[x - 1, y + 1][k] + (3 * quantError) / 16) // .clamp(0, 255)
                }
                if (y + 1 < img.height) {
                    img[x, y + 1][k] = (img[x, y + 1][k] + (5 * quantError) / 16) // .clamp(0, 255)
                }
                if (x + 1 < img.width && y + 1 < img.height) {
                    img[x + 1, y + 1][k] = (img[x + 1, y + 1][k] + (1 * quantError) / 16) // .clamp(0, 255)
                }
            }
        }
        return resImg
    }

    private fun findClosestPaletteColor(
        color: Vector3<Double>,
        palette: Array<Vector3<Double>>
    ): Vector3<Double> {
        var minI = 0
        var minDistance = vec3bDist(color, palette[0])

        palette.forEachIndexed { index, currentColor ->
            val distance = vec3bDist(color, currentColor)
            if (distance < minDistance) {
                minDistance = distance
                minI = index
            }
        }

        return palette[minI]
    }

    private fun vec3bDist(a: Vector3<Double>, b: Vector3<Double>) =
        Math.pow(a[0] - b[0], 2.0) + Math.pow(a[1] - b[1], 2.0) + Math.pow(a[2] - b[2], 2.0)
//    function perceptualDistance(labA, labB){
//        var deltaL = labA[0] - labB[0];
//        var deltaA = labA[1] - labB[1];
//        var deltaB = labA[2] - labB[2];
//        var c1 = Math.sqrt(labA[1] * labA[1] + labA[2] * labA[2]);
//        var c2 = Math.sqrt(labB[1] * labB[1] + labB[2] * labB[2]);
//        var deltaC = c1 - c2;
//        var deltaH = deltaA * deltaA + deltaB * deltaB - deltaC * deltaC;
//        deltaH = deltaH < 0 ? 0 : Math.sqrt(deltaH);
//        var sc = 1.0 + 0.045 * c1;
//        var sh = 1.0 + 0.015 * c1;
//        var deltaLKlsl = deltaL / (1.0);
//        var deltaCkcsc = deltaC / (sc);
//        var deltaHkhsh = deltaH / (sh);
//        var i = deltaLKlsl * deltaLKlsl + deltaCkcsc * deltaCkcsc + deltaHkhsh * deltaHkhsh;
//        return i < 0 ? 0 : Math.sqrt(i);
//    }
}

private inline fun <reified T : Any> Matrix<T>.toVector(): Array<T> {
    val result: Array<T?> = arrayOfNulls(width * height)
    var index = 0
    forEachIndexed { value, _, _ ->
        result[index] = value
        index++
    }
    return result as Array<T>
}

private fun <T : Any> Matrix<T>.forEachIndexed(function: (value: T, x: Int, y: Int) -> Unit) {
    for (i in 0 until width) {
        for (j in 0 until height) {
            function(this[i, j], i, j)
        }
    }
}

private operator fun Vector3<Int>.minus(vector3: Vector3<Int>) =
    Vector3(x - vector3.x, y - vector3.y, z - vector3.z)
