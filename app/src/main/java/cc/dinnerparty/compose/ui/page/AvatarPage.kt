package cc.dinnerparty.compose.ui.page

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.ApiException
import cc.dinnerparty.compose.api.model.User
import cc.dinnerparty.compose.ui.view.component.AvatarCropDialog
import cc.dinnerparty.compose.ui.view.component.PhoneSheet
import cc.dinnerparty.compose.ui.view.component.UserAvatar
import cc.dinnerparty.compose.utils.decodeImage
import cc.dinnerparty.compose.utils.toJpegBytes
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonAppearance
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemButton
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 修改头像页的数据 */
class AvatarPageUi(
    val user: User? = null,
    val avatarVersion: Long = 0L,
)

/** 修改头像页的操作 */
class AvatarPageOps(
    val onBack: () -> Unit = {},
    val onMessage: (String) -> Unit = {},
    val onSave: suspend (ByteArray) -> Unit = {},
)

/** 修改头像全屏页 */
@UnstableSaltUiApi
@Composable
fun AvatarPage(
    ui: AvatarPageUi,
    ops: AvatarPageOps,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 待裁剪的原图
    var source by remember { mutableStateOf<Bitmap?>(null) }

    // 裁剪结果 为空表示未修改
    var cropped by remember { mutableStateOf<Bitmap?>(null) }
    var choosing by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var shotUri by remember { mutableStateOf<Uri?>(null) }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { source = readImage(context, uri, ops.onMessage) }
    }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { done ->
        val uri = shotUri
        if (!done || uri == null) return@rememberLauncherForActivityResult
        scope.launch { source = readImage(context, uri, ops.onMessage) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PageScaffold(
            info = PageInfo(
                path = stringResource(R.string.avatar_page_path),
                backText = stringResource(R.string.avatar_back),
            ),
            onBack = ops.onBack,
        ) {
            RoundedColumn(
                modifier = Modifier.padding(bottom = PagePadding),
                paddingValues = PaddingValues(PagePadding),
            ) {
                AvatarPreview(ui = ui, image = cropped)
                Button(
                    onClick = { choosing = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = PagePadding),
                    text = stringResource(R.string.avatar_pick),
                )
                Button(
                    onClick = {
                        val image = cropped ?: return@Button
                        uploading = true
                        scope.launch {
                            val bytes = withContext(Dispatchers.IO) { image.toJpegBytes() }
                            runCatching { ops.onSave(bytes) }
                                .onSuccess {
                                    uploading = false
                                    ops.onMessage(context.getString(R.string.avatar_upload_success))
                                    ops.onBack()
                                }
                                .onFailure { error ->
                                    uploading = false
                                    ops.onMessage(
                                        (error as? ApiException)?.serverMessage
                                            ?: context.getString(R.string.avatar_upload_failed)
                                    )
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = PagePadding, bottom = PagePadding),
                    enabled = cropped != null && !uploading,
                    text = stringResource(
                        if (uploading) R.string.avatar_saving else R.string.avatar_save
                    ),
                )
            }
        }

        PhoneSheet(
            value = if (choosing) Unit else null,
            onDismissRequest = { choosing = false },
            modal = true,
        ) {
            Column(modifier = Modifier.padding(horizontal = PagePadding, vertical = CardPadding)) {
                Text(
                    text = stringResource(R.string.avatar_source_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PagePadding),
                    color = SaltTheme.colors.text,
                    textAlign = TextAlign.Center,
                    style = SaltTheme.textStyles.main,
                )
                ItemButton(
                    onClick = {
                        choosing = false
                        val uri = runCatching { newShotUri(context) }.getOrNull()
                        if (uri == null) {
                            ops.onMessage(context.getString(R.string.avatar_shot_failed))
                        } else {
                            shotUri = uri
                            camera.launch(uri)
                        }
                    },
                    text = stringResource(R.string.avatar_source_camera),
                )
                ItemButton(
                    onClick = {
                        choosing = false
                        gallery.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.padding(top = CardPadding),
                    text = stringResource(R.string.avatar_source_gallery),
                )
                ItemButton(
                    onClick = { choosing = false },
                    modifier = Modifier.padding(top = CardPadding),
                    text = stringResource(R.string.action_cancel),
                    primary = false,
                )
            }
        }
    }

    source?.let { image ->
        AvatarCropDialog(
            image = image,
            onCancel = { source = null },
            onConfirm = { result ->
                source = null
                cropped = result
            },
            onMessage = ops.onMessage,
        )
    }
}

/** 卡片里的圆形头像预览 */
@UnstableSaltUiApi
@Composable
private fun AvatarPreview(ui: AvatarPageUi, image: Bitmap?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 0.dp, start = 0.dp, end = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(BigAvatar)
                    .clip(CircleShape),
            )
        } else {
            val user = ui.user
            if (user != null) {
                UserAvatar(user = user, size = BigAvatar, avatarVersion = ui.avatarVersion)
            } else {
                Box(
                    modifier = Modifier
                        .size(BigAvatar)
                        .clip(CircleShape)
                        .background(SaltTheme.colors.subBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize),
                        tint = SaltTheme.colors.subText,
                    )
                }
            }
        }
    }
}

/** 读取相册或相机返回的图片 失败时提示并保持原状态 */
private suspend fun readImage(ctx: Context, uri: Uri, onMessage: (String) -> Unit): Bitmap? {
    val bitmap = runCatching { decodeImage(ctx, uri) }.getOrNull()
    if (bitmap == null) onMessage(ctx.getString(R.string.avatar_load_failed))
    return bitmap
}

/** 相机写入用的缓存文件 uri */
private fun newShotUri(ctx: Context): Uri {
    val dir = File(ctx.cacheDir, ShotDir).apply { mkdirs() }
    val file = File.createTempFile(ShotPrefix, ShotExt, dir)
    return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
}

private val BigAvatar = 160.dp

private val IconSize = 64.dp

private const val ShotDir = "avatar"

private const val ShotPrefix = "shot"

private const val ShotExt = ".jpg"
