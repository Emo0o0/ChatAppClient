package bg.tu_varna.sit.chatapp2.model.response

import kotlinx.serialization.Serializable

@Serializable
data class GetChatMessageResponse(
    val id: Long,
    val senderId: Long,
    val content: String,
    val timestamp: String,
    val messageStatus: String
)
