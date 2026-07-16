package neko.qq.hooks

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import neko.qq.constants.QQConfig
import neko.qq.util.Views

/**
 * 把「Neko设置」入口注入到 QQ 设置页，并放在「账号与安全」上方。
 *
 * 做法：hook 设置页 Activity 的 onCreate，等布局出来后做视图遍历，
 * 找到文字为「账号与安全」的那一行，在它前面插入一行可点击的 Neko设置。
 * 这种「视图遍历注入」不依赖 QQ 内部类，对原生设置列表兼容性强。
 */
object InjectSettingsEntry {

    private const val ROW_TAG = "neko_settings_row"

    fun hook(classLoader: ClassLoader, lpparam: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam) {
        for (name in QQConfig.SETTINGS_ACTIVITY_CANDIDATES) {
            try {
                val clazz = classLoader.loadClass(name)
                XposedHelpers.findAndHookMethod(
                    clazz, "onCreate", Bundle::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val activity = param.thisObject as? Activity ?: return
                            // 等布局稳定后再遍历，避免插入到未渲染完成的位置
                            activity.window.decorView.postDelayed({
                                runCatching { injectRow(activity) }
                                    .onFailure { XposedBridge.log("[NekoQQ] inject Neko设置 失败: ${it.message}") }
                            }, 700)
                        }
                    }
                )
                XposedBridge.log("[NekoQQ] 已 hook 设置页: $name")
                return
            } catch (_: Throwable) {
                // 该类在当前 QQ 版本不存在，尝试下一个候选
            }
        }
        XposedBridge.log("[NekoQQ] 未命中任何设置页候选类，Neko设置入口将仅通过桌面图标进入")
    }

    private fun injectRow(activity: Activity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val target = Views.findTextViewWithText(root, QQConfig.ACCOUNT_SECURITY_TEXT) ?: return
        val row = Views.findClickableRow(target) ?: return
        val parent = row.parent as? ViewGroup ?: return
        if (Views.hasInjected(parent, ROW_TAG)) return

        val idx = parent.indexOfChild(row)
        val nekoRow = buildNekoRow(activity)
        parent.addView(nekoRow, idx) // 插到「账号与安全」上面
        XposedBridge.log("[NekoQQ] 已插入 Neko设置 入口")
    }

    private fun buildNekoRow(activity: Activity): LinearLayout {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14))
            tag = ROW_TAG
            isClickable = true
            background = pressSelector(activity)
            minimumHeight = dp(activity, 56)
        }

        val icon = TextView(activity).apply {
            text = "🌸"
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(
                dp(activity, 28), dp(activity, 28)
            ).apply { marginEnd = dp(activity, 14) }
            gravity = Gravity.CENTER
        }

        val title = TextView(activity).apply {
            text = "Neko设置"
            textSize = 16f
            setTextColor(Color.parseColor("#E6EDF3"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val badge = TextView(activity).apply {
            text = "NekoQQ"
            textSize = 10f
            setTextColor(Color.WHITE)
            setPadding(dp(activity, 6), dp(activity, 2), dp(activity, 6), dp(activity, 2))
            background = roundBg(activity, Color.parseColor("#FF6FA5"))
            gravity = Gravity.CENTER
        }

        val chevron = ImageView(activity).apply {
            setImageResource(android.R.drawable.ic_media_play) // 仅占位，QQ 自带箭头更佳
            setColorFilter(Color.parseColor("#8B949E"))
            layoutParams = LinearLayout.LayoutParams(dp(activity, 16), dp(activity, 16)).apply {
                marginStart = dp(activity, 10)
            }
        }

        row.addView(icon)
        row.addView(title)
        row.addView(badge)
        row.addView(chevron)

        row.setOnClickListener {
            val intent = Intent().setClassName("neko.qq", "neko.qq.ui.NekoSettingsActivity")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        }
        return row
    }

    // ===== 简单 dp / 背景辅助 =====
    private fun dp(c: Activity, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    private fun pressSelector(c: Activity): android.graphics.drawable.StateListDrawable {
        return android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed),
                android.graphics.drawable.ColorDrawable(Color.parseColor("#232B3A")))
            addState(intArrayOf(),
                android.graphics.drawable.ColorDrawable(Color.parseColor("#1C2230")))
        }
    }

    private fun roundBg(c: Activity, color: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(c, 20).toFloat()
        }
    }
}
