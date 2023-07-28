package com.app.capturenotes.di

import com.app.capturenotes.business.data.NoteDataFactory
import com.app.capturenotes.business.data.cache.FakeNoteCacheDataSourceImpl
import com.app.capturenotes.business.data.cache.abstraction.NoteCacheDataSource
import com.app.capturenotes.business.data.network.FakeNoteNetworkDataSourceImpl
import com.app.capturenotes.business.data.network.abstraction.NoteNetworkDataSource
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.model.NoteFactory
import java.text.SimpleDateFormat
import java.util.*
import com.app.capturenotes.business.domain.util.DateUtil
import kotlin.collections.HashMap

class DependencyContainer {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH)
    val dateUtil = DateUtil(dateFormat)
    lateinit var noteNetworkDataSource: NoteNetworkDataSource
    lateinit var noteCacheDataSource: NoteCacheDataSource
    lateinit var noteFactory: NoteFactory
    lateinit var noteDataFactory: NoteDataFactory

    // data sets

    fun build() {
        this.javaClass.classLoader?.let { classLoader ->
            noteDataFactory = NoteDataFactory(classLoader)

            noteFactory = NoteFactory(dateUtil)
            noteNetworkDataSource = FakeNoteNetworkDataSourceImpl(
                dateUtil=dateUtil,
                notesData = noteDataFactory.produceHashMapOfNotes(
                    noteDataFactory.produceListOfNotes()
                ),
                deletedNotesData = HashMap()
            )
            noteCacheDataSource = FakeNoteCacheDataSourceImpl(
                notesData = noteDataFactory.produceHashMapOfNotes(
                    noteDataFactory.produceListOfNotes()
                ),
                dateUtil = dateUtil
            )
        }
    }
}