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
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.recommendation.data.MovieItem

/**
 * MovieRecyclerViewAdapter: a [RecyclerView.Adapter] that can display a [MovieItem] for
 * users to select from, and makes a call to the specified [ ].
 */
class MovieRecyclerViewAdapter(
    private val values: List<MovieItem>,
    private val listener: MovieFragment.OnListFragmentInteractionListener
) : RecyclerView.Adapter<MovieRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tfe_re_fragment_selection, parent, false)
        return ViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.item = values[position]
        holder.movieSwitch.setOnClickListener { v ->
            val sw = v as Switch
            // Use checked status.
            val selected = sw.isChecked
            holder.setSelected(selected)
        }
        holder.movieTitle.text = values[position].title
        holder.view.setOnClickListener { // Toggle checked status.
            val selected = !holder.movieSwitch.isChecked
            holder.setSelected(selected)
        }
    }

    override fun getItemCount(): Int {
        return values.size
    }

    /** ViewHolder to display one movie in list view of the selection.  */
    class ViewHolder(val view: View, listener: MovieFragment.OnListFragmentInteractionListener?) :
        RecyclerView.ViewHolder(
            view
        ) {
        val movieSwitch: Switch
        val movieTitle: TextView
        val listener: MovieFragment.OnListFragmentInteractionListener?
        var item: MovieItem? = null
        fun setSelected(selected: Boolean) {
            item!!.selected = selected
            if (movieSwitch.isChecked != selected) {
                movieSwitch.isChecked = selected
            }
            listener?.onItemSelectionChange(item)
        }

        override fun toString(): String {
            return super.toString() + " '" + movieTitle.text + "'"
        }

        init {
            movieSwitch = view.findViewById<View>(R.id.movie_switch) as Switch
            movieTitle = view.findViewById<View>(R.id.movie_title) as TextView
            this.listener = listener
        }
    }
}