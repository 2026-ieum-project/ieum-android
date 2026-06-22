package com.ieum.app.main

import com.ieum.app.NavRoute
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun GrandchildMainScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var groupId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("groupId")
            .get()
            .addOnSuccessListener { snapshot ->
                groupId = snapshot.getValue(String::class.java) ?: ""
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("손자녀 메인", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        when {
            groupId == null -> {
                Text("로딩 중...", style = MaterialTheme.typography.bodyMedium)
            }
            groupId!!.isEmpty() -> {
                Text(
                    "아직 가족 그룹에 참여하지 않았습니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(NavRoute.JoinGroup.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("초대 코드 입력하기")
                }
            }
            else -> {
                Button(
                    onClick = { navController.navigate(NavRoute.Chat.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("채팅")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate(NavRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("로그아웃")
        }
    }
}
