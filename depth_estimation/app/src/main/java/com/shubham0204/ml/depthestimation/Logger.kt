/*
 * Copyright 2021 Shubham Panchal
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shubham0204.ml.depthestimation

import android.util.Log

// Utility class to log messages
class Logger {

    companion object {

        private val APP_LOG_TAG = "Depth_Estimation_App"

        fun logError( message : String ) {
            Log.e( APP_LOG_TAG , message )
        }

        fun logInfo( message : String ) {
            Log.i( APP_LOG_TAG , message )
        }

    }

}