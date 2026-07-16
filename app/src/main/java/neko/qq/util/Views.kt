package neko.qq.util

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * 视图遍历工具。用于「不依赖 QQ 内部类」地完成 UI 注入：
 * 通过文字 / 类名在视图树里定位控件，并插入我们自己的行或按钮。
 */
object Views {

    /** 广度优先查找文字包含 [text] 的 TextView */
    fun findTextViewWithText(root: View?, text: String): TextView? {
        if (root == null) return null
        val queue = ArrayDeque<View>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            if (v is TextView) {
                val t = v.text?.toString().orEmpty()
                if (t.contains(text)) return v
            }
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) queue.add(v.getChildAt(i))
            }
        }
        return null
    }

    /** 向上回溯，找到第一个满足 [predicate] 的祖先 View（用于定位「整行」容器） */
    fun findAncestor(view: View?, predicate: (View) -> Boolean): View? {
        var p = view?.parent
        while (p is View) {
            if (predicate(p)) return p
            p = p.parent
        }
        return null
    }

    /** 向上找到可点击的行容器（通常是某层 ViewGroup，自身或其父级可点击） */
    fun findClickableRow(view: View?): View? =
        findAncestor(view) { it.isClickable || (it is ViewGroup && it.childCount > 0) }

    /** 判断 ViewGroup 里是否已经注入过指定 tag 的子 View */
    fun hasInjected(parent: ViewGroup, tag: String): Boolean {
        for (i in 0 until parent.childCount) {
            if (parent.getChildAt(i).tag == tag) return true
        }
        return false
    }
}
