package cc.dinnerparty.compose.ui.view.component

import androidx.compose.runtime.compositionLocalOf

/** 弹窗与页面动效开关 关闭时只淡入淡出 */
val LocalMotion = compositionLocalOf { true }
