package com.mitteloupe.photostyle.graphics.dithering

import android.graphics.Bitmap
import com.mitteloupe.photostyle.graphics.BitmapVector3Converter
import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class ImageToPaletteDirectConverter(
    private val bitmapVector3Converter: BitmapVector3Converter
) : RgbToPaletteConverter {
    override fun applyPalette(
        sourceBitmap: Bitmap,
        targetBitmap: Bitmap,
        palette: Array<Vector3<Int>>,
        imageToPalette: IntArray
    ) {
        bitmapVector3Converter.initialize(sourceBitmap.width, sourceBitmap.height)
        bitmapVector3Converter.vector3MatrixToBitmap(
            Matrix(sourceBitmap.width, sourceBitmap.height) { x, y ->
                val imageIndex = imageToPalette[sourceBitmap.height * x + y]
                palette[imageIndex]
            },
            targetBitmap
        )
    }
}
