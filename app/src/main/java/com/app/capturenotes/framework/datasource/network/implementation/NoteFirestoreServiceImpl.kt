package com.app.capturenotes.framework.datasource.network.implementation

import com.app.capturenotes.business.domain.model.Note
import com.app.capturenotes.framework.datasource.network.abstraction.NoteFirestoreService
import com.app.capturenotes.framework.datasource.network.mappers.NetworkMapper
import com.app.capturenotes.framework.datasource.network.model.NoteNetworkEntity
import com.app.capturenotes.framework.presentation.BaseApplication
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Singleton
class NoteFirestoreServiceImpl
@Inject
constructor(
    private val application: BaseApplication,
    private val firestore: FirebaseFirestore,
    private val networkMapper: NetworkMapper,
) : NoteFirestoreService {

    override suspend fun insertOrUpdateNote(note: Note) {
        val job = Job()
        getLoggedInUser()?.let { USER_ID ->
            val entity = networkMapper.mapToEntity(note)
            entity.updated_at = Timestamp.now() // for updates
            firestore
                .collection(NOTES_COLLECTION)
                .document(USER_ID)
                .collection(NOTES_COLLECTION)
                .document(entity.id)
                .set(entity)
                .addOnCompleteListener {
                    job.complete()
                }
        }
        while (!job.isCompleted) {
        }
    }

    override suspend fun deleteNote(primaryKey: String) {
        val job = Job()
        getLoggedInUser()?.let { USER_ID ->
            firestore
                .collection(NOTES_COLLECTION)
                .document(USER_ID)
                .collection(NOTES_COLLECTION)
                .document(primaryKey)
                .delete()
                .addOnCompleteListener {
                    job.complete()
                }
        }
        while (!job.isCompleted) {
        }
    }

    override suspend fun insertDeletedNote(note: Note) {
        val job = Job()
        getLoggedInUser()?.let { USER_ID ->
            val entity = networkMapper.mapToEntity(note)
            firestore
                .collection(DELETES_COLLECTION)
                .document(USER_ID)
                .collection(NOTES_COLLECTION)
                .document(entity.id)
                .set(entity)
                .addOnCompleteListener {
                    job.complete()
                }
        }
        while (!job.isCompleted) {
        }
    }

    override suspend fun insertDeletedNotes(notes: List<Note>) {
        val job = Job()
        getLoggedInUser()?.let { USER_ID ->
            if (notes.size > 500) {
                throw Exception("Cannot delete more than 500 notes at a time in firestore.")
            }

            val collectionRef = firestore
                .collection(DELETES_COLLECTION)
                .document(USER_ID)
                .collection(NOTES_COLLECTION)
            firestore.runBatch { batch ->
                for (note in notes) {
                    val documentRef = collectionRef.document(note.id)
                    batch.set(documentRef, networkMapper.mapToEntity(note))
                }
            }.addOnCompleteListener {
                job.complete()
            }
        }
        while (!job.isCompleted) {
        }
    }

    override suspend fun deleteDeletedNote(note: Note) {
        val job = Job()
        getLoggedInUser()?.let { USER_ID ->
            val entity = networkMapper.mapToEntity(note)
            firestore
                .collection(DELETES_COLLECTION)
                .document(USER_ID)
                .collection(NOTES_COLLECTION)
                .document(entity.id)
                .delete()
                .addOnCompleteListener {
                    job.complete()
                }
        }
        while (!job.isCompleted) {
        }
    }


    // used in testing
    override suspend fun deleteAllNotes() {
        getLoggedInUser()?.let { USER_ID ->
            firestore
                .collection(NOTES_COLLECTION)
                .document(USER_ID)
                .delete().asDeferred().await()
            firestore
                .collection(DELETES_COLLECTION)
                .document(USER_ID)
                .delete().asDeferred().await()
        }
    }

    override suspend fun getDeletedNotes(): List<Note> {
        val list = getLoggedInUser()?.let { USER_ID ->
            networkMapper.entityListToNoteList(
                firestore
                    .collection(DELETES_COLLECTION)
                    .document(USER_ID)
                    .collection(NOTES_COLLECTION)
                    .get()
                    .asDeferred().await()
                    .toObjects(NoteNetworkEntity::class.java)
            )
        } ?: listOf()
        return list
    }

    override suspend fun searchNote(note: Note): Note? {
        val job = Job()
        val n = getLoggedInUser()?.let { USER_ID ->
            firestore
                .collection(NOTES_COLLECTION)
                .document(USER_ID)
                .collection(NOTES_COLLECTION)
                .document(note.id)
                .get()
                .addOnCompleteListener {
                    job.complete()
                }
                .result
                .toObject(NoteNetworkEntity::class.java)?.let {
                    networkMapper.mapFromEntity(it)
                }
        }
        while (!job.isCompleted) {
        }
        return n
    }

    override suspend fun getAllNotes(): List<Note> {
        return getLoggedInUser()?.let { USER_ID ->
            networkMapper.entityListToNoteList(
                firestore
                    .collection(NOTES_COLLECTION)
                    .document(USER_ID)
                    .collection(NOTES_COLLECTION)
                    .get()
                    .asDeferred()
                    .await()
                    .toObjects(NoteNetworkEntity::class.java)
            )
        } ?: listOf()
    }

    override suspend fun insertOrUpdateNotes(notes: List<Note>) {
        val job = Job()
        getLoggedInUser()?.let { USER_ID ->
            if (notes.size > 500) {
                throw Exception("Cannot insert more than 500 notes at a time into firestore.")
            }

            val collectionRef = firestore
                .collection(NOTES_COLLECTION)
                .document(USER_ID)
                .collection(NOTES_COLLECTION)

            firestore.runBatch { batch ->
                for (note in notes) {
                    val entity = networkMapper.mapToEntity(note)
                    entity.updated_at = Timestamp.now()
                    val documentRef = collectionRef.document(note.id)
                    batch.set(documentRef, entity)
                }
            }.addOnCompleteListener {
                job.complete()
            }
        }
        while (!job.isCompleted) {
        }
    }

    fun getLoggedInUser(): String? {
        return GoogleSignIn.getLastSignedInAccount(application.baseContext)?.email
    }

    companion object {
        const val NOTES_COLLECTION = "notes"
        const val DELETES_COLLECTION = "deletes"
    }
}
