package com.ieum.app.main

import com.ieum.app.NavRoute
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun GrandchildMainScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var groupId by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().reference.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                groupId = snapshot.child("groupId").getValue(String::class.java) ?: ""
                userName = snapshot.child("name").getValue(String::class.java) ?: ""
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🧒", fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                if (userName.isNotEmpty()) "${userName}님, 안녕하세요!" else "안녕하세요!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "가족과 이야기 나눠보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(40.dp))

            when {
                groupId == null -> Text("로딩 중...", color = MaterialTheme.colorScheme.outline)
                groupId!!.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💌", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "아직 가족 그룹에 참여하지 않았어요",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { navController.navigate(NavRoute.JoinGroup.route) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("초대 코드 입력하기", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = { navController.navigate(NavRoute.Chat.route) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("💬  가족 채팅", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate(NavRoute.Login.route) { popUpTo(0) { inclusive = true } }
            },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.outline)
        ) {
            Text("로그아웃")
        }
    }
}
