[README.md](https://github.com/user-attachments/files/29913305/README.md)
# 别忘乐

"别忘乐"谐音"别忘了"，也有别忘“乐”的寓意。  
一款面向中老年用户的备忘录/提醒 Android 应用，大字体、暖色调、简单交互，完全离线无广告。  
同时最重要的也是为了在作者26岁之年和母亲52岁之年开发一款app来定格纪念这一刻，所以这也是一款我会誉之为“被关注爱”的App，也祝大家身体健康，阖家幸福，四代同堂！

## 主要功能

### 事项管理
- 添加、编辑、删除待办事项
- 支持设置提醒日期和时间
- 支持重复提醒（每天 / 每周 / 每月）
- 快捷模板标签：💊 吃药 / 🏥 复诊 / 🛒 买菜 / 📞 电话 / 🚶 锻炼

### 提醒通知
- 到期提醒弹窗通知（可显示在锁屏上）
- 自定义提醒铃声（支持裁剪音频片段）
- 稍后提醒（10分钟后再响）
- 开机自动恢复已设置的闹钟

### 统计与鼓励
- 今日完成率统计卡片
- 根据完成进度展示不同鼓励语（🌱 / 💪 / 🌟）

### 桌面小组件
- 今日待办事项桌面小组件（Glance AppWidget）

## 技术栈

- **Kotlin + Jetpack Compose** (Material 3)
- **Room** 本地数据库
- **MVVM + Repository** 架构
- **WorkManager** 闹钟调度
- **Glance AppWidget** 桌面小组件
- 完全离线，无网络权限

## 项目结构

```
app/src/main/
├── java/com/biewangle/dontforget/
│   ├── BiewangleApp.kt          # Application，Service Locator
│   ├── MainActivity.kt           # 唯一 Activity
│   ├── data/                     # 数据层
│   │   ├── db/                   # Room 数据库
│   │   ├── dao/                  # Data Access Object
│   │   ├── entity/               # Room Entity
│   │   ├── model/                # 领域模型
│   │   └── repository/           # 仓库模式
│   ├── service/                  # 闹钟服务
│   │   ├── AlarmScheduler.kt     # 闹钟调度
│   │   ├── AlarmReceiver.kt      # 广播接收器
│   │   ├── AlarmForegroundService.kt # 前台服务
│   │   └── BootReceiver.kt       # 开机自启
│   ├── ui/                       # 界面层
│   │   ├── components/           # 可复用组件
│   │   ├── screens/              # 各页面
│   │   └── theme/               # 主题配色
│   └── widget/                   # 桌面小组件
└── res/                          # 资源文件
```

## 构建

```bash
./gradlew assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

## 环境要求

- Android SDK 34
- Android 8.0（API 26）及以上
- Gradle 9.4.1（使用项目内置 wrapper）

## 开发说明

- 不引入任何网络依赖，完全离线
- 使用 Service Locator 进行手动依赖注入
- 目标用户为中老年，UI 保持大字、暖色、简单交互
