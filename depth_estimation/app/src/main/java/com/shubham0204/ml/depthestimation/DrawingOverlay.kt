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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

// Overlay to display the depth map over the `PreviewView`.
// See activity_main.xml
class DrawingOverlay(context : Context, attributeSet : AttributeSet) : SurfaceView( context , attributeSet ) , SurfaceHolder.Callback {

    // This variable is assigned in FrameAnalyser.kt
    var depthMaskBitmap : Bitmap? = null

    // These variables are assigned in MainActivity.kt
    var isFrontCameraOn = true
    var isShowingDepthMap = false



    override fun surfaceCreated(holder: SurfaceHolder) {
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }


    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }


    // Will be called when `drawingOverlay.invalidate()` is called in FrameAnalyser.kt.
    override fun onDraw(canvas: Canvas?) {
        if ( depthMaskBitmap != null && isShowingDepthMap ) {
            // If the front camera is on, then flip the Bitmap vertically ( or produce a mirror image of it ).
            canvas?.drawBitmap(
                if ( isFrontCameraOn ) { BitmapUtils.flipBitmap( depthMaskBitmap!! ) } else { depthMaskBitmap!! },
                0f , 0f , null )
        }
    }

}