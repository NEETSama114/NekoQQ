package neko.qq.hook

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

object InjectSettingsEntry {

    private const val TAG = "NekoQQ-InjectSettingsEntry"

    private const val ENTRY_TEXT = "Neko设置"
    private const val ENTRY_TAG = "neko_qq_settings_entry"

    /*
     * 不再只依赖“账号与安全”。
     * 新版 QQ 的设置页面可能调整顺序或布局，
     * 任意找到一个设置页面的特征文本即可作为锚点。
     */
    private val TARGET_TEXTS = listOf(
        "账号与安全",
        "消息通知",
        "模式选择",
        "个性装扮与特权外显",
        "通用",
        "隐私设置"
    )

    fun hook(
        application: Application,
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        if (lpparam.packageName != "com.tencent.mobileqq") {
            return
        }

        // 设置 UI 只需要在 QQ 主进程运行
        if (lpparam.processName != "com.tencent.mobileqq") {
            XposedBridge.log(
                "$TAG: 跳过非主进程: ${lpparam.processName}"
            )
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
                    val decorView = activity.window?.decorView ?: return

                    /*
                     * QQ 设置页面可能异步加载。
                     * 第一次尝试后再进行一次延迟尝试。
                     */
                    decorView.postDelayed({
                        safeInject(activity)
                    }, 500)

                    decorView.postDelayed({
                        safeInject(activity)
                    }, 1500)
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

    private fun safeInject(activity: Activity) {
        try {
            injectRow(activity)
        } catch (t: Throwable) {
            XposedBridge.log(
                "$TAG: injectRow异常: ${t.stackTraceToString()}"
            )
        }
    }

    /**
     * 在 QQ 设置页面插入 Neko设置。
     *
     * 新实现不再要求父级必须是 LinearLayout。
     */
    private fun injectRow(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val decorView = activity.window?.decorView ?: return

        // 防止重复插入
        if (findViewByTag(decorView, ENTRY_TAG) != null) {
            return
        }

        // 找到任意一个设置页特征文字
        var targetTextView: TextView? = null

        for (targetText in TARGET_TEXTS) {
            targetTextView = findTextView(
                decorView,
                targetText
            )

            if (targetTextView != null) {
                XposedBridge.log(
                    "$TAG: 找到设置页锚点: $targetText"
                )
                break
            }
        }

        if (targetTextView == null) {
            return
        }

        /*
         * 向上寻找真正的“列表项”，然后找到可插入的父容器。
         *
         * 这里不再检查 LinearLayout，因此可以兼容
         * FrameLayout、RecyclerView 等新版 QQ 的布局结构。
         */
        val result = findInsertPosition(targetTextView)

        if (result == null) {
            XposedBridge.log(
                "$TAG: 找到设置页文字，但无法确定插入位置，Activity=${activity.javaClass.name}"
            )
            return
        }

        val parent = result.parent
        val targetRow = result.row

        val index = parent.indexOfChild(targetRow)

        if (index < 0) {
            XposedBridge.log(
                "$TAG: 无法获取目标行索引"
            )
            return
        }

        val entryView = createEntryView(
            activity,
            targetTextView
        )

        try {
            /*
             * 插入到锚点所在项目的前面。
             * 如果找到的是“账号与安全”，效果就是原项目设计的位置。
             */
            parent.addView(entryView, index)

            XposedBridge.log(
                "$TAG: 成功插入 Neko设置，Activity=${activity.javaClass.name}"
            )

        } catch (t: Throwable) {
            XposedBridge.log(
                "$TAG: 插入入口失败: ${t.stackTraceToString()}"
            )
        }
    }

    private data class InsertPosition(
        val parent: ViewGroup,
        val row: View
    )

    /**
     * 从 TextView 向上寻找一个可以作为列表项插入的层级。
     *
     * 找到第一个父容器中“当前 View 是直接子元素”的位置，
     * 然后继续向上寻找具有多个子项的容器。
     */
    private fun findInsertPosition(
        target: View
    ): InsertPosition? {

        var current: View = target

        repeat(10) {
            val parent = current.parent as? ViewGroup
                ?: return null

            /*
             * 有多个直接子 View 的父容器通常是设置列表。
             */
            if (parent.childCount >= 2) {
                return InsertPosition(
                    parent = parent,
                    row = current
                )
            }

            current = parent
        }

        return null
    }

    /**
     * 递归查找指定文字。
     */
    private fun findTextView(
        root: View,
        targetText: String
    ): TextView? {

        if (root is TextView) {
            if (root.text?.toString()?.trim() == targetText) {
                return root
            }
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val result = findTextView(
                    root.getChildAt(i),
                    targetText
                )

                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    /**
     * 递归根据 Tag 查找 View。
     */
    private fun findViewByTag(
        root: View,
        tag: String
    ): View? {

        if (root.tag == tag) {
            return root
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val result = findViewByTag(
                    root.getChildAt(i),
                    tag
                )

                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    /**
     * 创建 Neko设置入口。
     */
    private fun createEntryView(
        activity: Activity,
        referenceTextView: TextView
    ): TextView {

        val entry = TextView(activity)

        entry.text = ENTRY_TEXT
        entry.tag = ENTRY_TAG

        entry.gravity = Gravity.CENTER_VERTICAL

        entry.textSize =
            referenceTextView.textSize /
                activity.resources.displayMetrics.scaledDensity

        entry.setTextColor(
            referenceTextView.currentTextColor
        )

        entry.typeface = referenceTextView.typeface

        entry.setPadding(
            referenceTextView.paddingLeft,
            referenceTextView.paddingTop,
            referenceTextView.paddingRight,
            referenceTextView.paddingBottom
        )

        entry.isClickable = true
        entry.isFocusable = true

        entry.background =
            referenceTextView.background
                ?.constantState
                ?.newDrawable()
                ?.mutate()

        val height = if (referenceTextView.height > 0) {
            referenceTextView.height
        } else {
            dp(activity, 56)
        }

        entry.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        )

        entry.setOnClickListener {
            openSettings(activity)
        }

        return entry
    }

    private fun openSettings(
        activity: Activity
    ) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "neko.qq",
                    "neko.qq.ui.NekoSettingsActivity"
                )
            }

            activity.startActivity(intent)

            XposedBridge.log(
                "$TAG: 已启动 NekoSettingsActivity"
            )

        } catch (t: Throwable) {
            XposedBridge.log(
                "$TAG: 启动 NekoSettingsActivity 失败: ${t.stackTraceToString()}"
            )
        }
    }

    private fun dp(
        activity: Activity,
        value: Int
    ): Int {
        return (
            value *
                activity.resources.displayMetrics.density +
                0.5f
            ).toInt()
    }
}
