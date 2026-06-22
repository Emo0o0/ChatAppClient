package bg.tu_varna.sit.chatapp2.model

import kotlinx.serialization.Serializable

@Serializable
public data class User(val id: String, val name: String)
