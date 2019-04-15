package com.mitteloupe.photostyle.math

class Vector3<T : Number>(
    var x: T,
    var y: T,
    var z: T
) {
    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z

    operator fun get(dimension: Int) =
        when (dimension) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw Exception("Vector3 dimension out of scope: $dimension")
        }

    operator fun set(dimension: Int, value: T) {
        when (dimension) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw Exception("Vector3 dimension out of scope: $dimension")
        }
    }

    override fun equals(other: Any?) = other is Vector3<*> && other.x == x && other.y == y && other.z == z
}