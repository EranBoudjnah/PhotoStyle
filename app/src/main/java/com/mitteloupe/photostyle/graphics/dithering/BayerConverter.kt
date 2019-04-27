package com.mitteloupe.photostyle.graphics.dithering

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import com.mitteloupe.photostyle.math.Vector3
import com.mitteloupe.photostyle.renderscript.ScriptC_bayer

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class BayerConverter(private val renderScript: RenderScript) : RgbToPaletteConverter {
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

        val bayerScript = ScriptC_bayer(renderScript)

        val paletteAllocation = getPaletteAllocation(renderScript, palette)
        bayerScript._palette = paletteAllocation
        bayerScript.invoke_prepare()

        val outPixelsAllocation = Allocation.createTyped(
            renderScript,
            inPixelsAllocation.type
        )

        bayerScript.forEach_bayer(inPixelsAllocation, outPixelsAllocation)
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
