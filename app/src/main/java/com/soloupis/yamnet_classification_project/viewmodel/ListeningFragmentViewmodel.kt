package com.soloupis.yamnet_classification_project.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.koin.core.KoinComponent

class ListeningFragmentViewmodel(application: Application) : AndroidViewModel(application),
    KoinComponent {


    init {

    }

    override fun onCleared() {
        super.onCleared()


    }
}