package cc.dinnerparty.compose.ui.nav

import cc.dinnerparty.compose.ui.page.AvatarPageOps
import cc.dinnerparty.compose.ui.page.AvatarPageUi
import cc.dinnerparty.compose.ui.page.SettingsPageOps
import cc.dinnerparty.compose.ui.page.SettingsPageUi

/** 全屏页面所需的数据与操作 */
class AppPages(
    val settings: SettingsPageUi = SettingsPageUi(),
    val settingsOps: SettingsPageOps = SettingsPageOps(),
    val avatar: AvatarPageUi = AvatarPageUi(),
    val avatarOps: AvatarPageOps = AvatarPageOps(),
)
