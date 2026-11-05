package neko.qq.util

import android.content.Context
import neko.qq.constants.PrefKeys

/**
 * 偏好读取（运行在 QQ 进程）。
 * 数据由模块自身的 NekoSettingsActivity 写入 PREFS_NAME，
 * 这里通过 moduleContext 读取同一份文件，实现「设置开关实时生效」。
 */
object Prefs {

    private val sp by lazy {
        Contexts.moduleContext()
            .getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun bool(key: String, def: Boolean = false) = sp.getBoolean(key, def)
    private fun str(key: String, def: String = "") = sp.getString(key, def) ?: def
    private fun int(key: String, def: Int = 0) = str(key).toIntOrNull() ?: def

    val antiRecallEnabled: Boolean get() = bool(PrefKeys.ANTI_RECALL, true)
    val recallShowBanner: Boolean get() = bool(PrefKeys.RECALL_BANNER, true)
    val recallKeepContent: Boolean get() = bool(PrefKeys.RECALL_KEEP, true)
    val recallText: String get() = str(PrefKeys.RECALL_TEXT, "想撤回？Neko 已经帮你存好啦~")

    val groupMgrEnabled: Boolean get() = bool(PrefKeys.GROUP_MGR, true)
    val groupConfirm: Boolean get() = bool(PrefKeys.GROUP_CONFIRM, true)
    val groupLog: Boolean get() = bool(PrefKeys.GROUP_LOG, true)
    val groupMuteMinutes: Int get() = int(PrefKeys.GROUP_MUTE_MIN, 10)

    val emojiPanelEnabled: Boolean get() = bool(PrefKeys.EMOJI_PANEL, true)
    val emojiKaomoji: Boolean get() = bool(PrefKeys.EMOJI_KAOMOJI, true)
    val emojiRecent: Boolean get() = bool(PrefKeys.EMOJI_RECENT, true)

    // ===== 显示与界面 =====
    val previewFontSize: Int get() = int(PrefKeys.PREVIEW_FONT_SIZE, 18)
    val isPreviewFontEnabled: Boolean get() = previewFontSize != 18
    val forceTablet: Boolean get() = bool(PrefKeys.FORCE_TABLET)
    val forcePhone: Boolean get() = bool(PrefKeys.FORCE_PHONE)
    val modifyDpi: Int? get() = str(PrefKeys.MODIFY_DPI).toIntOrNull()
    val forceDefaultFont: Boolean get() = bool(PrefKeys.FORCE_DEFAULT_FONT)
    val splashColumns: Int get() = int(PrefKeys.SPLASH_COLUMNS, 4)
    val randomBubble: Boolean get() = bool(PrefKeys.RANDOM_BUBBLE)
    val customEmojiPanel: Boolean get() = bool(PrefKeys.CUSTOM_EMOJI_PANEL)
    val transparentAvatar: Boolean get() = bool(PrefKeys.TRANSPARENT_AVATAR)

    // ===== 隐私与去广告 =====
    val disableQQShow: Boolean get() = bool(PrefKeys.DISABLE_QQSHOW)
    val hideQZoneAd: Boolean get() = bool(PrefKeys.HIDE_QZONE_AD)
    val hideEmojiReaction: Boolean get() = bool(PrefKeys.HIDE_EMOJI_REACTION)
    val removeMsgReaction: Boolean get() = bool(PrefKeys.REMOVE_MSG_REACTION)
    val removeTopBannerAd: Boolean get() = bool(PrefKeys.REMOVE_TOP_BANNER_AD)
    val disableCrashReport: Boolean get() = bool(PrefKeys.DISABLE_CRASH_REPORT)
    val disableSubchannelNotify: Boolean get() = bool(PrefKeys.DISABLE_SUBCHANNEL_NOTIFY)
    val disableNotifyBubble: Boolean get() = bool(PrefKeys.DISABLE_NOTIFY_BUBBLE)
    val disableInteractivePopup: Boolean get() = bool(PrefKeys.DISABLE_INTERACTIVE_POPUP)
    val useSystemGallery: Boolean get() = bool(PrefKeys.USE_SYSTEM_GALLERY)

    // ===== 社交与功能 =====
    val showQZoneFeed: Boolean get() = bool(PrefKeys.SHOW_QZONE_FEED)
    val atListReorder: Boolean get() = bool(PrefKeys.AT_LIST_REORDER)
    val disableSwipeVibrate: Boolean get() = bool(PrefKeys.DISABLE_SWIPE_VIBRATE)
    val forwardNoLimit: Boolean get() = bool(PrefKeys.FORWARD_NO_LIMIT)
    val msgPlusOne: Boolean get() = bool(PrefKeys.MSG_PLUS_ONE)
    val removeReactionLimit: Boolean get() = bool(PrefKeys.REMOVE_REACTION_LIMIT)
    val tamperSteps: Int? get() = str(PrefKeys.TAMPER_STEPS).toIntOrNull()
    val miniappToLink: Boolean get() = bool(PrefKeys.MINIAPP_TO_LINK)
    val cardToText: Boolean get() = bool(PrefKeys.CARD_TO_TEXT)

    // ===== 模块 =====
    val autoPull: Boolean get() = bool(PrefKeys.AUTO_PULL, true)
    val autoPullUrl: String get() = str(
        PrefKeys.AUTO_PULL_URL,
        "https://raw.githubusercontent.com/NEETSama114/NekoQQ/main/app/src/main/assets/hook_config.json"
    )
}
