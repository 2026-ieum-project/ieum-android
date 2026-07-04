package com.ieum.app.main

import com.ieum.app.NavRoute
import com.ieum.app.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GrandparentMainScreen(navController: NavController, viewModel: GrandparentViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var codeCopied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val todayDate = remember {
        val sdf = SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN)
        sdf.format(Date()) + "이에요"
    }

    val storyPrompt = remember(state.userGender, state.grandchildName, state.childName) {
        getStoryPrompt(state.userGender, state.grandchildName, state.childName)
    }

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            ElderBottomBar(selectedTab) { selectedTab = it }
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
                            fontSize = 19.sp, fontWeight = FontWeight.W800, color = Ink,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        CoralFilledButton(
                            text = "초대 코드 입력하기",
                            onClick = { navController.navigate(NavRoute.JoinGroup.route) },
                            height = 64.dp
                        )
                    }
                }
            }
        } else when (selectedTab) {
            1 -> {
                LaunchedEffect(Unit) {
                    navController.navigate(NavRoute.Diary.path(autoRecord = true))
                    selectedTab = 0
                }
            }
            2 -> {
                LaunchedEffect(Unit) {
                    navController.navigate(NavRoute.Chat.route)
                    selectedTab = 0
                }
            }
            3 -> Box(Modifier.padding(padding)) { PhotoTab() }
            4 -> Box(Modifier.padding(padding)) { ProfileTab(navController) }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 헤더
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(24.dp))
                                    .border(1.dp, Line, RoundedCornerShape(24.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("우리 가족", fontSize = 16.sp, fontWeight = FontWeight.W700, color = InkSub)
                            }
                            NotificationBell(
                                size = 52,
                                unreadCount = state.messageCount,
                                onClick = { navController.navigate(NavRoute.Notifications.route) }
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "${state.userName.ifEmpty { "할머니" }},",
                            fontSize = 30.sp, fontWeight = FontWeight.W800, color = Ink
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(todayDate, fontSize = 21.sp, fontWeight = FontWeight.W600, color = InkSub)
                    }
                }

                // 2. 가족 멤버 상태에 따라 다른 카드
                item {
                    when (state.hasFamilyMembers) {
                        null -> {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 18.dp)
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
                                    .padding(horizontal = 18.dp)
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
                                        "가족을 초대해주세요",
                                        fontSize = 18.sp, fontWeight = FontWeight.W800, color = Ink,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "아래 초대 코드를 자녀나 손주에게 공유하면\n가족 그룹에 참여할 수 있어요",
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
                            SectionCard(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                shadowColor = Coral.copy(alpha = 0.10f)
                            ) {
                                Column(modifier = Modifier.padding(26.dp, 26.dp, 26.dp, 24.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(11.dp).background(Coral, CircleShape)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("오늘의 이야기", fontSize = 17.sp, fontWeight = FontWeight.W800, color = Coral)
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        storyPrompt,
                                        fontSize = 26.sp, fontWeight = FontWeight.W800, color = Ink,
                                        lineHeight = 36.sp
                                    )
                                    Spacer(Modifier.height(22.dp))
                                    CoralFilledButton(
                                        text = "눌러서 말하기",
                                        onClick = { navController.navigate(NavRoute.Chat.route) },
                                        height = 76.dp,
                                        icon = Icons.Default.Mic,
                                        iconSize = 30
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        if (state.grandchildName != null) "${state.grandchildName}이(가) 기다리고 있어요"
                                        else if (state.childName != null) "${state.childName}이(가) 기다리고 있어요"
                                        else "가족이 기다리고 있어요",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontSize = 17.sp, fontWeight = FontWeight.W600, color = MutedSoft
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 가족이 보낸 사진
                if (state.hasFamilyMembers == true) {
                    item {
                        SectionCard(modifier = Modifier.padding(horizontal = 18.dp)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("가족이 사진을 보냈어요", fontSize = 19.sp, fontWeight = FontWeight.W800, color = Ink)
                                Spacer(Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFEAD9C6), Color(0xFFD8BFA6))
                                            )
                                        ),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Text(
                                        if (state.grandchildName != null) "${state.grandchildName}이(가) 보냈어요"
                                        else if (state.childName != null) "${state.childName}이(가) 보냈어요"
                                        else "가족이 보냈어요",
                                        modifier = Modifier.padding(14.dp, 12.dp),
                                        fontSize = 15.sp, fontWeight = FontWeight.W700, color = Color.White
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                CoralOutlineButton(
                                    text = "답장하기",
                                    onClick = { navController.navigate(NavRoute.Chat.route) },
                                    height = 64.dp
                                )
                            }
                        }
                    }
                }

                // 4. 기능 타일 2열
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            onClick = { navController.navigate(NavRoute.Chat.route) },
                            modifier = Modifier
                                .weight(1f)
                                .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.06f)),
                            shape = RoundedCornerShape(26.dp),
                            color = Surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(22.dp, 22.dp, 18.dp, 22.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(CoralSoft, RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.ChatBubbleOutline, null,
                                        tint = Coral, modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Text("가족 대화", fontSize = 20.sp, fontWeight = FontWeight.W800, color = Ink)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (state.messageCount > 0) "새 메시지 ${state.messageCount}개" else "대화하기",
                                    fontSize = 16.sp, fontWeight = FontWeight.W700, color = Coral
                                )
                            }
                        }
                        Surface(
                            onClick = { navController.navigate(NavRoute.Diary.path()) },
                            modifier = Modifier
                                .weight(1f)
                                .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.06f)),
                            shape = RoundedCornerShape(26.dp),
                            color = Surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(22.dp, 22.dp, 18.dp, 22.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(SageSoft, RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Videocam, null,
                                        tint = Sage, modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Text("영상 일기", fontSize = 20.sp, fontWeight = FontWeight.W800, color = Ink)
                                Spacer(Modifier.height(3.dp))
                                Text("오늘 기록", fontSize = 16.sp, fontWeight = FontWeight.W700, color = Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 공용 컴포저블 ──

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    shadowColor: Color = Color.Black.copy(alpha = 0.06f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = shadowColor),
        shape = RoundedCornerShape(26.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        content()
    }
}

@Composable
fun CoralFilledButton(
    text: String,
    onClick: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 54.dp,
    icon: ImageVector? = null,
    iconSize: Int = 24
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Coral),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(iconSize.dp), tint = Color.White)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, fontSize = 23.sp, fontWeight = FontWeight.W800, color = Color.White)
    }
}

