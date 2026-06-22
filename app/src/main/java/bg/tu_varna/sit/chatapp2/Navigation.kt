package bg.tu_varna.sit.chatapp2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
//import bg.tu_varna.sit.chatapp2.screen.ChatRoomScreen
//import bg.tu_varna.sit.chatapp2.screen.LoginScreen
//import bg.tu_varna.sit.chatapp2.screen.RegisterScreen
//import bg.tu_varna.sit.chatapp2.screen.UserListScreen
import bg.tu_varna.sit.chatapp2.view.ChatRoomScreen
import bg.tu_varna.sit.chatapp2.view.ChatViewModel
import bg.tu_varna.sit.chatapp2.view.LoginScreen
import bg.tu_varna.sit.chatapp2.view.RegisterScreen
import bg.tu_varna.sit.chatapp2.view.UserListScreen

@Composable
fun ChatApp() {

    val navController = rememberNavController()
    val viewModel: ChatViewModel = viewModel()
    val currentUserId by viewModel.currentUserId.collectAsState()

    NavHost(navController = navController, startDestination = "login") {

//        composable("login") {
//            var errorMessage by remember { mutableStateOf<String?>(null) }

            // Auto-navigate if already logged in
//            LaunchedEffect(currentUserId) {
//                if (currentUserId != null) {
//                    navController.navigate("user_list") {
//                        // Remove login screen from backstack so the user can't press "back" to it
//                        popUpTo("login") { inclusive = true }
//                    }
//                }
//            }

            //login without register
//            LoginScreen(
//                errorMessage = errorMessage,
//                onLoginClick = { username, password ->
//                    errorMessage = null
//                    viewModel.login(
//                        username = username,
//                        password = password,
//                        onSuccess = {
//                            // Navigation is handled automatically by the LaunchedEffect above
//                            // when currentUserId updates in the DataStore
//                            navController.navigate("user_list") {
//                                popUpTo("login") { inclusive = true }
//                            }
//                        },
//                        onError = { error ->
//                            errorMessage = error
//                        }
//                    )
//                }
//            )
//        }

            composable("login") {
                var errorMessage by remember { mutableStateOf<String?>(null) }

                LoginScreen(
                    errorMessage = errorMessage,
                    onLoginClick = { username, password ->
                        errorMessage = null
                        viewModel.login(
                            username = username,
                            password = password,
                            onSuccess = {
                                navController.navigate("user_list") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onError = { error -> errorMessage = error }
                        )
                    },
                    onRegisterNavigate = {
                        navController.navigate("register")
                    }
                )
            }

        composable("register") {
            var errorMessage by remember { mutableStateOf<String?>(null) }

            RegisterScreen(
                errorMessage = errorMessage,
                onRegisterClick = { email, username, password ->
                    errorMessage = null
                    viewModel.register(
                        email = email,
                        username = username,
                        password = password,
                        onSuccess = {
                            navController.popBackStack()
                        },
                        onError = { error -> errorMessage = error }
                    )
                },
                onBackToLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("user_list") {
            val users by viewModel.users.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.fetchUsers()
            }

            UserListScreen(
                users = users,
                onUserClick = { selectedUser ->
                    viewModel.getOrCreatePrivateRoom(otherUserId = selectedUser.id) { roomId ->
                        navController.navigate("chat_room/$roomId/${selectedUser.name}")
                    }
                }
            )
        }

        composable(
            route = "chat_room/{roomId}/{userName}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val userName = backStackEntry.arguments?.getString("userName") ?: "Chat"

            LaunchedEffect(roomId) {
                viewModel.connectToRoom(roomId)
            }

            val messages by viewModel.messages.collectAsState()

            ChatRoomScreen(
                userName = userName,
                messages = messages,
                onBackClick = { navController.popBackStack() },
                onSendMessage = { text -> viewModel.sendMessage(text) },
                onEditMessage = { id, newText -> viewModel.editMessage(id, newText) },
                onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                onLoadMore = { viewModel.loadMoreMessages(roomId) }
            )

        }
    }
}