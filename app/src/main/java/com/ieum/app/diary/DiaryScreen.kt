package com.ieum.app.diary

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.ieum.app.storage.OracleObjectStorageConfig
import com.ieum.app.ui.theme.Coral
import com.ieum.app.ui.theme.CoralSoft
import com.ieum.app.ui.theme.Ink
import com.ieum.app.ui.theme.InkSub
import com.ieum.app.ui.theme.Muted
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    navController: NavController,
    autoRecord: Boolean = false,
    viewModel: DiaryViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    // autoRecord로 진입하면 촬영 선택 시트를 바로 열어줌
    var showVideoSheet by remember { mutableStateOf(autoRecord) }
    var playingDiary by remember { mutableStateOf<Diary?>(null) }

    // 업로드 성공 토스트
    LaunchedEffect(state.uploadSuccess) {
        if (state.uploadSuccess) {
            Toast.makeText(context, "영상 일기가 저장되었어요", Toast.LENGTH_SHORT).show()
            viewModel.clearUploadSuccess()
        }
    }

    // 에러 토스트
    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage.isNotEmpty()) {
            Toast.makeText(context, state.errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    // 갤러리에서 영상 선택
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "video/mp4"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        viewModel.uploadVideo(bytes, mimeType)
    }

    // 카메라로 영상 촬영
    val videoUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (!success) return@rememberLauncherForActivityResult
        val uri = videoUri.value ?: return@rememberLauncherForActivityResult
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "video/mp4"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        viewModel.uploadVideo(bytes, mimeType)
    }

    // 카메라 권한
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "diary_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            videoUri.value = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 영상 재생 오버레이
    if (playingDiary != null) {
        VideoPlayerOverlay(
            videoUrl = playingDiary!!.videoUrl,
            title = "${playingDiary!!.senderName}의 영상 일기",
            subtitle = formatDiaryDate(playingDiary!!.timestamp),
            onDismiss = { playingDiary = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "영상 일기",
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
                        Text("로딩 중...", color = Muted)
                    }
                } else {
                    // 영상 촬영/선택 버튼
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { showVideoSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Coral),
                            enabled = !state.isUploading
                        ) {
                            if (state.isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "업로드 중...",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.W800,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "오늘의 영상 일기 남기기",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.W800,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 일기 목록
                    if (state.diaries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Muted
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "아직 영상 일기가 없어요\n오늘의 이야기를 영상으로 남겨보세요",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.W600,
                                    color = InkSub,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.diaries, key = { it.id }) { diary ->
                                DiaryItem(
                                    diary = diary,
                                    onClick = { playingDiary = diary }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 영상 선택 바텀시트
        if (showVideoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showVideoSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        "영상 일기 남기기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // 카메라로 촬영
                    Surface(
                        onClick = {
                            showVideoSheet = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                val file = File(context.cacheDir, "diary_${System.currentTimeMillis()}.mp4")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                videoUri.value = uri
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
                            showVideoSheet = false
                            galleryLauncher.launch("video/*")
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
                                Icons.Default.VideoLibrary,
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
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerOverlay(
    videoUrl: String,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(
                MediaItem.fromUri(OracleObjectStorageConfig.resolveReadUrl(videoUrl))
            )
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    BackHandler(onBack = onDismiss)

    // 상단 바를 PlayerView와 겹치지 않게 분리 배치:
    // PlayerView는 자기 영역의 터치를 모두 소비하므로 위에 겹친 Compose 버튼이 눌리지 않음
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // 오버레이 아래 화면으로 터치가 전달되지 않도록 소비
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        // 상단 바: 제목
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                // 강제 edge-to-edge(targetSdk 35+) 대응: 상태바 아래로 내용 배치
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W800,
                    color = Color.White
                )
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // 영상 플레이어
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // 하단 닫기 버튼 (크고 누르기 쉽게)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                // 하단 제스처 바와 겹치지 않게 배치
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("닫기", fontSize = 18.sp, fontWeight = FontWeight.W700)
            }
        }
    }
}

@Composable
private fun DiaryItem(diary: Diary, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 재생 아이콘
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(CoralSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "재생",
                    tint = Coral,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${diary.senderName}의 영상 일기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W800,
                    color = Ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatDiaryDate(diary.timestamp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                    color = Muted
                )
            }
        }
    }
}

private fun formatDiaryDate(timestamp: Long): String {
    return SimpleDateFormat("M월 d일 (E) a h:mm", Locale.KOREAN).apply {
        timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
    }.format(Date(timestamp))
}
