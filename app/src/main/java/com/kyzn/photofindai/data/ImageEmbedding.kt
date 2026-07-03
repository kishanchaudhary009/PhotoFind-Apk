package com.kyzn.photofindai.data

/**
 * Data class to store an image path and its CLIP embedding.
 */
data class ImageEmbedding(
    val path: String,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ImageEmbedding
        if (path != other.path) return false
        if (!embedding.contentEquals(other.embedding)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
