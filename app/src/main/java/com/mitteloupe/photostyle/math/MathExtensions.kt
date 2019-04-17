package com.mitteloupe.photostyle.math

/**
 * Created by Eran Boudjnah on 12/04/2019.
 */
fun Double.clamp(min: Double, max: Double) = Math.max(min, Math.min(this, max))

fun Float.clamp(min: Float, max: Float) = Math.max(min, Math.min(this, max))

fun Int.clamp(min: Int, max: Int) = Math.max(min, Math.min(this, max))

inline fun <T : Any> Matrix<T>.forEachIndexed(function: (value: T, x: Int, y: Int) -> Unit) {
    for (x in 0 until width) {
        this[x].forEachIndexed { y, value -> function(value, x, y) }
    }
}

inline fun <reified T : Any> Matrix<T>.toVector(): Array<T> {
    val result: Array<T?> = arrayOfNulls(width * height)
    var index = 0
    forEachIndexed { value, _, _ ->
        result[index] = value
        index++
    }
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}
