/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tensorflow.lite.examples.imagesegmentation


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.examples.imagesegmentation.tflite.ImageSegmentationModelExecutor
import org.tensorflow.lite.examples.imagesegmentation.tflite.ModelExecutionResult
import java.io.IOException
import java.io.InputStream
import java.util.*

/** Golden test for Image Segmentation Reference app.  */
@RunWith(AndroidJUnit4::class)
class ImageSegmentationModelExecutorTest {
    @Test
    @Throws(IOException::class)
    fun executeResultsShouldNotChange() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val segmenter = ImageSegmentationModelExecutor(context, false)
        val input = loadImage(INPUT)
        val result: ModelExecutionResult = segmenter.execute(input)

        // Verify the output mask bitmap.
        if (ImageSegmentationModelExecutor.TAG == "SegmentationTask") {
            val goldenOutputFileName = GOLDEN_OUTPUTS_TASK
            val goldenPixels = getPixels(loadImage(goldenOutputFileName))
            val resultPixels = getPixels(result.bitmapMaskOnly)

            Truth.assertThat(resultPixels).hasLength(goldenPixels.size)
            var inconsistentPixels = 0
            for (i in resultPixels.indices) {
                if (resultPixels[i] != goldenPixels[i]) {
                    inconsistentPixels++
                }
            }
            Truth.assertThat(inconsistentPixels.toDouble() / resultPixels.size)
                .isLessThan(GOLDEN_MASK_TOLERANCE)
        }

        // Verify labels.
        val resultLabels: MutableSet<String?> = HashSet()
        for (itemName in result.itemsFound.keys) {
            resultLabels.add(itemName)
        }
        val goldenLabels: MutableSet<String?> = HashSet()
        Collections.addAll(goldenLabels, *goldenLabelArray)
        Truth.assertThat(resultLabels).isEqualTo(goldenLabels)
    }

    companion object {
        private const val INPUT = "input_image.jpg"
        private const val GOLDEN_OUTPUTS_TASK = "golden_output_task.png"
        private const val GOLDEN_MASK_TOLERANCE = 1e-2
        private val goldenLabelArray = arrayOf("background", "person", "horse")
        private fun getPixels(bitmap: Bitmap): IntArray {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            return pixels
        }

        private fun loadImage(fileName: String): Bitmap {
            val assetManager = InstrumentationRegistry.getInstrumentation().context.assets
            var inputStream: InputStream? = null
            try {
                inputStream = assetManager.open(fileName)
            } catch (e: IOException) {
                Log.e("Test", "Cannot load image from assets")
            }
            return BitmapFactory.decodeStream(inputStream)
        }
    }
}