package com.sunit.zero_dce.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.sunit.zero_dce.ImageUtils
import com.sunit.zero_dce.MainActivity
import com.sunit.zero_dce.R
import com.sunit.zero_dce.ml.ZeroDce
import kotlinx.android.synthetic.main.fragment_inference.*
import kotlinx.coroutines.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*


class InferenceFragment : Fragment() {

    private val args: InferenceFragmentArgs by navArgs()
    private lateinit var filePath: String
    private lateinit var model: ZeroDce
    private var handler: Handler? = null

    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(
        Dispatchers.Main + parentJob
    )

    private val MAX_RESULTS = 3
    private val BATCH_SIZE = 1
    private val PIXEL_SIZE = 3
    private val IMAGE_MEAN = 128
    private val IMAGE_STD = 128.0f



    private fun getOutputAsync(bitmap: Bitmap): Deferred<Pair<Bitmap, Long>> =
        // use async() to create a coroutine in an IO optimized Dispatcher for model inference
        coroutineScope.async(Dispatchers.IO) {

            // Output
            val tensorImage: TensorImage
            val startTime = SystemClock.uptimeMillis()
            val tensorImageBitmap = inferenceWithZeroDCE(bitmap)               // Zero DCE

            // Note this inference time includes pre-processing and post-processing
            val inferenceTime = SystemClock.uptimeMillis() - startTime
           // val tensorImageBitmap = tensorImage.bitmap

            return@async Pair(tensorImageBitmap, inferenceTime)
        }

    private fun inferenceWithZeroDCE(sourceImage: Bitmap): Bitmap {

        val preprocessImage: Bitmap = Bitmap.createScaledBitmap(sourceImage, 400, 600, true)

        Log.d("TagWidth", preprocessImage.width.toString())
        Log.d("TagHeight", preprocessImage.height.toString())

        val buffer: ByteBuffer = convertBitmapToByteBuffer(preprocessImage)
            //ByteBuffer.allocate(preprocessImage.byteCount)
       // preprocessImage.copyPixelsToBuffer(buffer)

       // model = ZeroDce.newInstance(requireContext())
    // Creates inputs for reference.
        val inputFeature0 = TensorBuffer
            //.createDynamic(DataType.FLOAT32)
           .createFixedSize(intArrayOf(BATCH_SIZE, 400, 600, PIXEL_SIZE), DataType.FLOAT32)
    //inputFeature0.loadBuffer(buffer, intArrayOf(1, 400, 600, 3))
        inputFeature0.loadBuffer(buffer)
        // Runs model inference and gets result.

        var outputs: ZeroDce.Outputs? = null

        handler!!.post{
            outputs = model.process(inputFeature0)
        }

        val outputFeature0 = outputs?.outputFeature0AsTensorBuffer

    // Releases model resources if no longer used.
        model.close()

        val postprocessImage = outputFeature0?.let { getOutputImage(it.buffer) }
        return postprocessImage!!

    }


    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer =
            ByteBuffer.allocateDirect(4 * BATCH_SIZE * 400 * 600 * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray( 400 * 600)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until 400) {
            for (j in 0 until 600) {
                val `val` = intValues[pixel++]
                byteBuffer.putFloat(((`val` shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((`val` shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((`val` and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        return byteBuffer
    }
    private fun getOutputImage(output: ByteBuffer): Bitmap {
        val outputHeight= 600
        val outputWidth = 400

        output?.rewind() // Rewind the output buffer after running.

        val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(outputWidth * outputHeight) // Set your expected output's height and width
        for (i in 0 until outputWidth * outputHeight) {
            val a = 0xFF
            val r: Float = output?.float!! * 255.0f
            val g: Float = output?.float!! * 255.0f
            val b: Float = output?.float!! * 255.0f
            pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }
        bitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)

        return bitmap
    }

    private fun updateUI(outputBitmap: Bitmap, inferenceTime: Long) {
        progressbar.visibility = View.GONE
        imageview_output?.setImageBitmap(outputBitmap)
        inference_info.setText("Inference time: " + inferenceTime.toString() + "ms")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true) // enable toolbar

        retainInstance = true
        filePath = args.rootDir
        handler = Handler()
        handler!!.post{
            this.model = ZeroDce.newInstance(requireContext())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_inference, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val photoFile = File(filePath)

        Glide.with(imageview_input.context)
            .load(photoFile)
            .into(imageview_input)

        val selfieBitmap = BitmapFactory.decodeFile(filePath)
        coroutineScope.launch(Dispatchers.Main) {
            val (outputBitmap, inferenceTime) = getOutputAsync(selfieBitmap).await()
            updateUI(outputBitmap, inferenceTime)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // clean up coroutine job
        parentJob.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> saveInference()
        }
        return super.onOptionsItemSelected(item)
    }
    private fun saveInference(): String {

        val inferenceBitmap = imageview_output.drawable.toBitmap()
        val file = File(
            MainActivity.getOutputDirectory(requireContext()),
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + "_inference.jpg")

        ImageUtils.saveBitmap(inferenceBitmap, file)
        Toast.makeText(context, "saved to " + file.absolutePath.toString(), Toast.LENGTH_SHORT)
            .show()

        return file.absolutePath

    }

    companion object {
        private const val TAG = "InferenceFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

}