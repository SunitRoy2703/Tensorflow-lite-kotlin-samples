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

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView : PreviewView
    private lateinit var drawingOverlay: DrawingOverlay

    private var preview: Preview? = null
    private lateinit var cameraProviderListenableFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var frameAnalyser : FrameAnalyser
    private var frameAnalysis: ImageAnalysis? = null

    private var isFrontCameraOn = true
    private var isDepthMapDisplayed = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove the status bar to have a full screen experience
        // See this answer on SO -> https://stackoverflow.com/a/68152688/10878733
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_main)

        previewView = findViewById( R.id.camera_preview_view )

        // Make sure that the DrawingOverlay remains on top
        // See this SO answer -> https://stackoverflow.com/a/28883273/10878733
        drawingOverlay = findViewById( R.id.camera_drawing_overlay )
        drawingOverlay.setWillNotDraw(false)
        drawingOverlay.setZOrderOnTop(true)

        val depthEstimationModel = MiDASModel( this )
        frameAnalyser = FrameAnalyser( depthEstimationModel , drawingOverlay )

        val flipCameraFAB = findViewById<FloatingActionButton>( R.id.flip_camera_fab )
        flipCameraFAB.setOnClickListener {
            when( isFrontCameraOn ) {
                true -> setupCameraProvider( CameraSelector.LENS_FACING_BACK )
                false -> setupCameraProvider( CameraSelector.LENS_FACING_FRONT )
            }
            // Alert the DrawingOverlay regarding the change in lens facing.
            // This is important as for the front camera, we need to flip ( vertically ) the frames to
            // draw them on the overlay.
            drawingOverlay.isFrontCameraOn = !isFrontCameraOn
            isFrontCameraOn = !isFrontCameraOn
        }

        val depthMapDisplayFAB = findViewById<FloatingActionButton>( R.id.depth_map_toggle_fab )
        depthMapDisplayFAB.setOnClickListener {
            if ( isDepthMapDisplayed ) {
                drawingOverlay.visibility = View.GONE
            }
            // Alert the FrameAnalyser to stop computing the depth maps.
            frameAnalyser.isComputingDepthMap = !isDepthMapDisplayed
            // Alert the DrawingOverlay to stop drawing any depth maps ( if they were to be drawn ).
            drawingOverlay.isShowingDepthMap = !isDepthMapDisplayed
            isDepthMapDisplayed = !isDepthMapDisplayed
            depthMapDisplayFAB.backgroundTintList =
                ColorStateList.valueOf( if ( isDepthMapDisplayed ) { Color.GREEN } else { Color.WHITE } )
            val message = if ( isDepthMapDisplayed ) { "Displaying Depth map..." } else { "Displaying Camera Feed..." }
            Toast.makeText( this , message , Toast.LENGTH_LONG ).show()
        }
        depthMapDisplayFAB.backgroundTintList = ColorStateList.valueOf( Color.WHITE )

        // Request the CAMERA permission as we'll require it for displaying the camera preview.
        // See https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code
        if (ActivityCompat.checkSelfPermission( this , Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED ) {
            requestCameraPermission()
        }
        else {
            setupCameraProvider( CameraSelector.LENS_FACING_FRONT )
        }

    }


    private fun requestCameraPermission() {
        Logger.logInfo( "Requesting camera permission" )
        requestCameraPermissionLauncher.launch( Manifest.permission.CAMERA )
    }


    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission() ) {
            isGranted : Boolean ->
        if ( isGranted ) {
            Logger.logInfo( "Camera permission granted by user." )
            setupCameraProvider( CameraSelector.LENS_FACING_FRONT )
        }
        else {
            Logger.logInfo( "Camera permission denied by user." )
            val alertDialog = AlertDialog.Builder( this ).apply {
                setTitle( "Permissions" )
                setMessage( "The app requires the camera permission to function." )
                setPositiveButton( "GRANT") { dialog, _ ->
                    dialog.dismiss()
                    requestCameraPermission()
                }
                setNegativeButton( "CLOSE" ) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                setCancelable( false )
                create()
            }
            alertDialog.show()
        }
    }


    // Setup the PreviewView for live camera feed.
    // See the docs -> https://developer.android.com/training/camerax/preview
    // and https://developer.android.com/training/camerax/analyze
    private fun setupCameraProvider( cameraFacing : Int ) {
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance( this )
        cameraProviderListenableFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderListenableFuture.get()
                bindPreview( cameraProvider , cameraFacing )
            }
            catch (e: ExecutionException) {
                Logger.logError( e.message!! )
            }
            catch (e: InterruptedException) {
                Logger.logError( e.message!! )
            }
        }, ContextCompat.getMainExecutor( this ))
    }


    private fun bindPreview( cameraProvider: ProcessCameraProvider , lensFacing : Int ) {
        // Unbind any previous use-cases as we'll attach them once again.
        if ( preview != null && frameAnalysis != null ) {
            cameraProvider.unbind( preview , frameAnalysis )
        }

        Logger.logInfo( "Setting camera with ${ 
            if ( lensFacing == CameraSelector.LENS_FACING_FRONT ) { "front" }
            else { "rear" }
        } lens facing" )

        preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing( lensFacing )
            .build()
        preview!!.setSurfaceProvider(previewView.surfaceProvider)

        // Set the resolution which is the closest to the screen size.
        val displayMetrics = resources.displayMetrics
        val screenSize = Size( displayMetrics.widthPixels, displayMetrics.heightPixels )
        Logger.logInfo( "Screen size is $screenSize" )

        frameAnalysis = ImageAnalysis.Builder()
            .setTargetResolution( screenSize )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        frameAnalysis!!.setAnalyzer( Executors.newSingleThreadExecutor() , frameAnalyser )
        cameraProvider.bindToLifecycle(
            (this as LifecycleOwner),
            cameraSelector,
            frameAnalysis,
            preview
        )
    }

}