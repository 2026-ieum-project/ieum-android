package com.ieum.app.group

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun CreateGroupScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var inviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🏠", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "가족 그룹 만들기",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))

            if (inviteCode.isEmpty()) {
                Text(
                    "그룹을 만들면 고유한 초대 코드가 발급됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = ""
                        createGroup(uid) { code, error ->
                            isLoading = false
                            if (code != null) inviteCode = code
                            else errorMessage = error ?: "그룹 생성 실패"
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        if (isLoading) "생성 중..." else "그룹 만들기",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                Text(
                    "가족 그룹이 만들어졌어요!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "초대 코드",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            inviteCode,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 8.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "이 코드를 조부모님과 손자녀에게 공유해 주세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = {
                        navController.navigate(NavRoute.Chat.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("💬  채팅 시작하기", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun createGroup(uid: String, onResult: (code: String?, error: String?) -> Unit) {
    val code = generateInviteCode()
    val groupsRef = FirebaseDatabase.getInstance().reference.child("groups")
    val newGroupRef = groupsRef.push()
    val groupId = newGroupRef.key ?: run { onResult(null, "그룹 ID 생성 실패"); return }

    val groupData = mapOf(
        "inviteCode" to code,
        "createdBy" to uid,
        "members" to mapOf(uid to "child")
    )

    newGroupRef.setValue(groupData)
        .addOnSuccessListener {
            val db = FirebaseDatabase.getInstance().reference
            db.child("inviteCodes").child(code).setValue(groupId)
                .addOnSuccessListener {
                    db.child("users").child(uid).child("groupId")
                        .setValue(groupId)
                        .addOnSuccessListener { onResult(code, null) }
                        .addOnFailureListener { e -> onResult(null, e.message) }
                }
                .addOnFailureListener { e -> onResult(null, e.message) }
        }
        .addOnFailureListener { e -> onResult(null, e.message) }
}

private fun generateInviteCode(): String {
    val chars = ('A'..'Z') + ('0'..'9')
    return (1..6).map { chars.random() }.joinToString("")
}
