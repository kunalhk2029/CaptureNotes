package com.app.capturenotes.business.interactors.notelist

import com.app.capturenotes.business.data.cache.CacheResponseHandler
import com.app.capturenotes.business.data.cache.abstraction.NoteCacheDataSource
import com.app.capturenotes.business.data.util.safeCacheCall
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.state.*
import com.app.capturenotes.framework.presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SearchNotes(
    private val noteCacheDataSource: NoteCacheDataSource,
) {

    fun searchNotes(
        query: String,
        filterAndOrder: String,
        page: Int,
        stateEvent: StateEvent,
    ): Flow<DataState<NoteListViewState>?> = flow {

        var updatedPage = page
        if (page <= 0) {
            updatedPage = 1
        }
        val cacheResult = safeCacheCall(Dispatchers.IO) {
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

    companion object {
        const val SEARCH_NOTES_SUCCESS = "Successfully retrieved list of notes."
        const val SEARCH_NOTES_NO_MATCHING_RESULTS = "There are no notes that match that query."
    }
}