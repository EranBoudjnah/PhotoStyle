package com.mitteloupe.photostyle.math

class Matrix<T : Any>(
    val width: Int,
    val height: Int,
    initialValue: (Int, Int) -> T
) {
    private val array: Array<Array<T>>

    init {
        @Suppress("UNCHECKED_CAST")
        array = Array(height) { y ->
            Array(width) { x ->
                initialValue(x, y) as Any
            }
        } as Array<Array<T>>
    }

    operator fun get(y: Int) = array[y]

    operator fun get(x: Int, y: Int) = array[y][x]

    operator fun set(x: Int, y: Int, value: T) {
        array[y][x] = value
    }
}