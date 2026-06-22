package com.ieum.app.group

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun CreateGroupScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var inviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("가족 그룹 만들기", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        if (inviteCode.isEmpty()) {
            Text(
                "그룹을 만들면 고유한 초대 코드가 발급됩니다.\n가족에게 코드를 공유해 주세요.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "생성 중..." else "그룹 만들기")
            }
        } else {
            Text("그룹이 생성됐습니다!", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            Text("초대 코드", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                inviteCode,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "이 코드를 조부모/손자녀에게 공유하세요.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    navController.navigate(NavRoute.Chat.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("채팅 시작")
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
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
