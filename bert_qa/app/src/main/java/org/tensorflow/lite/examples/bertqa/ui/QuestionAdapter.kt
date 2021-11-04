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
package org.tensorflow.lite.examples.bertqa.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import org.tensorflow.lite.examples.bertqa.R
import org.tensorflow.lite.examples.bertqa.ui.QuestionAdapter.MyViewHolder

/** Adapter class to show question suggestion chips.  */
class QuestionAdapter(context: Context?, questions: Array<String>) :
    RecyclerView.Adapter<MyViewHolder>() {

    private val inflater: LayoutInflater
    private val questions: Array<String>
    private var onQuestionSelectListener: OnQuestionSelectListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view =
            inflater.inflate(R.layout.tfe_qa_question_chip, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.chip.text = questions[position]
        holder.chip.setOnClickListener { view: View? ->
            onQuestionSelectListener!!.onQuestionSelect(
                questions[position]
            )
        }
    }

    override fun getItemCount(): Int {
        return questions.size
    }

    fun setOnQuestionSelectListener(onQuestionSelectListener: OnQuestionSelectListener?) {
        this.onQuestionSelectListener = onQuestionSelectListener
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var chip: Chip

        init {
            chip = itemView.findViewById(R.id.chip)
        }
    }

    /** Interface for callback when a question is selected.  */
    interface OnQuestionSelectListener {
        fun onQuestionSelect(question: String?)
    }

    init {
        inflater = LayoutInflater.from(context)
        this.questions = questions
    }
}