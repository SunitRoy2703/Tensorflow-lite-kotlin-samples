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

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.recommendation.data.FileUtil.loadConfig
import org.tensorflow.lite.examples.recommendation.data.FileUtil.loadMovieList
import org.tensorflow.lite.examples.recommendation.data.MovieItem
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

/** The main activity to provide interactions with users.  */
class MainActivity : AppCompatActivity(), MovieFragment.OnListFragmentInteractionListener,
    RecommendationFragment.OnListFragmentInteractionListener {

    private lateinit var config: Config
    private var client: RecommendationClient? = null
    private val allMovies: MutableList<MovieItem> = ArrayList()
    private val selectedMovies: MutableList<MovieItem> = ArrayList()
    private var handler: Handler? = null
    private var movieFragment: MovieFragment? = null
    private var recommendationFragment: RecommendationFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tfe_re_activity_main)
        Log.v(TAG, "onCreate")

        // Load config file.
        try {
            config = loadConfig(assets, CONFIG_PATH)
        } catch (ex: IOException) {
            Log.e(TAG, String.format("Error occurs when loading config %s: %s.", CONFIG_PATH, ex))
        }

        // Load movies list.
        try {
            allMovies.clear()
            allMovies.addAll(loadMovieList(assets, config!!.movieList))
        } catch (ex: IOException) {
            Log.e(
                TAG,
                String.format("Error occurs when loading movies %s: %s.", config!!.movieList, ex)
            )
        }
        client = RecommendationClient(this, config)
        handler = Handler()
        movieFragment =
            supportFragmentManager.findFragmentById(R.id.movie_fragment) as MovieFragment?
        recommendationFragment =
            supportFragmentManager.findFragmentById(R.id.recommendation_fragment) as RecommendationFragment?
    }

    override fun onStart() {
        super.onStart()
        Log.v(TAG, "onStart")

        // Add favorite movies to the fragment.
        val favoriteMovies = allMovies.stream().limit(config!!.favoriteListSize.toLong()).collect(
            Collectors.toList()
        )
        movieFragment!!.setMovies(favoriteMovies)
        handler!!.post { client!!.load() }
    }

    override fun onStop() {
        super.onStop()
        Log.v(TAG, "onStop")
        handler!!.post { client!!.unload() }
    }

    /** Sends selected movie list and get recommendations.  */
    private fun recommend(movies: List<MovieItem>) {
        handler!!.post {

            // Run inference with TF Lite.
            Log.d(TAG, "Run inference with TFLite model.")
            val recommendations = client!!.recommend(movies)

            // Show result on screen
            showResult(recommendations)
        }
    }

    /** Shows result on the screen.  */
    private fun showResult(recommendations: List<RecommendationClient.Result>) {
        // Run on UI thread as we'll updating our app UI
        runOnUiThread { recommendationFragment!!.setRecommendations(recommendations) }
    }

    override fun onItemSelectionChange(item: MovieItem?) {
        item ?: kotlin.run {
            Log.e("MainActivity", "item not found")
            return
        }
        if (item.selected) {
            if (!selectedMovies.contains(item)) {
                selectedMovies.add(item)
            }
        } else {
            selectedMovies.remove(item)
        }
        if (!selectedMovies.isEmpty()) {
            // Log selected movies.
            val sb = StringBuilder()
            sb.append("Select movies in the following order:\n")
            for (movie in selectedMovies) {
                sb.append(String.format("  movie: %s\n", movie))
            }
            Log.d(TAG, sb.toString())

            // Recommend based on selected movies.
            recommend(selectedMovies)
        } else {
            // Clear result list.
            showResult(ArrayList())
        }
    }

    /** Handles click event of recommended movie.  */
    override fun onClickRecommendedMovie(item: MovieItem?) {
        item ?: kotlin.run {
            Log.e("MainActivity", "item not found")
            return
        }
        // Show message for the clicked movie.
        val message = String.format("Clicked recommended movie: %s.", item.title)
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "OnDeviceRecommendationDemo"
        private const val CONFIG_PATH = "config.json" // Default config path in assets.
    }
}