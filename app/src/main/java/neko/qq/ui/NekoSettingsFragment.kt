package neko.qq.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import neko.qq.R
import neko.qq.constants.PrefKeys

/**
 * Neko设置 偏好页面：防撤回 / 高级群管 / 表情面板 的全部开关与子项。
 * 使用独立偏好文件 neko_qq_prefs，与 QQ 进程中 Prefs 读取同一份数据。
 */
class NekoSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // 指定偏好文件名，确保与 QQ 进程读取的是同一份
        preferenceManager.sharedPreferencesName = PrefKeys.PREFS_NAME
        setPreferencesFromResource(R.xml.neko_prefs, rootKey)
    }
}
