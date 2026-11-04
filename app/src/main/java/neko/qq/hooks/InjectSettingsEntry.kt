
package neko.qq.hooks

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import neko.qq.constants.QQConfig
import neko.qq.util.Views

/**
 * 动态监听 QQ Activity 生命周期。
 *
 * 不再依赖 QQ 内部设置页混淆类名。
 * 当 Activity 恢复到前台时，延迟检查页面布局，
 * 找到「账号与安全」后注入 Neko设置入口。
 */
object InjectSettingsEntry {

    private const val ROW_TAG = "neko_settings_row"
    private const val INJECT_DELAY = 700L

    /**
     * 注册 Activity 生命周期监听器。
     *
     * 仅在 QQ 主进程注册。
     */
    fun hook(
        application: Application,
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        if (lpparam.packageName != QQConfig.PACKAGE_QQ) {
            return
        }

        if (lpparam.processName != QQConfig.PACKAGE_QQ) {
            XposedBridge.log(
                "[NekoQQ] 非主进程，跳过设置入口监听: ${lpparam.processName}"
            )
            return
        }

        XposedBridge.log(
            "[NekoQQ] 注册 Activity 生命周期监听器"
        )

        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?
                ) {
                    // 不在创建时注入，等待页面恢复前台
                }

                override fun onActivityStarted(
                    activity: Activity
                ) {
                    // 无操作
                }

                override fun onActivityResumed(
                    activity: Activity
                ) {
                    val decorView =
                        activity.window?.decorView ?: return

                    decorView.postDelayed({

                        if (activity.isFinishing || activity.isDestroyed) {
                            return@postDelayed
                        }

                        runCatching {
                            injectRow(activity)
                        }.onFailure {
                            XposedBridge.log(
                                "[NekoQQ] 动态注入 Neko设置失败:\n" +
                                    it.stackTraceToString()
                            )
                        }

                    }, INJECT_DELAY)
                }

                override fun onActivityPaused(
                    activity: Activity
                ) {
                    // 无操作
                }

                override fun onActivityStopped(
                    activity: Activity
                ) {
                    // 无操作
                }

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle
                ) {
                    // 无操作
                }

                override fun onActivityDestroyed(
                    activity: Activity
                ) {
                    // 无操作
                }
            }
        )

        XposedBridge.log(
            "[NekoQQ] Activity 生命周期监听器注册成功"
        )
    }

    /**
     * 尝试在当前 Activity 中插入入口。
     */
    private fun injectRow(activity: Activity) {

        val root =
            activity.findViewById<ViewGroup>(android.R.id.content)
                ?: return

        val target =
            Views.findTextViewWithText(
                root,
                QQConfig.ACCOUNT_SECURITY_TEXT
            ) ?: return

        val row =
            Views.findClickableRow(target) ?: return

        val parent =
            row.parent as? ViewGroup ?: return

        // 防止重复注入
        if (Views.hasInjected(parent, ROW_TAG)) {
            return
        }

        val index = parent.indexOfChild(row)

        if (index < 0) {
            return
        }

        val nekoRow = buildNekoRow(activity)

        parent.addView(nekoRow, index)

        XposedBridge.log(
            "[NekoQQ] 已插入 Neko设置入口，Activity=${activity.javaClass.name}"
        )
    }

    /**
     * 创建 Neko设置入口。
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

        // 图标
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

        // 标题
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

        // 标签
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

        // 箭头
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

        row.addView(icon)
        row.addView(title)
        row.addView(badge)
        row.addView(chevron)

        // 点击跳转
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

    /**
     * dp 转换。
     */
    private fun dp(
        activity: Activity,
        value: Int
    ): Int {
        return (
            value *
                activity.resources.displayMetrics.density
            ).toInt()
    }

    /**
     * 点击背景。
     */
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

    /**
     * 圆角背景。
     */
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
