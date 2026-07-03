package com.kyzn.photofindai.data

import android.content.Context
import android.util.Log
import java.io.*

data class SerializableEmbedding(val path: String, val embedding: FloatArray) : Serializable

class EmbeddingStore(private val context: Context) {
    private val fileName = "embeddings_data.bin"
    private val file = File(context.filesDir, fileName)
    private val TAG = "EmbeddingStore"

    fun saveEmbeddings(embeddings: List<ImageEmbedding>) {
        try {
            val serializable = embeddings.map { SerializableEmbedding(it.path, it.embedding) }
            ObjectOutputStream(FileOutputStream(file)).use { 
                it.writeObject(serializable)
            }
            Log.d(TAG, "Successfully saved ${embeddings.size} embeddings to disk.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving embeddings", e)
        }
    }

    fun loadEmbeddings(): MutableList<ImageEmbedding> {
        if (!file.exists()) {
            Log.d(TAG, "No embedding file found.")
            return mutableListOf()
        }
        return try {
            ObjectInputStream(FileInputStream(file)).use {
                val list = it.readObject() as List<SerializableEmbedding>
                Log.d(TAG, "Successfully loaded ${list.size} embeddings from disk.")
                list.map { se -> ImageEmbedding(se.path, se.embedding) }.toMutableList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading embeddings", e)
            mutableListOf()
        }
    }
}
