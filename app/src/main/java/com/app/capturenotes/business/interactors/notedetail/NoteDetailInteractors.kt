package com.app.capturenotes.business.interactors.notedetail

import com.app.capturenotes.business.interactors.common.DeleteNote
import com.app.capturenotes.framework.presentation.notedetail.state.NoteDetailViewState

class NoteDetailInteractors (
    val deleteNote: DeleteNote<NoteDetailViewState>,
    val updateNote: UpdateNote
)