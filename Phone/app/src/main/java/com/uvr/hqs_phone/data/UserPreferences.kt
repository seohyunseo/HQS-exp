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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hqs_prefs")

object UserPreferences {

    private val PARTICIPANT_ID_KEY = stringPreferencesKey("participant_id")
    private val ONBOARDING_DONE_KEY = booleanPreferencesKey("onboarding_done")

    // ── Participant ID ────────────────────────────────────────────────────

    /**
     * Returns the raw participant ID string stored by the researcher/participant
     * (e.g. "01", "3", "12"). Returns "" if not yet set.
     */
    suspend fun getParticipantId(context: Context): String =
        context.dataStore.data.first()[PARTICIPANT_ID_KEY] ?: ""

    /** Persists the raw numeric participant ID string. */
    suspend fun setParticipantId(context: Context, id: String) {
        context.dataStore.edit { it[PARTICIPANT_ID_KEY] = id.trim() }
    }

    /** Live Flow of the raw participant ID — used by the Settings ViewModel. */
    fun participantIdFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[PARTICIPANT_ID_KEY] ?: "" }

    /**
     * Converts a raw participant ID string into the "P"-prefixed, zero-padded format.
     *
     * Examples:
     *   "1"  → "P01"
     *   "01" → "P01"
     *   "12" → "P12"
     *   "100"→ "P100"
     *   ""   → "P00"  (safe fallback for un-set IDs / existing installs)
     */
    fun formattedParticipantId(rawId: String): String {
        val n = rawId.trim().toIntOrNull() ?: return "P00"
        return "P%02d".format(n)
    }

    // ── Onboarding ────────────────────────────────────────────────────────

    suspend fun isOnboardingDone(context: Context): Boolean =
        context.dataStore.data.first()[ONBOARDING_DONE_KEY] ?: false

    suspend fun setOnboardingDone(context: Context) {
        context.dataStore.edit { it[ONBOARDING_DONE_KEY] = true }
    }

    fun onboardingDoneFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_DONE_KEY] ?: false }
}
