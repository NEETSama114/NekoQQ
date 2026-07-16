package neko.qq.features

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import neko.qq.constants.QQConfig
import neko.qq.util.Contexts
import neko.qq.util.Prefs
import neko.qq.util.Toasts

/**
 * 高级群管。
 *
 * 在群聊里点击成员头像 / 成员项时，弹出 Neko 操作菜单：禁言、封禁、踢出、设为管理员。
 *
 * 流程：
 *   1) hook 候选的「打开成员资料 / 列表项点击」方法（GROUP_MEMBER_CLICK_CANDIDATES）。
 *   2) 命中后展示我们的操作菜单，并从参数/thisObject 解析出成员 uin（版本相关，见 TODO）。
 *   3) 执行动作时调用候选的群管下发方法（GROUP_ACTION_CANDIDATES）。
 *
 * 说明：点击头像「呼出菜单而非直接打开资料卡」需要阻止默认跳转，详见 TODO。
 */
object GroupManager {

    fun hook(classLoader: ClassLoader, lpparam: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam) {
        if (!Prefs.groupMgrEnabled) return

        for (hp in QQConfig.GROUP_MEMBER_CLICK_CANDIDATES) {
            try {
                val clazz = classLoader.loadClass(hp.clazz)
                val paramClasses = hp.paramTypes.map { classLoader.loadClass(it) }.toTypedArray()
                XposedHelpers.findAndHookMethod(
                    clazz, hp.method, *paramClasses,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            // TODO(版本校准): 从 param.args / param.thisObject 解析成员 uin 与群号
                            val memberUin = extractMemberUin(param) ?: return
                            val groupUin = extractGroupUin(param) ?: return
                            // 展示菜单；如需「只弹菜单不打开资料卡」，可在此 param.setResult(null)
                            showActionSheet(memberUin, groupUin)
                        }
                    }
                )
                XposedBridge.log("[NekoQQ] 高级群管 已 hook: ${hp.clazz}.${hp.method}")
            } catch (_: Throwable) {
                // 候选未命中，尝试下一个
            }
        }
    }

    /** 弹出成员操作菜单 */
    private fun showActionSheet(memberUin: String, groupUin: String) {
        Handler(Looper.getMainLooper()).post {
            val ctx = Contexts.appContext()
            val items = arrayOf("🔇 禁言 ${Prefs.groupMuteMinutes} 分钟", "⛔ 封禁该成员", "🚪 移出本群", "⭐ 设为管理员", "👤 查看资料")
            AlertDialog.Builder(ctx, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle("Neko 高级群管")
                .setItems(items) { _: DialogInterface, which: Int ->
                    when (which) {
                        0 -> performAction(groupUin, memberUin, "禁言")
                        1 -> performAction(groupUin, memberUin, "封禁")
                        2 -> performAction(groupUin, memberUin, "踢出")
                        3 -> performAction(groupUin, memberUin, "管理员")
                        4 -> { /* 交给 QQ 默认资料卡 */ }
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    /** 执行群管动作 */
    private fun performAction(groupUin: String, memberUin: String, action: String) {
        val doIt: () -> Unit = {
            // 危险操作二次确认
            if (Prefs.groupConfirm && action in listOf("封禁", "踢出")) {
                Handler(Looper.getMainLooper()).post {
                    AlertDialog.Builder(Contexts.appContext(), android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                        .setTitle("确认 $action？")
                        .setMessage("将对成员 $memberUin 执行「$action」，此操作会通知群内。")
                        .setPositiveButton("确定") { _, _ -> dispatch(groupUin, memberUin, action) }
                        .setNegativeButton("再想想", null)
                        .show()
                }
            } else {
                dispatch(groupUin, memberUin, action)
            }
        }
        doIt()
    }

    /** 真正下发群管指令（调用 QQ 内部候选方法） */
    private fun dispatch(groupUin: String, memberUin: String, action: String) {
        val cl = Contexts.appContext().classLoader
        for (hp in QQConfig.GROUP_ACTION_CANDIDATES) {
            try {
                val clazz = cl.loadClass(hp.clazz)
                val m = XposedHelpers.findMethodExact(clazz, hp.method, *hp.paramTypes.map { cl.loadClass(it) }.toTypedArray())
                // TODO(版本校准): 按真实方法签名传入 groupUin / memberUin / 时长 等参数
                XposedHelpers.callMethod(clazz.newInstance(), hp.method, groupUin, memberUin)
                XposedBridge.log("[NekoQQ] 群管动作[$action] 已下发: $memberUin")
                if (Prefs.groupLog) Toasts.show("🐱 已对 $memberUin 执行：$action")
                return
            } catch (_: Throwable) {
                // 尝试下一个候选
            }
        }
        // 候选都未命中时，至少给出可感知的反馈（便于联调）
        Toasts.show("🐱 已模拟 $action：$memberUin（请校准 GROUP_ACTION_CANDIDATES）")
        XposedBridge.log("[NekoQQ] 群管动作[$action] 未找到下发方法，已模拟")
    }

    // ===== 版本相关的成员/群号解析（接入点） =====
    private fun extractMemberUin(param: XC_MethodHook.MethodHookParam): String? {
        // TODO(版本校准): 从 param.args / param.thisObject 提取成员 uin（字符串或长整型）
        return null
    }

    private fun extractGroupUin(param: XC_MethodHook.MethodHookParam): String? {
        // TODO(版本校准): 从当前群聊上下文提取群号
        return null
    }
}
