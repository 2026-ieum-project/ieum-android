package com.ieum.app.auth

import com.ieum.app.NavRoute
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💌",
            fontSize = 64.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "이음",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "가족을 이어주는 공간",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
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
