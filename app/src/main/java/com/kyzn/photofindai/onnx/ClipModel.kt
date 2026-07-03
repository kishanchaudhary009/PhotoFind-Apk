package com.kyzn.photofindai.onnx

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

class ClipModel(private val context: Context) {
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var imageSession: OrtSession? = null
    private var textSession: OrtSession? = null

    init {
        try {
            // Load Image Encoder
            val imageModelBytes = context.assets.open("image_encoder_quant.onnx").readBytes()
            imageSession = ortEnv.createSession(imageModelBytes)

            // Load Text Encoder
            val textModelBytes = context.assets.open("text_encoder_quant.onnx").readBytes()
            textSession = ortEnv.createSession(textModelBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            // In a production app, handle this error (e.g., show an error dialog)
        }
    }

    /**
     * Encodes an image into a vector representation.
     * @param imageBuffer Preprocessed image data as a FloatBuffer.
     * @return The image embedding.
     */
    fun encodeImage(imageBuffer: FloatBuffer, dims: LongArray): FloatArray? {
        val inputName = imageSession?.inputNames?.firstOrNull() ?: return null
        val tensor = OnnxTensor.createTensor(ortEnv, imageBuffer, dims)
        
        val result = imageSession?.run(mapOf(inputName to tensor))
        val output = result?.get(0)?.value as? Array<FloatArray>
        return output?.firstOrNull()
    }

    /**
     * Encodes text into a vector representation.
     * @param tokenIds Preprocessed text tokens as a LongBuffer.
     * @return The text embedding.
     */
    fun encodeText(tokenIds: LongArray): FloatArray? {
        val inputName = textSession?.inputNames?.firstOrNull() ?: return null
        val tensor = OnnxTensor.createTensor(ortEnv, arrayOf(tokenIds))
        
        val result = textSession?.run(mapOf(inputName to tensor))
        val output = result?.get(0)?.value as? Array<FloatArray>
        return output?.firstOrNull()
    }

    fun close() {
        imageSession?.close()
        textSession?.close()
        ortEnv.close()
    }
}
