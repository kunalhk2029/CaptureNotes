package com.app.capturenotes.business.interactors.notelist

import com.app.capturenotes.business.data.cache.CacheErrors
import com.app.capturenotes.business.data.cache.FORCE_GENERAL_FAILURE
import com.app.capturenotes.business.data.cache.FORCE_NEW_NOTE_EXCEPTION
import com.app.capturenotes.business.data.cache.abstraction.NoteCacheDataSource
import com.app.capturenotes.business.data.network.abstraction.NoteNetworkDataSource
import com.app.capturenotes.business.domain.model.NoteFactory
import com.app.capturenotes.business.interactors.notelist.InsertNewNote.Companion.INSERT_NOTE_SUCCESS
import com.app.capturenotes.di.DependencyContainer
import com.app.capturenotes.framework.presentation.notelist.state.NoteListStateEvent.*
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

/**
Test cases:
1. insertNote_success_confirmNetworkAndCacheUpdated()
    a) insert a new note
    b) listen for INSERT_NOTE_SUCCESS emission from flow
    c) confirm cache was updated with new note
    d) confirm network was updated with new note
2. insertNote_fail_confirmNetworkAndCacheUnchanged()
    a) insert a new note
    b) force a failure (return -1 from db operation)
    c) listen for INSERT_NOTE_FAILED emission from flow
    e) confirm cache was not updated
    e) confirm network was not updated
3. throwException_checkGenericError_confirmNetworkAndCacheUnchanged()
    a) insert a new note
    b) force an exception
    c) listen for CACHE_ERROR_UNKNOWN emission from flow
    e) confirm cache was not updated
    e) confirm network was not updated
 */

@InternalCoroutinesApi
class InsertNewNoteTest {

    // system in test
    private val insertNewNote: InsertNewNote

    // dependencies
    private val dependencyContainer: DependencyContainer = DependencyContainer()
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteNetworkDataSource: NoteNetworkDataSource
    private val noteFactory: NoteFactory

    init {
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        noteFactory = dependencyContainer.noteFactory
        insertNewNote = InsertNewNote(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource,
            noteFactory = noteFactory
        )
    }

    @Test
    fun insertNote_success_confirmNetworkAndCacheUpdated() = runBlocking {

        val newNote = noteFactory.createSingleNote(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString())

        insertNewNote.insertNewNote(
            id = newNote.id,
            title = newNote.title,
            stateEvent = InsertNewNoteEvent(newNote.title)
        ).collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                INSERT_NOTE_SUCCESS
            )
        }

        // confirm network was updated
        val networkNoteThatWasInserted = noteNetworkDataSource.searchNote(newNote)
        assertTrue { networkNoteThatWasInserted == newNote }

        // confirm cache was updated
        val cacheNoteThatWasInserted = noteCacheDataSource.searchNoteById(newNote.id)
        assertTrue { cacheNoteThatWasInserted == newNote }
    }

    @Test
    fun insertNote_fail_confirmNetworkAndCacheUnchanged() = runBlocking {

        val newNote = noteFactory.createSingleNote(
            id = FORCE_GENERAL_FAILURE,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )

        insertNewNote.insertNewNote(
            id = newNote.id,
            title = newNote.title,
            stateEvent = InsertNewNoteEvent(newNote.title)
        ).collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                InsertNewNote.INSERT_NOTE_FAILED
            )
        }

        // confirm network was not changed
        val networkNoteThatWasInserted = noteNetworkDataSource.searchNote(newNote)
        assertTrue { networkNoteThatWasInserted == null }

        // confirm cache was not changed
        val cacheNoteThatWasInserted = noteCacheDataSource.searchNoteById(newNote.id)
        assertTrue { cacheNoteThatWasInserted == null }
    }

    @Test
    fun throwException_checkGenericError_confirmNetworkAndCacheUnchanged() = runBlocking {

        val newNote = noteFactory.createSingleNote(
            id = FORCE_NEW_NOTE_EXCEPTION,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )

        insertNewNote.insertNewNote(
            id = newNote.id,
            title = newNote.title,
            stateEvent = InsertNewNoteEvent(newNote.title)
        ).collect { value ->
            assert(
                value?.stateMessage?.response?.message
                    ?.contains(CacheErrors.CACHE_ERROR_UNKNOWN) ?: false
            )
        }

        // confirm network was not changed
        val networkNoteThatWasInserted = noteNetworkDataSource.searchNote(newNote)
        assertTrue { networkNoteThatWasInserted == null }

        // confirm cache was not changed
        val cacheNoteThatWasInserted = noteCacheDataSource.searchNoteById(newNote.id)
        assertTrue { cacheNoteThatWasInserted == null }
    }
}