@Composable
fun CoralOutlineButton(
    text: String,
    onClick: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 54.dp
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CoralSoft,
            contentColor = CoralDark
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, Coral)
    ) {
        Text(text, fontSize = 21.sp, fontWeight = FontWeight.W800)
    }
}

@Composable
fun SageOutlineButton(
    text: String,
    onClick: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 50.dp
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SageSoft2,
            contentColor = SageDark
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, Sage)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.W800)
    }
}

@Composable
fun NotificationBell(
    size: Int = 44,
    unreadCount: Int = 0,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White, CircleShape)
            .border(1.dp, Line, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Notifications, "알림",
            tint = InkSub, modifier = Modifier.size((size * 0.5).dp)
        )
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(17.dp)
                    .background(Coral, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.W800,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun Avatar(initial: String, size: Int = 48) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFFF0C9A0), Color(0xFFE0A878))),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, fontSize = (size * 0.38).sp, fontWeight = FontWeight.W800, color = Color.White)
    }
}

@Composable
private fun ElderBottomBar(selectedTab: Int, onSelect: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.border(width = 1.dp, color = Line)
    ) {
        val items = listOf(
            "홈" to Icons.Outlined.Home,
            "영상일기" to Icons.Default.Videocam,
            "가족 대화" to Icons.Outlined.ChatBubbleOutline,
            "사진" to Icons.Outlined.Image,
            "나" to Icons.Outlined.Person
        )
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onSelect(index) },
                icon = { Icon(icon, label, modifier = Modifier.size(29.dp)) },
                label = { Text(label, fontSize = 14.sp, fontWeight = if (selectedTab == index) FontWeight.W800 else FontWeight.W700) },
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

// ── 시간·성별·맥락 기반 이야기 멘트 ──

