# Biewangle（别忘乐）— 备忘录/提醒 Android 应用

---

## 🚨 核心规则（必读，违反就是搞破坏）

1. **不要大改架构**：保持 MVVM + Repository + Service Locator 模式。不要引入 Hilt/Dagger/Koin 等 DI 框架，不要换成 MVI/MVP，不要加不必要的抽象层。现有架构是刻意简化的——面向个人开发者维护，不是企业级项目。

2. **我没说改的就千万不要改**：只改我明确要求改的东西。不要顺手"优化"、不要重构你没被要求动的代码，有优化建议可以先跟我商量，我同意了再动手。

3. **改之前先确认范围**：如果不确定改动会影响哪些文件，先问。改完只验证相关功能，不要"顺便跑一下全局测试"。

4. **新增依赖要三思**：这是一个离线应用，无网络权限。不要引入需要网络的库。新增 Gradle 依赖前确认必要性。

5. **UI 改动要保守**：目标用户是中老年人，大字、暖色、简单交互。不要加复杂手势、不要缩小字体、不要换冷淡色调。

6. **新加的图片/资源必须和已有同类型资源风格一致**：app 里有**已建立的人物形象/视觉语言**（如 `mama2_clean.png` 妈妈贴纸：东亚中年女性 + 黑发低马尾 + 抱西瓜 + 黑色 T-shirt + 绿色背景 + 红爱心黄星星 + 写实感卡通）。新生成的贴纸、图标、插画必须**严格对齐**这个视觉语言（人物造型、服饰、姿势、配色、装饰元素），不要凭空画新人物或换画风。需要让 AI 出图时，**喂所有已有的同类参考图**，出图后用 `Biewangle_零碎/妈妈贴纸动效GIF-任务说明.md` 末尾的验收清单逐项核对，任一不过就重出。**违反这条 = 搞破坏**。

7. **"存档"默认指 git commit**：用户说"存档"而无其他特指时，应执行 `git add -A && git commit`，不需要先询问。涉及文件删除、远程操作等明确说明后再执行。

---

## 📁 项目文件地图

```
Biewangle/
├── CLAUDE.md                          ← 你正在看的文件
├── app/build.gradle.kts               ← 应用级构建配置
├── build.gradle.kts                   ← 根构建配置
├── settings.gradle.kts                ← 项目设置
├── gradle/wrapper/                    ← Gradle Wrapper（版本 9.4.1）
└── app/src/main/
    ├── AndroidManifest.xml
    ├── res/                           ← 资源文件（strings, drawable, etc.）
    └── java/com/biewangle/dontforget/
        ├── BiewangleApp.kt            ← Application，Service Locator，全局 fontScale
        ├── MainActivity.kt            ← 唯一 Activity，Compose 入口
        ├── data/
        │   ├── db/AppDatabase.kt      ← Room 数据库定义
        │   ├── dao/MemoDao.kt         ← 备忘录 DAO
        │   ├── dao/SettingsDao.kt     ← 设置 DAO
        │   ├── entity/MemoEntity.kt   ← 备忘录 Room Entity
        │   ├── entity/SettingsEntity.kt ← 设置 Room Entity
        │   ├── model/Memo.kt          ← 备忘录领域模型（UI 层使用）
        │   ├── model/AppSettings.kt   ← 设置领域模型
        │   ├── model/RepeatType.kt    ← 重复类型枚举
        │   ├── repository/MemoRepository.kt    ← 备忘录仓库
        │   └── repository/SettingsRepository.kt ← 设置仓库
        ├── service/
        │   ├── AlarmScheduler.kt      ← 闹钟调度（WorkManager）
        │   ├── AlarmReceiver.kt       ← 闹钟广播接收器
        │   ├── AlarmForegroundService.kt ← 提醒前台服务（显示全屏弹窗）
        │   ├── ReminderPlayer.kt      ← 铃声播放器（MediaPlayer）
        │   └── BootReceiver.kt        ← 开机自启恢复闹钟
        ├── ui/
        │   ├── navigation/AppNavigation.kt ← 导航图
        │   ├── theme/Color.kt         ← 暖色调色板
        │   ├── theme/Type.kt          ← 字体排版
        │   ├── theme/Theme.kt         ← Material 3 主题
        │   ├── components/            ← 可复用组件
        │   │   ├── MemoCard.kt        ← 备忘录卡片
        │   │   ├── StatsCard.kt       ← 统计卡片
        │   │   ├── LargeButton.kt     ← 大按钮（中老年友好）
        │   │   ├── QuickTemplateChip.kt ← 快速模板标签
        │   │   └── TimePickerDialog.kt  ← 时间选择器弹窗
        │   └── screens/
        │       ├── splash/SplashScreen.kt   ← 启动页
        │       ├── memo/MemoScreen.kt       ← 备忘录列表页
        │       ├── memo/MemoViewModel.kt    ← 备忘录 ViewModel
        │       ├── memo/AddMemoScreen.kt    ← 添加/编辑备忘录
        │       ├── memo/RingtonePickerDialog.kt ← 铃声选择
        │       ├── settings/SettingsScreen.kt   ← 设置页
        │       ├── settings/SettingsViewModel.kt ← 设置 ViewModel
        │       ├── settings/AudioTrimmerDialog.kt ← 录音裁剪
        │       └── alarm/AlarmActivity.kt   ← 闹钟提醒弹窗页
        ├── util/
        │   ├── Constants.kt           ← 全局常量
        │   ├── DateTimeUtils.kt       ← 日期时间工具
        │   └── AudioTrimmer.kt        ← 音频裁剪逻辑
        └── widget/
            └── TodayMemoWidget.kt     ← 桌面小组件（Glance）
```

