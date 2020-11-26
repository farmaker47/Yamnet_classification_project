package com.soloupis.yamnet_classification_project.ml_executor

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.*

class YamnetModelExecutor(
    context: Context,
    useGPU: Boolean
) {

    // Use of 2 threads after benchmarking the model
    // Also no GPU usage because it does not support the model
    private var numberThreads = 2
    private var interpreter: Interpreter
    private var predictTime = 0L

    init {

        interpreter = getInterpreter(context, YAMNET_MODEL, false)

    }

    companion object {
        private const val YAMNET_MODEL = "model_yamnet_tflite.tflite"
    }

    fun execute(floatsInput: FloatArray): ArrayList<String> {

        predictTime = System.currentTimeMillis()
        val inputSize = floatsInput.size // ~2 seconds of sound
        Log.i("YAMNET_INPUT_SIZE", inputSize.toString())

        val inputValues = floatsInput//FloatArray(inputSize)

        val inputs = arrayOf<Any>(inputValues)
        val outputs = HashMap<Int, Any>()

        // Outputs of yamnet model with tflite and for 2 seconds .wav file
        // scores(4, 521) emmbedings(4, 1024) spectogram(240, 64)
        val arrayScores = Array(4) { FloatArray(521) { 0f } }
        val arrayEmbeddings = Array(4) {FloatArray(1024) {0f} }
        val arraySpectograms = Array(240){FloatArray(64) {0f} }

        outputs[0] = arrayScores
        outputs[1] = arrayEmbeddings
        outputs[2] = arraySpectograms

        try {
            interpreter.runForMultipleInputsOutputs(inputs, outputs)
        } catch (e: Exception) {
            Log.e("EXCEPTION", e.toString())
        }

        Log.i("YAMNET_SCORES", arrayScores[0].contentToString())
        Log.i("YAMNET_SCORES_SIZE", arrayScores.size.toString())
        Log.i("YAMNET_EMBEDDINGS", arrayEmbeddings[0].contentToString())
        Log.i("YAMNET_EMBEDDINGS_SIZE", arrayEmbeddings.size.toString())

        Log.i("YAMNET_PREDICT_TIME", (System.currentTimeMillis() - predictTime).toString())

        return arrayListOf() // ArrayList<String>
    }

    // load tflite file from assets folder
    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        fileDescriptor.close()
        return retFile
    }

    @Throws(IOException::class)
    private fun getInterpreter(
        context: Context,
        modelName: String,
        useGpu: Boolean
    ): Interpreter {
        val tfliteOptions = Interpreter.Options()
        tfliteOptions.setNumThreads(numberThreads)
        return Interpreter(loadModelFile(context, modelName), tfliteOptions)
    }

    fun close() {
        interpreter.close()
    }
}