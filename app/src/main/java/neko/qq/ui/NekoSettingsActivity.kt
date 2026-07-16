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

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, NekoSettingsFragment())
            .commit()
    }
}
