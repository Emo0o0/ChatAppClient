package bg.tu_varna.sit.chatapp2.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bg.tu_varna.sit.chatapp2.model.ChatMessage
import bg.tu_varna.sit.chatapp2.model.MessageStatus
import bg.tu_varna.sit.chatapp2.model.User


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(users: List<User>, onUserClick: (User) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Chats") }) }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(users) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onUserClick(user) }
                ) {
                    Text(
                        text = user.name,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

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

@Composable
fun MessageBubble(
    message: ChatMessage,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val alignment = if (message.isMine) Alignment.End else Alignment.Start
    val isDeleted = message.status == MessageStatus.DELETED

    val backgroundColor = when {
        isDeleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        message.isMine -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isDeleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = backgroundColor,
                modifier = Modifier.pointerInput(message) {
                    detectTapGestures(
                        onLongPress = {
                            // Only allow menu if it's yours, has an ID, and isn't deleted
                            if (message.isMine && message.id != null && !isDeleted) {
                                showMenu = true
                            }
                        }
                    )
                }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
                ) {
                    // Main Message Text
                    Text(
                        text = if (isDeleted) "This message was deleted" else message.text,
                        color = textColor,
                        fontStyle = if (isDeleted) FontStyle.Italic else FontStyle.Normal
                    )

                    // Status Indicators (Edited tag or Sent/Delivered checkmarks)
                    if (!isDeleted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (message.status == MessageStatus.EDITED) {
                                Text(
                                    text = "(edited)",
                                    fontSize = 10.sp,
                                    color = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }

                            // Show checkmarks only for the sender's own messages
                            if (message.isMine) {
                                val icon =
                                    if (message.status == MessageStatus.DELIVERED) Icons.Default.Done else Icons.Default.Check
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Message Status",
                                    modifier = Modifier.size(14.dp),
                                    tint = textColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // The Dropdown Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEditClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}


@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterNavigate: () -> Unit,
    errorMessage: String?
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to ChatApp", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onLoginClick(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = email.isNotBlank() && password.isNotBlank()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRegisterNavigate) {
            Text(
                text = "Don't have an account? Register here",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String) -> Unit,
    onBackToLoginClick: () -> Unit,
    errorMessage: String?
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create an Account", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onRegisterClick(email, username, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = email.isNotBlank() && username.isNotBlank() && password.isNotBlank()
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBackToLoginClick) {
            Text(
                text = "Already have an account? Login",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

