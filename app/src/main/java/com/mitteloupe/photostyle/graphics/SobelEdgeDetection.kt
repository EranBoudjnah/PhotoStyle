package com.mitteloupe.photostyle.graphics

import android.graphics.Bitmap
import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class SobelEdgeDetection(
    private val bitmapVector3Converter: BitmapVector3Converter
) {

    fun processImage(sourceBitmap: Bitmap, targetBitmap: Bitmap) {
        bitmapVector3Converter.initialize(sourceBitmap.width, sourceBitmap.height)
        val sourceRgbMatrix = bitmapVector3Converter.bitmapToVector3Matrix(sourceBitmap)

        val widthRange = 1 until sourceBitmap.width - 1
        val heightRange = 1 until sourceBitmap.height - 1
        val resImg = Matrix(sourceBitmap.width, sourceBitmap.height)
        { x, y ->
            val result = Vector3(0, 0, 0)
            if (x in widthRange && y in heightRange) {
                val col0 = sourceRgbMatrix[x - 1, y - 1]
                val col1 = sourceRgbMatrix[x - 1, y]
                val col2 = sourceRgbMatrix[x - 1, y + 1]
                val col3 = sourceRgbMatrix[x, y - 1]
                val col5 = sourceRgbMatrix[x, y + 1]
                val col6 = sourceRgbMatrix[x + 1, y - 1]
                val col7 = sourceRgbMatrix[x + 1, y]
                val col8 = sourceRgbMatrix[x + 1, y + 1]
                var edgeMagnitude = 0
                for (channel in 0 until 3) {
                    val channel0 = col0[channel]
                    val channel1 = col1[channel]
                    val channel2 = col2[channel]
                    val channel3 = col3[channel]
                    val channel5 = col5[channel]
                    val channel6 = col6[channel]
                    val channel7 = col7[channel]
                    val channel8 = col8[channel]
                    val horizontalGrad = -channel0 + channel2 - channel3 - channel3 + channel5 + channel5 - channel6 +
                            channel8

                    val verticalGrad = -channel0 - channel1 - channel1 - channel2 + channel6 + channel7 +
                            channel7 + channel8

                    edgeMagnitude +=
                        Math.sqrt((horizontalGrad * horizontalGrad + verticalGrad * verticalGrad).toDouble()).toInt()
                }
                edgeMagnitude = when {
                    edgeMagnitude < 70 -> 0
                    edgeMagnitude > 765 -> 255
                    else -> (edgeMagnitude - 70) / 3
                }
                result.x = edgeMagnitude
                result.y = edgeMagnitude
                result.z = edgeMagnitude
            }

            result
        }

        bitmapVector3Converter.vector3MatrixToBitmap(resImg, targetBitmap)
    }
}