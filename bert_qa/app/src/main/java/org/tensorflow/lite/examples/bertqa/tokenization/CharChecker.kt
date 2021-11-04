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

/** To check whether a char is whitespace/control/punctuation.  */
internal object CharChecker {

    /** To judge whether it's an empty or unknown character.  */
    fun isInvalid(ch: Char): Boolean {
        return ch.toInt() == 0 || ch.toInt() == 0xfffd
    }

    /** To judge whether it's a control character(exclude whitespace).  */
    fun isControl(ch: Char): Boolean {
        if (Character.isWhitespace(ch)) {
            return false
        }
        val type = Character.getType(ch)
        return type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt()
    }

    /** To judge whether it can be regarded as a whitespace.  */
    fun isWhitespace(ch: Char): Boolean {
        if (Character.isWhitespace(ch)) {
            return true
        }
        val type = Character.getType(ch)
        return type == Character.SPACE_SEPARATOR.toInt() || type == Character.LINE_SEPARATOR.toInt() || type == Character.PARAGRAPH_SEPARATOR.toInt()
    }

    /** To judge whether it's a punctuation.  */
    fun isPunctuation(ch: Char): Boolean {
        val type = Character.getType(ch)
        return type == Character.CONNECTOR_PUNCTUATION.toInt() || type == Character.DASH_PUNCTUATION.toInt() || type == Character.START_PUNCTUATION.toInt() || type == Character.END_PUNCTUATION.toInt() || type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() || type == Character.FINAL_QUOTE_PUNCTUATION.toInt() || type == Character.OTHER_PUNCTUATION.toInt()
    }
}