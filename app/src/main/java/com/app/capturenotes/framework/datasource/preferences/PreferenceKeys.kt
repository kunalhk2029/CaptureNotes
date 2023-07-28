package com.app.capturenotes.framework.datasource.preferences

class PreferenceKeys {
    companion object{
        // Shared Preference Files:
        const val NOTE_PREFERENCES: String = "com.app.capturenotes.notes"
        // Shared Preference Keys
       const val NOTE_FILTER: String = "${NOTE_PREFERENCES}.NOTE_FILTER"
       const val NOTE_ORDER: String = "${NOTE_PREFERENCES}.NOTE_ORDER"
    }
}