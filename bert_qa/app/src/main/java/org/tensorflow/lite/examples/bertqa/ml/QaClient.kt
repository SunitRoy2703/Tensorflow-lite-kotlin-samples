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
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.common.base.Joiner
import com.google.common.base.Verify
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.examples.bertqa.ml.ModelHelper.extractDictionary
import org.tensorflow.lite.examples.bertqa.ml.ModelHelper.loadModelFile
import org.tensorflow.lite.examples.bertqa.ml.QaAnswer
import org.tensorflow.lite.support.metadata.MetadataExtractor
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/** Interface to load TfLite model and provide predictions.  */
class QaClient(private val context: Context) : AutoCloseable {

    private val dic: MutableMap<String, Int> = HashMap()
    private val featureConverter: FeatureConverter
    private var tflite: Interpreter? = null
    private var metadataExtractor: MetadataExtractor? = null

    @WorkerThread
    @Synchronized
    fun loadModel() {
        try {
            val buffer: ByteBuffer = loadModelFile(context)
            metadataExtractor = MetadataExtractor(buffer)
            val loadedDic = extractDictionary(metadataExtractor)
            Verify.verify(loadedDic != null, "dic can't be null.")
            dic.putAll(loadedDic!!)
            val opt = Interpreter.Options()
            opt.setNumThreads(NUM_LITE_THREADS)
            tflite = Interpreter(buffer, opt)
            Log.v(TAG, "TFLite model loaded.")
        } catch (ex: IOException) {
            Log.e(TAG, ex.message)
        }
    }

    @WorkerThread
    @Synchronized
    fun unload() {
        close()
    }

    override fun close() {
        if (tflite != null) {
            tflite!!.close()
            tflite = null
        }
        dic.clear()
    }

    /**
     * Input: Original content and query for the QA task. Later converted to Feature by
     * FeatureConverter. Output: A String[] array of answers and a float[] array of corresponding
     * logits.
     */
    @WorkerThread
    @Synchronized
    fun predict(query: String?, content: String?): List<QaAnswer> {
        Log.v(TAG, "TFLite model: " + ModelHelper.MODEL_PATH + " running...")
        Log.v(TAG, "Convert Feature...")
        val feature = featureConverter.convert(query, content!!)
        Log.v(TAG, "Set inputs...")
        val inputIds = Array(1) { IntArray(MAX_SEQ_LEN) }
        val inputMask = Array(1) { IntArray(MAX_SEQ_LEN) }
        val segmentIds = Array(1) { IntArray(MAX_SEQ_LEN) }
        val startLogits = Array(1) { FloatArray(MAX_SEQ_LEN) }
        val endLogits = Array(1) { FloatArray(MAX_SEQ_LEN) }
        for (j in 0 until MAX_SEQ_LEN) {
            inputIds[0][j] = feature.inputIds[j]
            inputMask[0][j] = feature.inputMask[j]
            segmentIds[0][j] = feature.segmentIds[j]
        }
        val inputs = Array<Any>(3) { }
        var useInputMetadata = false
        if (metadataExtractor != null && metadataExtractor!!.inputTensorCount == 3) {
            // If metadata exists and the size of input tensors in metadata is 3, use metadata to treat
            // the tensor order. Since the order of input tensors can be different for different models,
            // set the inputs according to input tensor names.
            useInputMetadata = true
            for (i in 0..2) {
                val inputMetadata = metadataExtractor!!.getInputTensorMetadata(i)
                when (inputMetadata.name()) {
                    IDS_TENSOR_NAME -> inputs[i] = inputIds
                    MASK_TENSOR_NAME -> inputs[i] = inputMask
                    SEGMENT_IDS_TENSOR_NAME -> inputs[i] = segmentIds
                    else -> {
                        Log.e(
                            TAG,
                            "Input name in metadata doesn't match the default input tensor names."
                        )
                        useInputMetadata = false
                    }
                }
            }
        }
        if (!useInputMetadata) {
            // If metadata doesn't exists or doesn't contain the info, fail back to a hard-coded order.
            Log.v(TAG, "Use hard-coded order of input tensors.")
            inputs[0] = inputIds
            inputs[1] = inputMask
            inputs[2] = segmentIds
        }
        val output: MutableMap<Int, Any> = HashMap()
        // Hard-coded idx for output, maybe changed according to metadata below.
        var endLogitsIdx = 0
        var startLogitsIdx = 1
        var useOutputMetadata = false
        if (metadataExtractor != null && metadataExtractor!!.outputTensorCount == 2) {
            // If metadata exists and the size of output tensors in metadata is 2, use metadata to treat
            // the tensor order. Since the order of output tensors can be different for different models,
            // set the indexs of the outputs according to output tensor names.
            useOutputMetadata = true
            for (i in 0..1) {
                val outputMetadata = metadataExtractor!!.getOutputTensorMetadata(i)
                when (outputMetadata.name()) {
                    END_LOGITS_TENSOR_NAME -> endLogitsIdx = i
                    START_LOGITS_TENSOR_NAME -> startLogitsIdx = i
                    else -> {
                        Log.e(
                            TAG,
                            "Output name in metadata doesn't match the default output tensor names."
                        )
                        useOutputMetadata = false
                    }
                }
            }
        }
        if (!useOutputMetadata) {
            Log.v(TAG, "Use hard-coded order of output tensors.")
            endLogitsIdx = 0
            startLogitsIdx = 1
        }
        output[endLogitsIdx] = endLogits
        output[startLogitsIdx] = startLogits
        Log.v(TAG, "Run inference...")
        tflite!!.runForMultipleInputsOutputs(inputs, output)
        Log.v(TAG, "Convert answers...")
        val answers = getBestAnswers(startLogits[0], endLogits[0], feature)
        Log.v(TAG, "Finish.")
        return answers
    }

