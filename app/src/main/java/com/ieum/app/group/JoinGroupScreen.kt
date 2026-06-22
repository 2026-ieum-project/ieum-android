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
import androidx.compose.material3.OutlinedTextField
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
fun JoinGroupScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var userRole by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                userRole = snapshot.getValue(String::class.java) ?: ""
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("그룹 참여", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "자녀에게 받은 초대 코드를 입력해 주세요.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = inviteCode,
            onValueChange = { inviteCode = it.uppercase().take(6) },
            label = { Text("초대 코드") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                isLoading = true
                errorMessage = ""
                joinGroup(uid, userRole, inviteCode.trim()) { success, error ->
                    isLoading = false
                    if (success) {
                        navController.navigate(NavRoute.Chat.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        errorMessage = error ?: "참여 실패"
                    }
                }
            },
            enabled = !isLoading && inviteCode.length == 6 && userRole.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "참여 중..." else "참여하기")
        }
    }
}

private fun joinGroup(
    uid: String,
    role: String,
    code: String,
    onResult: (success: Boolean, error: String?) -> Unit
) {
    val db = FirebaseDatabase.getInstance().reference
    db.child("inviteCodes").child(code).get()
        .addOnSuccessListener { snapshot ->
            val groupId = snapshot.getValue(String::class.java)
            if (groupId.isNullOrEmpty()) {
                onResult(false, "유효하지 않은 초대 코드입니다")
                return@addOnSuccessListener
            }
            db.child("groups").child(groupId).child("members").child(uid).setValue(role)
                .addOnSuccessListener {
                    db.child("users").child(uid).child("groupId").setValue(groupId)
                        .addOnSuccessListener { onResult(true, null) }
                        .addOnFailureListener { e -> onResult(false, e.message) }
                }
                .addOnFailureListener { e -> onResult(false, e.message) }
        }
        .addOnFailureListener { e -> onResult(false, e.message) }
}
