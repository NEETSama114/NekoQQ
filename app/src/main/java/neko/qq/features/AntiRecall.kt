package neko.qq.features

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import neko.qq.constants.QQConfig
import neko.qq.util.Prefs
import neko.qq.util.Toasts

/**
 * 防撤回。
 *
 * 思路：在「QQ 收到撤回信令、准备把消息从列表里移除」的候选方法上挂钩。
 *   - 命中后，按用户配置弹出提示横幅（recallText），并保留/标记原消息。
 *   - 真正「保住原气泡」需要从该方法的参数里取出消息对象并阻止默认移除逻辑，
 *     这一步依赖具体版本的消息类结构，请在 QQConfig.ANTI_RECALL_CANDIDATES
 *     校准命中后，在 onRecall() 里补充参数解析（见下方 TODO）。
 *
 * 当前实现：检测撤回事件 + 提示横幅 + 日志，确保模块本身可运行、可验证；
 * 原消息保留逻辑留好接入点。
 */
object AntiRecall {

    fun hook(classLoader: ClassLoader, lpparam: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam) {
        if (!Prefs.antiRecallEnabled) return

        for (hp in QQConfig.ANTI_RECALL_CANDIDATES) {
            try {
                val clazz = classLoader.loadClass(hp.clazz)
                val paramClasses = hp.paramTypes.map { classLoader.loadClass(it) }.toTypedArray()
                XposedHelpers.findAndHookMethod(
                    clazz, hp.method, *paramClasses,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            // TODO(版本校准): 在此从 param.args 解析出发送者 / 消息内容 / 消息 id
                            // 例如：val msg = param.args[0] as? SomeMsgClass
                            onRecall(param)
                        }
                    }
                )
                XposedBridge.log("[NekoQQ] 防撤回 已 hook: ${hp.clazz}.${hp.method}")
            } catch (_: Throwable) {
                // 候选未命中，尝试下一个
            }
        }
    }

    /**
     * 收到撤回事件时的处理。
     * @param param 命中方法的上下文，便于后续解析消息对象
     */
    private fun onRecall(param: XC_MethodHook.MethodHookParam) {
        if (!Prefs.antiRecallEnabled) return

        // 提示横幅（跨版本稳定可用）
        if (Prefs.recallShowBanner) {
            Toasts.show("🐱 ${Prefs.recallText}")
        }

        // 保留原文：在成功解析出消息对象后，可在此处把该消息标记为「已撤回-已保留」，
        // 并阻止 param 继续执行默认移除逻辑（param.setResult(null) / 或修改 args）。
        if (Prefs.recallKeepContent) {
            XposedBridge.log("[NekoQQ] 防撤回：已拦截一次撤回（保留原文待接入消息对象）")
            // TODO(版本校准): param.setResult(null)  // 阻止默认撤回
        }
    }
}
