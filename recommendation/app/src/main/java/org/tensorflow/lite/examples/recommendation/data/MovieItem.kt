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
package org.tensorflow.lite.examples.recommendation.data

import android.text.TextUtils
import org.tensorflow.lite.examples.recommendation.data.MovieItem
import java.util.ArrayList

/** A movie item representing recommended content.  */
class MovieItem(val id: Int, val title: String, val genres: List<String?>, val count: Int) {
    @JvmField
    var selected = false // For UI selection. Default item is not selected.

    private constructor() : this(0, "", ArrayList<String?>(), 0) {}

    override fun toString(): String {
        return String.format(
            "Id: %d, title: %s, genres: %s, count: %d, selected: %s",
            id, title, TextUtils.join(JOINER, genres), count, selected
        )
    }

    companion object {
        const val JOINER = " | "
        const val DELIMITER_REGEX = "[|]"
    }
}