package com.biewangle.dontforget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.biewangle.dontforget.MainActivity
import com.biewangle.dontforget.R
import com.biewangle.dontforget.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayMemoWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // goAsync() 确保 BroadcastReceiver 在协程完成前不会被系统杀死
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (widgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, widgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.today_memo_widget)

        // 点击跳转到主界面
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

        // 加载今日备忘
        try {
            val app = context.applicationContext as? com.biewangle.dontforget.BiewangleApp
            val memos = app?.memoRepository?.getTodayMemos() ?: emptyList()

            if (memos.isEmpty()) {
                views.setViewVisibility(R.id.widget_list, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
                views.setTextViewText(R.id.widget_empty, "今天没有待办事项")
            } else {
                views.setViewVisibility(R.id.widget_list, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)

                // 构建事项列表文字
                val memoTexts = memos.take(5).map { memo ->
                    val timeStr = memo.formattedReminderTime()?.let { "$it " } ?: ""
                    if (memo.isCompleted) "✅ $timeStr${memo.title}" else "☐ $timeStr${memo.title}"
                }

                // 简化为单个TextView显示（RemoteViews不支持ListAdapter复杂模式）
                views.setTextViewText(
                    R.id.widget_empty,
                    if (memoTexts.isEmpty()) "今天没有待办事项" else memoTexts.joinToString("\n")
                )
                views.setViewVisibility(R.id.widget_list, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        } catch (e: Exception) {
            views.setTextViewText(R.id.widget_empty, "加载中…")
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
