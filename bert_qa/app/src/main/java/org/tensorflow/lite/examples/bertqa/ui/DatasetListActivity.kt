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

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.bertqa.R
import org.tensorflow.lite.examples.bertqa.ml.LoadDatasetClient
import org.tensorflow.lite.examples.bertqa.ui.QaActivity.Companion.newInstance

/**
 * An activity representing a list of Datasets. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a [QaActivity] representing item details. On tablets, the activity
 * presents the list of items and item details side-by-side using two vertical panes.
 */
class DatasetListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tfe_qa_activity_dataset_list)
        val listView = findViewById<ListView>(R.id.dataset_list)!!
        val datasetClient = LoadDatasetClient(this)
        val datasetAdapter = ArrayAdapter(
            this, android.R.layout.simple_selectable_list_item, datasetClient.titles
        )
        listView.adapter = datasetAdapter
        listView.onItemClickListener =
            OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                startActivity(
                    newInstance(this, position)
                )
            }
    }
}