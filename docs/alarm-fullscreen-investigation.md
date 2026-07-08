# 闹钟全屏弹 Activity 调研记录

> 调研时间：2026-07
> 适用版本：v1.8 / v1.9
> 结论：**第三方 App 在 HarmonyOS / 国产 ROM 上无法做到"系统闹钟"那种自动全屏弹 Activity**

---

## 一、需求背景

Biewangle 是面向中老年用户的备忘录/提醒 Android 应用（offline only）。用户反馈：
> "希望到时间铃声提醒的时候，直接弹出全屏的提醒，就如同手机系统自带的闹钟一样"

期望行为：闹钟到时间 → 自动全屏弹出 AlarmActivity（覆盖锁屏、覆盖当前任务），用户看到滑动结束提醒按钮。

## 二、最终结论

**HarmonyOS（以及大多数国产 ROM）在 WindowManager 之上做了 Activity 启动拦截**。第三方 App 无系统签名，应用层做的所有"主动启动 Activity"尝试都会被拦截。

**只有系统签名应用**（如系统时钟）能绕过这层拦截。

**实际能做到的天花板**：
- ✅ 闹钟到时间 → 通知 + 铃声 + 震动
- ✅ 通知展开后显示完整内容
- ✅ 锁屏上显示完整通知内容
- ✅ **点通知 → 进入全屏 AlarmActivity**（用户主动操作）
- ❌ **自动全屏弹**（系统级拦截，无解）

## 三、尝试过的方案

### 方案 A：通知 setFullScreenIntent
```kotlin
NotificationCompat.Builder(...).setFullScreenIntent(pendingIntent, true)
```
**结果**：HarmonyOS 拦截，通知显示但不自动弹全屏。

**原因**：HarmonyOS 在通知层做了特殊处理，FullScreenIntent 只能由系统签名应用触发。

### 方案 B：AlarmReceiver.onReceive 内 startActivity
```kotlin
context.startActivity(Intent(context, AlarmActivity::class.java).apply { ... })
```
**结果**：HarmonyOS 锁屏时拦截；亮屏时也偶发拦截。

**原因**：BroadcastReceiver 启动 Activity 在 Google 文档里被允许，但 HarmonyOS 在底层拦截。

### 方案 C：AlarmForegroundService.onStartCommand 内 startActivity
```kotlin
override fun onStartCommand(...) {
    startForeground(...)
    startActivity(Intent(this, AlarmActivity::class.java).apply { ... })
}
```
**结果**：HarmonyOS 也拦截。

**原因**：虽然前台服务有"前台例外"权限，但 HarmonyOS 拦截力度超过 Android 规范。

### 方案 D：独立任务栈 + WakeLock
- `launchMode="singleInstance"`
- `taskAffinity=""`
- WakeLock `SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP` (15 秒)

**结果**：
- WakeLock ✅ 成功唤醒屏幕
- 独立任务栈 ❌ 仍被拦截

**原因**：HarmonyOS 的拦截在 WindowManager 之上，独立任务栈也不能绕过。

### 方案 E：setAlarmClock + showIntent 指向 Activity
```kotlin
alarmManager.setAlarmClock(info, showPendingIntent)  // showPendingIntent 指向 Activity
```
**结果**：无效，showIntent 只在用户**点击状态栏闹钟图标**时启动 Activity。

**原因**：Android 文档实际行为与早期理解不一致，showIntent 不会在闹钟到时自动启动。

## 四、最终交付方案（v1.9）

既然系统拦截全屏，改为让**通知本身承担主要提醒职责**：

```kotlin
NotificationCompat.Builder(context, CHANNEL_REMINDER)
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle(title)
    .setContentText(content)
    .setStyle(NotificationCompat.BigTextStyle().bigText(content))  // 展开显示完整
    .setPriority(NotificationCompat.PRIORITY_MAX)
    .setCategory(NotificationCompat.CATEGORY_ALARM)
    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)              // 锁屏可见
    .setShowWhen(true)
    .setFullScreenIntent(fullScreenPendingIntent, true)              // 兜底（部分 ROM 可能支持）
    .setContentIntent(fullScreenPendingIntent)                       // 点击通知进全屏
    .addAction(... "关闭" ...)
    .addAction(... "稍后提醒" ...)
```

**channel 锁屏可见性**：
```kotlin
NotificationChannel(CHANNEL_REMINDER, ..., IMPORTANCE_HIGH).apply {
    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
}
```

## 五、用户操作路径（v1.9 实际体验）

1. 闹钟到时间 → 通知 + 铃声 + 震动（横幅弹出）
2. 用户在锁屏看到通知（显示完整内容）
3. **下拉通知 / 点击通知正文** → 进入全屏 AlarmActivity
4. 在 AlarmActivity 上滑动结束 / 点稍后提醒

## 六、关键 commit

- `5b9be3e` v1.8: 尝试 4 套路径全屏弹（Receiver/Service startActivity + WakeLock + 独立任务栈）
- `a93aafb` v1.9: 通知体验优化（BigTextStyle + 锁屏可见 + 主操作）

## 七、未来如果 HarmonyOS 解除拦截

如果未来 HarmonyOS 解除拦截，可以重新启用以下代码（已存在但被 try-catch 兜底）：
- `AlarmReceiver.onReceive` 内的 `context.startActivity`
- `AlarmForegroundService.onStartCommand` 内的 `startActivity`
- `NotificationCompat.Builder.setFullScreenIntent`

## 八、给其他 AI / 后续维护者的建议

1. **不要在 HarmonyOS 上花时间尝试自动全屏弹 Activity** —— 已确认是系统级拦截
2. **把精力放在通知体验优化** —— BigTextStyle + 锁屏可见 + 点击进入是用户实际能用的路径
3. **如果用户坚持要自动全屏弹**，需要明确告知：这是 HarmonyOS 限制，需要换系统或接受现状
4. **如果未来需要"系统级闹钟"体验**，考虑接入 HarmonyOS 的 Push Kit（需要华为开发者账号和 HMS Core）
