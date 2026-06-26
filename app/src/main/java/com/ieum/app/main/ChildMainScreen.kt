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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun ChildMainScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var groupId by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    // 조부모 정보
    var grandparentName by remember { mutableStateOf<String?>(null) }
    var hasGrandparent by remember { mutableStateOf<Boolean?>(null) }
    var inviteCode by remember { mutableStateOf("") }
    var codeCopied by remember { mutableStateOf(false) }

    // 메시지 수
    var messageCount by remember { mutableIntStateOf(0) }

    // 더미 상태
    val vitalityScore = 82
    val vitalityDelta = 3
    val isAlert = false
    val weeklyBars = listOf(0.60f, 0.48f, 0.72f, 0.64f, 0.80f, 0.76f, 1f)

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                groupId = snapshot.child("groupId").getValue(String::class.java) ?: ""
                userName = snapshot.child("name").getValue(String::class.java) ?: ""
            }
    }

    // 그룹이 로드되면 조부모 멤버 확인
    LaunchedEffect(groupId) {
        val gid = groupId
        if (gid.isNullOrEmpty()) return@LaunchedEffect

        val db = FirebaseDatabase.getInstance().reference

        // 초대 코드 가져오기
        db.child("groups").child(gid).child("inviteCode").get()
            .addOnSuccessListener { snap ->
                inviteCode = snap.getValue(String::class.java) ?: ""
            }

        // 멤버에서 조부모 찾기
        db.child("groups").child(gid).child("members").get()
            .addOnSuccessListener { membersSnap ->
                var foundGrandparentUid: String? = null
                for (child in membersSnap.children) {
                    val role = child.getValue(String::class.java)
                    if (role == "grandparent") {
                        foundGrandparentUid = child.key
                        break
                    }
                }

                if (foundGrandparentUid != null) {
                    // 조부모 이름 가져오기
                    db.child("users").child(foundGrandparentUid).child("name").get()
                        .addOnSuccessListener { nameSnap ->
                            grandparentName = nameSnap.getValue(String::class.java) ?: "조부모"
                            hasGrandparent = true
                        }
                        .addOnFailureListener {
                            grandparentName = "조부모"
                            hasGrandparent = true
                        }
                } else {
                    hasGrandparent = false
                }
            }
            .addOnFailureListener {
                hasGrandparent = false
            }
    }

    // 메시지 수 실시간 리스너
    DisposableEffect(groupId) {
        val gid = groupId
        if (gid.isNullOrEmpty()) return@DisposableEffect onDispose {}

        val ref = FirebaseDatabase.getInstance().reference.child("messages").child(gid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageCount = snapshot.childrenCount.toInt()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            ChildBottomBar(selectedTab) { selectedTab = it }
        }
    ) { padding ->
        if (groupId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("로딩 중...", color = Muted)
            }
        } else if (groupId!!.isEmpty()) {
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
                            "아직 가족 그룹이 없어요\n그룹을 만들어 가족을 초대해보세요",
                            fontSize = 16.sp, fontWeight = FontWeight.W700, color = Ink,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { navController.navigate(NavRoute.CreateGroup.route) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Coral)
                        ) {
                            Text("가족 그룹 만들기", fontWeight = FontWeight.Bold, color = Color.White)
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
            2 -> Box(Modifier.padding(padding)) { ReportTab() }
            3 -> Box(Modifier.padding(padding)) { MemoryTab() }
            4 -> Box(Modifier.padding(padding)) { ProfileTab(navController) }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 20.dp),
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
                                "안녕하세요, ${userName.ifEmpty { "사용자" }}님",
                                fontSize = 22.sp, fontWeight = FontWeight.W800, color = Ink
                            )
                        }
                        NotificationBell(size = 44)
                    }
                }

                // 2. 조부모 상태에 따라 다른 카드 표시
                item {
                    when (hasGrandparent) {
                        null -> {
                            // 로딩 중
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Sage, strokeWidth = 3.dp)
                            }
                        }
                        true -> {
                            // 조부모가 있는 경우: 두뇌 활력 리포트
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Sage.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(26.dp),
                                color = Surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SageSoft)
                            ) {
                                Column(modifier = Modifier.padding(22.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(9.dp).background(Sage, CircleShape))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "${grandparentName ?: "조부모"} 어머니 · 두뇌 활력",
                                                fontSize = 15.sp, fontWeight = FontWeight.W800, color = Ink
                                            )
                                        }
                                        Text("이번 주 ›", fontSize = 13.sp, fontWeight = FontWeight.W700, color = Muted)
                                    }
                                    Spacer(Modifier.height(18.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.Bottom) {
                                                Text("$vitalityScore", fontSize = 48.sp, fontWeight = FontWeight.W800, color = Sage, lineHeight = 48.sp)
                                                Spacer(Modifier.width(4.dp))
                                                Text("점", fontSize = 17.sp, fontWeight = FontWeight.W700, color = Muted,
                                                    modifier = Modifier.padding(bottom = 6.dp))
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.KeyboardArrowUp, null, tint = Sage, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("지난주보다 +$vitalityDelta", fontSize = 14.sp, fontWeight = FontWeight.W800, color = Sage)
                                            }
                                        }
                                        MiniBarChart(
                                            bars = weeklyBars,
                                            modifier = Modifier.weight(1f).height(64.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(18.dp))

                                    if (!isAlert) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(SageSoft, RoundedCornerShape(16.dp))
                                                .padding(13.dp, 13.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Check, null, tint = Sage, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Text("이번 주는 평소처럼 안정적이에요", fontSize = 15.sp, fontWeight = FontWeight.W700, color = SageDark)
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(AlertBg, RoundedCornerShape(16.dp))
                                                .border(1.dp, AlertBorder, RoundedCornerShape(16.dp))
                                                .padding(14.dp, 14.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, null, tint = Honey, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(9.dp))
                                                Text("조용한 알림", fontSize = 14.sp, fontWeight = FontWeight.W800, color = AlertText)
                                            }
                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                "최근 소통 패턴에서 평소와 다른 변화가 이어지고 있어요. 필요하시면 상담 안내를 확인해보세요.",
                                                fontSize = 14.sp, fontWeight = FontWeight.W600, color = InkSub, lineHeight = 21.sp
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            Button(
                                                onClick = { /* TODO */ },
                                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                                shape = RoundedCornerShape(13.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Honey)
                                            ) {
                                                Text("치매안심센터 안내 보기", fontSize = 15.sp, fontWeight = FontWeight.W800, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        false -> {
                            // 조부모가 없는 경우: 초대 카드
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
                                        "조부모님을 초대해주세요",
                                        fontSize = 18.sp, fontWeight = FontWeight.W800, color = Ink,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "아래 초대 코드를 조부모님에게 공유하면\n가족 그룹에 참여할 수 있어요",
                                        fontSize = 14.sp, fontWeight = FontWeight.W600, color = InkSub,
                                        textAlign = TextAlign.Center, lineHeight = 21.sp
                                    )
                                    Spacer(Modifier.height(20.dp))

                                    // 초대 코드 표시
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
                                                inviteCode.ifEmpty { "------" },
                                                fontSize = 32.sp, fontWeight = FontWeight.W800,
                                                letterSpacing = 6.sp, color = Coral
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))

                                    // 복사 버튼
                                    Button(
                                        onClick = {
                                            if (inviteCode.isNotEmpty()) {
                                                clipboardManager.setText(AnnotatedString(inviteCode))
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
                    }
                }

                // 3. 오늘의 활동 (조부모 있을 때만 표시)
                if (hasGrandparent == true) {
                    item {
                        SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(modifier = Modifier.padding(18.dp, 18.dp, 20.dp, 18.dp)) {
                                Text("오늘의 활동", fontSize = 14.sp, fontWeight = FontWeight.W800, color = Ink)
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ActivityItem("음성", Icons.Default.Mic, active = true, Modifier.weight(1f))
                                    ActivityItem("사진 회상", Icons.Default.Image, active = true, Modifier.weight(1f))
                                    ActivityItem("대화", Icons.Outlined.ChatBubbleOutline, active = false, Modifier.weight(1f))
                                    ActivityItem("일기", Icons.Default.Edit, active = true, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // 4. 가족 대화 미리보기
                item {
                    Surface(
                        onClick = { navController.navigate(NavRoute.Chat.route) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.06f)),
                        shape = RoundedCornerShape(26.dp),
                        color = Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(
                                if (hasGrandparent == true) (grandparentName?.first()?.toString() ?: "조") else userName.firstOrNull()?.toString() ?: "?",
                                48
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("가족 대화", fontSize = 16.sp, fontWeight = FontWeight.W800, color = Ink)
                                    if (messageCount > 0) {
                                        Text("${messageCount}개의 메시지", fontSize = 12.sp, fontWeight = FontWeight.W600, color = MutedSoft)
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (messageCount > 0) "대화방에 메시지가 있어요"
                                    else "가족 대화를 시작해보세요",
                                    fontSize = 14.sp, fontWeight = FontWeight.W600, color = InkSub,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (messageCount > 0) {
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Coral, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (messageCount > 99) "99+" else "$messageCount",
                                        fontSize = 11.sp, fontWeight = FontWeight.W800, color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. 소통 기능 2열
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionCard(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(CoralSoft, RoundedCornerShape(13.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Mic, null, tint = Coral, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("오늘의 이야기", fontSize = 15.sp, fontWeight = FontWeight.W800, color = Ink)
                                Spacer(Modifier.height(2.dp))
                                Text("음성 답변 도착", fontSize = 13.sp, fontWeight = FontWeight.W700, color = Coral)
                            }
                        }
                        SectionCard(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(SageSoft, RoundedCornerShape(13.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Image, null, tint = Sage, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("함께 보는 사진", fontSize = 15.sp, fontWeight = FontWeight.W800, color = Ink)
                                Spacer(Modifier.height(2.dp))
                                Text("사진 보내기", fontSize = 13.sp, fontWeight = FontWeight.W700, color = Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(
                    if (active) SageSoft else InactiveItem,
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, label,
                tint = if (active) Sage else InactiveIcon,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.W700,
            color = if (active) InkSub else InactiveText
        )
    }
}

@Composable
fun MiniBarChart(bars: List<Float>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEachIndexed { index, value ->
            val color = when {
                index == bars.lastIndex -> Sage
                index >= bars.size - 3 -> BarTintMid
                else -> BarTintLo
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(value)
                    .background(color, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            )
        }
    }
}

@Composable
private fun ChildBottomBar(selectedTab: Int, onSelect: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.border(width = 1.dp, color = Line)
    ) {
        val items = listOf(
            "홈" to Icons.Outlined.Home,
            "가족 대화" to Icons.Outlined.ChatBubbleOutline,
            "리포트" to Icons.Outlined.BarChart,
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
