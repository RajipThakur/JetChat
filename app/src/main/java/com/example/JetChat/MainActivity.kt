package com.example.JetChat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.example.JetChat.Screens.ChatListScreen
import com.example.JetChat.Screens.LoginScreen
import com.example.JetChat.Screens.ProfileScreen
import com.example.JetChat.Screens.SignUpScreen
import com.example.JetChat.Screens.SingleChatScreen
import com.example.JetChat.Screens.SingleStatusScreen
import com.example.JetChat.Screens.StatusScreen
import com.example.JetChat.ui.theme.LiveChatTheme
import dagger.hilt.android.AndroidEntryPoint


sealed class DestinationScreen(var route:String){
    object SignUp:DestinationScreen("signup")
    object Login:DestinationScreen("login")
    object Profile:DestinationScreen("profile")
    object ChatList:DestinationScreen("chatlist")
    object SingleChat:DestinationScreen("singlechat/{chatId}"){
        fun createRoute(id:String)="singlechat/$id"
    }
    object StatusList:DestinationScreen("statusList")

    object SingleStatus:DestinationScreen("singlestatus/{UserId}"){
        fun createRoute(userId:String)="singlestatus/$userId"
    }

}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveChatTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatAppNavigation()
                }
            }
        }
    }

    @Composable
    fun ChatAppNavigation(){
        var navController= rememberNavController()

        var vm=hiltViewModel<LCViewModel>()

        NavHost(navController = navController, startDestination =DestinationScreen.SignUp.route ) {
            composable(DestinationScreen.SignUp.route){
                SignUpScreen(navController,vm)
            }

            composable(DestinationScreen.Login.route){
                LoginScreen(navController= navController,vm=vm)
            }

            composable(DestinationScreen.ChatList.route){
                ChatListScreen(navController = navController,vm=vm)
            }

            composable(DestinationScreen.SingleChat.route){
                val chatId=it.arguments?.getString("chatId")
                chatId?.let{
                    SingleChatScreen(navController=navController,vm=vm,chatId=chatId)
                }
            }


            composable(DestinationScreen.StatusList.route){
                StatusScreen(navController = navController,vm=vm)
            }


            composable(DestinationScreen.Profile.route){
                ProfileScreen(navController = navController,vm=vm)
            }

            composable(DestinationScreen.SingleStatus.route){
                val userId=it.arguments?.getString("userId")

                userId?.let{
                    SingleStatusScreen(navController=navController,vm=vm,userId=userId)
                }
            }

        }
    }
}

