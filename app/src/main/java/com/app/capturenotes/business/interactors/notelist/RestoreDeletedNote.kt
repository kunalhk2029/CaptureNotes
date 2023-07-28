package com.app.capturenotes.business.interactors.notelist

import com.app.capturenotes.business.data.cache.CacheResponseHandler
import com.app.capturenotes.business.data.cache.abstraction.NoteCacheDataSource
import com.app.capturenotes.business.data.network.abstraction.NoteNetworkDataSource
import com.app.capturenotes.business.data.util.safeApiCall
import com.app.capturenotes.business.data.util.safeCacheCall
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.state.*
import com.app.capturenotes.framework.presentation.notelist.state.NoteListViewState
import com.app.capturenotes.framework.presentation.notelist.state.NoteListViewState.NotePendingDelete
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RestoreDeletedNote(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val noteNetworkDataSource: NoteNetworkDataSource
) {

    fun restoreDeletedNote(
        note: Note,
        stateEvent: StateEvent
    ): Flow<DataState<NoteListViewState>?> = flow {

        val cacheResult = safeCacheCall(IO) {
            noteCacheDataSource.insertNote(note)
        }

        val response = object : CacheResponseHandler<NoteListViewState, Long>(
            response = cacheResult,
            stateEvent = stateEvent
        ) {
            override suspend fun handleSuccess(resultObj: Long): DataState<NoteListViewState>? {
                return if (resultObj > 0) {
                    val viewState =
                        NoteListViewState(
                            notePendingDelete = NotePendingDelete(
                                note = note
                            )
                        )
                    DataState.data(
                        response = Response(
                            message = RESTORE_NOTE_SUCCESS,
                            uiComponentType = UIComponentType.Toast(),
                            messageType = MessageType.Success()
                        ),
                        data = viewState,
                        stateEvent = stateEvent
                    )
                } else {
                    DataState.data(
                        response = Response(
                            message = RESTORE_NOTE_FAILED,
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

        updateNetwork(response?.stateMessage?.response?.message, note)
    }

    private suspend fun updateNetwork(response: String?, note: Note) {
        if (response.equals(RESTORE_NOTE_SUCCESS)) {

            // insert into "notes" node
            safeApiCall(IO) {
                noteNetworkDataSource.insertOrUpdateNote(note)
            }

            // remove from "deleted" node
            safeApiCall(IO) {
                noteNetworkDataSource.deleteDeletedNote(note)
            }
        }
    }

    companion object {
        const val RESTORE_NOTE_SUCCESS = "Successfully restored the deleted note."
        const val RESTORE_NOTE_FAILED = "Failed to restore the deleted note."
    }
}
