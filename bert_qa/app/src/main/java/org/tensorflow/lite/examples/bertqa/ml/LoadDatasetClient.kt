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
package org.tensorflow.lite.examples.bertqa.ml

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Interface to load squad dataset. Provide passages for users to choose from & provide questions
 * for autoCompleteTextView.
 */
class LoadDatasetClient(private val context: Context) {

    private lateinit var contents: Array<String>
    lateinit var titles: Array<String>
    private lateinit var questions: Array<Array<String?>?>

    private fun loadJson() {
        try {
            val `is` = context.assets.open(JSON_DIR)
            val reader = JsonReader(InputStreamReader(`is`))
            val map: HashMap<String, List<List<String>>> =
                Gson().fromJson(reader, HashMap::class.java)
            val jsonTitles = map["titles"]!!
            val jsonContents = map["contents"]!!
            val jsonQuestions = map["questions"]!!
            titles = listToArray(jsonTitles)
            contents = listToArray(jsonContents)
            questions = arrayOfNulls(jsonQuestions.size)
            var index = 0
            for (item in jsonQuestions) {
                questions[index++] = item.toTypedArray()
            }
        } catch (ex: IOException) {
            Log.e(TAG, ex.toString())
        }
    }

    fun getContent(index: Int): String? {
        return contents[index]
    }

    fun getQuestions(index: Int): Array<String> {
        return questions[index]!! as Array<String>
    }

    companion object {
        private const val TAG = "BertAppDemo"
        private const val JSON_DIR = "qa.json"
        private fun listToArray(list: List<List<String>>): Array<String> {
            val answer = Array(list.size) { "" }
            var index = 0
            for (item in list) {
                answer[index++] = item[0]
            }
            return answer
        }
    }

    init {
        loadJson()
    }
}