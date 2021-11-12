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
package org.tensorflow.lite.examples.recommendation.data

import kotlin.Throws
import android.content.res.AssetManager
import org.tensorflow.lite.examples.recommendation.data.MovieItem
import org.tensorflow.lite.examples.recommendation.data.FileUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.tensorflow.lite.examples.recommendation.Config
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors

/** FileUtil class to load data from asset files.  */
object FileUtil {
    /** Load TF Lite model from asset file.  */
    @JvmStatic
    @Throws(IOException::class)
    fun loadModelFile(assetManager: AssetManager, modelPath: String?): MappedByteBuffer {
        assetManager.openFd(modelPath!!).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        }
    }

    /** Load candidates from asset file.  */
    @JvmStatic
    @Throws(IOException::class)
    fun loadMovieList(
        assetManager: AssetManager, candidateListPath: String
    ): Collection<MovieItem> {
        val content = loadFileContent(assetManager, candidateListPath)
        val gson = Gson()
        val type = object : TypeToken<Collection<MovieItem?>?>() {}.type
        return gson.fromJson(content, type)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun loadGenreList(assetManager: AssetManager, genreListPath: String): List<String> {
        val content = loadFileContent(assetManager, genreListPath)
        val lines = content.split(System.lineSeparator().toRegex()).toTypedArray()
        return Arrays.asList(*lines)
    }

    /** Load config from asset file.  */
    @JvmStatic
    @Throws(IOException::class)
    fun loadConfig(assetManager: AssetManager, configPath: String): Config {
        val content = loadFileContent(assetManager, configPath)
        val gson = Gson()
        val type = object : TypeToken<Config?>() {}.type
        return gson.fromJson(content, type)
    }

    /** Load file content from asset file.  */
    @Throws(IOException::class)
    private fun loadFileContent(assetManager: AssetManager, path: String): String {
        assetManager.open(path).use { ins ->
            BufferedReader(InputStreamReader(ins, StandardCharsets.UTF_8)).use { reader ->
                return reader.lines().collect(
                    Collectors.joining(System.lineSeparator())
                )
            }
        }
    }
}