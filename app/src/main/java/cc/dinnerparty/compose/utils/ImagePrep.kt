package cc.dinnerparty.compose.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 读取图片并限制长边尺寸 */
internal suspend fun decodeImage(context: Context, uri: Uri): Bitmap =
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
internal fun Bitmap.toJpegBytes(): ByteArray {
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
        check(scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream)) { "jpeg encode failed" }
        stream.toByteArray()
    }
}

private const val DecodeMaxEdge = 2048
private const val UploadMaxEdge = 1024
