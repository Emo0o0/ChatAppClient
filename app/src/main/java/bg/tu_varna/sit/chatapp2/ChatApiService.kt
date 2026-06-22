package bg.tu_varna.sit.chatapp2

import bg.tu_varna.sit.chatapp2.model.request.DeleteMessageRequest
import bg.tu_varna.sit.chatapp2.model.request.EditMessageRequest
import bg.tu_varna.sit.chatapp2.model.request.LoginRequest
import bg.tu_varna.sit.chatapp2.model.request.RegisterRequest
import bg.tu_varna.sit.chatapp2.model.response.ChatRoomResponse
import bg.tu_varna.sit.chatapp2.model.response.DeleteMessageResponse
import bg.tu_varna.sit.chatapp2.model.response.EditMessageResponse
import bg.tu_varna.sit.chatapp2.model.response.GetChatMessageListResponse
import bg.tu_varna.sit.chatapp2.model.response.LoginResponse
import bg.tu_varna.sit.chatapp2.model.response.RegisterResponse
import bg.tu_varna.sit.chatapp2.model.response.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApiService {
    @GET("/user/users")
    suspend fun getUsers(): UserResponse

    @POST("/user/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/user/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("/chat/private/{userId1}/{userId2}")
    suspend fun getPrivateRoom(
        @Path("userId1") currentUserId: String,
        @Path("userId2") otherUserId: String,
    ): ChatRoomResponse


    //Chat messages API
    @PATCH("/chat/edit")
    suspend fun editMessage(@Body request: EditMessageRequest): EditMessageResponse

    @HTTP(method = "DELETE", path = "/chat/delete", hasBody = true)
    suspend fun deleteMessage(@Body request: DeleteMessageRequest): DeleteMessageResponse

    @GET("/chat/last/{roomId}_{limit}")
    suspend fun getLastMessages(
        @Path("roomId") roomId: Long,
        @Path("limit") limit: Int
    ): GetChatMessageListResponse

    @GET("/chat/last/{roomId}_{msgId}_{limit}")
    suspend fun getBeforeMessages(
        @Path("roomId") roomId: Long,
        @Path("msgId") msgId: Long,
        @Path("limit") limit: Int
    ): GetChatMessageListResponse
}