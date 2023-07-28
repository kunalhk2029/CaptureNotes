package com.app.capturenotes.framework.presentation

import android.app.Application
import com.app.capturenotes.di.AppComponent
import com.app.capturenotes.di.DaggerAppComponent
import kotlinx.coroutines.*

@FlowPreview
@ExperimentalCoroutinesApi

open class BaseApplication : Application(){

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        initAppComponent()
    }

    open fun initAppComponent(){
        appComponent = DaggerAppComponent
            .factory()
            .create(this)
    }
}