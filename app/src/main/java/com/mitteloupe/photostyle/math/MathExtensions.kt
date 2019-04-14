package com.mitteloupe.photostyle.math

/**
 * Created by Eran Boudjnah on 12/04/2019.
 */
fun Double.clamp(min: Double, max: Double) = Math.max(min, Math.min(this, max))

fun Float.clamp(min: Float, max: Float) = Math.max(min, Math.min(this, max))

fun Int.clamp(min: Int, max: Int) = Math.max(min, Math.min(this, max))
