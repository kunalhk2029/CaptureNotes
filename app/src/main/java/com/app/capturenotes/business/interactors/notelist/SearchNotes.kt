package com.app.capturenotes.business.interactors.notelist

import com.app.capturenotes.business.data.cache.CacheResponseHandler
import com.app.capturenotes.business.data.cache.abstraction.NoteCacheDataSource
import com.app.capturenotes.business.data.util.safeCacheCall
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.state.*
import com.app.capturenotes.business.interactors.splash.SyncDeletedNotes
import com.app.capturenotes.business.interactors.splash.SyncNotes
import com.app.capturenotes.framework.presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchNotes(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val syncNotes: SyncNotes,
    private val syncDeletedNotes: SyncDeletedNotes,
) {

    fun searchNotes(
        query: String,
        filterAndOrder: String,
        page: Int,
        doNetworkSync: Boolean,
        stateEvent: StateEvent,
    ): Flow<DataState<NoteListViewState>?> = flow {

        var updatedPage = page
        if (page <= 0) {
            updatedPage = 1
        }
        if (doNetworkSync) {
            executeDataSync()
        }

        val cacheResult = safeCacheCall(IO) {
            noteCacheDataSource.searchNotes(
                query = query,
                filterAndOrder = filterAndOrder,
                page = updatedPage
            )
        }

        val response = object : CacheResponseHandler<NoteListViewState, List<Note>>(
            response = cacheResult,
            stateEvent = stateEvent
        ) {
            override suspend fun handleSuccess(resultObj: List<Note>): DataState<NoteListViewState>? {
                var message: String? =
                    SEARCH_NOTES_SUCCESS
                var uiComponentType: UIComponentType? = UIComponentType.None()
                if (resultObj.size == 0) {
                    message =
                        SEARCH_NOTES_NO_MATCHING_RESULTS
                    uiComponentType = UIComponentType.Toast()
                }
                return DataState.data(
                    response = Response(
                        message = message,
                        uiComponentType = uiComponentType as UIComponentType,
                        messageType = MessageType.Success()
                    ),
                    data = NoteListViewState(
                        noteList = ArrayList(resultObj)
                    ),
                    stateEvent = stateEvent
                )
            }
        }.getResult()

        emit(response)
    }

    suspend fun executeDataSync(): Boolean {
        val job = Job()
        withContext(IO) {
            val syncJob = launch {
                val deletesJob = launch {
                    syncDeletedNotes.syncDeletedNotes()
                }
                deletesJob.join()

                launch {
                    syncNotes.syncNotes()
                }
            }
            syncJob.invokeOnCompletion {
                job.complete()
            }
        }
        while (job.isCompleted == false) {
        }

        return true
    }

    companion object {
        const val SEARCH_NOTES_SUCCESS = "Successfully retrieved list of notes."
        const val SEARCH_NOTES_NO_MATCHING_RESULTS = "There are no notes that match that query."
    }
}