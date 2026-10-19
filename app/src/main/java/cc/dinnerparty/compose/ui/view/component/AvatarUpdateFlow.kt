package cc.dinnerparty.compose.ui.view.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.ApiException
import cc.dinnerparty.compose.ui.viewmodel.AuthViewModel
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonType
import com.moriafly.salt.ui.ItemOuterTip
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.dialog.BasicDialog
import com.moriafly.salt.ui.dialog.DialogTitle
import com.moriafly.salt.ui.outerPadding
import com.tanishranjan.cropkit.CropDefaults
import com.tanishranjan.cropkit.CropRatio
import com.tanishranjan.cropkit.CropShape
import com.tanishranjan.cropkit.GridLinesType
import com.tanishranjan.cropkit.GridLinesVisibility
import com.tanishranjan.cropkit.ImageCropper
import com.tanishranjan.cropkit.rememberCropController
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 头像更新流程 */
@OptIn(UnstableSaltUiApi::class)
@Composable
fun AvatarUpdateFlow(
    visible: Boolean,
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var cropped by remember { mutableStateOf<Bitmap?>(null) }
    var uploading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            onDismiss()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val bitmap = runCatching { decodeBitmap(context, uri) }.getOrNull()
            if (bitmap == null) {
                onMessage(context.getString(R.string.avatar_load_failed))
                onDismiss()
            } else {
                source = bitmap
            }
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            picker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            source = null
            cropped = null
            uploading = false
        }
    }

    source?.let { image ->
        CropDialog(
            image = image,
            onCancel = onDismiss,
            onConfirm = { result ->
                source = null
                cropped = result
            },
            onMessage = onMessage,
        )
    }

    cropped?.let { image ->
        UploadDialog(
            image = image,
            uploading = uploading,
            onCancel = {
                if (!uploading) {
                    cropped = null
                    onDismiss()
                }
            },
            onConfirm = {
                if (!uploading) {
                    uploading = true
                    scope.launch {
                        val bytes = withContext(Dispatchers.IO) { image.toJpegBytes() }
                        runCatching { authViewModel.uploadAvatar(bytes) }
                            .onSuccess {
                                uploading = false
                                cropped = null
                                onMessage(context.getString(R.string.avatar_upload_success))
                                onDismiss()
                            }
                            .onFailure { error ->
                                uploading = false
                                onMessage(
                                    (error as? ApiException)?.serverMessage
                                        ?: context.getString(R.string.avatar_upload_failed)
                                )
                            }
                    }
                }
            },
        )
    }
}

/** 全屏裁剪界面 */
@OptIn(UnstableSaltUiApi::class)
@Composable
private fun CropDialog(
    image: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val options = remember {
        CropDefaults.cropOptions(
            cropShape = CropShape.AspectRatio(CropRatio.SQUARE),
            gridLinesVisibility = GridLinesVisibility.ALWAYS,
            gridLinesType = GridLinesType.GRID_AND_CIRCLE,
            handleRadius = 10.dp,
            touchPadding = 16.dp,
        )
    }
    val controller = rememberCropController(bitmap = image, cropOptions = options)

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            ImageCropper(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp, bottom = 104.dp, start = 16.dp, end = 16.dp),
                cropController = controller,
            )
            Text(
                text = stringResource(R.string.avatar_crop_title),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                    onClick = onCancel,
                    containerColor = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                    borderColor = null,
                )
                CircleIconButton(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = stringResource(R.string.avatar_crop_confirm),
                    onClick = {
                        runCatching { controller.crop() }
                            .onSuccess(onConfirm)
                            .onFailure {
                                onMessage(context.getString(R.string.avatar_crop_failed))
                            }
                    },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    borderColor = null,
                )
            }
        }
    }
}

/** 圆形头像预览与上传确认 */
@OptIn(UnstableSaltUiApi::class)
@Composable
private fun UploadDialog(
    image: Bitmap,
    uploading: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    BasicDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties()
    ) {
        DialogTitle(text = stringResource(R.string.avatar_upload_title))
        Box(
            modifier = Modifier.fillMaxWidth().padding(0.dp, 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
            )
        }
        Row(
            modifier = Modifier.outerPadding()
        ) {
            Button(
                onClick = onCancel,
                text = stringResource(R.string.action_cancel),
                modifier = Modifier
                    .weight(1f),
                type = ButtonType.Sub
            )
            Spacer(modifier = Modifier.width(SaltTheme.dimens.padding))
            Button(
                onClick = {
                    onConfirm()
                },
                text = stringResource(
                    if (uploading) R.string.avatar_uploading else R.string.avatar_upload_action
                ),
                modifier = Modifier
                    .weight(1f),
                maxLines = 1
            )
        }
    }
}

/** 读取图片并限制长边尺寸 */
private suspend fun decodeBitmap(context: Context, uri: Uri): Bitmap =
    withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > DecodeMaxEdge) {
                    val scale = DecodeMaxEdge.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

/** 压缩为上传用的 JPEG 字节 */
private fun Bitmap.toJpegBytes(): ByteArray {
    val longest = maxOf(width, height)
    val scaled = if (longest > UploadMaxEdge) {
        val scale = UploadMaxEdge.toFloat() / longest
        Bitmap.createScaledBitmap(
            this,
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
    } else {
        this
    }
    return ByteArrayOutputStream().use { stream ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        stream.toByteArray()
    }
}

private const val DecodeMaxEdge = 2048
private const val UploadMaxEdge = 1024
