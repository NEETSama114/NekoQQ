package neko.qq.constants

/**
 * QQNT 内部类 / 方法签名集中配置。
 *
 * ⚠️ 重要：QQ 的每个版本都会混淆并重命名内部类，下面的命名是「按历史版本整理的候选值」，
 * 不保证对你当前安装的 QQ 版本 100% 命中。请结合 README 中的「探针日志」方法，
 * 用 logcat -s NekoQQ 观察本模块打印的命中情况，把真实类名填到这里即可。
 *
 * 设计原则：所有 hook 点位都做成「候选列表 + 自动尝试」，命中任意一个即用，
 * 全部未命中则安全跳过（不会让 QQ 崩溃）。
 */
object QQConfig {

    /** QQ 包名（QQNT 与旧版一致） */
    const val PACKAGE_QQ = "com.tencent.mobileqq"

    /**
     * 设置页 Activity 候选类。
     * 注入「Neko设置」入口时，会按列表顺序尝试 hook 这些类的 onCreate，
     * 命中第一个存在的即可。视图遍历注入对原生设置列表兼容性好。
     */
    val SETTINGS_ACTIVITY_CANDIDATES = listOf(
        "com.tencent.mobileqq.activity.SettingActivity",
        "com.tencent.mobileqq.activity.QQSettingSettingActivity",
        "com.tencent.mobileqq.settings.fragment.SettingFragment",
        "com.tencent.qqnt.settings.MainSettingsActivity",
        "com.tencent.qqnt.kernel.settings.NTSettingActivity"
    )

    /** 设置列表中「账号与安全」的文案（用于定位插入位置） */
    const val ACCOUNT_SECURITY_TEXT = "账号与安全"

    /**
     * 防撤回：候选的「撤回/消息撤回处理」相关类与方法。
     * 真实逻辑应在「QQ 收到撤回信令、准备从消息列表移除该条消息」的点上拦截。
     */
    val ANTI_RECALL_CANDIDATES = listOf(
        HookPoint(
            clazz = "com.tencent.mobileqq.aio.msglist.AIOMsgItem",
            method = "onMsgRevoked",
            paramTypes = emptyArray()
        ),
        HookPoint(
            clazz = "com.tencent.mobileqq.app.MessageHandler",
            method = "handleRecall",
            paramTypes = arrayOf("com.tencent.qphone.base.remote.FromServiceMsg")
        ),
        HookPoint(
            clazz = "com.tencent.qqnt.kernel.nativeinterface.MessageService",
            method = "onMsgRecall",
            paramTypes = emptyArray()
        )
    )

    /**
     * 高级群管：候选的「群成员点击 / 打开成员资料」处理点。
     * 命中后在原逻辑之前弹出 Neko 操作菜单（禁言 / 封禁 / 踢出）。
     */
    val GROUP_MEMBER_CLICK_CANDIDATES = listOf(
        HookPoint(
            clazz = "com.tencent.mobileqq.troop.membercard.api.ITroopMemberCardApi\$Impl",
            method = "openTroopMemberCard",
            paramTypes = emptyArray()
        ),
        HookPoint(
            clazz = "com.tencent.mobileqq.adapter.TroopMemberListAdapter",
            method = "onItemClick",
            paramTypes = emptyArray()
        ),
        HookPoint(
            clazz = "com.tencent.qqnt.aio.adapter.api.ITroopApi\$Impl",
            method = "openMemberInfo",
            paramTypes = emptyArray()
        )
    )

