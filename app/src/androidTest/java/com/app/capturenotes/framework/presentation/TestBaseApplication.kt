package com.app.capturenotes.framework.presentation

import com.app.capturenotes.di.DaggerAppComponent
import com.app.capturenotes.di.DaggerTestAppComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TestBaseApplication: BaseApplication() {

    override fun initAppComponent() {
        appComponent = DaggerTestAppComponent.factory().create(this)
    }
}