package com.uvr.hqs_phone.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uvr.hqs_phone.data.UserPreferences
import com.uvr.hqs_phone.data.db.DurationSummary
import com.uvr.hqs_phone.data.db.LifelogDatabase
import com.uvr.hqs_phone.data.db.LifelogEntity
import com.uvr.hqs_phone.sync.SyncManager
import com.uvr.hqs_phone.util.KstTimeUtils
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** One-time UI events emitted by the ViewModel. Using Channel prevents
 *  events from re-triggering on screen rotation (unlike StateFlow). */
sealed class SyncUiEvent {
    data class ShowToast(val message: String) : SyncUiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = LifelogDatabase.getDatabase(application).lifelogDao()
    private val today get() = KstTimeUtils.todayKstString()

    /** Raw numeric string entered by the researcher (e.g. "01", "3"). */
    val participantId: StateFlow<String> =
        UserPreferences.participantIdFlow(application)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Formatted participant ID shown in the UI and used in Firebase paths (e.g. "P01"). */
    val formattedParticipantId: StateFlow<String> = participantId
        .map { UserPreferences.formattedParticipantId(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "P00")

    val todayLogs: StateFlow<List<LifelogEntity>> = dao.getByDateFlow(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val physicalEvents: StateFlow<List<LifelogEntity>> = todayLogs
        .map { list -> list.filter { it.category == "PHYSICAL" && it.duration > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val digitalEvents: StateFlow<List<LifelogEntity>> = todayLogs
        .map { list -> list.filter { it.category == "DIGITAL" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val physicalSummary: StateFlow<List<DurationSummary>> =
        dao.getSummaryByDateFlow(today)
            .map { it.filter { s -> s.category == "PHYSICAL" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val digitalSummary: StateFlow<List<DurationSummary>> =
        dao.getSummaryByDateFlow(today)
            .map { it.filter { s -> s.category == "DIGITAL" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Live list of completed SOCIAL (call) events for today, newest first. */
    val socialEvents: StateFlow<List<LifelogEntity>> =
        dao.getSocialByDateFlow(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Total call duration in milliseconds for today's SOCIAL events. */
    val socialTotalDurationMs: StateFlow<Long> = socialEvents
        .map { list -> list.sumOf { it.duration } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Live count of records not yet uploaded to Firebase. */
    val unsyncedCount: StateFlow<Int> = dao.getUnsyncedCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Sync loading state ────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ── One-time UI events via Channel ────────────────────────────────────
    private val _uiEvent = Channel<SyncUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        // No async init required — participantId is driven by DataStore Flow
    }

    /** Persists a new participant ID. Immediately reflected via [participantId] / [formattedParticipantId] flows. */
    fun updateParticipantId(newId: String) {
        viewModelScope.launch {
            UserPreferences.setParticipantId(getApplication(), newId.trim())
        }
    }

    /**
     * Triggers a manual incremental sync.
     *
     * Root cause of infinite loading: Firebase's `.await()` suspends forever
     * when the device is offline — it queues the operation and never throws.
     * Fix: [withTimeout] (30 s) cancels the suspended coroutine and forces
     * the [finally] block to run, which always resets [_isLoading].
     */
    fun triggerManualSync() {
        if (_isLoading.value) return   // Guard against double-tap
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // ── 30-second hard deadline ────────────────────────────────
                // If Firebase hangs (offline / no response), withTimeout throws
                // TimeoutCancellationException after 30 s, which propagates out
                // of SyncManager and is caught below — guaranteeing the finally
                // block always executes.
                val synced = withTimeout(30_000L) {
                    SyncManager.sync(getApplication())
                }

                if (synced >= 0) {
                    _uiEvent.send(
                        SyncUiEvent.ShowToast("✓ Sync complete — $synced record(s) uploaded")
                    )
                } else {
                    _uiEvent.send(
                        SyncUiEvent.ShowToast("✗ Sync failed — check internet connection")
                    )
                }

            } catch (e: TimeoutCancellationException) {
                // Firebase hung for > 30 s — device is likely offline
                Log.w("MainViewModel", "Sync timed out after 30 s")
                _uiEvent.send(
                    SyncUiEvent.ShowToast("✗ Sync timed out — check your internet connection")
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Sync exception: ${e.message}", e)
                _uiEvent.send(
                    SyncUiEvent.ShowToast("✗ Sync failed: ${e.localizedMessage ?: "Unknown error"}")
                )
            } finally {
                // ALWAYS runs — spinner stops regardless of timeout, error, or success
                _isLoading.value = false
            }
        }
    }
}
