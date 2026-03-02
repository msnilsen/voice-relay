package com.openclaw.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.openclaw.assistant.data.SettingsRepository
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.openclaw.assistant.ui.theme.VoiceRelayTheme

class SessionListActivity : ComponentActivity() {

    private val viewModel: SessionListViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val tag = try {
            SettingsRepository.getInstance(newBase).appLanguage.trim()
        } catch (e: Exception) { "" }
        if (tag.isNotBlank()) {
            val locale = java.util.Locale.forLanguageTag(tag)
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoiceRelayTheme {
                val sessions by viewModel.allSessions.collectAsState()
                SessionListScreen(
                    sessions = sessions,
                    onBack = { finish() },
                    onSessionClick = { session ->
                        startActivity(Intent(this, ChatActivity::class.java).apply {
                            putExtra(ChatActivity.EXTRA_SESSION_ID, session.id)
                        })
                    },
                    onCreateSession = { name ->
                        viewModel.createSession(name) { sessionId ->
                            startActivity(Intent(this, ChatActivity::class.java).apply {
                                putExtra(ChatActivity.EXTRA_SESSION_ID, sessionId)
                                putExtra(ChatActivity.EXTRA_SESSION_TITLE, name)
                            })
                        }
                    },
                    onDeleteSession = { sessionId ->
                        viewModel.deleteSession(sessionId)
                    },
                    onRenameSession = { sessionId, newName ->
                        viewModel.renameSession(sessionId, newName)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSessions()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    sessions: List<SessionUiModel>,
    onBack: () -> Unit,
    onSessionClick: (SessionUiModel) -> Unit,
    onCreateSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit = { _, _ -> }
) {
    var sessionToDelete by remember { mutableStateOf<SessionUiModel?>(null) }
    var sessionToRename by remember { mutableStateOf<SessionUiModel?>(null) }
    var sessionActionTarget by remember { mutableStateOf<SessionUiModel?>(null) }
    var showNameInputDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var scrollTrigger by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scrollTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(scrollTrigger) {
        if (sessions.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.conversations_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            val newSessionName = stringResource(R.string.new_chat)
            FloatingActionButton(onClick = { showNameInputDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = newSessionName)
            }
        }
    ) { paddingValues ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_sessions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionListItem(
                        session = session,
                        onClick = { onSessionClick(session) },
                        onLongClick = { sessionActionTarget = session }
                    )
                }
            }
        }
    }

    // HTTP name input dialog
    if (showNameInputDialog) {
        var inputName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNameInputDialog = false },
            title = { Text(stringResource(R.string.new_chat)) },
            text = { 
                Column {
                    Text(stringResource(R.string.enter_session_name))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text(stringResource(R.string.session_name_optional)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameInputDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showNameInputDialog = false
                    val finalName = if (inputName.isNotBlank()) inputName else "New Conversation"
                    onCreateSession(finalName)
                }) {
                    Text(stringResource(R.string.create))
                }
            }
        )
    }

    // Long-press action menu: Rename / Delete
    sessionActionTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionActionTarget = null },
            title = { Text(session.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
            text = null,
            confirmButton = {
                TextButton(onClick = {
                    sessionActionTarget = null
                    sessionToRename = session
                }) {
                    Text(stringResource(R.string.rename_session))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    sessionActionTarget = null
                    sessionToDelete = session
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Rename dialog
    sessionToRename?.let { session ->
        var renameInput by remember(session.id) { mutableStateOf(session.title) }
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text(stringResource(R.string.rename_session_title)) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text(stringResource(R.string.rename_session_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = renameInput.trim()
                        if (name.isNotBlank()) {
                            onRenameSession(session.id, name)
                        }
                        sessionToRename = null
                    },
                    enabled = renameInput.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(stringResource(R.string.delete_session_title)) },
            text = {
                Text(stringResource(R.string.delete_session_message, session.title))
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(session.id)
                    sessionToDelete = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionListItem(
    session: SessionUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dateText = remember(session.createdAt) {
                        java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(session.createdAt))
                    }
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
