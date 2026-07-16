package neko.qq.features

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import neko.qq.constants.QQConfig
import neko.qq.util.Prefs
import neko.qq.util.Views

/**
 * 表情面板。
 *
 * 不依赖 QQ 内部类：在「聊天输入栏的 EditText」左侧插入一个表情按钮，
 * 点击弹出表情面板（emoji + 颜文字），选中即插入到输入框。
 * 通过视图遍历定位输入栏 + 匹配 hint 关键词，跨 QQNT 版本兼容性较好。
 */
object EmojiPanel {

    private const val BTN_TAG = "neko_emoji_btn"
    private val CHAT_ACTIVITY_HINT = listOf("aio", "AIO", "chat", "Chat", "C2C", "Troop", "troop", "Msg")

    private val EMOJIS = listOf(
        "😀","😁","😂","🤣","😊","😍","😘","😎","🤔","😜","😭","😡",
        "👍","👏","🙏","💪","🎉","🔥","💡","⭐","❤️","💔","🌸","🐱",
        "🐶","🦊","🐰","🍰","🍉","☕","🌈","⚡","💤","🎵","📌","✅",
        "❌","💯","🤝","👀","🐾","🌟","💫","🍓","🍔","⚽","🎮","📱",
        "🌙","☀️","🍻","🎁","👻","💰","🔔","📢","😴","🥺","😏","🤩"
    )
    private val KAOMOJI = listOf(
        "(=^･ω･^=)","(ฅ´ω`ฅ)","(≧▽≦)","(｡•̀ᴗ-)✧","(´• ω •`)",
        "ヽ(=^･ω･^)丿","(≧ω≦)/","(*≧ω≦*)","(๑•̀ㅂ•́)و","ミ(≧Д≦)彡"
    )

    fun hook(classLoader: ClassLoader, lpparam: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam) {
        if (!Prefs.emojiPanelEnabled) return

        // hook 所有 Activity.onResume，再按类名筛选出聊天界面注入（版本无关）
        try {
            XposedHelpers.findAndHookMethod(
                Activity::class.java, "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val act = param.thisObject as? Activity ?: return
                        val name = act.javaClass.name
                        if (!CHAT_ACTIVITY_HINT.any { name.contains(it) }) return
                        act.window.decorView.postDelayed({
                            runCatching { injectEmojiButton(act) }
                                .onFailure { XposedBridge.log("[NekoQQ] 表情面板注入失败: ${it.message}") }
                        }, 800)
                    }
                }
            )
            XposedBridge.log("[NekoQQ] 表情面板：已挂载 Activity.onResume 监听")
        } catch (t: Throwable) {
            XposedBridge.log("[NekoQQ] 表情面板 hook 失败: ${t.message}")
        }
    }

    private fun injectEmojiButton(activity: Activity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val edit = findChatInput(root) ?: return
        val parent = edit.parent as? ViewGroup ?: return
        if (Views.hasInjected(parent, BTN_TAG)) return

        val btn = TextView(activity).apply {
            text = "😀"
            textSize = 22f
            gravity = Gravity.CENTER
            tag = BTN_TAG
            setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 8), dp(activity, 4))
            setOnClickListener { showPanel(activity, this, edit) }
        }
        val idx = parent.indexOfChild(edit)
        parent.addView(btn, idx) // 插到输入框前面
        XposedBridge.log("[NekoQQ] 表情面板：已插入表情按钮")
    }

    private fun showPanel(activity: Activity, anchor: TextView, edit: EditText) {
        val grid = GridView(activity).apply {
            numColumns = 6
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            setBackgroundColor(Color.parseColor("#1C2230"))
            setPadding(8, 8, 8, 8)
        }
        val data = if (Prefs.emojiKaomoji) EMOJIS + KAOMOJI else EMOJIS
        grid.adapter = object : android.widget.BaseAdapter() {
            override fun getCount() = data.size
            override fun getItem(i: Int) = data[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, convertView: android.view.View?, parent: ViewGroup): android.view.View {
                val tv = (convertView as? TextView) ?: TextView(activity).apply {
                    gravity = Gravity.CENTER
                    textSize = 20f
                }
                tv.text = data[i]
                return tv
            }
        }
        grid.setOnItemClickListener { _, _, position, _ ->
            edit.append(data[position])
            popup?.dismiss()
        }

        val w = dp(activity, 280)
        val h = dp(activity, 220)
        popup = PopupWindow(grid, w, h, true).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.parseColor("#1C2230")))
            elevation = 12f
            showAsDropDown(anchor, 0, -h - anchor.height)
        }
    }

    private var popup: PopupWindow? = null

    /** 找到聊天输入栏的 EditText（按 hint 关键词匹配） */
    private fun findChatInput(root: ViewGroup): EditText? {
        val queue = ArrayDeque<android.view.View>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            if (v is EditText) {
                val hint = v.hint?.toString().orEmpty()
                if (QQConfig.CHAT_INPUT_HINT_KEYWORDS.any { hint.contains(it) }) return v
            }
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) queue.add(v.getChildAt(i))
            }
        }
        return null
    }

    private fun dp(c: Activity, v: Int) = (v * c.resources.displayMetrics.density).toInt()
}
