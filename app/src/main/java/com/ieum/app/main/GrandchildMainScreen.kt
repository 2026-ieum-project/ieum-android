package com.ieum.app.main

import com.ieum.app.NavRoute
import com.ieum.app.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

private data class MemoryItem(
    val title: String,
    val date: String,
    val type: String,
)

@Composable
fun GrandchildMainScreen(navController: NavController, viewModel: GrandchildViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var codeCopied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val memories = remember {
        listOf(
            MemoryItem("바닷가 나들이", "3월 8일 · 사진", "photo"),
            MemoryItem("오늘의 이야기", "3월 12일 · 음성", "voice"),
            MemoryItem("텃밭 일기", "3월 10일 · 일기", "diary"),
        )
    }

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            GrandchildBottomBar(selectedTab) { selectedTab = it }
        }
    ) { padding ->
        if (state.groupId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("로딩 중...", color = Muted)
            }
        } else if (state.groupId!!.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                SectionCard {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "아직 가족 그룹에 참여하지 않았어요",
                            fontSize = 16.sp, fontWeight = FontWeight.W700, color = Ink,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { navController.navigate(NavRoute.JoinGroup.route) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Coral)
                        ) {
                            Text("초대 코드 입력하기", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else when (selectedTab) {
            1 -> {
                LaunchedEffect(Unit) {
                    navController.navigate(NavRoute.Chat.route)
                    selectedTab = 0
                }
            }
            2 -> Box(Modifier.padding(padding)) { SendPhotoTab() }
            3 -> Box(Modifier.padding(padding)) { MemoryTab() }
            4 -> Box(Modifier.padding(padding)) { ProfileTab(navController) }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 헤더
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("우리 가족", fontSize = 14.sp, fontWeight = FontWeight.W700, color = Muted)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${state.userName.ifEmpty { "사용자" }}님, 안녕하세요",
                                fontSize = 22.sp, fontWeight = FontWeight.W800, color = Ink
                            )
                        }
                        NotificationBell(
                            size = 44,
                            unreadCount = state.messageCount,
                            onClick = { navController.navigate(NavRoute.Notifications.route) }
                        )
                    }
                }

                // 2. 조부모 상태에 따라 다른 카드
                item {
                    when (state.hasGrandparent) {
                        null -> {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Coral, strokeWidth = 3.dp)
                            }
                        }
                        false -> {
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Coral.copy(alpha = 0.10f)),
                                shape = RoundedCornerShape(26.dp),
                                color = Surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralSoft)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(CoralSoft, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PersonAdd, null, tint = Coral, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "할머니/할아버지를 초대해주세요",
                                        fontSize = 18.sp, fontWeight = FontWeight.W800, color = Ink,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "아래 초대 코드를 할머니/할아버지에게 공유하면\n가족 그룹에 참여할 수 있어요",
                                        fontSize = 14.sp, fontWeight = FontWeight.W600, color = InkSub,
                                        textAlign = TextAlign.Center, lineHeight = 21.sp
                                    )
                                    Spacer(Modifier.height(20.dp))

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Paper,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("초대 코드", fontSize = 12.sp, fontWeight = FontWeight.W700, color = Muted)
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                state.inviteCode.ifEmpty { "------" },
                                                fontSize = 32.sp, fontWeight = FontWeight.W800,
                                                letterSpacing = 6.sp, color = Coral
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (state.inviteCode.isNotEmpty()) {
                                                clipboardManager.setText(AnnotatedString(state.inviteCode))
                                                codeCopied = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (codeCopied) Sage else Coral
                                        )
                                    ) {
                                        Icon(
                                            if (codeCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                            null, modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (codeCopied) "복사 완료!" else "초대 코드 복사하기",
                                            fontWeight = FontWeight.Bold, color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        true -> {
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Coral.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(26.dp),
                                color = Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF4E2D4))
                            ) {
                                Box(
                                    modifier = Modifier.background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFFF6EE), Color.White)
                                        )
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(22.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Avatar(state.grandparentName?.first()?.toString() ?: "할", 48)
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    "${state.grandparentName ?: "할머니"} 할머니",
                                                    fontSize = 16.sp, fontWeight = FontWeight.W800, color = Ink
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text("방금 음성 메시지를 보냈어요", fontSize = 13.sp, fontWeight = FontWeight.W700, color = Coral)
                                            }
                                        }
                                        Spacer(Modifier.height(18.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White, RoundedCornerShape(18.dp))
                                                .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                                                .padding(12.dp, 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .shadow(4.dp, CircleShape, spotColor = Coral.copy(alpha = 0.32f))
                                                    .background(Coral, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.PlayArrow, "재생", tint = Color.White, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(Modifier.width(14.dp))
                                            VoiceWaveform(
                                                modifier = Modifier.weight(1f).height(34.dp),
                                                playedRatio = 0.33f
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text("0:24", fontSize = 13.sp, fontWeight = FontWeight.W700, color = MutedSoft)
                                        }
                                        Spacer(Modifier.height(16.dp))

                                        Button(
                                            onClick = { navController.navigate(NavRoute.Chat.route) },
                                            modifier = Modifier.fillMaxWidth().height(54.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Coral),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp)
                                        ) {
                                            Text("답장하기", fontSize = 17.sp, fontWeight = FontWeight.W800, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. 사진 보내기 프롬프트
                if (state.hasGrandparent == true) {
                    item {
                        SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Image, null, tint = Sage, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "${state.grandparentName ?: "할머니"}께 옛날 사진 보내기",
                                        fontSize = 17.sp, fontWeight = FontWeight.W800, color = Ink
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "사진을 보내면 ${state.grandparentName ?: "할머니"}가 그때 이야기를 음성으로 들려줘요.",
                                    fontSize = 14.sp, fontWeight = FontWeight.W600, color = InkSub, lineHeight = 21.sp
                                )
                                Spacer(Modifier.height(16.dp))
                                SageOutlineButton(text = "사진 고르기", onClick = { /* TODO */ })
                            }
                        }
                    }
                }

                // 4. 우리 가족 추억
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("우리 가족 추억", fontSize = 17.sp, fontWeight = FontWeight.W800, color = Ink)
                            Text("모두 보기 ›", fontSize = 13.sp, fontWeight = FontWeight.W700, color = Muted)
                        }
                        Spacer(Modifier.height(14.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(memories) { memory ->
                                MemoryCard(memory)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceWaveform(
    modifier: Modifier = Modifier,
    playedRatio: Float = 0f
) {
    val heights = listOf(0.30f, 0.55f, 0.80f, 1.0f, 0.65f, 0.40f, 0.70f, 0.90f, 0.50f, 0.75f, 0.35f, 0.60f, 0.85f, 0.45f, 0.55f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, h ->
            val played = index.toFloat() / heights.size < playedRatio
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(h)
                    .background(
                        if (played) Coral else Color(0xFFE9C6B4),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun MemoryCard(memory: MemoryItem) {
    Column(modifier = Modifier.width(130.dp)) {
        when (memory.type) {
            "photo" -> {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFE3CFB4), Color(0xFFC2A786)))
                        )
                )
            }
            "voice" -> {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CoralSoft)
                        .border(1.dp, Color(0xFFF4E2D4), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Coral, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("할머니 음성", fontSize = 12.sp, fontWeight = FontWeight.W700, color = CoralDark)
                    }
                }
            }
            "diary" -> {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFDDE8DE), Color(0xFFBBD0BD)))
                        )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(memory.title, fontSize = 13.sp, fontWeight = FontWeight.W700, color = Ink)
        Text(memory.date, fontSize = 11.sp, fontWeight = FontWeight.W600, color = MutedSoft)
    }
}

@Composable
private fun GrandchildBottomBar(selectedTab: Int, onSelect: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.border(width = 1.dp, color = Line)
    ) {
        val items = listOf(
            "홈" to Icons.Outlined.Home,
            "가족 대화" to Icons.Outlined.ChatBubbleOutline,
            "사진 보내기" to Icons.Outlined.Image,
            "추억" to Icons.Outlined.Schedule,
            "나" to Icons.Outlined.Person
        )
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onSelect(index) },
                icon = { Icon(icon, label, modifier = Modifier.size(24.dp)) },
                label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.W800 else FontWeight.W700) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Coral,
                    selectedTextColor = Coral,
                    unselectedIconColor = MutedSoft,
                    unselectedTextColor = MutedSoft,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
