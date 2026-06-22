package bg.tu_varna.sit.chatapp2.view

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bg.tu_varna.sit.chatapp2.ChatApiService
import bg.tu_varna.sit.chatapp2.client.ChatWebSocketClient
import bg.tu_varna.sit.chatapp2.config.NGROK_URL
import bg.tu_varna.sit.chatapp2.model.ChatMessage
import bg.tu_varna.sit.chatapp2.model.MessageStatus
import bg.tu_varna.sit.chatapp2.model.User
import bg.tu_varna.sit.chatapp2.model.request.DeleteMessageRequest
import bg.tu_varna.sit.chatapp2.model.request.EditMessageRequest
import bg.tu_varna.sit.chatapp2.model.request.LoginRequest
import bg.tu_varna.sit.chatapp2.model.request.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("ngrok-skip-browser-warning", "true")
                .build()
            chain.proceed(request)
        }
        .build()
    private val webSocketClient = ChatWebSocketClient(okHttpClient)
    private val json = Json { ignoreUnknownKeys = true }


    private val retrofit = Retrofit.Builder()
        .baseUrl("https://${NGROK_URL}/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val apiService = retrofit.create(ChatApiService::class.java)

    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var isLoadingMore = false
    private var isLastPage = false

    init {

        viewModelScope.launch {
            application.dataStore.data.map { preferences ->
                preferences[USER_ID_KEY]
            }.collect { savedId ->
                _currentUserId.value = savedId
            }
        }

        viewModelScope.launch {
            webSocketClient.incomingMessages.collect { newMessage ->
                _messages.value = listOf(newMessage) + _messages.value
            }
        }
    }

    fun connectToRoom(roomId: String) {
        _messages.value = emptyList()
        isLoadingMore = false
        isLastPage = false

        val loggedUserId = _currentUserId.value ?: return
        webSocketClient.connect(roomId, loggedUserId)
        loadInitialMessages(roomId)
    }

    fun sendMessage(text: String) {
        if (text.isNotBlank()) {
            val myMessage = ChatMessage(text = text, isMine = true)
            webSocketClient.sendMessage(myMessage)

            _messages.value = listOf(myMessage) + _messages.value
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketClient.disconnect()
    }

    fun login(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Call Quarkus backend
                val response = apiService.login(LoginRequest(username, password))

                // Save the ID to DataStore permanently
                getApplication<Application>().dataStore.edit { preferences ->
                    preferences[USER_ID_KEY] = response.id.toString()
                }

                // Trigger navigation
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError("Login failed. Check your credentials.")
            }
        }
    }

    fun register(
        email: String,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                apiService.register(RegisterRequest(email, username, password))
                onSuccess() // Notify UI to navigate back to Login
            } catch (e: Exception) {
                e.printStackTrace()
                onError("Registration failed for some reason...")
            }
        }
    }

    fun fetchUsers() {
        viewModelScope.launch {
            try {
                val response = apiService.getUsers()
                _users.value = response.users
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getOrCreatePrivateRoom(otherUserId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Get the currently logged-in user's ID
                val currentId = _currentUserId.value ?: return@launch

                // Make the POST request
                val response = apiService.getPrivateRoom(
                    currentUserId = currentId,
                    otherUserId = otherUserId
                )

                // Pass the generated room ID back to the UI
                onSuccess(response.roomId.toString())
            } catch (e: Exception) {
                e.printStackTrace()
                // Optionally handle errors here (e.g., show a Toast)
            }
        }
    }

    // 1. In loadInitialMessages()
    fun loadInitialMessages(roomId: String) {
        viewModelScope.launch {
            try {
                val currentId = _currentUserId.value ?: return@launch
                val roomIdLong = roomId.toLongOrNull() ?: return@launch

                val response = apiService.getLastMessages(roomIdLong, 20)

                val history = response.messages.map { msg ->
                    ChatMessage(
                        id = msg.id,
                        text = msg.content,
                        isMine = msg.senderId.toString() == currentId,
                        // Safely parse the string to the Enum, defaulting to SENT if it fails
                        status = runCatching { MessageStatus.valueOf(msg.messageStatus) }
                            .getOrDefault(MessageStatus.SENT)
                    )
                }

                _messages.value = history
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMoreMessages(roomId: String) {
        // Don't fetch if already fetching or if there are no more older messages
        if (isLoadingMore || isLastPage) return

        viewModelScope.launch {
            isLoadingMore = true
            try {
                val currentId = _currentUserId.value ?: return@launch
                val roomIdLong = roomId.toLongOrNull() ?: return@launch

                // Since Index 0 is the newest, the last item in our list is the oldest.
                // We find the oldest message that actually has a database ID.
                val oldestMessage = _messages.value.lastOrNull { it.id != null }
                val msgId = oldestMessage?.id ?: return@launch

                // Fetch the next 50 messages BEFORE that ID
                val response = apiService.getBeforeMessages(roomIdLong, msgId, 10)

                if (response.messages.isEmpty()) {
                    isLastPage = true // No more messages to load
                    return@launch
                }

                val olderHistory = response.messages.map { msg ->
                    ChatMessage(
                        id = msg.id,
                        text = msg.content,
                        isMine = msg.senderId.toString() == currentId,
                        status = runCatching { MessageStatus.valueOf(msg.messageStatus) }
                            .getOrDefault(MessageStatus.SENT)
                    )
                }

                // Append the older messages to the END of our current list
                _messages.value = _messages.value + olderHistory

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun editMessage(messageId: Long, newContent: String) {
        viewModelScope.launch {
            try {
                val response = apiService.editMessage(EditMessageRequest(messageId, newContent))
                if (response.success) {
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == messageId) {
                            // Update text AND change status to EDITED
                            msg.copy(text = newContent, status = MessageStatus.EDITED)
                        } else msg
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteMessage(DeleteMessageRequest(messageId))
                if (response.success) {
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == messageId) {
                            // Keep the message in the list, but change status to DELETED
                            msg.copy(status = MessageStatus.DELETED)
                        } else msg
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}