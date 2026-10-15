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

    /** 被拒绝，但系统弹窗还能再弹，下次可以再申请。 */
    DENIED,

    /** 用户勾了「不再询问」，系统不会再弹窗，只能引导去应用设置页手动开。 */
    DENIED_PERMANENTLY
}

/** 发起一次权限申请；已授权的权限不会再弹窗。 */
typealias PermissionLauncher = () -> Unit

/**
 * 申请一组运行时权限。
 *
 * `onResult` 回调里不要直接申请别的权限，先处理结果（Android 的限制）。
 */
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
            // 已经有了就直接回调，不要再弹一次打扰用户
            onResult(PermissionResult.GRANTED)
        } else {
            // vararg 拿到的是 Array<out String>，launch 只收 Array<String>，这里拷一份
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

    // 被拒之后 shouldShowRequestPermissionRationale 仍为 true，说明系统下次还会弹窗；
    // 为 false 就是「不再询问」，再申请也不会弹，只能去设置页。
    val activity = context.findActivity()
    val canAskAgain = activity != null &&
        permissions.any { activity.shouldShowRequestPermissionRationale(it) }
    return if (canAskAgain) PermissionResult.DENIED else PermissionResult.DENIED_PERMANENTLY
}

/** 这个权限是不是已经授予了。给「有权限就直接做，没权限先别打扰」的场景用。 */
internal fun String.isGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, this) == PackageManager.PERMISSION_GRANTED

/** Compose 拿到的 Context 可能被 ContextWrapper 包过几层，逐层解开找宿主 Activity。 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
