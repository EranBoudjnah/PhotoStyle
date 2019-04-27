package com.mitteloupe.photostyle.graphics.dithering

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import com.mitteloupe.photostyle.math.Vector3
import com.mitteloupe.photostyle.renderscript.ScriptC_floydSteinberg


/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class FloydSteinbergConverter(private val renderScript: RenderScript) : RgbToPaletteConverter {

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

        val floydSteinbergScript = ScriptC_floydSteinberg(renderScript)

        setUpPalette(palette, floydSteinbergScript)

        setUpErrorMap(sourceBitmap, floydSteinbergScript, inPixelsAllocation)

        applyPalette(inPixelsAllocation, floydSteinbergScript, targetBitmap)
    }

    private fun applyPalette(
        inPixelsAllocation: Allocation,
        floydSteinbergScript: ScriptC_floydSteinberg,
        targetBitmap: Bitmap
    ) {
        val outPixelsAllocation = Allocation.createTyped(
            renderScript,
            inPixelsAllocation.type
        )

        floydSteinbergScript.forEach_applyColor(inPixelsAllocation, outPixelsAllocation)
        outPixelsAllocation.copyTo(targetBitmap)
    }

    private fun setUpErrorMap(
        sourceBitmap: Bitmap,
        floydSteinbergScript: ScriptC_floydSteinberg,
        inPixelsAllocation: Allocation?
    ) {
        val errorMapAllocation = getErrorMapAllocation(sourceBitmap)

        floydSteinbergScript._errorMap = errorMapAllocation

        floydSteinbergScript.invoke_calculateError(inPixelsAllocation)
    }

    private fun setUpPalette(
        palette: Array<Vector3<Int>>,
        floydSteinbergScript: ScriptC_floydSteinberg
    ) {
        val paletteAllocation = getPaletteAllocation(renderScript, palette)
        floydSteinbergScript._palette = paletteAllocation
        floydSteinbergScript.invoke_prepare(palette.size.toLong())
    }

    private fun getErrorMapAllocation(sourceBitmap: Bitmap): Allocation? {
        val rgbTypeBuilder = Type.Builder(renderScript, Element.F32_4(renderScript))
        rgbTypeBuilder.setX(sourceBitmap.width)
        rgbTypeBuilder.setY(sourceBitmap.height)

        return Allocation.createTyped(
            renderScript, rgbTypeBuilder.create(),
            Allocation.USAGE_SCRIPT
        )
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
