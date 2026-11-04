
package neko.qq

import android.app.Application
import android.content.Context
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import neko.qq.constants.QQConfig
import neko.qq.features.AntiRecall
import neko.qq.features.AutoPull
import neko.qq.features.Display
import neko.qq.features.EmojiPanel
import neko.qq.features.GroupManager
import neko.qq.features.Privacy
import neko.qq.features.Social
import neko.qq.hooks.InjectSettingsEntry
import neko.qq.util.Contexts
import java.util.concurrent.atomic.AtomicBoolean

class MainHook : IXposedHookLoadPackage {

    private val initialized = AtomicBoolean(false)

    override fun handleLoadPackage(
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        if (lpparam.packageName != QQConfig.PACKAGE_QQ) return

        XposedBridge.log(
            "[NekoQQ] 等待 Application.attach: ${lpparam.processName}"
        )

        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "attach",
            Context::class.java,
            object : XC_MethodHook() {

                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!initialized.compareAndSet(false, true)) return

                    val app = param.thisObject as Application

                    runCatching {
                        Contexts.initialize(app)
                        XposedBridge.log("[NekoQQ] Application Context 初始化成功")

                        initializeHooks(lpparam)
                    }.onFailure {
                        XposedBridge.log(
                            "[NekoQQ] 初始化异常: ${it.stackTraceToString()}"
                        )
                    }
                }
            }
        )
    }

    private fun initializeHooks(
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        val loader = lpparam.classLoader

        XposedBridge.log(
            "[NekoQQ] 开始初始化功能: ${lpparam.processName}"
        )

        runCatching { AutoPull.init() }
            .onFailure {
                XposedBridge.log("[NekoQQ] AutoPull.init 失败: $it")
            }

        runCatching { InjectSettingsEntry.hook(loader, lpparam) }
            .onFailure {
                XposedBridge.log("[NekoQQ] 设置入口 Hook 失败: $it")
            }

        runCatching { AntiRecall.hook(loader, lpparam) }
        runCatching { GroupManager.hook(loader, lpparam) }
        runCatching { EmojiPanel.hook(loader, lpparam) }

        runCatching { Display.hook(loader, lpparam) }
        runCatching { Privacy.hook(loader, lpparam) }
        runCatching { Social.hook(loader, lpparam) }

        runCatching { AutoPull.refresh() }
            .onFailure {
                XposedBridge.log("[NekoQQ] AutoPull.refresh 失败: $it")
            }

        XposedBridge.log("[NekoQQ] 功能初始化流程结束")
    }
}
