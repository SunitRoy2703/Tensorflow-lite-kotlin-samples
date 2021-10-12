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

import android.content.Context
import android.graphics.*
import android.media.Image
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

// Helper class for operations on Bitmaps
class BitmapUtils {

    companion object {

        // Rotate the given `source` by `degrees`.
        // See this SO answer -> https://stackoverflow.com/a/16219591/10878733
        fun rotateBitmap( source: Bitmap , degrees : Float ): Bitmap {
            val matrix = Matrix()
            matrix.postRotate( degrees )
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix , false )
        }


        // Use this method to save a Bitmap to the internal storage ( app-specific storage ) of your device.
        // To see the image, go to "Device File Explorer" -> "data" -> "data" -> "com.ml.quaterion.facenetdetection" -> "files"
        fun saveBitmap(context: Context, image: Bitmap, name: String) {
            val fileOutputStream = FileOutputStream(File( context.filesDir.absolutePath + "/$name.png"))
            image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        }


        // Resize the given Bitmap with given `targetWidth` and `targetHeight`.
        // See this SO answer -> https://stackoverflow.com/a/65574102/10878733
        fun resizeBitmap( src : Bitmap , targetWidth : Int , targetHeight : Int ) : Bitmap {
            return Bitmap.createScaledBitmap( src , targetWidth , targetHeight , true )
        }


        // Flip the given `Bitmap` vertically.
        // See this SO answer -> https://stackoverflow.com/a/36494192/10878733
        fun flipBitmap( source: Bitmap ): Bitmap {
            val matrix = Matrix()
            matrix.postScale(-1f, 1f, source.width / 2f, source.height / 2f)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }


        fun byteBufferToBitmap( imageArray : FloatArray , imageDim : Int ) : Bitmap {
            val pixels = imageArray.map { it.toInt() }.toIntArray()
            val bitmap = Bitmap.createBitmap(imageDim, imageDim, Bitmap.Config.RGB_565 );
            for ( i in 0 until imageDim ) {
                for ( j in 0 until imageDim ) {
                    val p = pixels[ i * imageDim + j ]
                    bitmap.setPixel( j , i , Color.rgb( p , p , p ))
                }
            }
            return bitmap
        }


        // Convert android.media.Image to android.graphics.Bitmap and rotate it by `rotationDegrees`
        // See the SO answer -> https://stackoverflow.com/a/44486294/10878733
        fun imageToBitmap( image : Image , rotationDegrees : Int ): Bitmap {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
            val yuv = out.toByteArray()
            return rotateBitmap( BitmapFactory.decodeByteArray(yuv, 0, yuv.size) , rotationDegrees.toFloat() )
        }

    }

}