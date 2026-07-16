package neko.qq.features

import de.robv.android.xposed.XposedBridge
import neko.qq.constants.QQConfig
import neko.qq.util.Hooker
import neko.qq.util.Prefs

/**
 * 社交与功能相关增强：
 *  - 显示空间动态
 *  - @ 列表重新排序
 *  - 禁用左滑回复震动
 *  - 去除转发人数限制
 *  - 消息 +1 Plus
 *  - 移除表情回应限制
 *  - 篡改上传运动步数
 *  - 小程序分享转链接（接收）
 *  - 卡片消息文本化
 */
object Social {

    fun hook(classLoader: ClassLoader, lpparam: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam) {

        // 显示空间动态（入口启用，留校准点）
        Hooker.before(classLoader, "NekoQQ:qzone_feed", "show_qzone_feed", QQConfig.SHOW_QZONE_FEED_CANDIDATES) {
            if (Prefs.showQZoneFeed) {
                XposedBridge.log("[NekoQQ] 显示空间动态：已启用（请校准数据加载点）")
                // TODO(版本校准): 确保对应入口直接拉取并展示动态列表
            }
        }

        // @ 列表重新排序：对结果列表按内容稳定重排
        Hooker.after(classLoader, "NekoQQ:at_reorder", "at_list_reorder", QQConfig.AT_LIST_REORDER_CANDIDATES) {
            if (!Prefs.atListReorder) return@after
            val list = it.result as? MutableList<*> ?: return@after
            runCatching { list.sortBy { item -> item.toString() } }
            XposedBridge.log("[NekoQQ] @ 列表已重排（${list.size} 项）")
        }

        // 禁用左滑回复震动：把震动参数清零
        Hooker.before(classLoader, "NekoQQ:swipe_vib", "disable_swipe_vibrate", QQConfig.DISABLE_SWIPE_VIBRATE_CANDIDATES) {
            if (!Prefs.disableSwipeVibrate) return@before
            when (it.args.size) {
                1 -> when (val a = it.args[0]) {
                    is Long -> it.args[0] = 0L
                    is LongArray -> it.args[0] = LongArray(0)
                }
                2 -> {
                    val a = it.args[0]
                    if (a is LongArray) it.args[0] = LongArray(0)
                }
            }
        }

        // 去除转发人数限制
        Hooker.after(classLoader, "NekoQQ:forward", "forward_no_limit", QQConfig.FORWARD_NO_LIMIT_CANDIDATES) {
            if (Prefs.forwardNoLimit && it.result is Int) it.setResult(999)
        }

        // 消息 +1 Plus：未读角标 +1
        Hooker.after(classLoader, "NekoQQ:plus_one", "msg_plus_one", QQConfig.MSG_PLUS_ONE_CANDIDATES) {
            if (Prefs.msgPlusOne && it.result is Int) it.setResult((it.result as Int) + 1)
        }

        // 移除表情回应限制
        Hooker.after(classLoader, "NekoQQ:react_limit", "remove_reaction_limit", QQConfig.REMOVE_REACTION_LIMIT_CANDIDATES) {
            if (Prefs.removeReactionLimit && it.result is Int) it.setResult(9999)
        }

        // 篡改上传运动步数
        Hooker.before(classLoader, "NekoQQ:steps", "tamper_steps", QQConfig.TAMPER_STEPS_CANDIDATES) {
            val target = Prefs.tamperSteps ?: return@before
            // 把整型 / 字符串型步数参数替换为目标值
            it.args.forEachIndexed { i, a ->
                when (a) {
                    is Int -> it.args[i] = target
                    is Long -> it.args[i] = target.toLong()
                    is String -> it.args[i] = target.toString()
                }
            }
            XposedBridge.log("[NekoQQ] 篡改步数 -> $target")
        }

        // 小程序分享转链接（接收）
        Hooker.before(classLoader, "NekoQQ:miniapp", "miniapp_to_link", QQConfig.MINIAPP_TO_LINK_CANDIDATES) {
            if (Prefs.miniappToLink) {
                XposedBridge.log("[NekoQQ] 小程序转链接：已启用（请校准提取 webUrl 的逻辑）")
                // TODO(版本校准): 从小程序分享参数中提取 webUrl，替换为链接分享
            }
        }

        // 卡片消息文本化：把返回内容替换为纯文本
        Hooker.after(classLoader, "NekoQQ:card_text", "card_to_text", QQConfig.CARD_TO_TEXT_CANDIDATES) {
            if (!Prefs.cardToText) return@after
            val text = it.result as? String ?: return@after
            val plain = text.replace(Regex("<[^>]+>"), "").replace("\\n", "\n").trim()
            it.setResult(plain)
        }
    }
}
