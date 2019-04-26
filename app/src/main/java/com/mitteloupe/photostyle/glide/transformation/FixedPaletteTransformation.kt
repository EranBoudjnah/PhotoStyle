package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.graphics.BitmapVector3Converter
import com.mitteloupe.photostyle.graphics.PaletteAndDither
import com.mitteloupe.photostyle.graphics.dithering.RgbToPaletteConverter
import com.mitteloupe.photostyle.math.Vector3
import java.security.MessageDigest
import kotlin.system.measureNanoTime

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class FixedPaletteTransformation(
    private val palette: Palette,
    private val rgbToPaletteConverter: RgbToPaletteConverter
) : BitmapTransformation() {
    private val id = "com.mitteloupe.photostyle.glide.transformation.FixedPaletteTransformation:$palette"

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val outputBitmap = pool.getEqualBitmap(toTransform)

        val benchmark = measureNanoTime {
            PaletteAndDither(
                BitmapVector3Converter(),
                rgbToPaletteConverter
            ).processImage(toTransform, outputBitmap, palette.colors)
        }
        Log.d("Benchmark", "Process image took ${benchmark / 1_000_000_000.0} seconds")

        return outputBitmap
    }

    override fun equals(other: Any?) = other is FixedPaletteTransformation &&
            palette == other.palette

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }
}

sealed class Palette(val colors: Array<Vector3<Int>>) {
    object Monochrome : Palette(
        arrayOf(
            Vector3(0, 0, 0),
            Vector3(255, 255, 255)
        )
    )

    object GameBoy : Palette(
        arrayOf(
            Vector3(32, 32, 32),
            Vector3(94, 103, 69),
            Vector3(174, 186, 137),
            Vector3(227, 238, 192)
        )
    )

    object SuperGameBoy : Palette(
        arrayOf(
            Vector3(51, 30, 80),
            Vector3(166, 55, 37),
            Vector3(214, 142, 73),
            Vector3(247, 231, 198)
        )
    )

    object CGA1 : Palette(
        arrayOf(
            Vector3(0, 0, 0),
            Vector3(0, 170, 0),
            Vector3(170, 0, 0),
            Vector3(170, 85, 0)
        )
    )

    object CGA2 : Palette(
        arrayOf(
            Vector3(0, 0, 0),
            Vector3(85, 255, 85),
            Vector3(255, 85, 85),
            Vector3(255, 255, 85)
        )
    )

    object CGA3 : Palette(
        arrayOf(
            Vector3(0, 0, 0),
            Vector3(0, 170, 170),
            Vector3(170, 0, 170),
            Vector3(170, 170, 170)
        )
    )

    object CGA4 : Palette(
        arrayOf(
            Vector3(0, 0, 0),
            Vector3(85, 255, 255),
            Vector3(255, 85, 255),
            Vector3(255, 255, 255)
        )
    )

    object CGA5 : Palette(
        arrayOf(
            Vector3(0, 0, 0),
            Vector3(0, 170, 170),
            Vector3(170, 0, 0),
            Vector3(170, 170, 170)
        )
    )

    object CGA6 : Palette(
        arrayOf(
            Vector3(0, 0, 0),
            Vector3(85, 255, 255),
            Vector3(255, 85, 85),
            Vector3(255, 255, 255)
        )
    )

    object EGA : Palette(
        arrayOf(
            Vector3(0, 0, 0), Vector3(0, 0, 170), Vector3(0, 170, 0),
            Vector3(0, 170, 170), Vector3(170, 0, 0), Vector3(170, 0, 170),
            Vector3(170, 85, 0), Vector3(170, 170, 170), Vector3(85, 85, 85),
            Vector3(85, 85, 255), Vector3(85, 255, 85), Vector3(85, 255, 255),
            Vector3(255, 85, 85), Vector3(255, 85, 255), Vector3(255, 255, 85),
            Vector3(255, 255, 255)
        )
    )

    object Windows16 : Palette(
        arrayOf(
            Vector3(0, 0, 0), Vector3(128, 0, 0), Vector3(0, 128, 0),
            Vector3(128, 128, 0), Vector3(0, 0, 128), Vector3(128, 0, 128),
            Vector3(0, 128, 128), Vector3(192, 192, 192), Vector3(128, 128, 128),
            Vector3(255, 0, 0), Vector3(0, 255, 0), Vector3(255, 255, 0),
            Vector3(0, 0, 255), Vector3(255, 0, 255), Vector3(0, 255, 255),
            Vector3(255, 255, 255)
        )
    )

