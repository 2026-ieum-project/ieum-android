package com.ieum.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ieum.app.auth.ForgotPasswordScreen
import com.ieum.app.ui.theme.IeumTheme
import com.ieum.app.auth.LoginScreen
import com.ieum.app.auth.RegisterScreen
import com.ieum.app.auth.SplashScreen
import com.ieum.app.chat.ChatScreen
import com.ieum.app.group.CreateGroupScreen
import com.ieum.app.group.JoinGroupScreen
import com.ieum.app.main.ChildMainScreen
import com.ieum.app.main.GrandchildMainScreen
import com.ieum.app.main.GrandparentMainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IeumTheme {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = NavRoute.Splash.route
            ) {
                composable(NavRoute.Splash.route) { SplashScreen(navController) }
                composable(NavRoute.Login.route) { LoginScreen(navController) }
                composable(NavRoute.Register.route) { RegisterScreen(navController) }
                composable(NavRoute.GrandparentMain.route) { GrandparentMainScreen(navController) }
                composable(NavRoute.ChildMain.route) { ChildMainScreen(navController) }
                composable(NavRoute.GrandchildMain.route) { GrandchildMainScreen(navController) }
                composable(NavRoute.ForgotPassword.route) { ForgotPasswordScreen(navController) }
                composable(NavRoute.CreateGroup.route) { CreateGroupScreen(navController) }
                composable(NavRoute.JoinGroup.route) { JoinGroupScreen(navController) }
                composable(NavRoute.Chat.route) { ChatScreen(navController) }
            }
            }
        }
    }
}
