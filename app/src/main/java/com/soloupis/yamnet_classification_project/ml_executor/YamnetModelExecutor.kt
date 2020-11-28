package com.soloupis.yamnet_classification_project.ml_executor

import android.content.Context
import android.os.FileUtils
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class YamnetModelExecutor(
    context: Context,
    useGPU: Boolean
) {

    // Use of 2 threads after benchmarking the model
    // Also no GPU usage because it does not support the model
    private var numberThreads = 2
    private var interpreter: Interpreter
    private var predictTime = 0L
    private lateinit var labels: List<String>


    init {

        interpreter = getInterpreter(context, YAMNET_MODEL, false)

        labels = FileUtil.loadLabels(context, "classes.txt")

    }

    companion object {
        private const val YAMNET_MODEL = "model_yamnet_tflite.tflite"
    }

    fun execute(floatsInput: FloatArray): Pair<ArrayList<String>, ArrayList<Float>> {

        predictTime = System.currentTimeMillis()
        val inputSize = floatsInput.size // ~2 seconds of sound
        //Log.i("YAMNET_INPUT_SIZE", inputSize.toString())

        val inputValues = floatsInput//FloatArray(inputSize)

        val inputs = arrayOf<Any>(inputValues)
        val outputs = HashMap<Int, Any>()

        // Outputs of yamnet model with tflite and for 2 seconds .wav file
        // scores(4, 521) emmbedings(4, 1024) spectogram(240, 64)
        val arrayScores = Array(4) { FloatArray(521) { 0f } }
        val arrayEmbeddings = Array(4) { FloatArray(1024) { 0f } }
        val arraySpectograms = Array(240) { FloatArray(64) { 0f } }

        outputs[0] = arrayScores
        outputs[1] = arrayEmbeddings
        outputs[2] = arraySpectograms

        try {
            interpreter.runForMultipleInputsOutputs(inputs, outputs)
        } catch (e: Exception) {
            Log.e("EXCEPTION", e.toString())
        }

        val arrayMeanScores = FloatArray(521) { 0f }
        for (i in 0 until 521) {
            // Find the average of the 4 arrays at axis = 0
            arrayMeanScores[i] = arrayListOf(
                arrayScores[0][i],
                arrayScores[1][i],
                arrayScores[2][i],
                arrayScores[3][i]
            ).average().toFloat()
        }

        val listOfArrayMeanScores = arrayMeanScores.toCollection(ArrayList())

        val listOfMaximumValues = arrayListOf<Float>()
        for (i in 0 until 10) {
            val number = listOfArrayMeanScores.max() ?: 0f
            listOfMaximumValues.add(number)
            listOfArrayMeanScores.remove(number)
        }

        val listOfMaxIndices = arrayListOf<Int>()
        for (i in 0 until 10) {
            for (k in arrayMeanScores.indices) {
                if (listOfMaximumValues[i] == arrayMeanScores[k]) {
                    listOfMaxIndices.add(k)
                }
            }

        }

        Log.i("YAMNET_SCORES", arrayMeanScores.contentToString())
        Log.i("YAMNET_SCORES_SIZE", arrayMeanScores.size.toString())
        Log.i("YAMNET_INDICES", listOfMaxIndices.toString())
        //Log.i("YAMNET_LABELS", labels.toString())
        //Log.i("YAMNET_EMBEDDINGS_SIZE", arrayEmbeddings.size.toString())

        val finalListOfOutputs = arrayListOf<String>()
        for (i in listOfMaxIndices.indices) {
            finalListOfOutputs.add(labels.get(listOfMaxIndices.get(i)))
        }

        return Pair(finalListOfOutputs, listOfMaximumValues) // ArrayList<String>
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