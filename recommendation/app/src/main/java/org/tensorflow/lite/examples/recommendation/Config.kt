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
package org.tensorflow.lite.examples.recommendation

import android.util.Log
import java.util.ArrayList

/** Config for recommendation app.  */
class Config {
    /** Feature of the model.  */
    class Feature {
        /** Input feature name.  */
        @JvmField
        var name: String? = null

        /** Input feature index.  */
        @JvmField
        var index = 0

        /** Input feature length.  */
        @JvmField
        var inputLength = 0
    }

    /** TF Lite model path.  */
    @JvmField
    var model = DEFAULT_MODEL_PATH

    /** List of input features  */
    @JvmField
    var inputs: List<Feature> = ArrayList()

    /** Number of output length from the model.  */
    @JvmField
    var outputLength = DEFAULT_OUTPUT_LENGTH

    /** Number of max results to show in the UI.  */
    @JvmField
    var topK = DEFAULT_TOP_K

    /** Path to the movie list.  */
    @JvmField
    var movieList = DEFAULT_MOVIE_LIST_PATH

    /** Path to the genre list. Use genre feature if it is not null.  */
    @JvmField
    var genreList: String? = null

    /** Id for padding.  */
    @JvmField
    var pad = PAD_ID

    /** Movie genre for unknown.  */
    @JvmField
    var unknownGenre = UNKNOWN_GENRE

    /** Output index for ID.  */
    @JvmField
    var outputIdsIndex = DEFAULT_OUTPUT_IDS_INDEX

    /** Output index for score.  */
    @JvmField
    var outputScoresIndex = DEFAULT_OUTPUT_SCORES_INDEX

    /** The number of favorite movies for users to choose from.  */
    @JvmField
    var favoriteListSize = DEFAULT_FAVORITE_LIST_SIZE
    fun validate(): Boolean {
        if (inputs.isEmpty()) {
            Log.e(TAG, "config inputs should not be empty")
            return false
        }
        var hasGenreFeature = false
        for (feature in inputs) {
            if (FEATURE_GENRE == feature.name) {
                hasGenreFeature = true
                break
            }
        }
        if (useGenres() || hasGenreFeature) {
            if (!useGenres() || !hasGenreFeature) {
                var msg =
                    "If uses genre, must set both `genreFeature` in inputs and `genreList` as vocab."
                if (!useGenres()) {
                    msg += "`genreList` is missing."
                }
                if (!hasGenreFeature) {
                    msg += "`genreFeature` is missing."
                }
                Log.e(TAG, msg)
                return false
            }
        }
        return true
    }

    fun useGenres(): Boolean {
        return genreList != null
    }

    companion object {
        private const val TAG = "Config"
        private const val DEFAULT_MODEL_PATH = "recommendation_rnn_i10o100.tflite"
        private const val DEFAULT_MOVIE_LIST_PATH = "sorted_movie_vocab.json"
        private const val DEFAULT_OUTPUT_LENGTH = 100
        private const val DEFAULT_TOP_K = 10
        private const val PAD_ID = 0
        private const val UNKNOWN_GENRE = 0
        private const val DEFAULT_OUTPUT_IDS_INDEX = 0
        private const val DEFAULT_OUTPUT_SCORES_INDEX = 1
        private const val DEFAULT_FAVORITE_LIST_SIZE = 100
        const val FEATURE_MOVIE = "movieFeature"
        const val FEATURE_GENRE = "genreFeature"
    }
}