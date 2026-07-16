# 🌸 NekoQQ · 手机 QQ 增强模块

一个基于 **LSPosed（Xposed）** 的安卓 QQ（QQNT）增强模块，在 QQ 设置里新增 **「Neko设置」**（位于「账号与安全」上方），并提供大量增强功能。支持**自动拉取**最新 hook 配置，跨 QQ 版本免重装适配。

---

## 一、功能总览

### 基础三大功能
| 功能 | 说明 |
|------|------|
| 🛡️ 防撤回 | 对方撤回消息时拦截并保留原文，弹出提示横幅 |
| 👮 高级群管 | 在群聊中点击成员头像，弹出菜单进行禁言 / 封禁 / 踢出 / 设管理员 |
| 😺 表情面板 | 在聊天输入栏左侧增加表情按钮，一键发送大量 emoji 与颜文字 |

### 显示与界面
| 功能 | 说明 |
|------|------|
| 篡改预览字体大小 | 聊天/空间预览使用的字体大小（sp） |
| 强制平板模式 | 强制 QQ 以平板布局运行 |
| 强制手机模式 | 强制 QQ 以手机布局运行 |
| 修改 DPI | 覆盖系统 DPI |
| 强制使用默认字体 | 忽略 QQ 内置字体，使用系统默认 |
| 自定义启动图列数 | 动态/表情面板启动图的列数 |
| 随机气泡 | 每次发送随机使用不同气泡样式 |
| 自定义表情面板 | 启用可编辑/上传的表情面板 |
| 允许透明头像/表情 | 上传时跳过圆角裁切与底色填充 |

### 隐私与去广告
| 功能 | 说明 |
|------|------|
| 禁用 QQ 秀 | 关闭个人资料中的 QQ 秀展示 |
| 隐藏 QQ 空间广告 | 隐藏空间内的广告 |
| 隐藏表情表态 | 不显示他人对消息的表情表态 |
| 移除消息表态 | 彻底关闭消息表态功能入口 |
| 移除顶栏横幅广告 | 移除消息列表顶部的横幅推广 |
| 禁用崩溃日志上报 | 关闭 Bugly 等崩溃上报 |
| 禁用子渠道通知 | 关闭公众号/子渠道推送通知 |
| 禁用通知气泡 | 关闭悬浮通知气泡 |
| 禁用交互式弹窗 | 屏蔽特定消息触发的互动弹窗 |
| 可选使用系统相册 | 选图/发图时调用系统相册 |

### 社交与功能
| 功能 | 说明 |
|------|------|
| 显示空间动态 | 在对应入口直接展示 QQ 空间动态 |
| @ 列表重新排序 | 按内容重排 @ 成员列表 |
| 禁用左滑回复震动 | 关闭左滑回复的震动反馈 |
| 去除转发人数限制 | 解除转发时 9 名联系人上限 |
| 消息 +1 Plus | 消息列表数字角标 +1 增强显示 |
| 移除表情回应限制 | 解除单条消息可加的表情回应数量上限 |
| 篡改上传运动步数 | 上报到 QQ 运动的步数自定义 |
| 小程序分享转链接 | 接收小程序分享时转为网页链接 |
| 卡片消息文本化 | 把卡片/分享消息渲染为纯文本 |

### 模块
- **模块设置入口**：Neko设置（位于「账号与安全」上方）
- **自动拉取**：启动时自动拉取仓库内最新 hook 配置，跨版本免重装适配

---

## 二、环境要求

- Android 7.0+（推荐 12+）
- 已 Root，并安装 **LSPosed** 框架（Magisk + LSPosed 模块）
- 目标 App：**手机 QQ（QQNT，com.tencent.mobileqq）**
- 编译环境：Android Studio + JDK 17

---

## 三、编译与安装

```bash
# 用 Android Studio 打开本工程根目录（NekoQQ/）
./gradlew assembleDebug
```

1. 生成的 APK：`app/build/outputs/apk/debug/app-debug.apk`
2. 安装到手机（这就是「模块本体」，同时提供桌面「Neko设置」图标）
3. 打开 **LSPosed → 模块**，勾选 **NekoQQ**
4. 在 LSPosed 里把 **手机 QQ** 加入 NekoQQ 的「作用域」
5. **重启手机 QQ**（强制停止后重新打开）
6. 打开 QQ → 设置 → 应能看到「Neko设置」位于「账号与安全」上方

> 若注入入口未出现，也可直接打开桌面的「Neko设置」App 进行配置（功能开关同样生效）。

---

## 四、工程结构

