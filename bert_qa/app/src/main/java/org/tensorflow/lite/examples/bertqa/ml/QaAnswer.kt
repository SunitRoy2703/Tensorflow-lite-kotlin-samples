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

/** QA Answer class.  */
class QaAnswer(var text: String?, var pos: Pos) {

    constructor(text: String?, start: Int, end: Int, logit: Float) : this(
        text,
        Pos(start, end, logit)
    )

    /** Position and related information from the model.  */
    class Pos(var start: Int, var end: Int, var logit: Float) : Comparable<Pos> {
        override fun compareTo(other: Pos): Int {
            return java.lang.Float.compare(other.logit, logit)
        }
    }
}