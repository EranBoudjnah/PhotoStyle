package com.mitteloupe.photostyle.graphics

import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3
import com.mitteloupe.photostyle.math.clamp

private val bayerMatrix = arrayOf(
    0.0, 48.0, 12.0, 60.0, 3.0, 51.0, 15.0, 63.0,
    32.0, 16.0, 44.0, 28.0, 35.0, 19.0, 47.0, 31.0,
    8.0, 56.0, 4.0, 52.0, 11.0, 59.0, 7.0, 55.0,
    40.0, 24.0, 36.0, 20.0, 43.0, 27.0, 39.0, 23.0,
    2.0, 50.0, 14.0, 62.0, 1.0, 49.0, 13.0, 61.0,
    34.0, 18.0, 46.0, 30.0, 33.0, 17.0, 45.0, 29.0,
    10.0, 58.0, 6.0, 54.0, 9.0, 57.0, 5.0, 53.0,
    42.0, 26.0, 38.0, 22.0, 41.0, 25.0, 37.0, 21.0
)

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class BayerConverter : RgbToPaletteConverter {
    override fun applyPalette(imgOrig: Matrix<Vector3<Int>>, palette: Array<Vector3<Int>>): Matrix<Vector3<Int>> {
        val img = Matrix<Vector3<Int>>(imgOrig.width, imgOrig.height)
            .initialize { x, y ->
                val pixelOriginal = imgOrig[x, y]
                Vector3(pixelOriginal[0], pixelOriginal[1], pixelOriginal[2])
            }
        val resImg = Matrix<Vector3<Int>>(imgOrig.width, imgOrig.height)
            .initialize { _, _ -> Vector3(0, 0, 0) }

        val scale = 256.0 / palette.size.toDouble()

        img.forEachIndexed { color, x, y ->
            val ditherMapValue = bayerMatrix[x % 8 + (y % 8) * 8] / 64.0 - 0.5
            color.x = (color.x.toDouble() + ditherMapValue * scale).toInt().clamp(0, 255)
            color.y = (color.y.toDouble() + ditherMapValue * scale).toInt().clamp(0, 255)
            color.z = (color.z.toDouble() + ditherMapValue * scale).toInt().clamp(0, 255)
            val colorIndex = findClosestPaletteColor(color, palette)
            resImg[x, y] = colorIndex
        }

        return resImg
    }

    private fun findClosestPaletteColor(
        color: Vector3<Int>,
        palette: Array<Vector3<Int>>
    ): Vector3<Int> {
        var minColor = palette[0]
        var minDistance = colorCCIR601Distance(color, palette[0])

        palette.forEachIndexed { index, currentColor ->
            val distance = colorCCIR601Distance(color, currentColor)
            if (distance < minDistance) {
                minDistance = distance
                minColor = currentColor
            }
        }

        return minColor
    }

    private fun colorCCIR601Distance(a: Vector3<Int>, b: Vector3<Int>): Double {
        val luma1 = (a.x * 299 + a.y * 587 + a.z * 114) / (255.0 * 1000)
        val luma2 = (b.x * 299 + b.y * 587 + b.z * 114) / (255.0 * 1000)
        val lumaDifference = luma1 - luma2
        val diffR = (a.x - b.x) / 255.0
        val diffG = (a.y - b.y) / 255.0
        val diffB = (a.z - b.z) / 255.0
        return (diffR * diffR * 0.299 + diffG * diffG * 0.587 + diffB * diffB * 0.114) * 0.75 + lumaDifference * lumaDifference
    }
}

private inline fun <T : Any> Matrix<T>.forEachIndexed(function: (value: T, x: Int, y: Int) -> Unit) {
    for (i in 0 until width) {
        for (j in 0 until height) {
            function(this[i, j], i, j)
        }
    }
}
