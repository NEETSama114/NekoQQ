package neko.qq.util

import neko.qq.constants.HookPoint

/**
 * 运行时 hook 覆盖表。
 * 「自动拉取」从仓库下载最新 hook_config.json 后，把其中的候选点位写到这里，
 * 各功能 hook 优先使用覆盖值，从而达到「跨 QQ 版本免重装适配」。
 */
object HookOverrides {
    private val map = mutableMapOf<String, List<HookPoint>>()

    fun load(overrides: Map<String, List<HookPoint>>) {
        map.clear()
        map.putAll(overrides)
    }

    fun get(featureKey: String): List<HookPoint>? = map[featureKey]

    fun size(): Int = map.size

    fun keys(): Set<String> = map.keys
}
