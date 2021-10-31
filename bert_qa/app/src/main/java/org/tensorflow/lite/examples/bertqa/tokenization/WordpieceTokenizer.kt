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

import org.tensorflow.lite.examples.bertqa.tokenization.BasicTokenizer.Companion.whitespaceTokenize
import java.util.*

/** Word piece tokenization to split a piece of text into its word pieces.  */
class WordpieceTokenizer(private val dic: Map<String, Int>) {

    /**
     * Tokenizes a piece of text into its word pieces. This uses a greedy longest-match-first
     * algorithm to perform tokenization using the given vocabulary. For example: input = "unaffable",
     * output = ["un", "##aff", "##able"].
     *
     * @param text: A single token or whitespace separated tokens. This should have already been
     * passed through `BasicTokenizer.
     * @return A list of wordpiece tokens.
     */
    fun tokenize(text: String?): List<String> {
        if (text == null) {
            throw NullPointerException("The input String is null.")
        }
        val outputTokens: MutableList<String> = ArrayList()
        for (token in whitespaceTokenize(text)) {
            if (token.length > MAX_INPUTCHARS_PER_WORD) {
                outputTokens.add(UNKNOWN_TOKEN)
                continue
            }
            var isBad = false // Mark if a word cannot be tokenized into known subwords.
            var start = 0
            val subTokens: MutableList<String> = ArrayList()
            while (start < token.length) {
                var curSubStr = ""
                var end = token.length // Longer substring matches first.
                while (start < end) {
                    val subStr =
                        if (start == 0) token.substring(start, end) else "##" + token.substring(
                            start,
                            end
                        )
                    if (dic.containsKey(subStr)) {
                        curSubStr = subStr
                        break
                    }
                    end--
                }

                // The word doesn't contain any known subwords.
                if ("" == curSubStr) {
                    isBad = true
                    break
                }

                // curSubStr is the longeset subword that can be found.
                subTokens.add(curSubStr)

                // Proceed to tokenize the resident string.
                start = end
            }
            if (isBad) {
                outputTokens.add(UNKNOWN_TOKEN)
            } else {
                outputTokens.addAll(subTokens)
            }
        }
        return outputTokens
    }

    companion object {
        private const val UNKNOWN_TOKEN = "[UNK]" // For unknown words.
        private const val MAX_INPUTCHARS_PER_WORD = 200
    }
}