    object Macintosh16 : Palette(
        arrayOf(
            Vector3(255, 255, 255), Vector3(251, 243, 5), Vector3(255, 100, 3),
            Vector3(221, 9, 7), Vector3(242, 8, 132), Vector3(71, 0, 165),
            Vector3(0, 0, 211), Vector3(2, 171, 234), Vector3(31, 183, 20),
            Vector3(0, 100, 18), Vector3(86, 44, 5), Vector3(144, 113, 58),
            Vector3(192, 192, 192), Vector3(128, 128, 128), Vector3(64, 64, 64),
            Vector3(0, 0, 0)
        )
    )

    object RiscOS : Palette(
        arrayOf(
            Vector3(4, 185, 255), Vector3(255, 185, 0), Vector3(85, 134, 0),
            Vector3(237, 237, 185), Vector3(220, 0, 0), Vector3(5, 202, 0),
            Vector3(237, 237, 4), Vector3(0, 69, 151), Vector3(0, 0, 0),
            Vector3(54, 54, 54), Vector3(85, 85, 85), Vector3(118, 118, 118),
            Vector3(151, 151, 151), Vector3(185, 185, 185), Vector3(220, 220, 220),
            Vector3(255, 255, 255)
        )
    )

    object ZXSpectrum : Palette(
        arrayOf(
            Vector3(0, 0, 0), Vector3(0, 34, 199), Vector3(0, 43, 251),
            Vector3(214, 40, 22), Vector3(255, 51, 28), Vector3(212, 51, 199),
            Vector3(255, 64, 252), Vector3(0, 197, 37), Vector3(0, 249, 47),
            Vector3(0, 199, 201), Vector3(0, 251, 254), Vector3(204, 200, 42),
            Vector3(255, 252, 54), Vector3(202, 202, 202), Vector3(255, 255, 255)
        )
    )

    object NintendoEntertainmentSystem : Palette(
        arrayOf(
            Vector3(0, 0, 0), Vector3(252, 252, 252), Vector3(248, 248, 248),
            Vector3(188, 188, 188), Vector3(124, 124, 124), Vector3(164, 228, 252),
            Vector3(60, 188, 252), Vector3(0, 120, 248), Vector3(0, 0, 252),
            Vector3(184, 184, 248), Vector3(104, 136, 252), Vector3(0, 88, 248),
            Vector3(0, 0, 188), Vector3(216, 184, 248), Vector3(152, 120, 248),
            Vector3(104, 68, 252), Vector3(68, 40, 188), Vector3(248, 184, 248),
            Vector3(248, 120, 248), Vector3(216, 0, 204), Vector3(148, 0, 132),
            Vector3(248, 164, 192), Vector3(248, 88, 152), Vector3(228, 0, 88),
            Vector3(168, 0, 32), Vector3(240, 208, 176), Vector3(248, 120, 88),
            Vector3(248, 56, 0), Vector3(168, 16, 0), Vector3(252, 224, 168),
            Vector3(252, 160, 68), Vector3(228, 92, 16), Vector3(136, 20, 0),
            Vector3(248, 216, 120), Vector3(248, 184, 0), Vector3(172, 124, 0),
            Vector3(80, 48, 0), Vector3(216, 248, 120), Vector3(184, 248, 24),
            Vector3(0, 184, 0), Vector3(0, 120, 0), Vector3(184, 248, 184),
            Vector3(88, 216, 84), Vector3(0, 168, 0), Vector3(0, 104, 0),
            Vector3(184, 248, 216), Vector3(88, 248, 152), Vector3(0, 168, 68),
            Vector3(0, 88, 0), Vector3(0, 252, 252), Vector3(0, 232, 216),
            Vector3(0, 136, 136), Vector3(0, 64, 88), Vector3(248, 216, 248),
            Vector3(120, 120, 120)
        )
    )

    object AtariST : Palette(colors9Bit)

    object AMIGA : Palette(colors12Bit)

    class Custom(colors: Array<Vector3<Int>>) : Palette(colors)

    companion object {
        private val lookup9BitColor by lazy {
            Array(8) { index -> (255 * index) / 7 }
        }

        private val lookup12BitColor by lazy {
            Array(16) { index -> (255 * index) / 15 }
        }

        private val colors9Bit by lazy {
            Array(512) { index ->
                Vector3(
                    lookup9BitColor[index and 7],
                    lookup9BitColor[(index shr 3) and 7],
                    lookup9BitColor[(index shr 6) and 7]
                )
            }
        }

        private val colors12Bit by lazy {
            Array(4096) { index ->
                Vector3(
                    lookup12BitColor[index and 15],
                    lookup12BitColor[(index shr 4) and 15],
                    lookup12BitColor[(index shr 8) and 15]
                )
            }
        }
    }
}