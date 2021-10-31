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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.recommendation.data.MovieItem

/**
 * RecommendationRecyclerViewAdapter: a [RecyclerView.Adapter] that can display a recommended
 * [MovieItem] and makes a call to the specified [OnListFragmentInteractionListener].
 */
class RecommendationRecyclerViewAdapter(
    private val results: List<RecommendationClient.Result>,
    private val listener: RecommendationFragment.OnListFragmentInteractionListener?
) : RecyclerView.Adapter<RecommendationRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tfe_re_fragment_recommendation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = results[position].item
        holder.result = results[position]
        holder.recommendationMovieTitleView.text = item.title + " - " + item.genres
        holder.scoreView.text = String.format("[%d]", item.id)
        holder.view.setOnClickListener { listener?.onClickRecommendedMovie(item) }
    }

    override fun getItemCount(): Int {
        return results.size
    }

    /** ViewHolder to display one movie in list view of recommendation result.  */
    class ViewHolder(val view: View) : RecyclerView.ViewHolder(
        view
    ) {
        val scoreView: TextView
        val recommendationMovieTitleView: TextView
        var result: RecommendationClient.Result? = null
        override fun toString(): String {
            return super.toString() + " '" + recommendationMovieTitleView.text + "'"
        }

        init {
            scoreView = view.findViewById<View>(R.id.recommendation_score) as TextView
            recommendationMovieTitleView =
                view.findViewById<View>(R.id.recommendation_movie_title) as TextView
        }
    }
}