package com.mitteloupe.photostyle.graphics.dithering

import android.graphics.Bitmap
import com.mitteloupe.photostyle.graphics.BitmapVector3Converter
import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class FloydSteinbergConverter(
    private val bitmapVector3Converter: BitmapVector3Converter
) : RgbToPaletteConverter {

    override fun applyPalette(
        sourceBitmap: Bitmap,
        targetBitmap: Bitmap,
        palette: Array<Vector3<Int>>,
        imageToPalette: IntArray
    ) {
        initConverter(sourceBitmap)

        val sourceMatrix = bitmapToMatrix(sourceBitmap)
        val resImg = Matrix(sourceBitmap.width, sourceBitmap.height) { x, y ->
            val value = sourceMatrix[x, y]
            val newPixel = findClosestPaletteColor(value, palette)

            for (k in 0 until 3) {
                val quantError = value[k] - newPixel[k]
                value[k] = newPixel[k]
                if (x + 1 < sourceMatrix.width) {
                    sourceMatrix[x + 1, y][k] += (quantError * 7) shr 4
                }
                if (y + 1 < sourceMatrix.height) {
                    sourceMatrix[x, y + 1][k] += (quantError * 5) shr 4
                    if (x - 1 > 0) {
                        sourceMatrix[x - 1, y + 1][k] += (quantError * 3) shr 4
                        if (x + 1 < sourceMatrix.width) {
                            sourceMatrix[x + 1, y + 1][k] += quantError shr 4
                        }
                    }
                }
            }

            value
        }

        bitmapVector3Converter.vector3MatrixToBitmap(resImg, targetBitmap)
    }

    private fun initConverter(sourceBitmap: Bitmap) {
        bitmapVector3Converter.initialize(sourceBitmap.width, sourceBitmap.height)
    }

    private fun findClosestPaletteColor(
        color: Vector3<Int>,
        palette: Array<Vector3<Int>>
    ): Vector3<Int> {
        var minI = 0
        var minDistance = colorDistance(color, palette[0])

        palette.forEachIndexed { index, currentColor ->
            val distance = colorDistance(color, currentColor)
            if (distance < minDistance) {
                minDistance = distance
                minI = index
            }
        }

        return palette[minI]
    }

    private fun bitmapToMatrix(sourceBitmap: Bitmap) = bitmapVector3Converter.bitmapToVector3Matrix(sourceBitmap)

    private fun colorDistance(a: Vector3<Int>, b: Vector3<Int>): Double {
        val luma1 = 299 * a.x + 587 * a.y + a.z * 114
        val luma2 = 299 * b.x + 587 * b.y + b.z * 114
        val lumaDifference = (luma1 - luma2) / 1000
        val redDifference = a.x - b.x
        val greenDifference = a.y - b.y
        val blueDifference = a.z - b.z
        return (redDifference * redDifference * 0.299 +
                greenDifference * greenDifference * 0.587 +
                blueDifference * blueDifference * 0.114) * 0.75 +
                lumaDifference * lumaDifference
    }
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
