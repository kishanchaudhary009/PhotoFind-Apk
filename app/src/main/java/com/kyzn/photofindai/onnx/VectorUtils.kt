package com.kyzn.photofindai.onnx

import kotlin.math.sqrt

object VectorUtils {
    /**
     * Calculates the cosine similarity between two vectors.
     * Higher values (closer to 1.0) mean more similar.
     */
    fun cosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}
