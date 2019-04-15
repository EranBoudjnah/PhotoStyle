package com.mitteloupe.photostyle.math

import com.mitteloupe.photostyle.clustering.KMeans

/**
 * Created by Eran Boudjnah on 15/04/2019.
 */
class Vector3Arithmetic : KMeans.Arithmetic<Vector3<Double>> {
    override fun add(value: Vector3<Double>, addTo: Vector3<Double>) {
        addTo.x += value.x
        addTo.y += value.y
        addTo.z += value.z
    }

    override fun divide(value: Vector3<Double>, divider: Int) {
        val dividerDouble = divider.toDouble()
        value.x /= dividerDouble
        value.y /= dividerDouble
        value.z /= dividerDouble
    }

    override fun reset(value: Vector3<Double>) {
        value.x = 0.0
        value.y = 0.0
        value.z = 0.0
    }

    override fun getRelativeDistance(from: Vector3<Double>, to: Vector3<Double>): Double {
        val x = from.x - to.x
        val y = from.y - to.y
        val z = from.z - to.z
        return x * x + y * y + z * z
    }

    override fun copyOf(value: Vector3<Double>) = Vector3(value[0], value[1], value[2])
}