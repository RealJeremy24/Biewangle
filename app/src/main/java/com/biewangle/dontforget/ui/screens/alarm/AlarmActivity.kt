package com.biewangle.dontforget.ui.screens.alarm

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.BiewangleApp
import com.biewangle.dontforget.service.AlarmForegroundService
import com.biewangle.dontforget.service.AlarmScheduler
import com.biewangle.dontforget.ui.theme.AlertOrangeRed
import com.biewangle.dontforget.ui.theme.BackgroundWarm
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.TextDarkBrown
import com.biewangle.dontforget.ui.theme.TextWarmGray
import com.biewangle.dontforget.ui.theme.WhiteText
import com.biewangle.dontforget.ui.theme.scaledSp
import com.biewangle.dontforget.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    private var memoId: Long = -1L
    private var title: String = ""
    private var content: String = ""
    private var useCustomRingtone: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏、点亮屏幕、覆盖锁屏
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }

        memoId = intent?.getLongExtra(Constants.EXTRA_MEMO_ID, -1L) ?: -1L
        title = intent?.getStringExtra(Constants.EXTRA_MEMO_TITLE) ?: "提醒"
        content = intent?.getStringExtra(Constants.EXTRA_MEMO_CONTENT) ?: ""
        useCustomRingtone = intent?.getBooleanExtra(Constants.EXTRA_USE_CUSTOM_RINGTONE, true) ?: true

        // 启动前台服务播放铃声
        AlarmForegroundService.start(this, memoId, title, content, useCustomRingtone)

        setContent {
            AlarmScreen(
                title = title,
                content = content,
                onDismiss = { dismissAlarm() },
                onSnooze = { snoozeAlarm() }
            )
        }
    }

    private fun dismissAlarm() {
        // 停止铃声
        AlarmForegroundService.stop(this)

        // 取消通知
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(memoId.toInt())

        // 完成事项
        CoroutineScope(Dispatchers.IO).launch {
            val app = BiewangleApp.instance
            val memo = app.memoRepository.getById(memoId)
            if (memo != null) {
                app.memoRepository.toggleComplete(memoId)
            }
        }

        finish()
    }

    private fun snoozeAlarm() {
        // 停止铃声
        AlarmForegroundService.stop(this)

        // 取消通知
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(memoId.toInt())

        // 10分钟后重新提醒
        val snoozeTime = System.currentTimeMillis() + Constants.SNOOZE_DELAY_MS
        val scheduler = AlarmScheduler(this)
        scheduler.schedule(memoId, title, content, snoozeTime, useCustomRingtone)

        finish()
    }

    override fun onBackPressed() {
        // 禁止返回键
    }
}

@Composable
fun AlarmScreen(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 图标
            Text("🔔", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))

            // 标题
            Text(
                text = "⏰ 提醒时间到",
                fontSize = scaledSp(32),
                fontWeight = FontWeight.Bold,
                color = AlertOrangeRed,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // 事项卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = scaledSp(28),
                    fontWeight = FontWeight.Bold,
                    color = TextDarkBrown,
                    textAlign = TextAlign.Center
                )

                if (content.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = content,
                        fontSize = scaledSp(22),
                        color = TextWarmGray,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("💊", fontSize = 36.sp)
            }

            Spacer(Modifier.height(24.dp))

            // 播放状态
            Text(
                text = "🔔 铃声正在循环播放…",
                fontSize = scaledSp(22),
                color = AlertOrangeRed,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(32.dp))

            // 我知道了 按钮（超大）
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertOrangeRed,
                    contentColor = WhiteText
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "我 知 道 了",
                    fontSize = scaledSp(28),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // 稍后提醒
            Button(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange,
                    contentColor = WhiteText
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "稍后提醒（10分钟后）",
                    fontSize = scaledSp(22),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
