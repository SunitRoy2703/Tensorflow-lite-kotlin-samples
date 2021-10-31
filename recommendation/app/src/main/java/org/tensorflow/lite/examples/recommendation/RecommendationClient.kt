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

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.examples.recommendation.data.FileUtil.loadGenreList
import org.tensorflow.lite.examples.recommendation.data.FileUtil.loadModelFile
import org.tensorflow.lite.examples.recommendation.data.FileUtil.loadMovieList
import org.tensorflow.lite.examples.recommendation.data.MovieItem
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/** Interface to load TfLite model and provide recommendations.  */
class RecommendationClient(private val context: Context, private val config: Config) {

    var tflite: Interpreter? = null
        private set
    val candidates: MutableMap<Int, MovieItem> = HashMap()
    val genres: MutableMap<String?, Int> = HashMap()

    /** An immutable result returned by a RecommendationClient.  */
    class Result(
        /** Predicted id.  */
        val id: Int,
        /** Recommended item.  */
        val item: MovieItem,
        /** A sortable score for how good the result is relative to others. Higher should be better.  */
        val confidence: Float
    ) {
        override fun toString(): String {
            return String.format("[%d] confidence: %.3f, item: %s", id, confidence, item)
        }
    }

    /** Load the TF Lite model and dictionary.  */
    @WorkerThread
    fun load() {
        loadModel()
        loadCandidateList()
        if (config.useGenres()) {
            loadGenreList()
        }
    }

    /** Load TF Lite model.  */
    @WorkerThread
    @Synchronized
    private fun loadModel() {
        try {
            val buffer: ByteBuffer = loadModelFile(context.assets, config.model)
            tflite = Interpreter(buffer)
            Log.v(TAG, "TFLite model loaded.")
        } catch (ex: IOException) {
            Log.e(TAG, ex.message)
        }
    }

    /** Load recommendation candidate list.  */
    @WorkerThread
    @Synchronized
    private fun loadCandidateList() {
        try {
            val collection = loadMovieList(context.assets, config.movieList)
            candidates.clear()
            for (item in collection) {
                Log.d(TAG, String.format("Load candidate: %s", item))
                candidates[item.id] = item
            }
            Log.v(TAG, "Candidate list loaded.")
        } catch (ex: IOException) {
            Log.e(TAG, ex.message)
        }
    }

    /** Load movie genre list.  */
    @WorkerThread
    @Synchronized
    private fun loadGenreList() {
        try {
            val genreList = loadGenreList(context.assets, config.genreList!!)
            genres.clear()
            for (genre in genreList) {
                Log.d(TAG, String.format("Load genre: \"%s\"", genre))
                genres[genre] = genres.size
            }
            Log.v(TAG, "Candidate list loaded.")
        } catch (ex: IOException) {
            Log.e(TAG, ex.message)
        }
    }

    /** Free up resources as the client is no longer needed.  */
    @WorkerThread
    @Synchronized
    fun unload() {
        tflite!!.close()
        candidates.clear()
    }

    fun preprocessIds(selectedMovies: List<MovieItem>, length: Int): IntArray {
        val inputIds = IntArray(length)
        Arrays.fill(inputIds, config.pad) // Fill inputIds with the default.
        var i = 0
        for (item in selectedMovies) {
            if (i >= inputIds.size) {
                break
            }
            inputIds[i] = item.id
            ++i
        }
        return inputIds
    }

    fun preprocessGenres(selectedMovies: List<MovieItem>, length: Int): IntArray {
        // Fill inputGenres.
        val inputGenres = IntArray(length)
        Arrays.fill(inputGenres, config.unknownGenre) // Fill inputGenres with the default.
        var i = 0
        for (item in selectedMovies) {
            if (i >= inputGenres.size) {
                break
            }
            for (genre in item.genres) {
                if (i >= inputGenres.size) {
                    break
                }
                inputGenres[i] =
                    if (genres.containsKey(genre)) genres[genre]!! else config.unknownGenre
                ++i
            }
        }
        return inputGenres
    }

    /** Given a list of selected items, preprocess to get tflite input.  */
    @WorkerThread
    @Synchronized
    fun preprocess(selectedMovies: List<MovieItem>): Array<Any> {
        val inputs: MutableList<Any> = ArrayList()

        // Sort features.
        val sortedFeatures: List<Config.Feature> = ArrayList(
            config.inputs
        )
        Collections.sort(sortedFeatures) { a: Config.Feature, b: Config.Feature ->
            Integer.compare(
                a.index,
                b.index
            )
        }
        for (feature in sortedFeatures) {
            if (Config.FEATURE_MOVIE == feature.name) {
                inputs.add(preprocessIds(selectedMovies, feature.inputLength))
            } else if (Config.FEATURE_GENRE == feature.name) {
                inputs.add(preprocessGenres(selectedMovies, feature.inputLength))
            } else {
                Log.e(TAG, String.format("Invalid feature: %s", feature.name))
            }
        }
        return inputs.toTypedArray()
    }

    /** Postprocess to gets results from tflite inference.  */
    @WorkerThread
    @Synchronized
    fun postprocess(
        outputIds: IntArray, confidences: FloatArray, selectedMovies: List<MovieItem>
    ): List<Result> {
        val results = ArrayList<Result>()

        // Add recommendation results. Filter null or contained items.
        for (i in outputIds.indices) {
            if (results.size >= config.topK) {
                Log.v(TAG, String.format("Selected top K: %d. Ignore the rest.", config.topK))
                break
            }
            val id = outputIds[i]
            val item = candidates[id]
            if (item == null) {
                Log.v(TAG, String.format("Inference output[%d]. Id: %s is null", i, id))
                continue
            }
            if (selectedMovies.contains(item)) {
                Log.v(TAG, String.format("Inference output[%d]. Id: %s is contained", i, id))
                continue
            }
            val result = Result(id, item, confidences[i])
            results.add(result)
            Log.v(TAG, String.format("Inference output[%d]. Result: %s", i, result))
        }
        return results
    }

    /** Given a list of selected items, and returns the recommendation results.  */
    @WorkerThread
    @Synchronized
    fun recommend(selectedMovies: List<MovieItem>): List<Result> {
        val inputs = preprocess(selectedMovies)

        // Run inference.
        val outputIds = IntArray(config.outputLength)
        val confidences = FloatArray(config.outputLength)
        val outputs: MutableMap<Int, Any> = HashMap()
        outputs[config.outputIdsIndex] = outputIds
        outputs[config.outputScoresIndex] = confidences
        tflite!!.runForMultipleInputsOutputs(inputs, outputs)
        return postprocess(outputIds, confidences, selectedMovies)
    }

    companion object {
        private const val TAG = "RecommendationClient"
    }

    init {
        if (!config.validate()) {
            Log.e(TAG, "Config is not valid.")
        }
    }
}