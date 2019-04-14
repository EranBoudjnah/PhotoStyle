package com.mitteloupe.photostyle.clustering

/**
 * Created by Eran Boudjnah on 14/04/2019.
 */

private const val NO_CLUSTER_ID = -1

class KMeans<T : Any>(
    private val arithmetic: Arithmetic<T>
) {
    private var executedSteps: Int = 0
    private lateinit var items: Array<T>
    private lateinit var centers: Array<T>
    private lateinit var itemToCenterPointers: IntArray
    private lateinit var itemCenterDistances: DoubleArray
    private lateinit var itemsPerCenter: IntArray

    fun execute(
        data: Array<T>,
        centersCount: Int,
        labels: IntArray,
        terminationCriteria: TerminationCriteria,
        centers: Array<T>
    ) {
        // TODO: flags: // KMEANS_RANDOM_CENTERS / KMEANS_PP_CENTERS / KMEANS_USE_INITIAL_LABELS

        initClustering(data, centers, labels)
        initCenters(centersCount, centers, data)
        initItemMetadataLists(labels)
        var changed = clusteringStep()
        calculateCenters(data, centers, labels)

        mainLoop@ while (changed && !isCriteriaMet(terminationCriteria)) {
            changed = clusteringStep()
        }
    }

    private fun isCriteriaMet(terminationCriteria: TerminationCriteria): Boolean {
        when (terminationCriteria) {
            is TerminationCriteria.Iterations -> if (terminationCriteria.iterations <= executedSteps) return true
        }
        return false
    }

    private fun initClustering(data: Array<T>, centers: Array<T>, labels: IntArray) {
        this.items = data
        this.centers = centers
        this.itemToCenterPointers = labels
        executedSteps = 0
        // TODO: Empty all arrays
    }

    private fun initCenters(centersCount: Int, centers: Array<T>, data: Array<T>) {
        val sample = data.toList().shuffled().subList(0, centersCount)

        repeat(centersCount) { index ->
            centers[index] = arithmetic.copyOf(sample[index])
        }
    }

    private fun initItemMetadataLists(labels: IntArray) {
        val center = centers[0]
        itemCenterDistances = DoubleArray(items.size) { index ->
            val item = items[index]
            arithmetic.getRelativeDistance(item, center)
        }
        itemsPerCenter = IntArray(items.size) { index ->
            if (index == 0) labels.size else 0
        }
    }

    private fun clusteringStep(): Boolean {
        val moved = reCluster()
        ++executedSteps

        return moved
    }

    private fun reCluster(): Boolean {
        var movedCount = 0

        items.forEachIndexed reassignItem@{ index, item ->
            val currentClusterId = itemToCenterPointers[index]

            // If the cluster only has one item left - don't re-assign its item.
            // Otherwise, we may lose that cluster.
            if (itemsPerCenter[currentClusterId] <= 1) {
                return@reassignItem
            }

            val center = centers[currentClusterId]

            var minDistance = itemCenterDistances[index]

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
                itemsPerCenter[currentClusterId]--
                itemToCenterPointers[index] = targetClusterId
                itemsPerCenter[targetClusterId]++

                itemCenterDistances[index] = minDistance

                ++movedCount
            }
        }

        val moved = movedCount != 0

        if (moved) {
            calculateCenters(items, centers, itemToCenterPointers)
        }

        return moved
    }

    private fun calculateCenters(data: Array<T>, centers: Array<T>, labels: IntArray) {
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

    private fun resetAllItems(centers: Array<T>) {
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