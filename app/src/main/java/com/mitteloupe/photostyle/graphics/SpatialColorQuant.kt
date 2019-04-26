package com.mitteloupe.photostyle.graphics

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.IntRange
import com.mitteloupe.photostyle.math.Matrix
import com.mitteloupe.photostyle.math.Vector3
import java.util.ArrayDeque
import java.util.Deque
import java.util.Vector

/**
 * Created by Eran Boudjnah on 12/04/2019.
 */
class SpatialColorQuant(
    sourceBitmap: Bitmap,
    private val targetBitmap: Bitmap,
    @IntRange(from = 2L, to = 255L) private val colorsCount: Int
) {
    private val imageWidth = sourceBitmap.width
    private val imageHeight = sourceBitmap.height
    private val image by lazy {
        Matrix(
            imageWidth,
            imageHeight
        ) { x, y ->
            val color = sourceBitmap.getPixel(x, y)
            Vector3(
                Color.red(color).toDouble() / 255.0,
                Color.green(color).toDouble() / 255.0,
                Color.blue(color).toDouble() / 255.0
            )
        }
    }

    fun processImage() {
        val filter1Weights = Matrix(
            1,
            1
        ) { _, _ -> Vector3(1.0, 1.0, 1.0) }

        val palette = Array(colorsCount) {
            Vector3(
                Math.random(),
                Math.random(),
                Math.random()
            )
        }

        // Dithering level must be more than 0.
        val ditheringLevel =
            0.09 * Math.log((image.width * image.height).toDouble()) - 0.04 * Math.log(palette.size.toDouble()) + 0.001
        // Filter size must be one of 1, 3, or 5.
        val filterSize = 3

        val standardDeviation = ditheringLevel * ditheringLevel

        var sum = 0.0
        val filter3Weights = Matrix(
            3,
            3
        ) { x, y ->
            val result = Vector3(0.0, 0.0, 0.0)
            for (z in 0 until 3) {
                val newValue =
                    Math.exp(-Math.sqrt(((x - 1) * (x - 1) + (y - 1) * (y - 1)).toDouble()) / standardDeviation)
                result[z] = newValue
                sum += newValue
            }
            result
        }
        sum /= 3
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                for (k in 0 until 3) {
                    filter3Weights[i, j][k] /= sum
                }
            }
        }

        sum = 0.0
        val filter5Weights = Matrix(
            5,
            5
        ) { x, y ->
            val result = Vector3(0.0, 0.0, 0.0)
            for (k in 0 until 3) {
                val newValue =
                    Math.exp(-Math.sqrt(((x - 2) * (x - 2) + (y - 2) * (y - 2)).toDouble()) / standardDeviation)
                result[k] = newValue
                sum += newValue
            }
            result
        }
        sum /= 3
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                for (k in 0 until 3) {
                    filter5Weights[i, j][k] /= sum
                }
            }
        }

        val quantizedImage = Matrix(imageWidth, imageHeight) { _, _ -> 0 }

        val filters = listOf(
            filter1Weights, filter3Weights, filter5Weights
        )
        spatialColorQuant(image, filters[filterSize - 1], quantizedImage, palette, 1.0, 0.001, 3, 1)

        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val red = (255.0 * palette[quantizedImage[x, y]][0]).toInt()
                val green = (255.0 * palette[quantizedImage[x, y]][1]).toInt()
                val blue = (255.0 * palette[quantizedImage[x, y]][2]).toInt()
                targetBitmap.setPixel(x, y, Color.rgb(red, green, blue))
            }
        }
    }

    private fun spatialColorQuant(
        image: Matrix<Vector3<Double>>,
        filterWeights: Matrix<Vector3<Double>>,
        quantizedImage: Matrix<Int>,
        palette: Array<Vector3<Double>>,
        initialTemperature: Double,
        finalTemperature: Double,
        temperaturesPerLevel: Int,
        repeatsPerTemperature: Int
    ) {
        val maxCoarseLevel = computeMaxCoarseLevel(image.width, image.height)
        var coarseVariables = Array3D(
            image.width.ushr(maxCoarseLevel),
            image.height.ushr(maxCoarseLevel),
            palette.size
        )

        fillRandom(coarseVariables)

        var temperature = initialTemperature

        // Compute a_i, b_{ij} according to (11)
        val extendedNeighborhoodWidth = filterWeights.width * 2 - 1
        val extendedNeighborhoodHeight = filterWeights.height * 2 - 1
        val b0 = Matrix(
            extendedNeighborhoodWidth,
            extendedNeighborhoodHeight
        ) { _, _ -> Vector3(0.0, 0.0, 0.0) }
        computeBArray(filterWeights, b0)

        val a0 = Matrix(
            image.width,
            image.height
        ) { _, _ -> Vector3(0.0, 0.0, 0.0) }

        computeAImage(image, b0, a0)

        // Compute a_I^l, b_{IJ}^l according to (18)
        val aVector = Vector<Matrix<Vector3<Double>>>()
        val bVector = Vector<Matrix<Vector3<Double>>>()
        aVector.add(a0)
        bVector.add(b0)

        for (coarse_level in 1..maxCoarseLevel) {
            val radiusWidth = (filterWeights.width - 1) / 2
            val radiusHeight = (filterWeights.height - 1) / 2
            val bi = Matrix(
                Math.max(3, bVector.last().width - 2),
                Math.max(3, bVector.last().height - 2)
            ) { _, _ -> Vector3(0.0, 0.0, 0.0) }
            for (J_y in 0 until bi.height) {
                for (J_x in 0 until bi.width) {
                    for (i_y in radiusHeight * 2 until radiusHeight * 2 + 2) {
                        for (i_x in radiusWidth * 2 until radiusWidth * 2 + 2) {
                            for (j_y in J_y * 2 until J_y * 2 + 2) {
                                for (j_x in J_x * 2 until J_x * 2 + 2) {
                                    bi[J_x, J_y] = bi[J_x, J_y] + bValue(
                                        bVector.last(),
                                        i_x,
                                        i_y,
                                        j_x,
                                        j_y
                                    )
                                }
                            }
                        }
                    }
                }
            }
            bVector.add(bi)

            val ai = Matrix(
                image.width.ushr(coarse_level),
                image.height.ushr(coarse_level)
            ) { _, _ -> Vector3(0.0, 0.0, 0.0) }
            sumCoarsen(aVector.last(), ai)
            aVector.add(ai)
        }

        // Multiscale annealing
        var coarseLevel = maxCoarseLevel
        val temperatureMultiplier =
            Math.pow(
                finalTemperature / initialTemperature,
                1.0 / (Math.max(3, maxCoarseLevel * temperaturesPerLevel))
            )
        var iterationsAtCurrentLevel = 0
        var skipPaletteMaintenance = false
        val s = Matrix(
            palette.size,
            palette.size
        ) { _, _ -> Vector3(0.0, 0.0, 0.0) }
        computeInitialS(s, coarseVariables, bVector[coarseLevel])
        var jPaletteSum = Matrix(
            coarseVariables.width,
            coarseVariables.height
        ) { _, _ -> Vector3(0.0, 0.0, 0.0) }
        computeInitialJPaletteSum(jPaletteSum, coarseVariables, palette)
        while (coarseLevel >= 0 || temperature > finalTemperature) {
            val a = aVector[coarseLevel]
            val b = bVector[coarseLevel]
            val middleB = bValue(b, 0, 0, 0, 0)
            val centerX = (b.width - 1) / 2
            val centerY = (b.height - 1) / 2
            var stepCounter = 0
            for (repeat in 0 until repeatsPerTemperature) {
                var pixelsChanged = 0
                var pixelsVisited = 0
                val visitQueue = ArrayDeque<Pair<Int, Int>>()
                randomPermutation2D(
                    coarseVariables.width,
                    coarseVariables.height,
                    visitQueue
                )

                // Compute 2*sum(j in extended neighborhood of i, j != i) b_ij

                while (!visitQueue.isEmpty()) {
                    // If we get to 10% above initial size, just revisit them all
                    if (visitQueue.size > coarseVariables.width * coarseVariables.height * 11 / 10) {
                        visitQueue.clear()
                        randomPermutation2D(
                            coarseVariables.width,
                            coarseVariables.height,
                            visitQueue
                        )
                    }

                    val iX = visitQueue.first().first
                    val iY = visitQueue.first().second
                    visitQueue.removeFirst()

                    // Compute (25)
                    val pI = Vector3(0.0, 0.0, 0.0)
                    for (y in 0 until b.height) {
                        for (x in 0 until b.width) {
                            val jX = x - centerX + iX
                            val jY = y - centerY + iY
                            if (iX == jX && iY == jY) continue
                            if (jX < 0 || jY < 0 || jX >= coarseVariables.width || jY >= coarseVariables.height) continue
                            val bIJ = bValue(b, iX, iY, jX, jY)
                            val jPal = jPaletteSum[jX, jY]
                            pI[0] += bIJ[0] * jPal[0]
                            pI[1] += bIJ[1] * jPal[1]
                            pI[2] += bIJ[2] * jPal[2]
                        }
                    }
                    pI.timesAssign(2.0)
                    pI.plusAssign(a[iX, iY])

                    val meanFieldLogs = Vector<Double>()
                    val meanFields = Vector<Double>()
                    var maxMeanFieldLog = Double.NEGATIVE_INFINITY
                    var meanFieldSum = 0.0
                    for (v in 0 until palette.size) {
                        // Update m_{pi(i)v}^I according to (23)
                        // We can subtract an arbitrary factor to prevent overflow,
                        // since only the weight relative to the sum matters, so we
                        // will choose a value that makes the maximum e^100.
                        meanFieldLogs.add(
                            -(palette[v].dotProduct(
                                pI + middleB.directProduct(palette[v])
                            )) / temperature
                        )
                        if (meanFieldLogs.last() > maxMeanFieldLog) {
                            maxMeanFieldLog = meanFieldLogs.last()
                        }
                    }
                    for (v in 0 until palette.size) {
                        meanFields.add(Math.exp(meanFieldLogs[v] - maxMeanFieldLog + 100))
                        meanFieldSum += meanFields.last()
                    }
                    if (meanFieldSum == 0.0) {
                        throw Exception("Fatal error: Meanfield sum underflowed. Please contact developer.")
                    }
                    val oldMaxY = bestMatchColor(coarseVariables, iX, iY, palette)
                    val jPalette = jPaletteSum[iX, iY]
                    for (v in 0 until palette.size) {
                        var newValue = meanFields[v] / meanFieldSum
                        // Prevent the matrix S from becoming singular
                        if (newValue <= 0) newValue = 1e-10
                        if (newValue >= 1) newValue = 1 - 1e-10
                        val deltaMIV = newValue - coarseVariables[iX, iY, v]
                        coarseVariables[iX, iY, v] = newValue
                        jPalette[0] += deltaMIV * palette[v][0]
                        jPalette[1] += deltaMIV * palette[v][1]
                        jPalette[2] += deltaMIV * palette[v][2]
                        if (Math.abs(deltaMIV) > 0.001 && !skipPaletteMaintenance) {
                            updateS(s, coarseVariables, b, iX, iY, v, deltaMIV)
                        }
                    }
                    val maxV = bestMatchColor(coarseVariables, iX, iY, palette)
                    // Only consider it a change if the colors are different enough
                    if ((palette[maxV] - palette[oldMaxY]).normSquared() >= 1.0 / (255.0 * 255.0)) {
                        pixelsChanged++
                        for (y in Math.min(1, centerY - 1) until Math.max(
                            b.height - 1,
                            centerY + 1
                        )) {
                            for (x in Math.min(1, centerX - 1) until Math.max(b.width - 1, centerX + 1)) {
                                val jX = x - centerX + iX
                                val jY = y - centerY + iY
                                if (jX < 0 || jY < 0 || jX >= coarseVariables.width || jY >= coarseVariables.height) continue
                                visitQueue.add(Pair(jX, jY))
                            }
                        }
                    }
                    pixelsVisited++

                    stepCounter++
                }
                if (skipPaletteMaintenance) {
                    computeInitialS(s, coarseVariables, bVector[coarseLevel])
                }
                refinePalette(s, coarseVariables, a, palette)
                computeInitialJPaletteSum(jPaletteSum, coarseVariables, palette)
            }

            iterationsAtCurrentLevel++
            skipPaletteMaintenance = false
            if ((temperature <= finalTemperature || coarseLevel > 0) &&
                iterationsAtCurrentLevel >= temperaturesPerLevel
            ) {
                coarseLevel--
                if (coarseLevel < 0) break
                val pNewCoarseVariables = Array3D(
                    image.width.ushr(coarseLevel),
                    image.height.ushr(coarseLevel),
                    palette.size
                )
                zoomDouble(coarseVariables, pNewCoarseVariables)
                coarseVariables = pNewCoarseVariables
                iterationsAtCurrentLevel = 0
                jPaletteSum = Matrix<Vector3<Double>>(
                    coarseVariables.width,
                    coarseVariables.height
                ) { _, _ -> Vector3(0.0, 0.0, 0.0) }

                computeInitialJPaletteSum(jPaletteSum, coarseVariables, palette)
                skipPaletteMaintenance = true
            }
            if (temperature > finalTemperature) {
                temperature *= temperatureMultiplier
            }
        }

        // This is normally not used, but is handy sometimes for debugging
        while (coarseLevel > 0) {
            coarseLevel--
            val pNewCoarseVariables = Array3D(
                image.width.ushr(coarseLevel),
                image.height.ushr(coarseLevel),
                palette.size
            )
            zoomDouble(coarseVariables, pNewCoarseVariables)
            coarseVariables = pNewCoarseVariables
        }

        for (i_x in 0 until image.width) {
            for (i_y in 0 until image.height) {
                quantizedImage[i_x, i_y] = bestMatchColor(coarseVariables, i_x, i_y, palette)
            }
        }

        for (v in 0 until palette.size) {
            for (k in 0 until 3) {
                if (palette[v][k] > 1.0) palette[v][k] = 1.0
                if (palette[v][k] < 0.0) palette[v][k] = 0.0
            }
        }
    }

    private fun updateS(
        s: Matrix<Vector3<Double>>,
        coarseVariables: Array3D,
        b: Matrix<Vector3<Double>>,
        j_x: Int,
        j_y: Int,
        alpha: Int,
        delta: Double
    ) {
        val paletteSize = s.width
        val coarseWidth = coarseVariables.width
        val coarseHeight = coarseVariables.height
        val centerX = (b.width - 1) / 2
        val centerY = (b.height - 1) / 2
        val maxIX = Math.min(coarseWidth, j_x + centerX + 1)
        val maxIY = Math.min(coarseHeight, j_y + centerY + 1)
        for (i_y in Math.max(0, j_y - centerY) until maxIY) {
            for (i_x in Math.max(0, j_x - centerX) until maxIX) {
                val deltaBIJ = delta * bValue(b, i_x, i_y, j_x, j_y)
                if (i_x == j_x && i_y == j_y) continue
                for (v in 0..alpha) {
                    val multiplier = coarseVariables[i_x, i_y, v]
                    s[v, alpha][0] += multiplier * deltaBIJ[0]
                    s[v, alpha][1] += multiplier * deltaBIJ[1]
                    s[v, alpha][2] += multiplier * deltaBIJ[2]
                }
                for (v in alpha until paletteSize) {
                    val multiplier = coarseVariables[i_x, i_y, v]
                    s[alpha, v][0] += multiplier * deltaBIJ[0]
                    s[alpha, v][1] += multiplier * deltaBIJ[1]
                    s[alpha, v][2] += multiplier * deltaBIJ[2]
                }
            }
        }
        s[alpha, alpha].plusAssign(delta * bValue(b, 0, 0, 0, 0))
    }

    private fun refinePalette(
        s: Matrix<Vector3<Double>>,
        coarseVariables: Array3D,
        a: Matrix<Vector3<Double>>,
        palette: Array<Vector3<Double>>
    ) {
        for (v in 0 until s.width) {
            for (alpha in 0 until v) {
                s[v, alpha] = s[alpha, v]
            }
        }

        val r = Array(palette.size) { v ->
            val value = Vector3(0.0, 0.0, 0.0)
            for (i_y in 0 until coarseVariables.height) {
                for (i_x in 0 until coarseVariables.width) {
                    value += coarseVariables[i_x, i_y, v] * a[i_x, i_y]
                }
            }
            value
        }

        for (channel in 0 until 3) {
            val sChannel = extractVectorLayer2D(s, channel)
            val rChannel = extractVectorLayer1D(r, channel)
            try {
                val paletteChannel = -1.0 * ((2.0 * sChannel).matrixInverse()) * rChannel
                for (v in 0 until palette.size) {
                    var value = paletteChannel[v]
                    if (value < 0.0) value = 0.0
                    if (value > 1.0) value = 1.0
                    palette[v][channel] = value
                }

            } catch (e: java.lang.Exception) {
                for (v in 0 until palette.size) {
                    palette[v][channel] = 0.0
                }
            }
        }
    }

    private operator fun Matrix<Double>.times(vector: Array<Double>): Vector<Double> {
        val result = Vector<Double>(vector.size)
        vector.forEach { _ ->
            for (j in 0 until height) {
                var dotProduct = 0.0
                for (i in 0 until width) {
                    dotProduct += this[i, j] * vector[j]
                }
                result.add(dotProduct)
            }
        }
        return result
    }

    private fun extractVectorLayer1D(s: Array<Vector3<Double>>, k: Int) =
        Array(s.size) { i -> s[i][k] }

    private fun extractVectorLayer2D(s: Matrix<Vector3<Double>>, dimension: Int) =
        Matrix(s.width, s.height) { x, y -> s[x, y][dimension] }

    private fun zoomDouble(small: Array3D, big: Array3D) {
        // Simple scaling of the weights array based on mixing the four
        // pixels falling under each fine pixel, weighted by area.
        // To mix the pixels a little, we assume each fine pixel
        // is 1.2 fine pixels wide and high.
        for (y in 0 until big.height / 2 * 2) {
            for (x in 0 until big.width / 2 * 2) {
                val left = Math.max(0.0, (x - 0.1) / 2.0)
                val right = Math.min(small.width - 0.001, (x + 1.1) / 2.0)
                val top = Math.max(0.0, (y - 0.1) / 2.0)
                val bottom = Math.min(small.height - 0.001, (y + 1.1) / 2.0)
                val xLeft = Math.floor(left).toInt()
                val xRight = Math.floor(right).toInt()
                val yTop = Math.floor(top).toInt()
                val yBottom = Math.floor(bottom).toInt()
                val area = (right - left) * (bottom - top)
                val topLeftWeight =
                    (Math.ceil(left) - left) * (Math.ceil(top) - top) / area
                val topRightWeight =
                    (right - Math.floor(right)) * (Math.ceil(top) - top) / area
                val bottomLeftWeight =
                    (Math.ceil(left) - left) * (bottom - Math.floor(bottom)) / area
                val bottomRightWeight =
                    (right - Math.floor(right)) * (bottom - Math.floor(
                        bottom
                    )) / area
                val topWeight =
                    (right - left) * (Math.ceil(top) - top) / area
                val bottomWeight =
                    (right - left) * (bottom - Math.floor(bottom)) / area
                val leftWeight =
                    (bottom - top) * (Math.ceil(left) - left) / area
                val rightWeight =
                    (bottom - top) * (right - Math.floor(right)) / area
                for (z in 0 until big.depth) {
                    if (xLeft == xRight && yTop == yBottom) {
                        big[x, y, z] = small[xLeft, yTop, z]
                    } else if (xLeft == xRight) {
                        big[x, y, z] =
                            topWeight * small[xLeft, yTop, z] + bottomWeight * small[xLeft, yBottom, z]
                    } else if (yTop == yBottom) {
                        big[x, y, z] =
                            leftWeight * small[xLeft, yTop, z] + rightWeight * small[xRight, yTop, z]
                    } else {
                        big[x, y, z] =
                            topLeftWeight * small[xLeft, yTop, z] +
                                    topRightWeight * small[xRight, yTop, z] +
                                    bottomLeftWeight * small[xLeft, yBottom, z] +
                                    bottomRightWeight * small[xRight, yBottom, z]
                    }
                }
            }
        }
    }

    private fun bestMatchColor(
        vars: Array3D,
        i_x: Int,
        i_y: Int,
        palette: Array<Vector3<Double>>
    ): Int {
        var maxV = 0
        var maxWeight = vars[i_x, i_y, 0]
        for (v in 1 until palette.size) {
            if (vars[i_x, i_y, v] > maxWeight) {
                maxV = v
                maxWeight = vars[i_x, i_y, v]
            }
        }
        return maxV
    }

    private fun randomPermutation2D(
        width: Int,
        height: Int,
        result: Deque<Pair<Int, Int>>
    ) {
        val perm1d = Vector<Int>()
        randomPermutation(width * height, perm1d)
        while (perm1d.isNotEmpty()) {
            val idx = perm1d.last()
            perm1d.removeAt(perm1d.size - 1)
            result.add(Pair(idx % width, idx / width))
        }
    }

    private fun randomPermutation(count: Int, result: Vector<Int>) {
        result.clear()
        for (i in 0 until count) {
            result.add(i)
        }
        result.shuffle()
    }

    private fun computeInitialS(
        s: Matrix<Vector3<Double>>,
        coarseVariables: Array3D,
        b: Matrix<Vector3<Double>>
    ) {
        val paletteSize = s.width
        val coarseWidth = coarseVariables.width
        val coarseHeight = coarseVariables.height
        val centerX = (b.width - 1) / 2
        val centerY = (b.height - 1) / 2
        val centerB = bValue(b, 0, 0, 0, 0)
        val zeroVector = Vector3(0.0, 0.0, 0.0)
        for (v in 0 until paletteSize) {
            for (alpha in v until paletteSize) {
                s[v, alpha] = zeroVector
            }
        }
        for (i_y in 0 until coarseHeight) {
            for (i_x in 0 until coarseWidth) {
                val maxJX = Math.min(coarseWidth, i_x - centerX + b.width)
                val maxJY = Math.min(coarseHeight, i_y - centerY + b.height)
                for (j_y in Math.max(0, i_y - centerY) until maxJY) {
                    for (j_x in Math.max(0, i_x - centerX) until maxJX) {
                        if (i_x == j_x && i_y == j_y) continue
                        val bIJ = bValue(b, i_x, i_y, j_x, j_y)
                        for (v in 0 until paletteSize) {
                            for (alpha in v until paletteSize) {
                                val multiplier =
                                    coarseVariables[i_x, i_y, v] * coarseVariables[j_x, j_y, alpha]
                                s[v, alpha][0] += multiplier * bIJ[0]
                                s[v, alpha][1] += multiplier * bIJ[1]
                                s[v, alpha][2] += multiplier * bIJ[2]
                            }
                        }
                    }
                }
                for (v in 0 until paletteSize) {
                    s[v, v].plusAssign(coarseVariables[i_x, i_y, v] * centerB)
                }
            }
        }
    }

    private fun sumCoarsen(
        fine: Matrix<Vector3<Double>>,
        coarse: Matrix<Vector3<Double>>
    ) {
        for (y in 0 until coarse.height) {
            for (x in 0 until coarse.width) {
                var divisor = 1.0
                val value = fine[x * 2, y * 2]
                if (x * 2 + 1 < fine.width) {
                    divisor += 1; value.plusAssign(fine[x * 2 + 1, y * 2])
                }
                if (y * 2 + 1 < fine.height) {
                    divisor += 1; value.plusAssign(fine[x * 2, y * 2 + 1])
                }
                if (x * 2 + 1 < fine.width && y * 2 + 1 < fine.height) {
                    divisor += 1; value.plusAssign(fine[x * 2 + 1, y * 2 + 1])
                }
                coarse[x, y] = value
            }
        }
    }

    private fun bValue(
        b: Matrix<Vector3<Double>>,
        i_x: Int,
        i_y: Int,
        j_x: Int,
        j_y: Int
    ): Vector3<Double> {
        val radiusWidth = (b.width - 1) / 2
        val radiusHeight = (b.height - 1) / 2
        val kX = j_x - i_x + radiusWidth
        val kY = j_y - i_y + radiusHeight
        return if (kX in 0 until b.width && kY in 0 until b.height)
            b[kX, kY]
        else
            Vector3(0.0, 0.0, 0.0)
    }

    private fun computeInitialJPaletteSum(
        jPaletteSum: Matrix<Vector3<Double>>,
        coarseVariables: Array3D,
        palette: Array<Vector3<Double>>
    ) {
        for (j_y in 0 until coarseVariables.height) {
            for (j_x in 0 until coarseVariables.width) {
                val paletteSum = Vector3(0.0, 0.0, 0.0)
                for (alpha in 0 until palette.size) {
                    paletteSum += coarseVariables[j_x, j_y, alpha] * palette[alpha]
                }
                jPaletteSum[j_x, j_y] = paletteSum
            }
        }
    }

    private fun computeMaxCoarseLevel(width: Int, height: Int): Int {
        // We want the coarsest layer to have at most maxPixels pixels
        val maxPixels = 4000
        var result = 0
        var tempWidth = width
        var tempHeight = height
        while (tempWidth * tempHeight > maxPixels) {
            tempWidth = tempWidth.ushr(1)
            tempHeight = tempHeight.ushr(1)
            result++
        }
        return result
    }

    private fun fillRandom(a: Array3D) {
        for (i in 0 until a.width) {
            for (j in 0 until a.height) {
                for (k in 0 until a.depth) {
                    a[i, j, k] = Math.random()
                }
            }
        }
    }

    private fun computeAImage(
        image: Matrix<Vector3<Double>>,
        b: Matrix<Vector3<Double>>,
        a: Matrix<Vector3<Double>>
    ) {
        val radiusWidth = (b.width - 1) / 2
        val radiusHeight = (b.height - 1) / 2
        for (i_y in 0 until a.height) {
            for (i_x in 0 until a.width) {
                var jY = i_y - radiusHeight
                while (jY <= i_y + radiusHeight) {
                    if (jY < 0) jY = 0
                    if (jY >= a.height) break

                    var jX = i_x - radiusWidth
                    while (jX <= i_x + radiusWidth) {
                        if (jX < 0) jX = 0
                        if (jX >= a.width) break

                        a[i_x, i_y].plusAssign(
                            bValue(
                                b,
                                i_x,
                                i_y,
                                jX,
                                jY
                            ).directProduct(image[jX, jY])
                        )
                        jX++
                    }
                    jY++
                }
                a[i_x, i_y] = a[i_x, i_y] * -2.0
            }
        }
    }

    private fun computeBArray(
        filter_weights: Matrix<Vector3<Double>>,
        b: Matrix<Vector3<Double>>
    ) {
        // Assume that the pixel i is always located at the center of b,
        // and vary pixel j's location through each location in b.
        val radiusWidth = (filter_weights.width - 1) / 2
        val radiusHeight = (filter_weights.height - 1) / 2
        val offsetX = (b.width - 1) / 2 - radiusWidth
        val offsetY = (b.height - 1) / 2 - radiusHeight
        for (j_y in 0 until b.height) {
            for (j_x in 0 until b.width) {
                for (k_y in 0 until filter_weights.height) {
                    for (k_x in 0 until filter_weights.width) {
                        if (k_x + offsetX >= j_x - radiusWidth &&
                            k_x + offsetX <= j_x + radiusWidth &&
                            k_y + offsetY >= j_y - radiusWidth &&
                            k_y + offsetY <= j_y + radiusWidth
                        ) {
                            b[j_x, j_y].plusAssign(
                                filter_weights[k_x, k_y].directProduct(
                                    filter_weights[
                                            k_x + offsetX - j_x + radiusWidth,
                                            k_y + offsetY - j_y + radiusHeight
                                    ]
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private operator fun Double.times(s_k: Matrix<Double>) =
        Matrix(s_k.width, s_k.height)
        { x, y -> s_k[x, y] * this@times }

    private operator fun Double.times(vector3: Vector3<Double>) =
        Vector3(
            vector3.x * this,
            vector3.y * this,
            vector3.z * this
        )

    private operator fun Vector3<Double>.minus(vector3: Vector3<Double>) =
        Vector3(x - vector3.x, y - vector3.y, z - vector3.z)

    private operator fun Vector3<Double>.times(d: Double) =
        Vector3(x * d, y * d, z * d)

    private fun Vector3<Double>.normSquared() = x * x + y * y + z * z

    private fun Vector3<Double>.dotProduct(vector: Vector3<Double>) = x * vector.x + y * vector.y + z * vector.z

    private operator fun Vector3<Double>.timesAssign(value: Double) {
        this.x *= value
        this.y *= value
        this.z *= value
    }

    operator fun Vector3<Double>.plusAssign(value: Vector3<Double>) {
        this.x += value.x
        this.y += value.y
        this.z += value.z
    }

    operator fun Vector3<Double>.plus(value: Vector3<Double>) =
        Vector3(x + value.x, y + value.y, z + value.z)

    private fun Vector3<Double>.directProduct(vector3: Vector3<Double>) =
        Vector3(x * vector3.x, y * vector3.y, z * vector3.z)

    class Array3D(
        val width: Int,
        val height: Int,
        val depth: Int
    ) {
        private val array: Array<Array<DoubleArray>> =
            Array(width) {
                Array(height) {
                    DoubleArray(depth)
                }
            }

        operator fun get(x: Int, y: Int, z: Int): Double =
            array[x][y][z]

        operator fun set(
            x: Int,
            y: Int,
            z: Int,
            value: Double
        ) {
            array[x][y][z] = value
        }
    }
}

fun Matrix<Double>.matrixInverse(): Matrix<Double> {
    // I use Guassian Elimination to calculate the inverse:
    // (1) 'augment' the matrix (left) by the identity (on the right)
    // (2) Turn the matrix on the left into the identity by elemetry row ops
    // (3) The matrix on the right is the inverse (was the identity matrix)
    // There are 3 elemtary row ops: (I combine b and c in my code)
    // (a) Swap 2 rows
    // (b) Multiply a row by a scalar
    // (c) Add 2 rows

    val dim = this.width
    val identityMatrix = Matrix(dim, dim) { x, y -> if (x == y) 1.0 else 0.0 }

    val copyMatrix = Matrix(dim, dim) { x, y -> this@matrixInverse[x, y] }

    // Perform elementary row operations
    for (i in 0 until dim) {
        // get the element e on the diagonal
        // if we have a 0 on the diagonal (we'll need to swap with a lower row)
        if (copyMatrix[i, i] == 0.0) {
            //look through every row below the i'th row
            for (ii in i + 1 until dim) {
                //if the ii'th row has a non-0 in the i'th col
                if (copyMatrix[ii, i] != 0.0) {
                    //it would make the diagonal have a non-0 so swap it
                    for (j in 0 until dim) {
                        copyMatrix[i, j] =
                            copyMatrix[ii, j].apply {
                                copyMatrix[ii, j] = copyMatrix[i, j]
                            }
                        identityMatrix[i, j] =
                            identityMatrix[ii, j].apply {
                                identityMatrix[ii, j] = identityMatrix[i, j]
                            }
                    }
                    //don't bother checking other rows since we've swapped
                    break
                }
            }
            //if it's still 0, not invertible (error)
            if (copyMatrix[i, i] == 0.0) {
                throw Exception("Non-invertible")
            }
        }

        // Scale this row down by e (so we have a 1 on the diagonal)
        val e = copyMatrix[i, i]
        for (j in 0 until dim) {
            copyMatrix[i, j] =
                copyMatrix[i, j] / e //apply to original matrix
            identityMatrix[i, j] =
                identityMatrix[i, j] / e //apply to identity
        }

        // Subtract this row (scaled appropriately for each row) from ALL of
        // the other rows so that there will be 0's in this column in the
        // rows above and below this one
        for (ii in 0 until dim) {
            // Only apply to other rows (we want a 1 on the diagonal)
            if (ii == i) {
                continue
            }

            // We want to change this element to 0
            val temporaryValue2 = copyMatrix[ii, i]

            // Subtract (the row above(or below) scaled by e) from (the
            // current row) but start at the i'th column and assume all the
            // stuff left of diagonal is 0 (which it should be if we made this
            // algorithm correctly)
            for (j in 0 until dim) {
                copyMatrix[ii, j] -= temporaryValue2 * copyMatrix[i, j] //apply to original matrix
                identityMatrix[ii, j] -= temporaryValue2 * identityMatrix[i, j] //apply to identity
            }
        }
    }

    //we've done all operations, C should be the identity
    //matrix I should be the inverse:
    return identityMatrix
}