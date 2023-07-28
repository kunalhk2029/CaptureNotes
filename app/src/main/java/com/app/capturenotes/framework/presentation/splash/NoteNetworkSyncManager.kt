package com.app.capturenotes.framework.presentation.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.app.capturenotes.business.interactors.splash.SyncDeletedNotes
import com.app.capturenotes.business.interactors.splash.SyncNotes
import com.app.capturenotes.util.printLogD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteNetworkSyncManager
@Inject
constructor(
    private val syncNotes: SyncNotes,
    private val syncDeletedNotes: SyncDeletedNotes
){

    private val _hasSyncBeenExecuted: MutableLiveData<Boolean> = MutableLiveData(false)

    val hasSyncBeenExecuted: LiveData<Boolean>
            get() = _hasSyncBeenExecuted

    fun executeDataSync(coroutineScope: CoroutineScope){
        if(_hasSyncBeenExecuted.value!!){
            return
        }

        val syncJob = coroutineScope.launch {
            val deletesJob = launch {
                printLogD("SyncNotes",
                    "syncing deleted notes.")
                syncDeletedNotes.syncDeletedNotes()
            }
            deletesJob.join()

            launch {
                printLogD("SyncNotes",
                    "syncing notes.")
                syncNotes.syncNotes()
            }
        }
        syncJob.invokeOnCompletion {
            CoroutineScope(Main).launch{
                _hasSyncBeenExecuted.value = true
            }
        }
    }
}