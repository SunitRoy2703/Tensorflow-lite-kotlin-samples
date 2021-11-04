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

import org.tensorflow.lite.examples.bertqa.tokenization.FullTokenizer
import java.util.*

/** Convert String to features that can be fed into BERT model.  */
class FeatureConverter(
    inputDic: Map<String, Int>, doLowerCase: Boolean, maxQueryLen: Int, maxSeqLen: Int
) {

    private val tokenizer: FullTokenizer
    private val maxQueryLen: Int
    private val maxSeqLen: Int

    fun convert(query: String?, context: String): Feature {
        var queryTokens: List<String?> = tokenizer.tokenize(query)
        if (queryTokens.size > maxQueryLen) {
            queryTokens = queryTokens.subList(0, maxQueryLen)
        }
        val origTokens =
            Arrays.asList(*context.trim { it <= ' ' }.split("\\s+".toRegex()).toTypedArray())
        val tokenToOrigIndex: MutableList<Int> = ArrayList()
        var allDocTokens: MutableList<String?> = ArrayList()
        for (i in origTokens.indices) {
            val token = origTokens[i]
            val subTokens = tokenizer.tokenize(token)
            for (subToken in subTokens) {
                tokenToOrigIndex.add(i)
                allDocTokens.add(subToken)
            }
        }

        // -3 accounts for [CLS], [SEP] and [SEP].
        val maxContextLen = maxSeqLen - queryTokens.size - 3
        if (allDocTokens.size > maxContextLen) {
            allDocTokens = allDocTokens.subList(0, maxContextLen)
        }
        val tokens: MutableList<String> = ArrayList()
        val segmentIds: MutableList<Int> = ArrayList()

        // Map token index to original index (in feature.origTokens).
        val tokenToOrigMap: MutableMap<Int, Int> = HashMap()

        // Start of generating the features.
        tokens.add("[CLS]")
        segmentIds.add(0)

        // For query input.
        for (queryToken in queryTokens) {
            queryToken ?: continue
            tokens.add(queryToken)
            segmentIds.add(0)
        }

        // For Separation.
        tokens.add("[SEP]")
        segmentIds.add(0)

        // For Text Input.
        for (i in allDocTokens.indices) {
            val docToken = allDocTokens[i]
            docToken ?: continue
            tokens.add(docToken)
            segmentIds.add(1)
            tokenToOrigMap[tokens.size] = tokenToOrigIndex[i]
        }

        // For ending mark.
        tokens.add("[SEP]")
        segmentIds.add(1)
        val inputIds = tokenizer.convertTokensToIds(tokens.toList())
        val inputMask: MutableList<Int?> = ArrayList(Collections.nCopies(inputIds.size, 1))
        while (inputIds.size < maxSeqLen) {
            inputIds.add(0)
            inputMask.add(0)
            segmentIds.add(0)
        }
        return Feature(inputIds, inputMask, segmentIds, origTokens, tokenToOrigMap)
    }

    init {
        tokenizer = FullTokenizer(inputDic, doLowerCase)
        this.maxQueryLen = maxQueryLen
        this.maxSeqLen = maxSeqLen
    }
}