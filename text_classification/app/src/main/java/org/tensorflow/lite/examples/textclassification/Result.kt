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

/**
 * An immutable result returned by a TextClassifier describing what was classified.
 */
class Result(
    /**
     * A unique identifier for what has been classified. Specific to the class, not the instance of
     * the object.
     */
    val id: String?,
    /**
     * Display name for the result.
     */
    val title: String?,
    /**
     * A sortable score for how good the result is relative to others. Higher should be better.
     */
    val confidence: Float?
) : Comparable<Result> {

    override fun toString(): String {
        var resultString = ""
        if (id != null) {
            resultString += "[$id] "
        }
        if (title != null) {
            resultString += "$title "
        }
        if (confidence != null) {
            resultString += String.format("(%.1f%%) ", confidence * 100.0f)
        }
        return resultString.trim { it <= ' ' }
    }

    override fun compareTo(o: Result): Int {
        return o.confidence!!.compareTo(confidence!!)
    }
}