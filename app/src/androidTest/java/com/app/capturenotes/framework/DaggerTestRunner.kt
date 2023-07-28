package com.app.capturenotes.framework

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.app.capturenotes.framework.presentation.TestBaseApplication

class DaggerTestRunner: AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application {
        return super.newApplication(cl, TestBaseApplication::class.java.name, context)
    }
}