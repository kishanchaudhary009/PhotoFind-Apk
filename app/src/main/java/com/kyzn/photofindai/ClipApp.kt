package com.kyzn.photofindai

import android.app.Application
import com.kyzn.photofindai.data.EmbeddingStore
import com.kyzn.photofindai.data.ImageEmbedding
import com.kyzn.photofindai.onnx.ClipModel
import com.kyzn.photofindai.onnx.ClipTokenizer

class ClipApp : Application() {
    
    // Shared ML components and data
    lateinit var clipModel: com.kyzn.photofindai.onnx.ClipModel
    lateinit var clipTokenizer: com.kyzn.photofindai.onnx.ClipTokenizer
    
    // Persistent storage for indexed embeddings
    lateinit var embeddingStore: com.kyzn.photofindai.data.EmbeddingStore
    val indexedEmbeddings = mutableListOf<com.kyzn.photofindai.data.ImageEmbedding>()
    
    @Volatile
    var isIndexing = false

    override fun onCreate() {
        super.onCreate()
        embeddingStore = _root_ide_package_.com.kyzn.photofindai.data.EmbeddingStore(this)
        // Load existing embeddings from disk
        indexedEmbeddings.addAll(embeddingStore.loadEmbeddings())
    }
    
    fun initML() {
        if (!::clipModel.isInitialized) {
            clipModel = _root_ide_package_.com.kyzn.photofindai.onnx.ClipModel(this)
            clipTokenizer = _root_ide_package_.com.kyzn.photofindai.onnx.ClipTokenizer(this)
        }
    }

    fun saveToDisk() {
        embeddingStore.saveEmbeddings(indexedEmbeddings)
    }
}
