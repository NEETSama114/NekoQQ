package neko.qq.features

import de.robv.android.xposed.XposedBridge
import neko.qq.constants.QQConfig
import neko.qq.util.Hooker
import neko.qq.util.Prefs

/**
 * 隐私与去广告相关增强：
 *  - 禁用 QQ 秀
 *  - 隐藏 QQ 空间广告
 *  - 隐藏 / 移除表情表态
 *  - 移除消息列表顶栏横幅广告
 *  - 禁用崩溃日志上报
 *  - 禁用子渠道通知 / 通知气泡
 *  - 禁用特定消息触发的交互式弹窗
 *  - 可选使用系统相册
 *
 * 说明：返回型方法用 setResult 短路即可禁用；少数 void 方法（如崩溃上报、通知）
 * 真正「拦截」需要抛异常或 hook 上层调用方，已在对应处留 TODO，按版本校准。
 */
object Privacy {

    fun hook(classLoader: ClassLoader, lpparam: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam) {

        // 禁用 QQ 秀（假设返回布尔）
        Hooker.before(classLoader, "NekoQQ:qqshow", "disable_qqshow", QQConfig.DISABLE_QQSHOW_CANDIDATES) {
            if (Prefs.disableQQShow) it.setResult(false)
        }

        // 隐藏 QQ 空间广告（假设返回广告列表）
        Hooker.before(classLoader, "NekoQQ:qzone_ad", "hide_qzone_ad", QQConfig.HIDE_QZONE_AD_CANDIDATES) {
            if (Prefs.hideQZoneAd) it.setResult(emptyList<Any>())
        }

        // 隐藏表情表态（void，留接入点）
        Hooker.before(classLoader, "NekoQQ:hide_reaction", "hide_emoji_reaction", QQConfig.HIDE_EMOJI_REACTION_CANDIDATES) {
            if (Prefs.hideEmojiReaction) {
                XposedBridge.log("[NekoQQ] 隐藏表情表态：已启用（void 方法，请校准拦截方式）")
                // TODO(版本校准): showReaction 为 void，需在 before 抛异常或 hook 上层调用方才能跳过
            }
        }

        // 移除消息表态入口（假设返回布尔）
        Hooker.before(classLoader, "NekoQQ:rm_reaction", "remove_msg_reaction", QQConfig.REMOVE_MSG_REACTION_CANDIDATES) {
            if (Prefs.removeMsgReaction) it.setResult(false)
        }

        // 移除顶栏横幅广告（假设返回 banner 对象）
        Hooker.before(classLoader, "NekoQQ:top_ad", "remove_top_banner_ad", QQConfig.REMOVE_TOP_BANNER_AD_CANDIDATES) {
            if (Prefs.removeTopBannerAd) it.setResult(null)
        }

        // 禁用崩溃日志上报（void，抛异常可拦截，按版本评估风险）
        Hooker.before(classLoader, "NekoQQ:crash", "disable_crash_report", QQConfig.DISABLE_CRASH_REPORT_CANDIDATES) {
            if (Prefs.disableCrashReport) {
                XposedBridge.log("[NekoQQ] 禁用崩溃上报：已启用")
                // TODO(版本校准): 如需彻底拦截，可 it.throwable = Throwable()（谨慎，可能被上层捕获）
            }
        }

        // 禁用子渠道通知（void）
        Hooker.before(classLoader, "NekoQQ:sub_notify", "disable_subchannel_notify", QQConfig.DISABLE_SUBCHANNEL_NOTIFY_CANDIDATES) {
            if (Prefs.disableSubchannelNotify) {
                XposedBridge.log("[NekoQQ] 禁用子渠道通知：已启用（请校准拦截方式）")
                // TODO(版本校准): 同上，void 方法需抛异常或 hook 上层
            }
        }

        // 禁用通知气泡（void）
        Hooker.before(classLoader, "NekoQQ:notify_bubble", "disable_notify_bubble", QQConfig.DISABLE_NOTIFY_BUBBLE_CANDIDATES) {
            if (Prefs.disableNotifyBubble) {
                XposedBridge.log("[NekoQQ] 禁用通知气泡：已启用（请校准拦截方式）")
                // TODO(版本校准): 同上
            }
        }

        // 禁用交互式弹窗（void）
        Hooker.before(classLoader, "NekoQQ:popup", "disable_interactive_popup", QQConfig.DISABLE_INTERACTIVE_POPUP_CANDIDATES) {
            if (Prefs.disableInteractivePopup) {
                XposedBridge.log("[NekoQQ] 禁用交互式弹窗：已启用（请校准拦截方式）")
                // TODO(版本校准): 同上
            }
        }

        // 使用系统相册（void，留接入点跳转系统 picker）
        Hooker.before(classLoader, "NekoQQ:gallery", "use_system_gallery", QQConfig.USE_SYSTEM_GALLERY_CANDIDATES) {
            if (Prefs.useSystemGallery) {
                XposedBridge.log("[NekoQQ] 使用系统相册：已启用（请校准跳转逻辑）")
                // TODO(版本校准): 改为 startActivity(Intent.ACTION_PICK) 调用系统相册
            }
        }
    }
}
