package neko.qq.hook

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

import neko.qq.constants.PrefKeys
import neko.qq.util.Contexts

object InjectSettingsEntry {

    private const val TAG = "NekoQQ-InjectSettingsEntry"

    private const val ENTRY_TAG = "neko_qq_settings_entry"

    fun hook(
        application: Application,
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        if (lpparam.packageName != "com.tencent.mobileqq") {
            return
        }

        if (lpparam.processName != "com.tencent.mobileqq") {
            return
        }

        XposedBridge.log("$TAG: 注册 ActivityLifecycleCallbacks")

        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?
                ) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityResumed(activity: Activity) {
                    val root = activity.window?.decorView ?: return

                    root.postDelayed({
                        inject(activity)
                    }, 500)

                    root.postDelayed({
                        inject(activity)
                    }, 1500)
                }

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle
                ) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }

    private fun inject(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val root = activity.window?.decorView ?: return

        if (findViewByTag(root, ENTRY_TAG) != null) {
            return
        }

        val anchor = findTextView(root, "账号与安全")

        if (anchor == null) {
            return
        }

        val row = findSettingsRow(anchor)

        if (row == null) {
            XposedBridge.log(
                "$TAG: 找到账号与安全，但无法找到完整设置行"
            )
            return
        }

        val parent = row.parent as? ViewGroup

        if (parent == null) {
            XposedBridge.log(
                "$TAG: 设置行没有可用父容器"
            )
            return
        }

        val index = parent.indexOfChild(row)

        if (index < 0) {
            return
        }

        val entry = createEntry(
            activity,
            row
        )

        try {
            /*
             * 插入到“账号与安全”前面。
             */
            parent.addView(entry, index)

            XposedBridge.log(
                "$TAG: 成功插入完整 Neko设置行，Activity=${activity.javaClass.name}"
            )

        } catch (t: Throwable) {
            XposedBridge.log(
                "$TAG: 插入失败: ${t.stackTraceToString()}"
            )
        }
    }

    /**
     * 从“账号与安全”的文字向上寻找真正的一整行设置项。
     */
    private fun findSettingsRow(
        textView: TextView
    ): View? {

        var current: View = textView

        repeat(8) {
            val parent = current.parent as? ViewGroup
                ?: return current

            /*
             * 如果当前 View 是父容器的直接子项，
             * 并且父容器还有多个类似项目，
             * 继续向上寻找。
             */
            val grandParent = parent.parent as? ViewGroup

            if (grandParent != null) {
                val siblings = grandParent.childCount

                /*
                 * QQ 设置列表通常包含多个条目。
                 * current 的父布局作为“整行”更合理。
                 */
                if (siblings >= 2 && siblings <= 30) {
                    return parent
                }
            }

            current = parent
        }

        return current
    }

    private fun createEntry(
        activity: Activity,
        referenceRow: View
    ): View {

        val density =
            activity.resources.displayMetrics.density

        fun dp(value: Int): Int =
            (value * density + 0.5f).toInt()

        /*
         * 创建完整的一行，而不是只创建 TextView。
         */
        val container = LinearLayout(activity)

        container.tag = ENTRY_TAG
        container.orientation = LinearLayout.HORIZONTAL
        container.gravity = Gravity.CENTER_VERTICAL

        container.minimumHeight =
            if (referenceRow.height > 0) {
                referenceRow.height
            } else {
                dp(56)
            }

        container.setPadding(
            dp(20),
            dp(12),
            dp(16),
            dp(12)
        )

        val title = TextView(activity)

        title.text = "Neko设置"
        title.textSize = 16f
        title.setTextColor(Color.WHITE)

        title.layoutParams =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )

        val arrow = TextView(activity)

        arrow.text = "›"
        arrow.textSize = 28f
        arrow.gravity = Gravity.CENTER
        arrow.setTextColor(
            Color.rgb(160, 160, 160)
        )

        container.addView(title)
        container.addView(arrow)

        /*
         * 尽量继承 QQ 原设置行的背景。
         */
        container.background =
            referenceRow.background
                ?.constantState
                ?.newDrawable()
                ?.mutate()

        if (container.background == null) {
            val bg = GradientDrawable()

            bg.setColor(
                Color.rgb(40, 40, 40)
            )

            bg.cornerRadius =
                dp(12).toFloat()

            container.background = bg
        }

        container.setOnClickListener {
            showNekoSettings(activity)
        }

        return container
    }

    /**
     * 在 QQ 当前窗口内显示 NekoQQ 设置。
     *
     * 不启动 NekoSettingsActivity，
     * 因此不会触发“即将离开QQ”的提示。
     */
    private fun showNekoSettings(
        activity: Activity
    ) {
        try {
            val moduleContext =
                Contexts.moduleContext()

            val prefs =
                moduleContext.getSharedPreferences(
                    PrefKeys.PREFS_NAME,
                    android.content.Context.MODE_PRIVATE
                )

            val density =
                activity.resources.displayMetrics.density

            fun dp(value: Int): Int =
                (value * density + 0.5f).toInt()

            val scrollView =
                ScrollView(activity)

            val content =
                LinearLayout(activity)

            content.orientation =
                LinearLayout.VERTICAL

            content.setPadding(
                dp(20),
                dp(8),
                dp(20),
                dp(8)
            )

            scrollView.addView(content)

            /*
             * 防撤回
             */
            val antiRecall =
                Switch(activity)

            antiRecall.text = "防撤回"

            antiRecall.isChecked =
                prefs.getBoolean(
                    PrefKeys.ANTI_RECALL,
                    true
                )

            antiRecall.setOnCheckedChangeListener {
                    _,
                    checked ->

                prefs.edit()
                    .putBoolean(
                        PrefKeys.ANTI_RECALL,
                        checked
                    )
                    .apply()
            }

            content.addView(antiRecall)

            /*
             * 撤回提示
             */
            val recallBanner =
                Switch(activity)

            recallBanner.text =
                "显示撤回提示"

            recallBanner.isChecked =
                prefs.getBoolean(
                    PrefKeys.RECALL_BANNER,
                    true
                )

            recallBanner.setOnCheckedChangeListener {
                    _,
                    checked ->

                prefs.edit()
                    .putBoolean(
                        PrefKeys.RECALL_BANNER,
                        checked
                    )
                    .apply()
            }

            content.addView(recallBanner)

            /*
             * 群管理
             */
            val groupManager =
                Switch(activity)

            groupManager.text =
                "高级群管理"

            groupManager.isChecked =
                prefs.getBoolean(
                    PrefKeys.GROUP_MGR,
                    true
                )

            groupManager.setOnCheckedChangeListener {
                    _,
                    checked ->

                prefs.edit()
                    .putBoolean(
                        PrefKeys.GROUP_MGR,
                        checked
                    )
                    .apply()
            }

            content.addView(groupManager)

            /*
             * 表情面板
             */
            val emojiPanel =
                Switch(activity)

            emojiPanel.text =
                "自定义表情面板"

            emojiPanel.isChecked =
                prefs.getBoolean(
                    PrefKeys.EMOJI_PANEL,
                    true
                )

            emojiPanel.setOnCheckedChangeListener {
                    _,
                    checked ->

                prefs.edit()
                    .putBoolean(
                        PrefKeys.EMOJI_PANEL,
                        checked
                    )
                    .apply()
            }

            content.addView(emojiPanel)

            /*
             * 自动拉取
             */
            val autoPull =
                Switch(activity)

            autoPull.text =
                "自动更新 Hook 配置"

            autoPull.isChecked =
                prefs.getBoolean(
                    PrefKeys.AUTO_PULL,
                    true
                )

            autoPull.setOnCheckedChangeListener {
                    _,
                    checked ->

                prefs.edit()
                    .putBoolean(
                        PrefKeys.AUTO_PULL,
                        checked
                    )
                    .apply()
            }

            content.addView(autoPull)

            AlertDialog.Builder(activity)
                .setTitle("NekoQQ 设置")
                .setView(scrollView)
                .setPositiveButton(
                    "完成",
                    null
                )
                .show()

            XposedBridge.log(
                "$TAG: 已在 QQ 内显示 NekoQQ 设置"
            )

        } catch (t: Throwable) {
            XposedBridge.log(
                "$TAG: 打开内嵌设置失败: ${t.stackTraceToString()}"
            )
        }
    }

    private fun findTextView(
        root: View,
        target: String
    ): TextView? {

        if (root is TextView &&
            root.text?.toString()?.trim() == target
        ) {
            return root
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val result =
                    findTextView(
                        root.getChildAt(i),
                        target
                    )

                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    private fun findViewByTag(
        root: View,
        tag: String
    ): View? {

        if (root.tag == tag) {
            return root
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val result =
                    findViewByTag(
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
}
