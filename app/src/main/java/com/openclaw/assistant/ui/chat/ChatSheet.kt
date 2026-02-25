package com.openclaw.assistant.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openclaw.assistant.ChatScreen
import com.openclaw.assistant.SessionListScreen
import com.openclaw.assistant.SessionListViewModel
import com.openclaw.assistant.SessionUiModel
import com.openclaw.assistant.ui.chat.ChatViewModel
import com.openclaw.assistant.ui.chat.ChatUiState

@Composable
fun ChatSheet(
    chatViewModel: ChatViewModel = viewModel(),
    sessionListViewModel: SessionListViewModel = viewModel()
) {
    var showChat by remember { mutableStateOf(false) }

    val uiState by chatViewModel.uiState.collectAsState()
    val sessions by sessionListViewModel.allSessions.collectAsState()
    val currentSessionId by chatViewModel.currentSessionId.collectAsState()
    val initialSessionTitle by chatViewModel.initialSessionTitle.collectAsState()

    // If a session is selected and we are not showing chat, show it
    LaunchedEffect(currentSessionId) {
        if (currentSessionId != null) {
            showChat = true
        }
    }

    if (showChat && currentSessionId != null) {
        BackHandler {
            showChat = false
        }

        ChatScreen(
            uiState = uiState,
            allSessions = emptyList(), // We don't really use this in ChatScreen for logic other than title, which we handle
            currentSessionId = currentSessionId,
            initialSessionTitle = initialSessionTitle,
            onSendMessage = { chatViewModel.sendMessage(it) },
            onStartListening = { chatViewModel.startListening() },
            onStopListening = { chatViewModel.stopListening() },
            onStopSpeaking = { chatViewModel.stopSpeaking() },
            onInterruptAndListen = { chatViewModel.interruptAndListen() },
            onBack = { showChat = false },
            onAgentSelected = { chatViewModel.setAgent(it) },
            onAcceptGatewayTrust = { chatViewModel.acceptGatewayTrust() },
            onDeclineGatewayTrust = { chatViewModel.declineGatewayTrust() }
        )
    } else {
        SessionListScreen(
            sessions = sessions,
            isGatewayConfigured = sessionListViewModel.isGatewayConfigured,
            isHttpConfigured = sessionListViewModel.isHttpConfigured,
            onBack = { /* Possibly close app or switch tab? */ },
            onSessionClick = { session ->
                sessionListViewModel.setUseNodeChat(session.isGateway)
                chatViewModel.selectSession(session.id)
                showChat = true
            },
            onCreateSession = { name, isGateway ->
                sessionListViewModel.setUseNodeChat(isGateway)
                sessionListViewModel.createSession(name, isGateway) { sessionId, _ ->
                    chatViewModel.selectSession(sessionId)
                    showChat = true
                }
            },
            onDeleteSession = { sessionId, isGateway ->
                sessionListViewModel.deleteSession(sessionId, isGateway)
                if (currentSessionId == sessionId) {
                    showChat = false
                }
            }
        )
    }
}
