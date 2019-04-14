package com.mitteloupe.photostyle

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.IntRange
import com.mitteloupe.photostyle.clustering.KMeans
import com.mitteloupe.photostyle.clustering.KMeans.TerminationCriteria
import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3
import com.mitteloupe.photostyle.math.clamp
import java.util.Vector

/**
 * Created by Eran Boudjnah on 14/04/2019.
 */

class PaletteAndDither(
    sourceBitmap: Bitmap,
    private val kMeans: KMeans<Vector3<Double>>
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
        val colorsCount = 4
        val sourceLabMatrix = convertRgbMatrixToLab(sourceRgbMatrix)

        val colorsVector: Vector<Vector3<Double>> = sourceLabMatrix.toVector()

        val labels = Vector<Int>(totalPixels)
        val palette = Vector<Vector3<Double>>(colorsCount)
        kMeans.execute(
            colorsVector,
            colorsCount,
            labels,
            TerminationCriteria.Iterations(100),
            palette
        )

        // replace pixels by there corresponding image centers
//        val posterizedLabMatrix = Matrix<Vector3<Double>>(imageWidth, imageHeight)
//            .initialize { _, _ -> Vector3(0.0, 0.0, 0.0) }
//        for (i in 0 until sourceLabMatrix.width) {
//            for (j in 0 until sourceLabMatrix.height) {
//                for (k in 0 until 3) {
//                    posterizedLabMatrix[i, j][k] = palette[labels[j + sourceLabMatrix.width * i]][k]
//                }
//            }
//        }

        val paletteInts = Vector<Vector3<Int>>(colorsCount)
        palette.forEach { color ->
            paletteInts.add(Vector3(color[0].toInt(), color[1].toInt(), color[2].toInt()))
        }
        val floydSteinbergLabMatrix = floydSteinberg(sourceLabMatrix, paletteInts)
        val floydSteinbergRGBMatrix = convertLabMatrixToRgb(floydSteinbergLabMatrix)

        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val red = floydSteinbergRGBMatrix[x, y][0]
                val green = floydSteinbergRGBMatrix[x, y][1]
                val blue = floydSteinbergRGBMatrix[x, y][2]
                targetBitmap.setPixel(x, y, Color.rgb(red, green, blue))
            }
        }
    }

    private fun convertRgbMatrixToLab(
        sourceRgbMatrix: Matrix<Vector3<Int>>
    ) = Matrix<Vector3<Double>>(
        sourceRgbMatrix.width,
        sourceRgbMatrix.height
    ).initialize { x, y ->
        convertRgbVector3ToLab(sourceRgbMatrix[x, y])
    }

    private fun convertRgbVector3ToLab(rgb: Vector3<Int>): Vector3<Double> {
        var r = rgb[0].toDouble() / 255.0
        var g = rgb[1].toDouble() / 255.0
        var b = rgb[2].toDouble() / 255.0

        r = if (r > 0.04045) Math.pow((r + 0.055) / 1.055, 2.4) else r / 12.92
        g = if (g > 0.04045) Math.pow((g + 0.055) / 1.055, 2.4) else g / 12.92
        b = if (b > 0.04045) Math.pow((b + 0.055) / 1.055, 2.4) else b / 12.92

        var x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
        var y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.00000
        var z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883

        x = if (x > 0.008856) Math.pow(x, 1.0 / 3.0) else (7.787 * x) + 16 / 116
        y = if (y > 0.008856) Math.pow(y, 1.0 / 3.0) else (7.787 * y) + 16 / 116
        z = if (z > 0.008856) Math.pow(z, 1.0 / 3.0) else (7.787 * z) + 16 / 116

        return Vector3((116.0 * y) - 16.0, 500.0 * (x - y), 200.0 * (y - z))
    }

    private fun convertLabMatrixToRgb(
        sourceLabMatrix: Matrix<Vector3<Int>>
    ) = Matrix<Vector3<Int>>(
        sourceLabMatrix.width,
        sourceLabMatrix.height
    ).initialize { x, y ->
        convertLabVector3ToRgb(sourceLabMatrix[x, y])
    }

    private fun convertLabVector3ToRgb(lab: Vector3<Int>): Vector3<Int> {
        var y = (lab[0].toDouble() + 16.0) / 116.0
        var x = lab[1].toDouble() / 500.0 + y
        var z = y - lab[2].toDouble() / 200.0

        x = 0.95047 * (if (x * x * x > 0.008856) x * x * x else (x - 16.0 / 116.0) / 7.787)
        y = 1.00000 * (if (y * y * y > 0.008856) y * y * y else (y - 16.0 / 116.0) / 7.787)
        z = 1.08883 * (if (z * z * z > 0.008856) z * z * z else (z - 16.0 / 116.0) / 7.787)

        var r = x * 3.2406 + y * -1.5372 + z * -0.4986
        var g = x * -0.9689 + y * 1.8758 + z * 0.0415
        var b = x * 0.0557 + y * -0.2040 + z * 1.0570

        r = if (r > 0.0031308) 1.055 * Math.pow(r, 1 / 2.4) - 0.055 else 12.92 * r
        g = if (g > 0.0031308) 1.055 * Math.pow(g, 1 / 2.4) - 0.055 else 12.92 * g
        b = if (b > 0.0031308) 1.055 * Math.pow(b, 1 / 2.4) - 0.055 else 12.92 * b

        return Vector3(
            (r.clamp(0.0, 1.0) * 255.0).toInt(),
            (g.clamp(0.0, 1.0) * 255.0).toInt(),
            (b.clamp(0.0, 1.0) * 255.0).toInt()
        )
    }

    private fun floydSteinberg(
        imgOrig: Matrix<Vector3<Double>>,
        palette: Vector<Vector3<Int>>
    ): Matrix<Vector3<Int>> {
        val img = Matrix<Vector3<Int>>(imgOrig.width, imgOrig.height)
            .initialize { x, y ->
                val pixelOriginal = imgOrig[x, y]
                Vector3(pixelOriginal[0].toInt(), pixelOriginal[1].toInt(), pixelOriginal[2].toInt())
            }
        val resImg = Matrix<Vector3<Int>>(imgOrig.width, imgOrig.height)
            .initialize { _, _ -> Vector3(0, 0, 0) }

        img.forEachIndexed { value, x, y ->
            val newPixel = findClosestPaletteColor(value, palette)
            resImg[x, y] = newPixel

            for (k in 0 until 3) {
                val quantError = value[k] - newPixel[k]
                if (x + 1 < img.width) {
                    img[x + 1, y][k] = (img[x + 1, y][k] + (7 * quantError) / 16).clamp(0, 255)
                }
                if (x - 1 > 0 && y + 1 < img.height) {
                    img[x - 1, y + 1][k] = (img[x - 1, y + 1][k] + (3 * quantError) / 16).clamp(0, 255)
                }
                if (y + 1 < img.height) {
                    img[x, y + 1][k] = (img[x, y + 1][k] + (5 * quantError) / 16).clamp(0, 255)
                }
                if (x + 1 < img.width && y + 1 < img.height) {
                    img[x + 1, y + 1][k] = (img[x + 1, y + 1][k] + (1 * quantError) / 16).clamp(0, 255)
                }
            }
        }
        return resImg
    }

    private fun findClosestPaletteColor(
        color: Vector3<Int>,
        palette: Vector<Vector3<Int>>
    ): Vector3<Int> {
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

    private fun vec3bDist(a: Vector3<Int>, b: Vector3<Int>) =
        Math.sqrt(
            Math.pow((a[0] - b[0]).toDouble(), 2.0) +
                    Math.pow((a[1] - b[1]).toDouble(), 2.0) +
                    Math.pow((a[2] - b[2]).toDouble(), 2.0)
        )
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

private fun <T : Any> Matrix<T>.toVector(): Vector<T> {
    val result = Vector<T>(width * height)
    forEachIndexed { value, _, _ -> result.add(value) }
    return result
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
