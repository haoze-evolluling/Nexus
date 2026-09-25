package com.haoze.nexus.ui.compose

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import kotlinx.coroutines.launch

private fun Context.showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

@Composable
fun SponsorListScreen(
    onBack: () -> Unit,
    title: String = stringResource(R.string.home_sponsor_list_title)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var configuration by remember { mutableStateOf<RecognitionMembersConfiguration?>(null) }
    var isConfigurationLoading by remember { mutableStateOf(true) }
    val sponsors = configuration?.sponsors.orEmpty()
    val avatarLoader = rememberRecognitionAvatarLoader(sponsors)
    var newestFirst by remember { mutableStateOf(false) }
    val displayedSponsors = if (newestFirst) sponsors.asReversed() else sponsors

    LaunchedEffect(context.applicationContext) {
        val cachedConfiguration = RecognitionMembersRepository.loadCached(context.applicationContext)
        configuration = cachedConfiguration
        if (cachedConfiguration != null) isConfigurationLoading = false
        runCatching { RecognitionMembersRepository.refresh(context.applicationContext) }
            .onFailure { error ->
                context.showToast("名单更新失败：")
            }
            .getOrNull()
            ?.let { configuration = it }
        isConfigurationLoading = false
    }

    SettingsScaffold(
        title = title,
        onBack = onBack,
        actions = {
            IconButton(
                enabled = !avatarLoader.isRefreshing,
                onClick = {
                    scope.launch {
                        val refreshedConfiguration = runCatching {
                            RecognitionMembersRepository.refresh(context.applicationContext)
                        }.getOrElse { error ->
                            context.showToast("名单更新失败：")
                            return@launch
                        }
                        if (refreshedConfiguration != null) {
                            configuration = refreshedConfiguration
                            context.showToast("名单已更新，正在加载头像")
                            return@launch
                        }
                        val result = avatarLoader.retryMissingOrFailed()
                        val message = when {
                            result.refreshedCount == 0 && result.failedCount == 0 -> "头像均已缓存，无需刷新"
                            result.failedCount == 0 -> "已刷新  个头像"
                            else -> "已刷新  个头像， 个头像仍未加载"
                        }
                        context.showToast(message, Toast.LENGTH_SHORT)
                    }
                }
            ) {
                if (avatarLoader.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.padding(10.dp))
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "刷新头像")
                }
            }
            IconButton(onClick = {
                newestFirst = !newestFirst
                context.showToast(if (newestFirst) "当前按赞助时间由晚到早排列" else "当前按赞助时间由早到晚排列")
            }) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = if (newestFirst) "当前按赞助时间由晚到早排列，点击切换为由早到晚" else "当前按赞助时间由早到晚排列，点击切换为由晚到早"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsInfoText(
                text = "感谢每一位支持 Nexus 项目的朋友！名单默认按赞助时间由早到晚排列，可通过右上角按钮切换为由晚到早；与赞助金额无关，每一份支持都同样珍贵。",
                modifier = Modifier.padding(top = 8.dp)
            )
            if (isConfigurationLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                RecognitionList(
                    members = displayedSponsors,
                    emptyText = "暂时还没有赞助者，期待在这里写下你的名字。",
                    avatarStates = avatarLoader.states
                )
            }
        }
    }
}

@Composable
fun RecognitionList(
    members: List<RecognitionMember>,
    emptyText: String,
    avatarStates: Map<String, RecognitionAvatarState>
) {
    SettingsSurfaceGroup(
        content = if (members.isEmpty()) {
            listOf {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                )
            }
        } else {
            members.map { member ->
                {
                    RecognitionListItem(member, member.avatarFileName?.let(avatarStates::get))
                }
            }
        }
    )
}

@Composable
private fun RecognitionListItem(member: RecognitionMember, avatarState: RecognitionAvatarState?) {
    val avatarFile = (avatarState as? RecognitionAvatarState.Available)?.file
    val avatar = remember(avatarFile) {
        avatarFile?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
    }
    val usesDefaultAvatar = member.avatarFileName == null
    val hasAvatar = usesDefaultAvatar || avatar != null
    val textStart by animateDpAsState(
        targetValue = if (hasAvatar) 60.dp else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "recognitionTextStart"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            AnimatedVisibility(
                visible = hasAvatar,
                enter = fadeIn(animationSpec = tween(durationMillis = 160, delayMillis = 220)),
                exit = fadeOut(animationSpec = tween(durationMillis = 100))
            ) {
                if (usesDefaultAvatar) {
                    Image(
                        painter = painterResource(R.drawable.default_avatar),
                        contentDescription = "的头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else {
                    avatar?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "的头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.padding(start = textStart)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = member.acknowledgement,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
