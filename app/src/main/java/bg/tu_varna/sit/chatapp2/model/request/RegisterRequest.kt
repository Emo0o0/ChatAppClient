package bg.tu_varna.sit.chatapp2.model.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val username: String, val password: String)
