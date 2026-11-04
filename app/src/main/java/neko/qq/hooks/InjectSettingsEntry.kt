
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
import de.robv.android.xposed.callbacks.XC_LoadPackage
import neko.qq.constants.QQConfig
import neko.qq.util.Views

/**
 * 将「Neko设置」入口注入到 QQ 设置页。
 *
 * 当前方案：
 * 1. 遍历 QQConfig.SETTINGS_ACTIVITY_CANDIDATES。
 * 2. 尝试加载候选 Activity。
 * 3. 检查 Activity 是否存在 onCreate(Bundle)。
 * 4. Hook onCreate(Bundle)。
 * 5. 页面创建后延迟遍历视图。
 * 6. 找到「账号与安全」后插入 Neko设置。
 *
 * 本实现主要用于诊断设置页候选类未命中的问题。
 */
object InjectSettingsEntry {

    private const val ROW_TAG = "neko_settings_row"

    /**
     * Hook 设置页 Activity。
     */
    fun hook(
        classLoader: ClassLoader,
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        var loadedClassCount = 0
        var hookSuccessCount = 0

        XposedBridge.log(
            "[NekoQQ] 开始扫描设置页候选类，" +
                "进程=${lpparam.processName}"
        )

        for (name in QQConfig.SETTINGS_ACTIVITY_CANDIDATES) {

            // ============================
            // 1. 尝试加载候选类
            // ============================

            val clazz = try {
                classLoader.loadClass(name)
            } catch (e: ClassNotFoundException) {
                XposedBridge.log(
                    "[NekoQQ] 设置页候选类不存在: $name"
                )
                continue
            } catch (e: Throwable) {
                XposedBridge.log(
                    "[NekoQQ] 设置页候选类加载异常: $name\n" +
                        e.stackTraceToString()
                )
                continue
            }

            loadedClassCount++

            XposedBridge.log(
                "[NekoQQ] 设置页候选类加载成功: $name"
            )

            // ============================
            // 2. 判断是否为 Activity
            // ============================

            if (!Activity::class.java.isAssignableFrom(clazz)) {
                XposedBridge.log(
                    "[NekoQQ] 候选类不是 Activity: $name"
                )
                continue
            }

            // ============================
            // 3. Hook onCreate(Bundle)
            // ============================

            try {
                XposedHelpers.findAndHookMethod(
                    clazz,
                    "onCreate",
                    Bundle::class.java,
                    object : XC_MethodHook() {

                        override fun afterHookedMethod(
                            param: MethodHookParam
                        ) {
                            val activity =
                                param.thisObject as? Activity ?: return

                            val decorView =
                                activity.window?.decorView ?: return

                            // 等待 QQ 设置页布局完成
                            decorView.postDelayed({

                                runCatching {
                                    injectRow(activity)
                                }.onFailure {
                                    XposedBridge.log(
                                        "[NekoQQ] inject Neko设置 失败:\n" +
                                            it.stackTraceToString()
                                    )
                                }

                            }, 700)
                        }
                    }
                )

                hookSuccessCount++

                XposedBridge.log(
                    "[NekoQQ] 设置页 Hook 成功: $name"
                )

                // 保持原行为：第一个成功的候选即结束扫描
                return

            } catch (e: Throwable) {

                XposedBridge.log(
                    "[NekoQQ] 候选类存在，但 Hook onCreate(Bundle) 失败: " +
                        "$name\n" +
                        e.stackTraceToString()
                )
            }
        }

        // ============================
        // 4. 扫描结束，输出汇总
        // ============================

        XposedBridge.log(
            "[NekoQQ] 设置页候选扫描结束: " +
                "加载成功=$loadedClassCount, " +
                "Hook成功=$hookSuccessCount, " +
                "候选总数=${QQConfig.SETTINGS_ACTIVITY_CANDIDATES.size}"
        )

        XposedBridge.log(
            "[NekoQQ] 未命中任何设置页候选类，" +
                "Neko设置入口将仅通过桌面图标进入"
        )
    }

