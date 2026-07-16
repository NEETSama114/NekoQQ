package neko.qq.features

import de.robv.android.xposed.XposedBridge
import neko.qq.constants.HookPoint
import neko.qq.constants.QQConfig
import neko.qq.util.Contexts
import neko.qq.util.HookOverrides
import neko.qq.util.Prefs
import org.json.JSONObject
import java.io.File
import java.net.URL

/**
 * 自动拉取：启动时同步应用本地缓存的 hook 配置，并后台刷新仓库内最新配置。
 *
 * 这样当 QQ 更新导致内部类重命名时，维护者只需在仓库更新 hook_config.json，
 * 用户重启 QQ 即可获得新点位，无需重装模块。
 *
 * JSON 格式（overrides 的 key 必须与代码中的 featureKey 一致）：
 * {
 *   "version": 1,
 *   "overrides": {
 *     "preview_font_size": [ { "clazz": "...", "method": "...", "paramTypes": [] } ]
 *   }
 * }
 */
object AutoPull {
    private const val TAG = "NekoQQ:autopull"
    private const val CACHE_NAME = "hook_config.cache.json"

    /** QQ 启动时同步加载本地缓存（若存在），使上一次拉取的覆盖立即生效 */
    fun init() {
        if (!Prefs.autoPull) return
        val cache = cacheFile() ?: return
        if (cache.exists()) runCatching { applyJson(cache.readText()) }
            .onFailure { XposedBridge.log("[$TAG] 读取缓存失败: ${it.message}") }
    }

    /** 后台刷新：下载最新配置并写缓存（下次启动生效） */
    fun refresh() {
        if (!Prefs.autoPull) return
        Thread {
            runCatching {
                val url = Prefs.autoPullUrl.ifBlank { QQConfig.DEFAULT_HOOK_CONFIG_URL }
                val json = URL(url).readText()
                cacheFile()?.writeText(json)
                applyJson(json)
                XposedBridge.log("[$TAG] 已拉取并应用最新配置，覆盖 ${HookOverrides.size()} 项：${HookOverrides.keys()}")
            }.onFailure { XposedBridge.log("[$TAG] 拉取失败: ${it.message}") }
        }.start()
    }

    private fun applyJson(json: String) {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return
        val overrides = root.optJSONObject("overrides") ?: return
        val map = mutableMapOf<String, List<HookPoint>>()
        overrides.keys().forEach { key ->
            val arr = overrides.optJSONArray(key) ?: return@forEach
            val list = mutableListOf<HookPoint>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val clazz = o.optString("clazz")
                val method = o.optString("method")
                val types = o.optJSONArray("paramTypes")?.let { a ->
                    Array(a.length()) { a.optString(it) }
                } ?: emptyArray()
                if (clazz.isNotEmpty() && method.isNotEmpty()) list.add(HookPoint(clazz, method, types))
            }
            if (list.isNotEmpty()) map[key] = list
        }
        HookOverrides.load(map)
    }

    private fun cacheFile(): File? = runCatching {
        File(Contexts.moduleContext().filesDir, CACHE_NAME)
    }.getOrNull()
}
