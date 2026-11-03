package neko.qq.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import neko.qq.R

/**
 * Neko设置 主页面（模块自身 Activity，也在 QQ 设置里被入口跳转至此）。
 * 写入的偏好位于 neko_qq_prefs，由运行在 QQ 进程的 Prefs 实时读取。
 */
class NekoSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_neko_settings)

        // 初始化 Toolbar，配置返回按钮
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // 加载设置 Fragment
        // 注意：这里的 R.id.container 必须和你 activity_neko_settings.xml 中的 Fragment 容器 ID 一致
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, NekoSettingsFragment())
            .commit()
    }
}
