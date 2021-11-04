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

import com.google.common.base.Ascii
import com.google.common.collect.Iterables
import java.util.*

/** Basic tokenization (punctuation splitting, lower casing, etc.)  */
class BasicTokenizer(private val doLowerCase: Boolean) {

    fun tokenize(text: String?): List<String> {
        val cleanedText = cleanText(text)
        val origTokens = whitespaceTokenize(cleanedText)
        val stringBuilder = StringBuilder()
        origTokens.forEach { token ->
            val newToken = if (doLowerCase) {
                Ascii.toLowerCase(token)
            } else {
                token
            }
            val list = runSplitOnPunc(newToken)
            for (subToken in list) {
                stringBuilder.append(subToken).append(" ")
            }
        }
        return whitespaceTokenize(stringBuilder.toString())
    }

    companion object {
        /* Performs invalid character removal and whitespace cleanup on text. */
        @JvmStatic
        fun cleanText(text: String?): String {
            if (text == null) {
                throw NullPointerException("The input String is null.")
            }
            val stringBuilder = StringBuilder("")
            for (index in 0 until text.length) {
                val ch = text[index]

                // Skip the characters that cannot be used.
                if (CharChecker.isInvalid(ch) || CharChecker.isControl(ch)) {
                    continue
                }
                if (CharChecker.isWhitespace(ch)) {
                    stringBuilder.append(" ")
                } else {
                    stringBuilder.append(ch)
                }
            }
            return stringBuilder.toString()
        }

        /* Runs basic whitespace cleaning and splitting on a piece of text. */
        @JvmStatic
        fun whitespaceTokenize(text: String?): List<String> {
            if (text == null) {
                throw NullPointerException("The input String is null.")
            }
            return Arrays.asList(*text.split(" ".toRegex()).toTypedArray())
        }

        /* Splits punctuation on a piece of text. */
        @JvmStatic
        fun runSplitOnPunc(text: String?): List<String> {
            if (text == null) {
                throw NullPointerException("The input String is null.")
            }
            val tokens: MutableList<String> = ArrayList()
            var startNewWord = true
            for (i in 0 until text.length) {
                val ch = text[i]
                if (CharChecker.isPunctuation(ch)) {
                    tokens.add(ch.toString())
                    startNewWord = true
                } else {
                    if (startNewWord) {
                        tokens.add("")
                        startNewWord = false
                    }
                    tokens[tokens.size - 1] = Iterables.getLast(tokens) + ch
                }
            }
            return tokens
        }
    }
}