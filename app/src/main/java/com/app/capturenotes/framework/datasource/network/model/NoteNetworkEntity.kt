package com.app.capturenotes.framework.datasource.network.model

import com.google.firebase.Timestamp


data class NoteNetworkEntity(

    var id: String,

    var title: String,

    var body: String,

    var updated_at: Timestamp,

    var created_at: Timestamp

){

    // no arg constructor for firestore
    constructor(): this(
        "",
        "",
        "",
        Timestamp.now(),
        Timestamp.now()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoteNetworkEntity

        if (id != other.id) return false
        if (title != other.title) return false
        if (body != other.body) return false
        if (created_at != other.created_at) return false

        return true
    }
}