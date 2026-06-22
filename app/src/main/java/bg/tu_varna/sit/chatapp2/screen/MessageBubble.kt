package bg.tu_varna.sit.chatapp2.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bg.tu_varna.sit.chatapp2.model.ChatMessage
import bg.tu_varna.sit.chatapp2.model.MessageStatus


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