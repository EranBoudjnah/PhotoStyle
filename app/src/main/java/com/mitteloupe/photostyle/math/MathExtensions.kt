package com.mitteloupe.photostyle.math

/**
 * Created by Eran Boudjnah on 12/04/2019.
 */
fun Float.clamp(min: Float, max: Float): Float = Math.max(min, Math.min(this, max))
