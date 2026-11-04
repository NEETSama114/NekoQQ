
package neko.qq.hook

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object InjectSettingsEntry {

    private const val TAG = "NekoQQ-InjectSettingsEntry"

    private const val TARGET_TEXT = "账号与安全"
    private const val ENTRY_TEXT = "Neko设置"

    private const val ENTRY_TAG = "neko_qq_settings_entry"

    /**
     * 注册 Activity 生命周期回调。
     *
     * 注意：第一个参数是 Application，不是 ClassLoader。
     */
    fun hook(
        application: Application,
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        if (lpparam.packageName != "com.tencent.mobileqq") {
            return
        }

        XposedBridge.log("$TAG: 注册 ActivityLifecycleCallbacks")

        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?
                ) {
                }

                override fun onActivityStarted(activity: Activity) {
                }

                override fun onActivityResumed(activity: Activity) {
                    try {
                        val decorView = activity.window?.decorView ?: return

                        decorView.postDelayed({
                            try {
                                injectRow(activity)
                            } catch (t: Throwable) {
                                XposedBridge.log(
                                    "$TAG: injectRow异常: ${t.stackTraceToString()}"
                                )
                            }
                        }, 700)

                    } catch (t: Throwable) {
                        XposedBridge.log(
                            "$TAG: onActivityResumed异常: ${t.stackTraceToString()}"
                        )
                    }
                }

                override fun onActivityPaused(activity: Activity) {
                }

                override fun onActivityStopped(activity: Activity) {
                }

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle
                ) {
                }

                override fun onActivityDestroyed(activity: Activity) {
                }
            }
        )
    }

    /**
     * 在设置页面中插入 Neko设置 入口。
     */
    private fun injectRow(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val decorView = activity.window?.decorView ?: return

        // 防止重复插入
        if (decorView.findViewWithTag<View>(ENTRY_TAG) != null) {
            return
        }

        val targetTextView = findTextView(
            decorView as? ViewGroup ?: return,
            TARGET_TEXT
        )

        if (targetTextView == null) {
            XposedBridge.log(
                "$TAG: 当前Activity未找到文本：$TARGET_TEXT，Activity=${activity.javaClass.name}"
            )
            return
        }

        val targetRow = findVerticalParent(targetTextView)

        if (targetRow == null) {
            XposedBridge.log(
                "$TAG: 未找到目标文本对应的垂直LinearLayout"
            )
            return
        }

        val parent = targetRow.parent as? ViewGroup

        if (parent == null) {
            XposedBridge.log(
                "$TAG: 目标行没有有效的父容器"
            )
            return
        }

        val targetIndex = parent.indexOfChild(targetRow)

        if (targetIndex < 0) {
            XposedBridge.log(
                "$TAG: 无法获取目标行的位置"
            )
            return
        }

        val entryView = createEntryView(
            activity,
            targetTextView
        )

        try {
            parent.addView(entryView, targetIndex)

            XposedBridge.log(
                "$TAG: 成功插入 Neko设置，Activity=${activity.javaClass.name}"
            )

        } catch (t: Throwable) {
            XposedBridge.log(
                "$TAG: 插入入口失败: ${t.stackTraceToString()}"
            )
        }
    }

    /**
     * 创建 Neko设置 入口。
     */
    private fun createEntryView(
        activity: Activity,
        referenceTextView: TextView
    ): TextView {

        val entry = TextView(activity)

        entry.text = ENTRY_TEXT
        entry.tag = ENTRY_TAG

        entry.gravity = Gravity.CENTER_VERTICAL

        entry.textSize = referenceTextView.textSize / activity.resources.displayMetrics.scaledDensity

        entry.setTextColor(referenceTextView.currentTextColor)

        entry.typeface = referenceTextView.typeface

        entry.setPadding(
            referenceTextView.paddingLeft,
            referenceTextView.paddingTop,
            referenceTextView.paddingRight,
            referenceTextView.paddingBottom
        )

        entry.isClickable = true
        entry.isFocusable = true

        entry.background = referenceTextView.background?.constantState?.newDrawable()?.mutate()

        val referenceHeight = referenceTextView.height

        val height = if (referenceHeight > 0) {
            referenceHeight
        } else {
            dp(activity, 56)
        }

        entry.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        )

        entry.setOnClickListener {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "neko.qq",
                        "neko.qq.ui.NekoSettingsActivity"
                    )
                }

                activity.startActivity(intent)

                XposedBridge.log(
                    "$TAG: 点击 Neko设置，尝试启动 NekoSettingsActivity"
                )

            } catch (t: Throwable) {
                XposedBridge.log(
                    "$TAG: 启动 NekoSettingsActivity 失败: ${t.stackTraceToString()}"
                )
            }
        }

        return entry
    }

    /**
     * 递归查找指定文本的 TextView。
     */
    private fun findTextView(
        root: ViewGroup,
        targetText: String
    ): TextView? {

        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)

            if (child is TextView) {
                if (child.text?.toString()?.trim() == targetText) {
                    return child
                }
            }

            if (child is ViewGroup) {
                val result = findTextView(child, targetText)

                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    /**
     * 从目标 TextView 向上寻找垂直 LinearLayout。
     *
     * 这个容器用于放置新入口。
     */
    private fun findVerticalParent(
        target: TextView
    ): ViewGroup? {

        var current: View? = target

        repeat(5) {
            val parent = current?.parent as? ViewGroup ?: return null

            if (
                parent is LinearLayout &&
                parent.orientation == LinearLayout.VERTICAL
            ) {
                val grandParent = parent.parent

                if (grandParent is ViewGroup) {
                    return parent
                }
            }

            current = parent
        }

        return null
    }

    private fun dp(
        activity: Activity,
        value: Int
    ): Int {
        return (value * activity.resources.displayMetrics.density + 0.5f).toInt()
    }
}
