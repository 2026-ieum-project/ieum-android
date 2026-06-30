package com.ieum.app.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ieum.app.NavRoute
import com.ieum.app.api.FeatureApiClient
import com.ieum.app.keystroke.KeystrokeAnalyzer
import com.ieum.app.ui.theme.BubbleReceived
import com.ieum.app.ui.theme.BubbleReceivedText
import com.ieum.app.ui.theme.BubbleSent
import com.ieum.app.ui.theme.BubbleSentText
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scope = rememberCoroutineScope()

    var groupId by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var textInput by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val keystrokeAnalyzer = remember { KeystrokeAnalyzer() }
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val audioFile = remember { mutableStateOf<File?>(null) }
    val recordingStartTime = remember { mutableStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 권한 결과는 버튼 클릭 시 재확인 */ }

    // 사용자 정보 로드
    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                groupId = snapshot.child("groupId").getValue(String::class.java) ?: ""
                userName = snapshot.child("name").getValue(String::class.java) ?: ""
                isLoading = false
            }
    }

    // 화면 나갈 때 녹음 정리
    DisposableEffect(Unit) {
        onDispose {
            recorder.value?.let { rec ->
                try { rec.stop() } catch (_: Exception) {}
                rec.release()
                recorder.value = null
            }
            audioFile.value?.delete()
        }
    }

    // 실시간 메시지 리스너
    DisposableEffect(groupId) {
        if (groupId.isEmpty()) return@DisposableEffect onDispose {}
        val repo = MessageRepository(groupId)
        val listener = repo.listenMessages { newMessages ->
            messages = newMessages
        }
        onDispose { repo.removeListener(listener) }
    }

    // 새 메시지 수신 시 자동 스크롤
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "가족 채팅",
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
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("로딩 중...", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                // 메시지 목록
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(message = message, isMine = message.senderId == uid)
                    }
                }

                // 입력 바
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 음성 녹음 버튼 (탭 토글: 누르면 녹음, 다시 누르면 전송)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (isRecording) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .clickable {
                                if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@clickable
                                }
                                if (groupId.isEmpty()) return@clickable

                                if (!isRecording) {
                                    // 녹음 시작
                                    val file = File(
                                        context.cacheDir,
                                        "voice_${System.currentTimeMillis()}.m4a"
                                    )
                                    audioFile.value = file

                                    try {
                                        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            MediaRecorder(context)
                                        } else {
                                            @Suppress("DEPRECATION") MediaRecorder()
                                        }
                                        rec.apply {
                                            setAudioSource(MediaRecorder.AudioSource.MIC)
                                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                            setOutputFile(file.absolutePath)
                                            prepare()
                                            start()
                                        }
                                        recorder.value = rec
                                        isRecording = true
                                        recordingStartTime.value = System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        file.delete()
                                        Toast.makeText(context, "녹음을 시작할 수 없습니다", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // 녹음 중지 + 전송
                                    val rec = recorder.value
                                    val file = audioFile.value
                                    val duration = System.currentTimeMillis() - recordingStartTime.value

                                    isRecording = false

                                    if (rec != null) {
                                        try {
                                            rec.stop()
                                            rec.release()
                                        } catch (e: Exception) {
                                            rec.release()
                                            file?.delete()
                                            recorder.value = null
                                            return@clickable
                                        }
                                        recorder.value = null
                                    }

                                    if (file != null && file.exists() && duration >= 500) {
                                        val bytes = file.readBytes()
                                        file.delete()
                                        scope.launch {
                                            MessageRepository(groupId).sendVoiceMessage(
                                                uid, userName, bytes
                                            ) { _, _ -> }
                                        }
                                    } else {
                                        file?.delete()
                                        Toast.makeText(context, "녹음이 너무 짧습니다", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "녹음 중지" else "녹음 시작",
                            tint = Color.White
                        )
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = {
                            keystrokeAnalyzer.onTextChanged(it)
                            textInput = it
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        placeholder = { Text("메시지 입력", color = MaterialTheme.colorScheme.outline) },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isBlank() || groupId.isEmpty()) return@IconButton
                            val repo = MessageRepository(groupId)
                            repo.sendTextMessage(uid, userName, textInput.trim()) { success, _ ->
                                if (success) {
                                    textInput = ""
                                    if (keystrokeAnalyzer.hasData()) {
                                        val f = keystrokeAnalyzer.getFeatures()
                                        scope.launch {
                                            FeatureApiClient.sendKeystroke(uid, f)
                                        }
                                        keystrokeAnalyzer.reset()
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    val bubbleColor = if (isMine) BubbleSent else BubbleReceived
    val textColor = if (isMine) BubbleSentText else BubbleReceivedText

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            if (!isMine) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(2.dp))
            }

            Box(
                modifier = Modifier
                    .background(color = bubbleColor, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (message.type == Message.TYPE_TEXT) {
                    Text(text = message.content, color = textColor)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (isPlaying) return@IconButton
                                isPlaying = true
                                try {
                                    MediaPlayer().apply {
                                        setDataSource(message.content)
                                        setOnErrorListener { mp, _, _ ->
                                            isPlaying = false
                                            mp.release()
                                            true
                                        }
                                        setOnPreparedListener { it.start() }
                                        setOnCompletionListener {
                                            isPlaying = false
                                            it.release()
                                        }
                                        prepareAsync()
                                    }
                                } catch (e: Exception) {
                                    isPlaying = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop
                                else Icons.Default.PlayArrow,
                                contentDescription = "음성 재생",
                                tint = textColor
                            )
                        }
                        Text(text = "음성 메시지", color = textColor)
                    }
                }
            }

            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
    }.format(Date(timestamp))
}
