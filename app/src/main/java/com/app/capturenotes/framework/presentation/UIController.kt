package com.app.capturenotes.framework.presentation

import com.app.capturenotes.business.domain.state.DialogInputCaptureCallback
import com.app.capturenotes.business.domain.state.Response
import com.app.capturenotes.business.domain.state.StateMessageCallback


interface UIController {

    fun displayProgressBar(isDisplayed: Boolean)

    fun hideSoftKeyboard()

    fun displayInputCaptureDialog(title: String, callback: DialogInputCaptureCallback)

    fun onResponseReceived(
        response: Response,
        stateMessageCallback: StateMessageCallback
    )
}