package bg.tu_varna.sit.chatapp2.client

import bg.tu_varna.sit.chatapp2.config.NGROK_URL
import bg.tu_varna.sit.chatapp2.model.ChatMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class ChatWebSocketClient(private val client: OkHttpClient) {

    private var webSocket: WebSocket? = null

    private val json = Json { ignoreUnknownKeys = true }

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 10)
    val incomingMessages = _incomingMessages.asSharedFlow()

    fun connect(roomId: String, userId: String) {
        val request = Request.Builder()
            .url("wss://$NGROK_URL/chat/$roomId/$userId")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val parsedMessage = json.decodeFromString<ChatMessage>(text)
                    val receivedMessage = parsedMessage.copy(isMine = false)


                    _incomingMessages.tryEmit(receivedMessage);
                } catch (e: Exception) {
//                    e.printStackTrace()
                    println("WebSocket Parse Error: ${e.message}")
                    println("Raw text received: $text")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
            }
        })
    }


    fun sendMessage(message: ChatMessage) {
        try {
            val jsonString = json.encodeToString(message)
            webSocket?.send(jsonString)
//            webSocket?.send(message.text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}