package com.mitteloupe.photostyle.glide.extension

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool

/**
 * Created by Eran Boudjnah on 11/04/2019.
 */
fun BitmapPool.getEqualBitmap(
    sourceBitmap: Bitmap
): Bitmap {
    val sourceWidth = sourceBitmap.width
    val sourceHeight = sourceBitmap.height
    val bitmapConfig = sourceBitmap.config ?: Bitmap.Config.ARGB_8888
    return this.getDirty(sourceWidth, sourceHeight, bitmapConfig)
}

fun BitmapPool.getBitmapWithSize(
    bitmapWidth: Int,
    bitmapHeight: Int,
    bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888
) = this.getDirty(bitmapWidth, bitmapHeight, bitmapConfig)
