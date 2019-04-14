package com.mitteloupe.photostyle.glide.transformation

import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.mitteloupe.photostyle.PaletteAndDither
import com.mitteloupe.photostyle.clustering.KMeans
import com.mitteloupe.photostyle.glide.extension.getEqualBitmap
import com.mitteloupe.photostyle.math.RgbLabConverter
import com.mitteloupe.photostyle.math.Vector3
import java.security.MessageDigest
import kotlin.system.measureNanoTime

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
class ColorCountTransformation(
    private val colorCount: Int = 16
) : BitmapTransformation() {
    private val id = "com.mitteloupe.photostyle.glide.transformation.ColorCountTransformation:$colorCount"

    private val arithmetic by lazy {
        object : KMeans.Arithmetic<Vector3<Double>> {
            override fun add(value: Vector3<Double>, addTo: Vector3<Double>) {
                addTo[0] += value[0]
                addTo[1] += value[1]
                addTo[2] += value[2]
            }

            override fun divide(value: Vector3<Double>, divider: Int) {
                val dividerDouble = divider.toDouble()
                value[0] /= dividerDouble
                value[1] /= dividerDouble
                value[2] /= dividerDouble
            }

            override fun reset(value: Vector3<Double>) {
                value[0] = 0.0
                value[1] = 0.0
                value[2] = 0.0
            }

            override fun getRelativeDistance(from: Vector3<Double>, to: Vector3<Double>): Double {
                val x = from[0] - to[0]
                val y = from[1] - to[1]
                val z = from[2] - to[2]
                return x * x + y * y + z * z
            }

            override fun copyOf(value: Vector3<Double>) = Vector3(value[0], value[1], value[2])
        }
    }

    private val kMeans by lazy { KMeans(arithmetic) }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val outputBitmap = pool.getEqualBitmap(toTransform)

        val benchmark = measureNanoTime {
            PaletteAndDither(toTransform, kMeans, RgbLabConverter()).processImage(outputBitmap, colorCount)
        }
        Log.d("Benchmark", "Process image took ${benchmark / 1_000_000_000.0} seconds")

        return outputBitmap
    }

    override fun equals(other: Any?) = other is ColorCountTransformation &&
            colorCount == other.colorCount

    override fun hashCode() = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }
}