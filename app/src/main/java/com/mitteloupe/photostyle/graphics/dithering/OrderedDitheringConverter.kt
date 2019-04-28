package com.mitteloupe.photostyle.graphics.dithering

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import com.mitteloupe.photostyle.math.Vector3
import com.mitteloupe.photostyle.renderscript.ScriptC_orderedDithering
import com.mitteloupe.photostyle.renderscript.ScriptC_orderedDithering.const_PATTERN_BAYER
import com.mitteloupe.photostyle.renderscript.ScriptC_orderedDithering.const_PATTERN_HALFTONE
import com.mitteloupe.photostyle.renderscript.ScriptC_orderedDithering.const_PATTERN_LINEAR
import com.mitteloupe.photostyle.renderscript.ScriptC_orderedDithering.const_PATTERN_STRIPS

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class OrderedDitheringConverter(
    private val renderScript: RenderScript,
    private val pattern: Pattern
) : RgbToPaletteConverter {
    override fun applyPalette(
        sourceBitmap: Bitmap,
        targetBitmap: Bitmap,
        palette: Array<Vector3<Int>>,
        imageToPalette: IntArray
    ) {
        val inPixelsAllocation = Allocation.createFromBitmap(
            renderScript,
            sourceBitmap,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )

        val orderedDitheringScript = ScriptC_orderedDithering(renderScript)

        val paletteAllocation = getPaletteAllocation(renderScript, palette)
        orderedDitheringScript._palette = paletteAllocation
        orderedDitheringScript.invoke_prepare(pattern.renderScriptValue)

        val outPixelsAllocation = Allocation.createTyped(
            renderScript,
            inPixelsAllocation.type
        )

        orderedDitheringScript.forEach_bayer(inPixelsAllocation, outPixelsAllocation)
        outPixelsAllocation.copyTo(targetBitmap)
    }

    private fun getPaletteAllocation(
        renderScript: RenderScript?,
        palette: Array<Vector3<Int>>
    ): Allocation? {
        val rgbaType = Type.Builder(renderScript, Element.RGBA_8888(renderScript))
            .setX(palette.size)
            .create()

        val paletteAllocation = Allocation.createTyped(renderScript, rgbaType)
        val paletteShortArray = palette.flatMap { vector3 ->
            listOf(vector3.x.toByte(), vector3.y.toByte(), vector3.z.toByte(), 255.toByte())
        }.toByteArray()
        paletteAllocation.copyFrom(paletteShortArray)
        return paletteAllocation
    }
}

enum class Pattern(val renderScriptValue: Int) {
    PATTERN_BAYER(const_PATTERN_BAYER),
    PATTERN_HALFTONE(const_PATTERN_HALFTONE),
    PATTERN_STRIPS(const_PATTERN_STRIPS),
    PATTERN_LINEAR(const_PATTERN_LINEAR)
}