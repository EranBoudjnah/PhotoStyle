package com.mitteloupe.photostyle.graphics

import android.graphics.Bitmap
import android.graphics.Color
import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class BitmapVector3Converter {
    private var width: Int = 0
    private var height: Int = 0
    private lateinit var pixels: IntArray

    fun initialize(width: Int, height: Int) {
        this.width = width
        this.height = height
        pixels = IntArray(width * height)
    }

    fun bitmapToVector3Matrix(bitmap: Bitmap): Matrix<Vector3<Int>> {
        if (bitmap.width != width || bitmap.height != height) {
            throw IllegalArgumentException("Bitmap does not match initialized size: was ${bitmap.width}x${bitmap.height}, expected ${width}x$height")
        }

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return Matrix(width, height) { x, y ->
            val color = pixels[x + y * width]
            Vector3(
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
        }
    }

    fun vector3MatrixToBitmap(matrix: Matrix<Vector3<Int>>, targetBitmap: Bitmap) {
        var targetPointer = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = matrix[x, y]
                pixels[targetPointer] = Color.rgb(color.x, color.y, color.z)
                targetPointer++
            }
        }
        targetBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}