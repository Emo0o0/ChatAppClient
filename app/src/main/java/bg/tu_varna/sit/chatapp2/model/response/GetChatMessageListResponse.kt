package bg.tu_varna.sit.chatapp2.model.response

import kotlinx.serialization.Serializable

@Serializable
data class GetChatMessageListResponse(val messages: List<GetChatMessageResponse>)
