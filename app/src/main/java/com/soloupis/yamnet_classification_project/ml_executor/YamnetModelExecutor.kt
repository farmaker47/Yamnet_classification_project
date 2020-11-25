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
    private var numberThreads = 4
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
        var outputSize = 0
        when (inputSize) {
            // 16.000 * 2 seconds recording
            32000 -> outputSize = ceil(inputSize / 512.0).toInt()
            else -> outputSize = (ceil(inputSize / 512.0) + 1).toInt()
        }
        val inputValues = floatsInput//FloatArray(inputSize)

        val inputs = arrayOf<Any>(inputValues)
        val outputs = HashMap<Int, Any>()

        val pitches = FloatArray(outputSize)
        val uncertainties = FloatArray(outputSize)

        outputs[0] = pitches
        outputs[1] = uncertainties

        try {
            interpreter.runForMultipleInputsOutputs(inputs, outputs)
        } catch (e: Exception) {
            Log.e("EXCEPTION", e.toString())
        }

        Log.i("PITCHES", pitches.contentToString())
        Log.i("PITCHES_SIZE", pitches.size.toString())
        Log.i("UNCERTAIN", uncertainties.contentToString())
        Log.i("UNCERTAIN_SIZE", uncertainties.size.toString())

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