package bg.tu_varna.sit.chatapp2.model.response

import bg.tu_varna.sit.chatapp2.model.User
import kotlinx.serialization.Serializable

@Serializable
class UserResponse(
    val users: List<User>
)