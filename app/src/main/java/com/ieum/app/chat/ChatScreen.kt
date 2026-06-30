package com.ieum.app.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import android.widget.Toast
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
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
fun ChatScreen(navController: NavController, viewModel: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var isRecording by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val keystrokeAnalyzer = remember { KeystrokeAnalyzer() }
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val audioFile = remember { mutableStateOf<File?>(null) }
    val recordingStartTime = remember { mutableStateOf(0L) }

    val scope = rememberCoroutineScope()
    var showPhotoSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 권한 결과는 버튼 클릭 시 재확인 */ }

    // 갤러리 선택 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        viewModel.sendImageMessage(bytes, mimeType)
    }

    // 카메라 촬영 런처
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) return@rememberLauncherForActivityResult
        val uri = cameraImageUri.value ?: return@rememberLauncherForActivityResult
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        viewModel.sendImageMessage(bytes, mimeType)
    }

    // 카메라 권한 런처
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraImageUri.value = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 갤러리 권한 런처 (Android 12 이하)
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            Toast.makeText(context, "갤러리 접근 권한이 필요합니다", Toast.LENGTH_SHORT).show()
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

    // 최초 로드 완료 시 맨 아래로 스크롤
    var initialScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && !initialScrollDone && state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.size - 1)
            initialScrollDone = true
        }
    }

    // 새 메시지 수신 시 자동 스크롤 (맨 아래 근처일 때만)
    val previousMessageCount = remember { mutableStateOf(state.messages.size) }
    LaunchedEffect(state.messages.size) {
        val added = state.messages.size - previousMessageCount.value
        previousMessageCount.value = state.messages.size
        if (added > 0 && added <= 2 && state.messages.isNotEmpty()) {
            // 새 메시지가 추가된 경우 (페이징이 아닌 경우에만 스크롤)
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val isNearBottom = lastVisible >= state.messages.size - 3
            if (isNearBottom) {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    // 상단 스크롤 감지 → 이전 메시지 로드
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstIndex ->
                if (firstIndex <= 1 && state.hasOlderMessages && !state.isLoadingOlder) {
                    val beforeCount = state.messages.size
                    viewModel.loadOlderMessages()
                    // 로드 후 스크롤 위치 보정: 추가된 메시지 수만큼 아래로 밀어줌
                    snapshotFlow { viewModel.uiState.value.messages.size }
                        .collect { afterCount ->
                            val added = afterCount - beforeCount
                            if (added > 0) {
                                listState.scrollToItem(firstIndex + added)
                                return@collect
                            }
                        }
                }
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
            if (state.isLoading) {
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
                    // 이전 메시지 로딩 인디케이터
                    if (state.isLoadingOlder) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message = message, isMine = message.senderId == state.uid)
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
                    // 음성 녹음 버튼
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
                                if (state.groupId.isEmpty()) return@clickable

                                if (!isRecording) {
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
                                        viewModel.sendVoiceMessage(bytes)
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

                    // 사진 버튼 → 바텀시트 열기
                    IconButton(onClick = { showPhotoSheet = true }) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "사진 보내기",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = state.textInput,
                        onValueChange = {
                            keystrokeAnalyzer.onTextChanged(it)
                            viewModel.onTextInputChange(it)
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

                    IconButton(onClick = {
                        if (keystrokeAnalyzer.hasData()) {
                            val f = keystrokeAnalyzer.getFeatures()
                            scope.launch { FeatureApiClient.sendKeystroke(state.uid, f) }
                            keystrokeAnalyzer.reset()
                        }
                        viewModel.sendTextMessage()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // 사진 선택 바텀시트
    if (showPhotoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "사진 보내기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 카메라로 촬영
                Surface(
                    onClick = {
                        showPhotoSheet = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraImageUri.value = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text("카메라로 촬영", fontSize = 16.sp, fontWeight = FontWeight.W600)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 갤러리에서 선택
                Surface(
                    onClick = {
                        showPhotoSheet = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // Android 13+: Photo Picker는 권한 불필요
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            // Android 12 이하: READ_EXTERNAL_STORAGE 필요
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text("갤러리에서 선택", fontSize = 16.sp, fontWeight = FontWeight.W600)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
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
                when (message.type) {
                    Message.TYPE_TEXT -> {
                        Text(text = message.content, color = textColor)
                    }
                    Message.TYPE_IMAGE -> {
                        AsyncImage(
                            model = message.content,
                            contentDescription = "사진",
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        )
                    }
                    else -> {
                        // voice
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) return@IconButton
                                    isPlaying = true
                                    try {
                                        val mp = MediaPlayer()
                                        player.value = mp
                                        mp.setDataSource(message.content)
                                        mp.setOnErrorListener { p, _, _ ->
                                            isPlaying = false
                                            p.release()
                                            player.value = null
                                            true
                                        }
                                        mp.setOnPreparedListener { it.start() }
                                        mp.setOnCompletionListener {
                                            isPlaying = false
                                            it.release()
                                            player.value = null
                                        }
                                        mp.prepareAsync()
                                    } catch (e: Exception) {
                                        isPlaying = false
                                        player.value = null
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
