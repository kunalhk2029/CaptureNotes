package com.app.capturenotes.business.domain.model

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable

@Parcelize
data class Note(
    val id: String,
    val title: String,
    val body: String,
    val updated_at: String,
    val created_at: String
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Note

        if (id != other.id) return false
        if (title != other.title) return false
        if (body != other.body) return false
        if (created_at != other.created_at) return false

        return true
    }
}