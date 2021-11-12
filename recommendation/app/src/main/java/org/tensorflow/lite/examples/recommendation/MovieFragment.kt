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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.recommendation.data.MovieItem
import java.util.*

/**
 * A fragment representing a list of items for user to select from.
 * Activities containing this fragment MUST implement the [ ] interface.
 */
class MovieFragment : Fragment() {

    private var columnCount = 1
    private var listener: OnListFragmentInteractionListener? = null
    private var recyclerView: RecyclerView? = null
    private var items: List<MovieItem> = ArrayList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener =
            if (context is OnListFragmentInteractionListener) {
                context
            } else {
                throw IllegalStateException("$context must implement OnListFragmentInteractionListener")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            columnCount = arguments!!.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tfe_re_fragment_selection_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            val context = view.getContext()
            recyclerView = view
            if (columnCount <= 1) {
                recyclerView!!.layoutManager = LinearLayoutManager(context)
            } else {
                recyclerView!!.layoutManager = GridLayoutManager(context, columnCount)
            }
            recyclerView!!.adapter = MovieRecyclerViewAdapter(items, listener!!)
        }
        return view
    }

    fun setMovies(movies: List<MovieItem>) {
        items = movies
        recyclerView!!.adapter = MovieRecyclerViewAdapter(items, listener!!)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this fragment to allow an
     * interaction in this fragment to be communicated to the activity and potentially other fragments
     * contained in that activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnListFragmentInteractionListener {
        fun onItemSelectionChange(item: MovieItem?)
    }

    companion object {
        private const val ARG_COLUMN_COUNT = "movie-fragment-column-count"
        fun newInstance(columnCount: Int): MovieFragment {
            val fragment = MovieFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }
}