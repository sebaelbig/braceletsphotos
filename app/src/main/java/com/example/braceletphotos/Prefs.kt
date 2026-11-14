package com.example.braceletphotos

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.store by preferencesDataStore("bracelet")

object Prefs {
    val TAG_ID = stringPreferencesKey("tag_id")
    val FOLDER_ID = stringPreferencesKey("folder_id")
    val LAST_CHECK = stringPreferencesKey("last_check") // RFC 3339 time

    suspend fun get(ctx: Context, key: Preferences.Key<String>) =
        ctx.store.data.first()[key]

    suspend fun set(ctx: Context, key: Preferences.Key<String>, v: String) {
        ctx.store.edit { it[key] = v }
    }
}
