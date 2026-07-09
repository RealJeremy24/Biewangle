package com.biewangle.dontforget

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.biewangle.dontforget.ui.navigation.BiewangleNavHost
import com.biewangle.dontforget.ui.theme.BiewangleTheme

class MainActivity : ComponentActivity() {

    // 请求通知权限（Android 13+需要）
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(this, "提醒功能已开启", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "无法接收提醒通知，请在设置中开启通知权限", Toast.LENGTH_LONG).show()
        }
    }

    // 请求精确闹钟权限（Android 12+需要）
    private val exactAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    // 请求全屏通知权限（Android 14+需要）
    private val fullScreenIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 用户从系统设置返回后再次检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!nm.canUseFullScreenIntent()) {
                Toast.makeText(this, "未开启全屏通知，到时提醒只能从通知栏下拉", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 任意页面按一次返回键直接退到系统桌面（不再回退到 Splash）。
        // 注意：这里用 moveTaskToBack(false) 而非 finish()。
        // 某些国产 ROM（含 HarmonyOS）在销毁带 LAUNCHER intent-filter 的 Activity 后，会
        // 自动以启动器意图重启，导致 Splash 重新播放——走 finish 用户体感还是看到 Splash。
        // moveTaskToBack 把整个 task 推到 home 之后但保留 Activity，OS 不会触发自动重启。
        // 拨号器层：dispatchKeyEvent 在 OnBackPressedDispatcher 之前获得 KeyEvent，是最后的兜底。
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveTaskToBack(false)
                }
            }
        )

        // 请求必要权限
        requestNecessaryPermissions()

        // 请求电池优化白名单（国产 ROM 保证后台闹钟可靠性）
        requestBatteryOptimizationExemption()

        // 设置 Compose 界面
        setContent {
            BiewangleTheme {
                BiewangleNavHost()
            }
        }
    }

    /**
     * 兜底拦截：在 dispatcher 之前捕获 KEYCODE_BACK。
     * 如果 dispatcher 因任何原因（ROM 差异 / 优先级竞态）没回调，回这里同样把 task 推到 home。
     */
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return if (event?.keyCode == KeyEvent.KEYCODE_BACK &&
            event.action == KeyEvent.ACTION_UP
        ) {
            moveTaskToBack(false)
            true
        } else {
            super.dispatchKeyEvent(event)
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // 降级：打开电池优化设置页
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun requestNecessaryPermissions() {
        // Android 13+ 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Android 12+ 精确闹钟权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "请允许「别忘了」设置精确闹钟，以确保准时提醒",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    exactAlarmLauncher.launch(intent)
                } catch (e: Exception) {
                    // 降级使用非精确闹钟
                }
            }
        }

        // Android 14+ 全屏通知权限（让闹钟到点能直接弹出全屏）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!nm.canUseFullScreenIntent()) {
                Toast.makeText(
                    this,
                    "请允许「别忘了」使用全屏通知，到时能直接弹出提醒",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    fullScreenIntentLauncher.launch(intent)
                } catch (e: Exception) {
                    // 降级：跳到应用详情页
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
