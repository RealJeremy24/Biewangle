package com.biewangle.dontforget.speech

import android.content.Intent
import android.speech.RecognizerIntent

object SpeechHelper {

    fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "说出事项内容…")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
}
