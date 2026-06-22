package com.ieum.app.auth

import com.ieum.app.NavRoute
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            navController.navigate(NavRoute.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navigateAfterAuth(user.uid, navController)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("로딩 중...")
    }
}

fun navigateAfterAuth(uid: String, navController: NavController) {
    FirebaseDatabase.getInstance().reference
        .child("users").child(uid).child("role")
        .get()
        .addOnSuccessListener { snapshot ->
            val role = snapshot.getValue(String::class.java) ?: ""
            val destination = when (role) {
                "grandparent" -> NavRoute.GrandparentMain.route
                "child" -> NavRoute.ChildMain.route
                "grandchild" -> NavRoute.GrandchildMain.route
                else -> NavRoute.Login.route
            }
            navController.navigate(destination) {
                popUpTo(0) { inclusive = true }
            }
        }
        .addOnFailureListener {
            navController.navigate(NavRoute.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
}
