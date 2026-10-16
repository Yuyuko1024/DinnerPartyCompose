package cc.dinnerparty.compose.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** 权限申请结果。 */
enum class PermissionResult {
    /** 申请的全部权限都已授予。 */
    GRANTED,

    /** 被拒绝，系统仍可再次弹窗。 */
    DENIED,

    /** 用户勾选「不再询问」，只能去应用设置页手动开启。 */
    DENIED_PERMANENTLY
}

/** 发起一次权限申请。 */
typealias PermissionLauncher = () -> Unit

/** 申请一组运行时权限。 */
@Composable
fun rememberPermissionLauncher(
    vararg permissions: String,
    onResult: (PermissionResult) -> Unit
): PermissionLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        onResult(resolveResult(context, permissions, grants))
    }
    return {
        if (permissions.all { it.isGranted(context) }) {
            onResult(PermissionResult.GRANTED)
        } else {
            launcher.launch(arrayOf(*permissions))
        }
    }
}

/** 打开本应用的系统设置页，供用户手动开启权限。 */
fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}

private fun resolveResult(
    context: Context,
    permissions: Array<out String>,
    grants: Map<String, Boolean>
): PermissionResult {
    if (permissions.all { grants[it] == true }) return PermissionResult.GRANTED

    // rationale 为 true 表示系统仍可弹窗，为 false 则表示「不再询问」
    val activity = context.findActivity()
    val canAskAgain = activity != null &&
        permissions.any { activity.shouldShowRequestPermissionRationale(it) }
    return if (canAskAgain) PermissionResult.DENIED else PermissionResult.DENIED_PERMANENTLY
}

/** 这个权限是不是已经授予了。 */
internal fun String.isGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, this) == PackageManager.PERMISSION_GRANTED

/** 逐层解开 ContextWrapper 找宿主 Activity。 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
