package com.mitteloupe.photostyle.graphics.dithering

import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class ImageToPaletteDirectConverter : RgbToPaletteConverter {
    override fun applyPalette(
        sourceImage: Matrix<Vector3<Int>>,
        palette: Array<Vector3<Int>>,
        imageToPalette: IntArray
    ) = Matrix(sourceImage.width, sourceImage.height) { x, y ->
            val imageIndex = imageToPalette[sourceImage.height * x + y]
            palette[imageIndex]
        }
}
