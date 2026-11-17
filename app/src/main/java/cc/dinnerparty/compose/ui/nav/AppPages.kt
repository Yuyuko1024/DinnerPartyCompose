package cc.dinnerparty.compose.ui.nav

import cc.dinnerparty.compose.ui.page.AvatarPageOps
import cc.dinnerparty.compose.ui.page.AvatarPageUi
import cc.dinnerparty.compose.ui.page.PasswordPageOps
import cc.dinnerparty.compose.ui.page.SettingsPageOps
import cc.dinnerparty.compose.ui.page.SettingsPageUi
import cc.dinnerparty.compose.ui.page.UsernamePageOps
import cc.dinnerparty.compose.ui.page.UsernamePageUi

/** 全屏页面所需的数据 */
class AppPageData(
    val settings: SettingsPageUi = SettingsPageUi(),
    val avatar: AvatarPageUi = AvatarPageUi(),
    val username: UsernamePageUi = UsernamePageUi(),
)

/** 全屏页面所需的操作 */
class AppPageOps(
    val settings: SettingsPageOps = SettingsPageOps(),
    val avatar: AvatarPageOps = AvatarPageOps(),
    val username: UsernamePageOps = UsernamePageOps(),
    val password: PasswordPageOps = PasswordPageOps(),
)
