package com.ieum.app

sealed class NavRoute(val route: String) {
    object Splash : NavRoute("splash")
    object Login : NavRoute("login")
    object Register : NavRoute("register")
    object GrandparentMain : NavRoute("grandparent_main")
    object ChildMain : NavRoute("child_main")
    object GrandchildMain : NavRoute("grandchild_main")
    object ForgotPassword : NavRoute("forgot_password")
    object CreateGroup : NavRoute("create_group")
    object JoinGroup : NavRoute("join_group")
    object Chat : NavRoute("chat")
}
