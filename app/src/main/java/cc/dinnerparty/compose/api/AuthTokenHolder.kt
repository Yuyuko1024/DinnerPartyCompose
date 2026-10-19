package cc.dinnerparty.compose.api

/** 当前登录凭证，由本地登录态同步而来。 */
internal object AuthTokenHolder {

    @Volatile
    var token: String? = null
}
