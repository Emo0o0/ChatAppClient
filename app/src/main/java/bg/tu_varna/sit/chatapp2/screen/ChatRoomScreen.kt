package bg.tu_varna.sit.chatapp2.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bg.tu_varna.sit.chatapp2.model.ChatMessage
import bg.tu_varna.sit.chatapp2.view.MessageBubble


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    userName: String,
    messages: List<ChatMessage>,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onEditMessage: (Long, String) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var messageBeingEdited by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(listState, messages.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= messages.size - 3) {
                    onLoadMore()
                }
            }
    }

    // When the user selects a message to edit, populate the input field with its current text
    LaunchedEffect(messageBeingEdited) {
        textInput = messageBeingEdited?.text ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(userName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Chat History
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                reverseLayout = true
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        onEditClick = { messageBeingEdited = message },
                        onDeleteClick = { message.id?.let { onDeleteMessage(it) } }
                    )
                }
            }

            // Edit Mode Indicator
            if (messageBeingEdited != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Editing message...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { messageBeingEdited = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Edit")
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            if (messageBeingEdited != null && messageBeingEdited!!.id != null) {
                                // Trigger Edit API
                                onEditMessage(messageBeingEdited!!.id!!, textInput)
                                messageBeingEdited = null // Clear edit state
                            } else {
                                // Trigger Send WebSocket
                                onSendMessage(textInput)
                            }
                            textInput = "" // Clear input
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}