package com.app.capturenotes.framework.presentation.notedetail.state

import android.os.Parcelable
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.state.ViewState
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NoteDetailViewState(
    var note: Note? = null,
    var isUpdatePending: Boolean? = null
) : Parcelable, ViewState