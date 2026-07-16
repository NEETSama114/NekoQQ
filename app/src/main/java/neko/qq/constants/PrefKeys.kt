package neko.qq.constants

/** 与 res/xml/neko_prefs.xml 中的 android:key 一一对应 */
object PrefKeys {
    const val ANTI_RECALL = "anti_recall"
    const val RECALL_BANNER = "recall_banner"
    const val RECALL_KEEP = "recall_keep"
    const val RECALL_TEXT = "recall_text"

    const val GROUP_MGR = "group_mgr"
    const val GROUP_CONFIRM = "group_confirm"
    const val GROUP_LOG = "group_log"
    const val GROUP_MUTE_MIN = "group_mute_min"

    const val EMOJI_PANEL = "emoji_panel"
    const val EMOJI_KAOMOJI = "emoji_kaomoji"
    const val EMOJI_RECENT = "emoji_recent"

    // ===== 显示与界面 =====
    const val PREVIEW_FONT_SIZE = "preview_font_size"   // 篡改预览字体大小
    const val FORCE_TABLET = "force_tablet"             // 强制平板模式
    const val FORCE_PHONE = "force_phone"               // 强制手机模式
    const val MODIFY_DPI = "modify_dpi"                 // 修改 DPI
    const val FORCE_DEFAULT_FONT = "force_default_font" // 强制使用默认字体
    const val SPLASH_COLUMNS = "splash_columns"         // 自定义启动图列数
    const val RANDOM_BUBBLE = "random_bubble"           // 随机气泡
    const val CUSTOM_EMOJI_PANEL = "custom_emoji_panel" // 自定义表情面板

    // ===== 隐私与去广告 =====
    const val DISABLE_QQSHOW = "disable_qqshow"             // 禁用QQ秀功能
    const val HIDE_QZONE_AD = "hide_qzone_ad"               // 隐藏QQ空间广告
    const val HIDE_EMOJI_REACTION = "hide_emoji_reaction"   // 隐藏表情表态
    const val REMOVE_MSG_REACTION = "remove_msg_reaction"   // 移除消息表态
    const val REMOVE_TOP_BANNER_AD = "remove_top_banner_ad" // 移除消息列表顶栏横幅广告
    const val DISABLE_CRASH_REPORT = "disable_crash_report" // 禁用崩溃日志上报
    const val DISABLE_SUBCHANNEL_NOTIFY = "disable_subchannel_notify" // 禁用子渠道发送通知
    const val DISABLE_NOTIFY_BUBBLE = "disable_notify_bubble"         // 禁用通知气泡
    const val DISABLE_INTERACTIVE_POPUP = "disable_interactive_popup" // 禁用特定消息触发的交互式弹窗
    const val USE_SYSTEM_GALLERY = "use_system_gallery"     // 可选使用系统相册
    const val TRANSPARENT_AVATAR = "transparent_avatar"     // 允许上传透明头像和表情

    // ===== 社交与功能 =====
    const val SHOW_QZONE_FEED = "show_qzone_feed"       // 显示空间动态
    const val AT_LIST_REORDER = "at_list_reorder"       // @列表重新排序
    const val DISABLE_SWIPE_VIBRATE = "disable_swipe_vibrate" // 禁用左滑回复震动
    const val FORWARD_NO_LIMIT = "forward_no_limit"     // 去除转发9名联系人限制
    const val MSG_PLUS_ONE = "msg_plus_one"             // 消息+1 Plus
    const val REMOVE_REACTION_LIMIT = "remove_reaction_limit" // 移除表情回应限制
    const val TAMPER_STEPS = "tamper_steps"             // 篡改上传运动步数
    const val MINIAPP_TO_LINK = "miniapp_to_link"       // 小程序分享转链接（接收）
    const val CARD_TO_TEXT = "card_to_text"             // 卡片消息文本化

    // ===== 模块 =====
    const val AUTO_PULL = "auto_pull"                   // 自动拉取最新 hook 配置
    const val AUTO_PULL_URL = "auto_pull_url"           // 自动拉取地址

    /** 共享偏好文件名（模块进程写入，QQ 进程经 createPackageContext 读取） */
    const val PREFS_NAME = "neko_qq_prefs"
}
