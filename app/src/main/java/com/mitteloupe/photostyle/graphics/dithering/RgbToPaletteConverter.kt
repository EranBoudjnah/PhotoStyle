package com.mitteloupe.photostyle.graphics.dithering

import android.graphics.Bitmap
import com.mitteloupe.photostyle.math.Vector3

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
interface RgbToPaletteConverter {
    fun applyPalette(
        sourceBitmap: Bitmap,
        targetBitmap: Bitmap,
        palette: Array<Vector3<Int>>,
        imageToPalette: IntArray
    )
}