---

## 技术栈

- Kotlin + Jetpack Compose (Material 3)
- Room 数据库
- MVVM + Repository 架构，手动 DI (Service Locator，见 `BiewangleApp.kt`)
- Glance AppWidget + WorkManager
- 完全离线，无网络权限

## 构建

```bash
./gradlew assembleDebug
# APK 输出: app/build/outputs/apk/debug/app-debug.apk
```

**必须用 `./gradlew` 而不是 `gradle`**。项目需要 Gradle 9.4.1（wrapper 已配置），系统全局 Gradle 版本不符会导致 `Minimum supported Gradle version is 9.4.1` 错误。

## ADB 路径

```bash
/d/Android/Sdk/platform-tools/adb
```

---

## ⚠️ 踩坑记录（改之前先看，避免重蹈覆辙）

### Kotlin/协程
- **`runBlocking` 禁止用于数据库读取**：会阻塞主线程导致 ANR。用 `CoroutineScope(Dispatchers.IO).launch` 替代。Application.onCreate、铃声播放、震动的入口已全部改为异步。
- **`ReminderPlayer` 的 `playerScope` 在 `stop()` 时取消**：不要额外创建无管理的协程去操作 MediaPlayer，否则 stop() 后协程还在跑会崩溃。

### Room 数据库
- **数据库操作必须在 IO 线程**：Room 默认不在主线程执行查询会抛异常，确保 DAO 调用都在协程的 IO dispatcher 下。

### AppWidget (Glance)
- **`onUpdate` 用 `goAsync()` 绑定生命周期**：与 `BootReceiver` 模式一致，否则 widget 更新可能在 receiver 结束后被系统杀掉。
- **Glance 不支持所有 Compose 组件**：只支持 Glance 限定的子集，不要直接复用普通 Compose UI。

### 通知与前台服务
- **前台服务在某些国产 ROM 上可能无法启动**：NotificationChannel 已设置震动作为降级路径。
- **Android 14+ 前台服务类型必须声明**：AlarmForegroundService 在 Manifest 中需要 `foregroundServiceType="mediaPlayback"`。

### 构建
- **必须用 `./gradlew`**：系统全局 Gradle 版本和 wrapper 版本不匹配会直接报错。
- **Windows 路径**：桌面是 `/d/Desktop`，不是 `/c/Users/11447/Desktop/`。

---

## 目标用户

面向中老年用户，大字体、暖色调、简化交互。

---

## 📚 项目调研记录

> 重要技术决策、踩坑记录放在 `docs/` 目录下。当前文档：

- **[alarm-fullscreen-investigation.md](docs/alarm-fullscreen-investigation.md)** — HarmonyOS 上第三方 App **无法自动全屏弹 Activity** 的系统级限制（已尝试 4 套方案全部拦截）。当前 v1.9 退而求其次：通知 + 铃声 + 用户点通知进全屏。**接到"全屏弹提醒"相关需求时必读**。

