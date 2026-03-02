package com.openclaw.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.assistant.data.repository.ChatRepository
import com.openclaw.assistant.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionUiModel(
    val id: String,
    val title: String,
    val createdAt: Long
)

class SessionListViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepository = ChatRepository.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)

    val isHttpConfigured: Boolean
        get() = settingsRepository.isConfigured()

    val allSessions: StateFlow<List<SessionUiModel>> = chatRepository.allSessionsWithLatestTime.map { localSessions ->
        localSessions.map { session ->
            SessionUiModel(
                id = session.id,
                title = session.title,
                createdAt = session.latestMessageTime ?: session.createdAt
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshSessions()
    }

    fun refreshSessions() {
    }

    fun createSession(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = chatRepository.createSession(name.trim())
            onCreated(id)
        }
    }

    fun renameSession(sessionId: String, newName: String) {
        viewModelScope.launch {
            chatRepository.renameSession(sessionId, newName.trim())
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
        }
    }
}
