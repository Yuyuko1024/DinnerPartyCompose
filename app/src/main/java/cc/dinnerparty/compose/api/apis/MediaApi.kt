package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.MediaUpload
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** 图片上传接口。 */
interface MediaApi {

    /** 上传单张图片，需要登录。 */
    @Multipart
    @POST("upload")
    suspend fun upload(@Part image: MultipartBody.Part): MediaUpload
}
