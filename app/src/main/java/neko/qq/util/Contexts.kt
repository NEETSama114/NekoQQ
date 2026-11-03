package neko.qq.util

import android.content.Context
// ❌ 删除了 import android.app.ActivityThread，因为这是隐藏 API，直接 import 会导致编译失败

/**
 * 跨进程取 Context 的小工具。
 * hook 运行在 QQ 进程，需要用「模块自身包名」创建一个可访问模块私有数据的 Context，
 * 从而读取 NekoSettingsActivity 写入的偏好。
 */
object Contexts {

    /** 当前应用（QQ）的 Application Context */
    fun appContext(): Context {
        // ✅ 使用反射获取，完全不受影响
        val at = Class.forName("android.app.ActivityThread")
        val m = at.getMethod("currentApplication")
        return m.invoke(null) as Context
    }

    /** 模块（neko.qq）的 Context，可读取模块私有 SharedPreferences */
    fun moduleContext(): Context =
        appContext().createPackageContext("neko.qq", Context.CONTEXT_IGNORE_SECURITY)

    /** 屏幕密度（sp -> px 换算用） */
    fun density(): Float = appContext().resources.displayMetrics.density
}
