package com.ieum.app.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ieum.app.NavRoute
import com.ieum.app.ui.theme.*

@Composable
fun PlaceholderTab(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Paper),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(CoralSoft, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Coral, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.W800, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.W600, color = MutedSoft, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StoryTab() {
    PlaceholderTab(
        icon = Icons.Outlined.Mic,
        title = "이야기",
        subtitle = "오늘의 이야기를 들려주세요"
    )
}

@Composable
fun PhotoTab() {
    PlaceholderTab(
        icon = Icons.Outlined.Image,
        title = "사진",
        subtitle = "가족 사진을 확인하세요"
    )
}

@Composable
fun ReportTab() {
    PlaceholderTab(
        icon = Icons.Outlined.BarChart,
        title = "두뇌 활력 리포트",
        subtitle = "주간 활력 리포트를 확인하세요"
    )
}

@Composable
fun MemoryTab() {
    PlaceholderTab(
        icon = Icons.Outlined.Schedule,
        title = "추억",
        subtitle = "가족의 소중한 추억을 모아보세요"
    )
}

@Composable
fun ProfileTab(navController: NavController? = null, viewModel: ProfileViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            navController?.navigate(NavRoute.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val roleLabel = when (state.userRole) {
        "grandparent" -> "조부모"
        "child" -> "자녀"
        "grandchild" -> "손자녀"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            SectionCard {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(CoralSoft, CircleShape)
                            .border(2.dp, Coral, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            state.userName.firstOrNull()?.toString() ?: "?",
                            fontSize = 32.sp, fontWeight = FontWeight.W800, color = Coral
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        state.userName.ifEmpty { "사용자" },
                        fontSize = 22.sp, fontWeight = FontWeight.W800, color = Ink
                    )
                    if (roleLabel.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(roleLabel, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Muted)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            SectionCard {
                Surface(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Logout, null,
                            tint = Color(0xFFCC4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "로그아웃",
                            fontSize = 16.sp, fontWeight = FontWeight.W700,
                            color = Color(0xFFCC4444)
                        )
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃", fontWeight = FontWeight.W800) },
            text = { Text("정말 로그아웃 하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) {
                    Text("로그아웃", color = Color(0xFFCC4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", color = Muted)
                }
            }
        )
    }
}

@Composable
fun SendPhotoTab() {
    PlaceholderTab(
        icon = Icons.Outlined.Image,
        title = "사진 보내기",
        subtitle = "할머니께 사진을 보내보세요"
    )
}
