package neko.qq.util

import android.os.Handler
import android.os.Looper
import android.widget.Toast

/** 在任何线程（含 hook 回调）安全地弹 Toast */
object Toasts {
    fun show(text: String, long: Boolean = false) {
        Handler(Looper.getMainLooper()).post {
            runCatching {
                Toast.makeText(
                    Contexts.appContext(),
                    text,
                    if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
