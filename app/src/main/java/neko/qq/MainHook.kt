package neko.qq

// ❌ 原来错误的引入：import de.robv.android.xposed.IXposedMod
// ✅ 正确的引入：
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
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

/**
 * NekoQQ 模块入口（被 assets/xposed_init 引用）。
 * 仅在 QQ 进程内生效，按顺序挂载：自动拉取配置 → Neko设置入口 → 全部功能。
 */
// ❌ 原来错误的继承：class MainHook : IXposedMod
// ✅ 正确的继承：必须实现 IXposedHookLoadPackage 接口
class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != QQConfig.PACKAGE_QQ) return

        // 提前拿到 QQ 的 Application Context，供 Prefs / Toasts 等工具使用
        runCatching { Contexts.appContext() }.onFailure {
            XposedBridge.log("[NekoQQ] 获取 Context 失败: ${it.message}")
        }

        XposedBridge.log("[NekoQQ] 注入 QQ 进程 (${lpparam.processName})")

        // 0) 自动拉取：先同步应用本地缓存，使上次拉取的 hook 覆盖立即生效
        AutoPull.init()

        // 1) Neko设置入口（位于「账号与安全」上方）
        InjectSettingsEntry.hook(lpparam.classLoader, lpparam)

        // 2) 消息 / 群管 / 表情 三大基础功能
        AntiRecall.hook(lpparam.classLoader, lpparam)
        GroupManager.hook(lpparam.classLoader, lpparam)
        EmojiPanel.hook(lpparam.classLoader, lpparam)

        // 3) 显示与界面 / 隐私去广告 / 社交功能 增强
        Display.hook(lpparam.classLoader, lpparam)
        Privacy.hook(lpparam.classLoader, lpparam)
        Social.hook(lpparam.classLoader, lpparam)

        // 4) 后台刷新最新 hook 配置（下次启动生效）
        AutoPull.refresh()
    }
}
