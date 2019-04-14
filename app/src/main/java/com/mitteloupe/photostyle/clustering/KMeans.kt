package com.mitteloupe.photostyle.clustering

import android.util.SparseIntArray
import java.util.Vector

/**
 * Created by Eran Boudjnah on 14/04/2019.
 */
class KMeans<T : Any>(
    private val arithmetic: Arithmetic<T>
) {
    private var executedSteps: Int = 0
    private lateinit var data: Vector<T>
    private lateinit var centers: Vector<T>
    private lateinit var labels: Vector<Int>
    private val itemsPerCenter = SparseIntArray()

    fun execute(
        data: Vector<T>,
        centersCount: Int,
        labels: Vector<Int>,
        terminationCriteria: TerminationCriteria,
        centers: Vector<T>
    ) {
        // TODO: flags: // KMEANS_RANDOM_CENTERS / KMEANS_PP_CENTERS / KMEANS_USE_INITIAL_LABELS

        initClustering(data, centers, labels)
        initCenters(centersCount, centers, data)
        initLabels(labels)
        var changed = clusteringStep()
        calculateCenters(data, centers, labels)

        while (changed) {
            // Break if criteria met
            changed = clusteringStep()
        }
    }

    private fun initClustering(data: Vector<T>, centers: Vector<T>, labels: Vector<Int>) {
        this.data = data
        this.centers = centers
        this.labels = labels
        executedSteps = 0
        itemsPerCenter.clear()
    }

    private fun initCenters(centersCount: Int, centers: Vector<T>, data: Vector<T>) {
        val sample = data.toList().shuffled().subList(0, centersCount)

        repeat(centersCount) { index ->
            centers.add(arithmetic.copyOf(sample[index]))
        }
    }

    private fun initLabels(labels: Vector<Int>) {
        repeat(labels.capacity()) {
            labels.add(0)
        }
        itemsPerCenter.put(0, labels.size)
    }

    private fun clusteringStep(): Boolean {
        val moved = reCluster()
        ++executedSteps

        return moved
    }

    private fun reCluster(): Boolean {
        var movedCount = 0

        val NO_CLUSTER_ID = -1
        data.forEachIndexed reassignItem@{ index, item ->
            val currentClusterId = labels[index]

            // If the cluster only has one item left - don't re-assign its item.
            // Otherwise, we may lose that cluster.
            if (itemsPerCenter[currentClusterId] <= 1) {
                return@reassignItem
            }

            val center = centers[currentClusterId]

            var minDistance = arithmetic.getRelativeDistance(item, center)

            var targetClusterId = NO_CLUSTER_ID
            centers.forEachIndexed findClosestCenter@{ visitedClusterId, visitedCluster ->
                if (visitedClusterId == currentClusterId) return@findClosestCenter

                val newDistance = arithmetic.getRelativeDistance(item, visitedCluster)
                if (newDistance < minDistance) {
                    minDistance = newDistance
                    targetClusterId = visitedClusterId
                }
            }

            if (targetClusterId != NO_CLUSTER_ID) {
                val lastClusterCount = itemsPerCenter[currentClusterId]
                itemsPerCenter.put(currentClusterId, lastClusterCount - 1)
                labels[index] = targetClusterId
                val newClusterCount = itemsPerCenter[targetClusterId] + 1
                itemsPerCenter.put(targetClusterId, newClusterCount)
                ++movedCount
            }
        }

        val moved = movedCount != 0

        if (moved) {
            calculateCenters(data, centers, labels)
        }

        return moved
    }

    private fun calculateCenters(data: Vector<T>, centers: Vector<T>, labels: Vector<Int>) {
        resetAllItems(centers)

        labels.forEachIndexed { dataIndex, centerIndex ->
            val item = data[dataIndex]
            val center = centers[centerIndex]
            arithmetic.add(item, center)
        }

        centers.forEachIndexed { index, currentCenter ->
            arithmetic.divide(currentCenter, itemsPerCenter[index])
        }
    }

    private fun resetAllItems(centers: Vector<T>) {
        centers.forEach { currentCenter ->
            arithmetic.reset(currentCenter)
        }
    }

    interface Arithmetic<T> {
        fun add(value: T, addTo: T)
        fun divide(value: T, divider: Int)
        fun reset(value: T)
        fun getRelativeDistance(from: T, to: T): Double
        fun copyOf(value: T): T
    }

    sealed class TerminationCriteria {
        data class Iterations(val iterations: Int) : TerminationCriteria()
    }
}