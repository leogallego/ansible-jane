package com.example.aapremote

import android.app.Application
import com.example.aapremote.assistant.assistantModule
import com.example.aapremote.assistant.localToolsModule
import com.example.aapremote.data.dataModule
import com.example.aapremote.network.networkModule
import com.example.aapremote.presentation.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AapRemoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AapRemoteApp)
            modules(networkModule, dataModule, presentationModule, localToolsModule, assistantModule)
        }
    }
}
