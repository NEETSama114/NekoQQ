package neko.qq.util

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import neko.qq.constants.HookPoint

/**
 * 批量 hook 工具：遍历候选点位，命中即用，未命中自动跳过。
 * 用于「不关心具体版本、命中任一即可」的禁用/篡改类功能，减少样板代码。
 *
 * 候选点位优先取 [HookOverrides] 中「自动拉取」下发的覆盖值，其次用传入的默认值。
 */
object Hooker {

    /** 在方法执行「前」挂钩（常用于拦截/短路返回值） */
    fun before(
        classLoader: ClassLoader,
        tag: String,
        featureKey: String,
        defaults: List<HookPoint>,
        block: (XC_MethodHook.MethodHookParam) -> Unit
    ) = hook(classLoader, tag, featureKey, defaults, true, block)

    /** 在方法执行「后」挂钩（常用于改返回值/改视图） */
    fun after(
        classLoader: ClassLoader,
        tag: String,
        featureKey: String,
        defaults: List<HookPoint>,
        block: (XC_MethodHook.MethodHookParam) -> Unit
    ) = hook(classLoader, tag, featureKey, defaults, false, block)

    private fun hook(
        classLoader: ClassLoader,
        tag: String,
        featureKey: String,
        defaults: List<HookPoint>,
        isBefore: Boolean,
        block: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        val candidates = HookOverrides.get(featureKey) ?: defaults
        for (hp in candidates) {
            try {
                val clazz = classLoader.loadClass(hp.clazz)
                val paramClasses = hp.paramTypes.map { classLoader.loadClass(it) }.toTypedArray()
                XposedHelpers.findAndHookMethod(
                    clazz, hp.method, *paramClasses,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (isBefore) runCatching { block(param) }
                                .onFailure { XposedBridge.log("[$tag] before err: ${it.message}") }
                        }

                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (!isBefore) runCatching { block(param) }
                                .onFailure { XposedBridge.log("[$tag] after err: ${it.message}") }
                        }
                    }
                )
                XposedBridge.log("[$tag] 已 hook: ${hp.clazz}.${hp.method}")
            } catch (_: Throwable) {
                // 候选未命中，尝试下一个
            }
        }
    }
}
