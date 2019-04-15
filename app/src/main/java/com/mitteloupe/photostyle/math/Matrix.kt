package com.mitteloupe.photostyle.math

class Matrix<T : Any>(
    val width: Int,
    val height: Int
) {
    private lateinit var array: Array<Array<T>>

    fun initialize(initialValue: (Int, Int) -> T): Matrix<T> {
        array = Array(width) { x ->
            Array(height) { y ->
                initialValue(x, y) as Any
            }
        } as Array<Array<T>>
        return this
    }

    @Suppress("UNCHECKED_CAST")
    operator fun get(x: Int, y: Int) = array[x][y]

    operator fun set(x: Int, y: Int, value: T) {
        array[x][y] = value
    }
}