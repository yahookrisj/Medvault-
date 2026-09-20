package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.Medicine
import com.example.data.model.MedicineImage
import com.example.data.model.VisualMatchResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object FeatureExtractor {

    private const val GRID_SIZE = 8 // 8x8 spatial grid = 64 cells
    private const val FEATURES_PER_CELL = 4 // Mean R, G, B, and Edge Energy = 256 dimensions
    const val EMBEDDING_SIZE = GRID_SIZE * GRID_SIZE * FEATURES_PER_CELL // 256 dimensions

    /**
     * Extracts a normalized 256-dimensional visual feature embedding vector from the bitmap.
     * Incorporates spatial color distribution and edge intensity.
     */
    fun extractEmbedding(bitmap: Bitmap): FloatArray {
        // Downsample to a standardized 64x64 working bitmap
        val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        val pixels = IntArray(64 * 64)
        scaled.getPixels(pixels, 0, 64, 0, 0, 64, 64)

        val embedding = FloatArray(EMBEDDING_SIZE)
        val cellWidth = 64 / GRID_SIZE
        val cellHeight = 64 / GRID_SIZE

        var vectorIndex = 0

        for (gridY in 0 until GRID_SIZE) {
            for (gridX in 0 until GRID_SIZE) {
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var edgeEnergy = 0f
                var count = 0

                val startX = gridX * cellWidth
                val endX = (gridX + 1) * cellWidth
                val startY = gridY * cellHeight
                val endY = (gridY + 1) * cellHeight

                for (y in startY until endY) {
                    for (x in startX until endX) {
                        val pixel = pixels[y * 64 + x]
                        val r = Color.red(pixel) / 255f
                        val g = Color.green(pixel) / 255f
                        val b = Color.blue(pixel) / 255f

                        sumR += r
                        sumG += g
                        sumB += b

                        // Simple edge detection with horizontal and vertical neighbor
                        if (x + 1 < 64 && y + 1 < 64) {
                            val rightPixel = pixels[y * 64 + (x + 1)]
                            val downPixel = pixels[(y + 1) * 64 + x]
                            val currentLuma = 0.299f * r + 0.587f * g + 0.114f * b
                            val rightLuma = 0.299f * (Color.red(rightPixel) / 255f) + 0.587f * (Color.green(rightPixel) / 255f) + 0.114f * (Color.blue(rightPixel) / 255f)
                            val downLuma = 0.299f * (Color.red(downPixel) / 255f) + 0.587f * (Color.green(downPixel) / 255f) + 0.114f * (Color.blue(downPixel) / 255f)

                            edgeEnergy += abs(currentLuma - rightLuma) + abs(currentLuma - downLuma)
                        }

                        count++
                    }
                }

                if (count > 0) {
                    embedding[vectorIndex++] = sumR / count
                    embedding[vectorIndex++] = sumG / count
                    embedding[vectorIndex++] = sumB / count
                    embedding[vectorIndex++] = edgeEnergy / count
                } else {
                    vectorIndex += 4
                }
            }
        }

        // L2 Normalization so dot product equals cosine similarity
        var normSquared = 0f
        for (v in embedding) {
            normSquared += v * v
        }
        val norm = sqrt(normSquared)
        if (norm > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }

        return embedding
    }

    /**
     * Calculates cosine similarity between two normalized feature vectors.
     * Output range is converted to 0.0f - 100.0f percentage.
     */
    fun calculateSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size) return 0f

        var dotProduct = 0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
        }

        // Cosine similarity ranges from -1 to 1 for normalized vectors.
        // For visual feature embeddings, values typically range from 0.4 to 1.0.
        // Scale to a realistic, user-friendly percentage:
        val normalizedScore = max(0f, (dotProduct - 0.2f) / 0.8f) * 100f
        return min(100f, max(0f, normalizedScore))
    }

    fun serializeVector(vector: FloatArray): String {
        return vector.joinToString(",") { "%.5f".format(it) }
    }

    fun deserializeVector(str: String): FloatArray? {
        return try {
            val parts = str.split(",")
            val floats = FloatArray(parts.size)
            for (i in parts.indices) {
                floats[i] = parts[i].trim().toFloat()
            }
            floats
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Compare a query bitmap against stored medicine images and return top matches.
     */
    fun findTopMatches(
        queryBitmap: Bitmap,
        storedImages: List<MedicineImage>,
        medicinesMap: Map<Long, Medicine>,
        limit: Int = 3
    ): List<VisualMatchResult> {
        val queryEmbedding = extractEmbedding(queryBitmap)
        val results = mutableListOf<VisualMatchResult>()

        for (image in storedImages) {
            val medicine = medicinesMap[image.medicineId] ?: continue

            val imageVector = if (!image.featureVector.isNullOrBlank()) {
                deserializeVector(image.featureVector)
            } else {
                // Compute from stored file if cached vector was null
                val loadedBitmap = ImageStorageHelper.loadBitmapFromFile(image.imagePath, 256)
                loadedBitmap?.let { extractEmbedding(it) }
            }

            if (imageVector != null) {
                val similarity = calculateSimilarity(queryEmbedding, imageVector)
                results.add(
                    VisualMatchResult(
                        medicine = medicine,
                        matchingImage = image,
                        similarityPercentage = similarity
                    )
                )
            }
        }

        // Sort descending by similarity percentage and take unique medicines
        val uniqueByMedicine = mutableMapOf<Long, VisualMatchResult>()
        results.sortedByDescending { it.similarityPercentage }.forEach { match ->
            val existing = uniqueByMedicine[match.medicine.id]
            if (existing == null || match.similarityPercentage > existing.similarityPercentage) {
                uniqueByMedicine[match.medicine.id] = match
            }
        }

        return uniqueByMedicine.values
            .sortedByDescending { it.similarityPercentage }
            .take(limit)
    }
}
