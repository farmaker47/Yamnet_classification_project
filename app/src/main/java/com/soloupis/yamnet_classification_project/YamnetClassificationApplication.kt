package com.soloupis.yamnet_classification_project

import android.app.Application
import com.soloupis.yamnet_classification_project.di.listeningFragmentModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class YamnetClassificationApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            // Context
            androidContext(this@YamnetClassificationApplication)
            modules(
                listeningFragmentModule
            )
        }

    }
}