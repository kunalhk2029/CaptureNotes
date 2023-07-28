package com.app.capturenotes.business.data.network

import com.app.capturenotes.business.data.network.abstraction.NoteNetworkDataSource
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.util.DateUtil

class FakeNoteNetworkDataSourceImpl
constructor(
    private val dateUtil: DateUtil,
    private val notesData: HashMap<String, Note>,
    private val deletedNotesData: HashMap<String, Note>
) : NoteNetworkDataSource{

    override suspend fun insertOrUpdateNote(note: Note) {
        notesData[note.id] = note
    }

    override suspend fun deleteNote(primaryKey: String) {
        notesData.remove(primaryKey)
    }

    override suspend fun insertDeletedNote(note: Note) {
        deletedNotesData[note.id] = note
    }

    override suspend fun insertDeletedNotes(notes: List<Note>) {
        for(note in notes){
            deletedNotesData[note.id] = note
        }
    }

    override suspend fun deleteDeletedNote(note: Note) {
        deletedNotesData.remove(note.id)
    }

    override suspend fun getDeletedNotes(): List<Note> {
        return ArrayList(deletedNotesData.values)
    }

    override suspend fun deleteAllNotes() {
        deletedNotesData.clear()
    }

    override suspend fun searchNote(note: Note): Note? {
        return notesData[note.id]
    }

    override suspend fun getAllNotes(): List<Note> {
        return ArrayList(notesData.values)
    }

    override suspend fun insertOrUpdateNotes(notes: List<Note>) {
        for(note in notes){
            notesData[note.id] = note
        }
    }
}
