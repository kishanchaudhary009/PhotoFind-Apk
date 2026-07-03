package com.kyzn.photofindai.onnx

import android.content.Context
import org.json.JSONObject

class ClipTokenizer(private val context: Context) {
    private val vocab: Map<String, Int>
    private val maxTokenLength = 77
    private val startToken = 49406
    private val endToken = 49407

    init {
        val jsonString = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val tempVocab = mutableMapOf<String, Int>()
        jsonObject.keys().forEach { key ->
            tempVocab[key] = jsonObject.getInt(key)
        }
        vocab = tempVocab
    }

    /**
     * Simple tokenizer that splits by whitespace and looks up tokens.
     * Note: This is a simplified version. A full BPE tokenizer would 
     * handle sub-words using merges.txt.
     */
    fun tokenize(text: String): LongArray {
        val tokens = mutableListOf<Long>()
        tokens.add(startToken.toLong())

        val words = text.lowercase().split(Regex("\\s+"))
        for (word in words) {
            val token = vocab[word + "</w>"] ?: vocab[word] ?: 259 // Unknown token placeholder
            tokens.add(token.toLong())
        }

        tokens.add(endToken.toLong())

        // Pad or truncate to maxTokenLength
        val result = LongArray(maxTokenLength) { 0 }
        for (i in 0 until Math.min(tokens.size, maxTokenLength)) {
            result[i] = tokens[i]
        }
        return result
    }
}
