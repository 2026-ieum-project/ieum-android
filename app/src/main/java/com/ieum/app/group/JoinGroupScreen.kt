package com.ieum.app.group

import com.ieum.app.NavRoute
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "그룹 참여",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("💌", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "초대 코드 입력",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "자녀에게 받은 6자리 초대 코드를 입력해 주세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase().take(6) },
                label = { Text("초대 코드") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(24.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    if (isLoading) "참여 중..." else "참여하기",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
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
            if (!groupId.isNullOrEmpty()) {
                joinGroupById(db, uid, role, groupId, onResult)
            } else {
                // inviteCodes에 없으면 groups에서 직접 검색 (구버전 그룹 호환)
                db.child("groups").orderByChild("inviteCode").equalTo(code).get()
                    .addOnSuccessListener { groupSnapshot ->
                        if (!groupSnapshot.exists()) {
                            onResult(false, "유효하지 않은 초대 코드입니다")
                            return@addOnSuccessListener
                        }
                        val groupEntry = groupSnapshot.children.first()
                        val foundGroupId = groupEntry.key ?: run {
                            onResult(false, "그룹 정보를 읽을 수 없습니다")
                            return@addOnSuccessListener
                        }
                        // 이후 조회를 위해 inviteCodes에도 저장
                        db.child("inviteCodes").child(code).setValue(foundGroupId)
                        joinGroupById(db, uid, role, foundGroupId, onResult)
                    }
                    .addOnFailureListener { e -> onResult(false, e.message) }
            }
        }
        .addOnFailureListener { e -> onResult(false, e.message) }
}

private fun joinGroupById(
    db: com.google.firebase.database.DatabaseReference,
    uid: String,
    role: String,
    groupId: String,
    onResult: (success: Boolean, error: String?) -> Unit
) {
    db.child("groups").child(groupId).child("members").child(uid).setValue(role)
        .addOnSuccessListener {
            db.child("users").child(uid).child("groupId").setValue(groupId)
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { e -> onResult(false, e.message) }
        }
        .addOnFailureListener { e -> onResult(false, e.message) }
}
