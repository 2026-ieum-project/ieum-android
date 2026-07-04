package com.ieum.app.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.VideoCameraFront
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ieum.app.NavRoute
import com.ieum.app.chat.Message
import com.ieum.app.ui.theme.Coral
import com.ieum.app.ui.theme.CoralSoft
import com.ieum.app.ui.theme.Ink
import com.ieum.app.ui.theme.InkSub
import com.ieum.app.ui.theme.Line
import com.ieum.app.ui.theme.Muted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("알림", fontWeight = FontWeight.W800, color = Ink) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        when {
            state.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Coral, strokeWidth = 3.dp)
                }
            }
            state.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.NotificationsNone, null,
                            tint = Muted, modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "아직 알림이 없어요",
                            fontSize = 16.sp, fontWeight = FontWeight.W700, color = Muted
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.items, key = { it.kind + it.id }) { item ->
                        NotificationRow(item = item) {
                            if (item.kind == NotificationViewModel.KIND_DIARY) {
                                navController.navigate(NavRoute.Diary.path())
                            } else {
                                navController.navigate(NavRoute.Chat.route)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (item.unread) CoralSoft else Color.White
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Line, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconOf(item.kind),
                    contentDescription = null,
                    tint = InkSub,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.senderName,
                    fontSize = 15.sp, fontWeight = FontWeight.W800, color = Ink
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.preview,
                    fontSize = 14.sp, fontWeight = FontWeight.W600, color = InkSub,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatNotificationTime(item.timestamp),
                    fontSize = 12.sp, color = Muted
                )
            }
            if (item.unread) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(9.dp).background(Coral, CircleShape))
            }
        }
    }
}

private fun iconOf(kind: String): ImageVector = when (kind) {
    Message.TYPE_VOICE -> Icons.Outlined.Mic
    Message.TYPE_IMAGE -> Icons.Outlined.Image
    Message.TYPE_VIDEO -> Icons.Outlined.Videocam
    NotificationViewModel.KIND_DIARY -> Icons.Outlined.VideoCameraFront
    else -> Icons.AutoMirrored.Outlined.Chat
}

private fun formatNotificationTime(timestamp: Long): String {
    return SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).apply {
        timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
    }.format(Date(timestamp))
}