private fun getStoryPrompt(
    gender: String,
    grandchildName: String?,
    childName: String?
): String {
    val cal = java.util.Calendar.getInstance()
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val dayOfYear = cal.get(java.util.Calendar.DAY_OF_YEAR)

    val gcName = grandchildName ?: "손주"
    val cName = childName ?: "자녀"
    val isFemale = gender == "female"

    val morningPrompts = listOf(
        "오늘 아침은\n뭘 드셨어요?",
        "어젯밤에\n잘 주무셨어요?",
        "오늘 기분이\n어떠세요?",
        "아침에 일어나서\n제일 먼저 뭐 하셨어요?",
        "오늘 날씨가\n어때 보여요?",
        "요즘 아침마다\n드시는 게 있으세요?",
        "오늘 아침 산책은\n하셨어요?",
        if (isFemale) "오늘 아침에\n${gcName}이(가) 꿈에 나왔어요?"
        else "오늘 아침에\n운동 좀 하셨어요?",
        "${gcName}이(가) 안부 궁금해 해요.\n오늘 컨디션 어떠세요?",
        if (isFemale) "오늘은 어떤\n반찬을 하셨어요?"
        else "오늘 아침 신문에서\n재밌는 거 있었어요?"
    )

    val lunchPrompts = listOf(
        "오늘 점심은\n무엇을 드셨어요?",
        "점심 맛있게\n드셨어요?",
        "오늘 뭐 하고\n오셨어요?",
        "요즘 제일 맛있게\n드시는 게 뭐예요?",
        "오늘 누구 만나셨어요?\n재밌는 일 있었어요?",
        "${gcName}이(가) ${if (isFemale) "할머니" else "할아버지"} 점심 궁금해 해요.\n뭐 드셨어요?",
        if (isFemale) "오늘 시장에\n다녀오셨어요?"
        else "오늘 친구분들\n만나셨어요?",
        "요즘 좋아하시는\n음식이 있으세요?",
        "오전에 재미있는\n일 있었어요?",
        if (isFemale) "오늘은 뭘\n만들어 드셨어요?"
        else "오늘 텃밭이나 화분은\n잘 자라고 있어요?"
    )

    val afternoonPrompts = listOf(
        "오늘 오후에는\n뭐 하고 계세요?",
        "점심 먹고 낮잠은\n좀 주무셨어요?",
        "오후에 산책\n다녀오셨어요?",
        "요즘 재미있게 보시는\nTV 프로그램 있어요?",
        "오늘 날씨가 좋은데\n밖에 좀 나가셨어요?",
        "${cName}이(가) 안부 전해 달래요.\n뭐 하고 계세요?",
        if (isFemale) "오늘 동네 친구분\n만나셨어요?"
        else "오후에 바둑이나\n장기 두셨어요?",
        "요즘 취미로\n뭐 하고 계세요?",
        "오늘 뭐 재밌는 일\n있었어요?",
        if (isFemale) "오늘 손수건이나\n뜨개질 하셨어요?"
        else "오늘 뉴스에서\n재밌는 거 보셨어요?"
    )

    val eveningPrompts = listOf(
        "오늘 저녁은\n뭐 드실 거예요?",
        "오늘 하루\n어떠셨어요?",
        "저녁 맛있게\n드셨어요?",
        "오늘 하루 중에\n제일 좋았던 건 뭐예요?",
        "${gcName}이(가) 하루 이야기\n듣고 싶대요!",
        "오늘은 좀\n쉬셨어요?",
        if (isFemale) "저녁 반찬은\n뭐 하셨어요?"
        else "저녁에 막걸리 한 잔\n하셨어요?",
        "오늘 산책 중에\n본 거 있어요?",
        "내일은 뭐 하실\n계획이에요?",
        "요즘 건강은\n어떠세요?"
    )

    val nightPrompts = listOf(
        "오늘 하루는\n어떠셨어요?",
        "오늘 하루 중에\n기억나는 일 있어요?",
        "내일은 뭐\n하실 거예요?",
        "요즘 잠은\n잘 오세요?",
        "${gcName}이(가) 잘 자라고\n인사 전해 달래요!",
        "오늘 있었던\n재밌는 이야기 해주세요!",
        if (isFemale) "오늘 드라마\n보셨어요?"
        else "오늘 저녁 뉴스에서\n뭐 재밌는 거 나왔어요?",
        "이번 주에 가장\n좋았던 날은 언제예요?",
        "요즘 가족한테\n하고 싶은 말 있으세요?",
        "편안한 밤 되세요.\n오늘 이야기 들려주세요!"
    )

    val prompts = when {
        hour < 9 -> morningPrompts
        hour < 13 -> lunchPrompts
        hour < 18 -> afternoonPrompts
        hour < 21 -> eveningPrompts
        else -> nightPrompts
    }

    return prompts[dayOfYear % prompts.size]
}
