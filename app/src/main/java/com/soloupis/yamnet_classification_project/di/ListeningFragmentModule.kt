package com.soloupis.yamnet_classification_project.di

import com.soloupis.yamnet_classification_project.recorder.ListeningRecorder
import com.soloupis.yamnet_classification_project.viewmodel.ListeningFragmentViewmodel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val listeningFragmentModule = module {

    single { ListeningRecorder("hotKey", 0, get()) }

    // Use factory instead of single when user presses back button...
    // to force execution of init block when interpreter is closed
    //factory { PitchModelExecutor(get(), getKoin().getProperty("koinUseGpu")!!) }

    viewModel {
        ListeningFragmentViewmodel(get())
    }
}