    /**
     * 群管操作（禁言 / 封禁 / 踢出）对应的 QQ 内部调用点。
     * 这些是需要你按版本校准的「执行动作」方法，逻辑框架已写好，
     * 真正下发指令时调用这列候选方法。
     */
    val GROUP_ACTION_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.troop.utils.TroopUtils", "setTroopMemberMute", emptyArray()),
        HookPoint("com.tencent.mobileqq.troop.handler.TroopMngHandler", "kickMember", emptyArray()),
        HookPoint("com.tencent.mobileqq.troop.handler.TroopMngHandler", "banMember", emptyArray())
    )

    /**
     * 表情面板：聊天输入栏的 EditText 候选 hint 关键词。
     * 通过视图遍历，找到输入栏 EditText，在它前面插入表情按钮。
     */
    val CHAT_INPUT_HINT_KEYWORDS = listOf("说点什么", "发消息", "输入", "发条消息")

    // ============================================================
    //  下方为各增强功能的 hook 候选点位（均为「按历史版本整理的候选值」，
    //  需结合 README 的探针日志按你当前 QQ 版本校准；命中任一即用）。
    // ============================================================

    // ---- 显示与界面 ----
    val PREVIEW_FONT_SIZE_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.text.QQText", "getPaint", emptyArray())
    )
    val FORCE_TABLET_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.activity.SplashActivity", "isTablet", emptyArray())
    )
    val FORCE_PHONE_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.activity.SplashActivity", "isTablet", emptyArray())
    )
    val MODIFY_DPI_CANDIDATES = listOf(
        HookPoint("android.util.DisplayMetrics", "setTo", emptyArray())
    )
    val FORCE_DEFAULT_FONT_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.app.FontManager", "getFontPath", emptyArray())
    )
    val SPLASH_COLUMNS_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.emosm.favemoji.FavEmoConstants", "getColumnCount", emptyArray())
    )
    val RANDOM_BUBBLE_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.bubble.BubbleManager", "getBubble", emptyArray())
    )
    val TRANSPARENT_AVATAR_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.util.FaceDrawable", "getFaceDrawable", emptyArray())
    )

    // ---- 隐私与去广告 ----
    val DISABLE_QQSHOW_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.richstatus.RichStatItem", "isShow", emptyArray())
    )
    val HIDE_QZONE_AD_CANDIDATES = listOf(
        HookPoint("com.tencent.component.net.html.parser.HtmlParser", "parseAds", emptyArray())
    )
    val HIDE_EMOJI_REACTION_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItem", "showReaction", emptyArray())
    )
    val REMOVE_MSG_REACTION_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.aio.api.IReactionApi", "supportReaction", emptyArray())
    )
    val REMOVE_TOP_BANNER_AD_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.activity.recent.RecentAdapter", "getBanner", emptyArray())
    )
    val DISABLE_CRASH_REPORT_CANDIDATES = listOf(
        HookPoint("com.tencent.bugly.crashreport.CrashReport", "postCatchedException", emptyArray())
    )
    val DISABLE_SUBCHANNEL_NOTIFY_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.app.BaseMessageHandler", "notifySubChannel", emptyArray())
    )
    val DISABLE_NOTIFY_BUBBLE_CANDIDATES = listOf(
        HookPoint("android.app.NotificationManager", "notifyAsUser", emptyArray())
    )
    val DISABLE_INTERACTIVE_POPUP_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.activity.QQBrowserActivity", "showInteractivePopup", emptyArray())
    )
    val USE_SYSTEM_GALLERY_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.activity.photo.PhotoListPanel", "openQQAlbum", emptyArray())
    )

    // ---- 社交与功能 ----
    val SHOW_QZONE_FEED_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.qzone.QZoneFeedFragment", "loadFeed", emptyArray())
    )
    val AT_LIST_REORDER_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.troop.adapter.AtListAdapter", "sort", emptyArray())
    )
    val DISABLE_SWIPE_VIBRATE_CANDIDATES = listOf(
        HookPoint("android.os.Vibrator", "vibrate", arrayOf("long"))
    )
    val FORWARD_NO_LIMIT_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.activity.ForwardRecentActivity", "getMaxForwardCount", emptyArray())
    )
    val MSG_PLUS_ONE_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.activity.recent.RecentAdapter", "getUnreadCount", emptyArray())
    )
    val REMOVE_REACTION_LIMIT_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.aio.api.IReactionApi", "getReactionLimit", emptyArray())
    )
    val TAMPER_STEPS_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.health.step.StepDataManager", "uploadSteps", emptyArray())
    )
    val MINIAPP_TO_LINK_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.mini.api.IMiniAppService", "shareMiniApp", emptyArray())
    )
    val CARD_TO_TEXT_CANDIDATES = listOf(
        HookPoint("com.tencent.mobileqq.structmsg.StructMsg", "getContent", emptyArray())
    )

    /** 自动拉取：默认仓库内 hook 配置地址（可被设置项覆盖） */
    const val DEFAULT_HOOK_CONFIG_URL =
        "https://raw.githubusercontent.com/NEETSama114/NekoQQ/main/app/src/main/assets/hook_config.json"
}

/** 一个简单的 hook 点位描述 */
data class HookPoint(
    val clazz: String,
    val method: String,
    val paramTypes: Array<out String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HookPoint
        return clazz == other.clazz && method == other.method && paramTypes.contentEquals(other.paramTypes)
    }

    override fun hashCode(): Int {
        var result = clazz.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + paramTypes.contentHashCode()
        return result
    }
}
