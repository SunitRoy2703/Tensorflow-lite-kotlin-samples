/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package org.tensorflow.lite.examples.bertqa.ml

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.common.base.Verify
import org.tensorflow.lite.support.metadata.MetadataExtractor
import java.io.*
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

/** Helper to load TfLite model and dictionary.  */
object ModelHelper {

    private const val TAG = "BertDemo"
    const val MODEL_PATH = "model.tflite"
    const val DIC_PATH = "vocab.txt"

    /** Load tflite model from context.  */
    @Throws(IOException::class)
    fun loadModelFile(context: Context): MappedByteBuffer {
        return loadModelFile(context.assets)
    }

    /** Load tflite model from assets.  */
    @Throws(IOException::class)
    fun loadModelFile(assetManager: AssetManager): MappedByteBuffer {
        assetManager.openFd(MODEL_PATH).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        }
    }

    /** Extract dictionary from metadata.  */
    @JvmStatic
    fun extractDictionary(metadataExtractor: MetadataExtractor?): Map<String, Int>? {
        var dic: Map<String, Int>? = null
        try {
            Verify.verify(metadataExtractor != null, "metadataExtractor can't be null.")
            dic = loadDictionaryFile(metadataExtractor!!.getAssociatedFile(DIC_PATH))
            Log.v(TAG, "Dictionary loaded.")
        } catch (ex: IOException) {
            Log.e(TAG, ex.message)
        }
        return dic
    }

    /** Load dictionary from assets.  */
    @Throws(IOException::class)
    fun loadDictionaryFile(inputStream: InputStream?): Map<String, Int> {
        val dic: MutableMap<String, Int> = HashMap()
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var index = 0
            while (reader.ready()) {
                val key = reader.readLine()
                dic[key] = index++
            }
        }
        return dic
    }
}