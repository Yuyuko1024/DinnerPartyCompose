package cc.dinnerparty.compose.ui.nav

import cc.dinnerparty.compose.ui.view.screen.AvatarPageOps
import cc.dinnerparty.compose.ui.view.screen.AvatarPageUi
import cc.dinnerparty.compose.ui.view.screen.MapSettingsPageOps
import cc.dinnerparty.compose.ui.view.screen.MapSettingsPageUi
import cc.dinnerparty.compose.ui.view.screen.PasswordPageOps
import cc.dinnerparty.compose.ui.view.screen.SettingsPageOps
import cc.dinnerparty.compose.ui.view.screen.SettingsPageUi
import cc.dinnerparty.compose.ui.view.screen.SkinPageOps
import cc.dinnerparty.compose.ui.view.screen.ThemePageOps
import cc.dinnerparty.compose.ui.view.screen.ThemePageUi
import cc.dinnerparty.compose.ui.view.screen.UsernamePageOps
import cc.dinnerparty.compose.ui.view.screen.UsernamePageUi

/** 全屏页面所需的数据 */
class AppPageData(
    val settings: SettingsPageUi = SettingsPageUi(),
    val theme: ThemePageUi = ThemePageUi(),
    val mapSettings: MapSettingsPageUi = MapSettingsPageUi(),
    val avatar: AvatarPageUi = AvatarPageUi(),
    val username: UsernamePageUi = UsernamePageUi(),
)

/** 全屏页面所需的操作 */
class AppPageOps(
    val settings: SettingsPageOps = SettingsPageOps(),
    val theme: ThemePageOps = ThemePageOps(),
    val skin: SkinPageOps = SkinPageOps(),
    val mapSettings: MapSettingsPageOps = MapSettingsPageOps(),
    val avatar: AvatarPageOps = AvatarPageOps(),
    val username: UsernamePageOps = UsernamePageOps(),
    val password: PasswordPageOps = PasswordPageOps(),
)