    /**
     * 注入 Neko设置入口。
     *
     * 将入口放置在「账号与安全」上方。
     */
    private fun injectRow(activity: Activity) {

        val root =
            activity.findViewById<ViewGroup>(android.R.id.content)
                ?: return

        // 查找「账号与安全」文字
        val target =
            Views.findTextViewWithText(
                root,
                QQConfig.ACCOUNT_SECURITY_TEXT
            ) ?: return

        // 查找对应可点击行
        val row =
            Views.findClickableRow(target) ?: return

        val parent =
            row.parent as? ViewGroup ?: return

        // 防止重复插入
        if (Views.hasInjected(parent, ROW_TAG)) {
            XposedBridge.log(
                "[NekoQQ] Neko设置入口已存在，跳过重复注入"
            )
            return
        }

        val index = parent.indexOfChild(row)

        if (index < 0) return

        val nekoRow = buildNekoRow(activity)

        // 插入「账号与安全」上方
        parent.addView(nekoRow, index)

        XposedBridge.log(
            "[NekoQQ] 已插入 Neko设置 入口"
        )
    }

    /**
     * 创建 Neko设置 UI 行。
     */
    private fun buildNekoRow(
        activity: Activity
    ): LinearLayout {

        val row = LinearLayout(activity).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                dp(activity, 16),
                dp(activity, 14),
                dp(activity, 16),
                dp(activity, 14)
            )

            tag = ROW_TAG

            isClickable = true
            isFocusable = true

            background = pressSelector(activity)

            minimumHeight = dp(activity, 56)
        }

        // ============================
        // 图标
        // ============================

        val icon = TextView(activity).apply {

            text = "🌸"

            textSize = 20f

            layoutParams =
                LinearLayout.LayoutParams(
                    dp(activity, 28),
                    dp(activity, 28)
                ).apply {
                    marginEnd = dp(activity, 14)
                }

            gravity = Gravity.CENTER
        }

        // ============================
        // 标题
        // ============================

        val title = TextView(activity).apply {

            text = "Neko设置"

            textSize = 16f

            setTextColor(
                Color.parseColor("#E6EDF3")
            )

            typeface = Typeface.DEFAULT_BOLD

            layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
        }

        // ============================
        // NekoQQ 标签
        // ============================

        val badge = TextView(activity).apply {

            text = "NekoQQ"

            textSize = 10f

            setTextColor(Color.WHITE)

            setPadding(
                dp(activity, 6),
                dp(activity, 2),
                dp(activity, 6),
                dp(activity, 2)
            )

            background =
                roundBg(
                    activity,
                    Color.parseColor("#FF6FA5")
                )

            gravity = Gravity.CENTER
        }

        // ============================
        // 右侧箭头
        // ============================

        val chevron = ImageView(activity).apply {

            setImageResource(
                android.R.drawable.ic_media_play
            )

            setColorFilter(
                Color.parseColor("#8B949E")
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    dp(activity, 16),
                    dp(activity, 16)
                ).apply {
                    marginStart = dp(activity, 10)
                }
        }

        // ============================
        // 添加子视图
        // ============================

        row.addView(icon)
        row.addView(title)
        row.addView(badge)
        row.addView(chevron)

        // ============================
        // 点击跳转
        // ============================

        row.setOnClickListener {

            runCatching {

                val intent = Intent().setClassName(
                    "neko.qq",
                    "neko.qq.ui.NekoSettingsActivity"
                )

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                activity.startActivity(intent)

            }.onFailure {

                XposedBridge.log(
                    "[NekoQQ] 打开 NekoSettingsActivity 失败:\n" +
                        it.stackTraceToString()
                )
            }
        }

        return row
    }

    // ============================================================
    // dp 转换
    // ============================================================

    private fun dp(
        activity: Activity,
        value: Int
    ): Int {
        return (
            value *
                activity.resources.displayMetrics.density
            ).toInt()
    }

    // ============================================================
    // 点击背景
    // ============================================================

    private fun pressSelector(
        activity: Activity
    ): android.graphics.drawable.StateListDrawable {

        return android.graphics.drawable.StateListDrawable().apply {

            addState(
                intArrayOf(android.R.attr.state_pressed),
                android.graphics.drawable.ColorDrawable(
                    Color.parseColor("#232B3A")
                )
            )

            addState(
                intArrayOf(),
                android.graphics.drawable.ColorDrawable(
                    Color.parseColor("#1C2230")
                )
            )
        }
    }

    // ============================================================
    // 圆角背景
    // ============================================================

    private fun roundBg(
        activity: Activity,
        color: Int
    ): android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable.GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                dp(activity, 20).toFloat()
        }
    }
}
