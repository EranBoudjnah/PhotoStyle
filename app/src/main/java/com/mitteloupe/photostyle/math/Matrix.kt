package com.mitteloupe.photostyle.math

class Matrix<T : Any>(
    val width: Int,
    val height: Int
) {
    lateinit var array: Array<Array<Any>>

    fun initialize(initialValue: (Int, Int) -> T): Matrix<T> {
        array = Array(width) { x ->
            Array(height) { y ->
                initialValue(x, y) as Any
            }
        }
        return this
    }

    @Suppress("UNCHECKED_CAST")
    operator fun get(x: Int, y: Int) = array[x][y] as T

    operator fun set(x: Int, y: Int, value: T) {
        array[x][y] = value
    }
}