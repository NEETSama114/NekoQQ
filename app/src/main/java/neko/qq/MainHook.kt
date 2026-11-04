
package neko.qq

import android.app.Application
import android.content.Context

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

import java.util.concurrent.atomic.AtomicBoolean

import neko.qq.hook.InjectSettingsEntry

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "NekoQQ-MainHook"

        private val initialized = AtomicBoolean(false)
    }

    override fun handleLoadPackage(
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        if (lpparam.packageName != "com.tencent.mobileqq") {
            return
        }

        XposedBridge.log("$TAG: QQ进程加载")

        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application
                            ?: return

                        if (!initialized.compareAndSet(false, true)) {
                            return
                        }

                        try {
                            Contexts.initialize(app)

                            XposedBridge.log(
                                "$TAG: Application.attach完成"
                            )

                            initializeHooks(
                                app,
                                lpparam
                            )

                        } catch (t: Throwable) {
                            XposedBridge.log(
                                "$TAG: 初始化异常: ${t.stackTraceToString()}"
                            )
                        }
                    }
                }
            )

        } catch (t: Throwable) {
            XposedBridge.log(
                "$TAG: Hook Application.attach失败: ${t.stackTraceToString()}"
            )
        }
    }

    private fun initializeHooks(
        app: Application,
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        val loader = lpparam.classLoader

        XposedBridge.log("$TAG: 开始初始化模块")

        // 自动拉取
        runCatching {
            AutoPull.init()
        }.onFailure {
            XposedBridge.log("$TAG: AutoPull.init失败: $it")
        }

        // 设置页入口
        runCatching {
            InjectSettingsEntry.hook(
                app,
                lpparam
            )
        }.onFailure {
            XposedBridge.log("$TAG: InjectSettingsEntry失败: $it")
        }

        // 撤回消息
        runCatching {
            AntiRecall.hook(loader)
        }.onFailure {
            XposedBridge.log("$TAG: AntiRecall失败: $it")
        }

        // 群管理
        runCatching {
            GroupManager.hook(loader)
        }.onFailure {
            XposedBridge.log("$TAG: GroupManager失败: $it")
        }

        // 表情面板
        runCatching {
            EmojiPanel.hook(loader)
        }.onFailure {
            XposedBridge.log("$TAG: EmojiPanel失败: $it")
        }

        // 显示相关
        runCatching {
            Display.hook(loader)
        }.onFailure {
            XposedBridge.log("$TAG: Display失败: $it")
        }

        // 隐私功能
        runCatching {
            Privacy.hook(loader)
        }.onFailure {
            XposedBridge.log("$TAG: Privacy失败: $it")
        }

        // 社交功能
        runCatching {
            Social.hook(loader)
        }.onFailure {
            XposedBridge.log("$TAG: Social失败: $it")
        }

        // 刷新自动拉取
        runCatching {
            AutoPull.refresh()
        }.onFailure {
            XposedBridge.log("$TAG: AutoPull.refresh失败: $it")
        }

        XposedBridge.log("$TAG: 模块初始化完成")
    }
}
