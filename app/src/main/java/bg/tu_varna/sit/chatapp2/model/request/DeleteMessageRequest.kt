package bg.tu_varna.sit.chatapp2.model.request

import kotlinx.serialization.Serializable

@Serializable
data class DeleteMessageRequest(val messageId: Long)
