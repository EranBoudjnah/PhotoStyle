package com.mitteloupe.photostyle.math

class Matrix<T : Any>(
    val width: Int,
    val height: Int,
    initialValue: (Int, Int) -> T
) {
    private val array: Array<Array<T>>

    init {
        @Suppress("UNCHECKED_CAST")
        array = Array(width) { x ->
            Array(height) { y ->
                initialValue(x, y) as Any
            }
        } as Array<Array<T>>
    }

    @Suppress("UNCHECKED_CAST")
    operator fun get(x: Int, y: Int) = array[x][y]

    operator fun set(x: Int, y: Int, value: T) {
        array[x][y] = value
    }
}