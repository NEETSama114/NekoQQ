package neko.qq.features

import android.graphics.Paint
import de.robv.android.xposed.XposedBridge
import neko.qq.constants.QQConfig
import neko.qq.util.Hooker
import neko.qq.util.Prefs
import neko.qq.util.Contexts // 👈 增加这一行，修复 Unresolved reference: Contexts

/**
 * 显示与界面相关增强：
 *  - 篡改预览字体大小
 *  - 强制平板 / 强制手机模式
 *  - 修改 DPI
 *  - 强制默认字体
 *  - 自定义启动图列数
 *  - 随机气泡
 *  - 允许上传透明头像/表情
 */
object Display {

    fun hook(classLoader: ClassLoader, lpparam: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam) {

        // 篡改预览字体大小：拿到画笔后改 textSize
        Hooker.after(classLoader, "NekoQQ:preview_font", "preview_font_size", QQConfig.PREVIEW_FONT_SIZE_CANDIDATES) {
            if (!Prefs.isPreviewFontEnabled) return@after
            val paint = it.result as? Paint ?: return@after
            paint.textSize = Prefs.previewFontSize * Contexts.density()
            XposedBridge.log("[NekoQQ] 预览字体大小 -> ${Prefs.previewFontSize}sp")
        }

        // 强制平板 / 手机模式：短路 isTablet 返回值
        Hooker.before(classLoader, "NekoQQ:tablet", "force_tablet", QQConfig.FORCE_TABLET_CANDIDATES) {
            if (Prefs.forceTablet) it.setResult(true)
            else if (Prefs.forcePhone) it.setResult(false)
        }

        // 修改 DPI：覆盖 DisplayMetrics
        Hooker.after(classLoader, "NekoQQ:dpi", "modify_dpi", QQConfig.MODIFY_DPI_CANDIDATES) {
            val dpi = Prefs.modifyDpi ?: return@after
            val dm = it.thisObject as? android.util.DisplayMetrics ?: return@after
            dm.densityDpi = dpi
            dm.density = dpi / 160f
            dm.scaledDensity = dpi / 160f
            XposedBridge.log("[NekoQQ] DPI -> $dpi")
        }

        // 强制默认字体：让字体路径返回空，使用系统默认
        Hooker.before(classLoader, "NekoQQ:font", "force_default_font", QQConfig.FORCE_DEFAULT_FONT_CANDIDATES) {
            if (Prefs.forceDefaultFont) it.setResult("")
        }

        // 自定义启动图列数
        Hooker.after(classLoader, "NekoQQ:splash_col", "splash_columns", QQConfig.SPLASH_COLUMNS_CANDIDATES) {
            if (Prefs.splashColumns != 4) it.setResult(Prefs.splashColumns)
        }

        // 随机气泡：每次取随机气泡 id
        Hooker.before(classLoader, "NekoQQ:bubble", "random_bubble", QQConfig.RANDOM_BUBBLE_CANDIDATES) {
            if (Prefs.randomBubble) {
                val id = (1..200).random()
                XposedBridge.log("[NekoQQ] 随机气泡 -> $id")
                // TODO(版本校准): 视气泡接口返回类型，setResult(id) 或替换参数
            }
        }

        // 允许透明头像/表情：跳过圆角裁切与底色（版本相关，留接入点）
        Hooker.before(classLoader, "NekoQQ:transparent", "transparent_avatar", QQConfig.TRANSPARENT_AVATAR_CANDIDATES) {
            if (Prefs.transparentAvatar) {
                XposedBridge.log("[NekoQQ] 透明头像/表情：已启用（请校准裁切点）")
                // TODO(版本校准): 在头像解码/裁切处跳过圆角与底色填充
            }
        }
    }
}
