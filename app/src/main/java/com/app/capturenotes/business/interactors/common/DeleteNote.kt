package com.app.capturenotes.business.interactors.common

import com.app.capturenotes.business.data.cache.CacheResponseHandler
import com.app.capturenotes.business.data.cache.abstraction.NoteCacheDataSource
import com.app.capturenotes.business.data.network.abstraction.NoteNetworkDataSource
import com.app.capturenotes.business.data.util.safeApiCall
import com.app.capturenotes.business.data.util.safeCacheCall
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.state.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeleteNote<ViewState>(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val noteNetworkDataSource: NoteNetworkDataSource,
) {

    fun deleteNote(
        note: Note,
        stateEvent: StateEvent,
    ): Flow<DataState<ViewState>?> = flow {

        val cacheResult = safeCacheCall(IO) {
            noteCacheDataSource.deleteNote(note.id)
        }

        val response = object : CacheResponseHandler<ViewState, Int>(
            response = cacheResult,
            stateEvent = stateEvent
        ) {
            override suspend fun handleSuccess(resultObj: Int): DataState<ViewState>? {
                return if (resultObj > 0) {
                    DataState.data(
                        response = Response(
                            message = DELETE_NOTE_SUCCESS,
                            uiComponentType = UIComponentType.None(),
                            messageType = MessageType.Success()
                        ),
                        data = null,
                        stateEvent = stateEvent
                    )
                } else {
                    DataState.data(
                        response = Response(
                            message = DELETE_NOTE_FAILED,
                            uiComponentType = UIComponentType.Toast(),
                            messageType = MessageType.Error()
                        ),
                        data = null,
                        stateEvent = stateEvent
                    )
                }
            }
        }.getResult()

        emit(response)

        // update network
        updateNetwork(response?.stateMessage?.response?.message,note)

    }
    private suspend fun updateNetwork(response:String?,note: Note){
        if (response.equals(DELETE_NOTE_SUCCESS)) {
            // delete from 'notes' node
            safeApiCall(IO) {
                noteNetworkDataSource.deleteNote(note.id)
            }
            // insert into 'deletes' node
            safeApiCall(IO) {
                noteNetworkDataSource.insertDeletedNote(note)
            }
        }
    }

    companion object {
        const val DELETE_NOTE_SUCCESS = "Successfully deleted note."
        const val DELETE_NOTE_PENDING = "Delete pending..."
        const val DELETE_NOTE_FAILED = "Failed to delete note."
        const val DELETE_ARE_YOU_SURE = "Are you sure you want to delete this?"
    }
}