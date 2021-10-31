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
package org.tensorflow.lite.examples.bertqa.tokenization

import java.util.*

/**
 * A java realization of Bert tokenization. Original python code:
 * https://github.com/google-research/bert/blob/master/tokenization.py runs full tokenization to
 * tokenize a String into split subtokens or ids.
 */
class FullTokenizer(private val dic: Map<String, Int>, doLowerCase: Boolean) {

    private val basicTokenizer: BasicTokenizer
    private val wordpieceTokenizer: WordpieceTokenizer

    fun tokenize(text: String?): List<String> {
        val splitTokens: MutableList<String> = ArrayList()
        for (token in basicTokenizer.tokenize(text)) {
            splitTokens.addAll(wordpieceTokenizer.tokenize(token))
        }
        return splitTokens
    }

    fun convertTokensToIds(tokens: List<String>): MutableList<Int?> {
        val outputIds: MutableList<Int?> = ArrayList()
        for (token in tokens) {
            outputIds.add(dic[token])
        }
        return outputIds
    }

    init {
        basicTokenizer = BasicTokenizer(doLowerCase)
        wordpieceTokenizer = WordpieceTokenizer(dic)
    }
}