package com.mitteloupe.photostyle.graphics.dithering

import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3
import com.mitteloupe.photostyle.math.forEachIndexed

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class FloydSteinbergConverter : RgbToPaletteConverter {
    override fun applyPalette(
        sourceImage: Matrix<Vector3<Int>>,
        palette: Array<Vector3<Int>>,
        imageToPalette: IntArray
    ): Matrix<Vector3<Int>> {
        val img = Matrix(sourceImage.width, sourceImage.height)
        { x, y ->
            val pixelOriginal = sourceImage[x, y]
            Vector3(pixelOriginal[0], pixelOriginal[1], pixelOriginal[2])
        }
        val resImg = Matrix(sourceImage.width, sourceImage.height) { x, y ->
            val value = img[x, y]

            val newPixel = findClosestPaletteColor(value, palette)

            for (k in 0 until 3) {
                val quantError = value[k] - newPixel[k]
                if (x + 1 < img.width) {
                    img[x + 1, y][k] += (7 * quantError) / 16
                }
                if (x - 1 > 0 && y + 1 < img.height) {
                    img[x - 1, y + 1][k] += (3 * quantError) / 16
                }
                if (y + 1 < img.height) {
                    img[x, y + 1][k] += (5 * quantError) / 16
                }
                if (x + 1 < img.width && y + 1 < img.height) {
                    img[x + 1, y + 1][k] += (1 * quantError) / 16
                }
            }

            Vector3(0, 0, 0)
        }

        img.forEachIndexed { value, x, y ->
            val newPixel = findClosestPaletteColor(value, palette)
            resImg[x, y] = newPixel
        }

        return resImg
    }

    private fun findClosestPaletteColor(
        color: Vector3<Int>,
        palette: Array<Vector3<Int>>
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

    private fun vec3bDist(a: Vector3<Int>, b: Vector3<Int>): Double {
        val luma1 = (a.x * 299 + a.y * 587 + a.z * 114) / (255.0 * 1000)
        val luma2 = (b.x * 299 + b.y * 587 + b.z * 114) / (255.0 * 1000)
        val lumadiff = luma1 - luma2
        val diffR = (a.x - b.x) / 255.0
        val diffG = (a.y - b.y) / 255.0
        val diffB = (a.z - b.z) / 255.0
        return (diffR * diffR * 0.299 + diffG * diffG * 0.587 + diffB * diffB * 0.114) * 0.75 + lumadiff * lumadiff

//        val x = a.x - b.x
//        val y = a.y - b.y
//        val z = a.z - b.z
//        return x * x + y * y + z * z
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
