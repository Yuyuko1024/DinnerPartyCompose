package cc.dinnerparty.compose.ui.view.component

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.dinnerparty.compose.R
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.tanishranjan.cropkit.CropDefaults
import com.tanishranjan.cropkit.CropRatio
import com.tanishranjan.cropkit.CropShape
import com.tanishranjan.cropkit.GridLinesType
import com.tanishranjan.cropkit.GridLinesVisibility
import com.tanishranjan.cropkit.ImageCropper
import com.tanishranjan.cropkit.rememberCropController

/** 全屏裁剪界面 */
@UnstableSaltUiApi
@Composable
internal fun AvatarCropDialog(
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
                    contentColor = Color.White
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
                    contentColor = Color.Black
                )
            }
        }
    }
}
