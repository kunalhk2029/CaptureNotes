package com.app.capturenotes.framework.presentation.notedetail

import androidx.lifecycle.LiveData
import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.business.domain.state.*
import com.app.capturenotes.business.interactors.notedetail.NoteDetailInteractors
import com.app.capturenotes.business.interactors.notedetail.UpdateNote.Companion.UPDATE_NOTE_FAILED
import com.app.capturenotes.framework.datasource.cache.model.NoteCacheEntity
import com.app.capturenotes.framework.presentation.common.BaseViewModel
import com.app.capturenotes.framework.presentation.notedetail.state.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton


const val NOTE_DETAIL_ERROR_RETRIEVEING_SELECTED_NOTE = "Error retrieving selected note from bundle."
const val NOTE_DETAIL_SELECTED_NOTE_BUNDLE_KEY = "selectedNote"
const val NOTE_TITLE_CANNOT_BE_EMPTY = "Note title can not be empty."

@ExperimentalCoroutinesApi
@FlowPreview
@Singleton
class NoteDetailViewModel
@Inject
constructor(
    private val noteInteractors: NoteDetailInteractors
): BaseViewModel<NoteDetailViewState>(){

    private val noteInteractionManager: NoteInteractionManager =
        NoteInteractionManager()
    val noteTitleInteractionState: LiveData<NoteInteractionState>
        get() = noteInteractionManager.noteTitleState
    val noteBodyInteractionState: LiveData<NoteInteractionState>
        get() = noteInteractionManager.noteBodyState
    val collapsingToolbarState: LiveData<CollapsingToolbarState>
        get() = noteInteractionManager.collapsingToolbarState

    override fun handleNewData(data: NoteDetailViewState) {
        // no data coming in from requests...
    }

    override fun setStateEvent(stateEvent: StateEvent) {

//        if(canExecuteNewStateEvent(stateEvent)){
            val job: Flow<DataState<NoteDetailViewState>?> = when(stateEvent){

                is NoteDetailStateEvent.UpdateNoteEvent -> {
                    val pk = getNote()?.id
                    if(!isNoteTitleNull() && pk != null){
                        noteInteractors.updateNote.updateNote(
                            note = getNote()!!,
                            stateEvent = stateEvent
                        )
                    }else{
                        emitStateMessageEvent(
                            stateMessage = StateMessage(
                                response = Response(
                                    message = UPDATE_NOTE_FAILED,
                                    uiComponentType = UIComponentType.Dialog(),
                                    messageType = MessageType.Error()
                                )
                            ),
                            stateEvent = stateEvent
                        )
                    }
                }

                is NoteDetailStateEvent.DeleteNoteEvent -> {
                    noteInteractors.deleteNote.deleteNote(
                        note = stateEvent.note,
                        stateEvent = stateEvent
                    )
                }

                is NoteDetailStateEvent.CreateStateMessageEvent -> {
                    emitStateMessageEvent(
                        stateMessage = stateEvent.stateMessage,
                        stateEvent = stateEvent
                    )
                }

                else -> {
                    emitInvalidStateEvent(stateEvent)
                }
            }
            launchJob(stateEvent, job)
//        }
    }

    fun beginPendingDelete(note: Note){
        setStateEvent(
            NoteDetailStateEvent.DeleteNoteEvent(
                note = note
            )
        )
    }

    private fun isNoteTitleNull(): Boolean {
        val title = getNote()?.title
        if (title.isNullOrBlank()) {
            setStateEvent(
                NoteDetailStateEvent.CreateStateMessageEvent(
                    stateMessage = StateMessage(
                        response = Response(
                            message = NOTE_TITLE_CANNOT_BE_EMPTY,
                            uiComponentType = UIComponentType.Dialog(),
                            messageType = MessageType.Info()
                        )
                    )
                )
            )
            return true
        } else {
            return false
        }
    }

    fun getNote(): Note? {
        return getCurrentViewStateOrNew().note
    }

    override fun initNewViewState(): NoteDetailViewState {
        return NoteDetailViewState()
    }

    fun setNote(note: Note?){
        val update = getCurrentViewStateOrNew()
        update.note = note
        setViewState(update)
    }

    fun setCollapsingToolbarState(
        state: CollapsingToolbarState
    ) = noteInteractionManager.setCollapsingToolbarState(state)

    fun updateNote(title: String?, body: String?){
        updateNoteTitle(title)
        updateNoteBody(body)
    }

    fun updateNoteTitle(title: String?){
        if(title == null){
            setStateEvent(
                NoteDetailStateEvent.CreateStateMessageEvent(
                    stateMessage = StateMessage(
                        response = Response(
                            message = NoteCacheEntity.nullTitleError(),
                            uiComponentType = UIComponentType.Dialog(),
                            messageType = MessageType.Error()
                        )
                    )
                )
            )
        }
        else{
            val update = getCurrentViewStateOrNew()
            val updatedNote = update.note?.copy(
                title = title
            )
            update.note = updatedNote
            setViewState(update)
        }
    }

    fun updateNoteBody(body: String?){
        val update = getCurrentViewStateOrNew()
        val updatedNote = update.note?.copy(
            body = body?: ""
        )
        update.note = updatedNote
        setViewState(update)
    }

    fun setNoteInteractionTitleState(state: NoteInteractionState){
        noteInteractionManager.setNewNoteTitleState(state)
    }

    fun setNoteInteractionBodyState(state: NoteInteractionState){
        noteInteractionManager.setNewNoteBodyState(state)
    }

    fun isToolbarCollapsed() = collapsingToolbarState.toString()
        .equals(CollapsingToolbarState.Collapsed().toString())

    fun setIsUpdatePending(isPending: Boolean){
        val update = getCurrentViewStateOrNew()
        update.isUpdatePending = isPending
        setViewState(update)
    }

    fun getIsUpdatePending(): Boolean{
        return getCurrentViewStateOrNew().isUpdatePending?: false
    }

    fun isToolbarExpanded() = collapsingToolbarState.toString()
        .equals(CollapsingToolbarState.Expanded().toString())

    // return true if in EditState
    fun checkEditState() = noteInteractionManager.checkEditState()

    fun exitEditState() = noteInteractionManager.exitEditState()

    fun isEditingTitle() = noteInteractionManager.isEditingTitle()

    fun isEditingBody() = noteInteractionManager.isEditingBody()

    // force observers to refresh
    fun triggerNoteObservers(){
        getCurrentViewStateOrNew().note?.let { note ->
            setNote(note)
        }
    }
}












































