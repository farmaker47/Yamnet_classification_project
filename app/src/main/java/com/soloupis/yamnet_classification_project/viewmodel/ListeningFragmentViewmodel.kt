package com.soloupis.yamnet_classification_project.viewmodel

import android.app.Application
import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.soloupis.yamnet_classification_project.ml_executor.YamnetModelExecutor
import com.soloupis.yamnet_classification_project.recorder.ListeningRecorder
import com.soloupis.yamnet_classification_project.view.ListeningFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.get
import java.io.ByteArrayOutputStream

class ListeningFragmentViewmodel(application: Application) : AndroidViewModel(application),
    KoinComponent {

    private var listeningRecorderObject: ListeningRecorder
    private var yamnetModelExecutor:YamnetModelExecutor
    var listeningRunning = false
    private var context: Context = application

    // Handlers for repeating sound collection and karaoke effect
    private val updateLoopListeningHandler = Handler()
    private val handler = Handler()
    private val handlerStart = Handler()

    private val _inferenceDone = MutableLiveData<Boolean>()
    val inferenceDone: LiveData<Boolean>
        get() = _inferenceDone

    private val _listeningEnd = MutableLiveData<Boolean>()
    val listeningEnd: LiveData<Boolean>
        get() = _listeningEnd

    private val _listOfclasses = MutableLiveData<Pair<ArrayList<String>, ArrayList<Float>>>()
    val listOfClasses: LiveData<Pair<ArrayList<String>, ArrayList<Float>>>
        get() = _listOfclasses

    init {
        // DI
        listeningRecorderObject = get()
        yamnetModelExecutor = get()

    }


    fun startListening() {

        listeningRunning = true

        listeningRecorderObject.startRecording()
    }

    fun stopListening() {

        val stream = listeningRecorderObject.stopRecording()
        val streamForInference = listeningRecorderObject.stopRecordingForInference()

        Log.i("VIEWMODEL_SIZE", streamForInference.size.toString())
        Log.i("VIEWMODEL_VALUES", streamForInference.takeLast(100).toString())

        listeningRunning = false
        // Background thread to do inference with the generated short arraylist
        viewModelScope.launch {
            doInference(stream, streamForInference)
        }

    }

    private suspend fun doInference(
        stream: ByteArrayOutputStream,
        arrayListShorts: ArrayList<Short>
    ) = withContext(Dispatchers.IO) {
        // write .wav file to external directory
        listeningRecorderObject.writeWav(stream)
        // reset stream
        listeningRecorderObject.reInitializePcmStream()

        // The input must be normalized to floats between -1 and 1.
        // To normalize it, we just need to divide all the values by 2**16 or in our code, MAX_ABS_INT16 = 32768
        val floatsForInference = FloatArray(arrayListShorts.size)
        for ((index, value) in arrayListShorts.withIndex()) {
            floatsForInference[index] = (value / 32768F)
        }

        Log.i("YAMNET_FLOATS", floatsForInference.takeLast(100).toString())

        // Inference
        _inferenceDone.postValue(false)

        _listOfclasses.postValue(yamnetModelExecutor.execute(floatsForInference))

        _inferenceDone.postValue(true)

    }

    fun setUpdateLoopListeningHandler() {
        // Start loop for collecting sound and inferring
        updateLoopListeningHandler.postDelayed(updateLoopListeningRunnable, 0)
    }

    fun stopAllListening() {
        // remove queue of callbacks when user presses stop before song stops
        updateLoopListeningHandler.removeCallbacks(updateLoopListeningRunnable)
        //updateKaraokeHandler.removeCallbacks(updateKaraokeRunnable)
        handler.removeCallbacksAndMessages(null)
        handlerStart.removeCallbacksAndMessages(null)

        _listeningEnd.value = true
    }

    private var updateLoopListeningRunnable: Runnable = Runnable {
        run {

            // Start listening
            startListening()
            _listeningEnd.value = false

            // Stop after 2048 millis
            val handler = Handler()
            handler.postDelayed({
                stopListening()
            }, ListeningFragment.UPDATE_INTERVAL_INFERENCE)

            // Re-run it after the update interval
            updateLoopListeningHandler.postDelayed(
                updateLoopListeningRunnable,
                ListeningFragment.UPDATE_INTERVAL_INFERENCE
            )

        }

    }











    override fun onCleared() {
        super.onCleared()

        // Below stopAllListening to execute when back button is used
        stopAllListening()
        yamnetModelExecutor.close()

    }
}