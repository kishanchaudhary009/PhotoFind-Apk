package com.kyzn.photofindai.onnx

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ImageProcessor {
    companion object {
        private const val INPUT_SIZE = 224
        
        // CLIP Normalization Constants (ImageNet)
        private val MEAN = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
        private val STD = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

        /**
         * Preprocesses a Bitmap for the CLIP Image Encoder.
         * Resizes, center-crops, and normalizes the image.
         */
        fun preprocess(bitmap: Bitmap): FloatBuffer {
            val resizedBitmap = resizeAndCenterCrop(bitmap)
            val floatBuffer = ByteBuffer.allocateDirect(4 * 3 * INPUT_SIZE * INPUT_SIZE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
            resizedBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

            // Normalize pixels and put into buffer in NCHW format (Channel first)
            for (i in 0 until 3) {
                for (j in 0 until INPUT_SIZE * INPUT_SIZE) {
                    val pixel = intValues[j]
                    val value = when (i) {
                        0 -> (pixel shr 16 and 0xFF) / 255f // R
                        1 -> (pixel shr 8 and 0xFF) / 255f  // G
                        else -> (pixel and 0xFF) / 255f     // B
                    }
                    floatBuffer.put((value - MEAN[i]) / STD[i])
                }
            }
            floatBuffer.rewind()
            return floatBuffer
        }

        private fun resizeAndCenterCrop(bitmap: Bitmap): Bitmap {
            val size = Math.min(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            
            val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
            return Bitmap.createScaledBitmap(cropped, INPUT_SIZE, INPUT_SIZE, true)
        }
    }
}
