package com.biewangle.dontforget.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.biewangle.dontforget.BiewangleApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? BiewangleApp ?: return
            val repository = app.memoRepository
            val scheduler = AlarmScheduler(context)

            // goAsync() 防止 BroadcastReceiver 在协程完成前被系统杀死
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val memos = repository.getAllPendingReminders()
                    memos.forEach { memo ->
                        memo.reminderTime?.let { time ->
                            if (time > System.currentTimeMillis()) {
                                scheduler.schedule(memo.id, memo.title, memo.content, time)
                            }
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
