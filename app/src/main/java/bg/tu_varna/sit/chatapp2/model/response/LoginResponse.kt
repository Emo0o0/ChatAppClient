package bg.tu_varna.sit.chatapp2.model.response

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(val id: Long, val username: String)