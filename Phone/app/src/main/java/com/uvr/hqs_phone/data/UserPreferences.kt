package com.uvr.hqs_phone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hqs_prefs")

object UserPreferences {

    private val UUID_KEY = stringPreferencesKey("user_uuid")
    private val ONBOARDING_DONE_KEY = booleanPreferencesKey("onboarding_done")

    /** Returns existing UUID or generates and persists a new one ("User-XXXXXXXX"). */
    suspend fun getUserUUID(context: Context): String {
        val prefs = context.dataStore.data.first()
        prefs[UUID_KEY]?.let { return it }
        val newId = "User-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        context.dataStore.edit { it[UUID_KEY] = newId }
        return newId
    }

    suspend fun isOnboardingDone(context: Context): Boolean =
        context.dataStore.data.first()[ONBOARDING_DONE_KEY] ?: false

    suspend fun setOnboardingDone(context: Context) {
        context.dataStore.edit { it[ONBOARDING_DONE_KEY] = true }
    }

    fun onboardingDoneFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_DONE_KEY] ?: false }
}
