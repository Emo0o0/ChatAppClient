package bg.tu_varna.sit.chatapp2.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Long? = null,
    val text: String,
    val isMine: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT
)