    /** Find the Best N answers & logits from the logits array and input feature.  */
    @Synchronized
    private fun getBestAnswers(
        startLogits: FloatArray, endLogits: FloatArray, feature: Feature
    ): List<QaAnswer> {
        // Model uses the closed interval [start, end] for indices.
        val startIndexes = getBestIndex(startLogits)
        val endIndexes = getBestIndex(endLogits)
        val origResults: MutableList<QaAnswer.Pos> = ArrayList()
        for (start in startIndexes) {
            for (end in endIndexes) {
                if (!feature.tokenToOrigMap.containsKey(start + OUTPUT_OFFSET)) {
                    continue
                }
                if (!feature.tokenToOrigMap.containsKey(end + OUTPUT_OFFSET)) {
                    continue
                }
                if (end < start) {
                    continue
                }
                val length = end - start + 1
                if (length > MAX_ANS_LEN) {
                    continue
                }
                origResults.add(QaAnswer.Pos(start, end, startLogits[start] + endLogits[end]))
            }
        }
        Collections.sort(origResults)
        val answers: MutableList<QaAnswer> = ArrayList()
        for (i in origResults.indices) {
            if (i >= PREDICT_ANS_NUM) {
                break
            }
            var convertedText: String
            convertedText = if (origResults[i].start > 0) {
                convertBack(feature, origResults[i].start, origResults[i].end)
            } else {
                ""
            }
            val ans = QaAnswer(convertedText, origResults[i])
            answers.add(ans)
        }
        return answers
    }

    /** Get the n-best logits from a list of all the logits.  */
    @WorkerThread
    @Synchronized
    private fun getBestIndex(logits: FloatArray): IntArray {
        val tmpList: MutableList<QaAnswer.Pos> = ArrayList()
        for (i in 0 until MAX_SEQ_LEN) {
            tmpList.add(QaAnswer.Pos(i, i, logits[i]))
        }
        Collections.sort(tmpList)
        val indexes = IntArray(PREDICT_ANS_NUM)
        for (i in 0 until PREDICT_ANS_NUM) {
            indexes[i] = tmpList[i].start
        }
        return indexes
    }

    companion object {
        private const val TAG = "BertDemo"
        private const val MAX_ANS_LEN = 32
        private const val MAX_QUERY_LEN = 64
        private const val MAX_SEQ_LEN = 384
        private const val DO_LOWER_CASE = true
        private const val PREDICT_ANS_NUM = 5
        private const val NUM_LITE_THREADS = 4
        private const val IDS_TENSOR_NAME = "ids"
        private const val MASK_TENSOR_NAME = "mask"
        private const val SEGMENT_IDS_TENSOR_NAME = "segment_ids"
        private const val END_LOGITS_TENSOR_NAME = "end_logits"
        private const val START_LOGITS_TENSOR_NAME = "start_logits"

        // Need to shift 1 for outputs ([CLS]).
        private const val OUTPUT_OFFSET = 1
        private val SPACE_JOINER = Joiner.on(" ")

        /** Convert the answer back to original text form.  */
        @WorkerThread
        private fun convertBack(
            feature: Feature,
            start: Int,
            end: Int
        ): String {
            // Shifted index is: index of logits + offset.
            val shiftedStart = start + OUTPUT_OFFSET
            val shiftedEnd = end + OUTPUT_OFFSET
            val startIndex = feature.tokenToOrigMap[shiftedStart]!!
            val endIndex = feature.tokenToOrigMap[shiftedEnd]!!
            // end + 1 for the closed interval.
            return SPACE_JOINER.join(feature.origTokens.subList(startIndex, endIndex + 1))
        }
    }

    init {
        featureConverter = FeatureConverter(dic, DO_LOWER_CASE, MAX_QUERY_LEN, MAX_SEQ_LEN)
    }
}