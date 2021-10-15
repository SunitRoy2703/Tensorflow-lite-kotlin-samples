/*
 * Copyright 2020 The TensorFlow Authors. All Rights Reserved.
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
package org.tensorflow.lite.examples.textclassification

import android.content.Context
import android.util.Log
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier
import org.tensorflow.lite.examples.textclassification.TextClassificationClient
import java.io.IOException
import java.util.*

/**
 * Load TfLite model and provide predictions with task api.
 */
class TextClassificationClient(private val context: Context) {
    @JvmField
    var classifier: NLClassifier? = null

    /**
     * Load TF Lite model.
     */
    fun load() {
        try {
            classifier = NLClassifier.createFromFile(context, MODEL_PATH)
        } catch (e: IOException) {
            Log.e(TAG, e.message)
        }
    }

    /**
     * Free up resources as the client is no longer needed.
     */
    fun unload() {
        classifier!!.close()
        classifier = null
    }

    /**
     * Classify an input string and returns the classification results.
     */
    fun classify(text: String?): List<Result> {
        val apiResults = classifier!!.classify(text)
        val results: MutableList<Result> = ArrayList(apiResults.size)
        for (i in apiResults.indices) {
            val category = apiResults[i]
            results.add(Result("" + i, category.label, category.score))
        }
        Collections.sort(results)
        return results
    }

    companion object {
        private const val TAG = "TaskApi"
        private const val MODEL_PATH = "text_classification.tflite"
    }
}