```
NekoQQ/
├── app/src/main/
│   ├── AndroidManifest.xml        # LSPosed 模块声明 + 权限 + Neko设置 Activity
│   ├── assets/
│   │   ├── xposed_init            # 模块入口类：neko.qq.MainHook
│   │   └── hook_config.json       # 自动拉取用的 hook 覆盖配置示例
│   ├── res/xml/neko_prefs.xml     # 全部功能的开关与子项
│   └── java/neko/qq/
│       ├── MainHook.kt            # 模块入口，挂载各 hook + 自动拉取
│       ├── constants/
│       │   ├── QQConfig.kt        # ⭐ QQ 内部类/方法签名集中配置（版本校准在这里）
│       │   └── PrefKeys.kt        # 偏好 key
│       ├── hooks/
│       │   └── InjectSettingsEntry.kt  # 视图遍历注入「Neko设置」入口
│       ├── features/
│       │   ├── AntiRecall.kt / GroupManager.kt / EmojiPanel.kt  # 三大基础功能
│       │   ├── Display.kt / Privacy.kt / Social.kt              # 增强功能
│       │   └── AutoPull.kt        # 自动拉取最新 hook 配置
│       ├── ui/                    # Neko设置 Activity + PreferenceFragment
│       └── util/                  # Contexts / Prefs / Views / Toasts / Hooker / HookOverrides
```

---

## 五、关于「版本校准」（重要）

QQNT 每次发版都会重命名内部类，因此 `features/*` 里的 hook 点位都做成了
**「候选列表 + 自动尝试」**：命中任意一个即用，全未命中则安全跳过（不会让 QQ 闪退）。

校准步骤：

```bash
adb logcat -s NekoQQ
```

观察日志，模块会打印每个 hook 的命中情况，例如：
```
[NekoQQ] 防撤回 已 hook: com.tencent.xxx.YYY.onMsgRevoked
[NekoQQ] 未命中任何设置页候选类 ...
```

把日志里**真实存在**的类名/方法名，填进 `constants/QQConfig.kt` 对应列表的最前面即可。

---

## 六、自动拉取（AutoPull）

为避免「QQ 一更新模块就失效」，NekoQQ 支持从仓库自动拉取最新 hook 配置：

1. 维护者发现某功能在新版 QQ 的命中点位变化后，更新仓库里的
   `app/src/main/assets/hook_config.json`（把更好的 `clazz/method` 写进对应 featureKey）。
2. 用户端 QQ 启动时：
   - 先**同步应用本地缓存**的覆盖（来自上一次拉取），立即生效；
   - 再**后台刷新**仓库最新配置并写缓存，**下次启动**生效。
3. 各功能 hook 优先使用覆盖点位，其次用代码内置的候选默认值。

`hook_config.json` 格式：
```json
{
  "version": 1,
  "overrides": {
    "preview_font_size": [
      { "clazz": "com.tencent.mobileqq.text.QQText", "method": "getPaint", "paramTypes": [] }
    ]
  }
}
```
> featureKey 必须与代码中的 key 一致（见各 `features/*.kt`）。
> 默认拉取地址指向 `NekoAiDev/NekoQQ`，可在 Neko设置 → 模块 → 自动拉取地址 中修改。

---

## 七、各功能实现要点

### Neko设置入口（InjectSettingsEntry）
- 不依赖 QQ 内部类：hook 设置页 `onCreate`，用**视图遍历**找到「账号与安全」那一行，
  在其**上方**插入一行可点击的 Neko设置。点击通过 `Intent.setClassName` 跳转模块设置页。

### 防撤回（AntiRecall）
- hook 候选的「撤回处理」方法，命中后弹提示横幅。
- `recallKeepContent` 控制是否保留原文；真正「保住气泡」需在 `onRecall()` 里解析消息对象并
  `param.setResult(null)` 阻止默认移除（版本相关，见 TODO）。

### 高级群管（GroupManager）
- hook 候选的「打开成员资料 / 列表项点击」方法，弹出操作菜单。
- 危险操作受二次确认保护；真正下发指令调用 `GROUP_ACTION_CANDIDATES` 候选方法（需校准）。

### 表情面板（EmojiPanel）
- hook `Activity.onResume`，按类名筛选聊天界面，用视图遍历找到输入栏 EditText，
  在其左侧插入 😀 按钮，点击弹出 PopupWindow 表情面板。

### 显示 / 隐私 / 社交（Display / Privacy / Social）
- 统一通过 `util/Hooker` 批量 hook 候选点位；返回型方法用 `setResult` 短路/篡改，
  void 方法的彻底拦截（崩溃上报、通知等）需抛异常或 hook 上层调用方，已留 `TODO(版本校准)`。

---

## 八、已知限制 / 后续可扩展

- 部分 QQNT 版本的设置/聊天为 WebView 渲染，视图遍历注入可能失效——此时可用桌面「Neko设置」图标进入。
- 防撤回「保留原气泡」、群管「真实下发」、若干 void 方法的「彻底拦截」依赖版本校准，属预期工作量。
- 可扩展：消息防抖、自动回复、群管定时任务、表情自定义上传等。
