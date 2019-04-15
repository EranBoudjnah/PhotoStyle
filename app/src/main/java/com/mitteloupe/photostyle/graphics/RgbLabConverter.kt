package com.mitteloupe.photostyle.graphics

import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3
import com.mitteloupe.photostyle.math.clamp

/**
 * Created by Eran Boudjnah on 14/04/2019.
 */
private const val THIRD = 1.0 / 3.0
private const val OFFSET_VALUE = 16.0 / 116.0

class RgbLabConverter {
    private val colorScalePreCalculated by lazy {
        calculateColorScale()
    }

    fun convertRgbMatrixToLab(
        sourceRgbMatrix: Matrix<Vector3<Int>>
    ) = Matrix(
        sourceRgbMatrix.width,
        sourceRgbMatrix.height
    ) { x, y ->
        convertRgbVector3ToLab(sourceRgbMatrix[x, y])
    }

    fun convertLabMatrixToRgb(
        sourceLabMatrix: Matrix<Vector3<Double>>
    ) = Matrix(
        sourceLabMatrix.width,
        sourceLabMatrix.height
    ) { x, y ->
        convertLabVector3ToRgb(sourceLabMatrix[x, y])
    }

    fun convertLabArrayToRgb(paletteLab: Array<Vector3<Double>>) =
        Array(paletteLab.size) { index ->
            convertLabVector3ToRgb(paletteLab[index])
        }

    private fun convertRgbVector3ToLab(rgb: Vector3<Int>): Vector3<Double> {
        val redScaled = colorScalePreCalculated[rgb[0]]
        val greenScaled = colorScalePreCalculated[rgb[1]]
        val blueScaled = colorScalePreCalculated[rgb[2]]

        var x = (redScaled * 0.4124 + greenScaled * 0.3576 + blueScaled * 0.1805) / 0.95047
        var y = (redScaled * 0.2126 + greenScaled * 0.7152 + blueScaled * 0.0722)
        var z = (redScaled * 0.0193 + greenScaled * 0.1192 + blueScaled * 0.9505) / 1.08883

        x = if (x > 0.008856) Math.pow(
            x,
            THIRD
        ) else (7.787 * x) + OFFSET_VALUE
        y = if (y > 0.008856) Math.pow(
            y,
            THIRD
        ) else (7.787 * y) + OFFSET_VALUE
        z = if (z > 0.008856) Math.pow(
            z,
            THIRD
        ) else (7.787 * z) + OFFSET_VALUE

        return Vector3((116.0 * y) - 16.0, 500.0 * (x - y), 200.0 * (y - z))
    }

    private fun convertLabVector3ToRgb(lab: Vector3<Double>): Vector3<Int> {
        var y = (lab[0] + 16.0) / 116.0
        var x = lab[1] / 500.0 + y
        var z = y - lab[2] / 200.0

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

    private fun calculateColorScale(): DoubleArray {
        val result = DoubleArray(256)
        repeat(256) { value ->
            val valueScaled = value.toDouble() / 255.0
            result[value] =
                if (valueScaled > 0.04045) Math.pow((valueScaled + 0.055) / 1.055, 2.4) else valueScaled / 12.92
        }
        return result
    }
}
