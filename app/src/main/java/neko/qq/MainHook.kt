package neko.qq

import android.app.Application
import android.content.Context

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

import java.util.concurrent.atomic.AtomicBoolean

import neko.qq.features.AutoPull
import neko.qq.features.AntiRecall
import neko.qq.features.GroupManager
import neko.qq.features.EmojiPanel
import neko.qq.features.Display
import neko.qq.features.Privacy
import neko.qq.features.Social

import neko.qq.hook.InjectSettingsEntry
import neko.qq.util.Contexts

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "NekoQQ-MainHook"
        private const val QQ_PACKAGE = "com.tencent.mobileqq"

        /*
         * 每个进程都有独立的 ClassLoader 和静态变量。
         * 这个标志用于防止同一个进程内重复初始化。
         */
        private val initialized = AtomicBoolean(false)
    }

    override fun handleLoadPackage(
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        if (lpparam.packageName != QQ_PACKAGE) {
            return
        }

        XposedBridge.log(
            "$TAG: QQ进程加载: ${lpparam.processName}"
        )

        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {

                    override fun afterHookedMethod(
                        param: MethodHookParam
                    ) {
                        val app = param.thisObject as? Application
                            ?: return

                        /*
                         * 防止 Application.attach 被重复 Hook 后
                         * 导致同一进程重复初始化。
                         */
                        if (!initialized.compareAndSet(false, true)) {
                            XposedBridge.log(
                                "$TAG: 当前进程已经初始化，跳过: ${lpparam.processName}"
                            )
                            return
                        }

                        try {
                            Contexts.initialize(app)

                            XposedBridge.log(
                                "$TAG: Application.attach完成，进程=${lpparam.processName}"
                            )

                            initializeHooks(
                                app,
                                lpparam
                            )

                        } catch (t: Throwable) {
                            /*
                             * 初始化失败时允许后续重新尝试。
                             */
                            initialized.set(false)

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
        val isMainProcess =
            lpparam.processName == QQ_PACKAGE

        XposedBridge.log(
            "$TAG: 开始初始化模块，主进程=$isMainProcess"
        )

        /*
         * 自动拉取配置。
         *
         * 目前保留在所有进程初始化，以避免其他功能
         * 在独立进程中读取不到配置。
         */
        runCatching {
            AutoPull.init()
        }.onFailure {
            XposedBridge.log(
                "$TAG: AutoPull.init失败: ${it.stackTraceToString()}"
            )
        }

        /*
         * 设置页入口只需要在 QQ 主进程运行。
         *
         * 这是本次最重要的修改：
         * 避免 MSF、pushservice 等后台进程重复注册
         * ActivityLifecycleCallbacks 和扫描设置页面。
         */
        if (isMainProcess) {
            runCatching {
                InjectSettingsEntry.hook(
                    app,
                    lpparam
                )
            }.onFailure {
                XposedBridge.log(
                    "$TAG: InjectSettingsEntry失败: ${it.stackTraceToString()}"
                )
            }
        } else {
            XposedBridge.log(
                "$TAG: 非主进程，跳过设置页入口: ${lpparam.processName}"
            )
        }

        // 防撤回
        runCatching {
            AntiRecall.hook(
                loader,
                lpparam
            )
        }.onFailure {
            XposedBridge.log(
                "$TAG: AntiRecall失败: ${it.stackTraceToString()}"
            )
        }

        // 高级群管
        runCatching {
            GroupManager.hook(
                loader,
                lpparam
            )
        }.onFailure {
            XposedBridge.log(
                "$TAG: GroupManager失败: ${it.stackTraceToString()}"
            )
        }

        // 表情面板
        runCatching {
            EmojiPanel.hook(
                loader,
                lpparam
            )
        }.onFailure {
            XposedBridge.log(
                "$TAG: EmojiPanel失败: ${it.stackTraceToString()}"
            )
        }

        // 显示相关增强
        runCatching {
            Display.hook(
                loader,
                lpparam
            )
        }.onFailure {
            XposedBridge.log(
                "$TAG: Display失败: ${it.stackTraceToString()}"
            )
        }

        // 隐私功能
        runCatching {
            Privacy.hook(
                loader,
                lpparam
            )
        }.onFailure {
            XposedBridge.log(
                "$TAG: Privacy失败: ${it.stackTraceToString()}"
            )
        }

        // 社交功能
        runCatching {
            Social.hook(
                loader,
                lpparam
            )
        }.onFailure {
            XposedBridge.log(
                "$TAG: Social失败: ${it.stackTraceToString()}"
            )
        }

        /*
         * 远程配置刷新建议只在主进程进行，
         * 防止 QQ 多个进程同时发起网络请求。
         */
        if (isMainProcess) {
            runCatching {
                AutoPull.refresh()
            }.onFailure {
                XposedBridge.log(
                    "$TAG: AutoPull.refresh失败: ${it.stackTraceToString()}"
                )
            }
        }

        XposedBridge.log(
            "$TAG: 模块初始化完成，进程=${lpparam.processName}"
        )
    }
}
