
package neko.qq.util

import android.content.Context

object Contexts {

    @Volatile
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun appContext(): Context {
        return checkNotNull(applicationContext) {
            "Application Context 尚未初始化"
        }
    }

    fun moduleContext(): Context =
        appContext().createPackageContext(
            "neko.qq",
            Context.CONTEXT_IGNORE_SECURITY
        )

    fun density(): Float =
        appContext().resources.displayMetrics.density
}
