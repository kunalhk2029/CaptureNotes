package com.app.capturenotes.business.interactors.notelist

import com.app.capturenotes.business.interactors.common.DeleteNote
import com.app.capturenotes.framework.presentation.notelist.state.NoteListViewState

// Use cases
class NoteListInteractors (
    val insertNewNote: InsertNewNote,
    val deleteNote: DeleteNote<NoteListViewState>,
    val searchNotes: SearchNotes,
    val getNumNotes: GetNumNotes,
    val restoreDeletedNote: RestoreDeletedNote,
    val deleteMultipleNotes: DeleteMultipleNotes,
)














